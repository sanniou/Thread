package ai.saniou.coreui.richtext

import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.richtext.plugins.SpoilerPlugin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * A plugin-aware RichText component.
 *
 * @param plugins List of [RichTextPlugin] to apply.
 */
@Composable
fun SmartRichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    plugins: List<RichTextPlugin> = emptyList(),
    onLinkClick: ((String) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    blankLinePolicy: BlankLinePolicy = BlankLinePolicy.KEEP,
    color: Color = Color.Unspecified,
    spoilerBackgroundColor: Color = MaterialTheme.colorScheme.primary,
) {
    // Always include SpoilerPlugin by default for backward compatibility if desired,
    // or let the caller decide. To keep it clean, we'll just use the passed plugins.
    // However, the original RichText had [h] support hardcoded.
    // We should probably include SpoilerPlugin implicitly if we remove it from RichText.
    
    val allPlugins = remember(plugins) {
        // Ensure SpoilerPlugin is present if not already (for consistency with original behavior)
        // Or better yet, we just pass the plugins provided.
        // But since we are going to remove logic from RichText, we must add SpoilerPlugin here if we want to keep [h] support.
        if (plugins.none { it is SpoilerPlugin }) {
            plugins + SpoilerPlugin()
        } else {
            plugins
        }
    }

    val processedText = remember(text, allPlugins) {
        var result = text
        allPlugins.forEach { plugin ->
            result = plugin.transform(result)
        }
        result
    }

    val clickablePatterns = remember(allPlugins) {
        allPlugins.flatMap { it.getPatterns() }
    }

    RichText(
        text = processedText,
        modifier = modifier,
        style = style,
        clickablePatterns = clickablePatterns,
        onLinkClick = onLinkClick,
        overflow = overflow,
        maxLines = maxLines,
        blankLinePolicy = blankLinePolicy,
        color = color,
        spoilerBackgroundColor = spoilerBackgroundColor,
        // Disable internal [h] processing in RichText since we handle it here via plugins
        disableSpoilerProcessing = true
    )
}