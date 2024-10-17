package ai.saniou.nmb

import ai.saniou.corecommon.getPlatform
import ai.saniou.coreui.Greeting
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.mock.ApiTest
import ai.saniou.nmb.di.nmbdi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.instance
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {

    val exampleApi: NmbXdApi by nmbdi.instance()



    MaterialTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Text("Drawer title", modifier = Modifier.padding(16.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(text = "Drawer Item") },
                        selected = false,
                        onClick = { /*TODO*/ }
                    )
                    // ...other drawer items
                }
            }
        ) {
            var showContent by remember { mutableStateOf(false) }
            var showContentText by remember { mutableStateOf("") }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    showContent = !showContent
                    GlobalScope.launch {
                        println("Ktorfit:" + getPlatform().name + ":" + exampleApi.getTimelineList())
                    }
                }) {
                    Text("Click me! $showContentText")
                }
                AnimatedVisibility(showContent) {
                    val greeting = remember { Greeting().greet() }
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(painterResource(Res.drawable.compose_multiplatform), null)
                        Text("Compose: $greeting")
                    }
                }
            }
        }
    }
}

