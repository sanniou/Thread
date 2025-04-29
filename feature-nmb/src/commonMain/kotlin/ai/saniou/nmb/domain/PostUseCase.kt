package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.PostReplyRequest
import ai.saniou.nmb.data.entity.PostThreadRequest
import ai.saniou.nmb.data.repository.ForumRepository

class PostUseCase(
    private val forumRepository: ForumRepository
) {
    /**
     * 发布新帖子
     */
    suspend fun postThread(request: PostThreadRequest): String {
        return try {
            forumRepository.postThread(request)
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("PostUseCase postThread Exception ${e.message}")
            throw e
        }
    }
    
    /**
     * 回复帖子
     */
    suspend fun postReply(request: PostReplyRequest): String {
        return try {
            forumRepository.postReply(request)
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("PostUseCase postReply Exception ${e.message}")
            throw e
        }
    }
}
