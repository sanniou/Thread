package ai.saniou.coreui.interaction

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThreadShortcutTest {
    @Test
    fun commandShortcutAcceptsCtrlOrMetaButNotPlainTyping() {
        val shortcut = ThreadShortcut(Key.K) {}

        assertTrue(shortcut.matches(Key.K, true, true, false, false))
        assertFalse(shortcut.matches(Key.K, true, false, false, false))
        assertFalse(shortcut.matches(Key.K, false, true, false, false))
    }

    @Test
    fun escapeShortcutDoesNotRequireACommandModifier() {
        val shortcut = ThreadShortcut(Key.Escape, command = false) {}

        assertTrue(shortcut.matches(Key.Escape, true, false, false, false))
        assertFalse(shortcut.matches(Key.Escape, true, false, true, false))
    }
}
