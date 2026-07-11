package com.deskora.setup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DeskoraLightColors = lightColorScheme(
    primary = DeskoraColors.BlueprintNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBD9E4),
    onPrimaryContainer = DeskoraColors.DeepNavy,
    secondary = DeskoraColors.DeskOak,
    onSecondary = Color.White,
    secondaryContainer = DeskoraColors.LightDeskWood,
    onSecondaryContainer = Color(0xFF3E2E1C),
    tertiary = DeskoraColors.ZoneBorder,
    onTertiary = Color.White,
    background = DeskoraColors.AppBackground,
    onBackground = DeskoraColors.DeepText,
    surface = DeskoraColors.Surface,
    onSurface = DeskoraColors.DeepText,
    surfaceVariant = Color(0xFFE4E9EC),
    onSurfaceVariant = DeskoraColors.SecondaryText,
    outline = DeskoraColors.Divider,
    error = DeskoraColors.ErrorRed,
    onError = Color.White
)

private val DeskoraTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

/**
 * Deskora uses a single, calm light theme regardless of the system dark setting,
 * so the blueprint surface renders consistently. The [darkTheme] parameter is
 * accepted for API completeness but the palette is intentionally stable.
 */
@Composable
fun DeskoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeskoraLightColors,
        typography = DeskoraTypography,
        content = content
    )
}
