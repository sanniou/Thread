package ai.saniou.corecommon.utils

import android.os.Build
import java.util.UUID

actual object DeviceUtils {
    actual fun getCuid(): String {
        return UUID.randomUUID().toString() // Simplified for demo
    }
    actual fun getAndroidId(): String {
        return "android_id_placeholder"
    }
    actual fun getModel(): String = Build.MODEL
    actual fun getBrand(): String = Build.BRAND
}