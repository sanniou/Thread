package ai.saniou.forum.workflow.topicdetail

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Effect
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Event
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.State
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.block.ContentBlock
import ai.saniou.thread.domain.model.block.ContentBlockMatcher
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.usecase.block.ObserveContentBlocksUseCase
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.Tag
import ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.subscription.GetActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicCommentsPagerUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicDetailUseCase
import ai.saniou.thread.domain.usecase.thread.GetSubCommentsUseCase
import ai.saniou.thread.domain.service.ImageUrlResolver
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.usecase.thread.GetTopicMetadataUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastAccessTimeUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastReadCommentIdUseCase
import ai.saniou.thread.domain.usecase.post.UpvoteTopicUseCase
import ai.saniou.thread.domain.usecase.post.DownvoteTopicUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_0e055beabc
import thread.feature_forum.generated.resources.s_12a870cbdf
import thread.feature_forum.generated.resources.s_1ec0da44f8
import thread.feature_forum.generated.resources.s_2cb63ed15e
import thread.feature_forum.generated.resources.s_3395596aa5
import thread.feature_forum.generated.resources.s_3eb8dbcc7f
import thread.feature_forum.generated.resources.s_5efbdd9e46
import thread.feature_forum.generated.resources.s_64f538c2fa
import thread.feature_forum.generated.resources.s_b11cc4945e
import thread.feature_forum.generated.resources.downvote_failed
import thread.feature_forum.generated.resources.downvote_success
import thread.feature_forum.generated.resources.s_b8d684d1cb
import thread.feature_forum.generated.resources.s_beda0ebfaf

data class TopicDetailViewModelParams(
    val sourceId: String,
    val topicId: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TopicDetailViewModel(
    params: TopicDetailViewModelParams,
    private val getTopicMetadataUseCase: GetTopicMetadataUseCase,
    private val getTopicCommentsPagerUseCase: GetTopicCommentsPagerUseCase,
    private val getSubCommentsUseCase: GetSubCommentsUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val getActiveSubscriptionKeyUseCase: GetActiveSubscriptionKeyUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val getChannelNameUseCase: GetChannelNameUseCase,
    private val updateTopicLastAccessTimeUseCase: UpdateTopicLastAccessTimeUseCase,
    private val updateTopicLastReadCommentIdUseCase: UpdateTopicLastReadCommentIdUseCase,
    private val imageUrlResolver: ImageUrlResolver,
    private val upvoteTopicUseCase: UpvoteTopicUseCase,
    private val downvoteTopicUseCase: DownvoteTopicUseCase,
    observeContentBlocks: ObserveContentBlocksUseCase,
) : ScreenModel {

    private val sourceId = params.sourceId
    private val threadId = params.topicId

    private data class LoadRequest(
        val threadId: String,
        val isPoOnly: Boolean = false,
        val isReverse: Boolean = false,
        val page: Int = 1,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadRequest = MutableStateFlow(LoadRequest(threadId = threadId))
    private val contentBlocks = MutableStateFlow<List<ContentBlock>>(emptyList())

    val replies: Flow<PagingData<Comment>> =
        loadRequest.flatMapLatest { request ->
            getTopicCommentsPagerUseCase(
                sourceId = sourceId,
                topicId = request.threadId,
                isPoOnly = request.isPoOnly,
                isReverse = request.isReverse,
                startPage = request.page,
            )
        }.combine(contentBlocks) { paging, blocks ->
            if (blocks.isEmpty()) {
                paging
            } else {
                paging.filter { comment ->
                    !ContentBlockMatcher.shouldBlockContent(
                        text = listOfNotNull(comment.title, comment.content).joinToString("\n"),
                        userId = comment.author.id,
                        userName = comment.author.name,
                        rules = blocks,
                    )
                }
            }
        }.cachedIn(screenModelScope)

    init {
        observeTopicDetail()
        updateLastAccessTime()
        _state.update { it.copy(replies = replies) }

        screenModelScope.launch {
            observeContentBlocks().collect { contentBlocks.value = it }
        }
        screenModelScope.launch {
            observeSubscriptionStatus()
            loadRequest.collect { request ->
                _state.update {
                    it.copy(
                        isPoOnlyMode = request.isPoOnly,
                        isReverseOrder = request.isReverse,
                        currentPage = request.page
                    )
                }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.JumpToPage -> jumpToPage(event.page)
            Event.Refresh -> refresh()
            Event.TogglePoOnlyMode -> togglePoOnlyMode()
            Event.ToggleReverseOrder -> toggleReverseOrder()
            Event.LoadMoreSubComments -> loadMoreSubComments()
            Event.ToggleSubscription -> toggleSubscription()
            Event.CopyLink -> copyLink()
            is Event.CopyContent -> copyContent(event.content)
            is Event.BookmarkTopic -> bookmarkTopic()
            is Event.BookmarkReply -> bookmarkReply(event.reply)
            is Event.BookmarkImage -> bookmarkImage(event.image)
            is Event.UpvoteTopic -> upvoteTopic()
            is Event.DownvoteTopic -> downvoteTopic()
            is Event.UpdateLastReadReplyId -> updateLastReadReplyId(event.id)
            is Event.ShowSubComments -> showSubComments(event.commentId)
            Event.HideSubComments -> _state.update { it.copy(showSubCommentsDialog = false) }
            Event.RetryTopicLoad -> observeTopicDetail(forceRefresh = true)
            Event.RetrySubCommentsLoad -> state.value.activeCommentId?.let { showSubComments(it) }
        }
    }

    private fun jumpToPage(page: Int) {
        if (page < 1) return
        loadRequest.update { it.copy(page = page) }
    }

    private fun refresh() {
        loadRequest.update { it.copy(page = 1) }
        observeTopicDetail(forceRefresh = true)
    }

    private fun togglePoOnlyMode() {
        loadRequest.update { it.copy(isPoOnly = !it.isPoOnly, page = 1) }
    }

    private fun toggleReverseOrder() {
        loadRequest.update { it.copy(isReverse = !it.isReverse, page = 1) }
    }

    private fun showSubComments(commentId: String) {
        _state.update {
            it.copy(
                showSubCommentsDialog = true,
                activeCommentId = commentId,
                subCommentsPage = 1,
                subCommentsHasMore = false,
                isLoadingMoreSubComments = false,
                subCommentsWrapper = UiStateWrapper.Loading
            )
        }
        screenModelScope.launch {
            getSubCommentsUseCase(sourceId, threadId, commentId, 1)
                .onSuccess { comments ->
                    val filtered = filterBlockedComments(comments)
                    _state.update {
                        it.copy(
                            subCommentsWrapper = UiStateWrapper.Success(filtered),
                            subCommentsPage = 1,
                            // pbFloor 一页通常 10 条；有数据则允许尝试下一页
                            subCommentsHasMore = comments.size >= 10,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(subCommentsWrapper = UiStateWrapper.Error(e.toAppError {
                            showSubComments(commentId)
                        }))
                    }
                }
        }
    }

    private fun loadMoreSubComments() {
        val commentId = state.value.activeCommentId ?: return
        if (state.value.isLoadingMoreSubComments || !state.value.subCommentsHasMore) return
        val current = (state.value.subCommentsWrapper as? UiStateWrapper.Success)?.value.orEmpty()
        val nextPage = state.value.subCommentsPage + 1
        _state.update { it.copy(isLoadingMoreSubComments = true) }
        screenModelScope.launch {
            getSubCommentsUseCase(sourceId, threadId, commentId, nextPage)
                .onSuccess { more ->
                    val filteredMore = filterBlockedComments(more)
                    _state.update {
                        it.copy(
                            isLoadingMoreSubComments = false,
                            subCommentsPage = nextPage,
                            subCommentsHasMore = more.size >= 10,
                            subCommentsWrapper = UiStateWrapper.Success(current + filteredMore),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMoreSubComments = false) }
                    _effect.send(Effect.ShowSnackbar(e.message ?: "load more failed"))
                }
        }
    }

    private fun observeTopicDetail(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.update { it.copy(topicWrapper = UiStateWrapper.Loading) }
            getTopicMetadataUseCase(sourceId, threadId, forceRefresh)
                .catch { e ->
                    _state.update {
                        it.copy(topicWrapper = UiStateWrapper.Error(e.toAppError {
                            observeTopicDetail(forceRefresh = true)
                        }))
                    }
                }
                .collectLatest { metadata ->
                    observeChannelName(metadata.channelId)
                    _state.update {
                        it.copy(
                            topicWrapper = UiStateWrapper.Success(metadata),
                            totalPages = metadata.totalPages ?: 1
                        )
                    }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun updateLastAccessTime() {
        val tid = threadId.toLongOrNull() ?: return
        screenModelScope.launch {
            try {
                updateTopicLastAccessTimeUseCase(
                    sourceId,
                    tid.toString(),
                    Clock.System.now().toEpochMilliseconds()
                )
            } catch (e: Exception) {
                // Ignore errors during background update
            }
        }
    }

    private fun updateLastReadReplyId(replyId: String) {
        val tid = threadId.toLongOrNull() ?: return
        screenModelScope.launch {
            updateTopicLastReadCommentIdUseCase(sourceId, tid.toString(), replyId)
        }
    }

    private fun observeChannelName(channelId: String) {
        screenModelScope.launch {
            getChannelNameUseCase(sourceId, channelId).collect { forumName ->
                _state.update { it.copy(forumName = forumName ?: "") }
            }
        }
    }

    private fun observeSubscriptionStatus() {
        screenModelScope.launch {
            val subscriptionKey = getActiveSubscriptionKeyUseCase() ?: return@launch
            isSubscribedUseCase(subscriptionKey, threadId).collect { isSubscribed ->
                _state.update { it.copy(isSubscribed = isSubscribed) }
            }
        }
    }

    private fun toggleSubscription() {
        if (state.value.isTogglingSubscription) return
        val metadata = (state.value.topicWrapper as? UiStateWrapper.Success)?.value ?: return

        _state.update { it.copy(isTogglingSubscription = true) }

        val currentSubscribed = state.value.isSubscribed
        screenModelScope.launch {
            val subscriptionKey =
                getActiveSubscriptionKeyUseCase() ?: run {
                    _effect.send(Effect.ShowSnackbar(getString(Res.string.s_5efbdd9e46)))
                    _state.update { it.copy(isTogglingSubscription = false) }
                    return@launch
                }
            toggleSubscriptionUseCase(subscriptionKey, metadata.id, currentSubscribed)
                .onSuccess { resultMessage ->
                    // UI state will be updated by the database flow
                    _state.update { it.copy(isTogglingSubscription = false) }
                    _effect.send(Effect.ShowSnackbar(resultMessage))
                }
                .onFailure { e ->
                    _state.update { it.copy(isTogglingSubscription = false) }
                    _effect.send(Effect.ShowSnackbar(getString(Res.string.s_0e055beabc, e.message.orEmpty())))
                }
        }
    }

    private fun copyLink() {
        screenModelScope.launch {
            val metadata = (state.value.topicWrapper as? UiStateWrapper.Success)?.value
            val url = metadata?.sourceUrl.orEmpty()
            if (url.isBlank()) {
                _effect.send(Effect.ShowSnackbar(getString(Res.string.s_beda0ebfaf)))
                return@launch
            }
            _effect.send(Effect.CopyToClipboard(url))
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_1ec0da44f8)))
        }
    }

    private fun copyContent(content: String) {
        screenModelScope.launch {
            _effect.send(Effect.CopyToClipboard(content))
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_b8d684d1cb)))
        }
    }

    private fun bookmarkTopic() {
        screenModelScope.launch {
            val metadata = (state.value.topicWrapper as? UiStateWrapper.Success)?.value
            if (metadata == null) {
                _effect.send(Effect.ShowSnackbar(getString(Res.string.s_2cb63ed15e)))
                return@launch
            }

            addBookmarkUseCase(
                Bookmark.Quote(
                    id = "$sourceId.Topic.${metadata.id}",
                    createdAt = Clock.System.now(),
                    tags = metadata.tags,
                    content = metadata.title
                        ?: "", // Metadata doesn't have content, use title or empty
                    sourceId = metadata.id,
                    sourceType = "$sourceId.Topic"
                )
            )
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_12a870cbdf)))
        }
    }

    private fun bookmarkReply(reply: Comment) {
        screenModelScope.launch {
            addBookmarkUseCase(
                Bookmark.Quote(
                    id = "$sourceId.Comment.${reply.id}",
                    createdAt = Clock.System.now(),
                    tags = listOf(),
                    content = reply.content,
                    sourceId = reply.id,
                    sourceType = "$sourceId.Comment"
                )
            )
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_3395596aa5)))
        }
    }

    private fun bookmarkImage(image: Image) {
        screenModelScope.launch {
            val fullUrl = imageUrlResolver.resolveOriginal(image)
            val id = "$sourceId.Image.${fullUrl.hashCode()}"
            addBookmarkUseCase(
                Bookmark.Image(
                    id = id,
                    createdAt = Clock.System.now(),
                    tags = listOf(),
                    url = fullUrl,
                    width = null,
                    height = null
                )
            )
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_3eb8dbcc7f)))
        }
    }

    private fun upvoteTopic() {
        if (state.value.isReacting) return
        screenModelScope.launch {
            _state.update { it.copy(isReacting = true) }
            upvoteTopicUseCase(sourceId, threadId)
                .onSuccess {
                    _effect.send(Effect.ShowSnackbar(getString(Res.string.s_64f538c2fa)))
                    observeTopicDetail(forceRefresh = true)
                }
                .onFailure { error ->
                    _effect.send(Effect.ShowSnackbar(error.message ?: getString(Res.string.s_b11cc4945e)))
                }
            _state.update { it.copy(isReacting = false) }
        }
    }

    private fun downvoteTopic() {
        if (state.value.isReacting) return
        screenModelScope.launch {
            _state.update { it.copy(isReacting = true) }
            downvoteTopicUseCase(sourceId, threadId)
                .onSuccess {
                    _effect.send(Effect.ShowSnackbar(getString(Res.string.downvote_success)))
                    observeTopicDetail(forceRefresh = true)
                }
                .onFailure { error ->
                    _effect.send(Effect.ShowSnackbar(error.message ?: getString(Res.string.downvote_failed)))
                }
            _state.update { it.copy(isReacting = false) }
        }
    }
    private fun filterBlockedComments(comments: List<Comment>): List<Comment> {
        val blocks = contentBlocks.value
        if (blocks.isEmpty()) return comments
        return comments.filter { comment ->
            !ContentBlockMatcher.shouldBlockContent(
                text = listOfNotNull(comment.title, comment.content).joinToString("\n"),
                userId = comment.author.id,
                userName = comment.author.name,
                rules = blocks,
            )
        }
    }

}
