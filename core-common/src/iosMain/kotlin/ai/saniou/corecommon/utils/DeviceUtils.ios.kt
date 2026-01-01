package ai.saniou.corecommon.utils

import platform.UIKit.UIDevice

actual object DeviceUtils {
    actual fun getCuid(): String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "ios_cuid"
    actual fun getAndroidId(): String = "ios_id"
    actual fun getModel(): String = UIDevice.currentDevice.model
    actual fun getBrand(): String = "Apple"
}