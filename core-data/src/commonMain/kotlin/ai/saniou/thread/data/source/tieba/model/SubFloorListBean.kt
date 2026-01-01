package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SubFloorListBean ---

@Serializable
data class SubFloorListBean(
    @SerialName("error_code")
    val errorCode: String,
    @SerialName("error_msg")
    val errorMsg: String,
    @SerialName("subpost_list")
    val subPostList: List<PostInfo>?,
    val post: PostInfo?,
    val page: PageInfo?,
    val forum: ForumInfo?,
    val anti: AntiInfo?,
    val thread: ThreadInfo?
) {
    @Serializable
    data class PostInfo(
        val id: String,
        val title: String,
        val floor: String,
        val time: String,
        val content: List<ThreadContentBean.ContentBean>,
        val author: ThreadContentBean.UserInfoBean
    )

    @Serializable
    data class ThreadInfo(
        val id: String,
        val title: String,
        val author: ThreadContentBean.UserInfoBean,
        @SerialName("reply_num")
        val replyNum: String,
        @SerialName("collect_status")
        val collectStatus: String
    )

    @Serializable
    data class AntiInfo(
        val tbs: String
    )

    @Serializable
    data class PageInfo(
        @SerialName("current_page")
        val currentPage: String,
        @SerialName("total_page")
        val totalPage: String,
        @SerialName("total_count")
        val totalCount: String,
        @SerialName("page_size")
        val pageSize: String
    )

    @Serializable
    data class ForumInfo(
        val id: String,
        val name: String,
        @SerialName("is_exists")
        val isExists: String,
        @SerialName("first_class")
        val firstClass: String,
        @SerialName("second_class")
        val secondClass: String,
        @SerialName("is_liked")
        val isLiked: String
    )
}
