package ai.saniou.thread.feature.settings

import ai.saniou.thread.domain.model.sync.WebDavConfig
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.collection.SmartCollection
import ai.saniou.thread.domain.model.collection.SmartCollectionRules
import ai.saniou.thread.domain.repository.AppearanceRepository
import ai.saniou.thread.domain.usecase.block.AddContentBlockUseCase
import ai.saniou.thread.domain.usecase.block.ObserveContentBlocksUseCase
import ai.saniou.thread.domain.usecase.block.RemoveContentBlockUseCase
import ai.saniou.thread.domain.repository.SmartCollectionRepository
import ai.saniou.thread.domain.model.social.SocialSourceDescriptor
import ai.saniou.thread.domain.usecase.social.ObserveSocialSourcesUseCase
import ai.saniou.thread.domain.usecase.social.SaveSocialSourceUseCase
import ai.saniou.thread.domain.usecase.social.RemoveSocialSourceUseCase
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.usecase.reader.ObserveReaderSchedulerUseCase
import ai.saniou.thread.domain.usecase.refresh.ObserveRefreshDiagnosticsUseCase
import ai.saniou.thread.domain.usecase.sync.ObserveWebDavConfigUseCase
import ai.saniou.thread.domain.usecase.sync.SaveWebDavConfigUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.error_action_failed
import thread.composeapp.generated.resources.s_1ad61173b4
import thread.composeapp.generated.resources.filter_unread_only
import thread.composeapp.generated.resources.s_521889b79f
import thread.composeapp.generated.resources.s_6da9925ffe
import thread.composeapp.generated.resources.filter_bookmarked_only
import thread.composeapp.generated.resources.s_790909b45b
import thread.composeapp.generated.resources.s_86cbda7968
import thread.composeapp.generated.resources.s_d85e9647ff
import thread.composeapp.generated.resources.s_e0d57d4e7d
import thread.composeapp.generated.resources.s_f21ab6df37
import thread.composeapp.generated.resources.content_block_added
import thread.composeapp.generated.resources.content_block_removed

class SyncSettingsViewModel(
    private val executeProductAction: ExecuteProductActionUseCase,
    private val observeWebDavConfig: ObserveWebDavConfigUseCase,
    private val saveWebDavConfig: SaveWebDavConfigUseCase,
    private val observeReaderScheduler: ObserveReaderSchedulerUseCase,
    private val observeRefreshDiagnostics: ObserveRefreshDiagnosticsUseCase,
    private val appearanceRepository: AppearanceRepository,
    private val smartCollectionRepository: SmartCollectionRepository,
    observeSocialSources: ObserveSocialSourcesUseCase,
    private val saveSocialSourceUseCase: SaveSocialSourceUseCase,
    private val removeSocialSourceUseCase: RemoveSocialSourceUseCase,
    observeContentBlocks: ObserveContentBlocksUseCase,
    private val addContentBlock: AddContentBlockUseCase,
    private val removeContentBlock: RemoveContentBlockUseCase,
) : ScreenModel {
    private val _state = MutableStateFlow(SyncSettingsContract.State())
    val state: StateFlow<SyncSettingsContract.State> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            observeWebDavConfig().collect { config ->
                _state.update {
                    it.copy(
                        endpoint = config?.endpoint.orEmpty(),
                        username = config?.username.orEmpty(),
                        password = config?.password.orEmpty(),
                    )
                }
            }
        }
        screenModelScope.launch {
            observeReaderScheduler().collect { scheduler -> _state.update { it.copy(scheduler = scheduler) } }
        }
        screenModelScope.launch {
            observeRefreshDiagnostics().collect { tasks ->
                _state.update {
                    it.copy(
                        activeRefreshCount = tasks.values.count { task -> task.status == RefreshStatus.RUNNING },
                        failedRefreshCount = tasks.values.count { task -> task.status == RefreshStatus.FAILED },
                    )
                }
            }
        }
        screenModelScope.launch {
            appearanceRepository.observe().collect { appearance ->
                _state.update { it.copy(appearance = appearance) }
            }
        }
        screenModelScope.launch {
            smartCollectionRepository.observeCollections().collect { collections ->
                _state.update { it.copy(smartCollections = collections) }
            }
        }
        screenModelScope.launch {
            observeSocialSources().collect { sources ->
                _state.update { it.copy(socialSources = sources) }
            }
        }
        screenModelScope.launch {
            observeContentBlocks().collect { blocks ->
                _state.update { it.copy(contentBlocks = blocks) }
            }
        }

    }

    fun onEvent(event: SyncSettingsContract.Event) {
        when (event) {
            is SyncSettingsContract.Event.EndpointChanged -> _state.update { it.copy(endpoint = event.value) }
            is SyncSettingsContract.Event.UsernameChanged -> _state.update { it.copy(username = event.value) }
            is SyncSettingsContract.Event.PasswordChanged -> _state.update { it.copy(password = event.value) }
            SyncSettingsContract.Event.SaveWebDav -> saveConfig()
            SyncSettingsContract.Event.ClearWebDav -> clearConfig()
            SyncSettingsContract.Event.ExportLocal -> exportLocal()
            SyncSettingsContract.Event.ShowImportLocal -> _state.update { it.copy(dialog = UserDataDialog(true)) }
            is SyncSettingsContract.Event.ImportLocal -> importLocal(event.payload)
            SyncSettingsContract.Event.BackupWebDav -> backup()
            SyncSettingsContract.Event.RestoreWebDav -> restore()
            SyncSettingsContract.Event.DismissDialog -> _state.update { it.copy(dialog = null) }
            SyncSettingsContract.Event.MessageShown -> _state.update { it.copy(message = null) }
            is SyncSettingsContract.Event.AppearanceChanged -> screenModelScope.launch {
                appearanceRepository.save(event.value)
            }
            SyncSettingsContract.Event.ResetAppearance -> screenModelScope.launch {
                appearanceRepository.reset()
            }
            is SyncSettingsContract.Event.SaveSmartCollection -> saveSmartCollection(event)
            is SyncSettingsContract.Event.DeleteSmartCollection -> screenModelScope.launch {
                smartCollectionRepository.delete(event.id)
            }
            is SyncSettingsContract.Event.ToggleSmartCollectionPinned -> screenModelScope.launch {
                smartCollectionRepository.setPinned(event.id, event.pinned)
            }
            is SyncSettingsContract.Event.MoveSmartCollection -> moveSmartCollection(event.id, event.delta)
            is SyncSettingsContract.Event.SaveSocialSource -> saveSocialSource(event)
            is SyncSettingsContract.Event.ToggleSocialSource -> screenModelScope.launch {
                runCatching { saveSocialSourceUseCase(event.source.copy(enabled = !event.source.enabled)) }
                    .onFailure(::showFailure)
            }
            is SyncSettingsContract.Event.AddKeywordBlock -> addKeywordBlock(event.raw)
            is SyncSettingsContract.Event.AddUserBlock -> addUserBlock(event.userId, event.userName)
            is SyncSettingsContract.Event.RemoveContentBlock -> removeBlock(event.id)
            is SyncSettingsContract.Event.DeleteSocialSource -> screenModelScope.launch {
                runCatching { removeSocialSourceUseCase(event.id) }
                    .onSuccess { _state.update { it.copy(message = getString(Res.string.s_f21ab6df37)) } }
                    .onFailure(::showFailure)
            }
        }
    }

    private fun saveSmartCollection(event: SyncSettingsContract.Event.SaveSmartCollection) {
        val name = event.name.trim()
        val query = event.query.trim()
        if (name.isBlank() || (query.isBlank() && !event.unreadOnly && !event.bookmarkedOnly)) {
            screenModelScope.launch {
                _state.update { it.copy(message = getString(Res.string.s_e0d57d4e7d)) }
            }
            return
        }
        screenModelScope.launch {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            smartCollectionRepository.save(
                SmartCollection(
                    id = "collection-$now",
                    name = name,
                    description = buildList {
                        if (query.isNotBlank()) add(getString(Res.string.s_790909b45b, query))
                        if (event.unreadOnly) add(getString(Res.string.filter_unread_only))
                        if (event.bookmarkedOnly) add(getString(Res.string.filter_bookmarked_only))
                    }.joinToString(" · "),
                    rules = SmartCollectionRules(
                        query = query,
                        unreadOnly = event.unreadOnly,
                        bookmarkedOnly = event.bookmarkedOnly,
                    ),
                    sort = event.sort,
                    groupBy = event.groupBy,
                    position = _state.value.smartCollections.size,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            )
            _state.update { it.copy(message = getString(Res.string.s_1ad61173b4)) }
        }
    }

    private fun moveSmartCollection(id: String, delta: Int) {
        val ordered = _state.value.smartCollections.toMutableList()
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return
        val moved = ordered.removeAt(from)
        ordered.add(to, moved)
        screenModelScope.launch { smartCollectionRepository.reorder(ordered.map { it.id }) }
    }

    private fun saveSocialSource(event: SyncSettingsContract.Event.SaveSocialSource) {
        val baseUrl = event.baseUrl.trim().trimEnd('/')
        val name = event.name.trim()
        if (name.isBlank() || event.accessToken.isBlank() ||
            !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))
        ) {
            screenModelScope.launch {
                _state.update { it.copy(message = getString(Res.string.s_6da9925ffe)) }
            }
            return
        }
        val existing = _state.value.socialSources.firstOrNull { it.baseUrl.trimEnd('/') == baseUrl }
        val baseId = baseUrl.substringAfter("://").substringBefore('/')
            .lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').take(48)
            .ifBlank { "social" }
        val sourceId = existing?.id ?: buildString {
            append(baseId)
            if (_state.value.socialSources.any { it.id == baseId }) {
                append('-')
                append(kotlin.time.Clock.System.now().toEpochMilliseconds().toString().takeLast(6))
            }
        }.let { if (it.length == 1) "$it-social" else it }
        screenModelScope.launch {
            runCatching {
                saveSocialSourceUseCase(
                    SocialSourceDescriptor(sourceId, name, baseUrl, enabled = true),
                    event.accessToken,
                )
            }.onSuccess {
                _state.update { it.copy(message = getString(Res.string.s_d85e9647ff)) }
            }.onFailure(::showFailure)
        }
    }

    private fun saveConfig() = launchWork(Res.string.s_86cbda7968) {
        val current = _state.value
        saveWebDavConfig(WebDavConfig(current.endpoint.trim(), current.username, current.password))
    }

    private fun clearConfig() = launchWork(Res.string.s_521889b79f) { saveWebDavConfig(null) }

    private fun exportLocal() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.EXPORT_USER_DATA)).fold(
                onSuccess = { result ->
                    _state.update { it.copy(isWorking = false, dialog = UserDataDialog(false, result.output.orEmpty())) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun importLocal(payload: String) {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.IMPORT_USER_DATA, payload = payload)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            dialog = null,
                            message = result.message,
                        )
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun backup() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.BACKUP_TO_WEBDAV)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(isWorking = false, message = result.message)
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun restore() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.RESTORE_FROM_WEBDAV)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(isWorking = false, message = result.message)
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }


    private fun addKeywordBlock(raw: String) {
        val parts = raw.split(',', ' ', '，', '、', '\t', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        screenModelScope.launch {
            runCatching { addContentBlock.keyword(parts) }.fold(
                onSuccess = {
                    _state.update { it.copy(message = getString(Res.string.content_block_added)) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun addUserBlock(userId: String, userName: String) {
        screenModelScope.launch {
            runCatching { addContentBlock.user(userId, userName) }.fold(
                onSuccess = {
                    _state.update { it.copy(message = getString(Res.string.content_block_added)) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun removeBlock(id: Long) {
        screenModelScope.launch {
            runCatching { removeContentBlock(id) }.fold(
                onSuccess = {
                    _state.update { it.copy(message = getString(Res.string.content_block_removed)) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun launchWork(successRes: StringResource, block: suspend () -> Unit) {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            runCatching { block() }.fold(
                onSuccess = {
                    _state.update { it.copy(isWorking = false, message = getString(successRes)) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun showFailure(error: Throwable) {
        screenModelScope.launch {
            _state.update {
                it.copy(isWorking = false, message = error.message ?: getString(Res.string.error_action_failed))
            }
        }
    }
}
