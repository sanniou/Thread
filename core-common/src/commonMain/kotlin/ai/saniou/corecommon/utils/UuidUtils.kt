package ai.saniou.corecommon.utils

import kotlin.random.Random

object UuidUtils {
    fun randomUuid(): String {
        val charPool = "0123456789abcdef"
        return (1..36).map { i ->
            when (i) {
                9, 14, 19, 24 -> '-'
                15 -> '4'
                20 -> charPool[Random.nextInt(4) + 8] // 8, 9, a, b
                else -> charPool[Random.nextInt(16)]
            }
        }.joinToString("")
    }
}