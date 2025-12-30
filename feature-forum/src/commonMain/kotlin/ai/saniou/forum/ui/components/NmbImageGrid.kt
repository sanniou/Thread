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
import androidx.compose.foundation.layout.heightIn
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
 * 商业级图片网格组件
 *
 * 策略:
 * 1张图: 限制最大宽高比，根据图片比例自适应，最大高度不超过屏幕宽度的 4/3
 * 2张图: 2列平分
 * 3张图: 1大(2/3宽) + 2小(竖排) 或 3列平分? -> 采用 3列平分最稳妥，或者仿微信朋友圈 9宫格逻辑
 * 4张图: 2x2 网格
 * 5-9张: 3列多行网格
 * >9张: 3列网格，第9张显示 "+N" 遮罩
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NmbImageGrid(
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

                NmbImageItem(
                    image = image,
                    modifier = Modifier
                        .height(Dimens.image_height_medium)
                        .wrapContentWidth(Alignment.Start)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.FillHeight,
                    onClick = { onImageClick(image) },
                    onLongClick = { onImageLongClick?.invoke(image) }
                )
            }

            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    images.forEach { image ->
                        NmbImageItem(
                            image = image,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radius)),
                            onClick = { onImageClick(image) },
                            onLongClick = { onImageLongClick?.invoke(image) }
                        )
                    }
                }
            }

            4 -> {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        images.take(2).forEach { image ->
                            NmbImageItem(
                                image = image,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radius)),
                                onClick = { onImageClick(image) },
                                onLongClick = { onImageLongClick?.invoke(image) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        images.drop(2).take(2).forEach { image ->
                            NmbImageItem(
                                image = image,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radius)),
                                onClick = { onImageClick(image) },
                                onLongClick = { onImageLongClick?.invoke(image) }
                            )
                        }
                    }
                }
            }

            else -> {
                // 3, 5-9+ 使用 3列网格
                val rows = (count + 2) / 3
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    for (i in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            for (j in 0 until 3) {
                                val index = i * 3 + j
                                if (index < count) {
                                    val image = images[index]
                                    // 检查是否是第9张且还有更多图片
                                    val isOverlay = index == 8 && count > 9
                                    val remaining = count - 9

                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                        NmbImageItem(
                                            image = image,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(radius)),
                                            onClick = { onImageClick(image) },
                                            onLongClick = { onImageLongClick?.invoke(image) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NmbImageItem(
    image: Image,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    NmbImage(
        imgPath = image.thumbnailUrl,
        ext = "",
        isThumb = true,
        contentDescription = "帖子图片",
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        contentScale = contentScale
    )
}
