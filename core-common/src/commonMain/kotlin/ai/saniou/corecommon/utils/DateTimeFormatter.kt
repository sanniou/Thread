package ai.saniou.corecommon.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * 将 Instant 格式化为对用户友好的相对时间字符串。
 * 例如："刚刚", "5 分钟前", "3 小时前", "昨天", 或 "2023-10-27"。
 *
 * @param timeZone 用于转换日期以显示绝对日期的时区。
 * @return 格式化后的字符串。
 */
@OptIn(ExperimentalTime::class)
fun Instant.toRelativeTimeString(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val now = Clock.System.now()
    val duration = now - this

    return when {
        duration < 1.minutes -> "刚刚"
        duration < 1.hours -> "${duration.inWholeMinutes} 分钟前"
        duration < 24.hours -> "${duration.inWholeHours} 小时前"
        duration < 2.days -> {
            val localThis = this.toLocalDateTime(timeZone)
            val localNow = now.toLocalDateTime(timeZone)
            if (localThis.dayOfMonth == localNow.dayOfMonth - 1) "昨天" else "${duration.inWholeDays} 天前"
        }
        duration < 30.days -> "${duration.inWholeDays} 天前"
        else -> {
            val localDate = this.toLocalDateTime(timeZone)
            "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
        }
    }
}
