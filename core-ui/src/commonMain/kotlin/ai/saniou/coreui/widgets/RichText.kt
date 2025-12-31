package ai.saniou.coreui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

private const val TAG_URL = "URL"
private const val TAG_SPOILER = "SPOILER"
private const val TAG_SPOILER_HIDDEN = "SPOILER_HIDDEN"

/**
 * 定义一个可点击的文本模式
 *
 * @param tag 此模式在 AnnotatedString 中的唯一标识符
 * @param regex 用于查找匹配项的正则表达式。通常，捕获组1 (`groupValues[1]`) 的内容会被用作回调参数，如果不存在则使用整个匹配项。
 * @param onClick 点击匹配项时触发的回调，参数为匹配到的文本
 */
data class ClickablePattern(
    val tag: String,
    val regex: Regex,
    val onClick: (String) -> Unit,
)

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
 * - 隐藏内容/黑条: `[h]content[/h]`，点击可切换显示/隐藏状态
 * - 自定义引用匹配: 通过 `referencePattern` 实现对特定模式（如 `>>No.12345 or /t/50000001 or >>12345`）的点击支持
 * - 空白行处理: 通过 `blankLinePolicy` 控制空白行的显示
 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    clickablePatterns: List<ClickablePattern> = emptyList(),
    onLinkClick: ((String) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    blankLinePolicy: BlankLinePolicy = BlankLinePolicy.KEEP,
    color: Color = Color.Unspecified,
    spoilerBackgroundColor: Color = MaterialTheme.colorScheme.primary,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val linkClickHandler = onLinkClick ?: { url -> uriHandler.openUri(url) }

    val revealedSpoilers = remember { mutableStateListOf<Int>() }

    // 预处理：将 [h] 转换为 <spoiler> 标签
    val processedText = remember(text) {
        text.replace(Regex("\\[h]", RegexOption.IGNORE_CASE), "<spoiler>")
            .replace(Regex("\\[/h]", RegexOption.IGNORE_CASE), "</spoiler>")
    }

    val annotatedString = remember(
        processedText,
        clickablePatterns,
        style,
        linkColor,
        blankLinePolicy,
        revealedSpoilers.toList(),
        spoilerBackgroundColor
    ) {
        val styledText = parseHtml(
            html = processedText,
            linkColor = linkColor,
            blankLinePolicy = blankLinePolicy,
            onLinkClick = linkClickHandler,
            revealedSpoilers = revealedSpoilers.toSet(),
            onSpoilerClick = { id ->
                if (id in revealedSpoilers) revealedSpoilers.remove(id) else revealedSpoilers.add(id)
            },
            spoilerBackgroundColor = spoilerBackgroundColor
        )
        applyClickableAnnotations(styledText, clickablePatterns, linkColor)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        color = color
    )
}

/**
 * 第一步: 解析HTML标签并应用基础样式
 */
private fun parseHtml(
    html: String,
    linkColor: Color,
    blankLinePolicy: BlankLinePolicy,
    onLinkClick: (String) -> Unit,
    revealedSpoilers: Set<Int>,
    onSpoilerClick: (Int) -> Unit,
    spoilerBackgroundColor: Color,
): AnnotatedString {
    // 1. 预处理
    var cleanHtml = html
        // 1.1 处理结构化标签 (在解码实体之前处理，以免实体解码产生干扰)
        // 策略：对于块级元素，替换为换行符的同时，吞噬周围的空白字符（包括源码中的换行符），
        // 从而由我们就地控制垂直间距，避免源码格式化导致的意外空行。

        // <br>: 换行
        .replace(Regex("<br\\s*/?>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")
        // <p>, <div>: 简单换行
        .replace(Regex("[\\s\\r\\n]*</?p[^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("[\\s\\r\\n]*</?div[^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")
        // <li>: 换行 + 点。只处理 <li> 标签本身的前导空白，确保每个 li 另起一行。
        // </li>: 仅移除标签，不处理周围空白，以免误删紧随其后的下一行 li 的换行符。
        .replace(Regex("[\\s\\r\\n]*<li[^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n• ")
        .replace(Regex("</li[^>]*>", RegexOption.IGNORE_CASE), "")
        // <ul>, <ol>: 列表容器换行
        .replace(Regex("[\\s\\r\\n]*</?ul[^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("[\\s\\r\\n]*</?ol[^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")
        // <h1-6>: 标题前后双换行
        .replace(Regex("[\\s\\r\\n]*<h[1-6][^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("[\\s\\r\\n]*</h[1-6][^>]*>[\\s\\r\\n]*", RegexOption.IGNORE_CASE), "\n")

    // 1.2 解码HTML实体
    cleanHtml = decodeHtmlEntities(cleanHtml)

    // 1.5. 根据策略处理空白行
    cleanHtml = when (blankLinePolicy) {
        BlankLinePolicy.KEEP -> cleanHtml
        BlankLinePolicy.COLLAPSE -> cleanHtml.replace(Regex("(\\n\\s*){2,}"), "\n\n")
        BlankLinePolicy.REMOVE -> cleanHtml.lines().filter { it.isNotBlank() }.joinToString("\n")
    }

    // Trim leading/trailing whitespace after tag processing
    cleanHtml = cleanHtml.trim()


    // 2. 使用正则表达式将HTML分词为标签和文本
    val tokenizer = Regex("(<[^>]+>)|([^<]+)")
    val tokens = tokenizer.findAll(cleanHtml)

    // 3. 使用栈来处理嵌套样式
    val styleStack = mutableListOf(SpanStyle())
    var spoilerCounter = 0
    // 跟踪隐藏 Spoiler 的嵌套深度，用于在隐藏时禁用内部链接和颜色样式
    var hiddenSpoilerDepth = 0
    val spoilerStack = mutableListOf<Boolean>() // true if hidden

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
                        if (tag.name == "a") {
                            // 仅当链接不在隐藏的剧透内容中时，它的注解才会被推入。
                            // 因此，只有在这种情况下才需要弹出。
                            if (hiddenSpoilerDepth == 0) {
                                try {
                                    pop()
                                } catch (e: Exception) {

                                }
                            }
                        }
                        if (tag.name == "spoiler") {
                            pop() // pop spoiler link
                            val wasHidden = spoilerStack.removeLastOrNull() ?: false
                            if (wasHidden) hiddenSpoilerDepth--
                        }
                    }

                    !tag.isSelfClosing -> {
                        if (tag.name == "spoiler") {
                            val id = spoilerCounter++
                            val isRevealed = id in revealedSpoilers
                            val isHidden = !isRevealed

                            if (isHidden) hiddenSpoilerDepth++
                            spoilerStack.add(isHidden)

                            val tagString = if (isHidden) TAG_SPOILER_HIDDEN else TAG_SPOILER
                            pushLink(
                                LinkAnnotation.Clickable(
                                    tagString,
                                    linkInteractionListener = LinkInteractionListener {
                                        onSpoilerClick(
                                            id
                                        )
                                    }
                                )
                            )

                            val currentStyle = styleStack.last()
                            val newStyle = if (isHidden) {
                                currentStyle.copy(
                                    background = spoilerBackgroundColor,
                                    color = Color.Transparent
                                )
                            } else {
                                currentStyle
                            }
                            styleStack.add(newStyle)
                        } else {
                            // 开始标签, 推入新样式
                            styleStack.add(
                                tag.toSpanStyle(
                                    styleStack.last(),
                                    linkColor,
                                    ignoreColor = hiddenSpoilerDepth > 0
                                )
                            )
                            // 如果是链接，且不在隐藏的spoiler中，推入注解
                            if (tag.name == "a" && hiddenSpoilerDepth == 0) {
                                tag.attributes["href"]?.let { href ->
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            TAG_URL,
                                            linkInteractionListener = LinkInteractionListener {
                                                onLinkClick(
                                                    href
                                                )
                                            }
                                        )
                                    )
                                }
                            } else if (tag.name == "a") {
                                // 如果在隐藏区域，不推入Link
                            }
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
    clickablePatterns: List<ClickablePattern>,
    linkColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        append(styledText)

        // 获取所有已存在的注解范围
        val existingAnnotations = styledText.getLinkAnnotations(0, styledText.length)

        // 找出所有隐藏的 spoiler 范围
        val hiddenRanges = existingAnnotations
            .filter { (it.item as? LinkAnnotation.Clickable)?.tag == TAG_SPOILER_HIDDEN }
            .map { it.start until it.end }

        // 找出所有已存在的 URL 范围（避免重叠）
        val existingUrlRanges = existingAnnotations
            .filter { (it.item as? LinkAnnotation.Clickable)?.tag == TAG_URL }
            .map { it.start until it.end }

        // 跟踪已被占用的范围（包括现有的 URL 和新匹配到的模式），防止重叠
        val occupiedRanges = existingUrlRanges.toMutableList()

        clickablePatterns.forEach { pattern ->
            pattern.regex.findAll(styledText.text).forEach { matchResult ->
                val range = matchResult.range

                // 检查：
                // 1. 不在隐藏的 spoiler 中
                // 2. 不与已被占用的范围重叠
                val isHidden = hiddenRanges.any { it.overlaps(range) }
                val isOverlapping = occupiedRanges.any { it.overlaps(range) }

                if (!isHidden && !isOverlapping) {
                    val clickableText = matchResult.groupValues.getOrNull(1) ?: matchResult.value
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = pattern.tag,
                            linkInteractionListener = LinkInteractionListener {
                                pattern.onClick(clickableText)
                            }),
                        start = range.first,
                        end = range.last + 1
                    )
                    addStyle(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = range.first,
                        end = range.last + 1
                    )
                    occupiedRanges.add(range)
                }
            }
        }
    }
}

/**
 * 检查两个范围是否存在重叠
 */
private fun IntRange.overlaps(other: IntRange): Boolean {
    return first <= other.last && last >= other.first
}

private data class HtmlTag(
    val fullMatch: String,
    val name: String,
    val attributes: Map<String, String>,
    val isClosing: Boolean,
    val isSelfClosing: Boolean,
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

private fun HtmlTag.toSpanStyle(
    currentStyle: SpanStyle,
    linkColor: Color,
    ignoreColor: Boolean = false,
): SpanStyle {
    var newStyle = currentStyle
    when (name) {
        "b", "strong" -> newStyle = newStyle.copy(fontWeight = FontWeight.Bold)
        "i", "em" -> newStyle = newStyle.copy(fontStyle = FontStyle.Italic)
        "u" -> newStyle = newStyle.copy(textDecoration = TextDecoration.Underline)
        "s", "strike", "del" -> newStyle =
            newStyle.copy(textDecoration = TextDecoration.LineThrough)

        "font" -> {
            if (!ignoreColor) {
                val color = attributes["color"]?.let(::parseColorValue)
                if (color != null) {
                    newStyle = newStyle.copy(color = color)
                }
            }
        }

        "a" -> {
            if (attributes.containsKey("href") && !ignoreColor) {
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
        // 常见符号
        .replace("&bull;", "•")
        .replace("&middot;", "·")
        .replace("&copy;", "©")
        .replace("&reg;", "®")
        .replace("&trade;", "™")
        // 标点符号
        .replace("&ndash;", "–")
        .replace("&mdash;", "—")
        .replace("&lsquo;", "‘")
        .replace("&rsquo;", "’")
        .replace("&ldquo;", "“")
        .replace("&rdquo;", "”")
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
