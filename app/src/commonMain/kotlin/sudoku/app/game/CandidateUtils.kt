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

fun Array<out Set<Int>>.copyMutable(): Array<MutableSet<Int>> =
    Array(size) { this[it].toMutableSet() }

fun Array<out Set<Int>>.copyImmutable(): Array<Set<Int>> =
    Array(size) { this[it].toSet() }

data class PencilMarkErrors(
    /** (cellIndex, digit) pairs: candidate digit already placed in a peer */
    val toRemove: List<Pair<Int, Int>>,
    /** (cellIndex, digit) pairs: solution digit missing from pencil marks */
    val toAdd: List<Pair<Int, Int>>,
)
