package ai.saniou.forum.workflow.post

import ai.saniou.thread.domain.model.forum.PostAttachment
import ai.saniou.thread.domain.model.forum.PostDraft
import androidx.compose.ui.text.input.TextFieldValue

interface PostContract {
    data class State(
        val forumName: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false,
        val postBody: PostDraft = PostDraft(),
        val content: TextFieldValue = TextFieldValue(),
        val showEmoticonPicker: Boolean = false,
        val showDiceInputs: Boolean = false,
        val showMoreOptions: Boolean = false,
        val showConfirmDialog: Boolean = false,
    )

    sealed interface Event {
        data class UpdateName(val name: String) : Event
        data class UpdateTitle(val title: String) : Event
        data class UpdateContent(val content: TextFieldValue) : Event
        data class InsertContent(val text: String) : Event
        data class UpdateImage(val image: PostAttachment?) : Event
        data class ToggleWater(val water: Boolean) : Event
        data object ToggleEmoticonPicker : Event
        data object ToggleDiceInputs : Event
        data object ToggleMoreOptions : Event
        data object ToggleConfirmDialog : Event
        data object ClearError : Event
        data object Submit : Event
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data object NavigateBack : Effect
    }
}
