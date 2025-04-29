package ai.saniou.nmb.workflow.thread

import ai.saniou.nmb.data.NmbScreen
import ai.saniou.nmb.workflow.home.NavigationDestination
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

object ThreadPageNavigationDestination : NavigationDestination {
    override val route = NmbScreen.Thread.name
    const val nameArg = "tid"
    val routeWithArg = "$route/{$nameArg}"
}
