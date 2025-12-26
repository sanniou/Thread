package ai.saniou.thread.feature.challenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ChallengeWebView(
    url: String,
    modifier: Modifier = Modifier,
    onChallengeSuccess: (String) -> Unit
)