package sudoku.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sudoku.app.game.GameAction
import sudoku.app.game.GameState

private val btnShape = RoundedCornerShape(6.dp)

@Composable
fun GameToolbar(
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.peerHighlight) {
            FilledTonalButton(
                onClick = { onAction(GameAction.TogglePeerHighlight) },
                shape = btnShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Peers", fontSize = 14.sp)
            }
        } else {
            OutlinedButton(
                onClick = { onAction(GameAction.TogglePeerHighlight) },
                shape = btnShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Peers", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        OutlinedButton(
            onClick = { onAction(GameAction.FillAllCandidates) },
            shape = btnShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("Fill", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Button(
            onClick = { onAction(GameAction.ShowNewGameDialog) },
            shape = btnShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("New", fontSize = 14.sp)
        }
    }
}
