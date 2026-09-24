package com.glintbox.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.glintbox.app.data.model.AppSettings
import com.glintbox.app.data.model.ThemeMode

@Immutable
data class GlintColors(
    val gradient: List<Color>,
    val isDark: Boolean,
    val glass: Color,
    val glassBorder: Color,
    val scrim: Color,
)

val LocalGlint = staticCompositionLocalOf {
    GlintColors(
        gradient = listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFF06B6D4)),
        isDark = true,
        glass = Color(0x33FFFFFF),
        glassBorder = Color(0x22FFFFFF),
        scrim = Color(0x99000000),
    )
}

private val GlintShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private fun onColorFor(c: Color): Color = if (c.luminance() > 0.45f) Color(0xFF14111C) else Color.White

private fun darkScheme(p: PaletteSpec, amoled: Boolean): ColorScheme {
    val bg = if (amoled) Color.Black else Color(0xFF0C0B12)
    val surface = if (amoled) Color(0xFF07070A) else Color(0xFF13121B)
    return darkColorScheme(
        primary = p.primary,
        onPrimary = onColorFor(p.primary),
        primaryContainer = lerp(surface, p.primary, 0.28f),
        onPrimaryContainer = Color.White,
        secondary = p.secondary,
        onSecondary = onColorFor(p.secondary),
        secondaryContainer = lerp(surface, p.secondary, 0.22f),
        onSecondaryContainer = Color.White,
        tertiary = p.tertiary,
        onTertiary = onColorFor(p.tertiary),
        tertiaryContainer = lerp(surface, p.tertiary, 0.22f),
        onTertiaryContainer = Color.White,
        background = bg,
        onBackground = Color(0xFFEDEBF5),
        surface = surface,
        onSurface = Color(0xFFEDEBF5),
        surfaceVariant = Color(0xFF211E2C),
        onSurfaceVariant = Color(0xFFABA6BD),
        surfaceContainerLowest = bg,
        surfaceContainerLow = lerp(bg, Color(0xFF1B1925), 0.6f),
        surfaceContainer = Color(0xFF1B1925),
        surfaceContainerHigh = Color(0xFF23202F),
        surfaceContainerHighest = Color(0xFF2B2838),
        outline = Color(0xFF4A4659),
        outlineVariant = Color(0xFF302C3D),
        error = Color(0xFFFF6B7A),
        onError = Color(0xFF2B0006),
        inverseSurface = Color(0xFFEDEBF5),
        inverseOnSurface = Color(0xFF14121C),
        inversePrimary = lerp(p.primary, Color.Black, 0.3f),
        scrim = Color.Black,
    )
}

private fun lightScheme(p: PaletteSpec): ColorScheme {
    val primary = lerp(p.primary, Color.Black, 0.18f)
    val bg = Color(0xFFF7F5FC)
    return lightColorScheme(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = lerp(Color.White, p.primary, 0.2f),
        onPrimaryContainer = lerp(p.primary, Color.Black, 0.6f),
        secondary = lerp(p.secondary, Color.Black, 0.3f),
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, p.secondary, 0.2f),
        onSecondaryContainer = lerp(p.secondary, Color.Black, 0.6f),
        tertiary = lerp(p.tertiary, Color.Black, 0.25f),
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, p.tertiary, 0.2f),
        onTertiaryContainer = lerp(p.tertiary, Color.Black, 0.6f),
        background = bg,
        onBackground = Color(0xFF17141F),
        surface = Color.White,
        onSurface = Color(0xFF17141F),
        surfaceVariant = Color(0xFFEEEAF6),
        onSurfaceVariant = Color(0xFF5E596E),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFBFAFE),
        surfaceContainer = Color(0xFFF3F0FA),
        surfaceContainerHigh = Color(0xFFEDE9F6),
        surfaceContainerHighest = Color(0xFFE6E1F2),
        outline = Color(0xFFCBC5DB),
        outlineVariant = Color(0xFFE2DDEE),
        error = Color(0xFFD92D43),
        onError = Color.White,
        inverseSurface = Color(0xFF221F2B),
        inverseOnSurface = Color(0xFFF4F1FA),
        inversePrimary = p.primary,
        scrim = Color.Black,
    )
}

@Composable
fun isAppInDarkTheme(settings: AppSettings?): Boolean = when (settings?.themeMode ?: ThemeMode.SYSTEM) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GlintboxTheme(settings: AppSettings?, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(settings)
    val palette = (settings?.palette ?: com.glintbox.app.data.model.AccentPalette.AURORA).spec()
    val amoled = settings?.amoled == true
    val dynamic = settings?.dynamicColor == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamic && dark -> dynamicDarkColorScheme(context).let {
            if (amoled) it.copy(background = Color.Black, surface = Color(0xFF07070A)) else it
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamic -> dynamicLightColorScheme(context)
        dark -> darkScheme(palette, amoled)
        else -> lightScheme(palette)
    }

    val glint = remember(scheme, dark, dynamic, palette) {
        GlintColors(
            gradient = if (dynamic) listOf(scheme.primary, scheme.tertiary, scheme.secondary) else palette.gradient,
            isDark = dark,
            glass = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f),
            glassBorder = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
            scrim = Color.Black.copy(alpha = 0.55f),
        )
    }

    CompositionLocalProvider(LocalGlint provides glint) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GlintTypography,
            shapes = GlintShapes,
            content = content,
        )
    }
}
