package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.ThreadReply

interface ReferenceRepository {
    suspend fun getReference(id: Long): Result<ThreadReply>
}
