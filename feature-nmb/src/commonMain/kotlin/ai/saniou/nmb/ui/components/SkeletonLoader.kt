package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ShimmerContainer
import ai.saniou.coreui.widgets.SkeletonLine
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 16.dp, 16.dp, 24.dp)
    ) {
        // 标题
        SkeletonLine(
            modifier = Modifier.fillMaxWidth(0.6f),
            height = 28.dp, // HeadlineSmall size
            brush = brush
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 作者信息
        SkeletonAuthor(brush)

        Spacer(modifier = Modifier.height(16.dp))

        // 内容
        SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.9f), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), brush = brush)

        Spacer(modifier = Modifier.height(16.dp))

        // 图片占位
        SkeletonLine(
            modifier = Modifier.fillMaxWidth(),
            height = 200.dp,
            brush = brush
        )
    }
}

/**
 * 骨架屏回复项
 */
@Composable
fun SkeletonReplyItem(brush: Brush) {
    Column(modifier = Modifier.padding(16.dp, 12.dp)) {
        // 回复者信息
        SkeletonAuthor(brush)

        Spacer(modifier = Modifier.height(12.dp))

        // 回复内容
        SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_extra_small))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), brush = brush)
    }
}

@Composable
internal fun SkeletonAuthor(brush: Brush) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        // 头像占位
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(brush)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 用户ID
            SkeletonLine(
                modifier = Modifier.width(80.dp),
                height = 14.dp,
                brush = brush
            )
            // 时间
            SkeletonLine(
                modifier = Modifier.width(100.dp),
                height = 12.dp,
                brush = brush
            )
        }
    }
}
