package ai.saniou.thread.data.source.nmb.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CdnPath(
    val url: String,//图片 CDN 地址
    val rate: Double,//各个 CDN 地址的权重？
)
