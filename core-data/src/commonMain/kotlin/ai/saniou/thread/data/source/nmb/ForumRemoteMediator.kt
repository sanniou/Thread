package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.network.SaniouResponse
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult

@OptIn(ExperimentalPagingApi::class)
class ForumRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val fetcher: suspend (page: Int) -> SaniouResponse<List<Forum>>,
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    private val delegate = GenericRemoteMediator<Int, GetThreadsInForumOffset, List<Forum>>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = initialPage,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.FORUM,
            id = fid,
            itemIdExtractor = { it.channelId }
        ),
        fetcher = { page -> fetcher(page) },
        saver = { forums, page, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.topicQueries.deleteTopicsByChannelAndPage(sourceId, fid, page.toLong())
            }
            forums.forEach { forum ->
                db.topicQueries.upsertTopic(forum.toTable(sourceId, page.toLong()))
                db.topicQueries.upsertTopicInformation(
                    id = forum.id.toString(),
                    sourceId = sourceId,
                    remainingCount = forum.remainingCount, // remainReplies -> remainingCount
                    lastKey = forum.replies.lastOrNull()?.id ?: forum.id
                )
                forum.toTableReply(sourceId)
                    .forEach(db.commentQueries::upsertComment)
            }
        },
        endOfPaginationReached = { it.isEmpty() },
        cacheChecker = { page ->
            val threadsInDb =
                db.topicQueries.countTopicsByChannelAndPage(sourceId, fid, page.toLong())
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
