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
    Column {
        if (content.isNotBlank()) {
            ForumRichText(
                text = content,
                maxLines = maxLines,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                ),
                // ThreadBody uses onReferenceClick for everything (thread links and ref links)
                // because it typically opens a preview sheet or navigates in a specific way.
                onThreadClick = onReferenceClick,
                onReferenceClick = onReferenceClick,
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
