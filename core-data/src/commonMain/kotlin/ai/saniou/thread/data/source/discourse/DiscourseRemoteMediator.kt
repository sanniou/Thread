package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseLatestPostsResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.db.table.forum.ThreadReply
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult
import app.cash.paging.RemoteMediatorMediatorResultError
import app.cash.paging.RemoteMediatorMediatorResultSuccess
import kotlinx.datetime.Instant

@OptIn(ExperimentalPagingApi::class)
class DiscourseRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int = 0,
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    private val delegate =
        GenericRemoteMediator<Int, GetThreadsInForumOffset, DiscourseLatestPostsResponse>(
            db = db,
            dataPolicy = dataPolicy,
            initialKey = initialPage,
            remoteKeyStrategy = DefaultRemoteKeyStrategy(
                db = db,
                type = RemoteKeyType.FORUM,
                id = fid,
                itemIdExtractor = { it.fid.toString() }
            ),
            fetcher = { page ->
                try {
                    val response = if (fid == "latest") {
                        api.getLatestTopics(page)
                    } else {
                        api.getCategoryTopics(fid, page)
                    }
                    SaniouResponse.Success(response)
                } catch (e: Exception) {
                    throw e
                }
            },
            saver = { response, page, loadType ->
                val usersMap = response.users.associateBy { it.id }
                val topics = response.topicList.topics
                val threads = topics.map { topic ->
                    topic.toThreadEntity(sourceId, usersMap, page.toLong())
                }

                if (loadType == LoadType.REFRESH) {
                    // 注意：这里不能直接调用 suspend 函数，因为 saver 是在 db.transaction 中调用的
                    // 而 db.transaction 也是 suspend 函数，但是 GenericRemoteMediator 的 saver 参数不是 suspend
                    // 这是一个潜在的问题。
                    // 不过，GenericRemoteMediator 的 saver 是在 db.transaction 块内调用的。
                    // SQLDelight 的 transaction 块是同步执行的（在 JDBC 驱动上），但在协程驱动上可能是 suspend。
                    // 检查 GenericRemoteMediator 的定义，saver 是 (ResponseType, Key, LoadType) -> Unit
                    // 这意味着 saver 不能包含 suspend 调用。
                    // cache.clearForumCache 是 suspend 函数吗？
                    // 检查 SourceCache 接口定义：suspend fun clearForumCache(sourceId: String, fid: String)
                    // 是的，它是 suspend。
                    // 所以我们不能在这里直接调用 cache.clearForumCache。
                    // 我们需要直接操作数据库，或者修改 GenericRemoteMediator 以支持 suspend saver。
                    // 鉴于 GenericRemoteMediator 使用 db.transaction，它通常期望同步的数据库操作。
                    // 让我们直接使用 db.threadQueries 来删除数据，就像 ForumRemoteMediator 那样。
                    db.threadQueries.deleteThreadsByFidAndPage(sourceId, fid, page.toLong())
                }
                // cache.saveThreads 也是 suspend。
                // 我们需要直接使用 db.threadQueries。
                threads.forEach { thread ->
                    db.threadQueries.upsertThread(thread)
                    db.threadQueries.upsertThreadInformation(
                        id = thread.id,
                        sourceId = sourceId,
                        remainReplies = 0, // Discourse API doesn't seem to provide this directly in topic list
                        lastKey = 0 // Placeholder
                    )
                }
            },
            endOfPaginationReached = { response ->
                response.topicList.topics.isEmpty()
            },
            cacheChecker = { page ->
                val threadsInDb =
                    db.threadQueries.countThreadsByFidAndPage(sourceId, fid, page.toLong())
                        .executeAsOne()
                threadsInDb > 0
            },
            keyIncrementer = { it + 1 },
            keyDecrementer = { it - 1 },
            keyToLong = { it.toLong() },
            longToKey = { it.toInt() }
        )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForumOffset>,
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}

private fun DiscourseTopic.toThreadEntity(
    sourceId: String,
    usersMap: Map<Long, DiscourseUser>,
    page: Long,
): Thread {
    val originalPosterId =
        posters.firstOrNull { it.description.contains("Original Poster") }?.userId
            ?: posters.firstOrNull()?.userId
    val user = originalPosterId?.let { usersMap[it] }

    return Thread(
        id = id.toString(),
        sourceId = sourceId,
        fid = categoryId.toString(),
        replyCount = replyCount.toLong(),
        img = imageUrl ?: "",
        ext = "",
        now = createdAt,
        userHash = user?.username ?: "Unknown",
        name = user?.name ?: user?.username ?: "Anonymous",
        title = title,
        content = excerpt ?: fancyTitle,
        sage = 0,
        admin = if (pinned || closed) 1 else 0,
        hide = if (!visible) 1 else 0,
        page = page
    )
}
