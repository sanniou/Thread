package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetThreadRepliesPagingUseCase(
    private val nmbRepository: NmbSource,
    private val db: Database,
) {
    operator fun invoke(
        threadId: Long,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadReply>> {
        val poUserHash = if (isPoOnly) {
            db.threadQueries.getThread(threadId).executeAsOneOrNull()?.userHash
        } else {
            null
        }

        return nmbRepository.getThreadRepliesPager(
            threadId = threadId,
            poUserHash = poUserHash,
            policy = DataPolicy.CACHE_FIRST, // 帖子回复内容不变，总是优先使用缓存
            initialPage = initialPage
        )
    }
}
