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
import androidx.compose.ui.unit.dp

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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
    ) {
        // 头像占位，根据 Hash 生成颜色
        val avatarColor = remember(author.id) {
            val hash = author.id.hashCode()
            Color(hash).copy(alpha = 1f)
        }

        Box(
            modifier = Modifier
                .size(Dimens.avatar_size_medium)
                .clip(CircleShape)
                .background(avatarColor)
                .then(
                    if (onClick != null) Modifier.clickable { onClick(author.id) } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                NetworkImage(
                    imageUrl = avatarUrl,
                    contentDescription = author.id,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = author.id.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                Text(
                    text = author.id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isPo) {
                    PoTag(isPo = true)
                }

                if (author.name.isNotBlank() && author.name != "无名氏") {
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                badges?.invoke()
            }

            Text(
                text = threadTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
