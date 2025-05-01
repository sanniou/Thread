import ai.saniou.nmb.ui.components.HtmlText
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "HTML Test") {
        MaterialTheme {
            Surface {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("原始HTML:")
                    Text("<span style=\" color: green \">\r\n=========== 注册已开启 ==========<br>\r\n======= 周六日不定时开放注册 ======<br>\r\n<br><br>\r\n\r\n<\/span>\r\n我们的 微博：@X岛揭示板　微信公众号：<a href=\"https:\/\/image.nmb.best\/image\/2023-03-26\/64205d7d702ca.png\">")
                    
                    Text("\n\n渲染结果:")
                    HtmlText(
                        text = "<span style=\" color: green \">\r\n=========== 注册已开启 ==========<br>\r\n======= 周六日不定时开放注册 ======<br>\r\n<br><br>\r\n\r\n<\/span>\r\n我们的 微博：@X岛揭示板　微信公众号：<a href=\"https:\/\/image.nmb.best\/image\/2023-03-26\/64205d7d702ca.png\">点击查看</a>"
                    )
                }
            }
        }
    }
}
