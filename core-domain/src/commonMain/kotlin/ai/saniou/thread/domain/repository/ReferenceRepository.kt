package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.ThreadReply
import kotlinx.coroutines.flow.Flow

/**
 * 引用相关的仓库接口，定义了引用数据的契约
 */
interface ReferenceRepository {

    /**
     * 获取引用内容
     *
     * @param id 引用ID
     * @return 包含引用内容的 Flow
     */
    fun getReference(id: Long): Flow<ThreadReply>
}
