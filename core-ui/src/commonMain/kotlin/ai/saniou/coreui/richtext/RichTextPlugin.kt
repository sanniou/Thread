package ai.saniou.coreui.richtext

import ai.saniou.coreui.widgets.ClickablePattern

/**
 * Interface for RichText plugins that can transform text and provide clickable patterns.
 */
interface RichTextPlugin {
    /**
     * Pre-process the text before parsing HTML.
     * Use this to convert custom tags (e.g. `[h]`) to HTML tags (e.g. `<spoiler>`)
     * or to perform cleanups.
     */
    fun transform(text: String): String = text

    /**
     * Provide a list of clickable patterns (regex + click handler).
     */
    fun getPatterns(): List<ClickablePattern> = emptyList()
}