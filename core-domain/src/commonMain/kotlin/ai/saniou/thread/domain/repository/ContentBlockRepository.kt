package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.block.ContentBlock
import kotlinx.coroutines.flow.Flow

interface ContentBlockRepository {
    fun observeBlocks(): Flow<List<ContentBlock>>
    suspend fun getBlocks(): List<ContentBlock>
    suspend fun addKeywordRule(keywords: List<String>): ContentBlock
    suspend fun addUserRule(userId: String?, userName: String?): ContentBlock
    suspend fun removeBlock(id: Long)
    suspend fun clearBlocks()
}
