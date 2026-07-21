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
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_038184cabc
import thread.composeapp.generated.resources.s_05b0dbf37a
import thread.composeapp.generated.resources.s_06f94a77b2
import thread.composeapp.generated.resources.s_10119439c7
import thread.composeapp.generated.resources.s_188896795f
import thread.composeapp.generated.resources.s_1ec7e560da
import thread.composeapp.generated.resources.s_2c3ece162f
import thread.composeapp.generated.resources.s_2c7433dbae
import thread.composeapp.generated.resources.s_2eb291f695
import thread.composeapp.generated.resources.s_2f731abd93
import thread.composeapp.generated.resources.s_30b2c979ac
import thread.composeapp.generated.resources.s_31e11f96c9
import thread.composeapp.generated.resources.s_32d9d4ba90
import thread.composeapp.generated.resources.s_33246f6a5e
import thread.composeapp.generated.resources.s_342dc7b5e9
import thread.composeapp.generated.resources.s_351619af9a
import thread.composeapp.generated.resources.s_3930ecdc05
import thread.composeapp.generated.resources.s_3cce4b557d
import thread.composeapp.generated.resources.s_3f01ff8d8d
import thread.composeapp.generated.resources.s_430050ae51
import thread.composeapp.generated.resources.s_4373b82f21
import thread.composeapp.generated.resources.s_44ceb01368
import thread.composeapp.generated.resources.s_49c7737142
import thread.composeapp.generated.resources.s_4a88d2ab0b
import thread.composeapp.generated.resources.s_4d0b4688c7
import thread.composeapp.generated.resources.s_4e2f1163fc
import thread.composeapp.generated.resources.s_4edd1d0087
import thread.composeapp.generated.resources.s_56fdd69721
import thread.composeapp.generated.resources.s_58333db940
import thread.composeapp.generated.resources.s_5d95923b6f
import thread.composeapp.generated.resources.s_60e2bcad85
import thread.composeapp.generated.resources.s_6525ca3010
import thread.composeapp.generated.resources.s_6753381261
import thread.composeapp.generated.resources.s_70a5ae0f89
import thread.composeapp.generated.resources.s_71c9730dfe
import thread.composeapp.generated.resources.s_73bf8411b8
import thread.composeapp.generated.resources.s_73e3355932
import thread.composeapp.generated.resources.s_75ae6a8a7d
import thread.composeapp.generated.resources.s_774fb84fcf
import thread.composeapp.generated.resources.s_7b15e5e8e7
import thread.composeapp.generated.resources.s_80ec9e2b1b
import thread.composeapp.generated.resources.s_817af1870c
import thread.composeapp.generated.resources.s_8502e30b3b
import thread.composeapp.generated.resources.s_86773005b9
import thread.composeapp.generated.resources.s_86dab44bc6
import thread.composeapp.generated.resources.s_9301c30bde
import thread.composeapp.generated.resources.s_967f446633
import thread.composeapp.generated.resources.s_97d8a6c05b
import thread.composeapp.generated.resources.s_9a1c91d13c
import thread.composeapp.generated.resources.s_9ab6e56a49
import thread.composeapp.generated.resources.s_9abbfdbe4b
import thread.composeapp.generated.resources.s_a01a8d5393
import thread.composeapp.generated.resources.s_a1aaf352cb
import thread.composeapp.generated.resources.s_ad25121b16
import thread.composeapp.generated.resources.s_ad30571e81
import thread.composeapp.generated.resources.s_b4cdd77c3f
import thread.composeapp.generated.resources.s_b703aea0f9
import thread.composeapp.generated.resources.s_b75af5b178
import thread.composeapp.generated.resources.s_b7da86531d
import thread.composeapp.generated.resources.s_b97c3bc09e
import thread.composeapp.generated.resources.s_c0a9a35642
import thread.composeapp.generated.resources.s_c38cfe81c7
import thread.composeapp.generated.resources.s_c5e431f732
import thread.composeapp.generated.resources.s_c831720c36
import thread.composeapp.generated.resources.s_c837edc258
import thread.composeapp.generated.resources.s_c839a8ff17
import thread.composeapp.generated.resources.s_c8feb7b4cf
import thread.composeapp.generated.resources.s_c929a1d2a2
import thread.composeapp.generated.resources.s_ce14673623
import thread.composeapp.generated.resources.s_cf4b7d95a2
import thread.composeapp.generated.resources.s_d8e616516f
import thread.composeapp.generated.resources.s_dc35af8d69
import thread.composeapp.generated.resources.s_de6433b70d
import thread.composeapp.generated.resources.s_e3ec414278
import thread.composeapp.generated.resources.s_e5b4eb9f7c
import thread.composeapp.generated.resources.s_e848ddd482
import thread.composeapp.generated.resources.s_ed59779c27
import thread.composeapp.generated.resources.s_f4bbd91f79

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
            title = stringResource(Res.string.s_9abbfdbe4b),
            eyebrow = stringResource(Res.string.s_774fb84fcf),
            subtitle = stringResource(Res.string.s_3930ecdc05),
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
                    Text(stringResource(Res.string.s_4373b82f21), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.s_c831720c36),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(Res.string.s_e848ddd482), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.appearance.themeMode == mode,
                                onClick = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(themeMode = mode))) },
                                label = { Text(when (mode) { ThemeMode.SYSTEM -> stringResource(Res.string.s_f4bbd91f79); ThemeMode.LIGHT -> stringResource(Res.string.s_80ec9e2b1b); ThemeMode.DARK -> stringResource(Res.string.s_30b2c979ac) }) },
                            )
                        }
                    }
                    Text(stringResource(Res.string.s_2c7433dbae), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InterfaceDensity.entries.forEach { density ->
                            FilterChip(
                                selected = state.appearance.density == density,
                                onClick = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(density = density))) },
                                label = { Text(when (density) { InterfaceDensity.COMPACT -> stringResource(Res.string.s_c837edc258); InterfaceDensity.COMFORTABLE -> stringResource(Res.string.s_b97c3bc09e); InterfaceDensity.SPACIOUS -> stringResource(Res.string.s_b7da86531d) }) },
                            )
                        }
                    }
                    PreferenceSlider(
                        label = stringResource(Res.string.s_05b0dbf37a, (state.appearance.fontScale * 100).toInt()),
                        value = state.appearance.fontScale,
                        range = 0.85f..1.4f,
                        onValue = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(fontScale = it))) },
                    )
                    PreferenceSlider(
                        label = stringResource(Res.string.s_ce14673623, state.appearance.readerWidthDp),
                        value = state.appearance.readerWidthDp.toFloat(),
                        range = 520f..1080f,
                        onValue = { viewModel.onEvent(SyncSettingsContract.Event.AppearanceChanged(state.appearance.copy(readerWidthDp = it.toInt()))) },
                    )
                    PreferenceSlider(
                        label = stringResource(Res.string.s_c5e431f732, state.appearance.readerLineHeight),
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
                            Text(stringResource(Res.string.s_ad25121b16), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(Res.string.s_967f446633), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = stringResource(Res.string.s_ad30571e81),
                    )
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.s_9ab6e56a49), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.s_9301c30bde),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = collectionName,
                        onValueChange = { collectionName = it },
                        label = { Text(stringResource(Res.string.s_06f94a77b2)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = collectionQuery,
                        onValueChange = { collectionQuery = it },
                        label = { Text(stringResource(Res.string.s_31e11f96c9)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(collectionUnread, { collectionUnread = !collectionUnread }, { Text(stringResource(Res.string.s_342dc7b5e9)) })
                        FilterChip(collectionBookmarked, { collectionBookmarked = !collectionBookmarked }, { Text(stringResource(Res.string.s_70a5ae0f89)) })
                    }
                    Text(stringResource(Res.string.s_dc35af8d69), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmartCollectionSort.entries.forEach { sort ->
                            FilterChip(
                                selected = collectionSort == sort,
                                onClick = { collectionSort = sort },
                                label = { Text(when (sort) {
                                    SmartCollectionSort.NEWEST -> stringResource(Res.string.s_44ceb01368)
                                    SmartCollectionSort.OLDEST -> stringResource(Res.string.s_4e2f1163fc)
                                    SmartCollectionSort.RELEVANCE -> stringResource(Res.string.s_3f01ff8d8d)
                                }) },
                            )
                        }
                    }
                    Text(stringResource(Res.string.s_97d8a6c05b), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmartCollectionGroup.entries.forEach { group ->
                            FilterChip(
                                selected = collectionGroup == group,
                                onClick = { collectionGroup = group },
                                label = { Text(when (group) {
                                    SmartCollectionGroup.NONE -> stringResource(Res.string.s_73e3355932)
                                    SmartCollectionGroup.SOURCE -> stringResource(Res.string.s_2c3ece162f)
                                    SmartCollectionGroup.CONTENT_KIND -> stringResource(Res.string.s_c0a9a35642)
                                    SmartCollectionGroup.AUTHOR -> stringResource(Res.string.s_3cce4b557d)
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
                        text = stringResource(Res.string.s_9a1c91d13c),
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
                            }) { Icon(Icons.Default.KeyboardArrowUp, stringResource(Res.string.s_c38cfe81c7, collection.name)) }
                            IconButton(onClick = {
                                viewModel.onEvent(SyncSettingsContract.Event.MoveSmartCollection(collection.id, 1))
                            }) { Icon(Icons.Default.KeyboardArrowDown, stringResource(Res.string.s_e5b4eb9f7c, collection.name)) }
                            IconButton(onClick = {
                                viewModel.onEvent(SyncSettingsContract.Event.ToggleSmartCollectionPinned(collection.id, !collection.pinned))
                            }) {
                                Icon(if (collection.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin, stringResource(Res.string.s_32d9d4ba90, collection.name))
                            }
                            IconButton(onClick = { viewModel.onEvent(SyncSettingsContract.Event.DeleteSmartCollection(collection.id)) }) {
                                Icon(Icons.Default.DeleteOutline, stringResource(Res.string.s_430050ae51, collection.name))
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.s_6525ca3010), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(Res.string.s_2f731abd93),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = socialName,
                        onValueChange = { socialName = it },
                        label = { Text(stringResource(Res.string.s_75ae6a8a7d)) },
                        placeholder = { Text(stringResource(Res.string.s_71c9730dfe)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = socialBaseUrl,
                        onValueChange = { socialBaseUrl = it },
                        label = { Text(stringResource(Res.string.s_2eb291f695)) },
                        placeholder = { Text("https://mastodon.social") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = socialAccessToken,
                        onValueChange = { socialAccessToken = it },
                        label = { Text(stringResource(Res.string.s_73bf8411b8)) },
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
                        text = stringResource(Res.string.s_351619af9a),
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
                                Icon(Icons.Default.DeleteOutline, stringResource(Res.string.s_430050ae51, source.displayName))
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.s_cf4b7d95a2), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.s_e3ec414278),
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
                            Text(stringResource(Res.string.s_188896795f))
                        }
                        SaniouOutlinedButton(
                            onClick = { viewModel.onEvent(SyncSettingsContract.Event.ShowImportLocal) },
                            enabled = !state.isWorking,
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.s_60e2bcad85))
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
                                Text(stringResource(Res.string.s_58333db940))
                            }
                        }
                    }
                }

                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                  Text("WebDAV", style = MaterialTheme.typography.titleLarge)
                  Text(
                    stringResource(Res.string.s_c929a1d2a2),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  OutlinedTextField(
                      value = state.endpoint,
                      onValueChange = { viewModel.onEvent(SyncSettingsContract.Event.EndpointChanged(it)) },
                      label = { Text(stringResource(Res.string.s_d8e616516f)) },
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
                          text = stringResource(Res.string.s_817af1870c),
                      )
                      SaniouTextButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.ClearWebDav) },
                          enabled = !state.isWorking,
                          text = stringResource(Res.string.s_7b15e5e8e7),
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
                          Text(stringResource(Res.string.s_10119439c7))
                      }
                      SaniouOutlinedButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.RestoreWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) {
                          Icon(Icons.Default.CloudDownload, null)
                          Spacer(Modifier.width(8.dp))
                          Text(stringResource(Res.string.s_b75af5b178))
                      }
                  }
                }

                Text(stringResource(Res.string.s_5d95923b6f), style = MaterialTheme.typography.titleLarge)
                ThreadCard(Modifier.fillMaxWidth()) {
                    Text("Reader 自动刷新：${if (state.scheduler.isRunning) "运行中" else "已停止"}")
                    Text(stringResource(Res.string.s_ed59779c27, state.scheduler.dueCount, state.scheduler.refreshingSourceIds.size))
                    Text(stringResource(Res.string.s_b4cdd77c3f, state.activeRefreshCount, state.failedRefreshCount))
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
            label = { Text(stringResource(Res.string.s_a1aaf352cb)) },
            singleLine = true,
            modifier = modifier,
        )
    }
    val passwordField: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(Res.string.s_c839a8ff17)) },
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
                if (dialog.isImport) stringResource(Res.string.s_86dab44bc6) else stringResource(Res.string.s_86773005b9),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (dialog.isImport) {
                    stringResource(Res.string.s_038184cabc)
                } else {
                    stringResource(Res.string.s_1ec7e560da)
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
                    SaniouTextButton(onClick = onDismiss, enabled = !isWorking, text = stringResource(Res.string.s_4d0b4688c7))
                    SaniouButton(
                        onClick = { onImport(payload) },
                        enabled = payload.isNotBlank() && !isWorking,
                        loading = isWorking,
                        text = stringResource(Res.string.s_de6433b70d),
                    )
                } else {
                    if (onCopy != null) {
                        SaniouOutlinedButton(onClick = onCopy, text = stringResource(Res.string.s_4edd1d0087))
                    }
                    if (onExportToFile != null) {
                        SaniouOutlinedButton(onClick = onExportToFile, text = stringResource(Res.string.s_4a88d2ab0b))
                    }
                    SaniouButton(onClick = onDismiss, text = stringResource(Res.string.s_33246f6a5e))
                }
            }
        }
    }
}
