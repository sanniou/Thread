package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialRefreshReport
import ai.saniou.thread.domain.model.social.SocialSourceDescriptor
import ai.saniou.thread.domain.model.social.CursorDirection
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun observeSources(): Flow<List<SocialSourceDescriptor>>
    suspend fun getSources(): List<SocialSourceDescriptor>
    fun getTimeline(sourceIds: Set<String>? = null, query: String = ""): Flow<PagingData<SocialPost>>
    suspend fun getCachedPosts(
        sourceIds: Set<String>? = null,
        limit: Long = 30,
        offset: Long = 0,
    ): List<SocialPost>
    suspend fun getPost(sourceId: String, postId: String): SocialPost?
    suspend fun upsertSource(descriptor: SocialSourceDescriptor, accessToken: String? = null)
    suspend fun removeSource(sourceId: String)
    suspend fun refresh(
        sourceIds: Set<String>? = null,
        direction: CursorDirection = CursorDirection.NEWER,
    ): SocialRefreshReport
    suspend fun interact(
        sourceId: String,
        postId: String,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost>
}
