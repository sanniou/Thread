package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotTopicBean(
    @SerialName("pmy_topic_ext")
    val pmyTopicExt: String,
    @SerialName("yuren_rand")
    val yurenRand: Int,
    @SerialName("topic_info")
    val topicInfo: TopicInfoBean
) {
    @Serializable
    data class TopicInfoBean(
        val ret: List<TopicInfoRetBean>
    )

    @Serializable
    data class TopicInfoRetBean(
        @SerialName("create_time")
        val createTime: String,
        @SerialName("discuss_num")
        val discussNum: String,
        @SerialName("hot_value")
        val hotValue: String,
        @SerialName("topic_id")
        val topicId: String,
        @SerialName("topic_name")
        val topicName: String,
        @SerialName("topic_desc")
        val topicDesc: String,
        val tids: String,
        @SerialName("real_discuss_num")
        val realDiscussNum: String,
        val extra: TopicInfoRetExtraBean
    )

    @Serializable
    data class TopicInfoRetExtraBean(
        @SerialName("head_pic")
        val headPic: String,
        @SerialName("share_title")
        val shareTitle: String,
        @SerialName("share_pic")
        val sharePic: String,
        @SerialName("topic_tid")
        val topicTid: String
    )
}
