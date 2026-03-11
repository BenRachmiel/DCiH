package sudoku.core.solver

import sudoku.core.model.*

/**
 * Detects single-digit patterns based on chains of two strong links:
 * - Skyscraper: both links in same line type (row-row or col-col)
 * - Two-String Kite: one link in a row, one in a column, connected through a box
 * - Turbot Fish: general case involving at least one box-based strong link
 * - Empty Rectangle: box with L-shaped candidates + external conjugate pair
 */
class SingleDigitPatternSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        for (digit in 1..9) {
            val links = findStrongLinks(board, digit)
            findEmptyRectangle(board, digit, links)?.let { return listOf(it) }
            findTwoLinkPattern(board, digit, links)?.let { return listOf(it) }
        }
        return emptyList()
    }

    private data class StrongLink(val cell1: Int, val cell2: Int, val unit: Int)

    private fun findStrongLinks(board: Board, digit: Int): List<StrongLink> {
        val links = mutableListOf<StrongLink>()
        for (unitIdx in Board.ALL_UNITS.indices) {
            val unit = Board.ALL_UNITS[unitIdx]
            val c1 = findTwoCandidates(board, unit, digit) ?: continue
            links.add(StrongLink(c1.first, c1.second, unitIdx))
        }
        return links
    }

    /** Returns the two cells with candidate [digit] in [unit], or null if count != 2. */
    private fun findTwoCandidates(board: Board, unit: IntArray, digit: Int): Pair<Int, Int>? {
        var first = -1
        var second = -1
        for (cell in unit) {
            if (board.isCandidate(cell, digit)) {
                when {
                    first == -1 -> first = cell
                    second == -1 -> second = cell
                    else -> return null // more than 2
                }
            }
        }
        return if (second != -1) first to second else null
    }

    /**
     * Finds Skyscraper, Two-String Kite, or Turbot Fish patterns by searching
     * for pairs of strong links connected by a weak link (shared visibility).
     */
    private fun findTwoLinkPattern(board: Board, digit: Int, links: List<StrongLink>): SolutionStep? {
        for (i in links.indices) {
            for (j in i + 1 until links.size) {
                val sl1 = links[i]
                val sl2 = links[j]

                // Try all 4 endpoint orientations
                val endpoints = arrayOf(
                    intArrayOf(sl1.cell1, sl1.cell2, sl2.cell1, sl2.cell2),
                    intArrayOf(sl1.cell1, sl1.cell2, sl2.cell2, sl2.cell1),
                    intArrayOf(sl1.cell2, sl1.cell1, sl2.cell1, sl2.cell2),
                    intArrayOf(sl1.cell2, sl1.cell1, sl2.cell2, sl2.cell1),
                )

                for (ep in endpoints) {
                    val start = ep[0]    // elimination endpoint 1
                    val end = ep[1]      // weak link cell from link 1
                    val conn = ep[2]     // weak link cell from link 2
                    val far = ep[3]      // elimination endpoint 2

                    // Need 4 distinct cells
                    if (start == far || start == conn || end == far) continue

                    // end and conn must see each other (weak link)
                    if (!isBuddy(end, conn)) continue

                    // Chain: start --strong-- end ==weak== conn --strong-- far
                    // Either start or far has digit. Eliminate from cells seeing both.
                    val elims = findCommonEliminations(board, digit, start, far)
                    if (elims.isNotEmpty()) {
                        val type = classifyPattern(sl1.unit, sl2.unit)
                        return SolutionStep(
                            type = type,
                            value = digit,
                            indices = listOf(start, end, conn, far),
                            candidatesRemoved = elims,
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * Empty Rectangle: a box where all candidates for a digit lie in one row and one column.
     * Combined with an external strong link, creates an elimination.
     */
    private fun findEmptyRectangle(board: Board, digit: Int, links: List<StrongLink>): SolutionStep? {
        for (box in 0..8) {
            val boxStartRow = (box / 3) * 3
            val boxStartCol = (box % 3) * 3

            val boxCells = mutableListOf<Int>()
            for (r in boxStartRow until boxStartRow + 3) {
                for (c in boxStartCol until boxStartCol + 3) {
                    val idx = r * 9 + c
                    if (board.isCandidate(idx, digit)) boxCells.add(idx)
                }
            }
            if (boxCells.size < 2) continue

            // Try each row/col intersection as the ER pivot
            for (erRow in boxStartRow until boxStartRow + 3) {
                for (erCol in boxStartCol until boxStartCol + 3) {
                    if (!boxCells.all { it / 9 == erRow || it % 9 == erCol }) continue

                    // Need candidates in both arms (not just a line = locked candidate)
                    val hasRowArm = boxCells.any { it / 9 == erRow && it % 9 != erCol }
                    val hasColArm = boxCells.any { it % 9 == erCol && it / 9 != erRow }
                    if (!hasRowArm || !hasColArm) continue

                    // Case 1: Strong link in a ROW, one end in column erCol outside box
                    for (sl in links) {
                        if (sl.unit >= 9) continue // rows only (unit 0-8)
                        val slRow = sl.unit
                        if (slRow in boxStartRow until boxStartRow + 3) continue

                        val result = tryErWithRowLink(board, digit, sl, erRow, erCol, boxStartRow, boxStartCol, boxCells)
                        if (result != null) return result
                    }

                    // Case 2: Strong link in a COLUMN, one end in row erRow outside box
                    for (sl in links) {
                        if (sl.unit < 9 || sl.unit >= 18) continue // columns only (unit 9-17)
                        val slCol = sl.unit - 9
                        if (slCol in boxStartCol until boxStartCol + 3) continue

                        val result = tryErWithColLink(board, digit, sl, erRow, erCol, boxStartRow, boxStartCol, boxCells)
                        if (result != null) return result
                    }
                }
            }
        }
        return null
    }

    /**
     * ER + row strong link: one end in column erCol eliminates at (erRow, other end's column).
     */
    private fun tryErWithRowLink(
        board: Board, digit: Int, sl: StrongLink,
        erRow: Int, erCol: Int, boxStartRow: Int, boxStartCol: Int,
        erCells: List<Int>,
    ): SolutionStep? {
        for ((inColCell, otherCell) in arrayOf(sl.cell1 to sl.cell2, sl.cell2 to sl.cell1)) {
            if (inColCell % 9 != erCol) continue

            val targetIdx = erRow * 9 + otherCell % 9
            if (targetIdx == otherCell) continue
            if (!board.isCandidate(targetIdx, digit)) continue
            // Target must not be inside the ER box
            val tr = targetIdx / 9
            val tc = targetIdx % 9
            if (tr in boxStartRow until boxStartRow + 3 &&
                tc in boxStartCol until boxStartCol + 3
            ) continue

            return SolutionStep(
                type = SolutionType.EMPTY_RECTANGLE,
                value = digit,
                indices = listOf(otherCell, inColCell) + erCells,
                candidatesRemoved = listOf(targetIdx to digit),
            )
        }
        return null
    }

    /**
     * ER + column strong link: one end in row erRow eliminates at (other end's row, erCol).
     */
    private fun tryErWithColLink(
        board: Board, digit: Int, sl: StrongLink,
        erRow: Int, erCol: Int, boxStartRow: Int, boxStartCol: Int,
        erCells: List<Int>,
    ): SolutionStep? {
        for ((inRowCell, otherCell) in arrayOf(sl.cell1 to sl.cell2, sl.cell2 to sl.cell1)) {
            if (inRowCell / 9 != erRow) continue

            val targetIdx = otherCell / 9 * 9 + erCol
            if (targetIdx == otherCell) continue
            if (!board.isCandidate(targetIdx, digit)) continue
            val tr = targetIdx / 9
            val tc = targetIdx % 9
            if (tr in boxStartRow until boxStartRow + 3 &&
                tc in boxStartCol until boxStartCol + 3
            ) continue

            return SolutionStep(
                type = SolutionType.EMPTY_RECTANGLE,
                value = digit,
                indices = listOf(otherCell, inRowCell) + erCells,
                candidatesRemoved = listOf(targetIdx to digit),
            )
        }
        return null
    }

    /** Cells that see both [a] and [b] and have [digit] as candidate (excluding a and b). */
    private fun findCommonEliminations(board: Board, digit: Int, a: Int, b: Int): List<Pair<Int, Int>> {
        val elims = mutableListOf<Pair<Int, Int>>()
        for (cell in Board.BUDDIES_ARRAY[a]) {
            if (cell == b) continue
            if (!board.isCandidate(cell, digit)) continue
            if (isBuddy(cell, b)) {
                elims.add(cell to digit)
            }
        }
        return elims
    }

    private fun classifyPattern(unit1: Int, unit2: Int): SolutionType {
        val t1 = unitType(unit1)
        val t2 = unitType(unit2)
        return when {
            t1 == t2 && t1 != BOX -> SolutionType.SKYSCRAPER
            t1 != BOX && t2 != BOX -> SolutionType.TWO_STRING_KITE
            else -> SolutionType.TURBOT_FISH
        }
    }

    private fun isBuddy(a: Int, b: Int): Boolean {
        for (v in Board.BUDDIES_ARRAY[a]) {
            if (v == b) return true
        }
        return false
    }

    companion object {
        private const val BOX = 2
    }
}

private fun unitType(unit: Int): Int = when {
    unit < 9 -> 0   // ROW
    unit < 18 -> 1  // COL
    else -> 2        // BOX
}
