package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotTopicMainBean(
    @SerialName("best_info")
    val bestInfo: BestInfoBean
) {
    @Serializable
    data class BestInfoBean(
        val ret: List<BestInfoRetBean>
    )

    @Serializable
    data class BestInfoRetBean(
        @SerialName("common_type")
        val commonType: String,
        @SerialName("module_name")
        val moduleName: String,
        @SerialName("module_recoms")
        val moduleRecoms: List<String>,
        @SerialName("thread_list")
        val threadList: Map<String, ThreadBean>,
        @SerialName("recom_type")
        val recomType: String,
        @SerialName("topic_id")
        val topicId: String
    )

    @Serializable
    data class ThreadBean(
        @SerialName("abstract")
        val abstracts: String,
        @SerialName("agree_num")
        val agreeNum: String,
        val avatar: String,
        @SerialName("create_time")
        val createTime: String,
        @SerialName("forum_id")
        val forumId: String,
        @SerialName("forum_name")
        val forumName: String,
        val media: List<MediaBean>,
        @SerialName("name_show")
        val nameShow: String,
        @SerialName("post_num")
        val postNum: String,
        @SerialName("thread_id")
        val threadId: String,
        @SerialName("user_id")
        val userId: String,
        val title: String
    )

    @Serializable
    data class MediaBean(
        @SerialName("big_pic")
        val bigPic: String,
        val height: Int,
        val width: Int,
        @SerialName("small_pic")
        val smallPic: String,
        val type: String,
        @SerialName("water_pic")
        val waterPic: String
    )
}
