package sudoku.core.generator

import sudoku.core.model.*
import sudoku.core.model.HighlightRole.*

/**
 * Converts a solver board state + solution step into a [BoardExample] with
 * semantic highlights. Captures `board.cells` directly as `candidateMasks` —
 * exact solver-state candidates, not recomputed.
 */
fun buildBoardExample(board: Board, step: SolutionStep): BoardExample {
    val puzzle = board.toStringCompact()
    val candidateMasks = board.cells.copyOf()
    val highlights = buildHighlights(board, step)

    return BoardExample(
        puzzle = puzzle,
        candidateMasks = candidateMasks,
        highlights = highlights,
    )
}

private fun buildHighlights(board: Board, step: SolutionStep): List<CandidateHighlight> {
    val highlights = mutableListOf<CandidateHighlight>()

    when (step.type) {
        // Singles: DEFINING on the placed cell + value
        SolutionType.FULL_HOUSE,
        SolutionType.NAKED_SINGLE,
        SolutionType.HIDDEN_SINGLE -> {
            highlights.add(CandidateHighlight(step.cellIndex, step.value, DEFINING))
        }

        // Locked Candidates: DEFINING on trigger cells, ELIMINATION on removed
        SolutionType.LOCKED_CANDIDATES_1,
        SolutionType.LOCKED_CANDIDATES_2 -> {
            for (cell in step.indices) {
                highlights.add(CandidateHighlight(cell, step.value, DEFINING))
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // Locked Pair/Triple: same as locked candidates
        SolutionType.LOCKED_PAIR,
        SolutionType.LOCKED_TRIPLE -> {
            for (cell in step.indices) {
                highlights.add(CandidateHighlight(cell, step.value, DEFINING))
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // Naked Subsets: all candidates in subset cells are DEFINING
        SolutionType.NAKED_PAIR,
        SolutionType.NAKED_TRIPLE,
        SolutionType.NAKED_QUADRUPLE -> {
            for (cell in step.indices) {
                for (digit in Board.POSSIBLE_VALUES[board.cells[cell]]) {
                    highlights.add(CandidateHighlight(cell, digit, DEFINING))
                }
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // Hidden Subsets: hidden digits DEFINING, removed digits ELIMINATION
        SolutionType.HIDDEN_PAIR,
        SolutionType.HIDDEN_TRIPLE,
        SolutionType.HIDDEN_QUADRUPLE -> {
            val removedSet = step.candidatesRemoved.toSet()
            for (cell in step.indices) {
                for (digit in Board.POSSIBLE_VALUES[board.cells[cell]]) {
                    if ((cell to digit) !in removedSet) {
                        highlights.add(CandidateHighlight(cell, digit, DEFINING))
                    }
                }
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // Fish: DEFINING on fish cells with the digit, ELIMINATION on removed
        SolutionType.X_WING,
        SolutionType.SWORDFISH,
        SolutionType.JELLYFISH -> {
            for (cell in step.indices) {
                highlights.add(CandidateHighlight(cell, step.value, DEFINING))
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // XY-Wing: indices = [pivot, pincer1, pincer2]
        // SECONDARY on pivot candidates, DEFINING on pincer candidates
        SolutionType.XY_WING -> {
            val pivot = step.indices[0]
            for (digit in Board.POSSIBLE_VALUES[board.cells[pivot]]) {
                highlights.add(CandidateHighlight(pivot, digit, SECONDARY))
            }
            for (i in 1..2) {
                val pincer = step.indices[i]
                for (digit in Board.POSSIBLE_VALUES[board.cells[pincer]]) {
                    highlights.add(CandidateHighlight(pincer, digit, DEFINING))
                }
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // XYZ-Wing: indices = [pivot, pincer1, pincer2]
        // SECONDARY on pivot candidates, DEFINING on pincer candidates
        SolutionType.XYZ_WING -> {
            val pivot = step.indices[0]
            for (digit in Board.POSSIBLE_VALUES[board.cells[pivot]]) {
                highlights.add(CandidateHighlight(pivot, digit, SECONDARY))
            }
            for (i in 1..2) {
                val pincer = step.indices[i]
                for (digit in Board.POSSIBLE_VALUES[board.cells[pincer]]) {
                    highlights.add(CandidateHighlight(pincer, digit, DEFINING))
                }
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // W-Wing: indices = [cell1, cell2, link1, link2]
        // DEFINING on bivalue cell candidates, SECONDARY on strong link digit
        SolutionType.W_WING -> {
            val c1 = step.indices[0]
            val c2 = step.indices[1]
            val link1 = step.indices[2]
            val link2 = step.indices[3]
            for (digit in Board.POSSIBLE_VALUES[board.cells[c1]]) {
                highlights.add(CandidateHighlight(c1, digit, DEFINING))
            }
            for (digit in Board.POSSIBLE_VALUES[board.cells[c2]]) {
                highlights.add(CandidateHighlight(c2, digit, DEFINING))
            }
            // The strong link digit is the one that's NOT being eliminated
            val linkDigit = Board.POSSIBLE_VALUES[board.cells[c1]].first { it != step.value }
            highlights.add(CandidateHighlight(link1, linkDigit, SECONDARY))
            highlights.add(CandidateHighlight(link2, linkDigit, SECONDARY))
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // Simple Colors Wrap: indices = true-color cells, candidatesRemoved = false-color cells
        // COLOR_A on true cells, COLOR_B + ELIMINATION on false cells
        SolutionType.SIMPLE_COLORS_WRAP -> {
            for (cell in step.indices) {
                highlights.add(CandidateHighlight(cell, step.value, COLOR_A))
            }
            for ((cell, _) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, step.value, COLOR_B))
                highlights.add(CandidateHighlight(cell, step.value, ELIMINATION))
            }
        }

        // Simple Colors Trap: indices = all colored cells (combined), candidatesRemoved = trapped cell
        // Re-derive COLOR_A/B split via recolorChain, ELIMINATION on trapped cell
        SolutionType.SIMPLE_COLORS_TRAP -> {
            val (colorA, colorB) = recolorChain(board, step.indices, step.value)
            for (cell in colorA) {
                highlights.add(CandidateHighlight(cell, step.value, COLOR_A))
            }
            for (cell in colorB) {
                highlights.add(CandidateHighlight(cell, step.value, COLOR_B))
            }
            for ((cell, digit) in step.candidatesRemoved) {
                highlights.add(CandidateHighlight(cell, digit, ELIMINATION))
            }
        }

        // No solver — should never reach here for generation
        else -> {}
    }

    return highlights
}

/**
 * Re-derives the 2-coloring of a conjugate chain by BFS over strong links.
 * Takes the combined list of colored cells and splits them into two groups.
 *
 * @return Pair of (colorA cells, colorB cells)
 */
fun recolorChain(board: Board, coloredCells: List<Int>, digit: Int): Pair<List<Int>, List<Int>> {
    val cellSet = coloredCells.toSet()
    val colorA = mutableListOf<Int>()
    val colorB = mutableListOf<Int>()
    val visited = mutableSetOf<Int>()

    // Build strong links (conjugate pairs) among the colored cells
    val strongLinks = mutableMapOf<Int, MutableList<Int>>()
    for (unit in Board.ALL_UNITS) {
        val candidates = mutableListOf<Int>()
        for (cell in unit) {
            if (board.isCandidate(cell, digit)) candidates.add(cell)
        }
        if (candidates.size == 2 && candidates[0] in cellSet && candidates[1] in cellSet) {
            strongLinks.getOrPut(candidates[0]) { mutableListOf() }.add(candidates[1])
            strongLinks.getOrPut(candidates[1]) { mutableListOf() }.add(candidates[0])
        }
    }

    // BFS from the first colored cell
    for (start in coloredCells) {
        if (start in visited) continue
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(start to 0)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val (cell, color) = queue.removeFirst()
            if (color == 0) colorA.add(cell) else colorB.add(cell)

            for (neighbor in strongLinks[cell] ?: emptyList()) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor to (1 - color))
                }
            }
        }
    }

    return colorA to colorB
}
