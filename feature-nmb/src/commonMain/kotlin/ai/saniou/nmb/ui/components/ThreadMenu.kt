package ai.saniou.nmb.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 帖子菜单
 */
@Composable
fun ThreadMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onJumpToPage: () -> Unit,
    onTogglePoOnly: () -> Unit,
    onToggleSubscribe: (Boolean) -> Unit,
    onCopyLink: () -> Unit,
    isSubscribed: Boolean,
    isPoOnlyMode: Boolean
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        // 跳转到页面
        ThreadMenuItem(
            icon = Icons.Default.Place,
            text = "跳转到页面",
            onClick = {
                onJumpToPage()
                onDismissRequest()
            }
        )

        // 只看PO
        ThreadMenuItem(
            icon = Icons.Default.Person,
            text = if (isPoOnlyMode) "显示全部" else "只看PO",
            onClick = {
                onTogglePoOnly()
                onDismissRequest()
            }
        )

        // 订阅/取消订阅
        ThreadMenuItem(
            icon = if (isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            text = if (isSubscribed) "取消订阅" else "订阅",
            onClick = {
                onToggleSubscribe(!isSubscribed)
                onDismissRequest()
            }
        )

        // 复制链接
        ThreadMenuItem(
            icon = Icons.Default.Share,
            text = "复制链接",
            onClick = {
                onCopyLink()
                onDismissRequest()
            }
        )
    }
}

/**
 * 帖子菜单项
 */
@Composable
fun ThreadMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        },
        onClick = onClick
    )
}

