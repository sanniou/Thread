package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.source.ProfileEditRequest
import ai.saniou.thread.domain.source.UserRelationProfile

interface UserRelationRepository {
    suspend fun getProfile(sourceId: String, userId: String): Result<UserRelationProfile>
    suspend fun follow(sourceId: String, userId: String): Result<String>
    suspend fun unfollow(sourceId: String, userId: String): Result<String>
    suspend fun updateProfile(sourceId: String, request: ProfileEditRequest): Result<String>
    suspend fun uploadPortrait(
        sourceId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream",
    ): Result<String>
}
