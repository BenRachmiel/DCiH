package sudoku.app.engine

import sudoku.app.game.PencilMarkErrors
import sudoku.app.model.BoardExample
import sudoku.app.model.CandidateHighlight
import sudoku.app.model.Difficulty
import sudoku.app.model.SolutionStep
import sudoku.app.model.SolutionType
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

    actual fun generateExample(type: SolutionType, maxAttempts: Int): BoardExample? {
        val json = RustEngine.generateExample(type.ordinal, maxAttempts)
        if (json.isEmpty()) return null
        val (puzzle, candidateMasks, highlights) = RustEngine.parseBoardExampleResult(json) ?: return null
        return BoardExample(puzzle, candidateMasks, highlights)
    }

    actual fun computeAllCandidates(values: IntArray): Array<MutableSet<Int>> {
        val byteValues = ByteArray(81) { values[it].toByte() }
        val masks = RustEngine.computeAllCandidates(byteValues)
        return Array(81) { i ->
            val mask = masks[i].toInt() and 0xFFFF
            val set = mutableSetOf<Int>()
            for (d in 1..9) {
                if (mask and (1 shl (d - 1)) != 0) set.add(d)
            }
            set
        }
    }

    actual fun findPencilMarkErrors(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
    ): PencilMarkErrors? {
        val byteValues = ByteArray(81) { values[it].toByte() }
        val byteSolution = ByteArray(81) { solution[it].toByte() }
        val candMasks = ShortArray(81) { i ->
            if (values[i] != 0 || pencilMarks[i].isEmpty()) {
                0
            } else {
                var mask = 0
                for (d in pencilMarks[i]) mask = mask or (1 shl (d - 1))
                mask.toShort()
            }
        }
        val json = RustEngine.findPencilMarkErrors(byteValues, candMasks, byteSolution)
        if (json.isEmpty()) return null
        val (toRemove, toAdd) = RustEngine.parsePencilMarkErrors(json) ?: return null
        return PencilMarkErrors(toRemove, toAdd)
    }

    actual fun findSingleForCell(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        cellIndex: Int,
    ): Int {
        val byteValues = ByteArray(81) { values[it].toByte() }
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
        return RustEngine.findSingleForCell(byteValues, candMasks, cellIndex)
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
