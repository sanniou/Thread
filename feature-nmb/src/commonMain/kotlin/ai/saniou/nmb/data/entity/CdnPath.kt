package ai.saniou.nmb.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class CdnPath(
    val url: String,//图片 CDN 地址
    val rate: Double,//各个 CDN 地址的权重？
)
