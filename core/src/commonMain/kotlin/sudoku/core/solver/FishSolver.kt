package sudoku.core.solver

import sudoku.core.model.*

/**
 * Basic fish patterns: X-Wing (size 2), Swordfish (size 3), Jellyfish (size 4).
 * Unfinned only.
 */
class FishSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        for (size in 2..4) {
            findFish(board, size, useRows = true)?.let { return listOf(it) }
            findFish(board, size, useRows = false)?.let { return listOf(it) }
        }
        return emptyList()
    }

    private fun findFish(board: Board, size: Int, useRows: Boolean): SolutionStep? {
        val type = when (size) {
            2 -> SolutionType.X_WING
            3 -> SolutionType.SWORDFISH
            else -> SolutionType.JELLYFISH
        }
        val baseLines = if (useRows) Board.ROWS else Board.COLS
        val coverLines = if (useRows) Board.COLS else Board.ROWS

        for (digit in 1..9) {
            // For each digit, find which base lines have 2..size positions
            val candidateLines = mutableListOf<Int>()
            val linePositions = Array(9) { mutableListOf<Int>() }

            for (lineIdx in 0 until 9) {
                val line = baseLines[lineIdx]
                val positions = mutableListOf<Int>()
                for (cell in line) {
                    if (board.isCandidate(cell, digit)) {
                        val coverIdx = if (useRows) cell % 9 else cell / 9
                        positions.add(coverIdx)
                    }
                }
                if (positions.size in 2..size) {
                    linePositions[lineIdx] = positions
                    candidateLines.add(lineIdx)
                }
            }

            if (candidateLines.size < size) continue

            // Try all combinations of 'size' base lines
            val combo = IntArray(size)
            val step = findFishCombo(
                board, candidateLines, combo, 0, 0, size, digit,
                linePositions, baseLines, coverLines, type, useRows
            )
            if (step != null) return step
        }
        return null
    }

    private fun findFishCombo(
        board: Board, lines: List<Int>, combo: IntArray,
        start: Int, depth: Int, size: Int, digit: Int,
        linePositions: Array<MutableList<Int>>,
        baseLines: Array<IntArray>, coverLines: Array<IntArray>,
        type: SolutionType, useRows: Boolean
    ): SolutionStep? {
        if (depth == size) {
            // Collect all cover positions
            val coverSet = mutableSetOf<Int>()
            for (i in 0 until size) {
                coverSet.addAll(linePositions[combo[i]])
            }
            if (coverSet.size != size) return null

            // Find eliminations in cover lines
            val eliminations = mutableListOf<Pair<Int, Int>>()
            val fishCells = mutableListOf<Int>()

            for (coverIdx in coverSet) {
                for (cell in coverLines[coverIdx]) {
                    if (board.isCandidate(cell, digit)) {
                        val baseIdx = if (useRows) cell / 9 else cell % 9
                        if (baseIdx in combo.take(size)) {
                            fishCells.add(cell)
                        } else {
                            eliminations.add(cell to digit)
                        }
                    }
                }
            }

            if (eliminations.isNotEmpty()) {
                return SolutionStep(
                    type = type,
                    value = digit,
                    indices = fishCells,
                    candidatesRemoved = eliminations
                )
            }
            return null
        }
        for (i in start until lines.size) {
            combo[depth] = lines[i]
            val result = findFishCombo(
                board, lines, combo, i + 1, depth + 1, size, digit,
                linePositions, baseLines, coverLines, type, useRows
            )
            if (result != null) return result
        }
        return null
    }
}
