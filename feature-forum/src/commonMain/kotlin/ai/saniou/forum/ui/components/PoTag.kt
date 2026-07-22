package ai.saniou.forum.ui.components

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
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun PoTag(isPo: Boolean) {
    if (!isPo) return
    val holidayEmoji = remember { getHolidayEmoji() }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = holidayEmoji ?: "PO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun getHolidayEmoji(): String? {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return when {
        now.month.number == 12 && now.day == 25 -> "🎄"
        now.month.number == 1 && now.day == 1 -> "🎉"
        now.month.number == 10 && now.day == 1 -> "🇨🇳"
        else -> null
    }
}
