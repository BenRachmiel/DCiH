package sudoku.core.solver

import sudoku.core.model.*

/**
 * Simple Coloring (single-digit coloring) and Multi Coloring.
 */
class ColoringSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        findSimpleColoring(board)?.let { return listOf(it) }
        return emptyList()
    }

    /**
     * Simple Coloring: Build conjugate chains for each digit.
     * Two eliminations:
     * - Wrap: same color sees itself -> that color is false, eliminate all cells of that color
     * - Trap: uncolored cell sees both colors -> eliminate candidate from that cell
     */
    private fun findSimpleColoring(board: Board): SolutionStep? {
        for (digit in 1..9) {
            // Build conjugate pairs (strong links)
            val strongLinks = mutableMapOf<Int, MutableList<Int>>()
            for (unit in Board.ALL_UNITS) {
                val cells = mutableListOf<Int>()
                for (cell in unit) {
                    if (board.isCandidate(cell, digit)) cells.add(cell)
                }
                if (cells.size == 2) {
                    strongLinks.getOrPut(cells[0]) { mutableListOf() }.add(cells[1])
                    strongLinks.getOrPut(cells[1]) { mutableListOf() }.add(cells[0])
                }
            }

            if (strongLinks.isEmpty()) continue

            // Color connected components
            val color = IntArray(Board.LENGTH) { -1 }
            val visited = BooleanArray(Board.LENGTH)

            for (start in strongLinks.keys) {
                if (visited[start]) continue

                val colorA = mutableListOf<Int>()
                val colorB = mutableListOf<Int>()
                val queue = ArrayDeque<Pair<Int, Int>>()
                queue.add(start to 0)
                visited[start] = true
                color[start] = 0

                while (queue.isNotEmpty()) {
                    val (cell, c) = queue.removeFirst()
                    if (c == 0) colorA.add(cell) else colorB.add(cell)

                    for (neighbor in strongLinks[cell] ?: emptyList()) {
                        if (!visited[neighbor]) {
                            visited[neighbor] = true
                            color[neighbor] = 1 - c
                            queue.add(neighbor to (1 - c))
                        }
                    }
                }

                if (colorA.size < 2 && colorB.size < 2) continue

                // Check for Wrap: cells of same color that see each other
                for (colorGroup in listOf(colorA to colorB, colorB to colorA)) {
                    val (falseColor, trueColor) = colorGroup
                    var wrap = false
                    for (i in falseColor.indices) {
                        for (j in i + 1 until falseColor.size) {
                            if (falseColor[j] in Board.BUDDIES_ARRAY[falseColor[i]]) {
                                wrap = true
                                break
                            }
                        }
                        if (wrap) break
                    }
                    if (wrap && falseColor.isNotEmpty()) {
                        val eliminations = falseColor.map { it to digit }
                        if (eliminations.isNotEmpty()) {
                            return SolutionStep(
                                type = SolutionType.SIMPLE_COLORS_WRAP,
                                value = digit,
                                indices = trueColor,
                                candidatesRemoved = eliminations
                            )
                        }
                    }
                }

                // Check for Trap: uncolored cell sees both colors
                for (cell in 0 until Board.LENGTH) {
                    if (color[cell] != -1 || !board.isCandidate(cell, digit)) continue
                    val seesA = colorA.any { cell in Board.BUDDIES_ARRAY[it] }
                    val seesB = colorB.any { cell in Board.BUDDIES_ARRAY[it] }
                    if (seesA && seesB) {
                        return SolutionStep(
                            type = SolutionType.SIMPLE_COLORS_TRAP,
                            value = digit,
                            indices = colorA + colorB,
                            candidatesRemoved = listOf(cell to digit)
                        )
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
