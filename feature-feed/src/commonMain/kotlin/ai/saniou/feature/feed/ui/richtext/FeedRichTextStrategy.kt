package ai.saniou.feature.feed.ui.richtext

import ai.saniou.coreui.richtext.RichTextPlugin

/**
 * Strategy for providing feed-specific RichText plugins.
 */
interface FeedRichTextStrategy {
    fun getPlugins(
        onHashtagClick: ((String) -> Unit)?,
        onMentionClick: ((String) -> Unit)?
    ): List<RichTextPlugin>
}

/**
 * Strategy for generic Social Feeds (Twitter/Mastodon style).
 */
class SocialFeedRichTextStrategy : FeedRichTextStrategy {
    override fun getPlugins(
        onHashtagClick: ((String) -> Unit)?,
        onMentionClick: ((String) -> Unit)?
    ): List<RichTextPlugin> {
        val plugins = mutableListOf<RichTextPlugin>()
        
        if (onHashtagClick != null) {
            plugins.add(HashtagPlugin(onHashtagClick))
        }
        
        if (onMentionClick != null) {
            plugins.add(MentionPlugin(onMentionClick))
        }
        
        return plugins
    }
}

object FeedRichTextStrategyFactory {
    fun getStrategy(sourceId: String?): FeedRichTextStrategy {
        // Currently we only have one strategy, but this allows future expansion for Weibo, etc.
        return SocialFeedRichTextStrategy()
    }
}