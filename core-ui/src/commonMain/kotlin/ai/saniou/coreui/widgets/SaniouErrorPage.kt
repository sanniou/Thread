package ai.saniou.coreui.widgets

import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.AppErrorType
import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.core_ui_no_internet_retry

/**
 * 通用的错误页面 (Empty State / Error State)
 *
 * 风格化重构：
 * 1. 增加图标容器背景，提升视觉层级。
 * 2. 优化文案排版，增加副标题支持。
 * 3. 按钮样式统一为 Pill shape。
 */
@Composable
fun SaniouErrorPage(
    error: AppError,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavBack: (() -> Unit)? = null,
    title: String? = null
) {
    val isNetworkError = error.type == AppErrorType.NETWORK

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
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isNetworkError) Icons.Default.SignalWifiConnectedNoInternet4 else Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.padding_extra_large))

                // 错误信息 (Title)
                Text(
                    text = if (isNetworkError) "网络连接失败" else "出错了",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.padding_small))

                // 详细信息 (Body)
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 重试按钮
                SaniouButton(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth(0.5f)
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
