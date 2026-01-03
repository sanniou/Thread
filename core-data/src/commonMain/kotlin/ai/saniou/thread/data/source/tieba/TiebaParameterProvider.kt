package ai.saniou.thread.data.source.tieba

import ai.saniou.corecommon.utils.DeviceUtils
import ai.saniou.thread.data.source.tieba.model.OAID
import ai.saniou.thread.domain.repository.AccountRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Clock

class TiebaParameterProvider(
    private val accountRepository: AccountRepository
) {
    // Cache for synchronous access required by Ktor interceptors
    private var cachedBduss: String = "lpNRTJpc055fjFpfm90Um5xeURVNnBsU3hoWWQ1S01QdENPWDdERFdtMHFES3RrRVFBQUFBJCQAAAAAAAAAAAEAAABo7csqxM-52LK7zcsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACp~g2Qqf4Nkd"
    private var cachedSToken: String = "6d39212131010612a58545d887d7a798c9f155cbc8baec65a3878dd5f7e3aaf1"
    private var cachedUid: String = "718007656"
    private var cachedZid: String = ""

    // Initialized once per app run, mimicking RetrofitTiebaApi
    private val initTime = Clock.System.now().toEpochMilliseconds()
//    private val randomClientId = "wappc_${initTime}_${(Random.nextDouble() * 1000).roundToInt()}"
    private val randomClientId = "wappc_1686339341229_436"

    init {
        CoroutineScope(Dispatchers.Main).launch {
            accountRepository.getCurrentAccount("tieba").collectLatest { account ->
                cachedBduss = account?.value ?: ""
                cachedUid = account?.uid ?: ""

                // Parse extra_data for SToken and Zid
                account?.extraData?.let { extraDataJson ->
                    try {
                        val json = Json.parseToJsonElement(extraDataJson) as? JsonObject
                        cachedSToken = json?.get("stoken")?.jsonPrimitive?.content ?: ""
                        cachedZid = json?.get("zid")?.jsonPrimitive?.content ?: ""
                    } catch (e: Exception) {
                        // Ignore parse error
                    }
                }
            }
        }
    }

    fun getBduss(): String = cachedBduss

    fun getSToken(): String = cachedSToken

    fun getUid(): String = cachedUid

    fun getZid(): String = cachedZid

    fun getCuid(): String = DeviceUtils.getCuid()
    fun getAndroidId(base64: Boolean = false): String {
        val aid = DeviceUtils.getAndroidId()
        return if (base64) aid.encodeBase64() else aid
    }
    fun getModel(): String = DeviceUtils.getModel()
    fun getBrand(): String = DeviceUtils.getBrand()

    fun getClientId(): String = randomClientId

    fun getOaid(): String {
        val oaid = OAID()
        return try {
            Json.encodeToString(oaid)
            "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}"
        } catch (e: Exception) {
            e.printStackTrace()
            "{\"v\":\"GQYTOOLEGNTDCMLDG4ZTONJTGI4GMODEGBRWGYRXHBRDGMDEMY3TIODFGU2TEZRZGU4DQNRYGI4WKMRTGY2WMZBSMY3GKYTGGFRWIMA\",\"isTrackLimited\":0,\"sc\":0,\"sup\":1}"
        }
    }

    // TODO: Need proper implementation for screen metrics
    fun getScreenDensity(): Double = 3.0
    fun getScreenHeight(): Int = 2400
    fun getScreenWidth(): Int = 1080

    fun getTimestamp(): String = Clock.System.now().toEpochMilliseconds().toString()

    // Mock values for now, could be persisted
    fun getAppFirstInstallTime(): String = initTime.toString()
    fun getAppLastUpdateTime(): String = initTime.toString()
    fun getDeviceScore(): String = "0"
    fun getSampleId(): String = ""
    fun getActiveTimestamp(): String = getTimestamp() // Should ideally track user activity
}
