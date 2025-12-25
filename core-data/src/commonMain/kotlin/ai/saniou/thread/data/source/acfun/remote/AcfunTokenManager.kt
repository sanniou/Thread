package ai.saniou.thread.data.source.acfun.remote

import ai.saniou.corecommon.utils.UuidUtils
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AcfunTokenManager(
    private val settingsRepository: SettingsRepository
) {
    private val _udid = MutableStateFlow<String?>(null)
    val udid: StateFlow<String?> = _udid.asStateFlow()

    private val _cookie = MutableStateFlow<String?>(null)
    val cookie: StateFlow<String?> = _cookie.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _acSecurity = MutableStateFlow<String?>(null)
    val acSecurity: StateFlow<String?> = _acSecurity.asStateFlow()

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    private val mutex = Mutex()

    suspend fun getUdid(): String {
        return _udid.value ?: mutex.withLock {
            _udid.value ?: run {
                val savedUdid = settingsRepository.getValue<String>(KEY_UDID)
                if (savedUdid != null) {
                    _udid.value = savedUdid
                    savedUdid
                } else {
                    val newUdid = UuidUtils.randomUuid()
                    settingsRepository.saveValue(KEY_UDID, newUdid)
                    _udid.value = newUdid
                    newUdid
                }
            }
        }
    }

    suspend fun setTokens(
        cookie: String,
        token: String?,
        acSecurity: String?,
        userId: Long?
    ) {
        mutex.withLock {
            _cookie.value = cookie
            _token.value = token
            _acSecurity.value = acSecurity
            _userId.value = userId

            settingsRepository.saveValue(KEY_COOKIE, cookie)
            if (token != null) settingsRepository.saveValue(KEY_TOKEN, token)
            if (acSecurity != null) settingsRepository.saveValue(KEY_AC_SECURITY, acSecurity)
            if (userId != null) settingsRepository.saveValue(KEY_USER_ID, userId)
        }
    }

    suspend fun loadTokens() {
        mutex.withLock {
            _cookie.value = settingsRepository.getValue<String>(KEY_COOKIE)
            _token.value = settingsRepository.getValue<String>(KEY_TOKEN)
            _acSecurity.value = settingsRepository.getValue<String>(KEY_AC_SECURITY)
            _userId.value = settingsRepository.getValue<Long>(KEY_USER_ID)
        }
    }

    suspend fun getCookie(): String? = _cookie.value ?: run { loadTokens(); _cookie.value }
    suspend fun getToken(): String? = _token.value ?: run { loadTokens(); _token.value }
    suspend fun getAcSecurity(): String? = _acSecurity.value ?: run { loadTokens(); _acSecurity.value }

    suspend fun buildVisitorCookie(acSecurity: String, userId: Long, serviceToken: String): String {
        val udid = getUdid()
        // did=eeb4d228-fbce-35e2-99a8-f315da8d0a06;safety_id=AAKF9RgCxTeIVoWwux-2Sl5A
        // ;acSecurity=HWH9i0fGGpB00S8nOHMiZg==;userId=1000000258636841;acfun.api.visitor_st=ChRhY2Z1bi5hcGkudmlzaXRvci5zdBJwbT0de9VeO2KZE3ddiJ_N4-aK4IaqpJU_bNx1mcu2DeSuyZ8PHPFdTQcFheToAjVlLVy0FIMW1jxFCi2H2c1bxt4rkGGKWL_XrLR2ltNRtV05zRlncHfHl3s-2NEAXd8Z0jm_CAR_0VAsQpl_3z5_ehoStq8KBd_X3fCH8_FUsjZpiYZSIiAqFVPj6WymXh5sai6DhvBmgIvkvSBRq6MH9fd4bmFuNCgFMAE
        return buildString {
            append("did=$udid")
            append(";safety_id=AAFAsQ04RM6Acm0WUcbfyJ5Q") // Hardcoded safety_id as seen in AcService.qml
            append(";acSecurity=$acSecurity")
            append(";userId=$userId")
            append(";acfun.api.visitor_st=$serviceToken")
        }
    }

    companion object {
        private const val KEY_UDID = "acfun_udid"
        private const val KEY_COOKIE = "acfun_cookie"
        private const val KEY_TOKEN = "acfun_token"
        private const val KEY_AC_SECURITY = "acfun_ac_security"
        private const val KEY_USER_ID = "acfun_user_id"
    }
}