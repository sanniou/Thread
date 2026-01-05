package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.discourse.remote.dto.DiscoursePost
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopicDetailResponse
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Comment
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult
import kotlin.time.Instant

@OptIn(ExperimentalPagingApi::class)
class DiscourseThreadRemoteMediator(
    private val sourceId: String,
    private val topicId: String,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val fetcher: suspend (page: Int) -> Result<DiscourseTopicDetailResponse>,
) : RemoteMediator<Int, Comment>() {

    private val delegate = GenericRemoteMediator<Int, Comment, DiscourseTopicDetailResponse>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = initialPage,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.THREAD,
            id = topicId,
        ),
        fetcher = { page -> fetcher(page) },
        saver = { topic, page, _ ->
            // Update Topic Content (cooked HTML) and reply count
            val firstPost = topic.postStream.posts.firstOrNull { it.postNumber == 1 }
            db.topicQueries.updateTopicContent(
                commentCount = topic.replyCount.toLong(),
                value = firstPost?.cooked,
                sourceId = sourceId,
                id = topic.id.toString()
            )

            topic.postStream.posts.forEach { post ->
                db.commentQueries.upsertComment(post.toComment(sourceId, topic.id.toString(), page))
            }
        },
        endOfPaginationReached = { topic ->
            topic.postStream.posts.isEmpty()
        },
        cacheChecker = { page ->
            val repliesInDb = db.commentQueries.countCommentsByTopicIdAndPage(
                sourceId,
                topicId,
                page.toLong()
            ).executeAsOne()
            repliesInDb > 0
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Comment>
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}

internal fun DiscoursePost.toComment(sourceId: String, threadId: String, page: Int): Comment {
    val replyCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    return Comment(
        id = id.toString(),
        sourceId = sourceId,
        topicId = threadId,
        page = page.toLong(),
        userHash = username,
        admin = 0, // TODO: Map admin status
        title = null,
        createdAt = replyCreatedAt.toEpochMilliseconds(),
        content = cooked,
        authorName = name ?: username,
        floor = postNumber.toLong(),
        replyToId = replyToPostNumber?.toString(),
    )
}
