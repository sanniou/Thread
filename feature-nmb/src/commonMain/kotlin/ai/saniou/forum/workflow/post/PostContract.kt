package ai.saniou.forum.workflow.post

import ai.saniou.thread.data.source.nmb.remote.dto.PostThreadRequest
import androidx.compose.ui.text.input.TextFieldValue
import io.ktor.http.content.PartData

interface PostContract {
    data class State(
        val forumName: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false,
        val postBody: PostThreadRequest = PostThreadRequest(),
        val content: TextFieldValue = TextFieldValue(),
        val image: PartData? = null,
        val water: Boolean = false,
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
        data class UpdateImage(val image: PartData?) : Event
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
