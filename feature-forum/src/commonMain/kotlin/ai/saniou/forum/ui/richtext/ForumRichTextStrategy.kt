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

object DefaultForumRichTextStrategy : ForumRichTextStrategy {
    override fun getPlugins(
        onThreadClick: ((Long) -> Unit)?,
        onReferenceClick: ((Long) -> Unit)?,
    ): List<RichTextPlugin> = emptyList()

    override fun handleUrlClick(url: String, onThreadClick: ((Long) -> Unit)?): Boolean = false
}

/**
 * Factory to get the appropriate strategy for a source ID.
 */
object ForumRichTextStrategyFactory {
    private val strategies: Map<String, ForumRichTextStrategy> = mapOf(
        "nmb" to NmbRichTextStrategy(),
    )

    fun getStrategy(sourceId: String?): ForumRichTextStrategy {
        return sourceId?.let(strategies::get) ?: DefaultForumRichTextStrategy
    }
}
