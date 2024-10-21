package ai.saniou.coreui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun AwesomeTheme(
    colors: AwesomeColor = AwesomeTheme.colors,
    content: @Composable () -> Unit,
) {
    val rememberedColors = remember { colors.copy() }.apply { updateColors(colors) }
    CompositionLocalProvider(
        LocalColors provides rememberedColors,
    ) {
        content()
    }
}

internal val LocalColors = staticCompositionLocalOf {
    AwesomeColor(
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        Color(0xFF8B0000UL),
        false
    )
}

object AwesomeTheme {
    val colors: AwesomeColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

class AwesomeColor(
    primary: Color,
    onPrimary: Color,
    error: Color,
    onError: Color,
    success: Color,
    onSuccess: Color,
    warning: Color,
    onWarning: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    outline: Color,
    isLight: Boolean,
) {
    var primary by mutableStateOf(primary)
        private set

    var onPrimary by mutableStateOf(onPrimary)
        private set

    var error by mutableStateOf(error)
        private set

    var onError by mutableStateOf(onError)
        private set

    var success by mutableStateOf(success)
        private set

    var onSuccess by mutableStateOf(onSuccess)
        private set

    var warning by mutableStateOf(warning)
        private set

    var onWarning by mutableStateOf(onWarning)
        private set

    var background by mutableStateOf(background)
        private set

    var onBackground by mutableStateOf(onBackground)
        private set

    var surface by mutableStateOf(surface)
        private set

    var onSurface by mutableStateOf(onSurface)
        private set

    var outline by mutableStateOf(outline)
        private set

    var isLight by mutableStateOf(isLight)
        internal set

    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        error: Color = this.error,
        onError: Color = this.onError,
        success: Color = this.success,
        onSuccess: Color = this.onSuccess,
        warning: Color = this.warning,
        onWarning: Color = this.onWarning,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        outline: Color = this.outline,
        isLight: Boolean = this.isLight,
    ): AwesomeColor = AwesomeColor(
        primary,
        onPrimary,
        error,
        onError,
        success,
        onSuccess,
        warning,
        onWarning,
        background,
        onBackground,
        surface,
        onSurface,
        outline,
        isLight,
    )

    fun updateColors(other: AwesomeColor) {
        primary = other.primary
        onPrimary = other.onPrimary
        error = other.error
        onError = other.onError
        success = other.success
        onSuccess = other.onSuccess
        warning = other.warning
        onWarning = other.onWarning
        background = other.background
        onBackground = other.onBackground
        surface = other.surface
        onSurface = other.onSurface
        outline = other.outline
        isLight = other.isLight
    }
}
