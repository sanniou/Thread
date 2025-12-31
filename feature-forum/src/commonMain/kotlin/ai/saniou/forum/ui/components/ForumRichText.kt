package ai.saniou.forum.ui.components

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.richtext.RichTextPlugin
import ai.saniou.coreui.richtext.SmartRichText
import ai.saniou.coreui.richtext.plugins.UrlPlugin
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.forum.ui.richtext.ForumRichTextStrategyFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * A wrapper around [SmartRichText] that handles forum-specific link logic,
 * such as NMB thread links (/t/id) and references (>>id), using the Strategy pattern.
 *
 * @param sourceId The source ID of the current forum (e.g., "nmb"). If null, uses [LocalForumSourceId].
 * @param onThreadClick Callback when a thread link (/t/id) is clicked.
 * @param onReferenceClick Callback when a reference (>>id) is clicked.
 */
@Composable
fun ForumRichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    blankLinePolicy: BlankLinePolicy = BlankLinePolicy.KEEP,
    sourceId: String? = null,
    onThreadClick: ((Long) -> Unit)? = null,
    onReferenceClick: ((Long) -> Unit)? = null,
) {
    val actualSourceId = sourceId ?: LocalForumSourceId.current
    val uriHandler = LocalUriHandler.current

    val strategy = remember(actualSourceId) {
        ForumRichTextStrategyFactory.getStrategy(actualSourceId)
    }

    val handleLinkClick = remember(strategy, onThreadClick, uriHandler) {
        { url: String ->
            if (!strategy.handleUrlClick(url, onThreadClick)) {
                val fullUrl =
                    if (url.startsWith("www.", ignoreCase = true)) "http://$url" else url
                uriHandler.openUri(fullUrl)
            }
        }
    }

    val plugins = remember(strategy, onThreadClick, onReferenceClick, handleLinkClick) {
        val list = mutableListOf<RichTextPlugin>(
            UrlPlugin(onUrlClick = handleLinkClick)
        )
        list.addAll(strategy.getPlugins(onThreadClick, onReferenceClick))
        list
    }

    SmartRichText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        plugins = plugins,
        onLinkClick = handleLinkClick,
        blankLinePolicy = blankLinePolicy
    )
}
