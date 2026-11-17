package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LahoriLightColorScheme = lightColorScheme(
    primary = LahoriRed,
    onPrimary = LahoriWhite,
    primaryContainer = LahoriRedSoft,
    onPrimaryContainer = LahoriRedDark,
    secondary = LahoriBlack,
    onSecondary = LahoriWhite,
    secondaryContainer = LahoriOffWhite,
    onSecondaryContainer = LahoriBlack,
    tertiary = LahoriGold,
    onTertiary = LahoriBlack,
    background = LahoriBackground,
    onBackground = LahoriBlack,
    surface = LahoriWhite,
    onSurface = LahoriBlack,
    surfaceVariant = LahoriOffWhite,
    onSurfaceVariant = LahoriMutedText,
    surfaceContainerHigh = LahoriPillGrey,
    outline = LahoriBorder
)

private val LahoriDarkColorScheme = darkColorScheme(
    primary = LahoriRed,
    onPrimary = LahoriWhite,
    primaryContainer = LahoriDarkRedSoft,
    onPrimaryContainer = LahoriDarkRedSoftText,
    secondary = LahoriDarkSurfaceVariant,
    onSecondary = LahoriDarkTextPrimary,
    secondaryContainer = LahoriDarkSurfaceVariant,
    onSecondaryContainer = LahoriDarkTextPrimary,
    tertiary = LahoriGold,
    onTertiary = LahoriBlack,
    background = LahoriDarkBackground,
    onBackground = LahoriDarkTextPrimary,
    surface = LahoriDarkSurface,
    onSurface = LahoriDarkTextPrimary,
    surfaceVariant = LahoriDarkSurfaceVariant,
    onSurfaceVariant = LahoriDarkTextMuted,
    surfaceContainerHigh = LahoriDarkSurfaceVariant,
    outline = LahoriDarkBorder
)

private val LahoriShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun LahoriCafeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LahoriDarkColorScheme else LahoriLightColorScheme,
        typography = LahoriTypography,
        shapes = LahoriShapes,
        content = content
    )
}
