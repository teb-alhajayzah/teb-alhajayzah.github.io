package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = MintSecondary,
    onSecondary = Color.White,
    secondaryContainer = MintContainer,
    onSecondaryContainer = OnMintContainer,
    background = BackgroundMist,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoftGray,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    error = SlotBookedBorder,
    onError = Color.White,
    errorContainer = SlotBookedBg,
    onErrorContainer = SlotBookedText,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = Color(0xFF003639),
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = Color(0xFFB5F4F6),
    secondary = Color(0xFF6DD5C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF144E47),
    onSecondaryContainer = Color(0xFFC0F4EC),
    background = DarkBackground,
    onBackground = Color(0xFFE4F0F2),
    surface = DarkSurface,
    onSurface = Color(0xFFE4F0F2),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA8C2C7),
    outline = Color(0xFF335056),
    error = Color(0xFFF87171),
    onError = Color(0xFF5F0B0B),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TebAlHajayzahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
