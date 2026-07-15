package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Account

interface LoginRepository {
    suspend fun login(sourceId: String, inputs: Map<String, String>): Account
}
