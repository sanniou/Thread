package ai.saniou.thread.data.source.tieba

import ai.saniou.corecommon.utils.DeviceUtils
import ai.saniou.thread.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

class TiebaParameterProvider(
    private val accountRepository: AccountRepository
) {
    // Cache for synchronous access required by Ktor interceptors
    private var cachedBduss: String = "lpNRTJpc055fjFpfm90Um5xeURVNnBsU3hoWWQ1S01QdENPWDdERFdtMHFES3RrRVFBQUFBJCQAAAAAAAAAAAEAAABo7csqxM-52LK7zcsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACp~g2Qqf4Nkd"
    private var cachedSToken: String = "6d39212131010612a58545d887d7a798c9f155cbc8baec65a3878dd5f7e3aaf1"
    private var cachedUid: String = "718007656"

    init {
        CoroutineScope(Dispatchers.Main).launch {
            accountRepository.getCurrentAccount("tieba").collectLatest { account ->
                cachedBduss = account?.value ?: ""
                cachedUid = account?.uid ?: ""

                // Parse extra_data for SToken
                account?.extraData?.let { extraDataJson ->
                    try {
                        val json = Json.parseToJsonElement(extraDataJson) as? JsonObject
                        cachedSToken = json?.get("stoken")?.jsonPrimitive?.content ?: ""
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

    fun getCuid(): String = DeviceUtils.getCuid()
    fun getAndroidId(): String = DeviceUtils.getAndroidId()
    fun getModel(): String = DeviceUtils.getModel()
    fun getBrand(): String = DeviceUtils.getBrand()

    fun getTimestamp(): String = Clock.System.now().toEpochMilliseconds().toString()
}
