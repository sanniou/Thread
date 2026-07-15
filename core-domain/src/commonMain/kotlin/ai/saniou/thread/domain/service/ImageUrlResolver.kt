package ai.saniou.thread.domain.service

import ai.saniou.thread.domain.model.forum.Image

/**
 * Resolves source-specific image paths without exposing connector infrastructure to UI code.
 */
interface ImageUrlResolver {
    suspend fun initialize(): Boolean

    fun resolveOriginal(image: Image): String
}
