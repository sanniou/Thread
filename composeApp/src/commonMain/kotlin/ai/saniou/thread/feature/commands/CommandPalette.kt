package ai.saniou.thread.feature.commands

import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.usecase.search.SearchLocalContentUseCase
import ai.saniou.thread.feature.search.GlobalSearchResultRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class WorkspaceCommand(
    val destination: WorkspaceDestination,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val shortcut: String,
)

data class ProductCommand(
    val descriptor: ProductCommandDescriptor,
    val icon: ImageVector = when (descriptor.action) {
        ProductCommandAction.REFRESH_SOURCE -> Icons.Default.Refresh
        ProductCommandAction.SET_SOURCE_ENABLED -> Icons.Default.PowerSettingsNew
        ProductCommandAction.OPEN_SOURCE_LOGIN -> Icons.AutoMirrored.Filled.Login
        ProductCommandAction.REFRESH_ALL_READERS -> Icons.Default.Sync
        ProductCommandAction.EXPORT_DIAGNOSTIC -> Icons.Default.BugReport
    },
)

val defaultWorkspaceCommands = listOf(
    WorkspaceCommand(WorkspaceDestination.FORUM, "打开社区", "浏览来源、版块与主题", Icons.Default.Forum, "⌘1"),
    WorkspaceCommand(WorkspaceDestination.READER, "打开 Reader", "订阅、筛选与沉浸阅读", Icons.Default.RssFeed, "⌘2"),
    WorkspaceCommand(WorkspaceDestination.FEED, "打开动态", "聚合社区与订阅时间线", Icons.Default.DynamicFeed, "⌘3"),
    WorkspaceCommand(WorkspaceDestination.SEARCH, "全局发现", "搜索全部离线内容缓存", Icons.Default.Search, "⌘4"),
    WorkspaceCommand(WorkspaceDestination.BOOKMARKS, "打开收藏", "继续保存的阅读上下文", Icons.Default.Bookmark, "⌘5"),
    WorkspaceCommand(WorkspaceDestination.HISTORY, "打开历史", "回到最近浏览的主题", Icons.Default.History, "⌘6"),
    WorkspaceCommand(WorkspaceDestination.OPERATIONS, "来源运维", "检查缓存、刷新与连接器健康", Icons.Default.MonitorHeart, "⌘7"),
    WorkspaceCommand(WorkspaceDestination.LAB, "打开实验室", "探索实验性体验", Icons.Default.Games, "⌘8"),
    WorkspaceCommand(WorkspaceDestination.SETTINGS, "数据与同步", "备份、恢复与 WebDAV", Icons.Default.Settings, "⌘9"),
)

@Composable
fun CommandPalette(
    searchLocalContent: SearchLocalContentUseCase,
    productCommands: List<ProductCommand> = emptyList(),
    onDismiss: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
    onProductCommand: (ProductCommandDescriptor) -> Unit = {},
    onResult: (GlobalSearchResult) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GlobalSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val filteredCommands = remember(query) {
        val needle = query.trim()
        if (needle.isEmpty()) defaultWorkspaceCommands else defaultWorkspaceCommands.filter {
            it.label.contains(needle, ignoreCase = true) || it.description.contains(needle, ignoreCase = true)
        }
    }
    val filteredProductCommands = remember(query, productCommands) {
        val needle = query.trim()
        if (needle.isEmpty()) productCommands else productCommands.filter {
            it.descriptor.label.contains(needle, ignoreCase = true) ||
                it.descriptor.description.contains(needle, ignoreCase = true)
        }
    }
    val entries = remember(filteredCommands, filteredProductCommands, searchResults) {
        filteredCommands.map(PaletteEntry::Command) +
            filteredProductCommands.map(PaletteEntry::ProductAction) +
            searchResults.map(PaletteEntry::Result)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(query) {
        selectedIndex = 0
        if (query.trim().length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(180)
        searchResults = runCatching { searchLocalContent(query, limitPerType = 6).results }
            .getOrDefault(emptyList())
        isSearching = false
    }

    fun activate(entry: PaletteEntry) {
        when (entry) {
            is PaletteEntry.Command -> onCommand(entry.value)
            is PaletteEntry.ProductAction -> onProductCommand(entry.value.descriptor)
            is PaletteEntry.Result -> onResult(entry.value)
        }
        onDismiss()
    }

    AdaptiveModal(onDismissRequest = onDismiss, paneTitle = "Thread 命令面板") {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 720.dp)
                .threadShortcutHost(
                    ThreadShortcut(Key.DirectionDown, command = false) {
                        if (entries.isNotEmpty()) selectedIndex = (selectedIndex + 1).coerceAtMost(entries.lastIndex)
                    },
                    ThreadShortcut(Key.DirectionUp, command = false) {
                        if (entries.isNotEmpty()) selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    },
                    ThreadShortcut(Key.Enter, command = false) {
                        entries.getOrNull(selectedIndex)?.let(::activate)
                    },
                ),
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("命令与发现", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "输入工作区名称或搜索所有本地缓存 · Ctrl/Command + Shift + P",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(240) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).focusRequester(focusRequester),
                placeholder = { Text("跳转到工作区，或搜索主题、回复、文章…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                },
                singleLine = true,
            )
            HorizontalDivider(Modifier.padding(top = 16.dp))
            if (entries.isEmpty() && !isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(10.dp))
                        Text("没有匹配的命令或缓存内容")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                        when (entry) {
                            is PaletteEntry.Command -> CommandRow(
                                command = entry.value,
                                selected = index == selectedIndex,
                                onClick = { activate(entry) },
                            )
                            is PaletteEntry.ProductAction -> ProductCommandRow(
                                command = entry.value,
                                selected = index == selectedIndex,
                                onClick = { activate(entry) },
                            )
                            is PaletteEntry.Result -> Surface(
                                color = if (index == selectedIndex) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = MaterialTheme.shapes.large,
                            ) {
                                GlobalSearchResultRow(entry.value) { activate(entry) }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("↑↓ 选择", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("↵ 打开", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Esc 关闭", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProductCommandRow(command: ProductCommand, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${command.descriptor.label}，全局操作"
        },
        color = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Icon(command.icon, null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f)) {
                Text(command.descriptor.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    command.descriptor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text("操作", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CommandRow(command: WorkspaceCommand, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${command.label}，快捷键 ${command.shortcut}"
        },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Icon(command.icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(command.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(command.shortcut, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private sealed interface PaletteEntry {
    val key: String

    data class Command(val value: WorkspaceCommand) : PaletteEntry {
        override val key = "command:${value.destination.key}"
    }

    data class Result(val value: GlobalSearchResult) : PaletteEntry {
        override val key = "result:${value.type}:${value.sourceId}:${value.id}"
    }

    data class ProductAction(val value: ProductCommand) : PaletteEntry {
        override val key = "product:${value.descriptor.id}"
    }
}
