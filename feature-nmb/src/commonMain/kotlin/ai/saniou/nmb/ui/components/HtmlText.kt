package ai.saniou.nmb.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * 支持简单HTML标签的文本组件
 * 目前支持的标签：
 * - <b>粗体</b>
 * - <i>斜体</i>
 * - <u>下划线</u>
 * - <font color="red">颜色</font>
 * - <br>换行
 * - <span style="color: red">带样式的文本</span>
 * - <a href="https://example.com">链接</a>
 */
@Composable
fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    onReferenceClick: ((Long) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    // 处理URI链接的处理器
    val uriHandler = LocalUriHandler.current

    // 预处理文本
    val processedText = preprocessHtmlText(text)

    // 解析HTML并构建AnnotatedString
    val annotatedString = parseHtml(
        processedText,
        onReferenceClick != null
    )

    // 使用ClickableText替代Text，以支持点击事件
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            // 处理引用链接的点击事件
            annotatedString.getStringAnnotations(TAG_REFERENCE, offset, offset)
                .firstOrNull()?.let { annotation ->
                    val refId = annotation.item.toLongOrNull()
                    if (refId != null) {
                        onReferenceClick?.invoke(refId)
                    }
                }

            // 处理URL链接的点击事件
            annotatedString.getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    onLinkClick?.invoke(url) ?: uriHandler.openUri(url)
                }
        }
    )
}

/**
 * 预处理HTML文本，规范化换行符和处理转义字符
 */
private fun preprocessHtmlText(text: String): String {
    // 第一步：处理转义的斜杠
    var result = text.replace("\\/", "/")

    // 第二步：将所有换行符标准化为单个 \n
    result = result.replace("\r\n", "\n")

    // 第三步：将 <br> 标签替换为特殊标记，以便后续处理
    result = result.replace(Regex("<br\\s*/?>"), "{{BR_MARK}}")

    // 第四步：规范化连续的换行符（\n）
    result = result.replace(Regex("\n+"), "\n")

    // 第五步：规范化连续的 <br> 标记
    result = result.replace(Regex("(\\{\\{BR_MARK}})+"), "{{BR_MARK}}")

    // 第六步：处理 <br> 和 \n 混合的情况
    result = result.replace(Regex("\\{\\{BR_MARK}}\n"), "{{BR_MARK}}")
    result = result.replace(Regex("\n\\{\\{BR_MARK}}"), "{{BR_MARK}}")

    // 第七步：将特殊标记转回 <br> 标签
    result = result.replace("{{BR_MARK}}", "<br>")

    // 第八步：移除末尾的 <br> 标签
    result = result.replace(Regex("(<br>\\s*)+$"), "")

    return result
}

/**
 * 解析HTML文本并构建AnnotatedString
 */
@Composable
private fun parseHtml(
    text: String,
    enableReferenceDetection: Boolean
): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0
    val length = text.length

    // 引用链接的正则表达式
    val referencePattern = if (enableReferenceDetection) {
        ">>No\\.(\\d+)".toRegex()
    } else null

    while (currentIndex < length) {
        val tagStartIndex = text.indexOf("<", currentIndex)

        if (tagStartIndex == -1) {
            // 没有更多标签，添加剩余文本
            append(decodeHtmlEntities(text.substring(currentIndex)))
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

        // 处理标签
        currentIndex = when {
            // 处理<br>标签 - 统一处理所有形式的<br>标签
            tag == "br" || tag.startsWith("br ") || tag == "br/" -> {
                append("\n")
                tagEndIndex + 1
            }

            // 处理<span>标签
            tag.startsWith("span ") -> {
                handleSpanTag(text, tag, tagStartIndex, tagEndIndex, referencePattern)
            }

            // 处理<a>标签
            tag.startsWith("a ") -> {
                handleAnchorTag(text, tag, tagStartIndex, tagEndIndex)
            }

            // 处理<b>标签
            tag == "b" -> {
                handleSimpleTag(text, tagEndIndex, "</b>", SpanStyle(fontWeight = FontWeight.Bold))
            }

            // 处理<i>标签
            tag == "i" -> {
                handleSimpleTag(text, tagEndIndex, "</i>", SpanStyle(fontStyle = FontStyle.Italic))
            }

            // 处理<u>标签
            tag == "u" -> {
                handleSimpleTag(text, tagEndIndex, "</u>", SpanStyle(textDecoration = TextDecoration.Underline))
            }

            // 处理<font>标签
            tag.startsWith("font ") -> {
                handleFontTag(text, tag, tagStartIndex, tagEndIndex, referencePattern)
            }

            // 不支持的标签，原样显示
            else -> {
                append(text.substring(tagStartIndex, tagEndIndex + 1))
                tagEndIndex + 1
            }
        }
    }
}

/**
 * 处理<span>标签
 */
private fun AnnotatedString.Builder.handleSpanTag(
    text: String,
    tag: String,
    tagStartIndex: Int,
    tagEndIndex: Int,
    referencePattern: Regex?
): Int {
    // 提取style属性
    val styleAttr = extractAttribute(tag, "style")

    if (styleAttr != null) {
        val endTagIndex = text.indexOf("</span>", tagEndIndex, ignoreCase = true)
        if (endTagIndex != -1) {
            val spanText = text.substring(tagEndIndex + 1, endTagIndex)

            // 预处理span内部的文本，确保<br>标签被正确处理
            val processedSpanText = preprocessHtmlText(spanText)
            val decodedText = decodeHtmlEntities(processedSpanText)

            // 解析颜色样式
            val colorMatch = Regex("""color:\s*([^;"]+)""").find(styleAttr)
            if (colorMatch != null) {
                val colorValue = colorMatch.groupValues[1].trim()
                try {
                    val color = androidx.compose.ui.graphics.Color(parseColorValue(colorValue))

                    // 检查是否包含引用链接
                    if (referencePattern != null) {
                        val referenceMatch = referencePattern.find(decodedText)
                        if (referenceMatch != null) {
                            val refId = referenceMatch.groupValues[1].toLongOrNull()
                            if (refId != null) {
                                // 添加引用链接
                                pushStringAnnotation(
                                    tag = TAG_REFERENCE,
                                    annotation = refId.toString()
                                )
                                withStyle(
                                    SpanStyle(
                                        color = color,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    // 递归处理span内部的HTML标签
                                    append(parseNestedHtml(processedSpanText, referencePattern))
                                }
                                pop()
                                return endTagIndex + 7 // 7 是 </span> 的长度
                            }
                        }
                    }

                    // 普通带颜色的文本
                    withStyle(SpanStyle(color = color)) {
                        // 递归处理span内部的HTML标签
                        append(parseNestedHtml(processedSpanText, referencePattern))
                    }
                    return endTagIndex + 7
                } catch (e: Exception) {
                    // 颜色解析失败，使用默认样式
                    append(parseNestedHtml(processedSpanText, referencePattern))
                    return endTagIndex + 7
                }
            } else {
                // 没有颜色样式，使用默认样式
                append(parseNestedHtml(processedSpanText, referencePattern))
                return endTagIndex + 7
            }
        }
    }

    // 无法处理的span标签，原样显示
    append(text.substring(tagStartIndex, tagEndIndex + 1))
    return tagEndIndex + 1
}

/**
 * 解码HTML实体
 */
private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}

/**
 * 解析嵌套的HTML内容
 */
private fun parseNestedHtml(
    text: String,
    referencePattern: Regex?
): String {
    // 处理<br>标签
    val brProcessed = text.replace(Regex("<br\\s*/?>"), "\n")

    // 这里可以添加对其他嵌套标签的处理
    // 目前只处理<br>标签，其他标签保持原样

    return decodeHtmlEntities(brProcessed)
}

/**
 * 处理<a>标签
 */
@Composable
private fun AnnotatedString.Builder.handleAnchorTag(
    text: String,
    tag: String,
    tagStartIndex: Int,
    tagEndIndex: Int
): Int {
    // 提取href属性
    val hrefValue = extractAttribute(tag, "href")

    if (hrefValue != null) {
        val endTagIndex = text.indexOf("</a>", tagEndIndex, ignoreCase = true)
        if (endTagIndex != -1) {
            val linkText = text.substring(tagEndIndex + 1, endTagIndex)

            // 添加链接注释
            pushStringAnnotation(
                tag = TAG_URL,
                annotation = hrefValue
            )
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(decodeHtmlEntities(linkText))
            }
            pop()
            return endTagIndex + 4 // 4 是 </a> 的长度
        }
    }

    // 无法处理的a标签，原样显示
    append(text.substring(tagStartIndex, tagEndIndex + 1))
    return tagEndIndex + 1
}

/**
 * 处理<font>标签
 */
private fun AnnotatedString.Builder.handleFontTag(
    text: String,
    tag: String,
    tagStartIndex: Int,
    tagEndIndex: Int,
    referencePattern: Regex?
): Int {
    // 提取color属性
    val colorValue = extractAttribute(tag, "color")

    if (colorValue != null) {
        val endTagIndex = text.indexOf("</font>", tagEndIndex, ignoreCase = true)
        if (endTagIndex != -1) {
            val fontText = text.substring(tagEndIndex + 1, endTagIndex)

            // 预处理font内部的文本
            val processedFontText = preprocessHtmlText(fontText)
            val decodedText = decodeHtmlEntities(processedFontText)

            try {
                val color = androidx.compose.ui.graphics.Color(parseColorValue(colorValue))

                // 检查是否包含引用链接
                if (referencePattern != null) {
                    val referenceMatch = referencePattern.find(decodedText)
                    if (referenceMatch != null) {
                        val refId = referenceMatch.groupValues[1].toLongOrNull()
                        if (refId != null) {
                            // 添加引用链接
                            pushStringAnnotation(
                                tag = TAG_REFERENCE,
                                annotation = refId.toString()
                            )
                            withStyle(
                                SpanStyle(
                                    color = color,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(parseNestedHtml(processedFontText, referencePattern))
                            }
                            pop()
                            return endTagIndex + 7 // 7 是 </font> 的长度
                        }
                    }
                }

                // 普通带颜色的文本
                withStyle(SpanStyle(color = color)) {
                    append(parseNestedHtml(processedFontText, referencePattern))
                }
                return endTagIndex + 7
            } catch (e: Exception) {
                // 颜色解析失败，使用默认样式
                append(parseNestedHtml(processedFontText, referencePattern))
                return endTagIndex + 7
            }
        }
    }

    // 无法处理的font标签，原样显示
    append(text.substring(tagStartIndex, tagEndIndex + 1))
    return tagEndIndex + 1
}

/**
 * 处理简单标签（b, i, u等）
 */
private fun AnnotatedString.Builder.handleSimpleTag(
    text: String,
    tagEndIndex: Int,
    endTag: String,
    style: SpanStyle
): Int {
    val endTagIndex = text.indexOf(endTag, tagEndIndex, ignoreCase = true)
    if (endTagIndex != -1) {
        val tagContent = text.substring(tagEndIndex + 1, endTagIndex)

        // 预处理标签内部的文本
        val processedContent = preprocessHtmlText(tagContent)

        withStyle(style) {
            append(decodeHtmlEntities(processedContent))
        }
        return endTagIndex + endTag.length
    }

    // 标签未闭合，原样显示
    append(text.substring(tagEndIndex, tagEndIndex + 1))
    return tagEndIndex + 1
}

/**
 * 从标签中提取属性值
 */
private fun extractAttribute(tag: String, attrName: String): String? {
    val attrPattern = "$attrName\\s*=\\s*[\"']([^\"']*)[\"']".toRegex()
    val match = attrPattern.find(tag)
    return match?.groupValues?.getOrNull(1)
}

private const val ALPHA_MASK = 0xFF000000.toInt()
private const val TAG_URL = "URL"
private const val TAG_REFERENCE = "REFERENCE"

/**
 * 解析颜色值
 */
private fun parseColorValue(color: String): Int {
    // 处理命名颜色
    val namedColors = mapOf(
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF00FF00.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "black" to 0xFF000000.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "gray" to 0xFF808080.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "purple" to 0xFF800080.toInt(),
        "orange" to 0xFFFFA500.toInt(),
        "brown" to 0xFFA52A2A.toInt()
    )

    // 检查是否是命名颜色
    namedColors[color.lowercase()]?.let { return it }

    // 如果不是命名颜色，则尝试解析十六进制颜色
    if (!color.startsWith("#")) {
        return ALPHA_MASK // 默认黑色
    }

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
