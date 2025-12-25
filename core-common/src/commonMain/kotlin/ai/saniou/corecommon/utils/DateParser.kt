package ai.saniou.corecommon.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.time.Instant

object DateParser {
    /**
     * 从 NMB 的 now 字段解析日期。
     * 常见格式：
     * - 2023-12-24(日)00:15:22
     * - 2023-12-24 00:15:22
     *
     * 目前简单取前 10 位作为日期字符串进行解析。
     */
    fun parseDateFromNowString(now: String): LocalDate? {
        if (now.length < 10) return null
        return try {
            val datePart = now.take(10)
            LocalDate.parse(datePart)
        } catch (e: Exception) {
            null
        }
    }
}

fun String.toTime(): Instant {
    var input = this.trim()

    // 1. Try parsing as standard ISO-8601 Instant (e.g., 2023-12-24T00:15:22Z)
    try {
        return Instant.parse(input)
    } catch (e: Exception) {
        // Continue to other strategies
    }

    // 2. Pre-processing: Remove parentheses content (e.g., (日), (Mon)) common in forum timestamps
    input = input.replace(Regex("\\(.*?\\)"), "")

    // 3. Handle Date Only format (e.g., 2025-12-22)
    if (input.length == 10 && !input.contains(":")) {
        return try {
            LocalDate.parse(input).atStartOfDayIn(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            // If strictly 10 chars but not a valid date, fall through or error
            throw IllegalArgumentException("Failed to parse date: $input", e)
        }
    }

    // 4. Handle DateTime formats
    // Normalize separators to 'T' for LocalDateTime parsing
    val isoString = when {
        // Case: Already has T, but maybe needs cleanup (handled by LocalDateTime.parse or subsequent cleanup)
        input.contains("T") -> input
        // Case: Space separator (2023-12-24 00:15:22)
        input.contains(" ") -> input.replace(" ", "T")
        // Case: Compact format without separator (2023-12-2400:15:22, resultant from stripping parens)
        // Check if it looks like yyyy-MM-ddHH:mm:ss (length >= 18)
        input.length >= 18 -> input.take(10) + "T" + input.substring(10)
        else -> input
    }

    // Clean up potential double T's
    val finalIsoString = isoString.replace(Regex("T{2,}"), "T")

    return try {
        LocalDateTime.parse(finalIsoString).toInstant(TimeZone.currentSystemDefault())
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse time string: $this", e)
    }
}
