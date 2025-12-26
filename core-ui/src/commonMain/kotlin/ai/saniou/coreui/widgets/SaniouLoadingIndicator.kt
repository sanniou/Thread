package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.core_ui_list_loading_wording
import thread.core_ui.generated.resources.core_ui_loading_wording

/**
 * 基础的 Saniou 加载指示器
 * 使用 Material 3 CircularProgressIndicator
 */
@Composable
fun SaniouLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(
        modifier = modifier
    )
}

/**
 * 页面级加载指示器，带有提示文字
 * 默认居中显示
 */
@Composable
fun SaniouPageLoadingIndicator(
    modifier: Modifier = Modifier,
    messageId: StringResource = Res.string.core_ui_loading_wording
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SaniouLoadingIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(Dimens.padding_medium))
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 列表项加载占位符，通常用于分页加载时的底部
 */
@Composable
fun SaniouColumnLoadingPlaceholder(
    modifier: Modifier = Modifier,
    wording: String = stringResource(Res.string.core_ui_list_loading_wording)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SaniouLoadingIndicator(
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.padding_small))
        Text(
            text = wording,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}