package sudoku.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onPlayClick: () -> Unit,
    onLearnClick: () -> Unit,
    onPracticeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't Call it Hodoku",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(48.dp))
            FilledTonalButton(
                onClick = onPlayClick,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("Play")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLearnClick,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("Learn")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onPracticeClick,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("Practice")
            }
        }
    }
}
