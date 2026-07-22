package ai.saniou.thread.domain.model.block

/**
 * Local content block rule (TiebaLite BlockManager-inspired).
 * - [KEYWORD]: hide items whose text contains **all** keywords in [keywords] (AND).
 * - [USER]: hide items by [userId] and/or [userName].
 */
enum class ContentBlockType {
    KEYWORD,
    USER,
}

data class ContentBlock(
    val id: Long = 0L,
    val type: ContentBlockType,
    val keywords: List<String> = emptyList(),
    val userId: String? = null,
    val userName: String? = null,
    val createdAt: Long = 0L,
)

object ContentBlockMatcher {
    fun shouldBlockText(text: String?, rules: List<ContentBlock>): Boolean {
        if (text.isNullOrBlank()) return false
        val hay = text
        return rules.any { rule ->
            rule.type == ContentBlockType.KEYWORD &&
                rule.keywords.isNotEmpty() &&
                rule.keywords.all { kw -> kw.isNotBlank() && hay.contains(kw, ignoreCase = true) }
        }
    }

    fun shouldBlockUser(
        userId: String?,
        userName: String?,
        rules: List<ContentBlock>,
    ): Boolean {
        return rules.any { rule ->
            if (rule.type != ContentBlockType.USER) return@any false
            val idHit = !rule.userId.isNullOrBlank() &&
                !userId.isNullOrBlank() &&
                rule.userId == userId
            val nameHit = !rule.userName.isNullOrBlank() &&
                !userName.isNullOrBlank() &&
                rule.userName.equals(userName, ignoreCase = true)
            idHit || nameHit
        }
    }

    fun shouldBlockContent(
        text: String?,
        userId: String? = null,
        userName: String? = null,
        rules: List<ContentBlock>,
    ): Boolean = shouldBlockText(text, rules) || shouldBlockUser(userId, userName, rules)
}
