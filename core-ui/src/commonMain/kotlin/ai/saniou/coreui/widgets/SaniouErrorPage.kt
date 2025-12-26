package ai.saniou.coreui.widgets

import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.core_ui_no_internet_retry

/**
 * 通用的错误页面
 * 用于展示全屏错误信息，并提供重试操作
 */
@Composable
fun SaniouErrorPage(
    error: AppError,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavBack: (() -> Unit)? = null,
    title: String? = null
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (title != null || onNavBack != null) {
                SaniouTopAppBar(
                    title = title ?: "",
                    onNavigationClick = onNavBack
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.padding_large),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 图标
                Icon(
                    imageVector = Icons.Default.Warning, // 暂时使用通用图标，后续可替换为更精美的插画
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(Dimens.padding_large))

                // 错误信息
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.padding_medium))

                // 辅助说明（可选）
//                Text(
//                    text = "请检查网络连接或稍后重试",
//                    style = MaterialTheme.typography.bodyMedium,
//                    textAlign = TextAlign.Center,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )

                Spacer(modifier = Modifier.height(Dimens.padding_extra_large))

                // 重试按钮
                SaniouButton(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.core_ui_no_internet_retry))
                }
            }
        }
    }
}
