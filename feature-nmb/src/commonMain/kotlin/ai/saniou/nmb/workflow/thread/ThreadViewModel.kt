package ai.saniou.nmb.workflow.thread

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.domain.GetThreadDetailUseCase
import ai.saniou.nmb.domain.GetThreadRepliesPagingUseCase
import ai.saniou.nmb.domain.ToggleSubscriptionUseCase
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.thread.ThreadContract.Effect
import ai.saniou.nmb.workflow.thread.ThreadContract.Event
import ai.saniou.nmb.workflow.thread.ThreadContract.State
import androidx.paging.cachedIn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ThreadViewModel(
    private val threadId: Long,
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val getThreadRepliesPagingUseCase: GetThreadRepliesPagingUseCase,
    private val forumUseCase: ForumUseCase,
    private val nmbRepository: NmbRepository,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val db: Database,
    private val forumRepository: ForumRepository
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var imageObserverJob: Job? = null

    init {
        loadThread()
        updateLastAccessTime()
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.JumpToPage -> jumpToPage(event.page)
            Event.Refresh -> loadThread()
            Event.TogglePoOnlyMode -> togglePoOnlyMode()
            Event.ToggleSubscription -> toggleSubscription()
            Event.CopyLink -> copyLink()
            is Event.UpdateLastReadReplyId -> updateLastReadReplyId(event.id)
            is Event.ShowImagePreview -> showImagePreview(event.imgPath)
            Event.LoadMoreImages -> loadMoreImages()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun updateLastAccessTime() {
        screenModelScope.launch {
            nmbRepository.updateThreadLastAccessTime(threadId, Clock.System.now().epochSeconds)
        }
    }

    private fun updateLastReadReplyId(replyId: Long) {
        screenModelScope.launch {
            nmbRepository.updateThreadLastReadReplyId(threadId, replyId)
        }
    }

    private fun loadThread() {
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // 开始监听帖子主楼信息
            launch {
                getThreadDetailUseCase(threadId)
                    .catch { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "加载主楼失败: ${e.message}"
                            )
                        }
                    }
                    .collectLatest { detail ->
                        val thread = detail.thread
                        val totalPages =
                            (thread.replyCount / 19) + if (thread.replyCount % 19 > 0) 1 else 0
                        _state.update {
                            it.copy(
                                isLoading = false,
                                thread = thread,
                                lastReadReplyId = detail.lastReadReplyId,
                                totalPages = totalPages.toInt().coerceAtLeast(1),
                                forumName = forumUseCase.getForumName(thread.fid)
                            )
                        }
                    }
            }

            // 加载回复
            loadReplies(threadId, state.value.isPoOnlyMode)
        }
    }

    private fun loadReplies(threadId: Long, isPoOnly: Boolean, page: Int? = null) {
        val repliesFlow =
            getThreadRepliesPagingUseCase(threadId, isPoOnly, page).cachedIn(screenModelScope)
        _state.update { it.copy(replies = repliesFlow, currentPage = page ?: 1) }
    }

    private fun jumpToPage(page: Int) {
        loadReplies(threadId, state.value.isPoOnlyMode, page)
    }

    private fun togglePoOnlyMode() {
        val newPoOnlyMode = !state.value.isPoOnlyMode
        _state.update { it.copy(isPoOnlyMode = newPoOnlyMode) }
        loadReplies(threadId, newPoOnlyMode)
    }

    private fun toggleSubscription() {
        val currentSubscribed = state.value.isSubscribed

        screenModelScope.launch {
            toggleSubscriptionUseCase(threadId, currentSubscribed)
                .onSuccess { resultMessage ->
                    _state.update { it.copy(isSubscribed = !currentSubscribed) }
                    _effect.send(Effect.ShowSnackbar(resultMessage))
                }
                .onFailure { e ->
                    _effect.send(Effect.ShowSnackbar("操作失败: ${e.message}"))
                }
        }
    }

    private fun copyLink() {
        screenModelScope.launch {
            val url = "https://nmb.com/t/$threadId"
            _effect.send(Effect.CopyToClipboard(url))
            _effect.send(Effect.ShowSnackbar("链接已复制到剪贴板"))
        }
    }

    private fun showImagePreview(initialImgPath: String) {
        imageObserverJob?.cancel()
        imageObserverJob = screenModelScope.launch {
            // 立即发送导航事件
            _effect.send(Effect.NavigateToImagePreview)

            // 启动一个新的协程来监听图片数据的变化
            val threadFlow = db.threadQueries.getThread(threadId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.IO)

            val repliesFlow = db.threadReplyQueries.getThreadImages(threadId)
                .asFlow()
                .mapToList(Dispatchers.IO)

            combine(threadFlow, repliesFlow) { threadRow, replies ->
                val images = mutableListOf<ImageInfo>()
                if (threadRow != null && threadRow.img.isNotBlank()) {
                    images.add(ImageInfo(threadRow.img, threadRow.ext))
                }
                replies.forEach { reply ->
                    images.add(ImageInfo(reply.img, reply.ext))
                }
                images
            }.collectLatest { images ->
                val initialIndex = images.indexOfFirst { it.imgPath == initialImgPath }.coerceAtLeast(0)
                _state.update {
                    it.copy(
                        imagePreviewState = it.imagePreviewState.copy(
                            images = images,
                            initialIndex = initialIndex
                        )
                    )
                }
            }
        }
    }

    private fun loadMoreImages() {
        val imagePreviewState = state.value.imagePreviewState
        if (imagePreviewState.isLoading || imagePreviewState.endReached) return

        screenModelScope.launch {
            _state.update { it.copy(imagePreviewState = it.imagePreviewState.copy(isLoading = true)) }

            try {
                val maxPage = db.threadReplyQueries.getMaxPage(threadId).executeAsOne().MAX?.toLong() ?: 1L
                val nextPage = maxPage + 1

                when (val result = forumRepository.thread(threadId, nextPage)) {
                    is SaniouResponse.Success -> {
                        val threadDetail = result.data
                        if (threadDetail.replies.isEmpty()) {
                            _state.update { it.copy(imagePreviewState = it.imagePreviewState.copy(isLoading = false, endReached = true)) }
                        } else {
                            db.transaction {
                                db.threadQueries.upsetThread(threadDetail.toTable())
                                threadDetail.toTableReply(nextPage).forEach(db.threadReplyQueries::upsertThreadReply)
                            }
                            _state.update { it.copy(imagePreviewState = it.imagePreviewState.copy(isLoading = false)) }
                        }
                    }
                    is SaniouResponse.Error -> {
                        _state.update { it.copy(imagePreviewState = it.imagePreviewState.copy(isLoading = false, error = result.ex.message)) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(imagePreviewState = it.imagePreviewState.copy(isLoading = false, error = e.message)) }
            }
        }
    }
}
