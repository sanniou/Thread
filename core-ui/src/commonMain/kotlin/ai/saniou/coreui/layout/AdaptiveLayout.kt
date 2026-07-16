package ai.saniou.coreui.layout

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ThreadWindowWidthClass {
    Compact,
    Medium,
    Expanded,
    Large,
}

enum class ThreadWindowHeightClass {
    Compact,
    Regular,
}

@Immutable
data class ThreadWindowInfo(
    val width: Dp,
    val height: Dp,
    val widthClass: ThreadWindowWidthClass,
    val heightClass: ThreadWindowHeightClass,
) {
    val usesBottomNavigation: Boolean
        get() = widthClass == ThreadWindowWidthClass.Compact

    val showsNavigationLabels: Boolean
        get() = widthClass >= ThreadWindowWidthClass.Expanded

    val hasPermanentFeatureSidebar: Boolean
        get() = widthClass >= ThreadWindowWidthClass.Expanded

    val supportsListDetail: Boolean
        get() = widthClass == ThreadWindowWidthClass.Large

    val pageHorizontalPadding: Dp
        get() = when (widthClass) {
            ThreadWindowWidthClass.Compact -> 16.dp
            ThreadWindowWidthClass.Medium -> 22.dp
            ThreadWindowWidthClass.Expanded -> 28.dp
            ThreadWindowWidthClass.Large -> 36.dp
        }

    val featureSidebarWidth: Dp
        get() = when (widthClass) {
            ThreadWindowWidthClass.Compact -> Dimens.compactSidebarWidth
            ThreadWindowWidthClass.Medium -> Dimens.mediumSidebarWidth
            ThreadWindowWidthClass.Expanded -> Dimens.sidebarWidth
            ThreadWindowWidthClass.Large -> Dimens.largeSidebarWidth
        }

    val navigationRailWidth: Dp
        get() = if (showsNavigationLabels) Dimens.workspaceRailWidth else Dimens.compactRailWidth
}

val LocalThreadWindowInfo = staticCompositionLocalOf {
    ThreadWindowInfo(
        width = Dimens.ExpandedWidth,
        height = 800.dp,
        widthClass = ThreadWindowWidthClass.Expanded,
        heightClass = ThreadWindowHeightClass.Regular,
    )
}

fun classifyThreadWindow(width: Dp, height: Dp): ThreadWindowInfo = ThreadWindowInfo(
    width = width,
    height = height,
    widthClass = when {
        width < Dimens.CompactWidth -> ThreadWindowWidthClass.Compact
        width < Dimens.DesktopWidth -> ThreadWindowWidthClass.Medium
        width < Dimens.LargeWidth -> ThreadWindowWidthClass.Expanded
        else -> ThreadWindowWidthClass.Large
    },
    heightClass = if (height < Dimens.CompactHeight) {
        ThreadWindowHeightClass.Compact
    } else {
        ThreadWindowHeightClass.Regular
    },
)

/**
 * The single adaptive boundary for the whole product. Feature modules consume
 * [LocalThreadWindowInfo] instead of inventing platform or pixel checks.
 */
@Composable
fun ThreadAdaptiveWindow(
    modifier: Modifier = Modifier,
    content: @Composable (ThreadWindowInfo) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val windowInfo = classifyThreadWindow(maxWidth, maxHeight)
        CompositionLocalProvider(LocalThreadWindowInfo provides windowInfo) {
            content(windowInfo)
        }
    }
}

/**
 * Shared feature navigation behavior: a persistent sidebar on wide windows and
 * an overlay drawer on compact or medium windows. The content receives whether
 * it should expose a menu affordance and an action that opens the sidebar.
 */
@Composable
fun AdaptiveSidebarScaffold(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    coroutineScope: CoroutineScope,
    sidebar: @Composable () -> Unit,
    content: @Composable (showMenu: Boolean, openSidebar: () -> Unit) -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    val openSidebar: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }

    if (windowInfo.hasPermanentFeatureSidebar) {
        Row(modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.width(windowInfo.featureSidebarWidth).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                ),
            ) {
                sidebar()
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
                content(false, {})
            }
        }
    } else {
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(windowInfo.featureSidebarWidth),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    sidebar()
                }
            },
        ) {
            content(true, openSidebar)
        }
    }
}

@Composable
fun ReadingCanvas(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (windowInfo.widthClass >= ThreadWindowWidthClass.Expanded) {
                        Modifier.padding(horizontal = windowInfo.pageHorizontalPadding, vertical = 16.dp)
                    } else {
                        Modifier
                    }
                )
                .padding(contentPadding),
            color = MaterialTheme.colorScheme.surface,
            shape = if (windowInfo.widthClass >= ThreadWindowWidthClass.Expanded) {
                MaterialTheme.shapes.extraLarge
            } else {
                MaterialTheme.shapes.extraSmall
            },
            border = if (windowInfo.widthClass >= ThreadWindowWidthClass.Expanded) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            } else {
                null
            },
        ) {
            content()
        }
    }
}
