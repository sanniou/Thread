package ai.saniou.thread.domain.model

/**
 * 定义数据源的功能能力，用于 UI 层进行功能降级。
 */
data class SourceCapabilities(
    val supportsChannelCatalog: Boolean = true,
    val supportsFeedAggregation: Boolean = true,
    val supportsSearch: Boolean = false,
    val supportsTopicCreation: Boolean = false,
    val supportsReplies: Boolean = false,
    val supportsAttachments: Boolean = false,
    val supportsUserContent: Boolean = false,
    val supportsLogin: Boolean = false,
    val supportsPagination: Boolean = true,
    val commentPageSize: Int? = null,
    val hasSubComments: Boolean = false, // 是否支持楼中楼
    val hasUpvote: Boolean = false,      // 是否支持点赞
    val hasDownvote: Boolean = false,    // 是否支持点踩
    val hasPoOnly: Boolean = false,      // 是否支持只看PO
    val hasJumpPage: Boolean = false,    // 是否支持跳页
    val hasPoll: Boolean = false,        // 是否支持投票
    val hasHotReplies: Boolean = false,  // 是否支持热评
) {
    companion object {
        val Default = SourceCapabilities()
    }
}
