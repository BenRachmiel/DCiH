package sudoku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        surface = Color(0xFFFDFBF7),
        onSurface = Color(0xFF1C1917),
        background = Color(0xFFFDFBF7),
        onBackground = Color(0xFF1C1917),
        primary = Color(0xFF292524),
        onPrimary = Color(0xFFFAF9F7),
        primaryContainer = Color(0xFFF5F0EB),
        onPrimaryContainer = Color(0xFF1C1917),
        secondary = Color(0xFFF0EBE5),
        onSecondary = Color(0xFF292524),
        secondaryContainer = Color(0xFFF0EBE5),
        onSecondaryContainer = Color(0xFF292524),
        tertiary = Color(0xFF292524),
        onTertiary = Color(0xFFFAF9F7),
        tertiaryContainer = Color(0xFF292524),
        onTertiaryContainer = Color(0xFFFAF9F7),
        surfaceVariant = Color(0xFFF0EBE5),
        onSurfaceVariant = Color(0xFF78716C),
        outline = Color(0xFFD6D3CE),
        error = Color(0xFFEF4444),
        onError = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        surface = Color(0xFF1A1614),
        onSurface = Color(0xFFF5F0EB),
        background = Color(0xFF1A1614),
        onBackground = Color(0xFFF5F0EB),
        primary = Color(0xFFF5F0EB),
        onPrimary = Color(0xFF1C1917),
        primaryContainer = Color(0xFF302A27),
        onPrimaryContainer = Color(0xFFF5F0EB),
        secondary = Color(0xFF302A27),
        onSecondary = Color(0xFFF5F0EB),
        secondaryContainer = Color(0xFF302A27),
        onSecondaryContainer = Color(0xFFF5F0EB),
        tertiary = Color(0xFFF5F0EB),
        onTertiary = Color(0xFF1C1917),
        tertiaryContainer = Color(0xFFF5F0EB),
        onTertiaryContainer = Color(0xFF1C1917),
        surfaceVariant = Color(0xFF302A27),
        onSurfaceVariant = Color(0xFFA8A29E),
        outline = Color(0xFF302A27),
        error = Color(0xFFF87171),
        onError = Color(0xFF1C1917),
    )

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
