package ai.saniou.thread.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class InterfaceDensity { COMPACT, COMFORTABLE, SPACIOUS }

@Serializable
enum class MotionMode { SYSTEM, REDUCED }

@Serializable
data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val density: InterfaceDensity = InterfaceDensity.COMFORTABLE,
    val fontScale: Float = 1f,
    val motionMode: MotionMode = MotionMode.SYSTEM,
    val readerLineHeight: Float = 1.65f,
    val readerWidthDp: Int = 760,
) {
    init {
        require(fontScale in 0.85f..1.4f)
        require(readerLineHeight in 1.2f..2.2f)
        require(readerWidthDp in 520..1_080)
    }

    companion object {
        const val SETTINGS_KEY = "appearance_preferences_v1"
    }
}
