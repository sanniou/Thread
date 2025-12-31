package ai.saniou.coreui.richtext.plugins

import ai.saniou.coreui.richtext.RichTextPlugin
import ai.saniou.coreui.widgets.ClickablePattern

/**
 * Detects HTTP/HTTPS URLs and makes them clickable.
 *
 * @param onUrlClick Callback when a URL is clicked.
 */
class UrlPlugin(
    private val onUrlClick: (String) -> Unit
) : RichTextPlugin {
    override fun getPatterns(): List<ClickablePattern> {
        return listOf(
            ClickablePattern(
                tag = "URL_CUSTOM",
                regex = "(?:https?://|www\\.)[\\w\\-./?#&=%]+".toRegex(RegexOption.IGNORE_CASE),
                onClick = onUrlClick
            )
        )
    }
}