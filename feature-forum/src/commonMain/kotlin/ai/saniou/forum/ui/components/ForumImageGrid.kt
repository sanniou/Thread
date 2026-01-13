package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * 商业级图片网格组件 (九宫格布局)
 *
 * 策略:
 * 1张图: 限制最大宽高比，根据图片比例自适应，最大高度不超过屏幕宽度的 4/3
 * 2张图: 2列平分
 * 3张图: 3列平分 (九宫格第一行)
 * 4张图: 2x2 网格
 * 5-9张: 3列多行网格
 * >9张: 3列网格，第9张显示 "+N" 遮罩
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForumImageGrid(
    images: List<Image>,
    onImageClick: (Image) -> Unit,
    onImageLongClick: ((Image) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) return

    val count = images.size
    val spacing = 4.dp
    val radius = Dimens.corner_radius_small

    Column(modifier = modifier) {
        when (count) {
            1 -> {
                val image = images.first()

                ForumImage(
                    image = image,
                    isThumb = true,
                    modifier = Modifier
                        .height(Dimens.image_height_medium)
                        .wrapContentWidth(Alignment.Start)
                        .clip(MaterialTheme.shapes.small)
                        .combinedClickable(
                            onClick = { onImageClick(image) },
                            onLongClick = { onImageLongClick?.invoke(image) }
                        ),
                    contentScale = ContentScale.FillHeight,
                    contentDescription = "帖子图片"
                )
            }

            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    images.forEach { image ->
                        ForumImage(
                            image = image,
                            isThumb = true,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radius))
                                .combinedClickable(
                                    onClick = { onImageClick(image) },
                                    onLongClick = { onImageLongClick?.invoke(image) }
                                ),
                            contentDescription = "帖子图片",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            4 -> {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        images.take(2).forEach { image ->
                            ForumImage(
                                image = image,
                                isThumb = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radius))
                                    .combinedClickable(
                                        onClick = { onImageClick(image) },
                                        onLongClick = { onImageLongClick?.invoke(image) }
                                    ),
                                contentDescription = "帖子图片",
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        images.drop(2).take(2).forEach { image ->
                            ForumImage(
                                image = image,
                                isThumb = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radius))
                                    .combinedClickable(
                                        onClick = { onImageClick(image) },
                                        onLongClick = { onImageLongClick?.invoke(image) }
                                    ),
                                contentDescription = "帖子图片",
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            else -> {
                // 3, 5-9+ 使用 3列网格
                val rows = (count + 2) / 3
                // 最多显示3行 (9张)
                val displayRows = minOf(rows, 3)
                
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    for (i in 0 until displayRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            for (j in 0 until 3) {
                                val index = i * 3 + j
                                if (index < count) {
                                    val image = images[index]
                                    // 检查是否是第9张且还有更多图片
                                    val isOverlay = index == 8 && count > 9
                                    val remaining = count - 9

                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                        ForumImage(
                                            image = image,
                                            isThumb = true,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(radius))
                                                .combinedClickable(
                                                    onClick = { onImageClick(image) },
                                                    onLongClick = { onImageLongClick?.invoke(image) }
                                                ),
                                            contentDescription = "帖子图片",
                                            contentScale = ContentScale.Crop
                                        )

                                        if (isOverlay) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clip(RoundedCornerShape(radius))
                                                    .combinedClickable(
                                                        onClick = { onImageClick(image) }, // 这里其实应该跳转到图库查看更多
                                                        onLongClick = {}
                                                    )
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.5f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+$remaining",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}