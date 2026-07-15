package ai.saniou.forum.ui.login

import ai.saniou.thread.domain.model.user.LoginStrategy
import androidx.compose.runtime.Composable

@Composable
fun LoginScreen(
    strategy: LoginStrategy,
    onDismissRequest: () -> Unit,
    onLoginSuccess: (Map<String, String>) -> Unit
) {
    when (strategy) {
        is LoginStrategy.Manual -> {
            ManualLoginDialog(
                strategy = strategy,
                onDismissRequest = onDismissRequest,
                onConfirm = onLoginSuccess
            )
        }
        is LoginStrategy.WebView -> {
            LoginWebView(
                strategy = strategy,
                onDismissRequest = onDismissRequest,
                onCookieCaptured = onLoginSuccess,
            )
        }
        is LoginStrategy.Api -> {
            ManualLoginDialog(
                strategy = LoginStrategy.Manual(
                    title = strategy.title,
                    description = strategy.description,
                    fields = strategy.fields,
                ),
                onDismissRequest = onDismissRequest,
                onConfirm = onLoginSuccess,
            )
        }
    }
}
