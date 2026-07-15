package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.PostDraft

data class PostResult(
    val sourceId: String,
    val postId: String? = null,
    val topicId: String? = null,
    val message: String? = null,
)

interface PostRepository {
    suspend fun createThread(
        sourceId: String,
        channelId: String,
        draft: PostDraft,
    ): PostResult

    suspend fun createReply(
        sourceId: String,
        topicId: String,
        draft: PostDraft,
    ): PostResult
}
