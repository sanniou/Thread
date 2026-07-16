package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.SavedPostDraft

interface PostDraftRepository {
    suspend fun get(key: PostDraftKey): SavedPostDraft?
    suspend fun save(draft: SavedPostDraft)
    suspend fun discard(key: PostDraftKey)
}
