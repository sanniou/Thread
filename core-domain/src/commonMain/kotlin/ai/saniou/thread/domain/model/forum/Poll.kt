package ai.saniou.thread.domain.model.forum

/**
 * 投票模型
 *
 * @param id 投票ID
 * @param title 投票标题
 * @param options 选项列表
 * @param totalVotes 总票数
 * @param isMultiSelect 是否多选
 * @param isExpired 是否过期
 * @param userVotedOptions 用户已投选项ID列表
 */
data class Poll(
    val id: String,
    val title: String?,
    val options: List<PollOption>,
    val totalVotes: Long,
    val isMultiSelect: Boolean,
    val isExpired: Boolean,
    val userVotedOptions: List<String> = emptyList()
)

/**
 * 投票选项
 *
 * @param id 选项ID
 * @param text 选项文本
 * @param votes 票数
 * @param percent 百分比 (0-100)
 * @param imageUrl 选项图片URL (可选)
 */
data class PollOption(
    val id: String,
    val text: String,
    val votes: Long,
    val percent: Float,
    val imageUrl: String? = null
)