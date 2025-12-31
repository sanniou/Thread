package ai.saniou.feature.feed.ui.richtext

import ai.saniou.coreui.richtext.RichTextPlugin
import ai.saniou.coreui.widgets.ClickablePattern

/**
 * Matches #hashtags
 */
class HashtagPlugin(
    private val onHashtagClick: (String) -> Unit
) : RichTextPlugin {
    override fun getPatterns(): List<ClickablePattern> {
        return listOf(
            ClickablePattern(
                tag = "HASHTAG",
                regex = "#(\\w+)".toRegex(),
                onClick = { match ->
                    // match includes the #, e.g. "#kotlin"
                    onHashtagClick(match)
                }
            )
        )
    }
}

/**
 * Matches @mentions
 */
class MentionPlugin(
    private val onMentionClick: (String) -> Unit
) : RichTextPlugin {
    override fun getPatterns(): List<ClickablePattern> {
        return listOf(
            ClickablePattern(
                tag = "MENTION",
                regex = "@(\\w+)".toRegex(),
                onClick = { match ->
                    // match includes the @, e.g. "@saniou"
                    onMentionClick(match)
                }
            )
        )
    }
}