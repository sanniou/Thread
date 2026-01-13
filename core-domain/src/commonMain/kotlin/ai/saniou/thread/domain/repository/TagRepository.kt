package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
}
