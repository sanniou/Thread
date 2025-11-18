package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.db.table.Notice
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.history.HistoryPage
import ai.saniou.nmb.workflow.subscription.SubscriptionPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.DI
import org.kodein.di.compose.viewmodel.rememberViewModel
import org.kodein.di.instance

data class HomeScreen(val di: DI = nmbdi) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: HomeViewModel by rememberViewModel()
        val noticeState by viewModel.noticeState.collectAsStateWithLifecycle()

        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        val snackbarHostState = remember { SnackbarHostState() }
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = stringResource(it.contentDescription)
                            )
                        },
                        label = { Text(stringResource(it.label)) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) {

                when (currentDestination) {
                    AppDestinations.HOME -> HomePage()
                    AppDestinations.FAVORITES -> SubscriptionPage(onThreadClicked = {
                        navigator.push(ThreadPage(it))
                    }).Content()

                    AppDestinations.HISTORY -> HistoryPage().Content()
                    AppDestinations.SHOPPING -> SubscriptionPaneScreen().Content()
                    AppDestinations.PROFILE -> SubscriptionPaneScreen().Content()
                }

                noticeState?.let { notice ->
                    NoticeDisplay(notice, onDismiss = { viewModel.markAsRead() })
                }

            }

        }
    }

    @Composable
    fun NoticeDisplay(notice: Notice, onDismiss: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "公告",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = notice.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("不再显示")
                    }
                }
            }
        }
    }
}

