package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ForumRecommend ---

@Serializable
data class ForumRecommend(
    @SerialName("error_code")
    val errorCode: String,
    @SerialName("error_msg")
    val errorMsg: String,
    @SerialName("like_forum")
    val likeForum: List<LikeForum>
) {
    @Serializable
    data class LikeForum(
        @SerialName("forum_id")
        val forumId: String,
        @SerialName("forum_name")
        val forumName: String,
        @SerialName("level_id")
        val levelId: String,
        @SerialName("is_sign")
        val isSign: String,
        val avatar: String
    )
}
