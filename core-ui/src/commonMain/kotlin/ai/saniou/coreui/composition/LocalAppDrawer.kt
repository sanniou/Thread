package ai.saniou.coreui.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * A CompositionLocal that provides the global drawer content.
 * This allows feature modules to inject the app-level navigation (like switching between Forum and Reader)
 * into their own internal drawers.
 */
val LocalAppDrawer = compositionLocalOf<@Composable () -> Unit> {
    { }
}