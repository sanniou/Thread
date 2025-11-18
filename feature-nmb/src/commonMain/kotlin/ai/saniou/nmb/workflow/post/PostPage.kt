package ai.saniou.nmb.workflow.post

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI

@OptIn(ExperimentalMaterial3Api::class)
data class PostPage(
    val forumId: Long,
    val threadId: Long? = null,
    val di: DI = nmbdi,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val isReply = threadId != null
        val title = if (isReply) "回复" else "发帖"

        val postViewModel: PostViewModel = rememberScreenModel()

        val uiState by postViewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (isReply && threadId != null) {
                                postViewModel.submitReply()
                            } else if (forumId != null) {
                                postViewModel.submitThread()
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "提交")
                        }
                    }
                )
            }
        ) { paddingValues ->
            uiState.LoadingWrapper<PostUiState>(
                content = { state ->
                    PostContent(
                        state = state,
                        isReply = isReply,
                        modifier = Modifier.padding(paddingValues)
                    )
                },
                onRetryClick = {
                    // 重试逻辑
                }
            )
        }
    }

    @Composable
    fun PostContent(
        state: PostUiState,
        isReply: Boolean,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(16.dp)
        ) {
            // 标题输入框（发帖时显示）
            if (!isReply) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { state.onTitleChanged(it) },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 内容输入框
            OutlinedTextField(
                value = state.content,
                onValueChange = { state.onContentChanged(it) },
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 图片上传区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable { state.onSelectImage() }
            ) {
                if (state.hasImage) {
                    // 显示已选择的图片
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "已选择图片",
                            modifier = Modifier.align(Alignment.Center)
                        )

                        IconButton(
                            onClick = { state.onClearImage() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "移除图片")
                        }
                    }
                } else {
                    // 显示添加图片按钮
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加图片",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("添加图片")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 水印选项
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.addWatermark,
                    onCheckedChange = { state.onWatermarkChanged(it) }
                )
                Text("添加水印")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提交按钮
            Button(
                onClick = { state.onSubmit() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isReply) "回复" else "发帖")
            }
        }
    }
}
