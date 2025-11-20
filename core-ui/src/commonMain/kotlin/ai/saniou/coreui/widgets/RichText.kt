package ai.saniou.coreui.widgets

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

private const val TAG_URL = "URL"
private const val TAG_REFERENCE = "REFERENCE"

/**
 * 空白行处理策略
 */
enum class BlankLinePolicy {
    /** 不处理，保留所有空白行 */
    KEEP,

    /** 将连续的空白行合并为一行 */
    COLLAPSE,

    /** 移除所有空白行 */
    REMOVE
}


/**
 * 支持简单HTML标签的文本组件
 *
 * 通过一个健壮的、基于栈的解析器，支持以下特性：
 * - 嵌套标签: `<b><i>bold and italic</i></b>`
 * - 颜色: `<font color="#789922">color text</font>` or `<font color="green">color text</font>`
 * - 样式: `<b>`, `<i>`, `<u>`
 * - 链接: `<a href="https://example.com">link</a>`
 * - 换行: `<br>`
 * - 自定义引用匹配: 通过 `referencePattern` 实现对特定模式（如 `>>No.12345`）的点击支持
 * - 空白行处理: 通过 `blankLinePolicy` 控制空白行的显示
 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    onReferenceClick: ((String) -> Unit)? = null,
    referencePattern: Regex? = null,
    onLinkClick: ((String) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    blankLinePolicy: BlankLinePolicy = BlankLinePolicy.KEEP,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary

    val annotatedString = remember(text, referencePattern, style, linkColor, blankLinePolicy) {
        val styledText = parseHtml(text, style, linkColor, blankLinePolicy)
        applyClickableAnnotations(styledText, referencePattern)
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedString.getStringAnnotations(TAG_REFERENCE, offset, offset)
                .firstOrNull()?.let { annotation ->
                    onReferenceClick?.invoke(annotation.item)
                }

            annotatedString.getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    onLinkClick?.invoke(url) ?: uriHandler.openUri(url)
                }
        }
    )
}

/**
 * 第一步: 解析HTML标签并应用基础样式
 */
private fun parseHtml(
    html: String,
    baseStyle: TextStyle,
    linkColor: Color,
    blankLinePolicy: BlankLinePolicy
): AnnotatedString {
    // 1. 预处理：解码HTML实体并将<br>替换为换行符
    var cleanHtml = decodeHtmlEntities(html).replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // 1.5. 根据策略处理空白行
    cleanHtml = when (blankLinePolicy) {
        BlankLinePolicy.KEEP -> cleanHtml
        BlankLinePolicy.COLLAPSE -> cleanHtml.replace(Regex("(\\n\\s*){2,}"), "\n")
        BlankLinePolicy.REMOVE -> cleanHtml.lines().filter { it.isNotBlank() }.joinToString("\n")
    }


    // 2. 使用正则表达式将HTML分词为标签和文本
    val tokenizer = Regex("(<[^>]+>)|([^<]+)")
    val tokens = tokenizer.findAll(cleanHtml)

    // 3. 使用栈来处理嵌套样式
    val styleStack = mutableListOf(SpanStyle())

    return buildAnnotatedString {
        for (token in tokens) {
            val text = token.value
            if (text.startsWith("<") && text.endsWith(">")) {
                // 是一个标签
                val tag = parseTag(text)
                when {
                    tag.isClosing -> {
                        // 闭合标签, 弹出样式
                        if (styleStack.size > 1) styleStack.removeLast()
                        // 如果是链接，也需要弹出注解
                        if (tag.name == "a") pop()
                    }
                    !tag.isSelfClosing -> {
                        // 开始标签, 推入新样式
                        styleStack.add(tag.toSpanStyle(styleStack.last(), linkColor))
                        // 如果是链接，推入注解
                        tag.attributes["href"]?.let { href ->
                            pushStringAnnotation(TAG_URL, href)
                        }
                    }
                }
            } else {
                // 是纯文本
                withStyle(styleStack.last()) {
                    append(text)
                }
            }
        }
    }
}

/**
 * 第二步: 在已应用样式的文本上，查找并应用链接和引用的点击注解
 */
private fun applyClickableAnnotations(
    styledText: AnnotatedString,
    referencePattern: Regex?
): AnnotatedString {
    return buildAnnotatedString {
        append(styledText)

        // 应用自定义引用模式的注解
        referencePattern?.let { regex ->
            regex.findAll(styledText.text).forEach { matchResult ->
                val refId = matchResult.groupValues.getOrNull(1) ?: return@forEach
                val range = matchResult.range
                addStringAnnotation(TAG_REFERENCE, refId, range.first, range.last + 1)
                addStyle(
                    style = SpanStyle(
                        color = Color.Unspecified, // 保持原有颜色
                        textDecoration = TextDecoration.Underline
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}

private data class HtmlTag(
    val fullMatch: String,
    val name: String,
    val attributes: Map<String, String>,
    val isClosing: Boolean,
    val isSelfClosing: Boolean
)

private fun parseTag(tagString: String): HtmlTag {
    val isClosing = tagString.startsWith("</")
    val isSelfClosing = tagString.endsWith("/>")
    val name = tagString.trim('<', '>', '/').substringBefore(' ').lowercase()

    val attributes = if (!isClosing && !isSelfClosing) {
        "\\s*([\\w\\-]+)\\s*=\\s*[\"']([^\"']*)[\"']".toRegex()
            .findAll(tagString)
            .map { it.groupValues[1].lowercase() to it.groupValues[2] }
            .toMap()
    } else {
        emptyMap()
    }
    return HtmlTag(tagString, name, attributes, isClosing, isSelfClosing)
}

private fun HtmlTag.toSpanStyle(currentStyle: SpanStyle, linkColor: Color): SpanStyle {
    var newStyle = currentStyle
    when (name) {
        "b" -> newStyle = newStyle.copy(fontWeight = FontWeight.Bold)
        "i" -> newStyle = newStyle.copy(fontStyle = FontStyle.Italic)
        "u" -> newStyle = newStyle.copy(textDecoration = TextDecoration.Underline)
        "font" -> {
            val color = attributes["color"]?.let(::parseColorValue)
            if (color != null) {
                newStyle = newStyle.copy(color = color)
            }
        }
        "a" -> {
            if (attributes.containsKey("href")) {
                newStyle = newStyle.copy(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
    return newStyle
}

private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}

private fun parseColorValue(color: String): Color? {
    val namedColors = mapOf(
        "red" to Color.Red, "green" to Color.Green, "blue" to Color.Blue,
        "black" to Color.Black, "white" to Color.White, "gray" to Color.Gray,
        "yellow" to Color.Yellow, "purple" to Color(0xFF800080),
        "orange" to Color(0xFFFFA500), "brown" to Color(0xFFA52A2A)
    )
    namedColors[color.lowercase()]?.let { return it }

    if (color.startsWith("#")) {
        try {
            var hex = color.substring(1)
            if (hex.length == 3) hex = hex.map { "$it$it" }.joinToString("")
            return Color(hex.toLong(16) or 0x00000000FF000000)
        } catch (e: NumberFormatException) {
            return null
        }
    }
    return null
}
