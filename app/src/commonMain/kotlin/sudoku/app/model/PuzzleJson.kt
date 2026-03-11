package sudoku.app.model

/**
 * JSON-serializable snapshot of a puzzle's state.
 *
 * Manual JSON to avoid adding kotlinx-serialization as a dependency.
 */
data class PuzzleJson(
    /** Current cell values (81-char, givens + user entries, '0' = empty) */
    val puzzle: String,
    /** Original givens only (81-char, '0' = non-given) */
    val givens: String,
    /** Correct solution (81-char, all zeros if unavailable) */
    val solution: String,
    /** Difficulty label */
    val difficulty: String,
    /** Sparse pencil marks: cell index → sorted digit list */
    val candidates: Map<Int, List<Int>>,
) {
    fun toJson(): String =
        buildString {
            append("{\n")
            append("  \"puzzle\": \"")
            append(puzzle)
            append("\",\n")
            append("  \"givens\": \"")
            append(givens)
            append("\",\n")
            append("  \"solution\": \"")
            append(solution)
            append("\",\n")
            append("  \"difficulty\": \"")
            append(difficulty)
            append("\",\n")
            append("  \"candidates\": {")
            val entries = candidates.entries.sortedBy { it.key }
            if (entries.isNotEmpty()) {
                append("\n")
                entries.forEachIndexed { i, (cell, digits) ->
                    append("    \"")
                    append(cell)
                    append("\": [")
                    digits.forEachIndexed { j, d ->
                        if (j > 0) append(",")
                        append(d)
                    }
                    append("]")
                    if (i < entries.size - 1) append(",")
                    append("\n")
                }
                append("  ")
            }
            append("}\n")
            append("}")
        }

    companion object {
        fun fromGameState(
            values: IntArray,
            fixed: BooleanArray,
            solution: IntArray,
            pencilMarks: Array<out Set<Int>>,
            difficulty: Difficulty,
        ): PuzzleJson {
            val puzzleSb = StringBuilder(81)
            val givensSb = StringBuilder(81)
            val solutionSb = StringBuilder(81)
            val cands = mutableMapOf<Int, List<Int>>()

            for (i in 0 until 81) {
                puzzleSb.append(values[i])
                givensSb.append(if (fixed[i]) values[i] else 0)
                solutionSb.append(solution[i])
                val marks = pencilMarks[i]
                if (marks.isNotEmpty()) {
                    cands[i] = marks.sorted()
                }
            }
            return PuzzleJson(
                puzzle = puzzleSb.toString(),
                givens = givensSb.toString(),
                solution = solutionSb.toString(),
                difficulty = difficulty.label,
                candidates = cands,
            )
        }

    }
}
