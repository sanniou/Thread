package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadContentColumn
import ai.saniou.coreui.widgets.ThreadPage
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.OutlinedTextField
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

        ThreadPage {
          ThreadContentColumn(modifier = Modifier.fillMaxSize()) {
            ContextHero(
                icon = Icons.Default.Hub,
                title = "启用 ${state.sourceName}",
                subtitle = "建立跨平台订阅身份并准备该来源的本地状态",
                metric = "ONE-TIME SETUP",
            )
            StateLayout(
                state = state.uiState,
                onRetry = { viewModel.onEvent(Event.CompleteInitialization) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
              Column(
                  modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                  verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
              ) {
                ThreadCard(modifier = Modifier.fillMaxWidth()) {
                    Text("来源初始化", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "准备启用 ${state.sourceName}。账号凭据可在用户中心通过该来源声明的登录方式添加。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "初始化只创建通用订阅标识和来源状态，不在界面层保存平台专用配置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SaniouButton(
                        text = "完成初始化",
                        onClick = { viewModel.onEvent(Event.CompleteInitialization) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
              }
            }
          }
        }
    }
}
