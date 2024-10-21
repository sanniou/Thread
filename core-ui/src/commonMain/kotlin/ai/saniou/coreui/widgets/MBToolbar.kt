package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.md_theme_light_onTertiary
import ai.saniou.coreui.theme.md_theme_light_scrim
import ai.saniou.coreui.theme.subtitle2Bold
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.compose_multiplatform

val TOOLBAR_HEIGHT = 56.dp

@Composable
fun BaseMBToolbar(
    startContent: @Composable BoxScope.() -> Unit = {},
    centerContent: @Composable BoxScope.() -> Unit = {},
    endContent: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        Modifier
            .background(color = md_theme_light_scrim)
            .statusBarsPadding()
            .padding(11.dp, 0.dp, 16.dp, 0.dp)
            .fillMaxWidth()
            .height(TOOLBAR_HEIGHT)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
        ) {
            startContent(this)
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
        ) {
            centerContent(this)
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            endContent(this)
        }
    }
}

@Composable
fun MBToolbar(
    title: String,
    leftIconId: DrawableResource? = Res.drawable.compose_multiplatform,
    rightIconId: DrawableResource? = null,
    onLeftIconClick: () -> Unit = {},
    onRightIconClick: () -> Unit = {}
) {
    BaseMBToolbar({
        if (leftIconId != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable {
                        onLeftIconClick()
                    },
                painter = painterResource(leftIconId),
                contentDescription = null,
            )
        }
    }, {
        Text(
            modifier = Modifier
                .padding(start = 35.dp, end = 35.dp)
                .align(Alignment.Center),
            text = title,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = subtitle2Bold,
            color = md_theme_light_onTertiary
        )
    }, {
        if (rightIconId != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable {
                        onRightIconClick()
                    },
                painter = painterResource(rightIconId),
                contentDescription = null,
            )
        }
    })
}

@Composable
@Preview
fun ToolbarPreview() {
    MBToolbar("dududdu")
}
