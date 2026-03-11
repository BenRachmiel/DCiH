package sudoku.app.engine

import sudoku.core.model.CandidateHighlight
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionStep
import sudoku.engine.RustEngine

actual object NativeEngine {

    actual fun ensureLoaded() {
        RustEngine.ensureLoaded()
    }

    actual fun generatePuzzle(difficulty: Difficulty, maxRetries: Int): GenerateResult {
        val json = RustEngine.generatePuzzle(difficulty.ordinal, maxRetries)
        val (puzzle, solution, diffScore) = RustEngine.parseGenerateResult(json)
        return GenerateResult(puzzle, solution, diffScore.first, diffScore.second)
    }

    actual fun findNextStep(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        maxDifficulty: Difficulty?,
    ): SolutionStep? {
        val (byteValues, candMasks, byteSolution) = buildArrays(values, pencilMarks, solution)
        return RustEngine.findNextStep(byteValues, candMasks, byteSolution, maxDifficulty?.ordinal ?: -1)
    }

    actual fun buildHighlights(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        step: SolutionStep,
    ): List<CandidateHighlight> {
        val (byteValues, candMasks, byteSolution) = buildArrays(values, pencilMarks, solution)
        return RustEngine.buildHighlights(byteValues, candMasks, byteSolution, step)
    }

    /**
     * Convert high-level game state arrays to the raw format RustEngine expects.
     * Candidate bitmasks: use pencil marks if non-empty, otherwise compute from peer values.
     */
    private fun buildArrays(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
    ): Triple<ByteArray, ShortArray, ByteArray> {
        val byteValues = ByteArray(81) { values[it].toByte() }
        val byteSolution = ByteArray(81) { solution[it].toByte() }
        val candMasks = ShortArray(81) { i ->
            if (values[i] != 0) {
                0
            } else if (pencilMarks[i].isNotEmpty()) {
                var mask = 0
                for (d in pencilMarks[i]) mask = mask or (1 shl (d - 1))
                mask.toShort()
            } else {
                val used = BooleanArray(10)
                for (buddy in Buddies.ARRAY[i]) {
                    val v = values[buddy]
                    if (v != 0) used[v] = true
                }
                var mask = 0
                for (d in 1..9) if (!used[d]) mask = mask or (1 shl (d - 1))
                mask.toShort()
            }
        }
        return Triple(byteValues, candMasks, byteSolution)
    }
}
