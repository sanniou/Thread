package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.repository.ForumRepository

class ThreadUseCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(
        id: Long, page: Long
    ): Thread {
        return try {
            return when (val threadResponse = forumRepository.thread(id, page)) {
                is SaniouResponse.Success -> threadResponse.data
                else -> throw RuntimeException("获取帖子失败")
            }
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("ThreadUseCase Exception ${e.message}")
            throw e
        }
    }

    suspend fun getThreadPo(
        id: Long, page: Long
    ): List<Thread> {
        return try {
            return when (val threadResponse = forumRepository.po(id, page)) {
                is SaniouResponse.Success -> threadResponse.data
                else -> throw RuntimeException("获取PO主帖子失败")
            }
        } catch (e: Exception) {
            // 可以添加日志记录
            throw e
        }
    }

    suspend fun getReference(id: Long) = try {
        when (val refResponse = forumRepository.ref(id)) {
            is SaniouResponse.Success -> refResponse.data
            else -> throw RuntimeException("获取引用失败")
        }
    } catch (e: Exception) {
        // 可以添加日志记录
        throw e
    }
}
