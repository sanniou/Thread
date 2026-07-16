package ai.saniou.thread.feature.settings

import ai.saniou.thread.domain.model.reader.ReaderSchedulerState

data class UserDataDialog(
    val isImport: Boolean,
    val payload: String = "",
)

interface SyncSettingsContract {
    data class State(
        val endpoint: String = "",
        val username: String = "",
        val password: String = "",
        val isWorking: Boolean = false,
        val dialog: UserDataDialog? = null,
        val message: String? = null,
        val scheduler: ReaderSchedulerState = ReaderSchedulerState(),
        val activeRefreshCount: Int = 0,
        val failedRefreshCount: Int = 0,
    )

    sealed interface Event {
        data class EndpointChanged(val value: String) : Event
        data class UsernameChanged(val value: String) : Event
        data class PasswordChanged(val value: String) : Event
        object SaveWebDav : Event
        object ClearWebDav : Event
        object ExportLocal : Event
        object ShowImportLocal : Event
        data class ImportLocal(val payload: String) : Event
        object BackupWebDav : Event
        object RestoreWebDav : Event
        object DismissDialog : Event
        object MessageShown : Event
    }
}
