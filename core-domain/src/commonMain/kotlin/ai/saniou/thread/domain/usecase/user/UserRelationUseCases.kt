package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.repository.UserRelationRepository
import ai.saniou.thread.domain.source.ProfileEditRequest
import ai.saniou.thread.domain.source.UserRelationProfile

class GetUserRelationProfileUseCase(private val repository: UserRelationRepository) {
    suspend operator fun invoke(sourceId: String, userId: String): Result<UserRelationProfile> =
        repository.getProfile(sourceId, userId)
}

class FollowUserUseCase(private val repository: UserRelationRepository) {
    suspend operator fun invoke(sourceId: String, userId: String): Result<String> =
        repository.follow(sourceId, userId)
}

class UnfollowUserUseCase(private val repository: UserRelationRepository) {
    suspend operator fun invoke(sourceId: String, userId: String): Result<String> =
        repository.unfollow(sourceId, userId)
}

class UpdateUserProfileUseCase(private val repository: UserRelationRepository) {
    suspend operator fun invoke(sourceId: String, request: ProfileEditRequest): Result<String> =
        repository.updateProfile(sourceId, request)
}

class UploadUserPortraitUseCase(private val repository: UserRelationRepository) {
    suspend operator fun invoke(
        sourceId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream",
    ): Result<String> =
        repository.uploadPortrait(sourceId, fileName, bytes, contentType)
}
