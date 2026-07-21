package ai.saniou.thread.domain.usecase.social

import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialRefreshReport
import ai.saniou.thread.domain.model.social.SocialSourceDescriptor
import ai.saniou.thread.domain.repository.SocialRepository

class ObserveSocialSourcesUseCase(private val repository: SocialRepository) {
    operator fun invoke() = repository.observeSources()
}

class SaveSocialSourceUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(source: SocialSourceDescriptor, accessToken: String? = null) =
        repository.upsertSource(source, accessToken)
}

class RemoveSocialSourceUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(sourceId: String) = repository.removeSource(sourceId)
}

class LoadOlderSocialTimelineUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(sourceIds: Set<String>? = null): SocialRefreshReport =
        repository.refresh(sourceIds, CursorDirection.OLDER)
}

class InteractWithSocialPostUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(
        post: SocialPost,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost> = repository.interact(post.sourceId, post.id, interaction, enabled)
}

class GetSocialPostUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(sourceId: String, postId: String): SocialPost? =
        repository.getPost(sourceId, postId)
}
