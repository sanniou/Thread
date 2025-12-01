package ai.saniou.nmb.workflow.trend

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.workflow.home.SaniouAppBar
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.nmb.workflow.trend.TrendContract.Effect
import ai.saniou.nmb.workflow.trend.TrendContract.Event
import ai.saniou.nmb.workflow.trend.TrendContract.TrendItem
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI

data class TrendPage(val di: DI = nmbdi) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: TrendViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

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
                        // TODO: Use actual platform-specific URL opener or dialog
                        snackbarHostState.showSnackbar("源地址: ${effect.url}")
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                SaniouAppBar(
                    canNavigateBack = false,
                    navigateUp = { },
                    onUserIconClick = { /* Handled by parent */ },
                    onMenuClick = { /* Handled by parent */ },
                    showMenuIcon = false, // Trend page is usually part of bottom nav, handled by parent scafffold
                    customTitle = "趋势" + if (state.trendDate.isNotEmpty()) " - ${state.trendDate}" else "",
                    extraActions = {
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
                    LoadingFailedIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { viewModel.onEvent(Event.Refresh) }
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
        onClick: () -> Unit
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

                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 1.5.em
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