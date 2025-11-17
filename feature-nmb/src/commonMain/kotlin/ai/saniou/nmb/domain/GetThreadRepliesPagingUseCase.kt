package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.toThreadReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.SqlDelightPagingSource
import ai.saniou.nmb.data.source.ThreadRemoteMediator
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetThreadRepliesPagingUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        threadId: Long,
        po: Boolean,
        initialPage: Int? = null
    ): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20 * 2,
                maxSize = 20 * 5,
            ),
            remoteMediator = ThreadRemoteMediator(threadId, forumRepository, db, initialPage),
            pagingSourceFactory = {
                if (po) {
                    val poUserHash = db.threadQueries.getThread(threadId).executeAsOne().userHash
                    SqlDelightPagingSource(
                        countQueryProvider = {
                            db.threadReplyQueries.countThreadRepliesByUserHash(threadId, poUserHash)
                        },
                        transacter = db.threadReplyQueries,
                        context = Dispatchers.IO,
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getThreadRepliesByUserHash(
                                threadId = threadId,
                                userHash = poUserHash,
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                } else {
                    SqlDelightPagingSource(
                        countQueryProvider = {
                            db.threadReplyQueries.countThreadReplies(threadId)
                        },
                        transacter = db.threadReplyQueries,
                        context = Dispatchers.IO,
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getThreadReplies(
                                threadId = threadId,
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                }
            }
        ).flow.map { page -> page.map { reply -> reply.toThreadReply() } }
    }
}