package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.thread.domain.model.Bookmark
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object BookmarkPage : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: BookmarkViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("收藏夹") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null -> {
                        Text(
                            text = state.error!!,
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    state.bookmarks.isEmpty() -> {
                        Text(
                            text = "没有收藏",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(state.bookmarks) { bookmark ->
                                BookmarkItem(bookmark = bookmark, onBookmarkClick = {
                                    when (it) {
                                        is Bookmark.Quote -> navigator.push(ThreadPage(it.sourceId.toLong()))
                                        else -> {
                                            // 其他类型的点击事件
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkItem(bookmark: Bookmark, onBookmarkClick: (Bookmark) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookmarkClick(bookmark) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (bookmark) {
                is Bookmark.Text -> Text(text = bookmark.content, style = MaterialTheme.typography.bodyMedium)
                is Bookmark.Quote -> Text(text = bookmark.content, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                is Bookmark.Link -> Text(text = bookmark.title ?: bookmark.url, style = MaterialTheme.typography.bodyMedium)
                is Bookmark.Image -> Text(text = "图片: ${bookmark.url}", style = MaterialTheme.typography.bodyMedium)
                is Bookmark.Media -> Text(text = "媒体: ${bookmark.url}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
