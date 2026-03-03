package sudoku.core.solver

import sudoku.core.model.*

/**
 * XY-Wing, XYZ-Wing, and W-Wing patterns.
 */
class WingSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        findXYWing(board)?.let { return listOf(it) }
        findXYZWing(board)?.let { return listOf(it) }
        findWWing(board)?.let { return listOf(it) }
        return emptyList()
    }

    /**
     * XY-Wing: Three bivalue cells (pivot + 2 pincers).
     * Pivot has candidates {a,b}, pincers have {a,c} and {b,c}.
     * Pivot sees both pincers. Eliminate c from cells that see both pincers.
     */
    private fun findXYWing(board: Board): SolutionStep? {
        for (pivot in 0 until Board.LENGTH) {
            if (board.values[pivot] != 0 || Board.ANZ_VALUES[board.cells[pivot]] != 2) continue
            val pivotCands = Board.POSSIBLE_VALUES[board.cells[pivot]]
            val a = pivotCands[0]
            val b = pivotCands[1]

            val buddies = Board.BUDDIES_ARRAY[pivot]
            // Find pincers
            val pincersA = mutableListOf<Int>() // have a + some other digit c
            val pincersB = mutableListOf<Int>() // have b + some other digit c

            for (buddy in buddies) {
                if (board.values[buddy] != 0 || Board.ANZ_VALUES[board.cells[buddy]] != 2) continue
                val cands = board.cells[buddy]
                if (cands and Board.MASKS[a] != 0 && cands and Board.MASKS[b] == 0) pincersA.add(buddy)
                if (cands and Board.MASKS[b] != 0 && cands and Board.MASKS[a] == 0) pincersB.add(buddy)
            }

            for (pA in pincersA) {
                val candsA = Board.POSSIBLE_VALUES[board.cells[pA]]
                val c = if (candsA[0] == a) candsA[1] else candsA[0]

                for (pB in pincersB) {
                    if (pB == pA) continue
                    val candsB = Board.POSSIBLE_VALUES[board.cells[pB]]
                    val cB = if (candsB[0] == b) candsB[1] else candsB[0]
                    if (c != cB) continue

                    // Found XY-Wing! Eliminate c from cells that see both pincers
                    val eliminations = mutableListOf<Pair<Int, Int>>()
                    for (cell in 0 until Board.LENGTH) {
                        if (cell == pivot || cell == pA || cell == pB) continue
                        if (board.isCandidate(cell, c) &&
                            cell in Board.BUDDIES_ARRAY[pA] &&
                            cell in Board.BUDDIES_ARRAY[pB]
                        ) {
                            eliminations.add(cell to c)
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        return SolutionStep(
                            type = SolutionType.XY_WING,
                            value = c,
                            indices = listOf(pivot, pA, pB),
                            candidatesRemoved = eliminations
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * XYZ-Wing: Pivot has {a,b,c}, pincers have {a,c} and {b,c}.
     * Eliminate c from cells that see all three.
     */
    private fun findXYZWing(board: Board): SolutionStep? {
        for (pivot in 0 until Board.LENGTH) {
            if (board.values[pivot] != 0 || Board.ANZ_VALUES[board.cells[pivot]] != 3) continue
            val pivotCands = Board.POSSIBLE_VALUES[board.cells[pivot]]

            val buddies = Board.BUDDIES_ARRAY[pivot]
            // Find bivalue buddies that share exactly 2 candidates with pivot
            val bivalueBuddies = mutableListOf<Int>()
            for (buddy in buddies) {
                if (board.values[buddy] != 0 || Board.ANZ_VALUES[board.cells[buddy]] != 2) continue
                val shared = board.cells[buddy] and board.cells[pivot]
                if (Board.ANZ_VALUES[shared] == 2) bivalueBuddies.add(buddy)
            }

            // Try all pairs of bivalue buddies
            for (i in bivalueBuddies.indices) {
                for (j in i + 1 until bivalueBuddies.size) {
                    val p1 = bivalueBuddies[i]
                    val p2 = bivalueBuddies[j]
                    // Combined candidates of p1 and p2 must equal pivot's candidates
                    if ((board.cells[p1] or board.cells[p2]) != board.cells[pivot]) continue
                    // The shared candidate between p1 and p2 is c
                    val shared = board.cells[p1] and board.cells[p2]
                    if (Board.ANZ_VALUES[shared] != 1) continue
                    val c = Board.CAND_FROM_MASK[shared]

                    val eliminations = mutableListOf<Pair<Int, Int>>()
                    for (cell in 0 until Board.LENGTH) {
                        if (cell == pivot || cell == p1 || cell == p2) continue
                        if (board.isCandidate(cell, c) &&
                            cell in Board.BUDDIES_ARRAY[pivot] &&
                            cell in Board.BUDDIES_ARRAY[p1] &&
                            cell in Board.BUDDIES_ARRAY[p2]
                        ) {
                            eliminations.add(cell to c)
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        return SolutionStep(
                            type = SolutionType.XYZ_WING,
                            value = c,
                            indices = listOf(pivot, p1, p2),
                            candidatesRemoved = eliminations
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * W-Wing: Two bivalue cells with same candidates {a,b}, connected by
     * a strong link on one of the candidates.
     */
    private fun findWWing(board: Board): SolutionStep? {
        // Collect bivalue cells
        val bivalues = mutableListOf<Int>()
        for (i in 0 until Board.LENGTH) {
            if (board.values[i] == 0 && Board.ANZ_VALUES[board.cells[i]] == 2) {
                bivalues.add(i)
            }
        }

        for (i in bivalues.indices) {
            for (j in i + 1 until bivalues.size) {
                val c1 = bivalues[i]
                val c2 = bivalues[j]
                if (board.cells[c1] != board.cells[c2]) continue
                // Same candidates {a, b}
                if (c1 in Board.BUDDIES_ARRAY[c2]) continue // would be a naked pair

                val cands = Board.POSSIBLE_VALUES[board.cells[c1]]
                val a = cands[0]
                val b = cands[1]

                // Check strong link on 'a': some unit where c1 and c2 each have a buddy
                // with a strong link on 'a' (only 2 candidates for 'a' in that unit)
                for (digit in intArrayOf(a, b)) {
                    val otherDigit = if (digit == a) b else a
                    // Find strong links for 'digit'
                    for (unit in Board.ALL_UNITS) {
                        var count = 0
                        var link1 = -1
                        var link2 = -1
                        for (cell in unit) {
                            if (board.isCandidate(cell, digit)) {
                                count++
                                if (link1 == -1) link1 = cell else link2 = cell
                            }
                        }
                        if (count != 2) continue
                        // Check if link1 sees c1 and link2 sees c2 (or vice versa)
                        val case1 = link1 in Board.BUDDIES_ARRAY[c1] && link2 in Board.BUDDIES_ARRAY[c2]
                        val case2 = link1 in Board.BUDDIES_ARRAY[c2] && link2 in Board.BUDDIES_ARRAY[c1]
                        if (!case1 && !case2) continue

                        // Eliminate otherDigit from cells that see both c1 and c2
                        val eliminations = mutableListOf<Pair<Int, Int>>()
                        for (cell in 0 until Board.LENGTH) {
                            if (cell == c1 || cell == c2) continue
                            if (board.isCandidate(cell, otherDigit) &&
                                cell in Board.BUDDIES_ARRAY[c1] &&
                                cell in Board.BUDDIES_ARRAY[c2]
                            ) {
                                eliminations.add(cell to otherDigit)
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SolutionStep(
                                type = SolutionType.W_WING,
                                value = otherDigit,
                                indices = listOf(c1, c2, link1, link2),
                                candidatesRemoved = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private operator fun IntArray.contains(value: Int): Boolean {
        for (v in this) if (v == value) return true
        return false
    }
}
