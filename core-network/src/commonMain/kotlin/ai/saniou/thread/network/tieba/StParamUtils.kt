package ai.saniou.thread.network.tieba

import kotlin.math.roundToInt
import kotlin.random.Random

object StParamUtils {

    fun generate(isPostMethod: Boolean = true): Map<String, String> {
        val num = Random.nextInt(100, 850)
        
        var stErrorNums = "0"
        var stMethod: String? = null
        var stMode: String? = null
        var stTimesNum: String? = null
        var stTime: String? = null
        var stSize: String? = null

        if (num !in 100..120) {
            stErrorNums = "1"
            stMethod = if (isPostMethod) "2" else "1"
            stMode = "1"
            stTimesNum = "1"
            stTime = num.toString()
            stSize = ((Random.nextDouble() * 8 + 0.4) * num).roundToInt().toString()
        }

        val params = mutableMapOf<String, String>()
        params["stErrorNums"] = stErrorNums
        stMethod?.let { params["stMethod"] = it }
        stMode?.let { params["stMode"] = it }
        stTimesNum?.let { params["stTimesNum"] = it }
        stTime?.let { params["stTime"] = it }
        stSize?.let { params["stSize"] = it }
        
        return params
    }
}