package sudoku.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sudoku.app.ui.component.animatedGradient

@Composable
fun HomeScreen(
    onPlayClick: () -> Unit,
    onLearnClick: () -> Unit,
    onPracticeClick: () -> Unit,
    gradientEnabled: Boolean = true,
    onToggleGradient: () -> Unit = {},
) {
    val bgModifier =
        if (gradientEnabled) {
            Modifier.fillMaxSize().animatedGradient()
        } else {
            Modifier.fillMaxSize()
        }
    Box(modifier = bgModifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Gradient",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = gradientEnabled,
                    onCheckedChange = { onToggleGradient() },
                )
            }

            Spacer(Modifier.weight(1f))

            AppIcon(
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Don't Call it Hodoku",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.weight(1.6f))

            val btnColors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            OutlinedButton(
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = btnColors,
            ) {
                Text("Play")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLearnClick,
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = btnColors,
            ) {
                Text("Learn")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPracticeClick,
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = btnColors,
            ) {
                Text("Practice")
            }

            Spacer(Modifier.weight(0.4f))
        }
    }
}

@Composable
private fun AppIcon(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cell = w / 3f
        val strokeWidth = w * 0.012f
        // Internal grid lines only — no outer border
        for (i in 1..2) {
            val pos = cell * i
            drawLine(lineColor, Offset(pos, cell * 0.3f), Offset(pos, h - cell * 0.3f), strokeWidth, StrokeCap.Round)
            drawLine(lineColor, Offset(cell * 0.3f, pos), Offset(w - cell * 0.3f, pos), strokeWidth, StrokeCap.Round)
        }
        // Letters: D(0,0) C(0,2) i(2,0) H(2,2)
        val fontSize = (cell * 0.55f / density).sp
        val letters =
            listOf(
                Triple("D", 0, 0),
                Triple("C", 0, 2),
                Triple("i", 2, 0),
                Triple("H", 2, 2),
            )
        val style = TextStyle(color = textColor, fontSize = fontSize, textAlign = TextAlign.Center)
        for ((letter, row, col) in letters) {
            val cx = col * cell + cell / 2f
            val cy = row * cell + cell / 2f
            val result = measurer.measure(letter, style)
            drawText(
                result,
                topLeft =
                    Offset(
                        cx - result.size.width / 2f,
                        cy - result.size.height / 2f,
                    ),
            )
        }
    }
}
