package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.body1
import ai.saniou.coreui.theme.risBlack
import ai.saniou.coreui.theme.subtitle1Bold
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.compose_multiplatform
import thread.core_ui.generated.resources.core_ui_net_un_connect
import thread.core_ui.generated.resources.core_ui_net_un_connect_and_retry
import thread.core_ui.generated.resources.core_ui_no_internet_retry
import thread.core_ui.generated.resources.core_ui_server_busy
import thread.core_ui.generated.resources.core_ui_server_error


enum class MBErrorPageType {
    NETWORK,
    SERVER,
}

@Composable
fun MBErrorPage(
    type: MBErrorPageType,
    onRetryClick: () -> Unit,
    onNavBack: () -> Unit = {},
    isShowToolBar: Boolean = false,
    title: String = ""
) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        if (isShowToolBar) {
            MBToolbar(
                title,
                onLeftIconClick = {
                    onNavBack()
                }
            )
        }
    }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navigationBarHeight()),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier.size(80.dp),
                        painter = painterResource(type.iconResId()),
                        contentDescription = "",
                    )
                    Text(
                        modifier = Modifier.padding(top = 20.dp),
                        text = stringResource(type.errorLabelResId()),
                        style = subtitle1Bold,
                        color = risBlack()
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(type.errorOptionGuidResId()),
                        style = body1,
                        color = risBlack()
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                MBNormalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    stringResource(Res.string.core_ui_no_internet_retry),
                    onClick = {
                        onRetryClick()
                    }
                )
            }
        }
    }
}

fun navigationBarHeight() = Dp(0f)

private fun MBErrorPageType.iconResId(): DrawableResource {
    return when (this) {
        MBErrorPageType.NETWORK -> Res.drawable.compose_multiplatform
        MBErrorPageType.SERVER -> Res.drawable.compose_multiplatform
    }
}

private fun MBErrorPageType.errorLabelResId(): StringResource {
    return when (this) {
        MBErrorPageType.NETWORK -> Res.string.core_ui_net_un_connect
        MBErrorPageType.SERVER -> Res.string.core_ui_server_error
    }
}

private fun MBErrorPageType.errorOptionGuidResId(): StringResource {
    return when (this) {
        MBErrorPageType.NETWORK -> Res.string.core_ui_net_un_connect_and_retry
        MBErrorPageType.SERVER -> Res.string.core_ui_server_busy
    }
}
