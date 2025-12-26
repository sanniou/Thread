package ai.saniou.thread.feature.challenge

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun ChallengeWebView(
    url: String,
    modifier: Modifier,
    onChallengeSuccess: (String) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Web platform is not supported for Cloudflare verification yet.")
    }
}