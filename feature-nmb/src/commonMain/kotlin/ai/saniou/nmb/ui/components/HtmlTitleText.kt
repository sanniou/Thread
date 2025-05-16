package ai.saniou.nmb.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * 支持简单HTML标签的标题文本组件
 *
 * 主要用于在TopAppBar中显示大标题和小标题
 */
@Composable
fun HtmlTitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    // 解析HTML标签
    if (text.contains("<b>") && text.contains("</b>") && text.contains("<br>") && text.contains("<small>") && text.contains(
            "</small>"
        )
    ) {
        // 提取大标题和小标题
        val boldStart = text.indexOf("<b>") + 3
        val boldEnd = text.indexOf("</b>")
        val smallStart = text.indexOf("<small>") + 7
        val smallEnd = text.indexOf("</small>")

        if (boldStart < boldEnd && smallStart < smallEnd) {
            val mainTitle = text.substring(boldStart, boldEnd)
            val subtitle = text.substring(smallStart, smallEnd)

            Column(modifier = modifier) {
                // 大标题
                Text(
                    text = mainTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 小标题
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            return
        }
    }

    // 如果不包含特定的HTML标签，则直接显示文本
    Text(
        text = text.replace("<b>", "").replace("</b>", "")
            .replace("<br>", " ").replace("<small>", "").replace("</small>", ""),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
