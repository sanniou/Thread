package ai.saniou.nmb.workflow.post

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.PostReplyRequest
import ai.saniou.nmb.data.entity.PostThreadRequest
import ai.saniou.nmb.domain.PostUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(private val postUseCase: PostUseCase) : ViewModel() {

    private val dataUiState = MutableStateFlow(
        PostUiState(
            title = "",
            content = "",
            forumId = 0,
            threadId = 0,
            hasImage = false,
            imagePath = "",
            addWatermark = false,
            onTitleChanged = { title ->
                updateUiState { it.copy(title = title) }
            },
            onContentChanged = { content ->
                updateUiState { it.copy(content = content) }
            },
            onSelectImage = {
                // 选择图片的逻辑
                // 这里需要平台特定的实现
                updateUiState { it.copy(hasImage = true, imagePath = "selected_image_path") }
            },
            onClearImage = {
                updateUiState { it.copy(hasImage = false, imagePath = "") }
            },
            onWatermarkChanged = { addWatermark ->
                updateUiState { it.copy(addWatermark = addWatermark) }
            },
            onSubmit = {
                doSubmit()
            }
        )
    )

    private fun doSubmit() {
        if (dataUiState.value.threadId > 0) {
            submitReply()
        } else {
            submitThread()
        }
    }

    private val _uiState =
        MutableStateFlow<UiStateWrapper>(UiStateWrapper.Success(dataUiState.value))

    val uiState = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(3000),
        UiStateWrapper.Success(dataUiState.value)
    )

    fun setForumId(forumId: Int) {
        updateUiState { it.copy(forumId = forumId) }
    }

    fun setThreadId(threadId: Long) {
        updateUiState { it.copy(threadId = threadId) }
    }

    fun submitThread() {
        val state = dataUiState.value

        if (state.content.isBlank() && !state.hasImage) {
            _uiState.value = UiStateWrapper.Error(
                IllegalStateException("内容和图片不能同时为空"),
                "内容和图片不能同时为空"
            )
            return
        }

        if (state.forumId <= 0) {
            _uiState.value = UiStateWrapper.Error(
                IllegalStateException("未选择版块"),
                "请选择要发帖的版块"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = UiStateWrapper.Loading

                val request = PostThreadRequest(
                    title = state.title.takeIf { it.isNotBlank() },
                    content = state.content.takeIf { it.isNotBlank() },
                    fid = state.forumId,
                    water = if (state.addWatermark) true else null
                )

                val result = postUseCase.postThread(request)

                // 处理结果
                if (result.contains("成功")) {
                    _uiState.value = UiStateWrapper.Success(
                        dataUiState.value.copy(
                            title = "",
                            content = "",
                            hasImage = false,
                            imagePath = ""
                        )
                    )
                } else {
                    _uiState.value = UiStateWrapper.Error(
                        IllegalStateException(result),
                        "发帖失败: $result"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiStateWrapper.Error(
                    e,
                    "发帖失败: ${e.message}"
                )
            }
        }
    }

    fun submitReply() {
        val state = dataUiState.value

        if (state.content.isBlank() && !state.hasImage) {
            _uiState.value = UiStateWrapper.Error(
                IllegalStateException("内容和图片不能同时为空"),
                "内容和图片不能同时为空"
            )
            return
        }

        if (state.threadId <= 0) {
            _uiState.value = UiStateWrapper.Error(
                IllegalStateException("未指定回复的帖子"),
                "回复目标不存在"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = UiStateWrapper.Loading

                val request = PostReplyRequest(
                    title = state.title.takeIf { it.isNotBlank() },
                    content = state.content.takeIf { it.isNotBlank() },
                    resTo = state.threadId.toInt(),
                    image = state.imagePath.takeIf { state.hasImage },
                    water = if (state.addWatermark) true else null
                )

                val result = postUseCase.postReply(request)

                // 处理结果
                if (result.contains("成功")) {
                    _uiState.value = UiStateWrapper.Success(
                        dataUiState.value.copy(
                            title = "",
                            content = "",
                            hasImage = false,
                            imagePath = ""
                        )
                    )
                } else {
                    _uiState.value = UiStateWrapper.Error(
                        IllegalStateException(result),
                        "回复失败: $result"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiStateWrapper.Error(
                    e,
                    "回复失败: ${e.message}"
                )
            }
        }
    }

    private fun updateUiState(invoke: (PostUiState) -> PostUiState) {
        dataUiState.update(invoke)
        if (_uiState.value is UiStateWrapper.Success<*>) {
            _uiState.value = UiStateWrapper.Success(dataUiState.value)
        }
    }
}

data class PostUiState(
    val title: String,
    val content: String,
    val forumId: Int,
    val threadId: Long,
    val hasImage: Boolean,
    val imagePath: String,
    val addWatermark: Boolean,
    val onTitleChanged: (String) -> Unit,
    val onContentChanged: (String) -> Unit,
    val onSelectImage: () -> Unit,
    val onClearImage: () -> Unit,
    val onWatermarkChanged: (Boolean) -> Unit,
    val onSubmit: () -> Unit
) : UiStateWrapper
