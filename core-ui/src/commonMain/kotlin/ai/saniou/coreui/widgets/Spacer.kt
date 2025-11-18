package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VerticalSpacerSmall() {
    Spacer(modifier = Modifier.height(Dimens.padding_small))
}