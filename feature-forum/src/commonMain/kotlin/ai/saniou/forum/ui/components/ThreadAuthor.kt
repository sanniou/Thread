package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.thread.domain.model.forum.Author
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.label_anonymous
import kotlin.math.abs

@Composable
fun ThreadAuthor(
    author: Author,
    threadTime: String,
    isPo: Boolean = false,
    avatarUrl: String? = null,
    onClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    badges: @Composable (() -> Unit)? = null,
) {
    val anonymous = stringResource(Res.string.label_anonymous)
    val displayName = remember(author.name, author.id, anonymous) {
        author.name.takeIf { it.isNotBlank() && it != anonymous } ?: author.id.ifBlank { anonymous }
    }
    val monogram = remember(displayName) {
        displayName.trim().take(1).ifBlank { "?" }.uppercase()
    }
    val avatarColor = remember(author.id, displayName) {
        softAvatarColor(author.id.ifBlank { displayName })
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_medium),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.avatar_size_medium)
                .clip(CircleShape)
                .background(avatarColor)
                .then(
                    if (onClick != null) Modifier.clickable { onClick(author.id) } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                NetworkImage(
                    imageUrl = avatarUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onClick != null) Modifier.clickable { onClick(author.id) } else Modifier,
                        ),
                )
                if (isPo) {
                    PoTag(isPo = true)
                }
                badges?.invoke()
            }

            Text(
                text = threadTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun softAvatarColor(seed: String): Color {
    val hash = abs(seed.hashCode())
    // Quiet HSL-like palette: mid saturation, mid-high lightness.
    val hues = listOf(
        0xFF6366F1, // indigo
        0xFF0EA5E9, // sky
        0xFF14B8A6, // teal
        0xFF8B5CF6, // violet
        0xFFF59E0B, // amber
        0xFFEC4899, // pink muted
        0xFF22C55E, // green
        0xFF64748B, // slate
    )
    return Color(hues[hash % hues.size]).copy(alpha = 0.92f)
}
