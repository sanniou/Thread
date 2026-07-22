package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.source.UserRelationProfile
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface UserDetailContract {
    data class State(
        val userHash: String,
        val topics: Flow<PagingData<Topic>>? = null,
        val comments: Flow<PagingData<Comment>>? = null,
        val currentTab: Tab = Tab.Topics,
        val supportsUserFollow: Boolean = false,
        val supportsProfileEdit: Boolean = false,
        val isSelf: Boolean = false,
        val profile: UserRelationProfile? = null,
        val isProfileLoading: Boolean = false,
        val isFollowBusy: Boolean = false,
        val isEditDialogOpen: Boolean = false,
        val editNickName: String = "",
        val editIntro: String = "",
        val editSex: Int = 0,
        val isSavingProfile: Boolean = false,
        val isUploadingPortrait: Boolean = false,
        val pendingPortraitFileName: String? = null,
        val actionMessage: String? = null,
    )

    sealed interface Event {
        data class SwitchTab(val tab: Tab) : Event
        data object Back : Event
        data object ToggleFollow : Event
        data object OpenEditProfile : Event
        data object DismissEditProfile : Event
        data class EditNickNameChanged(val value: String) : Event
        data class EditIntroChanged(val value: String) : Event
        data class EditSexChanged(val value: Int) : Event
        data object SubmitEditProfile : Event
        data object PickPortrait : Event
        data class PortraitPicked(
            val fileName: String,
            val bytes: ByteArray,
            val contentType: String,
        ) : Event {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false
                other as PortraitPicked
                return fileName == other.fileName &&
                    contentType == other.contentType &&
                    bytes.contentEquals(other.bytes)
            }

            override fun hashCode(): Int {
                var result = fileName.hashCode()
                result = 31 * result + bytes.contentHashCode()
                result = 31 * result + contentType.hashCode()
                return result
            }
        }
        data object UploadPortrait : Event
        data object ClearPendingPortrait : Event
        data object ConsumeActionMessage : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data object RequestPortraitPicker : Effect
    }

    enum class Tab {
        Topics,
        Comments
    }
}
