package sudoku.core.model

/**
 * A board snapshot for strategy illustration.
 *
 * @param puzzle 81-char string (digits 1-9 for givens, '0' or '.' for empty).
 * @param candidateMasks Optional explicit candidate bitmasks per cell (9-bit, bit 0 = digit 1).
 *   When null, candidates are computed by eliminating peers of placed digits.
 * @param highlights Per-candidate colour annotations.
 * @param cellHighlights Whole-cell indices to shade (e.g. locked-candidate box).
 */
data class BoardExample(
    val puzzle: String,
    val candidateMasks: IntArray? = null,
    val highlights: List<CandidateHighlight> = emptyList(),
    val cellHighlights: Set<Int> = emptySet(),
) {
    /** Resolved candidate bitmask array (81 entries). */
    fun resolvedCandidates(): IntArray {
        if (candidateMasks != null) return candidateMasks
        val values = IntArray(81) { i ->
            val ch = puzzle[i]
            if (ch in '1'..'9') ch - '0' else 0
        }
        return IntArray(81) { i ->
            if (values[i] != 0) 0
            else computeCandidateMask(values, i)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardExample) return false
        return puzzle == other.puzzle &&
                candidateMasks.contentEquals(other.candidateMasks) &&
                highlights == other.highlights &&
                cellHighlights == other.cellHighlights
    }

    override fun hashCode(): Int {
        var result = puzzle.hashCode()
        result = 31 * result + (candidateMasks?.contentHashCode() ?: 0)
        result = 31 * result + highlights.hashCode()
        result = 31 * result + cellHighlights.hashCode()
        return result
    }
}

private fun IntArray?.contentEquals(other: IntArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return this == other
    if (size != other.size) return false
    for (i in indices) if (this[i] != other[i]) return false
    return true
}

/** Compute candidates for a cell by eliminating digits seen by peers. */
private fun computeCandidateMask(values: IntArray, index: Int): Int {
    val row = index / 9
    val col = index % 9
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    var mask = 0x1FF // bits 0..8 = digits 1..9
    for (c in 0..8) {
        val d = values[row * 9 + c]
        if (d != 0) mask = mask and (1 shl (d - 1)).inv()
    }
    for (r in 0..8) {
        val d = values[r * 9 + col]
        if (d != 0) mask = mask and (1 shl (d - 1)).inv()
    }
    for (r in boxRow until boxRow + 3) {
        for (c in boxCol until boxCol + 3) {
            val d = values[r * 9 + c]
            if (d != 0) mask = mask and (1 shl (d - 1)).inv()
        }
    }
    return mask
}
