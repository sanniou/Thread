package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchPostBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    val page: PageBean? = null,
    @SerialName("post_list")
    val postList: List<ThreadInfoBean>? = null
) {
    @Serializable
    data class PageBean(
        @SerialName("page_size")
        val pageSize: String? = null,
        val offset: String? = null,
        @SerialName("current_page")
        val currentPage: String? = null,
        @SerialName("total_count")
        val totalCount: String? = null,
        @SerialName("total_page")
        val totalPage: String? = null,
        @SerialName("has_more")
        val hasMore: String? = null,
        @SerialName("has_prev")
        val hasPrev: String? = null
    )

    @Serializable
    data class ThreadInfoBean(
        val tid: String? = null,
        val pid: String? = null,
        val title: String? = null,
        val content: String? = null,
        val time: String? = null,
        @SerialName("fname")
        val forumName: String? = null,
        val author: AuthorBean? = null
    )

    @Serializable
    data class AuthorBean(
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null
    )
}
