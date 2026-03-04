package sudoku.app.game

import sudoku.core.model.Board

fun computeCandidates(
    values: IntArray,
    index: Int,
): Set<Int> {
    if (values[index] != 0) return emptySet()
    val used = BooleanArray(10)
    for (buddy in Board.BUDDIES_ARRAY[index]) {
        val v = values[buddy]
        if (v != 0) used[v] = true
    }
    return buildSet { for (d in 1..9) if (!used[d]) add(d) }
}

fun computeAllCandidates(values: IntArray): Array<MutableSet<Int>> = Array(81) { computeCandidates(values, it).toMutableSet() }

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

/** Finds all pencil mark errors: impossible candidates and missing solution digits. Returns null if no errors. */
fun findPencilMarkErrors(state: GameState): PencilMarkErrors? {
    val toRemove = mutableListOf<Pair<Int, Int>>()
    val toAdd = mutableListOf<Pair<Int, Int>>()

    for (i in 0 until 81) {
        if (state.values[i] != 0 || state.pencilMarks[i].isEmpty()) continue

        // Check for impossible candidates (digit placed in a peer)
        val valid = computeCandidates(state.values, i)
        for (d in state.pencilMarks[i]) {
            if (d !in valid) toRemove.add(i to d)
        }

        // Check for missing solution digit
        if (state.solution[i] != 0 && state.solution[i] !in state.pencilMarks[i]) {
            toAdd.add(i to state.solution[i])
        }
    }

    return if (toRemove.isEmpty() && toAdd.isEmpty()) null else PencilMarkErrors(toRemove, toAdd)
}
