package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Sync ---

@Serializable
data class Sync(
    val client: Client,
    @SerialName("wl_config")
    val wlConfig: WlConfig
) {
    @Serializable
    data class Client(
        @SerialName("client_id")
        val clientId: String
    )

    @Serializable
    data class WlConfig(
        @SerialName("sample_id")
        val sampleId: String
    )
}
