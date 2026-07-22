package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.source.UserRelationProfile

interface UserRelationRepository {
    suspend fun getProfile(sourceId: String, userId: String): Result<UserRelationProfile>
    suspend fun follow(sourceId: String, userId: String): Result<String>
    suspend fun unfollow(sourceId: String, userId: String): Result<String>
}
