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
            // 使用 HSL 模式生成更和谐的颜色，避免过于刺眼或暗淡的颜色
            // 这里简单模拟：确保饱和度和亮度在一定范围内
            val r = (hash and 0xFF0000 shr 16) / 255f
            val g = (hash and 0x00FF00 shr 8) / 255f
            val b = (hash and 0x0000FF) / 255f
            // 简单的混合算法，偏向柔和色调
            Color(
                red = (r + 0.5f) / 1.5f,
                green = (g + 0.5f) / 1.5f,
                blue = (b + 0.5f) / 1.5f,
                alpha = 1f
            )
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
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
            ) {
                Text(
                    text = author.id,
                    style = MaterialTheme.typography.labelLarge,
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
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                Text(
                    text = threadTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                badges?.invoke()
            }
        }
    }
}
