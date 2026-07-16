package ai.saniou.coreui.composition

import androidx.compose.runtime.staticCompositionLocalOf

/** Root-owned URL router. Feature modules stay platform-neutral and never guess how to launch links. */
val LocalContentLinkHandler = staticCompositionLocalOf<((String) -> Unit)?> { null }
