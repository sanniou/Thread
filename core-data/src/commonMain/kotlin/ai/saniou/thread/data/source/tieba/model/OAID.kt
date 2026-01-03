package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAID(
    @SerialName("v")
    val encodedOAID: String = "GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA",
    @SerialName("sc")
    val statusCode: Int = 0,
    @SerialName("sup")
    val support: Int = 1,
    @SerialName("isTrackLimited")
    val isTrackLimited: Int = 0
)
