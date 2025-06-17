package ai.saniou.nmb.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Drawer菜单行组件，显示一排菜单入口
 *
 * @param menuItems 菜单项列表
 * @param modifier 修饰符
 */
@Composable
fun DrawerMenuRow(
    menuItems: List<DrawerMenuItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        menuItems.forEach { menuItem ->
            DrawerMenuIcon(
                icon = menuItem.icon,
                label = menuItem.label,
                onClick = menuItem.onClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Drawer菜单图标组件
 *
 * @param icon 图标
 * @param label 标签
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun DrawerMenuIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Drawer菜单项数据类
 */
data class DrawerMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

