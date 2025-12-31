package ai.saniou.forum.ui.components

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ClickablePattern
import ai.saniou.coreui.widgets.RichText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * A wrapper around [RichText] that handles forum-specific link logic,
 * such as NMB thread links (/t/id) and references (>>id).
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
    extraClickablePatterns: List<ClickablePattern> = emptyList(),
) {
    val actualSourceId = sourceId ?: LocalForumSourceId.current
    val uriHandler = LocalUriHandler.current

    val handleLinkClick = remember(actualSourceId, onThreadClick, uriHandler) {
        { url: String ->
            var handled = false
            if (actualSourceId == "nmb") {
                // Try to match internal thread links like "/t/12345" or "https://.../t/12345"
                val threadIdMatch = Regex("/t/(\\d+)").find(url)
                val threadId = threadIdMatch?.groupValues?.getOrNull(1)?.toLongOrNull()

                if (threadId != null && onThreadClick != null) {
                    onThreadClick(threadId)
                    handled = true
                }
            }

            if (!handled) {
                val fullUrl =
                    if (url.startsWith("www.", ignoreCase = true)) "http://$url" else url
                uriHandler.openUri(fullUrl)
            }
        }
    }

    val clickablePatterns = remember(actualSourceId, onReferenceClick, handleLinkClick, extraClickablePatterns) {
        val patterns = mutableListOf<ClickablePattern>()
        
        // 1. Custom URL Pattern (High Priority)
        // Matches common URLs in plain text that are not wrapped in <a> tags
        patterns.add(
            ClickablePattern(
                tag = "URL_CUSTOM",
                regex = "(?:https?://|www\\.)[\\w\\-./?#&=%]+".toRegex(RegexOption.IGNORE_CASE),
                onClick = handleLinkClick
            )
        )

        // 2. NMB Reference Pattern (>>id, >>No.id, /t/id)
        if (actualSourceId == "nmb") {
            patterns.add(
                ClickablePattern(
                    tag = "REFERENCE",
                    regex = "(?:>>No\\.|>>|/t/)(\\d+)".toRegex(),
                    onClick = { refText ->
                        // Determine if it is a thread link (/t/) or a reference (>>)
                        // Actually, for NMB, /t/ usually means thread, >> means reference/reply.
                        // However, ReferenceSheet handles replies.
                        // If the text starts with /t/, it might be better handled as a thread link?
                        // But ThreadBody's original logic treated all of these as "ReferenceClick".
                        // We will follow the original ThreadBody logic: treat all numbers extracted here as ID passed to onReferenceClick.
                        // EXCEPT if we want /t/ to go to onThreadClick?
                        // Let's refine:
                        // The original ThreadBody logic passed EVERYTHING to onReferenceClick.
                        // But wait, /t/ is a thread. >> is a reference (post).
                        // Usually onReferenceClick opens a ReferenceSheet or jumps to a post.
                        // onThreadClick opens a new page.
                        
                        // Let's look at the match.
                        // If regex matched "/t/12345", group(1) is "12345".
                        // If we have onThreadClick, maybe we should use it for /t/?
                        // But the regex captures the ID only. We need the full match to decide.
                        
                        // For now, to maintain compatibility with ThreadBody's previous behavior (which was just "onReferenceClick"),
                        // we will pass it to onReferenceClick.
                        // Users of ForumRichText can decide what "Reference" means (e.g. show post preview).
                        // Note: ThreadBody's "onReferenceClick" usually shows a preview popup.
                        
                        // Wait, if it is /t/12345 in plain text, do we want a popup or a jump?
                        // If it's a thread link, usually jump.
                        // But ThreadBody.kt's previous logic was:
                        // regex = "(?:>>No\\.|>>|/t/)(\\d+)" -> onReferenceClick
                        
                        // So I will stick to that. 
                        // If the caller wants to distinguish, they can't with this simplified callback.
                        // But `handleLinkClick` handles explicit URLs (including /t/ inside href or plain url).
                        // This pattern is for "shorthand" references.
                        
                        refText.toLongOrNull()?.let { id -> onReferenceClick?.invoke(id) }
                    }
                )
            )
        }

        patterns.addAll(extraClickablePatterns)
        patterns
    }

    RichText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        clickablePatterns = clickablePatterns,
        onLinkClick = handleLinkClick,
        blankLinePolicy = blankLinePolicy
    )
}