package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.body2
import ai.saniou.coreui.theme.md_theme_light_onTertiary
import ai.saniou.coreui.theme.md_theme_light_scrim
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.compose_multiplatform
import thread.core_ui.generated.resources.core_ui_list_loading_wording
import thread.core_ui.generated.resources.core_ui_loading_wording

@Composable
fun MBCircleIndicator(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        )
    )
    Box(
        modifier = modifier
            .rotate(angle)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null
        )
    }
}

@Composable
fun MBPageLoadingIndicator(
    modifier: Modifier = Modifier,
    messageId: StringResource = Res.string.core_ui_loading_wording
) {
    Column(modifier) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(messageId),
            style = body2,
            color = md_theme_light_scrim.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.size(12.dp))
        MBCircleIndicator(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun MBPageLoadingWithBgIndicator(
    modifier: Modifier = Modifier,
    messageId: StringResource = Res.string.core_ui_loading_wording
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(md_theme_light_scrim.copy(alpha = 0.8f))
            .size(124.dp)
    ) {
        Column(
            modifier.align(Alignment.Center)
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(messageId),
                style = body2,
                color = md_theme_light_onTertiary
            )
            Spacer(modifier = Modifier.size(12.dp))
            MBCircleIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ColumnLoadingPlaceholder(
    modifier: Modifier = Modifier,
    wording: String = stringResource(Res.string.core_ui_list_loading_wording)
) {
    Row(modifier) {

        MBCircleIndicator(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.size(8.dp))

        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = wording,
            style = body2,
            color = md_theme_light_scrim.copy(alpha = 0.6f)
        )
    }
}

@Preview
@Composable
fun PreviewMBCircleIndicator() {
    ColumnLoadingPlaceholder()
}
