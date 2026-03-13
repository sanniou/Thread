package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    Column {
        if (content.isNotBlank()) {
            ForumRichText(
                text = content,
                maxLines = maxLines,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                onThreadClick = onReferenceClick,
                onReferenceClick = onReferenceClick,
                blankLinePolicy = BlankLinePolicy.COLLAPSE
            )
        }

        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
            ForumImageGrid(
                images = images,
                onImageClick = onImageClick,
                onImageLongClick = onImageLongClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
