package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BoardColors(
    val background: Color,
    val thinLine: Color,
    val thickLine: Color,
    val given: Color,
    val entered: Color,
    val error: Color,
    val notes: Color,
    val selected: Color,
    val peer: Color,
    val sameDigit: Color,
    val conflict: Color,
    val regions: List<Color>,
    val catInk: Color,
    val xMark: Color,
    val chartSeries: Color,
    val chartGrid: Color,
)

private val LightBoard = BoardColors(
    background = Color(0xFFFFFFFF),
    thinLine = Color(0xFFD9D3C7),
    thickLine = Color(0xFF3B3A36),
    given = Color(0xFF23221F),
    entered = Color(0xFF3550C8),
    error = Color(0xFFC8372D),
    notes = Color(0xFF6E6B63),
    selected = Color(0xFFC6D3FF),
    peer = Color(0xFFF2EFE8),
    sameDigit = Color(0xFFDDE4FF),
    conflict = Color(0xFFF9DCD9),
    regions = listOf(
        0xFFF4A6A6, 0xFFF7C59F, 0xFFF3DF7E, 0xFFB5DDA4, 0xFF97D5C8,
        0xFFA7C7F2, 0xFFC3B1F0, 0xFFF2B3D9, 0xFFD9C2A7, 0xFFC4CCD6,
    ).map(::Color),
    catInk = Color(0xFF2B2A27),
    xMark = Color(0x8C2B2A27),
    chartSeries = Color(0xFF3550C8),
    chartGrid = Color(0xFFE6E1D7),
)

private val DarkBoard = BoardColors(
    background = Color(0xFF1E2128),
    thinLine = Color(0xFF343844),
    thickLine = Color(0xFF9AA0AD),
    given = Color(0xFFE8E9EC),
    entered = Color(0xFF8EA6FF),
    error = Color(0xFFFF6B60),
    notes = Color(0xFF9AA0AD),
    selected = Color(0xFF34436F),
    peer = Color(0xFF252933),
    sameDigit = Color(0xFF2C365A),
    conflict = Color(0xFF4A2523),
    regions = listOf(
        0xFF8A4A4A, 0xFF8C5E3C, 0xFF857735, 0xFF4E7A44, 0xFF3E7A6E,
        0xFF47658F, 0xFF64548F, 0xFF8A4C72, 0xFF75624C, 0xFF56606B,
    ).map(::Color),
    catInk = Color(0xFFF2F2F2),
    xMark = Color(0x8CF2F2F2),
    chartSeries = Color(0xFF6F86F0),
    chartGrid = Color(0xFF2C303A),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

@Composable
fun SudokuTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF8EA6FF),
            onPrimary = Color(0xFF0E1A4A),
            primaryContainer = Color(0xFF2C365A),
            onPrimaryContainer = Color(0xFFDDE4FF),
            background = Color(0xFF16181D),
            onBackground = Color(0xFFE8E9EC),
            surface = Color(0xFF1E2128),
            onSurface = Color(0xFFE8E9EC),
            surfaceVariant = Color(0xFF252933),
            onSurfaceVariant = Color(0xFF9AA0AD),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF3550C8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDE4FF),
            onPrimaryContainer = Color(0xFF14256E),
            background = Color(0xFFFBF8F3),
            onBackground = Color(0xFF23221F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF23221F),
            surfaceVariant = Color(0xFFF2EFE8),
            onSurfaceVariant = Color(0xFF6E6B63),
        )
    }
    CompositionLocalProvider(LocalBoardColors provides if (dark) DarkBoard else LightBoard) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
