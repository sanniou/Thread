package ai.saniou.nmb.workflow.post

import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.domain.PostUseCase
import ai.saniou.nmb.workflow.post.PostContract.Effect
import ai.saniou.nmb.workflow.post.PostContract.Event
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import ai.saniou.nmb.workflow.post.PostContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(
    private val postUseCase: PostUseCase,
    private val forumUseCase: ForumUseCase,
    private val fid: Int?,
    private val resto: Int?,
    private val forumName: String?
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect = _effect.asSharedFlow()

    init {
        _state.update { it.copy(forumName = forumName ?: "回复") }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.UpdateContent -> _state.update {
                it.copy(
                    content = event.content,
                    postBody = it.postBody.copy(content = event.content.text)
                )
            }

            is Event.InsertContent -> {
                val currentState = _state.value
                val currentContent = currentState.content
                val selection = currentContent.selection
                val newText = currentContent.text.replaceRange(selection.min, selection.max, event.text)
                val newSelection = selection.min + event.text.length
                _state.update {
                    it.copy(
                        content = TextFieldValue(
                            text = newText,
                            selection = TextRange(newSelection)
                        ),
                        postBody = it.postBody.copy(content = newText)
                    )
                }
            }

            is Event.UpdateName -> _state.update {
                it.copy(postBody = it.postBody.copy(name = event.name))
            }
            is Event.UpdateTitle -> _state.update {
                it.copy(postBody = it.postBody.copy(title = event.title))
            }
            is Event.UpdateImage -> _state.update { it.copy(image = event.image) }
            is Event.ToggleWater -> _state.update { it.copy(water = event.water) }
            Event.ToggleEmoticonPicker -> _state.update {
               it.copy(showEmoticonPicker = !it.showEmoticonPicker)
            }
            Event.ToggleDiceInputs -> _state.update {
               it.copy(showDiceInputs = !it.showDiceInputs)
            }
            Event.ToggleMoreOptions -> _state.update {
                it.copy(showMoreOptions = !it.showMoreOptions)
            }
            Event.ToggleConfirmDialog -> _state.update {
                it.copy(showConfirmDialog = !it.showConfirmDialog)
            }
            Event.ClearError -> _state.update {
                it.copy(error = null)
            }
            Event.Submit -> submit()
        }
    }

    private fun submit() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, showConfirmDialog = false) }
            try {
                val s = _state.value
                val responseHtml = if (resto != null) {
                    postUseCase.reply(
                        content = s.postBody.content!!,
                        resto = resto,
                        name = s.postBody.name,
                        title = s.postBody.title,
                        image = s.image,
                        water = s.water
                    )
                } else if (fid != null) {
                    postUseCase.post(
                        fid = fid,
                        content = s.postBody.content!!,
                        name = s.postBody.name,
                        title = s.postBody.title,
                        image = s.image,
                        water = s.water
                    )
                } else {
                    throw IllegalStateException("fid and resto cannot both be null")
                }

                val error = extractError(responseHtml)
                if (error != null) {
                    _state.update { it.copy(isLoading = false, error = error) }
                    // Error is now handled by State and Dialog, removing duplicate snackbar
                } else {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                    kotlinx.coroutines.delay(1500) // Keep success state for a while
                    _effect.emit(Effect.NavigateBack)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun extractError(html: String): String? {
        val errorTag = "<p class=\"error\">"
        val errorIndex = html.indexOf(errorTag)
        if (errorIndex != -1) {
            val startIndex = errorIndex + errorTag.length
            val endIndex = html.indexOf("</p>", startIndex)
            if (endIndex != -1) {
                return html.substring(startIndex, endIndex)
            }
        }
        return null
    }
}
