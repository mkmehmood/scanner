package com.tobaccoscanner.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Extra surface tones from the Material 3 design tokens that the stock color scheme does not expose. */
data class ExtraColors(
    val surfaceLow: Color,
    val surfaceMid: Color,
    val surfaceHigh: Color,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Color(0xFFF1F4F9), Color(0xFFEAEEF5), Color(0xFFE2E7F0))
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A56DB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF855300),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF2A1700),
    tertiary = Color(0xFF146C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EED0),
    onTertiaryContainer = Color(0xFF002109),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF171C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C22),
    surfaceVariant = Color(0xFFEAEEF5),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF1A3F8F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFFFB95C),
    onSecondary = Color(0xFF462A00),
    secondaryContainer = Color(0xFF653E00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF8ED79F),
    onTertiary = Color(0xFF003914),
    tertiaryContainer = Color(0xFF00531F),
    onTertiaryContainer = Color(0xFFC4EED0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFE0E2EB),
    surface = Color(0xFF151A21),
    onSurface = Color(0xFFE0E2EB),
    surfaceVariant = Color(0xFF232A33),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun TobaccoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val extra = if (dark) {
        ExtraColors(Color(0xFF1B2128), Color(0xFF232A33), Color(0xFF2C343E))
    } else {
        ExtraColors(Color(0xFFF1F4F9), Color(0xFFEAEEF5), Color(0xFFE2E7F0))
    }
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = AppShapes,
            content = content,
        )
    }
}
