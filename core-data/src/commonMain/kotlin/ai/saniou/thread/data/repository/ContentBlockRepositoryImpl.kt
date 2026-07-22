package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.table.ContentBlockEntity
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.block.ContentBlock
import ai.saniou.thread.domain.model.block.ContentBlockType
import ai.saniou.thread.domain.repository.ContentBlockRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ContentBlockRepositoryImpl(
    private val database: Database,
) : ContentBlockRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun observeBlocks(): Flow<List<ContentBlock>> =
        database.contentBlockQueries.getAllBlocks()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getBlocks(): List<ContentBlock> = withContext(ioDispatcher) {
        database.contentBlockQueries.getAllBlocks().executeAsList().map { it.toDomain() }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun addKeywordRule(keywords: List<String>): ContentBlock = withContext(ioDispatcher) {
        val cleaned = keywords.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        require(cleaned.isNotEmpty()) { "关键词不能为空" }
        val now = Clock.System.now().toEpochMilliseconds()
        database.contentBlockQueries.insertBlock(
            type = ContentBlockType.KEYWORD.name,
            keywords = json.encodeToString(cleaned),
            userId = null,
            userName = null,
            createdAt = now,
        )
        database.contentBlockQueries.getAllBlocks().executeAsList().first().toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun addUserRule(userId: String?, userName: String?): ContentBlock =
        withContext(ioDispatcher) {
            val id = userId?.trim()?.takeIf { it.isNotEmpty() }
            val name = userName?.trim()?.takeIf { it.isNotEmpty() }
            require(id != null || name != null) { "用户 ID 或用户名至少填一项" }
            val now = Clock.System.now().toEpochMilliseconds()
            database.contentBlockQueries.insertBlock(
                type = ContentBlockType.USER.name,
                keywords = "[]",
                userId = id,
                userName = name,
                createdAt = now,
            )
            database.contentBlockQueries.getAllBlocks().executeAsList().first().toDomain()
        }

    override suspend fun removeBlock(id: Long) = withContext(ioDispatcher) {
        database.contentBlockQueries.deleteBlock(id)
        Unit
    }

    override suspend fun clearBlocks() = withContext(ioDispatcher) {
        database.contentBlockQueries.clearBlocks()
        Unit
    }

    private fun ContentBlockEntity.toDomain(): ContentBlock {
        val parsedType = runCatching { ContentBlockType.valueOf(type) }.getOrDefault(ContentBlockType.KEYWORD)
        val kws = runCatching { json.decodeFromString<List<String>>(keywords) }.getOrDefault(emptyList())
        return ContentBlock(
            id = id,
            type = parsedType,
            keywords = kws,
            userId = userId,
            userName = userName,
            createdAt = createdAt,
        )
    }
}
