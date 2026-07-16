package ai.saniou.thread.feature.settings

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadPage
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance

class SyncSettingsPage : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<SyncSettingsViewModel>() }
        val state by viewModel.state.collectAsState()
        val snackbar = remember { SnackbarHostState() }

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
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
          ThreadPage(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.page_horizontal, vertical = Dimens.page_vertical),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PageHeader(
                    title = "数据与同步",
                    eyebrow = "PORTABILITY",
                    subtitle = "管理本地数据包、WebDAV 备份与运行状态",
                    actions = {
                        IconButton(onClick = navigator::pop) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                )
                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text("本地数据包", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "导出站点目录、Reader 订阅、收藏、阅读状态和必要设置。数据包带版本号，可在不同平台实现间复用。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.onEvent(SyncSettingsContract.Event.ExportLocal) },
                            enabled = !state.isWorking,
                        ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.width(8.dp)); Text("导出") }
                        OutlinedButton(
                            onClick = { viewModel.onEvent(SyncSettingsContract.Event.ShowImportLocal) },
                            enabled = !state.isWorking,
                        ) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("导入") }
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
                  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                      OutlinedTextField(
                          value = state.username,
                          onValueChange = { viewModel.onEvent(SyncSettingsContract.Event.UsernameChanged(it)) },
                          label = { Text("用户名") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                      )
                      OutlinedTextField(
                          value = state.password,
                          onValueChange = { viewModel.onEvent(SyncSettingsContract.Event.PasswordChanged(it)) },
                          label = { Text("密码") },
                          singleLine = true,
                          visualTransformation = PasswordVisualTransformation(),
                          modifier = Modifier.weight(1f),
                      )
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                      Button(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.SaveWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) { Text("保存配置") }
                      TextButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.ClearWebDav) },
                          enabled = !state.isWorking,
                      ) { Text("清除") }
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                      OutlinedButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.BackupWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) { Icon(Icons.Default.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("立即备份") }
                      OutlinedButton(
                          onClick = { viewModel.onEvent(SyncSettingsContract.Event.RestoreWebDav) },
                          enabled = state.endpoint.isNotBlank() && !state.isWorking,
                      ) { Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("从云端恢复") }
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
private fun UserDataTransferDialog(
    dialog: UserDataDialog,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var payload by remember(dialog) { mutableStateOf(dialog.payload) }
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text(if (dialog.isImport) "导入用户数据" else "导出用户数据") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (dialog.isImport) "粘贴 Thread JSON 数据包。数据通过校验后才会合并。" else "复制并妥善保存以下 JSON 数据包。")
                OutlinedTextField(
                    value = payload,
                    onValueChange = { if (dialog.isImport) payload = it },
                    readOnly = !dialog.isImport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 480.dp),
                )
            }
        },
        confirmButton = {
            if (dialog.isImport) {
                Button(onClick = { onImport(payload) }, enabled = payload.isNotBlank() && !isWorking) {
                    Text(if (isWorking) "导入中…" else "确认导入")
                }
            } else Button(onClick = onDismiss) { Text("完成") }
        },
        dismissButton = {
            if (dialog.isImport) TextButton(onClick = onDismiss, enabled = !isWorking) { Text("取消") }
        },
    )
}
