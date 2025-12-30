package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Badge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(Dimens.corner_radius_small)
) {
    Surface(
        color = containerColor.copy(alpha = 0.1f), // 极淡背景
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = containerColor.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = containerColor, // 使用容器颜色作为文字颜色，保持同色系但对比度更高
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}
