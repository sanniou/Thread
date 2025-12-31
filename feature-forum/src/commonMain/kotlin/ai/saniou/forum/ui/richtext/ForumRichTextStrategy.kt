package ai.saniou.forum.ui.richtext

import ai.saniou.coreui.richtext.RichTextPlugin
import ai.saniou.coreui.widgets.ClickablePattern

/**
 * Strategy for providing source-specific RichText plugins and link handling logic.
 */
interface ForumRichTextStrategy {
    /**
     * Get a list of plugins specific to this source.
     */
    fun getPlugins(
        onThreadClick: ((Long) -> Unit)?,
        onReferenceClick: ((Long) -> Unit)?
    ): List<RichTextPlugin>

    /**
     * Check if a URL should be handled internally (e.g. converted to a thread click).
     * Returns true if handled.
     */
    fun handleUrlClick(
        url: String,
        onThreadClick: ((Long) -> Unit)?
    ): Boolean
}

/**
 * Strategy for NMB (A岛) source.
 */
class NmbRichTextStrategy : ForumRichTextStrategy {
    override fun getPlugins(
        onThreadClick: ((Long) -> Unit)?,
        onReferenceClick: ((Long) -> Unit)?
    ): List<RichTextPlugin> {
        return listOf(
            object : RichTextPlugin {
                override fun getPatterns(): List<ClickablePattern> {
                    return listOf(
                        ClickablePattern(
                            tag = "REFERENCE",
                            regex = "(?:>>No\\.|>>|/t/)(\\d+)".toRegex(),
                            onClick = { refText ->
                                refText.toLongOrNull()?.let { id -> onReferenceClick?.invoke(id) }
                            }
                        )
                    )
                }
            }
        )
    }

    override fun handleUrlClick(
        url: String,
        onThreadClick: ((Long) -> Unit)?
    ): Boolean {
        // Try to match internal thread links like "/t/12345" or "https://.../t/12345"
        val threadIdMatch = Regex("/t/(\\d+)").find(url)
        val threadId = threadIdMatch?.groupValues?.getOrNull(1)?.toLongOrNull()

        if (threadId != null && onThreadClick != null) {
            onThreadClick(threadId)
            return true
        }
        return false
    }
}

/**
 * Factory to get the appropriate strategy for a source ID.
 */
object ForumRichTextStrategyFactory {
    fun getStrategy(sourceId: String?): ForumRichTextStrategy {
        return when (sourceId) {
            "nmb" -> NmbRichTextStrategy()
            else -> NmbRichTextStrategy() // Default to NMB for now, or could be a NoOpStrategy
        }
    }
}