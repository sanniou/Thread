package ai.saniou.nmb.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * 支持简单HTML标签的文本组件
 * 
 * 目前支持的标签：
 * - <b>粗体</b>
 * - <i>斜体</i>
 * - <u>下划线</u>
 * - <font color="red">颜色</font>
 * - <br>换行
 */
@Composable
fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val length = text.length
        
        while (currentIndex < length) {
            val tagStartIndex = text.indexOf("<", currentIndex)
            
            if (tagStartIndex == -1) {
                // 没有更多标签，添加剩余文本
                append(text.substring(currentIndex))
                break
            }
            
            // 添加标签前的文本
            if (tagStartIndex > currentIndex) {
                append(text.substring(currentIndex, tagStartIndex))
            }
            
            val tagEndIndex = text.indexOf(">", tagStartIndex)
            if (tagEndIndex == -1) {
                // 标签没有闭合，添加剩余文本
                append(text.substring(tagStartIndex))
                break
            }
            
            val tag = text.substring(tagStartIndex + 1, tagEndIndex).lowercase()
            
            when {
                tag == "br" -> {
                    append("\n")
                    currentIndex = tagEndIndex + 1
                }
                tag == "b" -> {
                    // 处理粗体标签
                    val endTagIndex = text.indexOf("</b>", tagEndIndex, ignoreCase = true)
                    if (endTagIndex != -1) {
                        val boldText = text.substring(tagEndIndex + 1, endTagIndex)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        currentIndex = endTagIndex + 4 // 4 是 </b> 的长度
                    } else {
                        append(text.substring(tagStartIndex, tagEndIndex + 1))
                        currentIndex = tagEndIndex + 1
                    }
                }
                tag == "i" -> {
                    // 处理斜体标签
                    val endTagIndex = text.indexOf("</i>", tagEndIndex, ignoreCase = true)
                    if (endTagIndex != -1) {
                        val italicText = text.substring(tagEndIndex + 1, endTagIndex)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicText)
                        }
                        currentIndex = endTagIndex + 4 // 4 是 </i> 的长度
                    } else {
                        append(text.substring(tagStartIndex, tagEndIndex + 1))
                        currentIndex = tagEndIndex + 1
                    }
                }
                tag == "u" -> {
                    // 处理下划线标签
                    val endTagIndex = text.indexOf("</u>", tagEndIndex, ignoreCase = true)
                    if (endTagIndex != -1) {
                        val underlineText = text.substring(tagEndIndex + 1, endTagIndex)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(underlineText)
                        }
                        currentIndex = endTagIndex + 4 // 4 是 </u> 的长度
                    } else {
                        append(text.substring(tagStartIndex, tagEndIndex + 1))
                        currentIndex = tagEndIndex + 1
                    }
                }
                tag.startsWith("font ") -> {
                    // 处理带颜色的字体标签
                    val colorAttr = "color="
                    val colorStartIndex = tag.indexOf(colorAttr)
                    if (colorStartIndex != -1) {
                        val colorValueStart = colorStartIndex + colorAttr.length
                        val quoteChar = tag[colorValueStart]
                        val colorValueEnd = tag.indexOf(quoteChar, colorValueStart + 1)
                        if (colorValueEnd != -1) {
                            val colorValue = tag.substring(colorValueStart + 1, colorValueEnd)
                            val endTagIndex = text.indexOf("</font>", tagEndIndex, ignoreCase = true)
                            if (endTagIndex != -1) {
                                val fontText = text.substring(tagEndIndex + 1, endTagIndex)
                                try {
                                    val color = androidx.compose.ui.graphics.Color(
                                        android.graphics.Color.parseColor(colorValue)
                                    )
                                    withStyle(SpanStyle(color = color)) {
                                        append(fontText)
                                    }
                                } catch (e: Exception) {
                                    // 颜色解析失败，使用默认样式
                                    append(fontText)
                                }
                                currentIndex = endTagIndex + 7 // 7 是 </font> 的长度
                            } else {
                                append(text.substring(tagStartIndex, tagEndIndex + 1))
                                currentIndex = tagEndIndex + 1
                            }
                        } else {
                            append(text.substring(tagStartIndex, tagEndIndex + 1))
                            currentIndex = tagEndIndex + 1
                        }
                    } else {
                        append(text.substring(tagStartIndex, tagEndIndex + 1))
                        currentIndex = tagEndIndex + 1
                    }
                }
                else -> {
                    // 不支持的标签，原样显示
                    append(text.substring(tagStartIndex, tagEndIndex + 1))
                    currentIndex = tagEndIndex + 1
                }
            }
        }
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style
    )
}
