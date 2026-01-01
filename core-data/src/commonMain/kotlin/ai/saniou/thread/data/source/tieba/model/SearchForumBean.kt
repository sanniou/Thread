package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SearchForumBean ---

@Serializable
data class SearchForumBean(
    @SerialName("no")
    val errorCode: Int? = null,
    @SerialName("error")
    val errorMsg: String? = null,
    val data: DataBean? = null
) {
    @Serializable
    data class ForumInfoBean(
        @SerialName("forum_id")
        val forumId: Long? = null,
        @SerialName("forum_name")
        val forumName: String? = null,
        @SerialName("forum_name_show")
        val forumNameShow: String? = null,
        val avatar: String? = null,
        @SerialName("post_num")
        val postNum: String = "0",
        @SerialName("concern_num")
        val concernNum: String = "0",
        @SerialName("has_concerned")
        val hasConcerned: Int = 0,
        val intro: String? = null,
        val slogan: String? = null,
        @SerialName("is_jiucuo")
        val isJiuCuo: Int? = null
    )

    @Serializable
    data class DataBean(
        @SerialName("has_more")
        val hasMore: Int = 0,
        @SerialName("pn")
        val page: Int = 0,
        @SerialName("fuzzy_match")
        val fuzzyMatch: List<ForumInfoBean> = emptyList(),
        @SerialName("exact_match")
        val exactMatch: ForumInfoBean? = null
    )
}
