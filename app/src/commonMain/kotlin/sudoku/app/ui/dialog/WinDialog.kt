package sudoku.app.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sudoku.core.model.Difficulty

@Composable
fun WinDialog(
    elapsedSeconds: Long,
    difficulty: Difficulty,
    onDismiss: () -> Unit,
    onNewGame: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Congratulations!", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You solved the ${difficulty.label} puzzle!")
                Text("Time: $minutes:${seconds.toString().padStart(2, '0')}")
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onNewGame) {
                Text("New Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
