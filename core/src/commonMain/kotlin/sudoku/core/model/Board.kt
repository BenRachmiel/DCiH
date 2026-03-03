package sudoku.core.model

/**
 * Core Sudoku board model - port of HoDoKu's Sudoku2.java.
 *
 * Uses bitmask candidate representation and constraint-based tracking
 * with singles queues for efficient solving.
 */
class Board {

    /** Cell values (0=empty, 1-9=set) */
    val values = IntArray(LENGTH)
    /** Candidate bitmasks per cell (9-bit: bit 0=digit 1 .. bit 8=digit 9). 0 when cell is set. */
    val cells = IntArray(LENGTH) { MAX_MASK }
    /** Which cells are givens */
    val fixed = BooleanArray(LENGTH)
    /** Free candidate count per constraint (27) per digit (1-9). free[constraint][digit] */
    val free = Array(ALL_UNITS.size) { IntArray(UNITS + 1) { UNITS } }
    /** The unique solution (for error checking) */
    val solution = IntArray(LENGTH)

    var unsolvedCellsAnz = LENGTH
        private set
    var solutionSet = false

    val nsQueue = SinglesQueue()
    val hsQueue = SinglesQueue()

    init {
        // free[c][0] is unused; free[c][1..9] = 9 initially
    }

    /** Reset to empty board */
    fun clear() {
        for (i in 0 until LENGTH) {
            cells[i] = MAX_MASK
            values[i] = 0
            solution[i] = 0
            fixed[i] = false
        }
        for (i in free.indices) {
            free[i][0] = 0
            for (j in 1..UNITS) free[i][j] = UNITS
        }
        unsolvedCellsAnz = LENGTH
        solutionSet = false
        nsQueue.clear()
        hsQueue.clear()
    }

    /** Deep copy from [src] */
    fun copyFrom(src: Board) {
        src.values.copyInto(values)
        src.cells.copyInto(cells)
        src.fixed.copyInto(fixed)
        src.solution.copyInto(solution)
        for (i in free.indices) src.free[i].copyInto(free[i])
        unsolvedCellsAnz = src.unsolvedCellsAnz
        solutionSet = src.solutionSet
        nsQueue.copyFrom(src.nsQueue)
        hsQueue.copyFrom(src.hsQueue)
    }

    /** Fast copy for backtracking solver - skips fixed/solution/queues */
    fun copyForBacktracking(src: Board) {
        src.values.copyInto(values)
        src.cells.copyInto(cells)
        for (i in free.indices) src.free[i].copyInto(free[i])
        unsolvedCellsAnz = src.unsolvedCellsAnz
        nsQueue.clear()
        hsQueue.clear()
    }

    /** Set or remove a candidate. Returns false if puzzle becomes invalid. */
    fun setCandidate(index: Int, value: Int, set: Boolean): Boolean {
        if (set) {
            if (cells[index] and MASKS[value] == 0) {
                cells[index] = cells[index] or MASKS[value]
                val newAnz = ANZ_VALUES[cells[index]]
                if (newAnz == 1) {
                    nsQueue.addSingle(index, value)
                } else if (newAnz == 2) {
                    nsQueue.deleteNakedSingle(index)
                }
                for (c in CONSTRAINTS[index]) {
                    val newFree = ++free[c][value]
                    if (newFree == 1) {
                        addHiddenSingle(c, value)
                    } else if (newFree == 2) {
                        hsQueue.deleteHiddenSingle(c, value)
                    }
                }
            }
        } else {
            if (cells[index] and MASKS[value] != 0) {
                cells[index] = cells[index] and MASKS[value].inv()
                if (cells[index] == 0) return false // invalid
                if (ANZ_VALUES[cells[index]] == 1) {
                    nsQueue.addSingle(index, CAND_FROM_MASK[cells[index]])
                }
                for (c in CONSTRAINTS[index]) {
                    val newFree = --free[c][value]
                    if (newFree == 1) {
                        addHiddenSingle(c, value)
                    } else if (newFree == 0) {
                        hsQueue.deleteHiddenSingle(c, value)
                    }
                }
            }
        }
        return true
    }

    /** Set a cell value. Returns false if puzzle becomes invalid. */
    fun setCell(index: Int, value: Int, isFixed: Boolean = false): Boolean {
        if (values[index] == value) return true

        values[index] = value
        fixed[index] = isFixed

        if (value != 0) {
            // Save which candidates were in this cell
            val oldCands = POSSIBLE_VALUES[cells[index]]
            cells[index] = 0
            unsolvedCellsAnz--

            var valid = true
            // Remove value from all buddies
            val buddyArr = BUDDIES_ARRAY[index]
            for (buddyIndex in buddyArr) {
                if (!setCandidate(buddyIndex, value, false)) {
                    valid = false
                }
            }

            // Adjust free counts for all candidates that were in this cell
            for (cand in oldCands) {
                for (c in CONSTRAINTS[index]) {
                    val newFree = --free[c][cand]
                    if (newFree == 1 && cand != value) {
                        addHiddenSingle(c, cand)
                    } else if (newFree == 0 && cand != value) {
                        valid = false
                    }
                }
            }
            return valid
        } else {
            // Removing a value (undo) - rebuild from scratch
            rebuildInternalData()
            return true
        }
    }

    /** Fast set for backtracking - no queue management, no validity return */
    fun setCellBS(index: Int, value: Int) {
        values[index] = value
        cells[index] = 0
        val buddyArr = BUDDIES_ARRAY[index]
        for (buddyIndex in buddyArr) {
            cells[buddyIndex] = cells[buddyIndex] and MASKS[value].inv()
        }
    }

    fun isCandidate(index: Int, value: Int): Boolean =
        cells[index] and MASKS[value] != 0

    fun isValidValue(index: Int, value: Int): Boolean {
        for (buddyIndex in BUDDIES_ARRAY[index]) {
            if (values[buddyIndex] == value) return false
        }
        return true
    }

    val isSolved: Boolean get() = unsolvedCellsAnz == 0

    /** Rebuild free counts, queues, and unsolved count from current state */
    fun rebuildInternalData() {
        nsQueue.clear()
        hsQueue.clear()
        for (i in free.indices) for (j in free[i].indices) free[i][j] = 0

        var anz = 0
        for (index in 0 until LENGTH) {
            if (values[index] != 0) {
                cells[index] = 0
            } else {
                anz++
                val cands = POSSIBLE_VALUES[cells[index]]
                for (cand in cands) {
                    for (c in CONSTRAINTS[index]) {
                        free[c][cand]++
                    }
                }
                if (ANZ_VALUES[cells[index]] == 1) {
                    nsQueue.addSingle(index, CAND_FROM_MASK[cells[index]])
                }
            }
        }
        unsolvedCellsAnz = anz

        // Check for hidden singles
        for (constraint in ALL_UNITS.indices) {
            for (value in 1..UNITS) {
                if (free[constraint][value] == 1) {
                    addHiddenSingle(constraint, value)
                }
            }
        }
    }

    /** Rebuild all candidates from values only */
    fun rebuildAllCandidates() {
        for (i in 0 until LENGTH) {
            cells[i] = if (values[i] != 0) 0 else MAX_MASK
        }
        for (i in 0 until LENGTH) {
            if (values[i] != 0) {
                for (buddy in BUDDIES_ARRAY[i]) {
                    cells[buddy] = cells[buddy] and MASKS[values[i]].inv()
                }
            }
        }
        rebuildInternalData()
    }

    private fun addHiddenSingle(constraint: Int, value: Int): Boolean {
        for (cellIndex in ALL_UNITS[constraint]) {
            if (isCandidate(cellIndex, value)) {
                hsQueue.addSingle(cellIndex, value)
                return true
            }
        }
        return false
    }

    /** Load from an 81-char string (digits and dots/zeros) */
    fun loadFromString(s: String) {
        clear()
        for (i in 0 until minOf(s.length, LENGTH)) {
            val ch = s[i]
            val v = if (ch in '1'..'9') ch - '0' else 0
            if (v != 0) {
                setCell(i, v, isFixed = true)
            }
        }
    }

    /** Export as 81-char string */
    fun toStringCompact(): String {
        val sb = StringBuilder(LENGTH)
        for (i in 0 until LENGTH) {
            sb.append(if (values[i] != 0) ('0' + values[i]) else '.')
        }
        return sb.toString()
    }

    companion object {
        const val LENGTH = 81
        const val UNITS = 9

        // Digit bitmasks: MASKS[1]=0x001, MASKS[2]=0x002, ..., MASKS[9]=0x100
        val MASKS = intArrayOf(0, 0x001, 0x002, 0x004, 0x008, 0x010, 0x020, 0x040, 0x080, 0x100)
        const val MAX_MASK = 0x1ff

        /** For each bitmask (0..0x1ff), the array of candidate digits */
        val POSSIBLE_VALUES = Array(0x200) { mask ->
            if (mask == 0) IntArray(0)
            else {
                val temp = IntArray(9)
                var count = 0
                var bit = 1
                for (d in 1..9) {
                    if (mask and bit != 0) temp[count++] = d
                    bit = bit shl 1
                }
                temp.copyOf(count)
            }
        }

        /** Number of candidates for each bitmask */
        val ANZ_VALUES = IntArray(0x200) { mask ->
            var count = 0
            var m = mask
            while (m != 0) { count++; m = m and (m - 1) }
            count
        }

        /** Least-significant set bit -> digit (for single-candidate detection) */
        val CAND_FROM_MASK = IntArray(0x200) { mask ->
            if (mask == 0) 0
            else {
                var d = 1
                var bit = 1
                while (mask and bit == 0) { d++; bit = bit shl 1 }
                d
            }
        }

        /** Row indices */
        val ROWS = Array(9) { r -> IntArray(9) { c -> r * 9 + c } }
        /** Column indices */
        val COLS = Array(9) { c -> IntArray(9) { r -> r * 9 + c } }
        /** Block indices */
        val BLOCKS = Array(9) { b ->
            val br = b / 3 * 3
            val bc = b % 3 * 3
            IntArray(9) { i -> (br + i / 3) * 9 + (bc + i % 3) }
        }
        /** All 27 constraints: rows 0-8, cols 9-17, blocks 18-26 */
        val ALL_UNITS = Array(27) { i ->
            when {
                i < 9 -> ROWS[i]
                i < 18 -> COLS[i - 9]
                else -> BLOCKS[i - 18]
            }
        }

        /** Block index for each cell */
        private val BLOCK_FROM_INDEX = IntArray(81) { i ->
            (i / 9 / 3) * 3 + (i % 9 / 3)
        }

        /** Constraints for each cell: [row, col, block] */
        val CONSTRAINTS = Array(LENGTH) { i ->
            intArrayOf(i / 9, 9 + i % 9, 18 + BLOCK_FROM_INDEX[i])
        }

        /** Buddy cell indices for each cell (pre-computed arrays) */
        val BUDDIES_ARRAY: Array<IntArray> = run {
            val buddySets = Array(LENGTH) { mutableSetOf<Int>() }
            for (i in 0 until LENGTH) {
                for (j in 0 until LENGTH) {
                    if (i != j && (i / 9 == j / 9 || i % 9 == j % 9 ||
                                BLOCK_FROM_INDEX[i] == BLOCK_FROM_INDEX[j])) {
                        buddySets[i].add(j)
                    }
                }
            }
            Array(LENGTH) { i -> buddySets[i].toIntArray() }
        }

        /** CellSet-based buddies for advanced solvers */
        val BUDDIES: Array<CellSet> = Array(LENGTH) { i ->
            CellSet().apply {
                for (buddy in BUDDIES_ARRAY[i]) add(buddy)
            }
        }

        /** CellSet for each constraint */
        val ALL_CONSTRAINTS_SETS: Array<CellSet> = Array(27) { i ->
            CellSet().apply {
                for (cell in ALL_UNITS[i]) add(cell)
            }
        }

        fun getRow(index: Int) = index / 9
        fun getCol(index: Int) = index % 9
        fun getBlock(index: Int) = BLOCK_FROM_INDEX[index]
        fun getIndex(row: Int, col: Int) = row * 9 + col
    }
}

/**
 * Process all queued naked and hidden singles until both queues are empty.
 * Returns false if the puzzle becomes invalid.
 */
fun Board.setAllExposedSingles(): Boolean {
    var valid = true
    do {
        var singleIndex: Int
        // Process naked singles first
        while (valid) {
            singleIndex = nsQueue.getSingle()
            if (singleIndex == -1) break
            val index = nsQueue.getIndex(singleIndex)
            val value = nsQueue.getValue(singleIndex)
            if (cells[index] and Board.MASKS[value] != 0) {
                valid = setCell(index, value)
            }
        }
        // Then hidden singles
        while (valid) {
            singleIndex = hsQueue.getSingle()
            if (singleIndex == -1) break
            val index = hsQueue.getIndex(singleIndex)
            val value = hsQueue.getValue(singleIndex)
            if (cells[index] and Board.MASKS[value] != 0) {
                valid = setCell(index, value)
            }
        }
    } while (valid && !(nsQueue.isEmpty && hsQueue.isEmpty))
    return valid
}
