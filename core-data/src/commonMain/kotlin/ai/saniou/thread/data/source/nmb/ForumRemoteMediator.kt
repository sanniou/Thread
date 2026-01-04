package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.network.SaniouResult
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
    private val cdnManager: CdnManager,
    private val fetcher: suspend (page: Int) -> SaniouResult<List<Forum>>,
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    private val delegate = GenericRemoteMediator<Int, GetThreadsInForumOffset, List<Forum>>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = initialPage,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.FORUM,
            id = fid,
        ),
        fetcher = { page -> fetcher(page) },
        saver = { forums, page, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.topicQueries.deleteTopicsByChannelAndPage(sourceId, fid, page.toLong())
            }
            forums.forEach { forum ->
                val topic = forum.toTable(sourceId, page.toLong())
                db.topicQueries.upsertTopic(topic)
                // Save Topic Image
                saveNmbImage(
                    db = db,
                    cdnManager = cdnManager,
                    sourceId = sourceId,
                    parentId = forum.id.toString(),
                    parentType = ImageType.Topic,
                    img = forum.img,
                    ext = forum.ext
                )

                db.topicQueries.upsertTopicInformation(
                    id = forum.id.toString(),
                    sourceId = sourceId,
                    remainingCount = forum.remainingCount,
                    latestCommentId = (forum.replies.lastOrNull()?.id ?: forum.id).toString()
                )

                forum.replies.forEach { reply ->
                    // Since forum.toTableReply maps internal replies, we should do it manually to access original reply object for images
                    // Or iterate forum.toTableReply for saving, but iterate forum.replies for images.
                    // Let's stick to using the list of replies from the forum object.
                    // Wait, Forum.toTableReply() maps `this.replies`.
                    // We can reuse the mapping logic if we iterate manually.

                    // Re-implementing reply saving loop to include image saving
                    db.commentQueries.upsertComment(
                        reply.toTableReply(
                            sourceId = sourceId,
                            threadId = forum.id,
                            page = Long.MIN_VALUE // As per original Forum.toTableReply logic
                        )
                    )

                    saveNmbImage(
                        db = db,
                        cdnManager = cdnManager,
                        sourceId = sourceId,
                        parentId = reply.id.toString(),
                        parentType = ImageType.Comment,
                        img = reply.img,
                        ext = reply.ext
                    )
                }
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
