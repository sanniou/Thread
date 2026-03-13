package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun Badge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(Dimens.corner_radius_small)
) {
    Surface(
        color = containerColor.copy(alpha = 0.14f),
        shape = shape,
        border = BorderStroke(
            width = Dimens.padding_tiny / 8,
            color = containerColor.copy(alpha = 0.32f)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = Dimens.padding_tiny + Dimens.padding_tiny / 2,
                vertical = Dimens.padding_tiny / 4
            )
        )
    }
}
