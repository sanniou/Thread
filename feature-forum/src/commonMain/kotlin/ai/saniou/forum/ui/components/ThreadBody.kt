package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ClickablePattern
import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadBody(
    content: String,
    images: List<Image> = emptyList(),
    maxLines: Int = Int.MAX_VALUE,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (Image) -> Unit,
    onImageLongClick: ((Image) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val clickablePatterns = remember(onReferenceClick) {
        listOf(
            ClickablePattern(
                tag = "REFERENCE",
                regex = ">>No\\.(\\d+)".toRegex(),
                onClick = { refId -> onReferenceClick?.invoke(refId.toLong()) }
            ),
            ClickablePattern(
                tag = "URL_CUSTOM",
                // A simple regex for URLs, including those without http(s) prefix
                regex = "(?:https?://|www\\.)[\\w\\-./?#&=%]+".toRegex(RegexOption.IGNORE_CASE),
                onClick = { url ->
                    val fullUrl =
                        if (url.startsWith("www.", ignoreCase = true)) "http://$url" else url
                    uriHandler.openUri(fullUrl)
                }
            )
        )
    }

    Column {
        if (content.isNotBlank()) {
            RichText(
                text = content,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                ),
                clickablePatterns = clickablePatterns,
                blankLinePolicy = BlankLinePolicy.COLLAPSE
            )
        }

        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
            NmbImageGrid(
                images = images,
                onImageClick = onImageClick,
                onImageLongClick = onImageLongClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
