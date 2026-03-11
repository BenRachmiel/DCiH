package sudoku.app.engine

/**
 * Precomputed peer table: for each cell (0-80), the indices of all cells
 * that share a row, column, or block (20 peers per cell).
 * Precomputed from Sudoku constraints (same cell excluded).
 */
object Buddies {
    /** All 27 units: rows 0-8, cols 9-17, blocks 18-26. */
    val ALL_UNITS: Array<IntArray> = Array(27) { i ->
        when {
            i < 9 -> IntArray(9) { c -> i * 9 + c }
            i < 18 -> IntArray(9) { r -> r * 9 + (i - 9) }
            else -> {
                val b = i - 18
                val br = (b / 3) * 3
                val bc = (b % 3) * 3
                IntArray(9) { j -> (br + j / 3) * 9 + (bc + j % 3) }
            }
        }
    }

    val ARRAY: Array<IntArray> = Array(81) { idx ->
        val row = idx / 9
        val col = idx % 9
        val blockRow = (row / 3) * 3
        val blockCol = (col / 3) * 3
        val peers = mutableSetOf<Int>()
        for (c in 0 until 9) peers.add(row * 9 + c)
        for (r in 0 until 9) peers.add(r * 9 + col)
        for (r in blockRow until blockRow + 3)
            for (c in blockCol until blockCol + 3)
                peers.add(r * 9 + c)
        peers.remove(idx)
        peers.toIntArray().also { it.sort() }
    }
}
