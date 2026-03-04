package sudoku.core

import sudoku.core.generator.buildHighlights
import sudoku.core.model.*
import sudoku.core.solver.StepFinder
import kotlin.test.*

class HintTest {
    // Easy puzzle solvable with singles
    private val easyPuzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    // Harder puzzle requiring advanced techniques
    private val hardPuzzle = "000801000000000043500000000000070800000000100020030000600000075003400000000200600"

    @Test
    fun testStepFinderReturnsStepForValidBoard() {
        val board = Board()
        board.loadFromString(easyPuzzle)
        val finder = StepFinder()
        val step = finder.findNextStep(board)
        assertNotNull(step, "StepFinder should find a step for a valid puzzle")
        assertTrue(step.type.isSingle, "Easy puzzle first step should be a single")
    }

    @Test
    fun testStepFinderReturnsNullForSolvedBoard() {
        // Load a fully completed grid — no empty cells
        val solved = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        val board = Board()
        board.loadFromString(solved)
        val finder = StepFinder()
        val step = finder.findNextStep(board)
        assertNull(step, "StepFinder should return null for a solved board")
    }

    @Test
    fun testDescribeVagueReturnsTechniqueName() {
        val step =
            SolutionStep(
                type = SolutionType.NAKED_SINGLE,
                cellIndex = 10,
                value = 5,
            )
        assertEquals("Naked Single", step.describeVague())
    }

    @Test
    fun testDescribeConcreteSingleIncludesCellAndDigit() {
        val step =
            SolutionStep(
                type = SolutionType.HIDDEN_SINGLE,
                cellIndex = 20, // row 3, col 3 (1-indexed: r3c3)
                value = 7,
            )
        val desc = step.describeConcrete()
        assertTrue(desc.contains("Hidden Single"), "Should include technique name")
        assertTrue(desc.contains("7"), "Should include digit")
        assertTrue(desc.contains("r3c3"), "Should include cell position")
    }

    @Test
    fun testDescribeConcreteEliminationIncludesDigits() {
        val step =
            SolutionStep(
                type = SolutionType.NAKED_PAIR,
                indices = listOf(0, 1),
                candidatesRemoved = listOf(2 to 3, 5 to 3, 2 to 7, 5 to 7),
            )
        val desc = step.describeConcrete()
        assertTrue(desc.contains("Naked Pair"), "Should include technique name")
        assertTrue(desc.contains("3") && desc.contains("7"), "Should include eliminated digits")
    }

    @Test
    fun testDescribeConcreteFishIncludesDigit() {
        val step =
            SolutionStep(
                type = SolutionType.X_WING,
                value = 4,
                indices = listOf(0, 8, 72, 80),
                candidatesRemoved = listOf(3 to 4, 6 to 4),
            )
        val desc = step.describeConcrete()
        assertTrue(desc.contains("X-Wing"), "Should include technique name")
        assertTrue(desc.contains("4"), "Should include digit")
    }

    @Test
    fun testBuildHighlightsForSingle() {
        val board = Board()
        board.loadFromString(easyPuzzle)
        val finder = StepFinder()
        val step = finder.findNextStep(board)
        assertNotNull(step)
        val highlights = buildHighlights(board, step)
        assertTrue(highlights.isNotEmpty(), "Highlights should not be empty")
        assertTrue(
            highlights.any { it.role == HighlightRole.DEFINING },
            "Single should have DEFINING highlights",
        )
    }

    @Test
    fun testBuildHighlightsForElimination() {
        val board = Board()
        board.loadFromString(hardPuzzle)
        val finder = StepFinder()
        // Advance past singles to find an elimination step
        var step: SolutionStep?
        do {
            step = finder.findNextStep(board)
            if (step == null) break
            if (!step.type.isSingle) break
            // Apply the step to advance
            board.setCell(step.cellIndex, step.value)
            board.setAllExposedSingles()
        } while (true)

        if (step != null && !step.type.isSingle) {
            val highlights = buildHighlights(board, step)
            assertTrue(highlights.isNotEmpty(), "Elimination step should produce highlights")
            assertTrue(
                highlights.any { it.role == HighlightRole.ELIMINATION },
                "Should have ELIMINATION highlights for ${step.type}",
            )
        }
    }

    @Test
    fun testBuildSolverBoardFromValues() {
        // Simulate what GameViewModel.buildSolverBoard does
        val values = IntArray(81)
        val puzzle = easyPuzzle
        for (i in puzzle.indices) {
            val ch = puzzle[i]
            if (ch in '1'..'9') values[i] = ch - '0'
        }

        val board = Board()
        for (i in 0 until 81) {
            if (values[i] != 0) board.setCell(i, values[i], isFixed = true)
        }

        // Board should have correct values
        for (i in 0 until 81) {
            assertEquals(values[i], board.values[i], "Cell $i should match")
        }

        // Board should have candidates for empty cells
        for (i in 0 until 81) {
            if (values[i] == 0) {
                assertTrue(board.cells[i] != 0, "Empty cell $i should have candidates")
            }
        }

        // StepFinder should find steps on this board
        val finder = StepFinder()
        val step = finder.findNextStep(board)
        assertNotNull(step, "Should find a step on reconstructed board")
    }
}
