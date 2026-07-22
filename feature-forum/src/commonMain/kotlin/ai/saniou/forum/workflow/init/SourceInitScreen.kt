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
import androidx.compose.ui.text.font.FontWeight
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
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_3dcc0e09cc
import thread.feature_forum.generated.resources.s_7b7f6285e9
import thread.feature_forum.generated.resources.s_85fb4f2e67
import thread.feature_forum.generated.resources.s_8f9660d07c
import thread.feature_forum.generated.resources.s_b1207c605f
import thread.feature_forum.generated.resources.s_c160ae5e85

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
                title = stringResource(Res.string.s_7b7f6285e9, state.sourceName),
                subtitle = stringResource(Res.string.s_c160ae5e85),
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
                    Text(stringResource(Res.string.s_85fb4f2e67), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stringResource(Res.string.s_b1207c605f, state.sourceName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.s_3dcc0e09cc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SaniouButton(
                        text = stringResource(Res.string.s_8f9660d07c),
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
