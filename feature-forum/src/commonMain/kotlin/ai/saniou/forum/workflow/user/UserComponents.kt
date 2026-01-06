package ai.saniou.forum.workflow.user

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.thread.data.source.tieba.TiebaMapper
import ai.saniou.thread.domain.model.forum.Account
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookieListContent(
    cookies: List<Account>,
    onDelete: (Account) -> Unit,
    onSortFinished: (List<Account>) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
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
            items(localCookies, key = { it.id }) { cookie ->
                ReorderableItem(state, key = cookie.id) { isDragging ->
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
    cookie: Account,
    onDelete: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp)
    val displayValue = if (cookie.sourceId == "tieba") cookie.alias ?: cookie.uid else cookie.value

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
                    Icon(Icons.Default.Delete, contentDescription = "删除账号")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayValue ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cookie.createdAt.toRelativeTimeString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}


@Composable
fun UserGuideCard(onOpenUri: () -> Unit, modifier: Modifier = Modifier) {
    val sourceId = LocalForumSourceId.current

    if (sourceId != TiebaMapper.SOURCE_ID) {
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
            text = "还没有账号\n点击右下角的按钮添加一个吧",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}