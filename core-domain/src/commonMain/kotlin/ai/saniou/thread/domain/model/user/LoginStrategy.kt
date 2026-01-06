package ai.saniou.thread.domain.model.user

sealed class LoginStrategy {
    data class Manual(
        val title: String,
        val description: String,
        val fields: List<LoginField>
    ) : LoginStrategy()

    data class WebView(
        val url: String,
        val userAgent: String? = null,
        val cookieDomain: String? = null,
        val targetCookieKeys: List<String> = emptyList()
    ) : LoginStrategy()
    
    data class Api(
        val title: String
    ) : LoginStrategy()
}

data class LoginField(
    val key: String,
    val label: String,
    val hint: String,
    val isMultiline: Boolean = false,
    val isRequired: Boolean = true
)