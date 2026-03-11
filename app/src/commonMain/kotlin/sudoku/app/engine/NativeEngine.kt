package sudoku.app.engine

import sudoku.app.game.PencilMarkErrors
import sudoku.app.model.BoardExample
import sudoku.app.model.CandidateHighlight
import sudoku.app.model.Difficulty
import sudoku.app.model.SolutionStep
import sudoku.app.model.SolutionType

data class GenerateResult(
    val puzzle: String,
    val solution: IntArray,
    val difficulty: Difficulty,
    val score: Int,
) {
    override fun equals(other: Any?): Boolean =
        other is GenerateResult && puzzle == other.puzzle

    override fun hashCode(): Int = puzzle.hashCode()
}

expect object NativeEngine {
    fun ensureLoaded()

    fun generatePuzzle(difficulty: Difficulty, maxRetries: Int): GenerateResult

    /**
     * Find the next solution step for the given board state.
     * Candidate bitmasks are built from pencilMarks (if non-empty) or computed from peer values.
     */
    fun findNextStep(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        maxDifficulty: Difficulty?,
    ): SolutionStep?

    fun buildHighlights(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        step: SolutionStep,
    ): List<CandidateHighlight>

    fun generateExample(type: SolutionType, maxAttempts: Int): BoardExample?

    fun computeAllCandidates(values: IntArray): Array<MutableSet<Int>>

    fun findPencilMarkErrors(values: IntArray, pencilMarks: Array<out Set<Int>>, solution: IntArray): PencilMarkErrors?

    fun findSingleForCell(values: IntArray, pencilMarks: Array<out Set<Int>>, cellIndex: Int): Int
}
