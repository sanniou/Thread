package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SearchUserBean ---

@Serializable
data class SearchUserBean(
    @SerialName("no")
    val errorCode: Int? = null,
    @SerialName("error")
    val errorMsg: String? = null,
    val data: SearchUserDataBean? = null
) {
    @Serializable
    data class SearchUserDataBean(
        @SerialName("pn")
        val pageNum: Int? = null,
        @SerialName("has_more")
        val hasMore: Int = 0,
        @SerialName("exact_match")
        val exactMatch: UserBean? = null,
        @SerialName("fuzzy_match")
        val fuzzyMatch: List<UserBean>? = null
    )

    @Serializable
    data class UserBean(
        val id: String? = null,
        val intro: String? = null,
        @SerialName("user_nickname")
        val userNickname: String? = null,
        @SerialName("show_nickname")
        val showNickname: String? = null,
        val name: String? = null,
        val portrait: String? = null,
        @SerialName("fans_num")
        val fansNum: String? = null,
        @SerialName("has_concerned")
        val hasConcerned: Int = 0
    )
}
