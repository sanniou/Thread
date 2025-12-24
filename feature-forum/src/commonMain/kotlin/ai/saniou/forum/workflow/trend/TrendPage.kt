package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouAppBarTitle
import ai.saniou.coreui.state.DefaultError
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.workflow.thread.ThreadPage
import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.forum.workflow.trend.TrendContract.TrendItem
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI

data class TrendPage(
    val di: DI = nmbdi,
    val onMenuClick: (() -> Unit)? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: TrendViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showSourceDialog by remember { mutableStateOf(false) }
        var sourceUrl by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is Effect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }

                    is Effect.NavigateToThread -> {
                        navigator.push(ThreadPage(effect.threadId))
                    }

                    is Effect.ShowInfoDialog -> {
                        sourceUrl = effect.url
                        showSourceDialog = true
                    }
                }
            }
        }

        if (showSourceDialog) {
            val sourceId = sourceUrl.substringAfterLast("/").toLongOrNull()
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("数据来源") },
                text = {
                    Column {
                        Text("本页 Trend 数据统计自串：")
                        VerticalSpacerSmall()
                        if (sourceId != null) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("No.$sourceId")
                                    }
                                },
                                modifier = Modifier.clickable {
                                    showSourceDialog = false
                                    navigator.push(ThreadPage(sourceId))
                                }
                            )
                        } else {
                            Text(sourceUrl)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (sourceId != null) {
                                navigator.push(ThreadPage(sourceId))
                            }
                            showSourceDialog = false
                        }
                    ) {
                        Text("查看原串")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSourceDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                SaniouTopAppBar(
                    title = {
                        SaniouAppBarTitle(
                            title = "趋势",
                            subtitle = state.trendDate.takeIf { it.isNotEmpty() }
                        )
                    },
                    onNavigationClick = {
                        if (onMenuClick != null) {
                            onMenuClick.invoke()
                        } else {
                            navigator.pop()
                        }
                    },
                    navigationIcon = {
                        if (onMenuClick != null) {
                            IconButton(onClick = { onMenuClick.invoke() }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(Event.PreviousDay) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天")
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(Event.NextDay) },
                            enabled = state.dayOffset > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天")
                        }
                        IconButton(onClick = { viewModel.onEvent(Event.OnInfoClick) }) {
                            Icon(Icons.Default.Info, contentDescription = "源地址")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (state.isLoading) {
                    LoadingIndicator()
                } else if (state.error != null) {
                    DefaultError(
                        error = state.error,
                        onRetryClick = { viewModel.onEvent(Event.Refresh) }
                    )
                } else {
                    PullToRefreshWrapper(
                        onRefreshTrigger = { viewModel.onEvent(Event.Refresh) }
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items, key = { it.threadId }) { item ->
                                TrendItemCard(
                                    item = item,
                                    onClick = { viewModel.onEvent(Event.OnTrendItemClick(item.threadId)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TrendItemCard(
        item: TrendItem,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat style for list
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Rank Number
                Text(
                    text = item.rank,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.width(48.dp) // Fixed width for alignment
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Header: Forum | ID | Time?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(item.forum) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )

                        Text(
                            text = "No.${item.threadId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (item.isNew) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    VerticalSpacerSmall()

                    RichText(
                        text = item.content,
                        blankLinePolicy = BlankLinePolicy.REMOVE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )

                    VerticalSpacerSmall()

                    Text(
                        text = item.trendNum, // e.g., "Trend 34"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
