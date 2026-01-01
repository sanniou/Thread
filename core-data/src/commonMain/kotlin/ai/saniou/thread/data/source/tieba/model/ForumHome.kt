package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Web Models ---

@Serializable
data class ForumHome(
    @SerialName("like_forum")
    val likeForum: LikeForum
) {
    @Serializable
    data class LikeForum(
        val list: List<ListItem>,
        val page: Page
    ) {
        @Serializable
        data class ListItem(
            val avatar: String,
            @SerialName("forum_id")
            val forumId: Long,
            @SerialName("forum_name")
            val forumName: String,
            @SerialName("hot_num")
            val hotNum: Long,
            @SerialName("is_brand_forum")
            val isBrandForum: Int,
            @SerialName("level_id")
            val levelId: Int
        )

        @Serializable
        data class Page(
            val currentPage: Int,
            val totalPage: Int
        )
    }
}
