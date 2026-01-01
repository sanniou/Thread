package ai.saniou.corecommon.utils

expect object DeviceUtils {
    fun getCuid(): String
    fun getAndroidId(): String
    fun getModel(): String
    fun getBrand(): String
}