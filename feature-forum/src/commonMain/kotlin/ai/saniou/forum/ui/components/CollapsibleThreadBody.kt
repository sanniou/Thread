package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun CollapsibleThreadBody(
    content: String,
    images: List<Image> = emptyList(),
    collapsedLines: Int = 7,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (Image) -> Unit,
    onImageLongClick: ((Image) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(content) { mutableStateOf(false) }
    val shouldCollapse = content.lineSequence().count() > collapsedLines || content.length > 220

    Column(modifier = modifier) {
        ForumRichText(
            text = content,
            maxLines = if (expanded || !shouldCollapse) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            onThreadClick = onReferenceClick,
            onReferenceClick = onReferenceClick
        )

        if (shouldCollapse) {
            Spacer(modifier = Modifier.height(Dimens.padding_tiny))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = Dimens.padding_small,
                        vertical = Dimens.padding_tiny
                    )
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (expanded) "收起" else "展开全文",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
            ForumImageGrid(
                images = images,
                onImageClick = onImageClick,
                onImageLongClick = onImageLongClick
            )
        }
    }
}
