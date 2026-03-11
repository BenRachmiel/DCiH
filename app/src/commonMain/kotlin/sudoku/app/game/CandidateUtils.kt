package sudoku.app.game

import sudoku.app.engine.Buddies

fun computeCandidates(
    values: IntArray,
    index: Int,
): Set<Int> {
    if (values[index] != 0) return emptySet()
    val used = BooleanArray(10)
    for (buddy in Buddies.ARRAY[index]) {
        val v = values[buddy]
        if (v != 0) used[v] = true
    }
    return buildSet { for (d in 1..9) if (!used[d]) add(d) }
}

/** Returns pencil marks if non-empty, else computed candidates. Single source of truth for user-visible candidates. */
fun effectiveCandidates(
    values: IntArray,
    pencilMarks: Array<out Set<Int>>,
    index: Int,
): Set<Int> {
    val marks = pencilMarks[index]
    return if (marks.isNotEmpty()) marks else computeCandidates(values, index)
}

data class PencilMarkErrors(
    /** (cellIndex, digit) pairs: candidate digit already placed in a peer */
    val toRemove: List<Pair<Int, Int>>,
    /** (cellIndex, digit) pairs: solution digit missing from pencil marks */
    val toAdd: List<Pair<Int, Int>>,
)
