package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.domain.model.bookmark.Tag
import ai.saniou.thread.domain.repository.TagRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl(private val db: Database) : TagRepository {
    override fun getAllTags(): Flow<List<Tag>> {
        return db.tagQueries.getAllTags()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }
}