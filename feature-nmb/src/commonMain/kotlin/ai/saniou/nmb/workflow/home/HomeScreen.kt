package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.db.table.Notice
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.subscription.SubscriptionPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.DI
import org.kodein.di.instance

data class HomeScreen(val di: DI = nmbdi) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: HomeViewModel = viewModel {
            val homeViewModel by di.instance<HomeViewModel>()
            homeViewModel;
        }
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

                    AppDestinations.SHOPPING -> SubscriptionPaneScreen().Content()
                    AppDestinations.PROFILE -> SubscriptionPaneScreen().Content()
                }

                noticeState?.let { notice ->
                    // NoticeDisplay(notice)
                }

            }

        }
    }

    @Composable
    fun NoticeDisplay(notice: Notice) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = notice.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.padding(4.dp))

                Text(
                    text = "发布日期: ${notice.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "状态: ${if (notice.enable != 0L) "生效中" else "已关闭"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (notice.enable != 0L) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )

                Text(
                    text = if (notice.readed != 0L) "已阅读" else "未阅读",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (notice.readed != 0L) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

