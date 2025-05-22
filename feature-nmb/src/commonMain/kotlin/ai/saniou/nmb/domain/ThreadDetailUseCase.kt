package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toThread
import ai.saniou.nmb.data.entity.toThreadReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.ThreadRemoteMediator
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.toTable
import androidx.paging.ExperimentalPagingApi
import androidx.paging.map
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThreadDetailUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        threadId: Long,
        po: Boolean,
    ): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20 * 2,
                maxSize = 20 * 5,
            ),
            remoteMediator = ThreadRemoteMediator(threadId, po, forumRepository, db),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.threadReplyQueries.countThreadReplies(threadId),
                    transacter = db.threadReplyQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.threadReplyQueries.queryThreadRepliesByIndex(
                            threadId = threadId,
                            start = offset,
                            end = offset + limit,
                        )
                    }
                )
            }
        ).flow.map { page -> page.map { reply -> reply.toThreadReply() } }
    }

    fun getThread(
        id: Long
    ) = db.threadQueries.getThread(id).executeAsOne().toThread()

    suspend fun getThreadPo(
        id: Long, page: Long
    ): Thread {
        return when (val threadResponse = forumRepository.po(id, page)) {
            is SaniouResponse.Success -> threadResponse.data
            else -> throw RuntimeException("获取PO主帖子失败")
        }
    }

    suspend fun getReference(id: Long): ThreadReply =
        db.threadReplyQueries.getThreadReplyById(id).executeAsOneOrNull()?.toThreadReply() ?: let {
            when (val refResponse = forumRepository.ref(id)) {
                is SaniouResponse.Success -> refResponse.data.let {
                    ThreadReply(
                        id = it.id,
                        userHash = it.userHash,
                        admin = 0,
                        title = it.title,
                        now = it.now,
                        content = it.content,
                        img = it.img,
                        ext = it.ext,
                        name = it.name,
                    )
                }.also {
                    db.threadReplyQueries.insertThreadReply(it.toTable(Long.MIN_VALUE))
                }

                else -> throw RuntimeException("获取引用失败")
            }
        }
}
