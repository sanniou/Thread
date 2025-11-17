package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toThreadReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database

class GetReferenceUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    suspend operator fun invoke(id: Long): Result<ThreadReply> {
        // 尝试从数据库获取
        val localReply = db.threadReplyQueries.getThreadReplyById(id).executeAsOneOrNull()?.toThreadReply()
        if (localReply != null) {
            return Result.success(localReply)
        }

        // 如果数据库没有，则从网络获取
        return when (val refResponse = forumRepository.ref(id)) {
            is SaniouResponse.Success -> {
                val reply = ThreadReply(
                    id = refResponse.data.id,
                    userHash = refResponse.data.userHash,
                    admin = 0,
                    title = refResponse.data.title,
                    now = refResponse.data.now,
                    content = refResponse.data.content,
                    img = refResponse.data.img,
                    ext = refResponse.data.ext,
                    name = refResponse.data.name,
                )
                // 存入数据库，注意 threadId 设为特殊值以作区分
                db.threadReplyQueries.upsertThreadReply(reply.toTable(Long.MIN_VALUE))
                Result.success(reply)
            }
//            is SaniouResponse.Failure -> Result.failure(Exception(refResponse.message))
            is SaniouResponse.Error -> Result.failure(refResponse.ex)
        }
    }
}
