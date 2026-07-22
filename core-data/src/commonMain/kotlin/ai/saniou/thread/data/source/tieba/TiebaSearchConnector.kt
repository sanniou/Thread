package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.SearchThreadBean
import ai.saniou.thread.data.source.tieba.remote.AppHybridTiebaApi
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.source.ForumSearchConnector
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Hybrid search endpoints from TiebaLite AppHybridTiebaApi.
 * Maps posts → Topic (THREAD tab) and floor-level hits → Comment (REPLY tab).
 */
class TiebaSearchConnector(
    private val source: TiebaSource,
    private val api: AppHybridTiebaApi,
) : ForumSearchConnector {
    override val sourceId: String = source.id

    override fun searchTopics(query: String): Flow<PagingData<Topic>> = pager { page ->
        val response = api.searchThread(
            keyword = query,
            page = page,
            sort = SORT_RELEVANCE,
            pageSize = PAGE_SIZE,
            referer = hybridSearchReferer(query),
        )
        response.ensureOk()
        response.data.postList.map { it.toTopic() }
    }

    override fun searchComments(query: String): Flow<PagingData<Comment>> = pager { page ->
        val response = api.searchThread(
            keyword = query,
            page = page,
            sort = SORT_RELEVANCE,
            pageSize = PAGE_SIZE,
            referer = hybridSearchReferer(query),
        )
        response.ensureOk()
        response.data.postList.map { it.toComment() }
    }

    private fun <T : Any> pager(loadPage: suspend (Int) -> List<T>): Flow<PagingData<T>> = Pager(
        config = threadPagingConfig(PAGE_SIZE),
        pagingSourceFactory = { TiebaSearchPagingSource(loadPage) },
    ).flow

    private companion object {
        const val PAGE_SIZE = 20
        /** Tieba hybrid sort: 1 = relevance (matches TiebaLite default). */
        const val SORT_RELEVANCE = 1
    }
}

private class TiebaSearchPagingSource<T : Any>(
    private val loadPage: suspend (Int) -> List<T>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = try {
        val page = params.key ?: 1
        val data = loadPage(page)
        LoadResult.Page(
            data = data,
            prevKey = if (page > 1) page - 1 else null,
            nextKey = if (data.isEmpty()) null else page + 1,
        )
    } catch (error: Throwable) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
}

internal fun hybridSearchReferer(keyword: String): String {
    val raw =
        "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${Clock.System.now().toEpochMilliseconds()}"
    return raw.encodeURLQueryComponent()
}

internal fun SearchThreadBean.ensureOk() {
    if (errorCode != 0) {
        throw IllegalStateException(errorMsg.ifBlank { "贴吧搜索失败 ($errorCode)" })
    }
}

internal fun SearchThreadBean.ThreadInfoBean.toTopic(): Topic {
    val displayName = user.showNickname?.takeIf(String::isNotBlank)
        ?: user.userName?.takeIf(String::isNotBlank)
        ?: "贴吧用户"
    val body = content.takeIf(String::isNotBlank) ?: title
    return Topic(
        id = tid,
        channelId = forumId,
        channelName = forumName.ifBlank { forumInfo.forumName },
        title = title.takeIf(String::isNotBlank),
        content = body,
        summary = body.takeIf(String::isNotBlank),
        author = Author(
            id = user.userId,
            name = displayName,
            avatar = user.portrait?.takeIf(String::isNotBlank)?.let { portraitUrl(it) },
            sourceName = TiebaMapper.SOURCE_NAME,
        ),
        createdAt = parseSearchTime(time, modifiedTime),
        commentCount = postNum.toLongOrNull() ?: 0,
        agreeCount = likeNum.toLongOrNull(),
        sourceId = TiebaMapper.SOURCE_ID,
        sourceName = TiebaMapper.SOURCE_NAME,
        sourceUrl = "https://tieba.baidu.com/p/$tid",
    )
}

internal fun SearchThreadBean.ThreadInfoBean.toComment(): Comment {
    val displayName = user.showNickname?.takeIf(String::isNotBlank)
        ?: user.userName?.takeIf(String::isNotBlank)
        ?: "贴吧用户"
    val body = content.takeIf(String::isNotBlank) ?: title
    val commentId = pid.takeIf { it.isNotBlank() && it != "0" } ?: tid
    return Comment(
        id = commentId,
        topicId = tid,
        author = Author(
            id = user.userId,
            name = displayName,
            avatar = user.portrait?.takeIf(String::isNotBlank)?.let { portraitUrl(it) },
            sourceName = TiebaMapper.SOURCE_NAME,
        ),
        createdAt = parseSearchTime(time, modifiedTime),
        title = title.takeIf(String::isNotBlank),
        content = body,
        isAdmin = false,
        floor = 0,
        sourceId = TiebaMapper.SOURCE_ID,
        agreeCount = likeNum.toLongOrNull(),
    )
}

private fun portraitUrl(portrait: String): String =
    if (portrait.startsWith("http")) portrait
    else "https://tb.himg.baidu.com/sys/portrait/item/$portrait"

private fun parseSearchTime(time: String, modifiedTime: Long): Instant {
    time.toLongOrNull()?.let { return Instant.fromEpochSeconds(it) }
    if (modifiedTime > 0L) return Instant.fromEpochSeconds(modifiedTime)
    return Instant.fromEpochSeconds(0)
}
