package ai.saniou.feature.feed.ui.components

import ai.saniou.coreui.richtext.SmartRichText
import ai.saniou.coreui.richtext.plugins.UrlPlugin
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.feature.feed.ui.richtext.FeedRichTextStrategyFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * A wrapper around [SmartRichText] that handles feed-specific link logic,
 * such as hashtags (#tag) and user mentions (@user).
 *
 * @param sourceId The source ID of the current feed.
 * @param onHashtagClick Callback when a hashtag is clicked.
 * @param onMentionClick Callback when a user mention is clicked.
 */
@Composable
fun FeedRichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    blankLinePolicy: BlankLinePolicy = BlankLinePolicy.KEEP,
    sourceId: String? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    val strategy = remember(sourceId) {
        FeedRichTextStrategyFactory.getStrategy(sourceId)
    }

    val plugins = remember(strategy, onHashtagClick, onMentionClick) {
        val list = mutableListOf(
            UrlPlugin(onUrlClick = { url -> 
                 val fullUrl = if (url.startsWith("www.", ignoreCase = true)) "http://$url" else url
                 uriHandler.openUri(fullUrl)
            })
        )
        list.addAll(strategy.getPlugins(onHashtagClick, onMentionClick))
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
        blankLinePolicy = blankLinePolicy
    )
}