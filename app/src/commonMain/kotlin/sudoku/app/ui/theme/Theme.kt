package sudoku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF09090B),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF09090B),
    primary = Color(0xFF18181B),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFF4F4F5),
    onPrimaryContainer = Color(0xFF09090B),
    secondary = Color(0xFFF4F4F5),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = Color(0xFF18181B),
    onTertiary = Color(0xFFFAFAFA),
    tertiaryContainer = Color(0xFF18181B),
    onTertiaryContainer = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFFAFAFA),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    primary = Color(0xFFFAFAFA),
    onPrimary = Color(0xFF18181B),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Color(0xFF27272A),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFFAFAFA),
    tertiary = Color(0xFFFAFAFA),
    onTertiary = Color(0xFF18181B),
    tertiaryContainer = Color(0xFFFAFAFA),
    onTertiaryContainer = Color(0xFF18181B),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A),
    error = Color(0xFFF87171),
    onError = Color(0xFF18181B),
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
