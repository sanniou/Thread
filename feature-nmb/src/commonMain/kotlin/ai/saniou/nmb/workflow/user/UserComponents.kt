package ai.saniou.nmb.workflow.user

import ai.saniou.thread.domain.model.forum.Cookie
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookieListContent(
    cookies: List<Cookie>,
    onDelete: (Cookie) -> Unit,
    onSortFinished: (List<Cookie>) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var localCookies by remember(cookies) { mutableStateOf(cookies) }

    val state = rememberReorderableLazyListState(listState) { from, to ->
        localCookies = localCookies.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onSortFinished(localCookies)
    }

    if (cookies.isEmpty()) {
        EmptyCookieList()
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(localCookies, key = { it.value }) { cookie ->
                ReorderableItem(state, key = cookie.value) { isDragging ->
                    CookieItem(
                        cookie = cookie,
                        onDelete = { onDelete(cookie) },
                        isDragging = isDragging,
                        modifier = Modifier.draggableHandle(),
                    )
                }
            }
        }
    }
}

@Composable
fun CookieItem(
    cookie: Cookie,
    onDelete: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cookie.alias ?: "未命名",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除饼干")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cookie.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatEpochSeconds(cookie.createdAt),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Serializable
private data class CookieJson(
    val cookie: String,
    val name: String? = null
)

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
                    onValueChange = { newValue ->
                        val trimmed = newValue.trim()
                        if (trimmed.startsWith("{")) {
                            try {
                                val cookieJson = Json.decodeFromString<CookieJson>(trimmed)
                                value = cookieJson.cookie
                                name = cookieJson.name ?: ""
                            } catch (e: Exception) {
                                value = newValue
                            }
                        } else {
                            value = newValue
                        }
                    },
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

@OptIn(ExperimentalTime::class)
private fun formatEpochSeconds(epochSeconds: Long): String {
   return try {
       val instant = Instant.fromEpochSeconds(epochSeconds)
       val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
       val year = localDateTime.year
       val month = localDateTime.monthNumber.toString().padStart(2, '0')
       val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
       val hour = localDateTime.hour.toString().padStart(2, '0')
       val minute = localDateTime.minute.toString().padStart(2, '0')
       "$year-$month-$day $hour:$minute"
   } catch (e: Exception) {
       "Invalid Date"
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
