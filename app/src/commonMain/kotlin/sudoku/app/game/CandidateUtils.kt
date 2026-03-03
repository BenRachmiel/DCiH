package sudoku.app.game

import sudoku.core.model.Board

fun computeCandidates(values: IntArray, index: Int): Set<Int> {
    if (values[index] != 0) return emptySet()
    val used = BooleanArray(10)
    for (buddy in Board.BUDDIES_ARRAY[index]) {
        val v = values[buddy]
        if (v != 0) used[v] = true
    }
    return buildSet { for (d in 1..9) if (!used[d]) add(d) }
}

fun computeAllCandidates(values: IntArray): Array<MutableSet<Int>> {
    return Array(81) { computeCandidates(values, it).toMutableSet() }
}
