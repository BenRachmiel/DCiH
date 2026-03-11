package sudoku.app.engine

/**
 * Precomputed peer table: for each cell (0-80), the indices of all cells
 * that share a row, column, or block (20 peers per cell).
 * Replaces Board.BUDDIES_ARRAY to avoid dependency on :core.
 */
object Buddies {
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
