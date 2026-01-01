package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- UserLikeForumBean ---

@Serializable
data class UserLikeForumBean(
    @SerialName("error_code")
    val errorCode: String = "-1",
    @SerialName("error_msg")
    val errorMsg: String = "unknown error",
    @SerialName("has_more")
    val hasMore: String = "0",
    @SerialName("forum_list")
    val forumList: ForumListBean = ForumListBean(),
    @SerialName("common_forum_list")
    val commonForumList: ForumListBean = ForumListBean()
) {
    @Serializable
    data class ForumListBean(
        @SerialName("non-gconforum")
        val forumList: List<ForumBean> = emptyList()
    )

    @Serializable
    data class ForumBean(
        val id: String = "",
        val name: String? = null,
        @SerialName("level_id")
        val levelId: String? = null,
        @SerialName("favo_type")
        val favoType: String? = null,
        @SerialName("level_name")
        val levelName: String? = null,
        @SerialName("cur_score")
        val curScore: String? = null,
        @SerialName("levelup_score")
        val levelUpScore: String? = null,
        val avatar: String? = null,
        val slogan: String? = null
    )
}
