package sudoku.app.ui.screen

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sudoku.app.game.GameAction
import sudoku.app.game.GameViewModel
import sudoku.app.ui.component.GameToolbar
import sudoku.app.ui.component.NumberPad
import sudoku.app.ui.component.SudokuBoard
import sudoku.app.ui.dialog.NewGameDialog
import sudoku.app.ui.dialog.WinDialog

@Composable
fun GameScreen(onNavigateHome: () -> Unit = {}) {
    val viewModel = remember { GameViewModel() }
    val state by viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                // Shift+digit toggles candidate filter
                if (event.isShiftPressed) {
                    val filterDigit = when (event.key) {
                        Key.One, Key.NumPad1 -> 1
                        Key.Two, Key.NumPad2 -> 2
                        Key.Three, Key.NumPad3 -> 3
                        Key.Four, Key.NumPad4 -> 4
                        Key.Five, Key.NumPad5 -> 5
                        Key.Six, Key.NumPad6 -> 6
                        Key.Seven, Key.NumPad7 -> 7
                        Key.Eight, Key.NumPad8 -> 8
                        Key.Nine, Key.NumPad9 -> 9
                        else -> null
                    }
                    if (filterDigit != null) {
                        viewModel.onAction(GameAction.ToggleFilterDigit(filterDigit))
                        return@onPreviewKeyEvent true
                    }
                }

                when (event.key) {
                    Key.DirectionUp -> {
                        val r = if (state.selectedRow > 0) state.selectedRow - 1 else 8
                        viewModel.onAction(GameAction.SelectCell(r, maxOf(0, state.selectedCol)))
                        true
                    }
                    Key.DirectionDown -> {
                        val r = if (state.selectedRow < 8) state.selectedRow + 1 else 0
                        viewModel.onAction(GameAction.SelectCell(r, maxOf(0, state.selectedCol)))
                        true
                    }
                    Key.DirectionLeft -> {
                        val c = if (state.selectedCol > 0) state.selectedCol - 1 else 8
                        viewModel.onAction(GameAction.SelectCell(maxOf(0, state.selectedRow), c))
                        true
                    }
                    Key.DirectionRight -> {
                        val c = if (state.selectedCol < 8) state.selectedCol + 1 else 0
                        viewModel.onAction(GameAction.SelectCell(maxOf(0, state.selectedRow), c))
                        true
                    }
                    Key.One, Key.NumPad1 -> { viewModel.onAction(GameAction.EnterDigit(1)); true }
                    Key.Two, Key.NumPad2 -> { viewModel.onAction(GameAction.EnterDigit(2)); true }
                    Key.Three, Key.NumPad3 -> { viewModel.onAction(GameAction.EnterDigit(3)); true }
                    Key.Four, Key.NumPad4 -> { viewModel.onAction(GameAction.EnterDigit(4)); true }
                    Key.Five, Key.NumPad5 -> { viewModel.onAction(GameAction.EnterDigit(5)); true }
                    Key.Six, Key.NumPad6 -> { viewModel.onAction(GameAction.EnterDigit(6)); true }
                    Key.Seven, Key.NumPad7 -> { viewModel.onAction(GameAction.EnterDigit(7)); true }
                    Key.Eight, Key.NumPad8 -> { viewModel.onAction(GameAction.EnterDigit(8)); true }
                    Key.Nine, Key.NumPad9 -> { viewModel.onAction(GameAction.EnterDigit(9)); true }
                    Key.Delete, Key.Backspace -> { viewModel.onAction(GameAction.Erase); true }
                    Key.P -> { viewModel.onAction(GameAction.TogglePencilMode); true }
                    Key.H -> { viewModel.onAction(GameAction.TogglePeerHighlight); true }
                    Key.Z -> {
                        if (event.isCtrlPressed) { viewModel.onAction(GameAction.Undo); true }
                        else false
                    }
                    Key.Y -> {
                        if (event.isCtrlPressed) { viewModel.onAction(GameAction.Redo); true }
                        else false
                    }
                    else -> false
                }
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        if (state.isGenerating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generating puzzle...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val mw: Dp = this.maxWidth
                val mh: Dp = this.maxHeight
                val isLandscape = mw > mh

                if (isLandscape) {
                    LandscapeLayout(mw, mh, state, viewModel, onNavigateHome)
                } else {
                    PortraitLayout(mw, mh, state, viewModel, onNavigateHome)
                }
            }
        }
    }

    if (state.showNewGameDialog) {
        NewGameDialog(
            onDismiss = { viewModel.onAction(GameAction.DismissNewGameDialog) },
            onConfirm = { difficulty ->
                viewModel.onAction(GameAction.NewGame(difficulty))
            }
        )
    }

    if (state.showWinDialog) {
        WinDialog(
            elapsedSeconds = state.elapsedSeconds,
            difficulty = state.difficulty,
            onDismiss = { viewModel.onAction(GameAction.DismissWinDialog) },
            onNewGame = { viewModel.onAction(GameAction.ShowNewGameDialog) }
        )
    }
}

@Composable
private fun PortraitLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    state: sudoku.app.game.GameState,
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit
) {
    val sidePadding = 8.dp
    val topPadding = 8.dp
    val bottomPadding = 8.dp
    val contentWidth = maxWidth - sidePadding * 2
    // Board fills width, but leave at least 200dp for header+toolbar+pad+gaps
    val boardSize = minOf(contentWidth, maxHeight - topPadding - bottomPadding - 200.dp)

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(start = sidePadding, end = sidePadding, top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Home | Difficulty | Errors: N | Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateHome) {
                Text("Home")
            }
            Text(
                text = state.difficulty.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Errors: ${state.errorCount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (state.errorCount > 0) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(state.elapsedSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))

        SudokuBoard(
            state = state,
            modifier = Modifier.size(boardSize),
            onCellClick = { row, col ->
                viewModel.onAction(GameAction.SelectCell(row, col))
            },
            onCellDoubleTap = { row, col ->
                viewModel.onAction(GameAction.DoubleTapCell(row, col))
            },
            onDragSelect = { cells ->
                viewModel.onAction(GameAction.DragSelectCells(cells))
            }
        )
        Spacer(Modifier.height(4.dp))
        GameToolbar(
            state = state,
            onAction = viewModel::onAction
        )
        Spacer(Modifier.height(4.dp))
        // Number pad fills all remaining vertical space, scales as a unit
        NumberPad(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
private fun LandscapeLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    state: sudoku.app.game.GameState,
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boardSize = minOf(maxHeight - 16.dp, maxWidth * 0.55f)
        SudokuBoard(
            state = state,
            modifier = Modifier.size(boardSize),
            onCellClick = { row, col ->
                viewModel.onAction(GameAction.SelectCell(row, col))
            },
            onCellDoubleTap = { row, col ->
                viewModel.onAction(GameAction.DoubleTapCell(row, col))
            },
            onDragSelect = { cells ->
                viewModel.onAction(GameAction.DragSelectCells(cells))
            }
        )
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateHome) {
                    Text("Home")
                }
                Text(state.difficulty.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Errors: ${state.errorCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.errorCount > 0) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formatTime(state.elapsedSeconds), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            GameToolbar(
                state = state,
                onAction = viewModel::onAction
            )
            Spacer(Modifier.height(8.dp))
            NumberPad(
                state = state,
                onAction = viewModel::onAction,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
