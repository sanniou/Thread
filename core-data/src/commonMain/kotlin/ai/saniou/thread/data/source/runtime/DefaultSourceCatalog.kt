package ai.saniou.thread.data.source.runtime

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.source.ForumSearchConnector
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.domain.source.ReactionConnector
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.domain.source.SubCommentConnector
import ai.saniou.thread.domain.source.UserContentConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DefaultSourceCatalog(
    private val database: Database,
    builtIns: List<Pair<SourceDescriptor, RuntimeSourceRegistration>>,
    factories: Set<RuntimeSourceFactory>,
    defaults: List<SourceDescriptor>,
) : SourceCatalog {
    private val builtInRegistrations = builtIns.associate { it.first.id to it.second }
    private val factoryByType = factories.associateBy(RuntimeSourceFactory::type)
    private val defaultDescriptors = defaults.associateBy(SourceDescriptor::id)
    private val updateMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var mutationVersion = 0L

    private var dynamicRegistrations = emptyMap<String, Pair<SourceDescriptor, RuntimeSourceRegistration>>()
    private var activeRegistrations = emptyMap<String, RuntimeSourceRegistration>()
    private val mutableDescriptors = MutableStateFlow(defaults.sortedBy(SourceDescriptor::displayName))
    override val descriptors: StateFlow<List<SourceDescriptor>> = mutableDescriptors.asStateFlow()
    private val mutableSources = MutableStateFlow<List<Source>>(emptyList())
    override val availableSources: StateFlow<List<Source>> = mutableSources.asStateFlow()

    init {
        require(builtInRegistrations.size == builtIns.size) { "Duplicate built-in source id" }
        require(factoryByType.size == factories.size) { "Duplicate runtime source factory type" }
        builtIns.forEach { (descriptor, registration) ->
            require(descriptor.id == registration.source.id) {
                "Built-in descriptor '${descriptor.id}' does not match source '${registration.source.id}'"
            }
        }
        rebuild(mutableDescriptors.value)
        scope.launch {
            val restored = loadDescriptors()
            if (restored != null) {
                updateMutex.withLock {
                    if (mutationVersion == 0L) applyDescriptors(mergeRequiredDefaults(restored))
                }
            }
        }
    }

    override fun source(sourceId: String): Source? = activeRegistrations[sourceId]?.source
    override fun supports(type: SourceType): Boolean = type in factoryByType
    override fun search(sourceId: String): ForumSearchConnector? = activeRegistrations[sourceId]?.search
    override fun userContent(sourceId: String): UserContentConnector? = activeRegistrations[sourceId]?.userContent
    override fun posting(sourceId: String): PostingConnector? = activeRegistrations[sourceId]?.posting
    override fun login(sourceId: String): LoginConnector? = activeRegistrations[sourceId]?.login
    override fun subComments(sourceId: String): SubCommentConnector? = activeRegistrations[sourceId]?.subComments
    override fun reactions(sourceId: String): ReactionConnector? = activeRegistrations[sourceId]?.reactions

    override suspend fun upsert(descriptor: SourceDescriptor) = updateMutex.withLock {
        val existing = mutableDescriptors.value.firstOrNull { it.id == descriptor.id }
        require(existing?.isBuiltIn != true) { "Built-in source '${descriptor.id}' cannot be replaced" }
        require(descriptor.type in factoryByType) { "No runtime factory for ${descriptor.type}" }
        val next = mutableDescriptors.value.filterNot { it.id == descriptor.id } + descriptor.copy(isBuiltIn = false)
        require(next.map(SourceDescriptor::id).distinct().size == next.size) { "Duplicate source id" }
        require(next.any(SourceDescriptor::enabled)) { "At least one source must remain enabled" }
        applyAndPersist(next)
        if (existing != null && existing.runtimeIdentity() != descriptor.runtimeIdentity()) {
            clearSourceNamespace(descriptor.id)
        }
    }

    override suspend fun setEnabled(sourceId: String, enabled: Boolean) = updateMutex.withLock {
        val current = mutableDescriptors.value
        require(current.any { it.id == sourceId }) { "Source '$sourceId' not found" }
        val next = current.map { if (it.id == sourceId) it.copy(enabled = enabled) else it }
        require(next.any(SourceDescriptor::enabled)) { "At least one source must remain enabled" }
        applyAndPersist(next)
    }

    override suspend fun remove(sourceId: String) = updateMutex.withLock {
        val descriptor = mutableDescriptors.value.firstOrNull { it.id == sourceId }
            ?: return@withLock
        require(!descriptor.isBuiltIn) { "Built-in source '$sourceId' cannot be removed" }
        applyAndPersist(mutableDescriptors.value.filterNot { it.id == sourceId })
        clearSourceNamespace(sourceId)
    }

    private suspend fun applyAndPersist(descriptors: List<SourceDescriptor>) {
        val normalized = descriptors.sortedBy(SourceDescriptor::displayName)
        val previous = mutableDescriptors.value
        applyDescriptors(normalized)
        try {
            persistDescriptors(normalized)
            mutationVersion++
        } catch (error: Throwable) {
            applyDescriptors(previous)
            throw error
        }
    }

    private fun applyDescriptors(descriptors: List<SourceDescriptor>) {
        rebuild(descriptors)
        mutableDescriptors.value = descriptors
    }

    private fun rebuild(descriptors: List<SourceDescriptor>) {
        val nextDynamic = mutableMapOf<String, Pair<SourceDescriptor, RuntimeSourceRegistration>>()
        val nextActive = linkedMapOf<String, RuntimeSourceRegistration>()
        val newlyCreated = mutableListOf<RuntimeSourceRegistration>()
        try {
            descriptors.forEach { descriptor ->
                if (!descriptor.enabled) return@forEach
                val cached = dynamicRegistrations[descriptor.id]?.takeIf { it.first == descriptor }?.second
                val registration = builtInRegistrations[descriptor.id]
                    ?: cached
                    ?: factoryByType[descriptor.type]?.create(descriptor)?.also { newlyCreated += it }
                    ?: error("No source implementation for ${descriptor.type}")
                check(registration.source.id == descriptor.id) {
                    "Runtime factory created '${registration.source.id}' for descriptor '${descriptor.id}'"
                }
                if (descriptor.id !in builtInRegistrations) {
                    nextDynamic[descriptor.id] = descriptor to registration
                }
                check(nextActive.put(descriptor.id, registration) == null) { "Duplicate source '${descriptor.id}'" }
            }
        } catch (error: Throwable) {
            newlyCreated.forEach { it.dispose() }
            throw error
        }
        dynamicRegistrations.forEach { (id, previous) ->
            if (nextDynamic[id]?.second !== previous.second) previous.second.dispose()
        }
        dynamicRegistrations = nextDynamic
        activeRegistrations = nextActive
        mutableSources.value = descriptors.mapNotNull { nextActive[it.id]?.source }
    }

    private fun mergeRequiredDefaults(restored: List<SourceDescriptor>): List<SourceDescriptor> {
        val byId = restored.associateBy(SourceDescriptor::id).toMutableMap()
        defaultDescriptors.values.filter(SourceDescriptor::isBuiltIn).forEach { builtIn ->
            val saved = byId[builtIn.id]
            byId[builtIn.id] = builtIn.copy(enabled = saved?.enabled ?: builtIn.enabled)
        }
        return byId.values.sortedBy(SourceDescriptor::displayName)
    }

    private suspend fun loadDescriptors(): List<SourceDescriptor>? = withContext(ioDispatcher) {
        val content = database.keyValueQueries.getKeyValue(STORAGE_KEY).executeAsOneOrNull()?.content
            ?: return@withContext null
        runCatching {
            json.decodeFromString<List<PersistedSourceDescriptor>>(content).map { it.toDomain() }
        }.getOrNull()
    }

    private suspend fun persistDescriptors(descriptors: List<SourceDescriptor>) = withContext(ioDispatcher) {
        val content = json.encodeToString(descriptors.map(PersistedSourceDescriptor::fromDomain))
        database.keyValueQueries.insertKeyValue(STORAGE_KEY, content)
    }

    private suspend fun clearSourceNamespace(sourceId: String) = withContext(ioDispatcher) {
        database.transaction {
            database.favoriteChannelQueries.deleteFavoriteChannelsBySource(sourceId)
            database.historyQueries.deleteHistoryBySource(sourceId)
            database.trendQueries.deleteTrendsBySource(sourceId)
            database.commentQueries.deleteCommentListingsBySource(sourceId)
            database.commentQueries.deleteCommentsBySource(sourceId)
            database.topicTagQueries.deleteTopicTagsBySource(sourceId)
            database.imageQueries.deleteImagesBySource(sourceId)
            database.topicQueries.deleteTopicListingsBySource(sourceId)
            database.topicQueries.deleteTopicsBySource(sourceId)
            database.channelQueries.deleteChannelsBySource(sourceId)
            database.channelQueries.deleteChannelCategoriesBySource(sourceId)
            database.accountQueries.deleteAccountsBySource(sourceId)
            database.remoteKeyQueries.deleteBySource(sourceId)
            database.keyValueQueries.deleteKeyValue("cookie_store_$sourceId")
            database.keyValueQueries.deleteKeyValue("${sourceId}_initialized")
        }
    }

    private companion object {
        const val STORAGE_KEY = "source_runtime_catalog_v1"
    }
}

private fun SourceDescriptor.runtimeIdentity(): Pair<SourceType, String?> =
    type to baseUrl?.trim()?.trimEnd('/')

@Serializable
private data class PersistedSourceDescriptor(
    val id: String,
    val type: String,
    val displayName: String,
    val baseUrl: String? = null,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val options: Map<String, String> = emptyMap(),
) {
    fun toDomain() = SourceDescriptor(
        id = id,
        type = SourceType(type),
        displayName = displayName,
        baseUrl = baseUrl,
        enabled = enabled,
        isBuiltIn = isBuiltIn,
        options = options,
    )

    companion object {
        fun fromDomain(value: SourceDescriptor) = PersistedSourceDescriptor(
            id = value.id,
            type = value.type.value,
            displayName = value.displayName,
            baseUrl = value.baseUrl,
            enabled = value.enabled,
            isBuiltIn = value.isBuiltIn,
            options = value.options,
        )
    }
}
