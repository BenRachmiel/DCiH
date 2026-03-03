package sudoku.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import sudoku.app.game.GameState
import sudoku.app.game.computeCandidates

@OptIn(ExperimentalTextApi::class)
@Composable
fun SudokuBoard(
    state: GameState,
    modifier: Modifier = Modifier,
    onCellClick: (row: Int, col: Int) -> Unit,
    onCellDoubleTap: (row: Int, col: Int) -> Unit = { _, _ -> },
    onDragSelect: (Set<Int>) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val gridColor = colorScheme.onSurface
    val selectedColor = colorScheme.primaryContainer
    val peerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val sameDigitColor = colorScheme.primaryContainer.copy(alpha = 0.5f)
    val filterHighlightColor = Color(0xFF4CAF50).copy(alpha = 0.35f) // green
    val bivalueColor = Color(0xFF00897B).copy(alpha = 0.35f) // teal for 2-candidate
    val trivalueColor = Color(0xFF7E57C2).copy(alpha = 0.35f) // purple for 3-candidate
    val givenColor = colorScheme.onSurface
    val userColor = colorScheme.primary
    val errorColor = colorScheme.error
    val pencilColor = colorScheme.onSurface.copy(alpha = 0.85f)
    val backgroundColor = colorScheme.surface

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .pointerInput(Unit) {
                var lastTapMark: TimeMark? = null
                var lastTapIdx = -1

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cellSize = size.width / 9f
                    val startCol = (down.position.x / cellSize).toInt().coerceIn(0, 8)
                    val startRow = (down.position.y / cellSize).toInt().coerceIn(0, 8)
                    val startIdx = startRow * 9 + startCol

                    onCellClick(startRow, startCol)

                    val dragSet = mutableSetOf(startIdx)
                    var dragged = false
                    val dragThreshold = cellSize * 0.4f

                    while (true) {
                        val event = awaitPointerEvent()
                        val allUp = event.changes.all { it.changedToUp() }
                        if (allUp) {
                            event.changes.forEach { it.consume() }
                            break
                        }
                        val pos = event.changes.firstOrNull()?.position ?: break
                        val distFromStart = (pos - down.position).getDistance()
                        if (distFromStart < dragThreshold) {
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        val col = (pos.x / cellSize).toInt().coerceIn(0, 8)
                        val row = (pos.y / cellSize).toInt().coerceIn(0, 8)
                        val newIdx = row * 9 + col
                        if (newIdx !in dragSet) {
                            dragSet.add(newIdx)
                            dragged = true
                            onDragSelect(dragSet.toSet())
                        }
                        event.changes.forEach { it.consume() }
                    }

                    if (!dragged) {
                        val now = TimeSource.Monotonic.markNow()
                        val prev = lastTapMark
                        if (startIdx == lastTapIdx && prev != null && prev.elapsedNow().inWholeMilliseconds < 400) {
                            onCellDoubleTap(startRow, startCol)
                            lastTapMark = null
                            lastTapIdx = -1
                        } else {
                            lastTapMark = now
                            lastTapIdx = startIdx
                        }
                    }
                }
            }
    ) {
        val cellSize = size.width / 9f
        val selectedIdx = state.selectedIndex
        val selectedDigit = if (selectedIdx >= 0) state.values[selectedIdx] else 0

        // Background
        drawRect(backgroundColor, Offset.Zero, size)

        // Layered cell highlighting — each layer draws semi-transparent rects that stack
        for (row in 0..8) {
            for (col in 0..8) {
                val idx = row * 9 + col
                val x = col * cellSize
                val y = row * cellSize
                val cellRect = Size(cellSize, cellSize)
                val topLeft = Offset(x, y)

                // Effective candidates: use pencil marks if the user has set them,
                // otherwise fall back to computed candidates from the grid.
                val candidates = if (state.values[idx] == 0) {
                    val marks = state.pencilMarks[idx]
                    if (marks.isNotEmpty()) marks else computeCandidates(state.values, idx)
                } else emptySet()

                // Layer 1a: Bivalue highlight (2-candidate cells)
                if (state.bivalueHighlight && state.values[idx] == 0 && candidates.size == 2) {
                    drawRect(bivalueColor, topLeft, cellRect)
                }

                // Layer 1b: Trivalue highlight (3-candidate cells)
                if (state.trivalueHighlight && state.values[idx] == 0 && candidates.size == 3) {
                    drawRect(trivalueColor, topLeft, cellRect)
                }

                // Layer 2: Candidate filter highlight
                if (state.filterDigit in 1..9 && state.values[idx] == 0) {
                    if (state.filterDigit in candidates) {
                        drawRect(filterHighlightColor, topLeft, cellRect)
                    }
                }

                // Layer 3: Same-digit highlight (skip when multi-selecting)
                if (state.multiSelectedCells.isEmpty() &&
                    selectedDigit != 0 && state.values[idx] == selectedDigit &&
                    !(row == state.selectedRow && col == state.selectedCol)
                ) {
                    drawRect(sameDigitColor, topLeft, cellRect)
                }

                // Layer 4: Peer highlight (skip when multi-selecting or disabled)
                if (state.peerHighlight &&
                    state.multiSelectedCells.isEmpty() &&
                    selectedIdx >= 0 && !(row == state.selectedRow && col == state.selectedCol) &&
                    (row == state.selectedRow || col == state.selectedCol ||
                            (row / 3 == state.selectedRow / 3 && col / 3 == state.selectedCol / 3))
                ) {
                    drawRect(peerColor, topLeft, cellRect)
                }

                // Layer 5: Selected cell
                if (state.multiSelectedCells.isEmpty() &&
                    row == state.selectedRow && col == state.selectedCol
                ) {
                    drawRect(selectedColor, topLeft, cellRect)
                }

                // Layer 6: Multi-selected cells
                if (idx in state.multiSelectedCells) {
                    drawRect(selectedColor, topLeft, cellRect)
                }
            }
        }

        // Grid lines
        val thinWidth = cellSize * 0.01f
        val thickWidth = cellSize * 0.04f

        for (i in 0..9) {
            val lineWidth = if (i % 3 == 0) thickWidth else thinWidth
            val color = gridColor.copy(alpha = if (i % 3 == 0) 0.8f else 0.3f)
            // Horizontal
            drawLine(color, Offset(0f, i * cellSize), Offset(size.width, i * cellSize), lineWidth)
            // Vertical
            drawLine(color, Offset(i * cellSize, 0f), Offset(i * cellSize, size.height), lineWidth)
        }

        // Cell values and pencil marks
        for (row in 0..8) {
            for (col in 0..8) {
                val idx = row * 9 + col
                val x = col * cellSize
                val y = row * cellSize
                val value = state.values[idx]

                if (value != 0) {
                    val color = when {
                        state.isError(idx) -> errorColor
                        state.fixed[idx] -> givenColor
                        else -> userColor
                    }
                    val style = TextStyle(
                        fontSize = (cellSize * 0.55f).toSp(),
                        color = color,
                        fontWeight = if (state.fixed[idx])
                            androidx.compose.ui.text.font.FontWeight.Bold
                        else
                            androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    val text = value.toString()
                    val measured = textMeasurer.measure(text, style)
                    drawText(
                        measured,
                        topLeft = Offset(
                            x + (cellSize - measured.size.width) / 2f,
                            y + (cellSize - measured.size.height) / 2f
                        )
                    )
                } else {
                    // Pencil marks
                    val marks = state.pencilMarks[idx]
                    if (marks.isNotEmpty()) {
                        val markSize = cellSize / 3f
                        val markStyle = TextStyle(
                            fontSize = (markSize * 0.7f).toSp(),
                            color = pencilColor
                        )
                        for (d in marks) {
                            val mr = (d - 1) / 3
                            val mc = (d - 1) % 3
                            val measured = textMeasurer.measure(d.toString(), markStyle)
                            drawText(
                                measured,
                                topLeft = Offset(
                                    x + mc * markSize + (markSize - measured.size.width) / 2f,
                                    y + mr * markSize + (markSize - measured.size.height) / 2f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Float.toSp() = this.sp
