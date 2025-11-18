package ai.saniou.nmb.ui.components

import ai.saniou.coreui.widgets.ShimmerBrush
import ai.saniou.coreui.widgets.SkeletonLine
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * 骨架屏加载效果
 * 用于显示内容加载中的占位UI
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    val brush = ShimmerBrush()
    Column(
        modifier = modifier.padding(8.dp)
    ) {
        repeat(itemCount) {
            SkeletonThreadItem(brush)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 骨架屏帖子项
 */
@Composable
fun SkeletonThreadItem(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 20.dp, brush = brush)

            Spacer(modifier = Modifier.height(8.dp))

            // 作者信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonLine(modifier = Modifier.width(60.dp), height = 12.dp, brush = brush)
                Spacer(modifier = Modifier.width(8.dp))
                SkeletonLine(modifier = Modifier.width(100.dp), height = 12.dp, brush = brush)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 内容
            SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), brush = brush)

            Spacer(modifier = Modifier.height(12.dp))

            // 图片占位
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 160.dp, brush = brush)
        }
    }
}

/**
 * 骨架屏回复项
 */
@Composable
fun SkeletonReplyItem(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 回复者信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonLine(modifier = Modifier.width(60.dp), height = 12.dp, brush = brush)
                Spacer(modifier = Modifier.width(8.dp))
                SkeletonLine(modifier = Modifier.width(100.dp), height = 12.dp, brush = brush)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 回复内容
            SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), brush = brush)
        }
    }
}
