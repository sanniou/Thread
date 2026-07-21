package ai.saniou.thread.feature.settings

import ai.saniou.thread.domain.model.reader.ReaderSchedulerState
import ai.saniou.thread.domain.model.collection.SmartCollection
import ai.saniou.thread.domain.model.collection.SmartCollectionSort
import ai.saniou.thread.domain.model.collection.SmartCollectionGroup
import ai.saniou.thread.domain.model.settings.AppearancePreferences
import ai.saniou.thread.domain.model.social.SocialSourceDescriptor

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
        val appearance: AppearancePreferences = AppearancePreferences(),
        val smartCollections: List<SmartCollection> = emptyList(),
        val socialSources: List<SocialSourceDescriptor> = emptyList(),
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
        data class AppearanceChanged(val value: AppearancePreferences) : Event
        object ResetAppearance : Event
        data class SaveSmartCollection(
            val name: String,
            val query: String,
            val unreadOnly: Boolean,
            val bookmarkedOnly: Boolean,
            val sort: SmartCollectionSort,
            val groupBy: SmartCollectionGroup,
        ) : Event
        data class DeleteSmartCollection(val id: String) : Event
        data class ToggleSmartCollectionPinned(val id: String, val pinned: Boolean) : Event
        data class MoveSmartCollection(val id: String, val delta: Int) : Event
        data class SaveSocialSource(
            val name: String,
            val baseUrl: String,
            val accessToken: String,
        ) : Event
        data class ToggleSocialSource(val source: SocialSourceDescriptor) : Event
        data class DeleteSocialSource(val id: String) : Event
    }
}
