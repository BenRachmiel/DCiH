package sudoku.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import sudoku.app.game.GameAction
import sudoku.app.game.GameState

@Composable
fun NumberPad(
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Layout: [side col] [3x3 digits] [side col]
        // 3 rows high, side buttons same height as digit cells
        val sideRatio = 0.7f
        val gapRatio = 0.1f
        val cellFromWidth = maxWidth / (2f * sideRatio + 3f + 4f * gapRatio)
        val cellFromHeight = maxHeight / (3f + 2f * gapRatio)
        val cellSize = minOf(cellFromWidth, cellFromHeight)
        val sideWidth = cellSize * sideRatio
        val gap = cellSize * gapRatio
        val cornerRadius = cellSize * 0.08f
        val btnShape = RoundedCornerShape(cornerRadius)

        val density = LocalDensity.current
        val digitFontSize = with(density) { (cellSize.toPx() * 0.38f).toSp() }
        val sideFontSize = with(density) { (cellSize.toPx() * 0.26f).toSp() }
        val sideIconSize = cellSize * 0.38f

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Undo, Redo, Erase
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                SideButton(sideWidth, cellSize, btnShape, onClick = { onAction(GameAction.Undo) }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", modifier = Modifier.size(sideIconSize))
                }
                SideButton(sideWidth, cellSize, btnShape, onClick = { onAction(GameAction.Redo) }) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", modifier = Modifier.size(sideIconSize))
                }
                SideButton(sideWidth, cellSize, btnShape, onClick = { onAction(GameAction.Erase) }) {
                    Text("X", fontSize = sideFontSize, fontWeight = FontWeight.Bold)
                }
            }

            // Center: 3x3 digit grid
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (rowStart in intArrayOf(1, 4, 7)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (d in rowStart..rowStart + 2) {
                            DigitButton(d, state, onAction, Modifier.size(cellSize), digitFontSize, btnShape)
                        }
                    }
                }
            }

            // Right column: xy, xyz, Pencil
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                SideToggleButton(sideWidth, cellSize, btnShape,
                    active = state.bivalueHighlight,
                    onClick = { onAction(GameAction.ToggleBivalueHighlight) }
                ) {
                    Text("xy", fontSize = sideFontSize, fontWeight = FontWeight.Bold)
                }
                SideToggleButton(sideWidth, cellSize, btnShape,
                    active = state.trivalueHighlight,
                    onClick = { onAction(GameAction.ToggleTrovalueHighlight) }
                ) {
                    Text("xyz", fontSize = sideFontSize, fontWeight = FontWeight.Bold)
                }
                SideToggleButton(sideWidth, cellSize, btnShape,
                    active = state.pencilMode,
                    onClick = { onAction(GameAction.TogglePencilMode) }
                ) {
                    Icon(Icons.Default.Edit, "Pencil", modifier = Modifier.size(sideIconSize))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DigitButton(
    digit: Int,
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier,
    fontSize: TextUnit,
    shape: RoundedCornerShape
) {
    val count = state.values.count { it == digit }
    val isComplete = count >= 9
    val isFiltered = state.filterDigit == digit
    val colors = MaterialTheme.colorScheme

    val containerColor = when {
        isFiltered -> colors.tertiaryContainer
        isComplete -> colors.surfaceVariant.copy(alpha = 0.4f)
        else -> colors.secondaryContainer
    }
    val contentColor = when {
        isFiltered -> colors.onTertiaryContainer
        isComplete -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        else -> colors.onSecondaryContainer
    }
    val borderColor = if (isFiltered) colors.tertiaryContainer else colors.outline

    Surface(
        modifier = modifier
            .clip(shape)
            .combinedClickable(
                onClick = { onAction(GameAction.EnterDigit(digit)) },
                onLongClick = { onAction(GameAction.ToggleFilterDigit(digit)) }
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(digit.toString(), fontSize = fontSize, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SideButton(
    width: Dp, height: Dp, shape: RoundedCornerShape,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.width(width).height(height),
        shape = shape,
        color = Color.Transparent,
        contentColor = colors.onSurfaceVariant,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun SideToggleButton(
    width: Dp, height: Dp, shape: RoundedCornerShape,
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.width(width).height(height),
        shape = shape,
        color = if (active) colors.tertiaryContainer else Color.Transparent,
        contentColor = if (active) colors.onTertiaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(1.dp, if (active) colors.tertiaryContainer else colors.outline)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
