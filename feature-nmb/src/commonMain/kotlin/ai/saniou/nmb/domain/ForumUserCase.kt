package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.data.repository.ForumRepository

class ForumUserCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(
        id: Long, page: Long
    ): List<ShowF> {
        return try {
            return when (val forumList = forumRepository.showf(id, page)) {
                is SaniouResponse.Success -> forumList.data
                else -> throw RuntimeException("")
            }
        } catch (e: Exception) {
//            MBLoggerKit.e("applyOrExitGroup Exception ${e.message}")
            throw e
        }
    }
}
