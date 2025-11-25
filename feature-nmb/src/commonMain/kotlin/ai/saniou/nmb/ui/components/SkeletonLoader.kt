package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ShimmerContainer
import ai.saniou.coreui.widgets.SkeletonLine
import ai.saniou.coreui.widgets.rememberShimmerBrush
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
fun ThreadListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    ShimmerContainer(modifier = modifier.padding(Dimens.padding_small)) { brush ->
        Column {
            repeat(itemCount) {
                SkeletonThreadItem(brush)
                Spacer(modifier = Modifier.height(Dimens.padding_small))
            }
        }
    }
}

/**
 * 骨架屏帖子项
 */
@Composable
private fun SkeletonThreadItem(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_medium)
        ) {
            // 标题
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(),
                height = Dimens.icon_size_medium,
                brush = brush
            )

            Spacer(modifier = Modifier.height(Dimens.padding_small))

            // 作者信息
            SkeletonAuthor(brush)

            Spacer(modifier = Modifier.height(Dimens.padding_medium))

            // 内容
            SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
            Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), brush = brush)

            Spacer(modifier = Modifier.height(Dimens.padding_medium))

            // 图片占位
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(),
                height = Dimens.image_height_medium,
                brush = brush
            )
        }
    }
}

/**
 * 骨架屏回复项
 */
@Composable
fun SkeletonReplyItem(brush: Brush) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // 回复者信息
        SkeletonAuthor(brush)

        Spacer(modifier = Modifier.height(12.dp))

        // 回复内容
        SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), brush = brush)
    }
}

@Composable
internal fun SkeletonAuthor(brush: Brush) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonLine(
            modifier = Modifier.width(60.dp),
            height = Dimens.padding_medium,
            brush = brush
        )
        Spacer(modifier = Modifier.width(Dimens.padding_small))
        SkeletonLine(
            modifier = Modifier.width(100.dp),
            height = Dimens.padding_medium,
            brush = brush
        )
    }
}
