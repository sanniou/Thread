package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.corecommon.utils.toMD5
import ai.saniou.thread.data.source.tieba.TiebaParameterProvider
import com.huanchengfly.tieba.post.api.models.protos.AppPosInfo
import com.huanchengfly.tieba.post.api.models.protos.CommonRequest
import com.huanchengfly.tieba.post.api.models.protos.frsPage.AdParam
import com.squareup.wire.Message
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.time.Clock

object TiebaProtoBuilder {

    private const val BOUNDARY = "--------7da3d81520810*"

    fun buildProtobufFormBody(
        data: Message<*, *>,
        clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
        needSToken: Boolean = true,
        parameterProvider: TiebaParameterProvider
    ): MultiPartFormDataContent {
        val params = mutableListOf<Pair<String, String>>()

        // Add common params to match cURL/Retrofit implementation
        params.add("BDUSS" to parameterProvider.getBduss())
        params.add("_client_id" to "wappc_1687508826727_436")
        params.add("_client_type" to "2")
        params.add("_client_version" to clientVersion.version)
        params.add("_phone_imei" to "000000000000000") // Fallback
        params.add("c3_aid" to parameterProvider.getAndroidId()) // Using Android ID as placeholder for c3_aid
        params.add("cuid" to parameterProvider.getCuid())
        params.add("cuid_galaxy2" to parameterProvider.getCuid())
        params.add("cuid_gid" to "")
        params.add("from" to "tieba")
        params.add("model" to parameterProvider.getModel())
        params.add("net_type" to "1")
        params.add("oaid" to "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}") // Placeholder
        params.add("timestamp" to parameterProvider.getTimestamp())

        if (needSToken) {
            val sToken = parameterProvider.getSToken()
            if (sToken.isNotEmpty()) params.add("stoken" to sToken)
        }

        // Calculate sign
        val sortedString = params.sortedBy { it.first }.joinToString("") { "${it.first}=${it.second}" }
        val sign = (sortedString + "tiebaclient!!!").toMD5()
        params.add("sign" to sign)

        return MultiPartFormDataContent(
            formData {
                params.forEach { (key, value) ->
                    append(key, value)
                }

                // Add the protobuf data as a file part named "data"
                val encodedData = data.encode()
                append("data", encodedData, Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"data\"; filename=\"file\"")
                })
            },
            boundary = BOUNDARY
        )
    }

    fun buildAdParam(
        load_count: Int = 0,
        refresh_count: Int = 4,
        yoga_lib_version: String? = "1.0"
    ): AdParam {
        return AdParam(
            load_count = load_count,
            refresh_count = refresh_count,
            yoga_lib_version = yoga_lib_version
        )
    }

    fun buildAppPosInfo(): AppPosInfo {
        return AppPosInfo(
            addr_timestamp = 0L,
            ap_connected = true,
            ap_mac = "02:00:00:00:00:00",
            asp_shown_info = "",
            coordinate_type = "BD09LL"
        )
    }

    fun buildCommonRequest(
        parameterProvider: TiebaParameterProvider,
        clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
        bduss: String? = null,
        stoken: String? = null,
        tbs: String? = null,
    ): CommonRequest {
        val currentBduss = bduss ?: parameterProvider.getBduss()
        val currentSToken = "6d39212131010612a58545d887d7a798c9f155cbc8baec65a3878dd5f7e3aaf1"
//        val currentSToken = stoken ?: parameterProvider.getSToken()
        val timestamp = parameterProvider.getTimestamp().toLongOrNull() ?: 0L
        val cuid = parameterProvider.getCuid()

        return when (clientVersion) {
            ClientVersion.TIEBA_V11 -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = "wappc_1687508826727_267", // Using a fixed ID or generate? TiebaLite uses generated.
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30", // SDK 30 (Android 11)
                    _phone_imei = "000000000000000", // Fallback to Android ID
                    _timestamp = timestamp,
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    from = "1024324o",
                    is_teenager = 0,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}", // OAID logic is complex, skipping for now
                    pversion = "1.0.3",
                    sample_id = "", // Skipping
                    stoken = currentSToken,
                )
            }

            ClientVersion.TIEBA_V12 -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = "wappc_1687508826727_267",
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30",
                    _phone_imei = "000000000000000",
                    _timestamp = timestamp,
                    active_timestamp = timestamp, // Using current time
                    android_id = parameterProvider.getAndroidId(), // Base64 encode? TiebaLite does.
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cmode = 1,
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    event_day = "", // Formatting requires date utils, skip or TODO
                    extra = "",
                    first_install_time = timestamp, // Mock
                    framework_ver = "3340042",
                    from = "1020031h",
                    is_teenager = 0,
                    last_update_time = timestamp,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}",
                    personalized_rec_switch = 1,
                    pversion = "1.0.3",
                    q_type = 0,
                    sample_id = "",
                    scr_dip = 3.0,
                    scr_h = 1920,
                    scr_w = 1080,
                    sdk_ver = "2.34.0",
                    start_scheme = "",
                    start_type = 1,
                    stoken = currentSToken,
                    swan_game_ver = "1038000",
                    user_agent = "", // Optional?
                    z_id = ""
                )
            }

            ClientVersion.TIEBA_V12_POST -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = "wappc_1687508826727_267",
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30",
                    _phone_imei = "000000000000000",
                    _timestamp = timestamp,
                    active_timestamp = timestamp,
                    android_id = parameterProvider.getAndroidId(),
                    applist = "",
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cmode = 1,
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    device_score = "0",
                    event_day = "",
                    extra = "",
                    first_install_time = timestamp,
                    framework_ver = "3340042",
                    from = "1020031h",
                    is_teenager = 0,
                    last_update_time = timestamp,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}",
                    personalized_rec_switch = 1,
                    pversion = "1.0.3",
                    q_type = 0,
                    sample_id = "",
                    scr_dip = 3.0,
                    scr_h = 1920,
                    scr_w = 1080,
                    sdk_ver = "2.34.0",
                    start_scheme = "",
                    start_type = 1,
                    stoken = currentSToken,
                    swan_game_ver = "1038000",
                    tbs = tbs,
                    user_agent = "",
                    z_id = ""
                )
            }
        }
    }
}
