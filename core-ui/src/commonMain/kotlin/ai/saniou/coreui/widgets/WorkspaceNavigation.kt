package ai.saniou.coreui.widgets

import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class WorkspaceNavigationItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val bottom: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Product-level navigation that becomes a bottom bar on phones, a compact rail
 * on tablets and a labelled rail on desktop. Every destination remains
 * reachable in compact mode through the overflow sheet.
 */
@Composable
fun WorkspaceNavigationSuite(
    items: List<WorkspaceNavigationItem>,
    modifier: Modifier = Modifier,
    onOpenCommandPalette: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    val focusRequester = remember { FocusRequester() }
    val shortcuts = items.mapIndexedNotNull { index, item ->
        workspaceShortcutKeys.getOrNull(index)?.let { key ->
            ThreadShortcut(key) { item.onClick() }
        }
    } + listOfNotNull(
        onOpenCommandPalette?.let { ThreadShortcut(Key.P, shift = true, action = it) }
    )
    val hostModifier = modifier
        .focusRequester(focusRequester)
        .focusable()
        .semantics { paneTitle = "Thread 工作区" }
        .threadShortcutHost(*shortcuts.toTypedArray())
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    if (windowInfo.usesBottomNavigation) {
        WorkspaceBottomNavigation(
            items = items,
            modifier = hostModifier,
            onOpenCommandPalette = onOpenCommandPalette,
            content = content,
        )
    } else {
        Row(hostModifier.fillMaxSize()) {
            WorkspaceNavigationRail(items, onOpenCommandPalette = onOpenCommandPalette)
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
        }
    }
}

@Composable
private fun WorkspaceBottomNavigation(
    items: List<WorkspaceNavigationItem>,
    modifier: Modifier,
    onOpenCommandPalette: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var showOverflow by remember { mutableStateOf(false) }
    val primaryItems = items.filterNot { it.bottom }.take(4)
    val overflowItems = items.filterNot { it in primaryItems }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                primaryItems.forEach { item ->
                    val index = items.indexOf(item)
                    NavigationBarItem(
                        modifier = Modifier.workspaceDestinationSemantics(item, index),
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
                NavigationBarItem(
                    modifier = Modifier.semantics {
                        contentDescription = "更多工作区"
                        stateDescription = if (overflowItems.any { it.selected }) "包含当前工作区" else "快捷键菜单"
                    },
                    selected = overflowItems.any { it.selected },
                    onClick = { showOverflow = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "更多") },
                    label = { Text("更多") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { content() }
    }

    if (showOverflow) {
        ModalBottomSheet(onDismissRequest = { showOverflow = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "工作区",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
                onOpenCommandPalette?.let { openPalette ->
                    Surface(
                        onClick = {
                            showOverflow = false
                            openPalette()
                        },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "打开命令与全局发现"
                        },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("命令与发现", style = MaterialTheme.typography.titleMedium)
                                Text("跳转工作区或搜索离线缓存", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
                primaryItems.forEach { item ->
                    CompactDestinationItem(item, items.indexOf(item)) { showOverflow = false }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                overflowItems.forEach { item ->
                    CompactDestinationItem(item, items.indexOf(item)) { showOverflow = false }
                }
            }
        }
    }
}

@Composable
private fun CompactDestinationItem(
    item: WorkspaceNavigationItem,
    index: Int,
    onDismiss: () -> Unit,
) {
    Surface(
        onClick = {
            item.onClick()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth().workspaceDestinationSemantics(item, index),
        shape = MaterialTheme.shapes.large,
        color = if (item.selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(item.icon, contentDescription = null)
            Text(item.label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun WorkspaceNavigationRail(
    items: List<WorkspaceNavigationItem>,
    modifier: Modifier = Modifier,
    onOpenCommandPalette: (() -> Unit)? = null,
) {
    val windowInfo = LocalThreadWindowInfo.current
    Surface(
        modifier = modifier.width(windowInfo.navigationRailWidth).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 9.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "T",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (windowInfo.showsNavigationLabels) {
                Text(
                    "THREAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(10.dp))
            onOpenCommandPalette?.let { openPalette ->
                Surface(
                    onClick = openPalette,
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "打开命令与全局发现，快捷键 Ctrl 或 Command 加 Shift 加 P"
                    },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = if (windowInfo.showsNavigationLabels) 9.dp else 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                        if (windowInfo.showsNavigationLabels) {
                            Text(
                                "命令",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.size(4.dp))
            }
            items.filterNot { it.bottom }.forEach { item ->
                WorkspaceRailItem(item, windowInfo.showsNavigationLabels, items.indexOf(item))
            }
            Spacer(Modifier.weight(1f))
            items.filter { it.bottom }.forEach { item ->
                WorkspaceRailItem(item, windowInfo.showsNavigationLabels, items.indexOf(item))
            }
        }
    }
}

@Composable
private fun WorkspaceRailItem(item: WorkspaceNavigationItem, showLabel: Boolean, index: Int) {
    val container = if (item.selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (item.selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = item.onClick,
        modifier = Modifier.fillMaxWidth().workspaceDestinationSemantics(item, index),
        shape = MaterialTheme.shapes.large,
        color = container,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = if (showLabel) 9.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(item.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            if (showLabel) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun Modifier.workspaceDestinationSemantics(
    item: WorkspaceNavigationItem,
    index: Int,
): Modifier = semantics {
    contentDescription = item.label
    selected = item.selected
    stateDescription = if (item.selected) {
        "当前工作区"
    } else {
        "快捷键 Ctrl 或 Command + ${index + 1}"
    }
}

private val workspaceShortcutKeys = listOf(
    Key.One,
    Key.Two,
    Key.Three,
    Key.Four,
    Key.Five,
    Key.Six,
    Key.Seven,
    Key.Eight,
    Key.Nine,
)
