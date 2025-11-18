package ai.saniou.nmb.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.saniou.coreui.theme.Dimens

@Composable
fun LoadEndIndicator(onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .run {
                if (onClick != null) {
                    this.clickable(onClick = onClick)
                } else {
                    this
                }
            }
            .padding(Dimens.padding_medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "已加载全部帖子",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingFailedIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.padding_medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "加载更多失败",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.padding_medium),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.icon_size_medium),
            strokeWidth = 2.dp
        )
    }
}
