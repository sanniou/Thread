package ai.saniou.forum.workflow.user

import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.forum.Account
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_35563060dc
import thread.feature_forum.generated.resources.s_92a1ea8c23
import thread.feature_forum.generated.resources.s_c699063a5e
import thread.feature_forum.generated.resources.s_cf70d344a7
import thread.feature_forum.generated.resources.s_f9a1271237

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
                        modifier = threadAnimateItem().then(Modifier.draggableHandle()),
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
    val displayValue = cookie.uid?.let { stringResource(Res.string.s_92a1ea8c23, it) } ?: stringResource(Res.string.s_f9a1271237)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDragging) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cookie.alias ?: stringResource(Res.string.s_35563060dc),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.s_cf70d344a7),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cookie.createdAt.toRelativeTimeString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
@Composable
fun EmptyCookieList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.s_c699063a5e),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
