package ai.saniou.forum.workflow.login

import ai.saniou.forum.workflow.login.TiebaLoginContract.Effect
import ai.saniou.forum.workflow.login.TiebaLoginContract.Event
import ai.saniou.forum.workflow.login.TiebaLoginContract.State
import ai.saniou.thread.domain.usecase.user.LoginTiebaUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TiebaLoginViewModel(
    private val loginTiebaUseCase: LoginTiebaUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    fun handleEvent(event: Event) {
        when (event) {
            is Event.CredentialsIntercepted -> {
                saveAccount(
                    bduss = event.bduss,
                    stoken = event.stoken,
                    uid = event.uid,
                    name = event.name,
                    portrait = event.portrait,
                    tbs = event.tbs
                )
            }
            is Event.SaveAccount -> {
                // Placeholder for simplified save if we only have cookies
                // Real implementation should probably fetch user info first if not provided
            }
            is Event.LoginSuccess -> {
                screenModelScope.launch {
                    _effect.send(Effect.NavigateBack)
                }
            }
            is Event.LoginFailed -> {
                _state.update { it.copy(error = event.error, isLoading = false) }
            }
        }
    }

    private fun saveAccount(
        bduss: String,
        stoken: String,
        uid: String,
        name: String,
        portrait: String,
        tbs: String
    ) {
        screenModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                loginTiebaUseCase(
                    bduss = bduss,
                    sToken = stoken,
                    uid = uid,
                    name = name,
                    portrait = portrait,
                    tbs = tbs
                )
                _effect.send(Effect.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                _effect.send(Effect.ShowError(e.message ?: "Login failed"))
            }
        }
    }
}