package ai.saniou.nmb.data.repository

import ai.saniou.nmb.data.entity.Reply

/**
 * NMB 仓库接口
 */
interface NmbRepository {
    
    /**
     * 获取引用的回复内容
     */
    suspend fun getReference(refId: Long): Reply?
}
