package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ThreadStoreBean ---

@Serializable
data class ThreadStoreBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    val error: ErrorInfo? = null,
    @SerialName("store_thread")
    val storeThread: List<ThreadStoreInfo>? = null
) {
    @Serializable
    data class ThreadStoreInfo(
        @SerialName("thread_id")
        val threadId: String,
        val title: String,
        @SerialName("forum_name")
        val forumName: String,
        val author: AuthorInfo,
        val media: List<MediaInfo>,
        @SerialName("is_deleted")
        val isDeleted: String,
        @SerialName("last_time")
        val lastTime: String,
        val type: String,
        val status: String,
        @SerialName("max_pid")
        val maxPid: String,
        @SerialName("min_pid")
        val minPid: String,
        @SerialName("mark_pid")
        val markPid: String,
        @SerialName("mark_status")
        val markStatus: String,
        @SerialName("post_no")
        val postNo: String,
        @SerialName("post_no_msg")
        val postNoMsg: String,
        val count: String
    )

    @Serializable
    data class MediaInfo(
        val type: String? = null,
        @SerialName("small_Pic")
        val smallPic: String? = null,
        @SerialName("big_pic")
        val bigPic: String? = null,
        val width: String? = null,
        val height: String? = null
    )

    @Serializable
    data class AuthorInfo(
        @SerialName("lz_uid")
        val lzUid: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        @SerialName("user_portrait")
        val userPortrait: String? = null
    )

    @Serializable
    data class ErrorInfo(
        @SerialName("errno")
        val errorCode: String? = null,
        @SerialName("errmsg")
        val errorMsg: String? = null
    )
}
