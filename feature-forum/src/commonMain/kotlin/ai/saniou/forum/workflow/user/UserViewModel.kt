package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.forum.workflow.user.UserContract.Effect
import ai.saniou.forum.workflow.user.UserContract.Event
import ai.saniou.forum.workflow.user.UserContract.State
import ai.saniou.thread.domain.usecase.user.AddAccountUseCase
import ai.saniou.thread.domain.usecase.user.DeleteAccountUseCase
import ai.saniou.thread.domain.usecase.user.GetAccountsUseCase
import ai.saniou.thread.domain.usecase.user.UpdateAccountSortUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateAccountSortUseCase: UpdateAccountSortUseCase
) : ScreenModel {

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
            is Event.DeleteCookie -> deleteCookie(event.account)
            is Event.UpdateCookieOrder -> updateCookieOrder(event.accounts)
        }
    }

    private fun loadCookies() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val cookies = getAccountsUseCase()
                _state.update { it.copy(isLoading = false, cookies = cookies) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "获取饼干列表失败: ${e.message}"
                    )
                }
            }
        }
    }

    private fun addCookie(name: String, value: String) {
        screenModelScope.launch {
            try {
                addAccountUseCase(name, value)
                loadCookies() // 重新加载以获取包含新 cookie 的完整列表
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("添加饼干失败: ${e.message}"))
            }
        }
    }

    private fun deleteCookie(account: Account) {
        screenModelScope.launch {
            try {
                deleteAccountUseCase(account)
                _state.update {
                    it.copy(cookies = it.cookies.filterNot { c -> c.value == account.value })
                }
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("删除饼干失败: ${e.message}"))
            }
        }
    }

    private fun updateCookieOrder(newList: List<Account>) {
        _state.update { it.copy(cookies = newList) }
        screenModelScope.launch {
            updateAccountSortUseCase(newList)
        }
    }
}
