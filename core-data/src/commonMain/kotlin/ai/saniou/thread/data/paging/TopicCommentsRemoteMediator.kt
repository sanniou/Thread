package ai.saniou.thread.data.paging

import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.tieba.TiebaSource
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Comment
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import ai.saniou.thread.db.table.forum.Comment as EntityComment

@OptIn(ExperimentalPagingApi::class)
class TopicCommentsRemoteMediator(
    private val topicId: String,
    private val isPoOnly: Boolean,
    private val database: Database,
    private val tiebaSource: TiebaSource,
) : RemoteMediator<Int, EntityComment>() {

    private val genericRemoteMediator = GenericRemoteMediator<Int, EntityComment, List<Comment>>(
        db = database,
        dataPolicy = DataPolicy.NETWORK_ONLY, // Or CACHE_ELSE_NETWORK depending on requirement
        initialKey = 1,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = database,
            type = RemoteKeyType.THREAD, // Assuming THREAD type for topic comments
            id = "topic_comments_${topicId}_${if (isPoOnly) "po" else "all"}"
        ),
        fetcher = { page ->
            tiebaSource.getTopicComments(topicId, page, isPoOnly)
        },
        saver = { comments, page, _ ->
            if (comments.isNotEmpty()) {
                comments.map {
                    it.toEntity(
                        sourceId = tiebaSource.id,
                        page = page.toLong()
                    )
                }.forEach {
                    database.commentQueries.upsertComment(it)
                }
            }
        },
        endOfPaginationReached = { comments ->
            comments.isEmpty()
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EntityComment>,
    ): MediatorResult {
        return genericRemoteMediator.load(loadType, state)
    }
}
