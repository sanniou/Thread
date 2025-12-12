package ai.saniou.forum.ui.components

import ai.saniou.coreui.widgets.NetworkImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

/**
 * Drawer 头部组件，显示欢迎图片
 * 作为背景层使用，其他内容可以叠加在上面
 *
 * @param imageUrl 图片URL
 * @param modifier 修饰符
 */
@Composable
fun DrawerHeader(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // 背景图片
        if (imageUrl != null) {
            NetworkImage(
                imageUrl,
                contentDescription = "欢迎图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 添加渐变遮罩，使图片与下方内容过渡更自然
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        } else {
            // 如果没有图片，显示纯色背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}
