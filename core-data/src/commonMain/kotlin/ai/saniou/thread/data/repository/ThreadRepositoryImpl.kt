package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.domain.model.Image
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.model.ThreadReply
import ai.saniou.thread.domain.repository.ThreadRepository
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import toTableThreadReply

class ThreadRepositoryImpl(
    private val db: Database,
    private val api: NmbXdApi,
) : ThreadRepository {

    override fun getThreadDetail(id: Long, forceRefresh: Boolean): Flow<Post> =
        db.threadQueries.getThread(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { it?.toDomain() }
            .onStart {
                val needsFetch =
                    forceRefresh || db.threadQueries.getThread(id).executeAsOneOrNull() == null
                if (needsFetch) {
                    val result = api.thread(id, 1)
                    if (result is SaniouResponse.Success) {
                        val threadDetail = result.data
                        db.transaction {
                            db.threadQueries.upsertThread(threadDetail.toTable(1))
                            threadDetail.toTableThreadReply(1)
                                .forEach(db.threadReplyQueries::upsertThreadReply)
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    override fun getThreadRepliesPaging(
        threadId: Long,
        isPoOnly: Boolean,
        initialPage: Int
    ): Flow<PagingData<ThreadReply>> {
        val poUserHash = if (isPoOnly) {
            db.threadQueries.getThread(threadId).executeAsOneOrNull()?.userHash
        } else {
            null
        }
        return Pager(
            config = PagingConfig(
                pageSize = 19,
                enablePlaceholders = false,
                initialLoadSize = 19
            ),
            initialKey = initialPage,
            pagingSourceFactory = {
                ThreadRepliesPagingSource(
                    api = api,
                    db = db,
                    threadId = threadId,
                    poUserHash = poUserHash
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getThreadImages(threadId: Long): Flow<List<Image>> =
        db.threadReplyQueries.getThreadImages(threadId)
            .asFlow()
            .map { query ->
                query.executeAsList().map {
                    Image(
                        name = it.img,
                        ext = it.ext
                    )
                }
            }
            .flowOn(Dispatchers.IO)
}
