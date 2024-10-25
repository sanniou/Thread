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

@Composable
fun ThreadPage(name: Long?) {
    Text(
        "A\nB\nC\nD\nE\nF\nG\nH\nI\nJ\nK\nL\nM\nN\nO\nP\nQ\nR\nS\nT\nU\nV\nW\nX\nY\nZ\n"
                + NmbScreen.Thread.name + " " + name
    )
}
