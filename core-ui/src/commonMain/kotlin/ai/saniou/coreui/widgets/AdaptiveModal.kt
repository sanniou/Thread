package ai.saniou.coreui.widgets

import org.jetbrains.compose.resources.stringResource
import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.layout.ThreadWindowWidthClass
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_21a58979fe

/** Phone-first bottom sheet that becomes a focused dialog on wider windows. */
@Composable
fun AdaptiveModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = stringResource(Res.string.s_21a58979fe),
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    val modalModifier = modifier
        .semantics { this.paneTitle = paneTitle }
        .threadShortcutHost(
            ThreadShortcut(Key.Escape, command = false, action = onDismissRequest)
        )
    if (windowInfo.widthClass == ThreadWindowWidthClass.Compact) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Box(modalModifier.fillMaxWidth().padding(bottom = 16.dp)) { content() }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = modalModifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 760.dp).heightIn(max = 780.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    content()
                }
            }
        }
    }
}
