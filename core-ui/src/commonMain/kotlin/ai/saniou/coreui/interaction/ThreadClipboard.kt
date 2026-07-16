@file:Suppress("DEPRECATION")

package ai.saniou.coreui.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * A single compatibility boundary for plain-text clipboard writes.
 *
 * Compose's new suspend Clipboard API does not yet expose a common constructor
 * for plain-text ClipEntry values on every target, so feature code stays on a
 * stable cross-platform contract while the platform detail remains isolated.
 */
@Stable
class ThreadClipboard internal constructor(
    private val manager: ClipboardManager,
) {
    fun copyText(text: String) {
        manager.setText(AnnotatedString(text))
    }
}

@Composable
fun rememberThreadClipboard(): ThreadClipboard {
    val manager = LocalClipboardManager.current
    return remember(manager) { ThreadClipboard(manager) }
}
