package ai.saniou.coreui.widgets

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Phone-first bottom sheet that becomes a focused dialog on wider windows. */
@Composable
fun AdaptiveModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    if (windowInfo.widthClass == ThreadWindowWidthClass.Compact) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) { content() }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 760.dp).heightIn(max = 780.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 18.dp,
                ) {
                    content()
                }
            }
        }
    }
}
