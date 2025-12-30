package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun PoTag(isPo: Boolean) {
    if (isPo) {
        val holidayEmoji = remember { getHolidayEmoji() }
        Surface(
            shape = RoundedCornerShape(Dimens.corner_radius_small),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Text(
                text = holidayEmoji ?: "PO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun getHolidayEmoji(): String? {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return when {
        now.monthNumber == 12 && now.dayOfMonth == 25 -> "🎄"
        now.monthNumber == 1 && now.dayOfMonth == 1 -> "🎉"
        now.monthNumber == 10 && now.dayOfMonth == 1 -> "🇨🇳"
        else -> null
    }
}
