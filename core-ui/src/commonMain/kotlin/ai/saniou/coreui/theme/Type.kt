package ai.saniou.coreui.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// Material 3 Typography
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Legacy Compatibility Aliases (Deprecated)
@Deprecated("Use Typography.displaySmall or custom style")
val headline3 = Typography.displaySmall.copy(fontSize = 40.sp, fontStyle = FontStyle.Italic)

@Deprecated("Use Typography.displaySmall")
val headline3Sans = Typography.displaySmall.copy(fontSize = 40.sp)

@Deprecated("Use Typography.headlineLarge")
val headline4 = Typography.headlineLarge.copy(fontSize = 30.sp, fontStyle = FontStyle.Italic)

@Deprecated("Use Typography.headlineLarge")
val headline4Sans = Typography.headlineLarge.copy(fontSize = 30.sp)

@Deprecated("Use Typography.headlineMedium")
val headline5 = Typography.headlineMedium.copy(fontSize = 26.sp, fontStyle = FontStyle.Italic)

@Deprecated("Use Typography.headlineMedium")
val headline5Sans = Typography.headlineMedium.copy(fontSize = 26.sp)

@Deprecated("Use Typography.titleLarge")
val headline6Sans = Typography.titleLarge

@Deprecated("Use Typography.titleMedium")
val subtitle1 = Typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal)

@Deprecated("Use Typography.titleMedium")
val subtitle1Bold = Typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.titleMedium")
val subtitle2 = Typography.titleMedium.copy(fontWeight = FontWeight.Normal)

@Deprecated("Use Typography.titleMedium")
val subtitle2Bold = Typography.titleMedium

@Deprecated("Use Typography.titleMedium with underline")
val subtitle2BoldUnderline = Typography.titleMedium.copy(textDecoration = TextDecoration.Underline)

@Deprecated("Use Typography.bodyLarge")
val body1 = Typography.bodyLarge

@Deprecated("Use Typography.bodyLarge with underline")
val body1UnderLine = Typography.bodyLarge.copy(textDecoration = TextDecoration.Underline)

@Deprecated("Use Typography.bodyLarge with bold")
val body1Bold = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.bodyMedium")
val body2 = Typography.bodyMedium

@Deprecated("Use Typography.bodyMedium with semi-bold")
val body2SemiBold = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

@Deprecated("Use Typography.bodyMedium with underline")
val body2Underline = Typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)

@Deprecated("Use Typography.bodyMedium with bold")
val body2Bold = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.bodyMedium with bold and underline")
val body2BoldUnderline = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)

@Deprecated("Use Typography.bodySmall")
val body3 = Typography.bodySmall.copy(fontSize = 13.sp)

@Deprecated("Use Typography.bodySmall")
val caption = Typography.bodySmall

@Deprecated("Use Typography.bodySmall with bold")
val captionBold = Typography.bodySmall.copy(fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.bodySmall with underline")
val captionUnderline = Typography.bodySmall.copy(textDecoration = TextDecoration.Underline)

@Deprecated("Use Typography.labelLarge")
val overLine = Typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 1.sp)

@Deprecated("Use Typography.labelLarge")
val button1 = Typography.labelLarge.copy(fontSize = 18.sp, letterSpacing = 0.5.sp)

@Deprecated("Use Typography.labelLarge with bold")
val button1Bold = Typography.labelLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.labelLarge")
val button2 = Typography.labelLarge

@Deprecated("Use Typography.labelLarge with bold")
val button2Bold = Typography.labelLarge.copy(fontWeight = FontWeight.Bold)

@Deprecated("Use Typography.labelSmall")
val footer = Typography.labelSmall.copy(fontSize = 10.sp)
