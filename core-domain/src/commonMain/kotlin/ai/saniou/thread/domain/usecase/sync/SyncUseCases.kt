package ai.saniou.thread.domain.usecase.sync

import ai.saniou.thread.domain.model.sync.WebDavConfig
import ai.saniou.thread.domain.repository.SyncRepository

class ExportUserDataUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.exportUserData()
}

class ImportUserDataUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke(payload: String) = repository.importUserData(payload)
}

class ObserveWebDavConfigUseCase(private val repository: SyncRepository) {
    operator fun invoke() = repository.observeWebDavConfig()
}

class SaveWebDavConfigUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke(config: WebDavConfig?) = repository.saveWebDavConfig(config)
}

class BackupToWebDavUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.backupToWebDav()
}

class RestoreFromWebDavUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.restoreFromWebDav()
}
