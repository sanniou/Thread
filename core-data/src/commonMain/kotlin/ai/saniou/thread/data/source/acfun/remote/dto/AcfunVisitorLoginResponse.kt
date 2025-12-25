package ai.saniou.thread.data.source.acfun.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcfunVisitorLoginResponse(
    @SerialName("result")
    val result: Int,
    @SerialName("acSecurity")
    val acSecurity: String? = null,
    @SerialName("userId")
    val userId: Long? = null,
    @SerialName("acfun.api.visitor_st")
    val serviceToken: String? = null
)