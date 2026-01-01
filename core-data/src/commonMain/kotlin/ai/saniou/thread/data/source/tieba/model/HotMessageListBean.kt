package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotMessageListBean(
    @SerialName("no")
    val errorCode: Int,
    @SerialName("error")
    val errorMsg: String,
    val data: HotMessageListDataBean
) {
    @Serializable
    data class HotMessageListDataBean(
        val list: DataListBean
    )

    @Serializable
    data class DataListBean(
        val ret: List<HotMessageRetBean>
    )

    @Serializable
    data class HotMessageRetBean(
        @SerialName("mul_id")
        val mulId: String,
        @SerialName("mul_name")
        val mulName: String,
        @SerialName("topic_info")
        val topicInfo: TopicInfoBean
    )

    @Serializable
    data class TopicInfoBean(
        @SerialName("topic_desc")
        val topicDesc: String
    )
}
