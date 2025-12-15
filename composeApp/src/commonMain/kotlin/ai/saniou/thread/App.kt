package ai.saniou.thread

import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.widgets.DrawerMenuItem
import ai.saniou.coreui.widgets.DrawerMenuRow
import ai.saniou.forum.di.coreCommon
import ai.saniou.forum.di.nmbFeatureModule
import ai.saniou.forum.workflow.home.ForumCategoryPage
import ai.saniou.forum.workflow.image.nmbImagePreviewModule
import ai.saniou.reader.di.readerModule
import ai.saniou.reader.workflow.reader.ReaderPage
import ai.saniou.thread.data.di.dataModule
import ai.saniou.thread.domain.di.domainModule
import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI
import org.kodein.di.compose.withDI

@Composable
fun App() {
    val di = DI {
        import(domainModule)
        import(dataModule)
        import(readerModule)
        import(coreCommon)
        import(nmbImagePreviewModule)
        import(nmbFeatureModule)
    }

    withDI(di) {
        MaterialTheme {
            Navigator(ForumRoute) { navigator ->
                val appDrawer = @Composable {
                    DrawerMenuRow(
                        menuItems = listOf(
                            DrawerMenuItem(
                                Icons.Default.Forum,
                                "匿名版"
                            ) { navigator.replaceAll(ForumRoute) },
                            DrawerMenuItem(
                                Icons.Default.RssFeed,
                                "阅读器"
                            ) { navigator.replaceAll(ReaderRoute) },
                            DrawerMenuItem(
                                Icons.Default.Games,
                                "元胞自动机"
                            ) { navigator.replaceAll(CellularAutomatonRoute) },
                        )
                    )
                }

                CompositionLocalProvider(
                    LocalAppDrawer provides appDrawer
                ) {
                    navigator.lastItem.Content()
                }
            }
        }
    }
}

object ForumRoute : Screen {
    @Composable
    override fun Content() {
        ForumCategoryPage().Content()
    }
}

object CellularAutomatonRoute : Screen {
    @Composable
    override fun Content() {
        CellularAutomatonScreen()
    }
}

object ReaderRoute : Screen {
    @Composable
    override fun Content() {
        ReaderPage().Content()
    }
}
