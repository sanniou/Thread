package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.data.entity.Cookie
import ai.saniou.nmb.domain.UserUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class UserViewModel(private val userUseCase: UserUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState = _uiState.asStateFlow()

    var currentTabIndex = 0
        private set

    private var email = ""
    private var password = ""
    private var passwordConfirm = ""
    private var verifyCode = ""
    private var verifyImageUrl = "https://www.nmbxd.com/Member/User/Index/verify.html"

    init {
        refreshCookies()
    }

    fun setCurrentTabIndex(index: Int) {
        currentTabIndex = index
        when (index) {
            0 -> refreshCookies()
            1 -> _uiState.value = UserUiState.Login(
                email = email,
                password = password,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )

            2 -> _uiState.value = UserUiState.Register(
                email = email,
                password = password,
                passwordConfirm = passwordConfirm,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )
        }
    }

    fun refreshCookies() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val cookies = userUseCase.getCookiesList()
                _uiState.value = UserUiState.CookieList(cookies)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("获取饼干列表失败: ${e.message}")
            }
        }
    }

    fun applyNewCookie() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val result = userUseCase.applyNewCookie()
                // 刷新饼干列表
                refreshCookies()
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("申请新饼干失败: ${e.message}")
            }
        }
    }

    fun updateEmail(value: String) {
        email = value
        updateLoginOrRegisterState()
    }

    fun updatePassword(value: String) {
        password = value
        updateLoginOrRegisterState()
    }

    fun updatePasswordConfirm(value: String) {
        passwordConfirm = value
        updateLoginOrRegisterState()
    }

    fun updateVerifyCode(value: String) {
        verifyCode = value
        updateLoginOrRegisterState()
    }

    fun refreshVerifyCode() {
        // 刷新验证码
        verifyImageUrl = "https://www.nmbxd.com/Member/User/Index/verify.html?t=${
            Clock.System.now().toEpochMilliseconds()
        }"
        updateLoginOrRegisterState()
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val response = userUseCase.login(email, password, verifyCode)
                if (response.success) {
                    // 登录成功，切换到饼干列表
                    setCurrentTabIndex(0)
                } else {
                    _uiState.value = UserUiState.Error("登录失败: ${response.message}")
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("登录失败: ${e.message}")
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val result = userUseCase.register(email, password, passwordConfirm, verifyCode)
                if (result.contains("成功")) {
                    // 注册成功，切换到登录
                    setCurrentTabIndex(1)
                } else {
                    _uiState.value = UserUiState.Error("注册失败: $result")
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("注册失败: ${e.message}")
            }
        }
    }

    fun retry() {
        when (currentTabIndex) {
            0 -> refreshCookies()
            1 -> _uiState.value = UserUiState.Login(
                email = email,
                password = password,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )

            2 -> _uiState.value = UserUiState.Register(
                email = email,
                password = password,
                passwordConfirm = passwordConfirm,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )
        }
    }

    private fun updateLoginOrRegisterState() {
        when (currentTabIndex) {
            1 -> _uiState.value = UserUiState.Login(
                email = email,
                password = password,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )

            2 -> _uiState.value = UserUiState.Register(
                email = email,
                password = password,
                passwordConfirm = passwordConfirm,
                verifyCode = verifyCode,
                verifyImageUrl = verifyImageUrl
            )
        }
    }
}

sealed class UserUiState {
    object Loading : UserUiState()
    data class Error(val message: String) : UserUiState()
    data class CookieList(val cookies: List<Cookie>) : UserUiState()
    data class Login(
        val email: String,
        val password: String,
        val verifyCode: String,
        val verifyImageUrl: String
    ) : UserUiState()

    data class Register(
        val email: String,
        val password: String,
        val passwordConfirm: String,
        val verifyCode: String,
        val verifyImageUrl: String
    ) : UserUiState()
}
