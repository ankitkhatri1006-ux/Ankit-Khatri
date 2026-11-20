package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val YodhaDarkColorScheme = darkColorScheme(
    primary = SaffronGold,
    onPrimary = ObsidianBg,
    primaryContainer = ColorTokens.PrimaryContainer,
    onPrimaryContainer = SaffronBright,
    secondary = BharatTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = ColorTokens.SecondaryContainer,
    onSecondaryContainer = BharatTeal,
    tertiary = RoyalCrown,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = TacticalSlate,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedArmor,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = CrimsonStrike,
    onError = TextPrimary
)

private object ColorTokens {
    val PrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3A2506)
    val SecondaryContainer = androidx.compose.ui.graphics.Color(0xFF062C28)
}

val YodhaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YodhaRoyaleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YodhaDarkColorScheme,
        typography = Typography,
        shapes = YodhaShapes,
        content = content
    )
}
