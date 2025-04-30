package ai.saniou.nmb.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.ClickableText
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
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    onReferenceClick: ((Long) -> Unit)? = null
) {
    // 添加引用链接的正则表达式
    val referencePattern = ">>No\\.(\\d+)".toRegex()

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

            // 添加标签前的文本，并解码HTML实体
            if (tagStartIndex > currentIndex) {
                val beforeTagText = text.substring(currentIndex, tagStartIndex)
                append(decodeHtmlEntities(beforeTagText))
            }

            val tagEndIndex = text.indexOf(">", tagStartIndex)
            if (tagEndIndex == -1) {
                // 标签没有闭合，添加剩余文本
                append(decodeHtmlEntities(text.substring(tagStartIndex)))
                break
            }

            val tag = text.substring(tagStartIndex + 1, tagEndIndex).lowercase()

            // 处理自闭合标签，如 <br />
            if (tag.startsWith("br") || tag == "br /") {
                append("\n")
                currentIndex = tagEndIndex + 1
                continue
            }

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
                            append(decodeHtmlEntities(boldText))
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
                            append(decodeHtmlEntities(italicText))
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
                            append(decodeHtmlEntities(underlineText))
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
                                        parseColorValue(colorValue)
                                    )
                                    // 处理引用链接
                                    val decodedText = decodeHtmlEntities(fontText)
                                    val referenceMatch = referencePattern.find(decodedText)

                                    if (referenceMatch != null && onReferenceClick != null) {
                                        // 如果是引用链接，则添加点击注释
                                        val refId = referenceMatch.groupValues[1].toLongOrNull()
                                        if (refId != null) {
                                            pushStringAnnotation(
                                                tag = "REFERENCE",
                                                annotation = refId.toString()
                                            )
                                            withStyle(SpanStyle(
                                                color = color,
                                                textDecoration = TextDecoration.Underline
                                            )) {
                                                append(decodedText)
                                            }
                                            pop()
                                        } else {
                                            withStyle(SpanStyle(color = color)) {
                                                append(decodedText)
                                            }
                                        }
                                    } else {
                                        withStyle(SpanStyle(color = color)) {
                                            append(decodedText)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 颜色解析失败，使用默认样式
                                    append(decodeHtmlEntities(fontText))
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

    // 使用ClickableText替代Text，以支持点击事件
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            // 处理引用链接的点击事件
            annotatedString.getStringAnnotations("REFERENCE", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val refId = annotation.item.toLongOrNull()
                    if (refId != null) {
                        onReferenceClick?.invoke(refId)
                    }
                }
        }
    )
}
private const val ALPHA_MASK = 0xFF000000.toInt()

/**
 * 解码HTML实体
 */
internal fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}

/**
 * 解析颜色值
 */
internal fun parseColorValue(color: String): Int {
    require(color.startsWith("#")) { "Invalid color value $color" }

    return when (color.length) {
        7 -> {
            // #RRGGBB
            color.substring(1).toUInt(16).toInt() or ALPHA_MASK
        }
        9 -> {
            // #AARRGGBB
            color.substring(1).toUInt(16).toInt()
        }
        4 -> {
            // #RGB
            val v = color.substring(1).toUInt(16).toInt()
            var k = (v shr 8 and 0xF) * 0x110000
            k = k or (v shr 4 and 0xF) * 0x1100
            k = k or (v and 0xF) * 0x11
            k or ALPHA_MASK
        }
        5 -> {
            // #ARGB
            val v = color.substring(1).toUInt(16).toInt()
            var k = (v shr 12 and 0xF) * 0x11000000
            k = k or (v shr 8 and 0xF) * 0x110000
            k = k or (v shr 4 and 0xF) * 0x1100
            k = k or (v and 0xF) * 0x11
            k or ALPHA_MASK
        }
        else -> ALPHA_MASK
    }
}
