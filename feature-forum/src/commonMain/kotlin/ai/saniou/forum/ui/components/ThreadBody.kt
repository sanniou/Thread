package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ClickablePattern
import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.data.source.nmb.remote.dto.IThreadBody
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onImageLongClick: ((String, String) -> Unit)? = null,
) {
    ThreadBody(
        content = body.content,
        img = body.img,
        ext = body.ext,
        maxLines = maxLines,
        onReferenceClick = onReferenceClick,
        onImageClick = onImageClick,
        onImageLongClick = onImageLongClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadBody(
    content: String,
    img: String?,
    ext: String?,
    maxLines: Int = Int.MAX_VALUE,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (String, String) -> Unit,
    onImageLongClick: ((String, String) -> Unit)? = null,
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
            text = content,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            clickablePatterns = clickablePatterns,
            blankLinePolicy = BlankLinePolicy.COLLAPSE
        )

        if (!(img.isNullOrEmpty()) && !(ext.isNullOrEmpty())) {
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            Box {
                var showImageMenu by remember { mutableStateOf(false) }
                NmbImage(
                    imgPath = img,
                    ext = ext,
                    isThumb = true,
                    contentDescription = "帖子图片",
                    modifier = Modifier
                        .height(Dimens.image_height_medium)
                        .wrapContentWidth(Alignment.Start)
                        .combinedClickable(
                            onClick = { onImageClick(img, ext) },
                            onLongClick = {
                                if (onImageLongClick != null) {
                                    onImageLongClick(img, ext)
                                } else {
                                    showImageMenu = true
                                }
                            }
                        ),
                    contentScale = ContentScale.FillHeight,
                )
                if (onImageLongClick == null) { // Only show internal menu if no external handler provided
                    // Ideally we should pass the bookmark action into ThreadBody, but for now let's rely on external handler
                }
            }
        }
    }
}
