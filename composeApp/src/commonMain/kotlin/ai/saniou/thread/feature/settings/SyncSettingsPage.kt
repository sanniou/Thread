package ai.saniou.thread.feature.settings

import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.layout.ThreadWindowWidthClass
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouOutlinedButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.platform.LocalUserDataFileService
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ai.saniou.thread.domain.model.settings.InterfaceDensity
import ai.saniou.thread.domain.model.settings.MotionMode
import ai.saniou.thread.domain.model.settings.ThemeMode
import ai.saniou.thread.domain.model.collection.SmartCollectionSort
import ai.saniou.thread.domain.model.collection.SmartCollectionGroup
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance

class SyncSettingsPage(
    private val showImportOnOpen: Boolean = false,
    private val initialImportPayload: String? = null,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<SyncSettingsViewModel>() }
        val state by viewModel.state.collectAsState()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val clipboard = rememberThreadClipboard()
        val userDataFileService = LocalUserDataFileService.current
        var collectionName by remember { mutableStateOf("") }
        var collectionQuery by remember { mutableStateOf("") }
        var collectionUnread by remember { mutableStateOf(false) }
        var collectionBookmarked by remember { mutableStateOf(false) }
        var collectionSort by remember { mutableStateOf(SmartCollectionSort.NEWEST) }
        var collectionGroup by remember { mutableStateOf(SmartCollectionGroup.NONE) }
        var socialName by remember { mutableStateOf("") }
        var socialBaseUrl by remember { mutableStateOf("") }
        var socialAccessToken by remember { mutableStateOf("") }

        LaunchedEffect(showImportOnOpen, initialImportPayload) {
            when {
                !initialImportPayload.isNullOrBlank() ->
                    viewModel.onEvent(SyncSettingsContract.Event.ImportLocal(initialImportPayload))
                showImportOnOpen -> viewModel.onEvent(SyncSettingsContract.Event.ShowImportLocal)
            }
        }

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(SyncSettingsContract.Event.MessageShown)
            }
        }
        state.dialog?.let { dialog ->
            UserDataTransferDialog(
                dialog = dialog,
                isWorking = state.isWorking,
                onDismiss = { viewModel.onEvent(SyncSettingsContract.Event.DismissDialog) },
                onImport = { viewModel.onEvent(SyncSettingsContract.Event.ImportLocal(it)) },
                onExportToFile = userDataFileService?.let { service ->
                    {
                        scope.launch {
                            service.exportText("thread-user-data.json", dialog.payload).fold(
                                onSuccess = { path ->
                                    snackbar.showSnackbar("已导出到 $path")
                                    viewModel.onEvent(SyncSettingsContract.Event.DismissDialog)
                                },
                                onFailure = { error ->
                                    if (error.message != "已取消导出") {
                                        snackbar.showSnackbar(error.message ?: "导出失败")
                                    }
                                },
                            )
                        }
                    }
                },
                onCopy = {
                    clipboard.copyText(dialog.payload)
                    scope.launch { snackbar.showSnackbar("数据包已复制") }
                },
            )
        }

        ThreadDetailScaffold(
            title = "数据与同步",
            eyebrow = "备份与同步",
            subtitle = "本地数据包、WebDAV 备份与运行诊断",
            onBack = navigator::pop,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
              Column(
                modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = Dimens.page_vertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text("外观与阅读", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "主题、界面密度、字体、动效和正文版心会同步应用到 Android、iOS 与 Desktop。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("主题", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.appearance.themeMode == mode,
                                onClick = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(themeMode = mode))) },
                                label = { Text(when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }) },
                            )
                        }
                    }
                    Text("信息密度", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InterfaceDensity.entries.forEach { density ->
                            FilterChip(
                                selected = state.appearance.density == density,
                                onClick = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(density = density))) },
                                label = { Text(when (density) { InterfaceDensity.COMPACT -> "紧凑"; InterfaceDensity.COMFORTABLE -> "舒适"; InterfaceDensity.SPACIOUS -> "宽松" }) },
                            )
                        }
                    }
                    PreferenceSlider(
                        label = "字体缩放 ${(state.appearance.fontScale * 100).toInt()}%",
                        value = state.appearance.fontScale,
                        range = 0.85f..1.4f,
                        onValue = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(fontScale = it))) },
                    )
                    PreferenceSlider(
                        label = "Reader 版心 ${state.appearance.readerWidthDp} dp",
                        value = state.appearance.readerWidthDp.toFloat(),
                        range = 520f..1080f,
                        onValue = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(readerWidthDp = it.toInt()))) },
                    )
                    PreferenceSlider(
                        label = "正文行高 ${state.appearance.readerLineHeight}",
                        value = state.appearance.readerLineHeight,
                        range = 1.2f..2.2f,
                        onValue = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(readerLineHeight = it))) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("减少动态效果", style = MaterialTheme.typography.titleSmall)
                            Text("为未来平台和辅助功能保留统一动效策略", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.appearance.motionMode == MotionMode.REDUCED,
                            onCheckedChange = { reduced ->
                                viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(
                                    state.appearance.copy(motionMode = if (reduced) MotionMode.REDUCED else MotionMode.SYSTEM)
                                ))
                            },
                        )
                    }
                    SaniouTextButton(
                        onClick = { viewModel.onEvent(SyncSettingsContract.Event.ResetAppearance) },
                        text = "恢复外观默认值",
                    )
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text("跨源智能集合", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "用同一组规则聚合社区主题、Reader 文章和未来社交流。集合随用户数据包同步。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = collectionName,
                        onValueChange = { collectionName = it },
                        label = { Text("集合名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = collectionQuery,
                        onValueChange = { collectionQuery = it },
                        label = { Text("跨源关键词（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(collectionUnread, { collectionUnread = !collectionUnread }, { Text("仅未读") })
                        FilterChip(collectionBookmarked, { collectionBookmarked = !collectionBookmarked }, { Text("仅收藏") })
                    }
                    Text("排序", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmartCollectionSort.entries.forEach { sort ->
                            FilterChip(
                                selected = collectionSort == sort,
                                onClick = { collectionSort = sort },
                                label = { Text(when (sort) {
                                    SmartCollectionSort.NEWEST -> "最新优先"
                                    SmartCollectionSort.OLDEST -> "最早优先"
                                    SmartCollectionSort.RELEVANCE -> "相关优先"
                                }) },
                            )
                        }
                    }
                    Text("分组", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmartCollectionGroup.entries.forEach { group ->
                            FilterChip(
                                selected = collectionGroup == group,
                                onClick = { collectionGroup = group },
                                label = { Text(when (group) {
                                    SmartCollectionGroup.NONE -> "不分组"
                                    SmartCollectionGroup.SOURCE -> "按来源"
                                    SmartCollectionGroup.CONTENT_KIND -> "按类型"
                                    SmartCollectionGroup.AUTHOR -> "按作者"
                                }) },
                            )
                        }
                    }
                    SaniouButton(
                        enabled = collectionName.isNotBlank() && (collectionQuery.isNotBlank() || collectionUnread || collectionBookmarked),
                        onClick = {
                            viewModel.onEvent(SyncSettingsContract.Event.SaveSmartCollection(
                                collectionName, collectionQuery, collectionUnread, collectionBookmarked,
                                collectionSort, collectionGroup,
                            ))
                            collectionName = ""
                            collectionQuery = ""
                            collectionUnread = false
                            collectionBookmarked = false
                        },
                        text = "创建智能集合",
                    )
                    state.smartCollections.forEach { collection ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(collection.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${collection.description} · ${collection.sort.name.lowercase()} · ${collection.groupBy.name.lowercase()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                viewModel.onEvent(SyncSettingsContract.Event.MoveSmartCollection(collection.id, -1))
                            }) { Icon(Icons.Default.KeyboardArrowUp, "上移 ${collection.name}") }
                            IconButton(onClick = {
                                viewModel.onEvent(SyncSettingsContract.Event.MoveSmartCollection(collection.id, 1))
                            }) { Icon(Icons.Default.KeyboardArrowDown, "下移 ${collection.name}") }
                            IconButton(onClick = {
                                viewModel.onEvent(SyncSettingsContract.Event.ToggleSmartCollectionPinned(collection.id, !collection.pinned))
                            }) {
                                Icon(if (collection.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin, "置顶 ${collection.name}")
                            }
                            IconButton(onClick = { viewModel.onEvent(SyncSettingsContract.Event.DeleteSmartCollection(collection.id)) }) {
                                Icon(Icons.Default.DeleteOutline, "删除 ${collection.name}")
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("开放社交来源", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "接入 Mastodon 兼容的 ActivityPub 时间线。令牌只保存在本机，不进入用户数据包或 WebDAV。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = socialName,
                        onValueChange = { socialName = it },
                        label = { Text("显示名称") },
                        placeholder = { Text("我的社区") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = socialBaseUrl,
                        onValueChange = { socialBaseUrl = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("https://mastodon.social") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = socialAccessToken,
                        onValueChange = { socialAccessToken = it },
                        label = { Text("访问令牌") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SaniouButton(
                        enabled = socialName.isNotBlank() && socialBaseUrl.isNotBlank() && socialAccessToken.isNotBlank(),
                        onClick = {
                            viewModel.onEvent(
                                SyncSettingsContract.Event.SaveSocialSource(
                                    socialName,
                                    socialBaseUrl,
                                    socialAccessToken,
                                )
                            )
                            socialName = ""
                            socialBaseUrl = ""
                            socialAccessToken = ""
                        },
                        text = "添加社交来源",
                    )
                    state.socialSources.forEach { source ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(source.displayName, style = MaterialTheme.typography.titleSmall)
                                Text(source.baseUrl, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = source.enabled,
                                onCheckedChange = {
                                    viewModel.onEvent(SyncSettingsContract.Event.ToggleSocialSource(source))
                                },
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(SyncSettingsContract.Event.DeleteSocialSource(source.id)) }
                            ) {
                                Icon(Icons.Default.DeleteOutline, "删除 ${source.displayName}")
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text("本地数据包", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "导出站点目录、Reader 订阅、收藏、阅读状态和必要设置。数据包带版本号，可在不同平台实现间复用。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SaniouButton(
                            onClick = { viewModel.onEvent(SyncSettingsContract.Event.ExportLocal) },
                            enabled = !state.isWorking,
                            loading = state.isWorking,
                        ) {
                            Icon(Icons.Default.Upload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("导出")
                        }
                        SaniouOutlinedButton(
                            onClick = { viewModel.onEvent(SyncSettingsContract.Event.ShowImportLocal) },
                            enabled = !state.isWorking,
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("导入")
                        }
                        if (userDataFileService != null) {
                            SaniouOutlinedButton(
                                onClick = {
                                    scope.launch {
                                        userDataFileService.importText().fold(
                                            onSuccess = { payload ->
                                                viewModel.onEvent(SyncSettingsContract.Event.ImportLocal(payload))
                                            },
                                            onFailure = { error ->
                                                if (error.message != "已取消导入") {
                                                    snackbar.showSnackbar(error.message ?: "导入失败")
                                                }
                                            },
                                        )
                                    }
                                },
                                enabled = !state.isWorking,
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text("从文件导入")
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                  Text("WebDAV", style = MaterialTheme.typography.titleLarge)
                  Text(
                    "配置远端 JSON 备份文件，凭据会保留以便开发和测试。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  OutlinedTextField(
                      value = state.endpoint,
                      onValueChange = { viewModel.onEvent(SyncSettingsContract.Event.EndpointChanged(it)) },
                      label = { Text("完整备份文件 URL") },
                      placeholder = { Text("https://dav.example.com/thread/backup.json") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                  )
                  WebDavCredentialFields(
                      username = state.username,
                      password = state.password,
                      stacked = LocalThreadWindowInfo.current.widthClass == ThreadWindowWidthClass.Compact,
                      onUsernameChange = { viewModel.onEvent(SyncSettingsContract.Event.UsernameChanged(it)) },
                      onPasswordChange = { viewModel.onEvent(SyncSettingsContract.Event.PasswordChanged(it)) },
                  )
                  FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                      SaniouButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.SaveWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                          text = "保存配置",
                      )
                      SaniouTextButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.ClearWebDav) },
                          enabled = !state.isWorking,
                          text = "清除",
                      )
                  }
                  FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                      SaniouOutlinedButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.BackupWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) {
                          Icon(Icons.Default.CloudUpload, null)
                          Spacer(Modifier.width(8.dp))
                          Text("立即备份")
                      }
                      SaniouOutlinedButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.RestoreWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) {
                          Icon(Icons.Default.CloudDownload, null)
                          Spacer(Modifier.width(8.dp))
                          Text("从云端恢复")
                      }
                  }
                }

                Text("运行诊断", style = MaterialTheme.typography.titleLarge)
                ThreadCard(Modifier.fillMaxWidth()) {
                    Text("Reader 自动刷新：${if (state.scheduler.isRunning) "运行中" else "已停止"}")
                    Text("待刷新 ${state.scheduler.dueCount}，正在刷新 ${state.scheduler.refreshingSourceIds.size}")
                    Text("全局刷新任务：活跃 ${state.activeRefreshCount}，失败 ${state.failedRefreshCount}")
                }
                if (state.isWorking) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            }
        }
    }
}

@Composable
private fun PreferenceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}

@Composable
private fun WebDavCredentialFields(
    username: String,
    password: String,
    stacked: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    val usernameField: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("用户名") },
            singleLine = true,
            modifier = modifier,
        )
    }
    val passwordField: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = modifier,
        )
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            usernameField(Modifier.fillMaxWidth())
            passwordField(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            usernameField(Modifier.weight(1f))
            passwordField(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserDataTransferDialog(
    dialog: UserDataDialog,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onExportToFile: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
) {
    var payload by remember(dialog) { mutableStateOf(dialog.payload) }
    AdaptiveModal(
        onDismissRequest = { if (!isWorking) onDismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (dialog.isImport) "导入用户数据" else "导出用户数据",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (dialog.isImport) {
                    "粘贴 Thread JSON 数据包。数据通过校验后才会合并。"
                } else {
                    "复制并妥善保存以下 JSON 数据包。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = payload,
                onValueChange = { if (dialog.isImport) payload = it },
                readOnly = !dialog.isImport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 480.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dialog.isImport) {
                    SaniouTextButton(onClick = onDismiss, enabled = !isWorking, text = "取消")
                    SaniouButton(
                        onClick = { onImport(payload) },
                        enabled = payload.isNotBlank() && !isWorking,
                        loading = isWorking,
                        text = "确认导入",
                    )
                } else {
                    if (onCopy != null) {
                        SaniouOutlinedButton(onClick = onCopy, text = "复制")
                    }
                    if (onExportToFile != null) {
                        SaniouOutlinedButton(onClick = onExportToFile, text = "保存到文件")
                    }
                    SaniouButton(onClick = onDismiss, text = "完成")
                }
            }
        }
    }
}
