package ai.saniou.forum.ui.components

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
import androidx.compose.material3.MaterialTheme
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
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
        ) {
            repeat(itemCount) {
                SkeletonThreadItem(brush)
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
            .padding(Dimens.padding_standard)
    ) {
        // 作者信息 (Header)
        SkeletonAuthor(brush)

        Spacer(modifier = Modifier.height(Dimens.padding_medium))

        // 标题 (Optional)
        SkeletonLine(
            modifier = Modifier.fillMaxWidth(0.4f),
            height = 24.dp, // titleMedium line height
            brush = brush
        )

        Spacer(modifier = Modifier.height(Dimens.padding_small))

        // 内容
        SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_tiny))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.9f), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_tiny))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), brush = brush)

        Spacer(modifier = Modifier.height(Dimens.padding_standard))

        // 图片占位
        SkeletonLine(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small),
            height = Dimens.image_height_medium,
            brush = brush
        )
    }
}

/**
 * 骨架屏回复项
 */
@Composable
fun SkeletonReplyItem(brush: Brush) {
    Column(modifier = Modifier.padding(Dimens.padding_standard, Dimens.padding_medium)) {
        // 回复者信息
        SkeletonAuthor(brush)

        Spacer(modifier = Modifier.height(Dimens.padding_medium))

        // 回复内容
        SkeletonLine(modifier = Modifier.fillMaxWidth(), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_tiny))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f), brush = brush)
        Spacer(modifier = Modifier.height(Dimens.padding_tiny))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), brush = brush)
    }
}

@Composable
internal fun SkeletonAuthor(brush: Brush) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.padding_medium)
    ) {
        // 头像占位
        Box(
            modifier = Modifier
                .size(Dimens.avatar_size_medium)
                .clip(CircleShape)
                .background(brush)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
            // 用户ID
            SkeletonLine(
                modifier = Modifier.width(80.dp),
                height = 16.dp, // labelMedium
                brush = brush
            )
            // 时间
            SkeletonLine(
                modifier = Modifier.width(100.dp),
                height = 14.dp, // labelSmall
                brush = brush
            )
        }
    }
}
