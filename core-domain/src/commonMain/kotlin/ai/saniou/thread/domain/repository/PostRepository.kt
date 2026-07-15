package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.PostDraft

interface PostRepository {
    suspend fun post(
        fid: Int,
        draft: PostDraft,
    ): String

    suspend fun reply(
        resto: Int,
        draft: PostDraft,
    ): String
}
