package sudoku.core

import sudoku.core.model.*
import sudoku.core.solver.StepFinder
import kotlin.test.*

class SingleDigitPatternTest {

    private val stepFinder = StepFinder()

    private fun findStepOfType(
        puzzleString: String,
        targetType: SolutionType,
    ): Pair<Board, SolutionStep>? {
        val board = Board()
        board.loadFromString(puzzleString)

        while (!board.isSolved) {
            val step = stepFinder.findNextStep(board) ?: break
            if (step.type == targetType) return board to step

            if (step.type.isSingle || step.type == SolutionType.BRUTE_FORCE) {
                board.setCell(step.cellIndex, step.value)
                board.setAllExposedSingles()
            } else {
                for ((cellIndex, candidate) in step.candidatesRemoved) {
                    board.setCandidate(cellIndex, candidate, false)
                }
                board.setAllExposedSingles()
            }
        }
        return null
    }

    // Puzzles found to contain specific single-digit patterns
    private val skyscraperPuzzle = "...486..384..3............2.3...8..6..8.2.5..4..7...1.2............5..976..891..."
    private val kitePuzzle = "..2.3.........5962.....8..1.8....4..5.4.9.2.3..6....8.7..5.....1237.........4.3.."
    private val erPuzzle = ".....6.4...1.3..5.9..2.813..5.3.....6.......8.....5.1..745.1..3.1..2.6...2.9....."

    @Test
    fun testHasSolverFlags() {
        assertTrue(SolutionType.SKYSCRAPER.hasSolver)
        assertTrue(SolutionType.TWO_STRING_KITE.hasSolver)
        assertTrue(SolutionType.EMPTY_RECTANGLE.hasSolver)
        assertTrue(SolutionType.TURBOT_FISH.hasSolver)
    }

    @Test
    fun testSkyscraper() {
        val result = findStepOfType(skyscraperPuzzle, SolutionType.SKYSCRAPER)
        assertNotNull(result, "Should find Skyscraper in curated puzzle")
        val (board, step) = result

        assertEquals(4, step.indices.size, "Skyscraper has 4 cells (2 strong links)")
        assertTrue(step.value in 1..9)
        assertTrue(step.candidatesRemoved.isNotEmpty())
        validateStep(board, step)
        validateTwoLinkEliminations(step)
    }

    @Test
    fun testTwoStringKite() {
        val result = findStepOfType(kitePuzzle, SolutionType.TWO_STRING_KITE)
        assertNotNull(result, "Should find 2-String Kite in curated puzzle")
        val (board, step) = result

        assertEquals(4, step.indices.size, "Kite has 4 cells")
        assertTrue(step.value in 1..9)
        assertTrue(step.candidatesRemoved.isNotEmpty())
        validateStep(board, step)
        validateTwoLinkEliminations(step)
    }

    @Test
    fun testEmptyRectangle() {
        val result = findStepOfType(erPuzzle, SolutionType.EMPTY_RECTANGLE)
        assertNotNull(result, "Should find Empty Rectangle in curated puzzle")
        val (board, step) = result

        assertTrue(step.indices.size >= 3, "ER has link cells + ER box cells")
        assertTrue(step.value in 1..9)
        assertTrue(step.candidatesRemoved.isNotEmpty())
        validateStep(board, step)
    }

    /** All eliminated candidates should exist on the board, all pattern cells should have the digit. */
    private fun validateStep(board: Board, step: SolutionStep) {
        for ((cell, digit) in step.candidatesRemoved) {
            assertTrue(
                board.isCandidate(cell, digit),
                "Eliminated candidate $digit at cell $cell should exist on board",
            )
        }
        for (cell in step.indices) {
            assertTrue(
                board.isCandidate(cell, step.value),
                "Pattern cell $cell should have candidate ${step.value}",
            )
        }
    }

    /** For two-link patterns, eliminated cells must see both chain endpoints. */
    private fun validateTwoLinkEliminations(step: SolutionStep) {
        val start = step.indices[0]
        val far = step.indices[3]

        for ((cell, _) in step.candidatesRemoved) {
            assertTrue(
                cell in Board.BUDDIES_ARRAY[start],
                "${step.type}: eliminated cell $cell should see endpoint $start",
            )
            assertTrue(
                cell in Board.BUDDIES_ARRAY[far],
                "${step.type}: eliminated cell $cell should see endpoint $far",
            )
        }
    }

    private operator fun IntArray.contains(value: Int): Boolean {
        for (v in this) if (v == value) return true
        return false
    }
}
