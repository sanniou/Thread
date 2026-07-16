package ai.saniou.coreui.interaction

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Immutable
data class ThreadShortcut(
    val key: Key,
    val command: Boolean = true,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val action: () -> Unit,
) {
    fun matches(
        key: Key,
        isKeyDown: Boolean,
        commandPressed: Boolean,
        shiftPressed: Boolean,
        altPressed: Boolean,
    ): Boolean = isKeyDown &&
        this.key == key &&
        (!command || commandPressed) &&
        shift == shiftPressed &&
        alt == altPressed
}

/** Cross-platform command host: Ctrl on conventional desktops, Command on macOS. */
fun Modifier.threadShortcutHost(vararg shortcuts: ThreadShortcut): Modifier =
    onPreviewKeyEvent { event ->
        val shortcut = shortcuts.firstOrNull {
            it.matches(
                key = event.key,
                isKeyDown = event.type == KeyEventType.KeyDown,
                commandPressed = event.isCtrlPressed || event.isMetaPressed,
                shiftPressed = event.isShiftPressed,
                altPressed = event.isAltPressed,
            )
        }
        shortcut?.action?.invoke()
        shortcut != null
    }
