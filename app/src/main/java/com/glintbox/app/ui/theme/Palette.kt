package com.glintbox.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.glintbox.app.R
import com.glintbox.app.data.model.AccentPalette

@Immutable
data class PaletteSpec(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    /** Signature gradient used for hero text, buttons and the aurora background. */
    val gradient: List<Color>,
    @param:StringRes val nameRes: Int,
)

fun AccentPalette.spec(): PaletteSpec = when (this) {
    AccentPalette.AURORA -> PaletteSpec(
        primary = Color(0xFF9B6BFF),
        secondary = Color(0xFF22D3EE),
        tertiary = Color(0xFFF472B6),
        gradient = listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFF06B6D4)),
        nameRes = R.string.palette_aurora,
    )
    AccentPalette.EMBER -> PaletteSpec(
        primary = Color(0xFFFF7A59),
        secondary = Color(0xFFFFB547),
        tertiary = Color(0xFFFF3D7F),
        gradient = listOf(Color(0xFFFF512F), Color(0xFFF09819), Color(0xFFDD2476)),
        nameRes = R.string.palette_ember,
    )
    AccentPalette.LAGOON -> PaletteSpec(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF34D399),
        tertiary = Color(0xFF818CF8),
        gradient = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1), Color(0xFF10B981)),
        nameRes = R.string.palette_lagoon,
    )
    AccentPalette.ORCHID -> PaletteSpec(
        primary = Color(0xFFE879F9),
        secondary = Color(0xFFF9A8D4),
        tertiary = Color(0xFFA78BFA),
        gradient = listOf(Color(0xFFC026D3), Color(0xFFDB2777), Color(0xFF7C3AED)),
        nameRes = R.string.palette_orchid,
    )
    AccentPalette.JADE -> PaletteSpec(
        primary = Color(0xFF34D399),
        secondary = Color(0xFFA3E635),
        tertiary = Color(0xFF2DD4BF),
        gradient = listOf(Color(0xFF059669), Color(0xFF14B8A6), Color(0xFF84CC16)),
        nameRes = R.string.palette_jade,
    )
    AccentPalette.MIDAS -> PaletteSpec(
        primary = Color(0xFFFBBF24),
        secondary = Color(0xFFF97316),
        tertiary = Color(0xFFFDE68A),
        gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEAB308)),
        nameRes = R.string.palette_midas,
    )
}
