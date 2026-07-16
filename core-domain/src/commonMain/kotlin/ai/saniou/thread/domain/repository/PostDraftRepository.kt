package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import kotlinx.coroutines.flow.Flow

interface PostDraftRepository {
    fun observeAll(): Flow<List<SavedPostDraft>>
    suspend fun get(key: PostDraftKey): SavedPostDraft?
    suspend fun save(draft: SavedPostDraft)
    suspend fun discard(key: PostDraftKey)
}
