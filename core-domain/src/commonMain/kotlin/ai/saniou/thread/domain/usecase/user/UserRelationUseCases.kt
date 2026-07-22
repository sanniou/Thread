package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.repository.UserRelationRepository
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
