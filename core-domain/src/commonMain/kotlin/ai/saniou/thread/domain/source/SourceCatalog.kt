package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.repository.Source
import kotlinx.coroutines.flow.StateFlow

/** Live runtime directory. Repositories must query this catalog instead of snapshotting Sources. */
interface SourceCatalog : ConnectorRegistry {
    val descriptors: StateFlow<List<SourceDescriptor>>
    val availableSources: StateFlow<List<Source>>

    fun supports(type: ai.saniou.thread.domain.model.source.SourceType): Boolean

    suspend fun upsert(descriptor: SourceDescriptor)
    suspend fun setEnabled(sourceId: String, enabled: Boolean)
    suspend fun remove(sourceId: String)
}
