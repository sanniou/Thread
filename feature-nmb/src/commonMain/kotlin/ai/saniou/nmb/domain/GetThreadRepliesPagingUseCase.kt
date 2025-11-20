package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.db.Database
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetThreadRepliesPagingUseCase(
    private val nmbRepository: NmbRepository,
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
