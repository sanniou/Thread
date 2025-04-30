package ai.saniou.nmb.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 线程页面的菜单组件
 */
@Composable
fun ThreadMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onJumpToPage: () -> Unit,
    onTogglePoOnly: () -> Unit,
    onToggleSubscribe: (Boolean) -> Unit,
    onCopyLink: () -> Unit,
    isSubscribed: Boolean = false,
    isPoOnlyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(200.dp)
        ) {
            // 跳页选项
            DropdownMenuItem(
                text = { Text("跳转到页面") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismissRequest()
                    onJumpToPage()
                }
            )

            // 只看PO选项
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isPoOnlyMode) "显示全部回复" else "只看PO",
                        color = if (isPoOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isPoOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onDismissRequest()
                    onTogglePoOnly()
                }
            )

            // 订阅/取消订阅选项
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isSubscribed) "取消订阅" else "订阅",
                        color = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleSubscribe(!isSubscribed)
                }
            )

            // 复制链接选项
            DropdownMenuItem(
                text = { Text("复制链接") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismissRequest()
                    onCopyLink()
                }
            )
        }
    }
}
