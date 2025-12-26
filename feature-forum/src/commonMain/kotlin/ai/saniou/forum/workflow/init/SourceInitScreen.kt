package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.init.SourceInitContract.Event
import ai.saniou.forum.workflow.init.SourceInitContract.State
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.data.source.nmb.NmbSource
import org.kodein.di.instance

data class SourceInitScreen(
    val sourceId: String,
    val onInitialized: () -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val viewModel: SourceInitViewModel = rememberScreenModel(arg = sourceId)
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.isInitialized) {
            if (state.isInitialized) {
                onInitialized()
            }
        }

        Scaffold(
            topBar = {
                SaniouTopAppBar(title = "初始化 ${state.sourceName}")
            }
        ) { paddingValues ->
            StateLayout(
                state = state.uiState,
                onRetry = {},
                modifier = Modifier.padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (sourceId) {
                        "nmb" -> NmbInitContent(
                            state = state,
                            onEvent = viewModel::onEvent
                        )

                        "discourse" -> DiscourseInitContent(
                            state = state,
                            onEvent = viewModel::onEvent
                        )

                        else -> {
                            Text("未知的源: $sourceId")
                            SaniouButton(
                                text = "跳过初始化",
                                onClick = { viewModel.onEvent(Event.CompleteInitialization) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NmbInitContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Text(
        text = "欢迎使用 A岛 (NMB)。为了获得完整的体验，您可以配置订阅 ID 和饼干。",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.nmbSubscriptionKey,
        onValueChange = { onEvent(Event.UpdateNmbSubscriptionKey(it)) },
        label = { Text("订阅 ID (UUID)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Text(
        text = "用于同步订阅列表。留空将自动生成。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.nmbCookie,
        onValueChange = { onEvent(Event.UpdateNmbCookie(it)) },
        label = { Text("饼干 (Cookie)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Text(
        text = "用于发串和查看部分板块。格式通常为 userhash=xxx",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    SaniouButton(
        text = "完成初始化",
        onClick = { onEvent(Event.CompleteInitialization) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DiscourseInitContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Text(
        text = "欢迎使用 Discourse。请输入您的 API Key 以访问受限内容。",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.discourseApiKey,
        onValueChange = { onEvent(Event.UpdateDiscourseApiKey(it)) },
        label = { Text("API Key") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.discourseUsername,
        onValueChange = { onEvent(Event.UpdateDiscourseUsername(it)) },
        label = { Text("用户名") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(24.dp))

    SaniouButton(
        text = "完成初始化",
        onClick = { onEvent(Event.CompleteInitialization) },
        modifier = Modifier.fillMaxWidth()
    )
}
