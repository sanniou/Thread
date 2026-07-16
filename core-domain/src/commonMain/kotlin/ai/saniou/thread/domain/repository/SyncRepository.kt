package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.sync.UserDataExport
import ai.saniou.thread.domain.model.sync.UserDataImportReport
import ai.saniou.thread.domain.model.sync.WebDavConfig
import kotlinx.coroutines.flow.Flow

/** Owns versioned user-data snapshots; transports remain data-layer details. */
interface SyncRepository {
    suspend fun exportUserData(): Result<UserDataExport>
    suspend fun importUserData(payload: String): Result<UserDataImportReport>

    fun observeWebDavConfig(): Flow<WebDavConfig?>
    suspend fun saveWebDavConfig(config: WebDavConfig?)
    suspend fun backupToWebDav(): Result<UserDataExport>
    suspend fun restoreFromWebDav(): Result<UserDataImportReport>
}
