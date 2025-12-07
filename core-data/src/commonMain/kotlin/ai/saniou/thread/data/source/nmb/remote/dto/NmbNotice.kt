package ai.saniou.thread.data.source.nmb.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NmbNotice(
    val content: String,//公告内容，使用 HTML
    val date: Long,//公告发布时间，可以据此判断是否为新公告
    val enable: Boolean,//?
)
