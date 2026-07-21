package ai.saniou.thread.feature.commands

import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.model.activity.ProductActionType
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
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.DeleteOutline
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
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_11fdaed970
import thread.composeapp.generated.resources.s_1ca2668ead
import thread.composeapp.generated.resources.s_1f425b6bf0
import thread.composeapp.generated.resources.s_358adbfeaf
import thread.composeapp.generated.resources.s_4bcb37a2a4
import thread.composeapp.generated.resources.s_528869a02b
import thread.composeapp.generated.resources.s_9f479438fb
import thread.composeapp.generated.resources.s_a29b060f6c
import thread.composeapp.generated.resources.s_bca2fcab84
import thread.composeapp.generated.resources.s_f3ea6d345e
import thread.composeapp.generated.resources.s_02dae4bb5f
import thread.composeapp.generated.resources.s_109d57e951
import thread.composeapp.generated.resources.s_10a8b6eb39
import thread.composeapp.generated.resources.s_29f6711704
import thread.composeapp.generated.resources.s_2c175e73aa
import thread.composeapp.generated.resources.s_54f69c3171
import thread.composeapp.generated.resources.s_7f0d5ec20d
import thread.composeapp.generated.resources.s_83e07208e8
import thread.composeapp.generated.resources.s_8475b81705
import thread.composeapp.generated.resources.s_88d95a1f5d
import thread.composeapp.generated.resources.s_88eff084e7
import thread.composeapp.generated.resources.s_89e3bbd558
import thread.composeapp.generated.resources.s_9abbfdbe4b
import thread.composeapp.generated.resources.s_af0c4a9884
import thread.composeapp.generated.resources.s_bab87bccea
import thread.composeapp.generated.resources.s_bbcc5f1de2
import thread.composeapp.generated.resources.s_bdf1267805
import thread.composeapp.generated.resources.s_d97d3180a5
import thread.composeapp.generated.resources.s_dfd1109735
import thread.composeapp.generated.resources.s_eb561f9bf0
import thread.composeapp.generated.resources.s_f0b38d648c
import thread.composeapp.generated.resources.s_f54431e0fc

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
        ProductCommandAction.EXECUTE_PRODUCT_ACTION -> when (descriptor.request?.type) {
            ProductActionType.REFRESH_SOURCE -> Icons.Default.Refresh
            ProductActionType.REFRESH_ALL_READERS -> Icons.Default.Sync
            ProductActionType.SET_SOURCE_ENABLED -> Icons.Default.PowerSettingsNew
            ProductActionType.CLEAR_SOURCE_DIAGNOSTIC,
            ProductActionType.EXPORT_DIAGNOSTIC -> Icons.Default.BugReport
            ProductActionType.EXPORT_READER_SUBSCRIPTIONS,
            ProductActionType.EXPORT_USER_DATA -> Icons.Default.FileDownload
            ProductActionType.IMPORT_READER_SUBSCRIPTIONS,
            ProductActionType.IMPORT_USER_DATA -> Icons.Default.FileUpload
            ProductActionType.BACKUP_TO_WEBDAV -> Icons.Default.CloudUpload
            ProductActionType.RESTORE_FROM_WEBDAV -> Icons.Default.CloudDownload
            ProductActionType.DISCARD_DRAFT -> Icons.Default.DeleteOutline
            null -> Icons.Default.Sync
        }
        ProductCommandAction.OPEN_SOURCE_LOGIN -> Icons.AutoMirrored.Filled.Login
        ProductCommandAction.OPEN_ACTIVITY_CENTER -> Icons.Default.NotificationsActive
        ProductCommandAction.OPEN_READER_IMPORT,
        ProductCommandAction.OPEN_USER_DATA_IMPORT -> Icons.Default.FileUpload
        ProductCommandAction.RESUME_DRAFT -> Icons.Default.EditNote
    },
)

@Composable
fun defaultWorkspaceCommands(): List<WorkspaceCommand> = listOf(
    WorkspaceCommand(WorkspaceDestination.FORUM, stringResource(Res.string.s_af0c4a9884), stringResource(Res.string.s_f54431e0fc), Icons.Default.Forum, "⌘1"),
    WorkspaceCommand(WorkspaceDestination.READER, stringResource(Res.string.s_88eff084e7), stringResource(Res.string.s_bab87bccea), Icons.Default.RssFeed, "⌘2"),
    WorkspaceCommand(WorkspaceDestination.FEED, stringResource(Res.string.s_dfd1109735), stringResource(Res.string.s_54f69c3171), Icons.Default.DynamicFeed, "⌘3"),
    WorkspaceCommand(WorkspaceDestination.SEARCH, stringResource(Res.string.s_eb561f9bf0), stringResource(Res.string.s_10a8b6eb39), Icons.Default.Search, "⌘4"),
    WorkspaceCommand(WorkspaceDestination.BOOKMARKS, stringResource(Res.string.s_02dae4bb5f), stringResource(Res.string.s_88d95a1f5d), Icons.Default.Bookmark, "⌘5"),
    WorkspaceCommand(WorkspaceDestination.INBOX, stringResource(Res.string.s_7f0d5ec20d), stringResource(Res.string.s_8475b81705), Icons.Default.NotificationsActive, "⌘6"),
    WorkspaceCommand(WorkspaceDestination.ACTIVITY, stringResource(Res.string.s_109d57e951), stringResource(Res.string.s_bbcc5f1de2), Icons.Default.NotificationsActive, "⌘7"),
    WorkspaceCommand(WorkspaceDestination.OPERATIONS, stringResource(Res.string.s_bdf1267805), stringResource(Res.string.s_d97d3180a5), Icons.Default.MonitorHeart, "⌘8"),
    WorkspaceCommand(WorkspaceDestination.SETTINGS, stringResource(Res.string.s_9abbfdbe4b), stringResource(Res.string.s_2c175e73aa), Icons.Default.Settings, "⌘9"),
    WorkspaceCommand(WorkspaceDestination.HISTORY, stringResource(Res.string.s_29f6711704), stringResource(Res.string.s_f0b38d648c), Icons.Default.History, ""),
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
    val allWorkspaceCommands = defaultWorkspaceCommands()
    var searchResults by remember { mutableStateOf<List<GlobalSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val filteredCommands = remember(query, allWorkspaceCommands) {
        val needle = query.trim()
        if (needle.isEmpty()) allWorkspaceCommands else allWorkspaceCommands.filter {
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
        if (entry is PaletteEntry.ProductAction && !entry.value.descriptor.enabled) return
        when (entry) {
            is PaletteEntry.Command -> onCommand(entry.value)
            is PaletteEntry.ProductAction -> onProductCommand(entry.value.descriptor)
            is PaletteEntry.Result -> onResult(entry.value)
        }
        onDismiss()
    }

    AdaptiveModal(onDismissRequest = onDismiss, paneTitle = stringResource(Res.string.s_1ca2668ead)) {
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
                Text(stringResource(Res.string.s_4bcb37a2a4), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.s_358adbfeaf),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(240) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).focusRequester(focusRequester),
                placeholder = { Text(stringResource(Res.string.s_9f479438fb)) },
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
                        Text(stringResource(Res.string.s_11fdaed970))
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
                Text(stringResource(Res.string.s_528869a02b), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.s_a29b060f6c), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.s_bca2fcab84), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProductCommandRow(command: ProductCommand, selected: Boolean, onClick: () -> Unit) {
    val contentDesc = stringResource(Res.string.s_83e07208e8, command.descriptor.label)
    Surface(
        onClick = onClick,
        enabled = command.descriptor.enabled,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = contentDesc
        },
        color = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Icon(
                command.icon,
                null,
                tint = if (command.descriptor.enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
            )
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
                Text(
                    if (command.descriptor.enabled) stringResource(Res.string.s_f3ea6d345e) else stringResource(Res.string.s_1f425b6bf0),
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CommandRow(command: WorkspaceCommand, selected: Boolean, onClick: () -> Unit) {
    val contentDesc = stringResource(Res.string.s_89e3bbd558, command.label, command.shortcut)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = contentDesc
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
