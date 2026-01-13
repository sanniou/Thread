package ai.saniou.thread.domain.model

/**
 * 定义数据源的功能能力，用于 UI 层进行功能降级。
 */
data class SourceCapabilities(
    val hasSubComments: Boolean = false, // 是否支持楼中楼
    val hasUpvote: Boolean = false,      // 是否支持点赞
    val hasDownvote: Boolean = false,    // 是否支持点踩
    val hasPoOnly: Boolean = false,      // 是否支持只看PO
    val hasJumpPage: Boolean = false,    // 是否支持跳页
    val hasPoll: Boolean = false,        // 是否支持投票
    val hasHotReplies: Boolean = false,  // 是否支持热评
) {
    companion object {
        // 默认能力 (最基础)
        val Default = SourceCapabilities()

        // 预定义常见源的能力 (可以在 Data 层根据 sourceId 动态匹配，也可以硬编码在这里作为参考)
        val Nmb = SourceCapabilities(
            hasSubComments = false, // A岛/X岛通常没有原生楼中楼，或者只是引用
            hasUpvote = false,
            hasDownvote = false,
            hasPoOnly = true,
            hasJumpPage = true
        )
        
        val Tieba = SourceCapabilities(
            hasSubComments = true,
            hasUpvote = true,
            hasDownvote = true,
            hasPoOnly = true,
            hasJumpPage = true
        )
    }
}