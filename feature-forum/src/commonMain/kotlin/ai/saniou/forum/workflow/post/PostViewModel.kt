package ai.saniou.forum.workflow.post

import ai.saniou.forum.workflow.post.PostContract.Effect
import ai.saniou.forum.workflow.post.PostContract.Event
import ai.saniou.forum.workflow.post.PostContract.State
import ai.saniou.thread.domain.usecase.post.CreateReplyUseCase
import ai.saniou.thread.domain.usecase.post.CreateThreadUseCase
import ai.saniou.thread.domain.usecase.post.GetPostDraftUseCase
import ai.saniou.thread.domain.usecase.post.SavePostDraftUseCase
import ai.saniou.thread.domain.usecase.post.DiscardPostDraftUseCase
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_38eb42e243
import thread.feature_forum.generated.resources.s_f95134f690
import thread.feature_forum.generated.resources.action_reply

class PostViewModel(
    private val createThreadUseCase: CreateThreadUseCase,
    private val createReplyUseCase: CreateReplyUseCase,
    private val getPostDraft: GetPostDraftUseCase,
    private val savePostDraft: SavePostDraftUseCase,
    private val discardPostDraft: DiscardPostDraftUseCase,
    private val params: PostViewModelParams,
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect = _effect.asSharedFlow()
    private val draftKey = params.toDraftKey()
    private var draftSaveJob: Job? = null

    init {
        screenModelScope.launch {
            if (params.forumName == null) {
                _state.update { it.copy(forumName = getString(Res.string.action_reply)) }
            } else {
                _state.update { it.copy(forumName = params.forumName) }
            }
            val saved = getPostDraft(draftKey)
            _state.update { current ->
                if (saved == null) current.copy(isDraftLoading = false) else current.copy(
                    postBody = saved.draft,
                    content = TextFieldValue(saved.draft.content),
                    isDraftLoading = false,
                    hasRestoredDraft = true,
                    draftUpdatedAtEpochMillis = saved.updatedAtEpochMillis,
                )
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.UpdateContent -> _state.update {
                it.copy(
                    content = event.content,
                    postBody = it.postBody.copy(content = event.content.text)
                )
            }.also { scheduleDraftSave() }

            is Event.InsertContent -> {
                val currentState = _state.value
                val currentContent = currentState.content
                val selection = currentContent.selection
                val newText =
                    currentContent.text.replaceRange(selection.min, selection.max, event.text)
                val newSelection = selection.min + event.text.length
                _state.update {
                    it.copy(
                        content = TextFieldValue(
                            text = newText,
                            selection = TextRange(newSelection)
                        ),
                        postBody = it.postBody.copy(content = newText)
                    )
                }.also { scheduleDraftSave() }
            }

            is Event.UpdateName -> _state.update {
                it.copy(postBody = it.postBody.copy(name = event.name))
            }.also { scheduleDraftSave() }

            is Event.UpdateTitle -> _state.update {
                it.copy(postBody = it.postBody.copy(title = event.title))
            }.also { scheduleDraftSave() }

            is Event.UpdateImage -> _state.update {
                it.copy(postBody = it.postBody.copy(attachment = event.image))
            }.also { scheduleDraftSave() }
            is Event.ToggleWater -> _state.update {
                it.copy(postBody = it.postBody.copy(water = event.water))
            }.also { scheduleDraftSave() }
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

            Event.DiscardDraft -> discardDraft()
            Event.Close -> closeComposer()

            Event.Submit -> submit()
        }
    }

    private fun scheduleDraftSave() {
        draftSaveJob?.cancel()
        draftSaveJob = screenModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MILLIS)
            saveDraftNow()
        }
    }

    private suspend fun saveDraftNow(): Boolean {
        val draft = _state.value.postBody
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        _state.update { it.copy(isDraftSaving = true) }
        return runCatching { savePostDraft(SavedPostDraft(key = draftKey, draft = draft, updatedAtEpochMillis = now)) }
            .onSuccess {
                _state.update {
                    it.copy(
                        isDraftSaving = false,
                        hasRestoredDraft = draft.hasContent(),
                        draftUpdatedAtEpochMillis = now.takeIf { draft.hasContent() },
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(isDraftSaving = false, error = error.message ?: getString(Res.string.s_f95134f690)) }
            }
            .isSuccess
    }

    private fun discardDraft() {
        draftSaveJob?.cancel()
        screenModelScope.launch {
            discardPostDraft(draftKey)
            _state.update {
                it.copy(
                    postBody = PostDraft(),
                    content = TextFieldValue(),
                    hasRestoredDraft = false,
                    draftUpdatedAtEpochMillis = null,
                    isDraftSaving = false,
                )
            }
            _effect.emit(Effect.ShowSnackbar(getString(Res.string.s_38eb42e243)))
        }
    }

    private fun closeComposer() {
        draftSaveJob?.cancel()
        screenModelScope.launch {
            if (saveDraftNow()) _effect.emit(Effect.NavigateBack)
        }
    }

    private fun submit() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, showConfirmDialog = false) }
            try {
                val s = _state.value
                val result = if (params.topicId != null) {
                    createReplyUseCase(
                        sourceId = params.sourceId,
                        topicId = params.topicId,
                        draft = s.postBody,
                    )
                } else if (params.channelId != null) {
                    createThreadUseCase(
                        sourceId = params.sourceId,
                        channelId = params.channelId,
                        draft = s.postBody,
                    )
                } else {
                    throw IllegalStateException("channelId and topicId cannot both be null")
                }

                val error = result.message
                if (error != null) {
                    _state.update { it.copy(isLoading = false, error = error) }
                } else {
                    discardPostDraft(draftKey)
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                    kotlinx.coroutines.delay(1500)
                    _effect.emit(Effect.NavigateBack)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

}

private const val DRAFT_SAVE_DEBOUNCE_MILLIS = 450L

private fun PostViewModelParams.toDraftKey(): PostDraftKey = when {
    topicId != null -> PostDraftKey(sourceId, PostDraftTargetKind.TOPIC, topicId)
    channelId != null -> PostDraftKey(sourceId, PostDraftTargetKind.CHANNEL, channelId)
    else -> throw IllegalArgumentException("channelId and topicId cannot both be null")
}

private fun PostDraft.hasContent() =
    content.isNotBlank() || !name.isNullOrBlank() || !title.isNullOrBlank() || attachment != null

data class PostViewModelParams(
    val sourceId: String,
    val channelId: String? = null,
    val topicId: String? = null,
    val forumName: String? = null,
)
