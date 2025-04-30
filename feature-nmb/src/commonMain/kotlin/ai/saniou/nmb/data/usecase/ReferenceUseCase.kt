package ai.saniou.nmb.data.usecase

import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.repository.NmbRepository

/**
 * 引用UseCase
 * 
 * 用于获取引用的回复内容
 */
class ReferenceUseCase(
    private val nmbRepository: NmbRepository
) {
    
    /**
     * 获取引用的回复内容
     */
    suspend fun getReference(refId: Long): Reply? {
        return nmbRepository.getReference(refId)
    }
}
