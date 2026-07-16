/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.saniou.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Immutable
data class ThreadSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val readerSurface: Color,
    val interactiveSurface: Color,
)

private val LightSemanticColors = ThreadSemanticColors(
    success = Color(0xFF13795B),
    onSuccess = Color.White,
    warning = Color(0xFF9A6700),
    onWarning = Color.White,
    readerSurface = Color(0xFFFFFEFA),
    interactiveSurface = Color(0xFFF0F1FA),
)

private val DarkSemanticColors = ThreadSemanticColors(
    success = Color(0xFF67D5AE),
    onSuccess = Color(0xFF052E22),
    warning = Color(0xFFFFCB6B),
    onWarning = Color(0xFF3E2B00),
    readerSurface = Color(0xFF181A20),
    interactiveSurface = Color(0xFF252833),
)

private val LocalThreadSemanticColors = staticCompositionLocalOf { LightSemanticColors }

enum class ThreadInterfaceDensity { COMPACT, COMFORTABLE, SPACIOUS }

@Immutable
data class ThreadUiPreferences(
    val interfaceDensity: ThreadInterfaceDensity = ThreadInterfaceDensity.COMFORTABLE,
    val reducedMotion: Boolean = false,
    val readerWidth: Dp = 760.dp,
    val readerLineHeightMultiplier: Float = 1.65f,
) {
    val sectionSpacing: Dp
        get() = when (interfaceDensity) {
            ThreadInterfaceDensity.COMPACT -> 12.dp
            ThreadInterfaceDensity.COMFORTABLE -> 20.dp
            ThreadInterfaceDensity.SPACIOUS -> 28.dp
        }
    val itemSpacing: Dp
        get() = when (interfaceDensity) {
            ThreadInterfaceDensity.COMPACT -> 6.dp
            ThreadInterfaceDensity.COMFORTABLE -> 10.dp
            ThreadInterfaceDensity.SPACIOUS -> 16.dp
        }
}

val LocalThreadUiPreferences = staticCompositionLocalOf { ThreadUiPreferences() }

val MaterialTheme.threadColors: ThreadSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalThreadSemanticColors.current

@Composable
fun ThreadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    interfaceDensity: ThreadInterfaceDensity = ThreadInterfaceDensity.COMFORTABLE,
    fontScale: Float = 1f,
    reducedMotion: Boolean = false,
    readerWidth: Dp = 760.dp,
    readerLineHeightMultiplier: Float = 1.65f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    val currentDensity = LocalDensity.current
    val uiPreferences = ThreadUiPreferences(
        interfaceDensity = interfaceDensity,
        reducedMotion = reducedMotion,
        readerWidth = readerWidth,
        readerLineHeightMultiplier = readerLineHeightMultiplier,
    )
    CompositionLocalProvider(
        LocalThreadSemanticColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors,
        LocalThreadUiPreferences provides uiPreferences,
        LocalDensity provides Density(currentDensity.density, currentDensity.fontScale * fontScale.coerceIn(0.85f, 1.4f)),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            typography = Typography,
            content = content
        )
    }
}
