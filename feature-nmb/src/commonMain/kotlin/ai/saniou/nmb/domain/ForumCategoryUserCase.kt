package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.repository.ForumRepository

class ForumCategoryUserCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(): List<ForumCategory> {
        return try {
            return when (val forumList = forumRepository.getForumList()) {
                is SaniouResponse.Success -> forumList.data
                else -> throw RuntimeException("")
            }
        } catch (e: Exception) {
//            MBLoggerKit.e("applyOrExitGroup Exception ${e.message}")
            throw e
        }
    }
}
