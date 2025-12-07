package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ClickablePattern
import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.data.source.nmb.remote.dto.IThreadBody
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ThreadBody(
    body: IThreadBody,
    maxLines: Int = Int.MAX_VALUE,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (String, String) -> Unit,
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
        RichText(
            text = body.content,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            clickablePatterns = clickablePatterns,
            blankLinePolicy = BlankLinePolicy.COLLAPSE
        )

        if (body.img.isNotEmpty() && body.ext.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            NmbImage(
                imgPath = body.img,
                ext = body.ext,
                isThumb = true,
                contentDescription = "帖子图片",
                modifier = Modifier
                    .height(Dimens.image_height_medium)
                    .wrapContentWidth(Alignment.Start)
                    .clickable { onImageClick(body.img, body.ext) },
                contentScale = ContentScale.FillHeight,
            )
        }
    }
}
