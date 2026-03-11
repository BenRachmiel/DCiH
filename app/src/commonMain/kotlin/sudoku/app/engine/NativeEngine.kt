package sudoku.app.engine

import sudoku.core.model.CandidateHighlight
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionStep

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
}
