package ai.saniou.coreui.widgets.palette

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.palette.graphics.Palette

fun PhotoPalette(palette: SimplePalette?, colorScheme: ColorScheme): PhotoPalette {
    return PhotoPalette(
        palette = palette,
        primaryColor = colorScheme.primary.toArgb(),
        primaryContainerColor = colorScheme.primaryContainer.toArgb(),
    )
}

fun PhotoPalette(colorScheme: ColorScheme): PhotoPalette {
    return PhotoPalette(
        palette = null,
        primaryColor = colorScheme.primary.toArgb(),
        primaryContainerColor = colorScheme.primaryContainer.toArgb(),
    )
}


data class PhotoPalette(
    private val palette: SimplePalette?,
    private val primaryColor: Int,
    private val primaryContainerColor: Int,
) {

    val containerColor: Color by lazy {
        val swatch = palette?.run {
            listOfNotNull(
                darkMutedSwatch,
                mutedSwatch,
                lightMutedSwatch,
                darkVibrantSwatch,
                vibrantSwatch,
                lightVibrantSwatch,
            ).firstOrNull()
        }
        Color(swatch?.rgb ?: primaryContainerColor).copy(alpha = 0.6f)
    }

    val containerColorInt: Int by lazy { containerColor.toArgb() }

    val accentColor: Color by lazy {
        val swatch = palette?.run {
            listOfNotNull(
                lightVibrantSwatch,
                vibrantSwatch,
                darkVibrantSwatch,
                lightMutedSwatch,
                mutedSwatch,
                darkMutedSwatch,
            ).firstOrNull()
        }
        Color(swatch?.rgb ?: primaryColor).copy(alpha = 0.6f)
    }

    val accentColorInt: Int by lazy { accentColor.toArgb() }

    val contentColor: Color = Color.White
    val contentColorInt: Int = contentColor.toArgb()
}

data class SimplePalette(
    val dominantSwatch: Palette.Swatch?,
    val darkMutedSwatch: Palette.Swatch?,
    val mutedSwatch: Palette.Swatch?,
    val lightMutedSwatch: Palette.Swatch?,
    val darkVibrantSwatch: Palette.Swatch?,
    val vibrantSwatch: Palette.Swatch?,
    val lightVibrantSwatch: Palette.Swatch?,
)

fun Palette.toSimplePalette(): SimplePalette = SimplePalette(
    dominantSwatch = dominantSwatch,
    darkMutedSwatch = darkMutedSwatch,
    mutedSwatch = mutedSwatch,
    lightMutedSwatch = lightMutedSwatch,
    darkVibrantSwatch = darkVibrantSwatch,
    vibrantSwatch = vibrantSwatch,
    lightVibrantSwatch = lightVibrantSwatch,
)
