package ai.saniou.forum.ui.components

import ai.saniou.thread.data.source.nmb.remote.dto.IBaseAuthor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ThreadAuthor(
    author: IBaseAuthor,
    isPo: Boolean = false,
    onClick: ((String) -> Unit)? = null,
) {
    ThreadAuthor(
        userName = author.userHash,
        showName = author.name,
        threadTime = author.now,
        isPo = isPo,
        onClick = onClick
    )
}

@Composable
fun ThreadAuthor(
    userName: String,
    showName: String,
    threadTime: String,
    isPo: Boolean = false,
    onClick: ((String) -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 头像占位，根据 Hash 生成颜色
        val avatarColor = remember(userName) {
            val hash = userName.hashCode()
            Color(hash).copy(alpha = 1f)
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .then(
                    if (onClick != null) Modifier.clickable { onClick(userName) } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isPo) {
                    PoTag(isPo = true)
                }

                if (showName.isNotBlank() && showName != "无名氏") {
                    Text(
                        text = showName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Text(
                text = threadTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
