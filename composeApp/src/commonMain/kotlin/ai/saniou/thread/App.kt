package ai.saniou.thread

import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Games
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
fun App() {
    MaterialTheme {
        Navigator(HomeScreen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { TopAppBar(title = { Text("Thread :: Demos") }) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigator.push(CellularAutomatonRoute) }
                ) {
                    ListItem(
                        headlineContent = { Text("元胞自动机") },
                        supportingContent = { Text("一个基于康威生命游戏规则的简单模拟器。") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Games,
                                contentDescription = "Cellular Automaton"
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Enter"
                            )
                        }
                    )
                }
            }
        }
    }
}

object CellularAutomatonRoute : Screen {
    @Composable
    override fun Content() {
        CellularAutomatonScreen()
    }
}
