package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.LinkResolution

interface ContentLinkRepository {
    suspend fun resolveUrl(url: String): LinkResolution
    suspend fun resolveReference(reference: ContentReference): LinkResolution
}
