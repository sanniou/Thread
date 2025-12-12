package ai.saniou.forum.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun PoTag(isPo: Boolean) {
    if (isPo) {
        val holidayEmoji = remember { getHolidayEmoji() }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text(
                text = holidayEmoji ?: "PO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
