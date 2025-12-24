package ai.saniou.corecommon.utils

import kotlinx.datetime.LocalDate

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