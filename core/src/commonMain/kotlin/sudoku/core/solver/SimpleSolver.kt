package sudoku.core.solver

import sudoku.core.model.*

/**
 * Handles singles, locked candidates, and naked/hidden pairs/triples/quads.
 */
class SimpleSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        // Try techniques in order of difficulty
        findFullHouse(board)?.let { return listOf(it) }
        findNakedSingle(board)?.let { return listOf(it) }
        findHiddenSingle(board)?.let { return listOf(it) }
        findLockedCandidates(board)?.let { return listOf(it) }
        findNakedSubset(board, 2)?.let { return listOf(it) }
        findHiddenSubset(board, 2)?.let { return listOf(it) }
        findNakedSubset(board, 3)?.let { return listOf(it) }
        findHiddenSubset(board, 3)?.let { return listOf(it) }
        findNakedSubset(board, 4)?.let { return listOf(it) }
        findHiddenSubset(board, 4)?.let { return listOf(it) }
        return emptyList()
    }

    /** Full House: a unit with exactly one empty cell */
    private fun findFullHouse(board: Board): SolutionStep? {
        for (unit in Board.ALL_UNITS) {
            var emptyIndex = -1
            var emptyCount = 0
            var missingDigit = 0
            val digitMask = IntArray(10)
            for (cell in unit) {
                if (board.values[cell] == 0) {
                    emptyCount++
                    emptyIndex = cell
                } else {
                    digitMask[board.values[cell]] = 1
                }
            }
            if (emptyCount == 1) {
                for (d in 1..9) if (digitMask[d] == 0) { missingDigit = d; break }
                return SolutionStep(
                    type = SolutionType.FULL_HOUSE,
                    cellIndex = emptyIndex,
                    value = missingDigit
                )
            }
        }
        return null
    }

    /** Naked Single: cell with exactly one candidate */
    private fun findNakedSingle(board: Board): SolutionStep? {
        for (i in 0 until Board.LENGTH) {
            if (board.cells[i] != 0 && Board.ANZ_VALUES[board.cells[i]] == 1) {
                val value = Board.CAND_FROM_MASK[board.cells[i]]
                return SolutionStep(
                    type = SolutionType.NAKED_SINGLE,
                    cellIndex = i,
                    value = value
                )
            }
        }
        return null
    }

    /** Hidden Single: candidate appears only once in a unit */
    private fun findHiddenSingle(board: Board): SolutionStep? {
        for (constraintIdx in Board.ALL_UNITS.indices) {
            val unit = Board.ALL_UNITS[constraintIdx]
            for (digit in 1..9) {
                var count = 0
                var lastIndex = -1
                for (cell in unit) {
                    if (board.isCandidate(cell, digit)) {
                        count++
                        lastIndex = cell
                    }
                }
                if (count == 1 && board.values[lastIndex] == 0) {
                    return SolutionStep(
                        type = SolutionType.HIDDEN_SINGLE,
                        cellIndex = lastIndex,
                        value = digit
                    )
                }
            }
        }
        return null
    }

    /**
     * Locked Candidates:
     * Type 1 (Pointing): candidate in a block confined to one row/col -> eliminate from rest of that row/col
     * Type 2 (Claiming): candidate in a row/col confined to one block -> eliminate from rest of that block
     */
    private fun findLockedCandidates(board: Board): SolutionStep? {
        // Type 1: Pointing
        for (block in 0 until 9) {
            val blockCells = Board.BLOCKS[block]
            for (digit in 1..9) {
                val positions = mutableListOf<Int>()
                for (cell in blockCells) {
                    if (board.isCandidate(cell, digit)) positions.add(cell)
                }
                if (positions.size < 2) continue

                // All in same row?
                val row = positions[0] / 9
                if (positions.all { it / 9 == row }) {
                    val eliminations = mutableListOf<Pair<Int, Int>>()
                    for (cell in Board.ROWS[row]) {
                        if (cell !in positions && board.isCandidate(cell, digit)) {
                            eliminations.add(cell to digit)
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        return SolutionStep(
                            type = SolutionType.LOCKED_CANDIDATES_1,
                            indices = positions,
                            value = digit,
                            candidatesRemoved = eliminations
                        )
                    }
                }
                // All in same col?
                val col = positions[0] % 9
                if (positions.all { it % 9 == col }) {
                    val eliminations = mutableListOf<Pair<Int, Int>>()
                    for (cell in Board.COLS[col]) {
                        if (cell !in positions && board.isCandidate(cell, digit)) {
                            eliminations.add(cell to digit)
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        return SolutionStep(
                            type = SolutionType.LOCKED_CANDIDATES_1,
                            indices = positions,
                            value = digit,
                            candidatesRemoved = eliminations
                        )
                    }
                }
            }
        }

        // Type 2: Claiming
        for (lineType in 0..1) { // 0=rows, 1=cols
            val lines = if (lineType == 0) Board.ROWS else Board.COLS
            for (line in lines) {
                for (digit in 1..9) {
                    val positions = mutableListOf<Int>()
                    for (cell in line) {
                        if (board.isCandidate(cell, digit)) positions.add(cell)
                    }
                    if (positions.size < 2) continue

                    val block = Board.getBlock(positions[0])
                    if (positions.all { Board.getBlock(it) == block }) {
                        val eliminations = mutableListOf<Pair<Int, Int>>()
                        for (cell in Board.BLOCKS[block]) {
                            if (cell !in positions && board.isCandidate(cell, digit)) {
                                eliminations.add(cell to digit)
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SolutionStep(
                                type = SolutionType.LOCKED_CANDIDATES_2,
                                indices = positions,
                                value = digit,
                                candidatesRemoved = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    /** Naked Pair/Triple/Quad: N cells in a unit with exactly N candidates combined */
    private fun findNakedSubset(board: Board, size: Int): SolutionStep? {
        val type = when (size) {
            2 -> SolutionType.NAKED_PAIR
            3 -> SolutionType.NAKED_TRIPLE
            else -> SolutionType.NAKED_QUADRUPLE
        }

        for (unit in Board.ALL_UNITS) {
            // Get unsolved cells in this unit
            val unsolved = mutableListOf<Int>()
            for (cell in unit) {
                if (board.cells[cell] != 0) unsolved.add(cell)
            }
            if (unsolved.size < size) continue

            // Try all combinations of 'size' cells
            val combo = IntArray(size)
            if (findNakedSubsetCombo(board, unsolved, combo, 0, 0, size, unit, type)) {
                // combo is filled, reconstruct the step
                var combinedMask = 0
                for (idx in combo) combinedMask = combinedMask or board.cells[idx]
                if (Board.ANZ_VALUES[combinedMask] != size) continue

                val eliminations = mutableListOf<Pair<Int, Int>>()
                for (cell in unit) {
                    if (board.cells[cell] != 0 && cell !in combo.take(size)) {
                        for (digit in Board.POSSIBLE_VALUES[combinedMask]) {
                            if (board.isCandidate(cell, digit)) {
                                eliminations.add(cell to digit)
                            }
                        }
                    }
                }
                if (eliminations.isNotEmpty()) {
                    return SolutionStep(
                        type = type,
                        indices = combo.toList(),
                        candidatesRemoved = eliminations
                    )
                }
            }
        }
        return null
    }

    private fun findNakedSubsetCombo(
        board: Board, unsolved: List<Int>, combo: IntArray,
        start: Int, depth: Int, size: Int, unit: IntArray, type: SolutionType
    ): Boolean {
        if (depth == size) {
            var combinedMask = 0
            for (i in 0 until size) combinedMask = combinedMask or board.cells[combo[i]]
            if (Board.ANZ_VALUES[combinedMask] != size) return false
            // Check eliminations exist
            for (cell in unit) {
                if (board.cells[cell] != 0 && cell !in combo.take(size)) {
                    for (digit in Board.POSSIBLE_VALUES[combinedMask]) {
                        if (board.isCandidate(cell, digit)) return true
                    }
                }
            }
            return false
        }
        for (i in start until unsolved.size) {
            combo[depth] = unsolved[i]
            if (findNakedSubsetCombo(board, unsolved, combo, i + 1, depth + 1, size, unit, type)) {
                return true
            }
        }
        return false
    }

    /** Hidden Pair/Triple/Quad: N candidates in a unit confined to N cells */
    private fun findHiddenSubset(board: Board, size: Int): SolutionStep? {
        val type = when (size) {
            2 -> SolutionType.HIDDEN_PAIR
            3 -> SolutionType.HIDDEN_TRIPLE
            else -> SolutionType.HIDDEN_QUADRUPLE
        }

        for (unit in Board.ALL_UNITS) {
            // Find which digits still need placing
            val availableDigits = mutableListOf<Int>()
            for (digit in 1..9) {
                var count = 0
                for (cell in unit) {
                    if (board.isCandidate(cell, digit)) count++
                }
                if (count in 2..size) availableDigits.add(digit)
            }
            if (availableDigits.size < size) continue

            val digitCombo = IntArray(size)
            if (findHiddenSubsetCombo(board, availableDigits, digitCombo, 0, 0, size, unit, type)) {
                // Found it - build the step
                val cellsInSubset = mutableListOf<Int>()
                for (cell in unit) {
                    for (i in 0 until size) {
                        if (board.isCandidate(cell, digitCombo[i])) {
                            if (cell !in cellsInSubset) cellsInSubset.add(cell)
                            break
                        }
                    }
                }
                val eliminations = mutableListOf<Pair<Int, Int>>()
                for (cell in cellsInSubset) {
                    for (digit in 1..9) {
                        if (digit !in digitCombo.take(size) && board.isCandidate(cell, digit)) {
                            eliminations.add(cell to digit)
                        }
                    }
                }
                if (eliminations.isNotEmpty()) {
                    return SolutionStep(
                        type = type,
                        indices = cellsInSubset,
                        candidatesRemoved = eliminations
                    )
                }
            }
        }
        return null
    }

    private fun findHiddenSubsetCombo(
        board: Board, digits: List<Int>, combo: IntArray,
        start: Int, depth: Int, size: Int, unit: IntArray, type: SolutionType
    ): Boolean {
        if (depth == size) {
            // Check: these digits must all appear in exactly 'size' cells
            val cellSet = mutableSetOf<Int>()
            for (i in 0 until size) {
                for (cell in unit) {
                    if (board.isCandidate(cell, combo[i])) cellSet.add(cell)
                }
            }
            if (cellSet.size != size) return false
            // Check there are eliminations
            for (cell in cellSet) {
                for (digit in 1..9) {
                    if (digit !in combo.take(size) && board.isCandidate(cell, digit)) return true
                }
            }
            return false
        }
        for (i in start until digits.size) {
            combo[depth] = digits[i]
            if (findHiddenSubsetCombo(board, digits, combo, i + 1, depth + 1, size, unit, type)) {
                return true
            }
        }
        return false
    }
}
