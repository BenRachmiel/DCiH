package sudoku.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sudoku.core.model.BoardExample
import sudoku.core.model.HighlightRole

/** Highlight role → color mapping, visible on both light and dark zinc themes. */
private val roleColors = mapOf(
    HighlightRole.DEFINING to Color(0xFF4CAF50),
    HighlightRole.ELIMINATION to Color(0xFFEF5350),
    HighlightRole.SECONDARY to Color(0xFF42A5F5),
    HighlightRole.TERTIARY to Color(0xFFFFA726),
    HighlightRole.COLOR_A to Color(0xFF66BB6A),
    HighlightRole.COLOR_B to Color(0xFFAB47BC),
)

/**
 * Read-only board for strategy examples with per-candidate highlighting.
 *
 * Renders placed digits, candidate pencil marks in a 3×3 sub-grid, and
 * coloured rounded-rect badges behind highlighted candidates.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ExampleBoard(
    example: BoardExample,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val gridColor = colorScheme.onSurface
    val givenColor = colorScheme.onSurface
    val pencilColor = colorScheme.onSurface.copy(alpha = 0.7f)
    val backgroundColor = colorScheme.surface
    val cellShadeColor = colorScheme.primaryContainer.copy(alpha = 0.3f)

    val textMeasurer = rememberTextMeasurer()

    // Pre-compute values and candidates
    val values = remember(example.puzzle) {
        IntArray(81) { i ->
            val ch = example.puzzle[i]
            if (ch in '1'..'9') ch - '0' else 0
        }
    }
    val candidates = remember(example) { example.resolvedCandidates() }

    // Pre-build highlight lookup: key = cellIndex * 10 + digit → role
    val highlightMap = remember(example.highlights) {
        val map = HashMap<Int, HighlightRole>(example.highlights.size)
        for (h in example.highlights) {
            map[h.cellIndex * 10 + h.value] = h.role
        }
        map
    }

    Canvas(
        modifier = modifier
            .widthIn(max = 400.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
    ) {
        val cellSize = size.width / 9f

        // Background
        drawRect(backgroundColor, Offset.Zero, size)

        // Cell shading for cellHighlights
        for (idx in example.cellHighlights) {
            val row = idx / 9
            val col = idx % 9
            drawRect(
                cellShadeColor,
                Offset(col * cellSize, row * cellSize),
                Size(cellSize, cellSize),
            )
        }

        // Grid lines
        val thinWidth = cellSize * 0.01f
        val thickWidth = cellSize * 0.04f
        for (i in 0..9) {
            val lineWidth = if (i % 3 == 0) thickWidth else thinWidth
            val color = gridColor.copy(alpha = if (i % 3 == 0) 0.8f else 0.3f)
            drawLine(color, Offset(0f, i * cellSize), Offset(size.width, i * cellSize), lineWidth)
            drawLine(color, Offset(i * cellSize, 0f), Offset(i * cellSize, size.height), lineWidth)
        }

        // Cell contents
        for (row in 0..8) {
            for (col in 0..8) {
                val idx = row * 9 + col
                val x = col * cellSize
                val y = row * cellSize
                val value = values[idx]

                if (value != 0) {
                    // Placed digit
                    val style = TextStyle(
                        fontSize = (cellSize * 0.55f).toSp(),
                        color = givenColor,
                        fontWeight = FontWeight.Bold,
                    )
                    val measured = textMeasurer.measure(value.toString(), style)
                    drawText(
                        measured,
                        topLeft = Offset(
                            x + (cellSize - measured.size.width) / 2f,
                            y + (cellSize - measured.size.height) / 2f,
                        ),
                    )
                } else {
                    // Pencil marks with per-candidate highlighting
                    val mask = candidates[idx]
                    if (mask == 0) continue
                    val markSize = cellSize / 3f
                    val badgePad = markSize * 0.08f
                    val badgeRadius = markSize * 0.18f

                    for (d in 1..9) {
                        if (mask and (1 shl (d - 1)) == 0) continue
                        val mr = (d - 1) / 3
                        val mc = (d - 1) % 3
                        val mx = x + mc * markSize
                        val my = y + mr * markSize

                        val role = highlightMap[idx * 10 + d]

                        if (role != null) {
                            val badgeColor = roleColors[role] ?: pencilColor
                            // Draw coloured rounded-rect badge behind digit
                            drawRoundRect(
                                color = badgeColor,
                                topLeft = Offset(mx + badgePad, my + badgePad),
                                size = Size(markSize - badgePad * 2, markSize - badgePad * 2),
                                cornerRadius = CornerRadius(badgeRadius, badgeRadius),
                            )
                            // Digit in white on badge
                            val style = TextStyle(
                                fontSize = (markSize * 0.7f).toSp(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            val measured = textMeasurer.measure(d.toString(), style)
                            drawText(
                                measured,
                                topLeft = Offset(
                                    mx + (markSize - measured.size.width) / 2f,
                                    my + (markSize - measured.size.height) / 2f,
                                ),
                            )
                            // Strikethrough for eliminations
                            if (role == HighlightRole.ELIMINATION) {
                                val centerY = my + markSize / 2f
                                drawLine(
                                    Color.White,
                                    Offset(mx + badgePad * 2, centerY),
                                    Offset(mx + markSize - badgePad * 2, centerY),
                                    strokeWidth = markSize * 0.08f,
                                )
                            }
                        } else {
                            // Normal pencil mark
                            val style = TextStyle(
                                fontSize = (markSize * 0.7f).toSp(),
                                color = pencilColor,
                            )
                            val measured = textMeasurer.measure(d.toString(), style)
                            drawText(
                                measured,
                                topLeft = Offset(
                                    mx + (markSize - measured.size.width) / 2f,
                                    my + (markSize - measured.size.height) / 2f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Float.toSp() = this.sp
