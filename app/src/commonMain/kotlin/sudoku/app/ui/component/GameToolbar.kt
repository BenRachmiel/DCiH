package sudoku.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import sudoku.app.game.GameAction
import sudoku.app.game.GameState
import sudoku.app.model.PuzzleJson

private val btnShape = RoundedCornerShape(6.dp)

@Composable
fun GameToolbar(
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlinedColors =
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleButton(
            active = state.peerHighlight,
            label = "Peers",
            outlinedColors = outlinedColors,
            onClick = { onAction(GameAction.TogglePeerHighlight) },
        )
        ToggleButton(
            active = state.hintLevel > 0,
            label = when (state.hintLevel) {
                1 -> "More"
                2 -> "Show"
                3 -> "Execute"
                else -> "Hint"
            },
            outlinedColors = outlinedColors,
            onClick = { onAction(GameAction.RequestHint) },
        )
        OutlinedButton(
            onClick = { onAction(GameAction.FillAllCandidates) },
            shape = btnShape,
            colors = outlinedColors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("Fill", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        ExportButton(state, outlinedColors)
        Button(
            onClick = { onAction(GameAction.ShowNewGameDialog) },
            shape = btnShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("New", fontSize = 14.sp)
        }
    }
}

@Composable
private fun ToggleButton(
    active: Boolean,
    label: String,
    outlinedColors: ButtonColors,
    onClick: () -> Unit,
) {
    if (active) {
        FilledTonalButton(
            onClick = onClick,
            shape = btnShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(label, fontSize = 14.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = btnShape,
            colors = outlinedColors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ExportButton(
    state: GameState,
    outlinedColors: ButtonColors,
) {
    val clipboard = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    if (showCopied) {
        LaunchedEffect(Unit) {
            delay(2000)
            showCopied = false
        }
    }

    OutlinedButton(
        onClick = {
            clipboard.setText(AnnotatedString(buildExportString(state)))
            showCopied = true
        },
        shape = btnShape,
        colors = outlinedColors,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            if (showCopied) "Copied!" else "Export",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun buildExportString(state: GameState): String =
    PuzzleJson
        .fromGameState(
            values = state.values,
            fixed = state.fixed,
            solution = state.solution,
            pencilMarks = state.pencilMarks,
            difficulty = state.difficulty,
        ).toJson()
