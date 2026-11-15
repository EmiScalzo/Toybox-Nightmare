package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ToyboxDarkColorScheme = darkColorScheme(
    primary = TarnishedBrass,
    onPrimary = CharcoalBlack,
    primaryContainer = AgedWoodMid,
    onPrimaryContainer = AgedIvory,
    secondary = CrimsonBlood,
    onSecondary = AgedIvory,
    secondaryContainer = AgedWoodLight,
    onSecondaryContainer = Parchment,
    tertiary = MutedMoss,
    onTertiary = CottonWhite,
    background = CharcoalBlack,
    onBackground = AgedIvory,
    surface = AgedWoodDark,
    onSurface = AgedIvory,
    surfaceVariant = AgedWoodMid,
    onSurfaceVariant = Parchment,
    outline = TarnishedBrassDim,
    error = CrimsonBright,
    onError = CottonWhite
)

private val ToyboxShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = CutCornerShape(4.dp),
    medium = CutCornerShape(6.dp),
    large = CutCornerShape(8.dp),
    extraLarge = CutCornerShape(10.dp)
)

@Composable
fun ToyboxNightmareTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ToyboxDarkColorScheme,
        typography = ToyboxTypography,
        shapes = ToyboxShapes,
        content = content
    )
}
