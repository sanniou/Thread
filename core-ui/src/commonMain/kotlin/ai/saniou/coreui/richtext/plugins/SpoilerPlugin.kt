package ai.saniou.coreui.richtext.plugins

import ai.saniou.coreui.richtext.RichTextPlugin

/**
 * Replaces `[h]` and `[/h]` with `<spoiler>` and `</spoiler>`.
 */
class SpoilerPlugin : RichTextPlugin {
    override fun transform(text: String): String {
        return text.replace(Regex("\\[h]", RegexOption.IGNORE_CASE), "<spoiler>")
            .replace(Regex("\\[/h]", RegexOption.IGNORE_CASE), "</spoiler>")
    }
}