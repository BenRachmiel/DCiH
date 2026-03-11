package sudoku.app.game

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sudoku.app.engine.Buddies
import sudoku.app.engine.NativeEngine
import sudoku.app.model.CandidateHighlight
import sudoku.app.model.Difficulty
import sudoku.app.model.HighlightRole
import sudoku.app.model.SolutionStep
import sudoku.app.game.copyMutable
import sudoku.app.game.copyImmutable

class GameViewModel(
    initialState: GameState = GameState(showNewGameDialog = true),
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val undoStack = mutableListOf<UndoEntry>()
    private var redoStack = mutableListOf<UndoEntry>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var generateJob: Job? = null

    /** Cached hint step's board state for building highlights at level 3. */
    private var hintValues: IntArray? = null
    private var hintPencilMarks: Array<out Set<Int>>? = null
    private var hintSolution: IntArray? = null

    /** Cached pencil mark errors for hint flow (transient, not in GameState). */
    private var pencilMarkErrors: PencilMarkErrors? = null

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.SelectCell -> selectCell(action.row, action.col)
            is GameAction.EnterDigit -> enterDigit(action.digit)
            is GameAction.Erase -> erase()
            is GameAction.TogglePencilMode -> togglePencil()
            is GameAction.Undo -> undo()
            is GameAction.Redo -> redo()
            is GameAction.ToggleErrorChecking -> toggleErrorChecking()
            is GameAction.NewGame -> newGame(action.difficulty)
            is GameAction.ShowNewGameDialog -> showNewGameDialog()
            is GameAction.DismissNewGameDialog -> dismissNewGameDialog()
            is GameAction.DismissWinDialog -> dismissWinDialog()
            is GameAction.FillAllCandidates -> fillAllCandidates()
            is GameAction.ToggleFilterDigit -> toggleFilterDigit(action.digit)
            is GameAction.DoubleTapCell -> doubleTapCell(action.row, action.col)
            is GameAction.ToggleBivalueHighlight -> toggleBivalueHighlight()
            is GameAction.ToggleTrovalueHighlight -> toggleTrovalueHighlight()
            is GameAction.TogglePeerHighlight -> togglePeerHighlight()
            is GameAction.DragSelectCells -> dragSelectCells(action.cells)
            is GameAction.RequestHint -> requestHint()
        }
    }

    private fun selectCell(
        row: Int,
        col: Int,
    ) {
        val s = _state.value
        if (s.isWon) return
        _state.value = s.copy(selectedRow = row, selectedCol = col, multiSelectedCells = emptySet())
    }

    private fun dragSelectCells(cells: Set<Int>) {
        val s = _state.value
        if (s.isWon) return
        val filtered = cells.filter { s.values[it] == 0 && !s.fixed[it] }.toSet()
        _state.value = s.copy(multiSelectedCells = filtered, selectedRow = -1, selectedCol = -1)
    }

    private fun enterDigit(digit: Int) {
        val s = clearHint(_state.value)

        // Batch pencil mark toggle for multi-selected cells
        if (s.multiSelectedCells.isNotEmpty()) {
            if (s.isWon) return
            val editable = s.multiSelectedCells.filter { i -> s.values[i] == 0 && !s.fixed[i] }
            if (editable.isEmpty()) return

            saveUndo()
            val marks = s.pencilMarks.copyMutable()
            val anyHas = editable.any { digit in marks[it] }
            for (i in editable) {
                if (anyHas) marks[i].remove(digit) else marks[i].add(digit)
            }
            _state.value =
                s.copy(
                    pencilMarks = marks,
                    pencilMarkVersion = s.pencilMarkVersion + 1,
                )
            redoStack.clear()
            return
        }

        val idx = s.selectedIndex
        if (idx < 0 || s.fixed[idx] || s.isWon) return

        saveUndo()

        if (s.pencilMode) {
            val marks = s.pencilMarks.copyMutable()
            if (digit in marks[idx]) marks[idx].remove(digit) else marks[idx].add(digit)
            _state.value =
                s.copy(
                    pencilMarks = marks,
                    pencilMarkVersion = s.pencilMarkVersion + 1,
                )
        } else {
            val newValues = s.values.copyOf()
            val marks = s.pencilMarks.copyMutable()

            var newErrorCount = s.errorCount
            if (newValues[idx] == digit) {
                newValues[idx] = 0 // toggle off
            } else {
                newValues[idx] = digit
                marks[idx].clear()
                for (peer in Buddies.ARRAY[idx]) {
                    marks[peer].remove(digit)
                }
                if (s.solution[idx] != 0 && digit != s.solution[idx]) {
                    newErrorCount++
                }
            }

            val isWon = checkWin(newValues, s.solution)
            _state.value =
                s.copy(
                    values = newValues,
                    pencilMarks = marks,
                    pencilMarkVersion = s.pencilMarkVersion + 1,
                    errorCount = newErrorCount,
                    isWon = isWon,
                    showWinDialog = isWon,
                )
            if (isWon) timerJob?.cancel()
        }
        redoStack.clear()
    }

    private fun erase() {
        val s = clearHint(_state.value)
        val idx = s.selectedIndex
        if (idx < 0 || s.fixed[idx] || s.isWon) return

        saveUndo()
        val newValues = s.values.copyOf()
        val marks = s.pencilMarks.copyMutable()
        newValues[idx] = 0
        marks[idx].clear()
        _state.value =
            s.copy(
                values = newValues,
                pencilMarks = marks,
                pencilMarkVersion = s.pencilMarkVersion + 1,
            )
        redoStack.clear()
    }

    private fun togglePencil() {
        val s = _state.value
        _state.value = s.copy(pencilMode = !s.pencilMode)
    }

    private fun toggleErrorChecking() {
        val s = _state.value
        _state.value = s.copy(errorChecking = !s.errorChecking)
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        val s = clearHint(_state.value)
        // Save current state to redo
        redoStack.add(
            UndoEntry(
                values = s.values.copyOf(),
                pencilMarks = s.pencilMarks.copyImmutable(),
            ),
        )
        val entry = undoStack.removeLast()
        val marks = entry.pencilMarks.copyMutable()
        _state.value =
            s.copy(
                values = entry.values,
                pencilMarks = marks,
                pencilMarkVersion = s.pencilMarkVersion + 1,
            )
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        val s = clearHint(_state.value)
        undoStack.add(
            UndoEntry(
                values = s.values.copyOf(),
                pencilMarks = s.pencilMarks.copyImmutable(),
            ),
        )
        val entry = redoStack.removeLast()
        val marks = entry.pencilMarks.copyMutable()
        _state.value =
            s.copy(
                values = entry.values,
                pencilMarks = marks,
                pencilMarkVersion = s.pencilMarkVersion + 1,
            )
    }

    private fun saveUndo() {
        val s = _state.value
        undoStack.add(
            UndoEntry(
                values = s.values.copyOf(),
                pencilMarks = s.pencilMarks.copyImmutable(),
            ),
        )
        // Limit undo stack
        if (undoStack.size > 100) undoStack.removeAt(0)
    }

    private fun fillAllCandidates() {
        val s = clearHint(_state.value)
        if (s.isWon) return
        saveUndo()
        val marks = NativeEngine.computeAllCandidates(s.values)
        _state.value =
            s.copy(
                pencilMarks = marks,
                pencilMarkVersion = s.pencilMarkVersion + 1,
            )
        redoStack.clear()
    }

    private fun toggleFilterDigit(digit: Int) {
        val s = _state.value
        _state.value =
            s.copy(
                filterDigit = if (s.filterDigit == digit) 0 else digit,
            )
    }

    private fun toggleBivalueHighlight() {
        val s = _state.value
        _state.value = s.copy(bivalueHighlight = !s.bivalueHighlight)
    }

    private fun toggleTrovalueHighlight() {
        val s = _state.value
        _state.value = s.copy(trivalueHighlight = !s.trivalueHighlight)
    }

    private fun togglePeerHighlight() {
        val s = _state.value
        _state.value = s.copy(peerHighlight = !s.peerHighlight)
    }

    private fun doubleTapCell(
        row: Int,
        col: Int,
    ) {
        val s = _state.value
        val idx = row * 9 + col
        if (s.fixed[idx] || s.values[idx] != 0 || s.isWon) return

        val digit = NativeEngine.findSingleForCell(s.values, s.pencilMarks, idx)
        if (digit != 0) {
            placeDigit(s, idx, digit)
        }
    }

    private fun placeDigit(
        s: GameState,
        idx: Int,
        digit: Int,
    ) {
        val cleared = clearHint(s)
        saveUndo()
        val newValues = cleared.values.copyOf()
        val marks = cleared.pencilMarks.copyMutable()
        newValues[idx] = digit
        marks[idx].clear()
        for (peer in Buddies.ARRAY[idx]) {
            marks[peer].remove(digit)
        }
        val isWon = checkWin(newValues, cleared.solution)
        _state.value =
            cleared.copy(
                values = newValues,
                pencilMarks = marks,
                pencilMarkVersion = s.pencilMarkVersion + 1,
                selectedRow = idx / 9,
                selectedCol = idx % 9,
                isWon = isWon,
                showWinDialog = isWon,
            )
        if (isWon) timerJob?.cancel()
        redoStack.clear()
    }

    private fun newGame(difficulty: Difficulty) {
        generateJob?.cancel()
        timerJob?.cancel()
        _state.value =
            _state.value.copy(
                showNewGameDialog = false,
                isGenerating = true,
                isWon = false,
                showWinDialog = false,
            )
        undoStack.clear()
        redoStack.clear()

        generateJob =
            scope.launch {
                val result = NativeEngine.generatePuzzle(difficulty, 200)

                val values = IntArray(81)
                val fixed = BooleanArray(81)
                for (i in result.puzzle.indices) {
                    val ch = result.puzzle[i]
                    if (ch in '1'..'9') {
                        values[i] = ch - '0'
                        fixed[i] = true
                    }
                }

                _state.value =
                    GameState(
                        values = values,
                        fixed = fixed,
                        solution = result.solution,
                        pencilMarks = Array(81) { mutableSetOf() },
                        selectedRow = -1,
                        selectedCol = -1,
                        pencilMode = false,
                        errorChecking = true,
                        difficulty = result.difficulty,
                        elapsedSeconds = 0L,
                        isWon = false,
                        showNewGameDialog = false,
                        showWinDialog = false,
                        isGenerating = false,
                        errorCount = 0,
                    )
                startTimer()
            }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            scope.launch {
                while (isActive) {
                    delay(1000)
                    _state.update { s ->
                        if (!s.isWon && !s.isGenerating) {
                            s.copy(elapsedSeconds = s.elapsedSeconds + 1)
                        } else {
                            s
                        }
                    }
                }
            }
    }

    private fun showNewGameDialog() {
        _state.value = _state.value.copy(showNewGameDialog = true)
    }

    private fun dismissNewGameDialog() {
        _state.value = _state.value.copy(showNewGameDialog = false)
    }

    private fun dismissWinDialog() {
        _state.value = _state.value.copy(showWinDialog = false)
    }

    private fun clearHint(s: GameState): GameState =
        if (s.hintLevel == 0 && s.hintMessage == null) {
            s
        } else {
            hintValues = null
            hintPencilMarks = null
            hintSolution = null
            pencilMarkErrors = null
            s.copy(hintLevel = 0, hintStep = null, hintHighlights = emptyList(), hintMessage = null)
        }

    private fun requestHint() {
        val s = _state.value
        if (s.isWon) return

        // Pencil mark error hint flow (hintStep is null, hintMessage is set)
        if (s.hintMessage != null) {
            requestPencilMarkHint(s)
            return
        }

        when (s.hintLevel) {
            0 -> {
                // Check for pencil mark errors first
                val errors = NativeEngine.findPencilMarkErrors(s.values, s.pencilMarks, s.solution)
                if (errors != null) {
                    pencilMarkErrors = errors
                    _state.value =
                        s.copy(
                            hintLevel = 1,
                            hintMessage = "Your pencil marks have errors",
                            hintCount = s.hintCount + 1,
                        )
                    return
                }

                // Vague: find next step via Rust engine
                val step = NativeEngine.findNextStep(s.values, s.pencilMarks, s.solution, null)
                    ?: return
                hintValues = s.values.copyOf()
                hintPencilMarks = s.pencilMarks.copyImmutable()
                hintSolution = s.solution.copyOf()
                _state.value =
                    s.copy(
                        hintLevel = 1,
                        hintStep = step,
                        hintHighlights = emptyList(),
                        hintCount = s.hintCount + 1,
                    )
            }

            1 -> {
                // Concrete: just advance level (step already cached)
                _state.value = s.copy(hintLevel = 2)
            }

            2 -> {
                // Full: build highlights from cached board state + step
                val step = s.hintStep ?: return
                val vals = hintValues ?: return
                val marks = hintPencilMarks ?: return
                val sol = hintSolution ?: return
                val highlights = NativeEngine.buildHighlights(vals, marks, sol, step)
                _state.value = s.copy(hintLevel = 3, hintHighlights = highlights)
            }

            3 -> {
                // Execute: apply the step
                val step = s.hintStep ?: return
                saveUndo()
                val newValues = s.values.copyOf()
                val marks = s.pencilMarks.copyMutable()

                if (step.type.isSingle) {
                    newValues[step.cellIndex] = step.value
                    marks[step.cellIndex].clear()
                    for (peer in Buddies.ARRAY[step.cellIndex]) {
                        marks[peer].remove(step.value)
                    }
                } else {
                    for ((cellIndex, digit) in step.candidatesRemoved) {
                        if (marks[cellIndex].isEmpty()) {
                            marks[cellIndex].addAll(computeCandidates(newValues, cellIndex))
                        }
                        marks[cellIndex].remove(digit)
                    }
                }

                hintValues = null
                hintPencilMarks = null
                hintSolution = null
                val isWon = checkWin(newValues, s.solution)
                _state.value =
                    s.copy(
                        values = newValues,
                        pencilMarks = marks,
                        pencilMarkVersion = s.pencilMarkVersion + 1,
                        hintLevel = 0,
                        hintStep = null,
                        hintHighlights = emptyList(),
                        hintMessage = null,
                        isWon = isWon,
                        showWinDialog = isWon,
                    )
                if (isWon) timerJob?.cancel()
                redoStack.clear()
            }
        }
    }

    private fun requestPencilMarkHint(s: GameState) {
        val errors = pencilMarkErrors ?: return

        when (s.hintLevel) {
            1 -> {
                // Concrete: show counts
                val parts = mutableListOf<String>()
                if (errors.toRemove.isNotEmpty()) parts.add("${errors.toRemove.size} impossible")
                if (errors.toAdd.isNotEmpty()) parts.add("${errors.toAdd.size} missing")
                _state.value = s.copy(hintLevel = 2, hintMessage = parts.joinToString(", "))
            }

            2 -> {
                // Full: build highlights from error lists
                val highlights = mutableListOf<CandidateHighlight>()
                for ((cell, digit) in errors.toRemove) {
                    highlights.add(CandidateHighlight(cell, digit, HighlightRole.ELIMINATION))
                }
                for ((cell, digit) in errors.toAdd) {
                    highlights.add(CandidateHighlight(cell, digit, HighlightRole.DEFINING))
                }
                _state.value = s.copy(hintLevel = 3, hintHighlights = highlights)
            }

            3 -> {
                // Execute: fix all pencil mark errors
                saveUndo()
                val marks = s.pencilMarks.copyMutable()
                for ((cell, digit) in errors.toRemove) {
                    marks[cell].remove(digit)
                }
                for ((cell, digit) in errors.toAdd) {
                    marks[cell].add(digit)
                }
                pencilMarkErrors = null
                _state.value =
                    s.copy(
                        pencilMarks = marks,
                        pencilMarkVersion = s.pencilMarkVersion + 1,
                        hintLevel = 0,
                        hintStep = null,
                        hintHighlights = emptyList(),
                        hintMessage = null,
                    )
                redoStack.clear()
            }
        }
    }

    private fun checkWin(
        values: IntArray,
        solution: IntArray,
    ): Boolean {
        if (solution.all { it == 0 }) return false
        for (i in values.indices) {
            if (values[i] == 0 || values[i] != solution[i]) return false
        }
        return true
    }
}
