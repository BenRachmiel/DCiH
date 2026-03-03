package sudoku.app.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionType

@Composable
fun NewGameDialog(
    onDismiss: () -> Unit,
    onConfirm: (Difficulty) -> Unit
) {
    val techniquesByDifficulty = remember { buildTechniqueHints() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Game", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (difficulty in Difficulty.entries) {
                    OutlinedCard(
                        onClick = { onConfirm(difficulty) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = difficulty.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            techniquesByDifficulty[difficulty]?.let { hint ->
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Builds a compact technique description for each difficulty level,
 * showing only the techniques NEW at that level.
 *
 * Uses category display name when 2+ techniques from a category appear;
 * uses the individual technique name when only one appears (e.g. "Remote Pair"
 * instead of "Wings" at MEDIUM).
 */
private fun buildTechniqueHints(): Map<Difficulty, String> =
    Difficulty.entries.associateWith { difficulty ->
        SolutionType.entries
            .filter { it.difficulty == difficulty && it != SolutionType.BRUTE_FORCE }
            .groupBy { it.category }
            .entries.joinToString(", ") { (category, techniques) ->
                if (techniques.size == 1) techniques[0].displayName
                else category.displayName
            }
    }.filterValues { it.isNotEmpty() }
