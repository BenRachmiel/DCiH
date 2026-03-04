package sudoku.core.model

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

        fun fromJson(json: String): PuzzleJson {
            val puzzle = extractString(json, "puzzle")
            val givens = extractString(json, "givens")
            val solution = extractString(json, "solution")
            val difficulty = extractString(json, "difficulty")
            val candidates = extractCandidates(json)
            return PuzzleJson(puzzle, givens, solution, difficulty, candidates)
        }

        private fun extractString(
            json: String,
            key: String,
        ): String {
            val marker = "\"$key\":"
            val keyStart = json.indexOf(marker)
            if (keyStart < 0) return ""
            val afterKey = json.indexOf('"', keyStart + marker.length)
            if (afterKey < 0) return ""
            val end = json.indexOf('"', afterKey + 1)
            if (end < 0) return ""
            return json.substring(afterKey + 1, end)
        }

        private fun extractCandidates(json: String): Map<Int, List<Int>> {
            val marker = "\"candidates\":"
            val start = json.indexOf(marker)
            if (start < 0) return emptyMap()
            val braceOpen = json.indexOf('{', start + marker.length)
            if (braceOpen < 0) return emptyMap()
            // Find matching close brace
            var depth = 1
            var pos = braceOpen + 1
            while (pos < json.length && depth > 0) {
                when (json[pos]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                pos++
            }
            val inner = json.substring(braceOpen + 1, pos - 1).trim()
            if (inner.isEmpty()) return emptyMap()

            val result = mutableMapOf<Int, List<Int>>()
            // Parse entries like "0": [2,5,7,8]
            var i = 0
            while (i < inner.length) {
                // Find next key
                val qStart = inner.indexOf('"', i)
                if (qStart < 0) break
                val qEnd = inner.indexOf('"', qStart + 1)
                if (qEnd < 0) break
                val cellIndex = inner.substring(qStart + 1, qEnd).toInt()
                val bracketOpen = inner.indexOf('[', qEnd)
                if (bracketOpen < 0) break
                val bracketClose = inner.indexOf(']', bracketOpen)
                if (bracketClose < 0) break
                val digits =
                    inner
                        .substring(bracketOpen + 1, bracketClose)
                        .split(',')
                        .filter { it.isNotBlank() }
                        .map { it.trim().toInt() }
                result[cellIndex] = digits
                i = bracketClose + 1
            }
            return result
        }
    }
}

/** Load a [PuzzleJson] into this board: sets givens, user values, candidates, and solution. */
fun Board.loadFromPuzzleJson(pj: PuzzleJson) {
    clear()

    // Set givens
    for (i in 0 until Board.LENGTH) {
        val g = pj.givens[i] - '0'
        if (g in 1..9) {
            setCell(i, g, isFixed = true)
        }
    }

    // Set non-given user values
    for (i in 0 until Board.LENGTH) {
        val v = pj.puzzle[i] - '0'
        if (v in 1..9 && !fixed[i]) {
            setCell(i, v, isFixed = false)
        }
    }

    // Apply candidate restrictions for empty cells with explicit pencil marks
    for ((cell, digits) in pj.candidates) {
        if (values[cell] == 0) {
            val keepMask = digits.fold(0) { acc, d -> acc or Board.MASKS[d] }
            // Remove candidates not in the explicit list
            for (d in 1..9) {
                if (keepMask and Board.MASKS[d] == 0) {
                    setCandidate(cell, d, false)
                }
            }
        }
    }

    // Set solution
    if (pj.solution.any { it != '0' }) {
        for (i in 0 until Board.LENGTH) {
            solution[i] = pj.solution[i] - '0'
        }
        solutionSet = true
    }
}
