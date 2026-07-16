package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.social.SocialCapabilities
import ai.saniou.thread.domain.model.social.SocialCursor
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialTimelinePage

interface SocialConnector : SourceConnector {
    val capabilities: SocialCapabilities

    suspend fun timeline(cursor: SocialCursor? = null): Result<SocialTimelinePage>

    suspend fun interact(postId: String, interaction: SocialInteraction, enabled: Boolean): Result<SocialPost>
}
