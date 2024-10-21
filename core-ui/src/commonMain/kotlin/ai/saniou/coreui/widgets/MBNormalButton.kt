package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.body2Bold
import ai.saniou.coreui.theme.risWhite
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.compose_multiplatform

@Composable
fun MBNormalButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean = true,
    clickable: Boolean = enabled,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
//            .border(
//                0.dp,
//                risAccentPrimary().copy(alpha = if (enabled) 1f else 0.3f),
//                RoundedCornerShape(10.dp)
//            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (clickable) {
                    onClick()
                }
            }
    ) {
        val bg = if (isPressed && enabled) {
            painterResource(Res.drawable.compose_multiplatform)
        } else {
            painterResource(Res.drawable.compose_multiplatform)
        }
        Image(
            contentScale = ContentScale.FillBounds,
            painter = bg,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            alpha = if (enabled) 1f else 0.3f
        )
        Text(
            modifier = Modifier
                .align(Alignment.Center),
            text = text,
            style = body2Bold,
            textAlign = TextAlign.Center,
            color = risWhite()
        )
    }
}

@Preview
@Composable
fun previewMBNormalButton() {
    MBNormalButton(
        modifier = Modifier
            .height(40.dp)
            .width(120.dp),
        text = "dadda",
    ) {
    }
}
