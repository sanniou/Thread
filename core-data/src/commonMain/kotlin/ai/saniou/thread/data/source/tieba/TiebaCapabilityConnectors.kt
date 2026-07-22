package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.UserPostBean
import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.domain.source.UserContentConnector
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

class TiebaUserContentConnector(
    private val source: TiebaSource,
    private val api: MiniTiebaApi,
) : UserContentConnector {
    override val sourceId: String = source.id

    override fun getUserTopics(userId: String): Flow<PagingData<Topic>> = pager { page ->
        api.userPost(uid = userId, page = page, is_thread = 1).postsOrThrow()
            .map { it.toTopic() }
    }

    override fun getUserComments(userId: String): Flow<PagingData<Comment>> = pager { page ->
        api.userPost(uid = userId, page = page, is_thread = 0).postsOrThrow()
            .map { it.toComment() }
    }

    private fun <T : Any> pager(loadPage: suspend (Int) -> List<T>): Flow<PagingData<T>> = Pager(
        config = threadPagingConfig(PAGE_SIZE),
        pagingSourceFactory = { TiebaUserContentPagingSource(loadPage) },
    ).flow

    private companion object {
        const val PAGE_SIZE = 20
    }
}

class TiebaPostingConnector internal constructor(
    private val source: TiebaSource,
    private val api: OfficialTiebaApi,
    private val webApi: WebTiebaApi,
    private val database: Database,
    private val parameterProvider: TiebaParameterProvider,
    private val imageUploader: TiebaImageUploader,
) : PostingConnector {
    constructor(
        source: TiebaSource,
        api: OfficialTiebaApi,
        webApi: WebTiebaApi,
        database: Database,
        parameterProvider: TiebaParameterProvider,
    ) : this(
        source = source,
        api = api,
        webApi = webApi,
        database = database,
        parameterProvider = parameterProvider,
        imageUploader = TiebaImageUploader(
            client = TiebaPictureUploadClient { body ->
                api.uploadPicture(body = body, cookie = "ka=open")
            },
        ),
    )
    override val sourceId: String = source.id

    override suspend fun createThread(channelId: String, draft: PostDraft): PostResult {
        require(draft.content.isNotBlank() || draft.attachment != null || !draft.title.isNullOrBlank()) {
            "发帖标题、正文和图片不能同时为空"
        }
        val channel = database.channelQueries.getChannel(sourceId, channelId).executeAsOneOrNull()
            ?: throw IllegalStateException("本地未找到贴吧版块 $channelId，请先刷新关注吧列表")
        // Official JSON addPost has no separate title field; Tieba clients prepend title into content.
        val content = buildList {
            draft.title?.trim()?.takeIf(String::isNotBlank)?.let(::add)
            draft.content.trim().takeIf(String::isNotBlank)?.let(::add)
            draft.attachment?.let { attachment ->
                add(imageUploader.upload(attachment, channel.name).markup)
            }
        }.joinToString("\n")
        return submitAddPost(
            content = content,
            forumId = channelId,
            forumName = channel.name,
            threadId = "",
            nameShow = draft.name,
            failureLabel = "贴吧发主题失败",
        )
    }

    override suspend fun createReply(topicId: String, draft: PostDraft): PostResult {
        require(draft.content.isNotBlank() || draft.attachment != null) { "回复内容和图片不能同时为空" }

        val topic = database.topicQueries.getTopic(sourceId, topicId).executeAsOneOrNull()
            ?: throw IllegalStateException("本地未找到贴吧主题 $topicId，请先打开主题详情")
        val channel = database.channelQueries.getChannel(sourceId, topic.channelId).executeAsOneOrNull()
            ?: throw IllegalStateException("本地未找到贴吧版块 ${topic.channelId}")
        val content = buildList {
            draft.content.trim().takeIf(String::isNotBlank)?.let(::add)
            draft.attachment?.let { attachment ->
                add(imageUploader.upload(attachment, channel.name).markup)
            }
        }.joinToString("\n")
        return submitAddPost(
            content = content,
            forumId = topic.channelId,
            forumName = channel.name,
            threadId = topicId,
            nameShow = draft.name,
            failureLabel = "贴吧回复失败",
            fallbackTopicId = topicId,
        )
    }

    private suspend fun submitAddPost(
        content: String,
        forumId: String,
        forumName: String,
        threadId: String,
        nameShow: String?,
        failureLabel: String,
        fallbackTopicId: String? = null,
    ): PostResult {
        val tbs = ensureTbs()
        val response = api.addPost(
            content = content,
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nameShow = nameShow,
            sToken = parameterProvider.getSToken().takeIf(String::isNotBlank),
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
        )
        if (response.errorCode.isNotBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.msg.ifBlank { "$failureLabel (${response.errorCode})" })
        }
        return PostResult(
            sourceId = sourceId,
            postId = response.pid.takeIf(String::isNotBlank),
            topicId = response.tid.takeIf(String::isNotBlank) ?: fallbackTopicId,
            message = response.msg.takeIf(String::isNotBlank),
        )
    }

    private suspend fun ensureTbs(): String = parameterProvider.ensureTbs(webApi)
}

private class TiebaUserContentPagingSource<T : Any>(
    private val loadPage: suspend (Int) -> List<T>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = try {
        val page = params.key ?: 1
        val items = loadPage(page)
        LoadResult.Page(
            data = items,
            prevKey = if (page > 1) page - 1 else null,
            nextKey = if (items.isEmpty()) null else page + 1,
        )
    } catch (error: Throwable) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.anchorPosition?.let { anchor ->
        state.closestPageToPosition(anchor)?.let { page ->
            page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        }
    }
}

private fun UserPostBean.postsOrThrow(): List<UserPostBean.PostBean> {
    if (!errorCode.isNullOrBlank() && errorCode != "0") {
        throw IllegalStateException(errorMsg ?: "贴吧用户内容加载失败 ($errorCode)")
    }
    return postList.orEmpty()
}

internal fun UserPostBean.PostBean.toTopic(): Topic = Topic(
    id = threadId.orEmpty(),
    channelId = forumId.orEmpty(),
    channelName = forumName.orEmpty(),
    title = title?.takeIf(String::isNotBlank),
    content = textContent(),
    summary = textContent().takeIf(String::isNotBlank),
    author = toAuthor(),
    createdAt = createdInstant(),
    commentCount = replyNum?.toLongOrNull() ?: 0,
    agreeCount = agree?.agreeNum?.toLongOrNull(),
    disagreeCount = agree?.disagreeNum?.toLongOrNull(),
    sourceId = TiebaMapper.SOURCE_ID,
    sourceName = TiebaMapper.SOURCE_NAME,
    sourceUrl = "https://tieba.baidu.com/p/${threadId.orEmpty()}",
)

internal fun UserPostBean.PostBean.toComment(): Comment = Comment(
    id = postId.orEmpty(),
    topicId = threadId.orEmpty(),
    author = toAuthor(),
    createdAt = createdInstant(),
    title = title?.takeIf(String::isNotBlank),
    content = textContent(),
    isAdmin = false,
    floor = 0,
    sourceId = TiebaMapper.SOURCE_ID,
    agreeCount = agree?.agreeNum?.toLongOrNull(),
    disagreeCount = agree?.disagreeNum?.toLongOrNull(),
)

private fun UserPostBean.PostBean.toAuthor(): Author = Author(
    id = userId.orEmpty(),
    name = nameShow?.takeIf(String::isNotBlank) ?: userName.orEmpty().ifBlank { "贴吧用户" },
    avatar = userPortrait?.takeIf(String::isNotBlank)
        ?.let { "https://tb.himg.baidu.com/sys/portrait/item/$it" },
    sourceName = TiebaMapper.SOURCE_NAME,
)

private fun UserPostBean.PostBean.textContent(): String = buildList {
    abstracts.orEmpty().mapNotNullTo(this) { it.text?.takeIf(String::isNotBlank) }
    content.orEmpty().flatMap { it.postContent.orEmpty() }
        .mapNotNullTo(this) { it.text?.takeIf(String::isNotBlank) }
}.distinct().joinToString("\n")

private fun UserPostBean.PostBean.createdInstant(): Instant =
    Instant.fromEpochSeconds(createTime?.toLongOrNull() ?: 0)
