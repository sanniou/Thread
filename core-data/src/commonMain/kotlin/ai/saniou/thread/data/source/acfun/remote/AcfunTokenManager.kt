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
        token: String,
        acSecurity: String
    ) {
        mutex.withLock {
            _cookie.value = cookie
            _token.value = token
            _acSecurity.value = acSecurity
            // TODO: Persist sensitive tokens if needed (using secure storage)
            settingsRepository.saveValue(KEY_COOKIE, cookie)
            settingsRepository.saveValue(KEY_TOKEN, token)
            settingsRepository.saveValue(KEY_AC_SECURITY, acSecurity)
        }
    }
    
    suspend fun loadTokens() {
        mutex.withLock {
             _cookie.value = settingsRepository.getValue<String>(KEY_COOKIE)
             _token.value = settingsRepository.getValue<String>(KEY_TOKEN)
             _acSecurity.value = settingsRepository.getValue<String>(KEY_AC_SECURITY)
        }
    }
    
    suspend fun getCookie(): String? = _cookie.value ?: run { loadTokens(); _cookie.value }
    suspend fun getToken(): String? = _token.value ?: run { loadTokens(); _token.value }
    suspend fun getAcSecurity(): String? = _acSecurity.value ?: run { loadTokens(); _acSecurity.value }


    companion object {
        private const val KEY_UDID = "acfun_udid"
        private const val KEY_COOKIE = "acfun_cookie"
        private const val KEY_TOKEN = "acfun_token"
        private const val KEY_AC_SECURITY = "acfun_ac_security"
    }
}