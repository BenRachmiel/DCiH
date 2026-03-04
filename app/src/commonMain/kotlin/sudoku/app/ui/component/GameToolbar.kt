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

private val btnShape = RoundedCornerShape(6.dp)

@Composable
fun GameToolbar(
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.peerHighlight) {
            FilledTonalButton(
                onClick = { onAction(GameAction.TogglePeerHighlight) },
                shape = btnShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text("Peers", fontSize = 14.sp)
            }
        } else {
            OutlinedButton(
                onClick = { onAction(GameAction.TogglePeerHighlight) },
                shape = btnShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text("Peers", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // Hint button — label changes with hint level
        val hintLabel =
            when (state.hintLevel) {
                1 -> state.hintMessage ?: state.hintStep?.describeVague() ?: "Hint"
                2 -> state.hintMessage ?: state.hintStep?.describeConcrete() ?: "Hint"
                3 -> "Showing"
                else -> "Hint"
            }
        if (state.hintLevel > 0) {
            FilledTonalButton(
                onClick = { onAction(GameAction.RequestHint) },
                shape = btnShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(hintLabel, fontSize = 14.sp, maxLines = 1)
            }
        } else {
            OutlinedButton(
                onClick = { onAction(GameAction.RequestHint) },
                shape = btnShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text("Hint", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        OutlinedButton(
            onClick = { onAction(GameAction.FillAllCandidates) },
            shape = btnShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("Fill", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        ExportButton(state)
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
private fun ExportButton(state: GameState) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
    buildString {
        // Line 1: 81-char puzzle string
        for (v in state.values) append(v)
        // Subsequent lines: pencil marks for non-empty cells
        for (i in 0 until 81) {
            val marks = state.pencilMarks[i]
            if (marks.isNotEmpty()) {
                append('\n')
                append(i)
                append(':')
                append(marks.sorted().joinToString(""))
            }
        }
    }
