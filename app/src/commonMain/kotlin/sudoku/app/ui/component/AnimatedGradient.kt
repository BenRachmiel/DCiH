package sudoku.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Modifier.animatedGradient(tint: Color = MaterialTheme.colorScheme.primaryContainer): Modifier {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
    )
    val surface = MaterialTheme.colorScheme.surface
    return this then
        Modifier.drawBehind {
            val rad = angle * (kotlin.math.PI / 180.0).toFloat()
            val diagonal = maxOf(size.width, size.height)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val dx = cos(rad) * diagonal / 2f
            val dy = sin(rad) * diagonal / 2f
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = listOf(surface, tint, surface),
                        start = Offset(cx - dx, cy - dy),
                        end = Offset(cx + dx, cy + dy),
                    ),
            )
        }
}
