package sudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sudoku.app.navigation.Screen
import sudoku.app.ui.screen.GameScreen
import sudoku.app.ui.screen.HomeScreen
import sudoku.app.ui.screen.LearnScreen
import sudoku.app.ui.theme.SudokuTheme

@Composable
fun App() {
    SudokuTheme {
        var currentScreen: Screen by remember { mutableStateOf(Screen.Home) }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .safeDrawingPadding(),
        ) {
            when (currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        onPlayClick = { currentScreen = Screen.Play },
                        onLearnClick = { currentScreen = Screen.Learn() },
                        onPracticeClick = { currentScreen = Screen.Practice() },
                    )
                }

                is Screen.Play -> {
                    GameScreen(
                        onNavigateHome = { currentScreen = Screen.Home },
                    )
                }

                is Screen.Learn -> {
                    LearnScreen(
                        technique = (currentScreen as Screen.Learn).technique,
                        onBack = {
                            val current = currentScreen as Screen.Learn
                            currentScreen = if (current.technique != null) Screen.Learn() else Screen.Home
                        },
                        onHome = { currentScreen = Screen.Home },
                        onNavigateTechnique = { currentScreen = Screen.Learn(it) },
                    )
                }

                is Screen.Practice -> {
                    PlaceholderScreen(
                        title = "Practice",
                        onBack = { currentScreen = Screen.Home },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Home")
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$title — coming soon",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
