package ai.saniou.thread.domain.usecase.source

import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.source.SourceCatalog

class ObserveSourceDescriptorsUseCase(private val catalog: SourceCatalog) {
    operator fun invoke() = catalog.descriptors
}

class UpsertSourceDescriptorUseCase(private val catalog: SourceCatalog) {
    suspend operator fun invoke(descriptor: SourceDescriptor) = catalog.upsert(descriptor)
}

class SetSourceEnabledUseCase(private val catalog: SourceCatalog) {
    suspend operator fun invoke(sourceId: String, enabled: Boolean) = catalog.setEnabled(sourceId, enabled)
}

class RemoveSourceDescriptorUseCase(private val catalog: SourceCatalog) {
    suspend operator fun invoke(sourceId: String) = catalog.remove(sourceId)
}
