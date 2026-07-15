package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.forum.workflow.user.UserContract.Effect
import ai.saniou.forum.workflow.user.UserContract.Event
import ai.saniou.forum.workflow.user.UserContract.State
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.usecase.user.LoginSourceUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class UserViewModel(
    private val getAvailableSourcesUseCase: GetAvailableSourcesUseCase,
    private val accountRepository: AccountRepository,
    private val loginSourceUseCase: LoginSourceUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()
    private var accountCollection: Job? = null

    fun handleEvent(event: Event) {
        when (event) {
            is Event.LoadData -> loadData(event.sourceId)
            is Event.AddAccount -> addAccount(event.inputs)
            is Event.DeleteAccount -> deleteAccount(event.account)
            is Event.UpdateAccountOrder -> updateAccountOrder(event.accounts)
        }
    }

    private fun loadData(sourceId: String) {
        accountCollection?.cancel()
        val source = getAvailableSourcesUseCase().find { it.id == sourceId }
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                sourceId = sourceId,
                loginStrategy = source?.takeIf { it.capabilities.supportsLogin }?.loginStrategy,
            )
        }
        accountCollection = screenModelScope.launch {
            try {
                accountRepository.getAccounts(sourceId).collectLatest { accounts ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            cookies = accounts.sortedBy(Account::sort),
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "获取账号列表失败: ${e.message}"
                    )
                }
            }
        }
    }

    private fun addAccount(inputs: Map<String, String>) {
        screenModelScope.launch {
            try {
                val sourceId = _state.value.sourceId
                loginSourceUseCase(sourceId, inputs)
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("添加账号失败: ${e.message}"))
            }
        }
    }

    private fun deleteAccount(account: Account) {
        screenModelScope.launch {
            try {
                accountRepository.deleteAccount(account.id)
            } catch (e: Exception) {
                _effect.send(Effect.ShowError("删除账号失败: ${e.message}"))
            }
        }
    }

    private fun updateAccountOrder(newList: List<Account>) {
        _state.update { it.copy(cookies = newList) }
        screenModelScope.launch {
            newList.forEachIndexed { index, account ->
                accountRepository.updateAccount(account.copy(sort = index.toLong()))
            }
        }
    }
}
