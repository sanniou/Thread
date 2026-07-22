package ai.saniou.thread.domain.model.forum

/**
 * Read-only forum rules (e.g. Tieba 吧规).
 */
data class ForumRuleDetail(
    val channelId: String,
    val title: String,
    val preface: String = "",
    val publishTime: String = "",
    val rules: List<ForumRuleItem> = emptyList(),
)

data class ForumRuleItem(
    val title: String,
    val content: String,
)
