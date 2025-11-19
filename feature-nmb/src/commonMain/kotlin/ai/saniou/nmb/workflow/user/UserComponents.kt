package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.db.table.Cookie
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookieListContent(
    cookies: List<Cookie>,
    onDelete: (Cookie) -> Unit,
    onSortFinished: (List<Cookie>) -> Unit,
) {
    var localCookies by remember(cookies) { mutableStateOf(cookies) }
    val lazyListState = rememberLazyListState()

    val state = rememberReorderableLazyListState(lazyListState) { from, to ->
        localCookies = localCookies.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onSortFinished(localCookies)
    }

    if (cookies.isEmpty()) {
        EmptyCookieList()
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(localCookies, key = { it.cookie }) { cookie ->
                ReorderableItem(state, key = cookie.cookie) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                    Surface(shadowElevation = elevation) {
                        CookieItem(
                            cookie = cookie,
                            onDelete = { onDelete(cookie) },
                            modifier = Modifier.draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CookieItem(
    cookie: Cookie,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(cookie.alias ?: "未命名") },
        supportingContent = {
            Column {
                Text(
                    text = "值: ${cookie.cookie}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "添加时间: ${cookie.createdAt}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除饼干")
            }
        }
    )
}

@Composable
fun AddCookieDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加饼干") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称 (可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("值") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, value) },
                enabled = value.isNotBlank()
            ) {
                Text("确认")
            }
        }
    )
}

@Composable
fun UserGuideCard(onOpenUri: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClick = onOpenUri),
            headlineContent = { Text("如何获取饼干？") },
            supportingContent = { Text("在网页端登录后，请访问指定页面获取。") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "信息"
                )
            }
        )
    }
}

@Composable
fun EmptyCookieList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "还没有饼干\n点击右下角的按钮添加一个吧",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
