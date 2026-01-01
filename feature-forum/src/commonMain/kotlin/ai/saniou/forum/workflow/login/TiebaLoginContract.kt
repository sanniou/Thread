package ai.saniou.forum.workflow.login


interface TiebaLoginContract {
    sealed interface Event {
        data object LoginSuccess : Event
        data class LoginFailed(val error: String) : Event
        data class CredentialsIntercepted(
            val bduss: String,
            val stoken: String,
            val uid: String,
            val name: String,
            val portrait: String,
            val tbs: String
        ) : Event
        data class SaveAccount(
            val bduss: String,
            val stoken: String
        ) : Event
    }

    data class State(
        val isLoading: Boolean = false,
        val loginUrl: String = "https://wappass.baidu.com/passport",
        val error: String? = null
    )

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: String) : Effect
    }
}
