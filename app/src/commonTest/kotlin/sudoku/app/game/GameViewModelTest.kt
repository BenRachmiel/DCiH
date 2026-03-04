package sudoku.app.game

import sudoku.core.model.Board
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionStep
import sudoku.core.model.setAllExposedSingles
import sudoku.core.solver.StepFinder
import kotlin.test.*

class GameViewModelTest {
    // Hard puzzle requiring advanced techniques past singles
    private val hardPuzzle = "000801000000000043500000000000070800000000100020030000600000075003400000000200600"

    private fun createGameState(
        puzzleString: String,
        solutionValues: IntArray = IntArray(81),
    ): GameState {
        val values = IntArray(81)
        val fixed = BooleanArray(81)
        for (i in puzzleString.indices) {
            val ch = puzzleString[i]
            if (ch in '1'..'9') {
                values[i] = ch - '0'
                fixed[i] = true
            }
        }
        return GameState(
            values = values,
            fixed = fixed,
            solution = solutionValues,
            pencilMarks = Array(81) { mutableSetOf() },
            selectedRow = -1,
            selectedCol = -1,
            difficulty = Difficulty.HARD,
            errorChecking = true,
        )
    }

    /** Advance past singles on a Board copy, returning the first non-single step found. */
    private fun advancePastSingles(puzzleString: String): Pair<Board, SolutionStep>? {
        val board = Board()
        board.loadFromString(puzzleString)
        val finder = StepFinder()
        while (true) {
            val step = finder.findNextStep(board) ?: return null
            if (!step.type.isSingle) return board to step
            board.setCell(step.cellIndex, step.value)
            board.setAllExposedSingles()
        }
    }

    // --- Existing tests ---

    @Test
    fun peerHighlightDefaultsToTrue() {
        val vm = GameViewModel()
        assertTrue(vm.state.value.peerHighlight)
    }

    @Test
    fun togglePeerHighlightFlipsState() {
        val vm = GameViewModel()
        assertTrue(vm.state.value.peerHighlight)

        vm.onAction(GameAction.TogglePeerHighlight)
        assertFalse(vm.state.value.peerHighlight)

        vm.onAction(GameAction.TogglePeerHighlight)
        assertTrue(vm.state.value.peerHighlight)
    }

    @Test
    fun togglePeerHighlightDoesNotAffectOtherHighlights() {
        val vm = GameViewModel()
        val before = vm.state.value

        vm.onAction(GameAction.TogglePeerHighlight)
        val after = vm.state.value

        assertEquals(before.bivalueHighlight, after.bivalueHighlight)
        assertEquals(before.trivalueHighlight, after.trivalueHighlight)
        assertEquals(before.filterDigit, after.filterDigit)
    }

    @Test
    fun peerHighlightIncludedInEquality() {
        val a = GameState(peerHighlight = true)
        val b = GameState(peerHighlight = false)
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun peerHighlightPreservedAcrossOtherToggles() {
        val vm = GameViewModel()
        vm.onAction(GameAction.TogglePeerHighlight)
        assertFalse(vm.state.value.peerHighlight)

        // Toggle pencil mode — peerHighlight should stay off
        vm.onAction(GameAction.TogglePencilMode)
        assertFalse(vm.state.value.peerHighlight)

        // Toggle error checking — peerHighlight should stay off
        vm.onAction(GameAction.ToggleErrorChecking)
        assertFalse(vm.state.value.peerHighlight)
    }

    // --- Bug 2: Double-tap uses effective candidates ---

    @Test
    fun testDoubleTapNakedSingleViaPencilMarks() {
        val state = createGameState(hardPuzzle)
        val vm = GameViewModel(state)

        // Fill all candidates
        vm.onAction(GameAction.FillAllCandidates)
        val s = vm.state.value

        // Find a cell with exactly 2 candidates
        var targetIdx = -1
        var removableDigit = 0
        var remainingDigit = 0
        for (i in 0 until 81) {
            if (s.values[i] == 0 && s.pencilMarks[i].size == 2) {
                targetIdx = i
                val cands = s.pencilMarks[i].sorted()
                removableDigit = cands[0]
                remainingDigit = cands[1]
                break
            }
        }
        if (targetIdx == -1) return // No bivalue cell found, skip

        // Remove one candidate via pencil mode, leaving a naked single
        vm.onAction(GameAction.SelectCell(targetIdx / 9, targetIdx % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(removableDigit))
        vm.onAction(GameAction.TogglePencilMode) // back to normal mode

        val beforeTap = vm.state.value
        assertEquals(1, beforeTap.pencilMarks[targetIdx].size)
        assertEquals(0, beforeTap.values[targetIdx])

        // Double-tap should detect naked single and place the digit
        vm.onAction(GameAction.DoubleTapCell(targetIdx / 9, targetIdx % 9))
        val afterTap = vm.state.value
        assertEquals(remainingDigit, afterTap.values[targetIdx])
    }

    @Test
    fun testDoubleTapHiddenSingleViaPencilMarks() {
        val state = createGameState(hardPuzzle)
        val vm = GameViewModel(state)

        // Fill all candidates
        vm.onAction(GameAction.FillAllCandidates)
        val s = vm.state.value

        // Find a hidden single scenario: a digit appearing in exactly 2 cells of a unit
        // Remove the digit from one cell's pencil marks, making it a hidden single in the other
        var targetIdx = -1
        var targetDigit = 0
        var removeFromIdx = -1

        for (unit in Board.ALL_UNITS) {
            for (digit in 1..9) {
                val cellsWithDigit =
                    unit.filter { i ->
                        s.values[i] == 0 && digit in s.pencilMarks[i]
                    }
                if (cellsWithDigit.size == 2) {
                    // Remove digit from one cell, making it a hidden single in the other
                    removeFromIdx = cellsWithDigit[0]
                    targetIdx = cellsWithDigit[1]
                    targetDigit = digit
                    break
                }
            }
            if (targetIdx != -1) break
        }
        if (targetIdx == -1) return // Skip if no suitable case found

        // The target cell must have more than 1 candidate (otherwise it's already a naked single)
        if (s.pencilMarks[targetIdx].size <= 1) return

        // Remove the digit from the other cell's pencil marks
        vm.onAction(GameAction.SelectCell(removeFromIdx / 9, removeFromIdx % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(targetDigit))
        vm.onAction(GameAction.TogglePencilMode)

        val beforeTap = vm.state.value
        assertEquals(0, beforeTap.values[targetIdx])

        // Double-tap should detect hidden single and place the digit
        vm.onAction(GameAction.DoubleTapCell(targetIdx / 9, targetIdx % 9))
        val afterTap = vm.state.value
        assertEquals(targetDigit, afterTap.values[targetIdx])
    }

    // --- Bug 1: Hint elimination is effective ---

    @Test
    fun testHintEliminationIsEffective() {
        // Advance past singles to get a state where first hint is an elimination
        val result = advancePastSingles(hardPuzzle) ?: return
        val (board, firstStep) = result

        // Build a GameState matching this board position
        val values = board.values.copyOf()
        val fixed = BooleanArray(81)
        for (i in hardPuzzle.indices) {
            if (hardPuzzle[i] in '1'..'9') fixed[i] = true
        }
        val state =
            GameState(
                values = values,
                fixed = fixed,
                solution = IntArray(81), // no solution needed for this test
                pencilMarks = Array(81) { mutableSetOf() },
                selectedRow = -1,
                selectedCol = -1,
                difficulty = Difficulty.HARD,
            )
        val vm = GameViewModel(state)

        // Fill candidates so pencil marks match solver state
        vm.onAction(GameAction.FillAllCandidates)

        // Request hint 4 times (vague → concrete → full → execute)
        vm.onAction(GameAction.RequestHint)
        val s1 = vm.state.value
        assertEquals(1, s1.hintLevel)
        val firstHintType = s1.hintStep?.type

        vm.onAction(GameAction.RequestHint) // concrete
        vm.onAction(GameAction.RequestHint) // full
        vm.onAction(GameAction.RequestHint) // execute
        assertEquals(0, vm.state.value.hintLevel, "Hint should be cleared after execution")

        // Request another hint — should find a different step (not the same elimination)
        vm.onAction(GameAction.RequestHint)
        val s2 = vm.state.value
        assertEquals(1, s2.hintLevel)
        val secondStep = s2.hintStep
        assertNotNull(secondStep)

        // The second step should differ from the first
        // (either different type or different cells/candidates)
        val sameStep =
            secondStep.type == firstHintType &&
                secondStep.cellIndex == firstStep.cellIndex &&
                secondStep.candidatesRemoved == firstStep.candidatesRemoved
        assertFalse(sameStep, "Second hint should be different from the first after execution")
    }

    @Test
    fun testHintExecutionPopulatesPencilMarks() {
        // Advance past singles to find an elimination step
        val result = advancePastSingles(hardPuzzle) ?: return
        val (board, step) = result
        if (step.type.isSingle || step.candidatesRemoved.isEmpty()) return

        // Build GameState with NO pencil marks filled
        val values = board.values.copyOf()
        val fixed = BooleanArray(81)
        for (i in hardPuzzle.indices) {
            if (hardPuzzle[i] in '1'..'9') fixed[i] = true
        }
        val state =
            GameState(
                values = values,
                fixed = fixed,
                solution = IntArray(81),
                pencilMarks = Array(81) { mutableSetOf() },
                selectedRow = -1,
                selectedCol = -1,
                difficulty = Difficulty.HARD,
            )
        val vm = GameViewModel(state)

        // Verify pencil marks are empty before hint
        val affectedCells = step.candidatesRemoved.map { it.first }.distinct()
        for (cell in affectedCells) {
            assertTrue(
                vm.state.value.pencilMarks[cell]
                    .isEmpty(),
                "Cell $cell should have no pencil marks initially",
            )
        }

        // Execute hint: vague → concrete → full → execute
        repeat(4) { vm.onAction(GameAction.RequestHint) }

        // Pencil marks should now be populated for affected cells (filled before removal)
        for (cell in affectedCells) {
            assertTrue(
                vm.state.value.pencilMarks[cell]
                    .isNotEmpty(),
                "Cell $cell should have pencil marks after hint execution",
            )
        }

        // The removed candidates should NOT be in the pencil marks
        for ((cell, digit) in step.candidatesRemoved) {
            assertFalse(
                digit in vm.state.value.pencilMarks[cell],
                "Digit $digit should be removed from cell $cell",
            )
        }
    }

    // --- Pencil mark validation in hints ---

    @Test
    fun testHintDetectsImpossiblePencilMark() {
        val state = createGameState(hardPuzzle)
        val vm = GameViewModel(state)

        // Fill all candidates
        vm.onAction(GameAction.FillAllCandidates)

        // Find a cell with a peer that has a placed value, and add that value as a pencil mark
        var targetIdx = -1
        var impossibleDigit = 0
        for (i in 0 until 81) {
            if (vm.state.value.values[i] != 0) continue
            for (buddy in Board.BUDDIES_ARRAY[i]) {
                val v = vm.state.value.values[buddy]
                if (v != 0 && v !in vm.state.value.pencilMarks[i]) {
                    targetIdx = i
                    impossibleDigit = v
                    break
                }
            }
            if (targetIdx != -1) break
        }
        if (targetIdx == -1) return

        // Add the impossible digit to pencil marks
        vm.onAction(GameAction.SelectCell(targetIdx / 9, targetIdx % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(impossibleDigit))
        vm.onAction(GameAction.TogglePencilMode)

        assertTrue(impossibleDigit in vm.state.value.pencilMarks[targetIdx])

        // Request hint — should detect pencil mark error
        vm.onAction(GameAction.RequestHint)
        val s = vm.state.value
        assertEquals(1, s.hintLevel)
        assertNotNull(s.hintMessage, "Should have a hint message for pencil mark errors")
        assertNull(s.hintStep, "hintStep should be null for pencil mark error hints")
    }

    @Test
    fun testHintDetectsMissingSolutionDigit() {
        // We need a puzzle with a known solution
        val board = Board()
        board.loadFromString(hardPuzzle)
        // Solve completely to get solution
        val solverBoard = Board()
        solverBoard.loadFromString(hardPuzzle)
        val finder = StepFinder()
        while (!solverBoard.isSolved) {
            val step = finder.findNextStep(solverBoard) ?: break
            if (step.type.isSingle) {
                solverBoard.setCell(step.cellIndex, step.value)
                solverBoard.setAllExposedSingles()
            } else {
                for ((cell, digit) in step.candidatesRemoved) {
                    solverBoard.setCandidate(cell, digit, false)
                }
                solverBoard.setAllExposedSingles()
            }
        }
        val solution = solverBoard.values.copyOf()

        val state = createGameState(hardPuzzle, solution)
        val vm = GameViewModel(state)

        // Fill all candidates
        vm.onAction(GameAction.FillAllCandidates)

        // Find a cell with pencil marks and remove the solution digit
        var targetIdx = -1
        var solutionDigit = 0
        for (i in 0 until 81) {
            if (vm.state.value.values[i] != 0) continue
            if (vm.state.value.pencilMarks[i]
                    .size > 1 && solution[i] != 0
            ) {
                targetIdx = i
                solutionDigit = solution[i]
                break
            }
        }
        if (targetIdx == -1) return

        // Remove the solution digit from pencil marks
        vm.onAction(GameAction.SelectCell(targetIdx / 9, targetIdx % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(solutionDigit))
        vm.onAction(GameAction.TogglePencilMode)

        assertFalse(solutionDigit in vm.state.value.pencilMarks[targetIdx])

        // Request hint — should detect missing solution digit
        vm.onAction(GameAction.RequestHint)
        val s = vm.state.value
        assertEquals(1, s.hintLevel)
        assertNotNull(s.hintMessage)
    }

    @Test
    fun testHintFixesAllPencilMarkErrors() {
        // Build a state with known solution
        val solverBoard = Board()
        solverBoard.loadFromString(hardPuzzle)
        val finder = StepFinder()
        while (!solverBoard.isSolved) {
            val step = finder.findNextStep(solverBoard) ?: break
            if (step.type.isSingle) {
                solverBoard.setCell(step.cellIndex, step.value)
                solverBoard.setAllExposedSingles()
            } else {
                for ((cell, digit) in step.candidatesRemoved) {
                    solverBoard.setCandidate(cell, digit, false)
                }
                solverBoard.setAllExposedSingles()
            }
        }
        val solution = solverBoard.values.copyOf()

        val state = createGameState(hardPuzzle, solution)
        val vm = GameViewModel(state)
        vm.onAction(GameAction.FillAllCandidates)

        // Add an impossible digit
        var impossibleCell = -1
        var impossibleDigit = 0
        for (i in 0 until 81) {
            if (vm.state.value.values[i] != 0) continue
            for (buddy in Board.BUDDIES_ARRAY[i]) {
                val v = vm.state.value.values[buddy]
                if (v != 0 && v !in vm.state.value.pencilMarks[i]) {
                    impossibleCell = i
                    impossibleDigit = v
                    break
                }
            }
            if (impossibleCell != -1) break
        }

        // Remove a solution digit from a different cell
        var missingCell = -1
        var missingDigit = 0
        for (i in 0 until 81) {
            if (i == impossibleCell) continue
            if (vm.state.value.values[i] != 0) continue
            if (vm.state.value.pencilMarks[i]
                    .size > 1 && solution[i] != 0
            ) {
                missingCell = i
                missingDigit = solution[i]
                break
            }
        }

        if (impossibleCell == -1 || missingCell == -1) return

        // Apply both errors
        vm.onAction(GameAction.SelectCell(impossibleCell / 9, impossibleCell % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(impossibleDigit))
        vm.onAction(GameAction.TogglePencilMode)

        vm.onAction(GameAction.SelectCell(missingCell / 9, missingCell % 9))
        vm.onAction(GameAction.TogglePencilMode)
        vm.onAction(GameAction.EnterDigit(missingDigit))
        vm.onAction(GameAction.TogglePencilMode)

        assertTrue(impossibleDigit in vm.state.value.pencilMarks[impossibleCell])
        assertFalse(missingDigit in vm.state.value.pencilMarks[missingCell])

        // Execute hint through all 4 levels
        vm.onAction(GameAction.RequestHint) // level 1: vague
        assertNotNull(vm.state.value.hintMessage)

        vm.onAction(GameAction.RequestHint) // level 2: concrete (counts)
        val msg = vm.state.value.hintMessage
        assertNotNull(msg)
        assertTrue(msg!!.contains("impossible") || msg.contains("missing"))

        vm.onAction(GameAction.RequestHint) // level 3: highlights
        assertTrue(
            vm.state.value.hintHighlights
                .isNotEmpty(),
        )

        vm.onAction(GameAction.RequestHint) // execute: fix all errors

        // Verify errors are fixed
        assertFalse(
            impossibleDigit in vm.state.value.pencilMarks[impossibleCell],
            "Impossible digit should be removed",
        )
        assertTrue(
            missingDigit in vm.state.value.pencilMarks[missingCell],
            "Missing digit should be added back",
        )
        assertEquals(0, vm.state.value.hintLevel)
        assertNull(vm.state.value.hintMessage)
    }
}
