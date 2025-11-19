package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.domain.UserUseCase
import ai.saniou.nmb.workflow.user.UserContract.Effect
import ai.saniou.nmb.workflow.user.UserContract.Event
import ai.saniou.nmb.workflow.user.UserContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(private val userUseCase: UserUseCase) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        handleEvent(Event.LoadCookies)
    }

    fun handleEvent(event: Event) {
        when (event) {
            is Event.LoadCookies -> loadCookies()
            is Event.AddCookie -> addCookie(event.name, event.value)
            is Event.DeleteCookie -> deleteCookie(event.cookie)
            is Event.UpdateCookieOrder -> updateCookieOrder(event.cookies)
        }
    }

    private fun loadCookies() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val cookies = userUseCase.getCookiesList()
                _state.update { it.copy(isLoading = false, cookies = cookies) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "获取饼干列表失败: ${e.message}") }
            }
        }
    }

    private fun addCookie(name: String, value: String) {
        screenModelScope.launch {
            try {
                userUseCase.addCookie(name, value)
                loadCookies() // 重新加载以获取包含新 cookie 的完整列表
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("添加饼干失败: ${e.message}"))
            }
        }
    }

    private fun deleteCookie(cookie: Cookie) {
        screenModelScope.launch {
            try {
                userUseCase.deleteCookie(cookie)
                _state.update {
                    it.copy(cookies = it.cookies.filterNot { c -> c.cookie == cookie.cookie })
                }
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("删除饼干失败: ${e.message}"))
            }
        }
    }

    private fun updateCookieOrder(newList: List<Cookie>) {
        _state.update { it.copy(cookies = newList) }
        screenModelScope.launch {
            userUseCase.updateCookieSort(newList)
        }
    }
}
