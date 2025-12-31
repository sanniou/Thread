package ai.saniou.coreui.widgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 通用的 Saniou 主操作按钮 (Filled Button)
 * 用于页面上最主要的动作，如“提交”、“完成”。
 *
 * 遵循 Material 3 设计规范：
 * - 高度：40dp (默认)
 * - 圆角：Circle (Full rounded)
 * - 字体：Label Large
 */
@Composable
fun SaniouButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge, // Pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        content = {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

/**
 * Saniou 次要操作按钮 (Outlined Button)
 * 用于次要动作，如“取消”、“跳过”。
 *
 * 遵循 Material 3 设计规范：
 * - 描边：1dp
 * - 圆角：Circle (Full rounded)
 */
@Composable
fun SaniouOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge, // Pill shape
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        content = {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

/**
 * Saniou 文本按钮 (Text Button)
 * 用于低强调级的动作，或在卡片/对话框中的操作。
 *
 * 遵循 Material 3 设计规范：
 * - 无边框，无背景
 * - 交互时显示背景
 */
@Composable
fun SaniouTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge, // Pill shape
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        content = {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}