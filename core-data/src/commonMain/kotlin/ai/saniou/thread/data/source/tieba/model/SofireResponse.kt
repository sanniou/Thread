package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SofireResponse ---

@Serializable
data class SofireResponse(
    val data: String,
    @SerialName("request_id")
    val requestId: Long,
    val skey: String
)

@Serializable
data class SofireResponseData(
    val token: String
)
