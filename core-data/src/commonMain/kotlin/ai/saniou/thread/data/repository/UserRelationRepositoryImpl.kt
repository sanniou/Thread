package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.repository.UserRelationRepository
import ai.saniou.thread.domain.source.ConnectorRegistry
import ai.saniou.thread.domain.source.UserRelationProfile

class UserRelationRepositoryImpl(
    private val registry: ConnectorRegistry,
) : UserRelationRepository {
    override suspend fun getProfile(sourceId: String, userId: String): Result<UserRelationProfile> {
        val connector = registry.userRelation(sourceId)
            ?: return Result.failure(UnsupportedOperationException("Source '$sourceId' does not support user follow"))
        return connector.getProfile(userId)
    }

    override suspend fun follow(sourceId: String, userId: String): Result<String> {
        val connector = registry.userRelation(sourceId)
            ?: return Result.failure(UnsupportedOperationException("Source '$sourceId' does not support user follow"))
        return connector.follow(userId)
    }

    override suspend fun unfollow(sourceId: String, userId: String): Result<String> {
        val connector = registry.userRelation(sourceId)
            ?: return Result.failure(UnsupportedOperationException("Source '$sourceId' does not support user follow"))
        return connector.unfollow(userId)
    }
}
