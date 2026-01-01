package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotTopicThreadBean(
    @SerialName("thread_list")
    val threadList: List<HotTopicMainBean.ThreadBean>
)
