package ai.saniou.coreui.theme

import androidx.compose.ui.unit.dp

object Dimens {
    /**
     * The width at which the reader page switches to mobile layout.
     */
    val MobileWidth = 600.dp

    // Padding (Semantic)
    val padding_tiny = 4.dp
    val padding_small = 8.dp
    val padding_medium = 12.dp
    val padding_standard = 16.dp
    val padding_large = 24.dp
    val padding_extra_large = 32.dp

    // Corner Radius (Semantic)
    val corner_radius_small = 4.dp
    val corner_radius_medium = 8.dp
    val corner_radius_large = 12.dp
    val corner_radius_extra_large = 16.dp

    // Icon Size
    val icon_size_small = 16.dp
    val icon_size_medium = 20.dp
    val icon_size_standard = 24.dp
    val icon_size_large = 32.dp

    // Avatar Size
    val avatar_size_small = 24.dp
    val avatar_size_medium = 40.dp
    val avatar_size_large = 56.dp

    // Image Height
    val image_height_medium = 240.dp

    // Component Specific
    val drawer_header_height = 140.dp

    // Legacy Aliases (Deprecated)
    @Deprecated("Use padding_tiny")
    val padding_extra_small = padding_tiny
}
