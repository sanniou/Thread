package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.ForumDetail
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.thread.data.source.nmb.remote.dto.toForumDetail
import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class ForumUseCase(
    private val nmbRepository: NmbSource,
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

    fun getForumDetail(fid: Long): ForumDetail? =
        db.forumQueries.getForum(fid).executeAsOneOrNull()?.toForumDetail()
}
