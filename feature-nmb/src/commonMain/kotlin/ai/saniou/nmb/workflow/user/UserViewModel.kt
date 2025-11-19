package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.domain.UserUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(private val userUseCase: UserUseCase) : ScreenModel {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadCookies()
    }

    fun loadCookies() {
        screenModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                _uiState.value = UserUiState.Success(userUseCase.getCookiesList())
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("获取饼干列表失败: ${e.message}")
            }
        }
    }

    fun addCookie(name: String, value: String) {
        screenModelScope.launch {
            try {
                userUseCase.addCookie(name, value)
                loadCookies()
            } catch (e: Exception) {
                // Optionally update UI to show an error message
            }
        }
    }

    fun deleteCookie(cookie: Cookie) {
        screenModelScope.launch {
            try {
                userUseCase.deleteCookie(cookie)
                loadCookies()
            } catch (e: Exception) {
                // Optionally update UI to show an error message
            }
        }
    }

    fun updateCookieOrder(newList: List<Cookie>) {
        _uiState.update {
            (it as? UserUiState.Success)?.copy(cookies = newList) ?: it
        }
        screenModelScope.launch {
            userUseCase.updateCookieSort(newList)
        }
    }
}

sealed class UserUiState {
    object Loading : UserUiState()
    data class Error(val message: String) : UserUiState()
    data class Success(val cookies: List<Cookie>) : UserUiState()
}
