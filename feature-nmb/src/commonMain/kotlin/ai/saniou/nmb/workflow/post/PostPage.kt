package ai.saniou.nmb.workflow.post

import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.direct
import org.kodein.di.instance

data class PostPage(
    val fid: Int? = null,
    val resto: Int? = null,
    val forumName: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: PostViewModel = rememberScreenModel(tag = "${fid}_${resto}") {
            nmbdi.direct.instance(arg = Triple(fid, resto, forumName))
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is PostContract.Effect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                    PostContract.Effect.NavigateBack -> navigator.pop()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (resto != null) "回复" else "发帖: ${state.forumName}") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (resto == null) { // Only show for new posts
                    OutlinedTextField(
                        value = state.postBody.name ?: "",
                        onValueChange = { viewModel.onEvent(PostContract.Event.UpdateName(it)) },
                        label = { Text("名称 (可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.postBody.title ?: "",
                        onValueChange = { viewModel.onEvent(PostContract.Event.UpdateTitle(it)) },
                        label = { Text("标题 (可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = state.postBody.content ?: "",
                    onValueChange = { viewModel.onEvent(PostContract.Event.UpdateContent(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("内容") },
                    isError = state.error != null
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { /* TODO: Implement image picker */ }) {
                        Icon(Icons.Default.Info, contentDescription = "选择图片")
                        Text("图片")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.water,
                            onCheckedChange = { viewModel.onEvent(PostContract.Event.ToggleWater(it)) }
                        )
                        Text("水印")
                    }
                }
                Button(
                    onClick = { viewModel.onEvent(PostContract.Event.Submit) },
                    enabled = state.postBody.content?.isNotBlank() == true && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                    Text("发送")
                }
            }
        }
    }
}
