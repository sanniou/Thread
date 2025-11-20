package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.db.Database
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class ForumUseCase(
    private val nmbRepository: NmbRepository,
    private val db: Database,
) {
    operator fun invoke(
        fid: Long,
        fgroup: Long,
        policy: DataPolicy,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadWithInformation>> {
        return if (fgroup == -1L) {
            nmbRepository.getTimelinePager(fid, policy, initialPage)
        } else {
            nmbRepository.getShowfPager(fid, policy, initialPage)
        }
    }

    fun getForumName(fid: Long): String =
        db.forumQueries.getForum(fid).executeAsOneOrNull()?.name ?: "未知版面"
}
