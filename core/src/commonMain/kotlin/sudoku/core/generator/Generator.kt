package sudoku.core.generator

import kotlinx.coroutines.yield
import sudoku.core.model.*
import sudoku.core.solver.Solver
import kotlin.random.Random

/**
 * Sudoku puzzle generator - port of HoDoKu's SudokuGenerator.
 *
 * Generates a full valid grid via backtracking, then removes clues
 * while maintaining uniqueness.
 */
class Generator(private val random: Random = Random) {

    private class StackEntry {
        val sudoku = Board()
        var index = 0
        var candidates = IntArray(0)
        var candIndex = 0
    }

    private val stack = Array(82) { StackEntry() }
    private val generateIndices = IntArray(81)
    private val newFullSudoku = IntArray(81)
    private val newValidSudoku = IntArray(81)
    private var solution = IntArray(81)
    private var solutionCount = 0

    /**
     * Generate a puzzle targeting the given [difficulty].
     * Retries generation until the graded difficulty matches.
     */
    suspend fun generate(difficulty: Difficulty, maxRetries: Int = 200): GeneratedPuzzle {
        val solver = Solver()
        for (attempt in 0 until maxRetries) {
            if (attempt % 5 == 4) yield() // yield to avoid blocking on wasm

            generateFullGrid()
            generateInitPos(symmetric = true)

            // Build the puzzle board
            val board = Board()
            for (i in newValidSudoku.indices) {
                if (newValidSudoku[i] != 0) {
                    board.setCell(i, newValidSudoku[i], isFixed = true)
                }
            }
            // Capture puzzle string BEFORE propagation (only original clues)
            val puzzleString = board.toStringCompact()
            board.setAllExposedSingles()

            // Set the solution
            newFullSudoku.copyInto(board.solution)
            board.solutionSet = true

            // Grade difficulty (ungated for accurate score)
            val result = solver.solve(board)
            val gradedDifficulty = Difficulty.fromScore(result.score)

            if (gradedDifficulty == difficulty) {
                // Verify solvable within the difficulty's technique set
                val gatedResult = solver.solve(board, maxDifficulty = difficulty)
                if (gatedResult.solved) {
                    return GeneratedPuzzle(
                        puzzle = puzzleString,
                        solution = newFullSudoku.copyOf(),
                        difficulty = difficulty,
                        score = result.score
                    )
                }
            }
        }
        // Fallback: return whatever we got last
        val board = Board()
        for (i in newValidSudoku.indices) {
            if (newValidSudoku[i] != 0) {
                board.setCell(i, newValidSudoku[i], isFixed = true)
            }
        }
        val puzzleString = board.toStringCompact()
        board.setAllExposedSingles()
        newFullSudoku.copyInto(board.solution)
        board.solutionSet = true
        val result = Solver().solve(board)
        return GeneratedPuzzle(
            puzzle = puzzleString,
            solution = newFullSudoku.copyOf(),
            difficulty = Difficulty.fromScore(result.score),
            score = result.score
        )
    }

    /** Count solutions (0, 1, or 2=multiple). Stores first solution. */
    fun countSolutions(board: Board, maxCount: Int = 2): Int {
        solve(board, maxCount)
        if (solutionCount == 1) {
            solution.copyInto(board.solution)
            board.solutionSet = true
        }
        return solutionCount
    }

    fun countSolutions(values: IntArray, maxCount: Int = 2): Int {
        val emptyBoard = Board()
        for (i in values.indices) {
            if (values[i] in 1..9) {
                emptyBoard.setCellBS(i, values[i])
            }
        }
        emptyBoard.rebuildInternalData()
        emptyBoard.setAllExposedSingles()
        solveBT(0, maxCount)
        return solutionCount
    }

    // --- Backtracking solver ---

    private fun solve(board: Board, maxSolutionCount: Int) {
        stack[0].sudoku.copyFrom(board)
        stack[0].candidates = IntArray(0)
        stack[0].candIndex = 0
        solveBT(0, maxSolutionCount)
    }

    private fun solve(cellValues: IntArray, maxSolutionCount: Int) {
        val board = Board()
        for (i in cellValues.indices) {
            if (cellValues[i] in 1..9) {
                board.setCellBS(i, cellValues[i])
            }
        }
        board.rebuildInternalData()
        stack[0].sudoku.copyFrom(board)
        stack[0].candidates = IntArray(0)
        stack[0].candIndex = 0
        solveBT(0, maxSolutionCount)
    }

    private fun solveBT(startLevel: Int, maxSolutionCount: Int) {
        solutionCount = 0

        if (!stack[0].sudoku.setAllExposedSingles()) return
        if (stack[0].sudoku.unsolvedCellsAnz == 0) {
            solution = stack[0].sudoku.values.copyOf()
            solutionCount = 1
            return
        }

        var level = startLevel
        var tries = 0
        while (true) {
            if (tries++ >= 1_000_000) break

            if (stack[level].sudoku.unsolvedCellsAnz == 0) {
                solutionCount++
                if (solutionCount == 1) {
                    solution = stack[level].sudoku.values.copyOf()
                }
                if (solutionCount > maxSolutionCount) return
            } else {
                // Find unsolved cell with fewest candidates
                var bestIndex = -1
                var bestCount = 10
                val s = stack[level].sudoku
                for (i in 0 until Board.LENGTH) {
                    val cell = s.cells[i]
                    if (cell != 0) {
                        val count = Board.ANZ_VALUES[cell]
                        if (count < bestCount) {
                            bestCount = count
                            bestIndex = i
                        }
                    }
                }
                level++
                if (bestIndex < 0) { solutionCount = 0; return }
                stack[level].index = bestIndex
                stack[level].candidates = Board.POSSIBLE_VALUES[stack[level - 1].sudoku.cells[bestIndex]]
                stack[level].candIndex = 0
            }

            var done = false
            do {
                while (stack[level].candIndex >= stack[level].candidates.size) {
                    level--
                    if (level <= 0) { done = true; break }
                }
                if (done) break

                val nextCand = stack[level].candidates[stack[level].candIndex++]
                stack[level].sudoku.copyForBacktracking(stack[level - 1].sudoku)
                if (!stack[level].sudoku.setCell(stack[level].index, nextCand)) continue
                if (stack[level].sudoku.setAllExposedSingles()) break
            } while (true)

            if (done) break
        }
    }

    // --- Full grid generation ---

    private fun generateFullGrid() {
        while (!doGenerateFullGrid()) { /* retry with new random order */ }
    }

    private fun doGenerateFullGrid(): Boolean {
        // Random cell order
        for (i in generateIndices.indices) generateIndices[i] = i
        for (i in generateIndices.indices) {
            val j = random.nextInt(generateIndices.size)
            val tmp = generateIndices[i]
            generateIndices[i] = generateIndices[j]
            generateIndices[j] = tmp
        }

        stack[0].sudoku.clear()
        stack[0].index = -1

        var level = 0
        var actTries = 0

        while (true) {
            if (stack[level].sudoku.unsolvedCellsAnz == 0) {
                stack[level].sudoku.values.copyInto(newFullSudoku)
                return true
            }

            // Find first unsolved cell in random order
            var index = -1
            val actValues = stack[level].sudoku.values
            for (i in generateIndices) {
                if (actValues[i] == 0) { index = i; break }
            }

            level++
            stack[level].index = index
            stack[level].candidates = Board.POSSIBLE_VALUES[stack[level - 1].sudoku.cells[index]]
            stack[level].candIndex = 0

            if (++actTries > 100) return false

            var done = false
            do {
                while (stack[level].candIndex >= stack[level].candidates.size) {
                    level--
                    if (level <= 0) { done = true; break }
                }
                if (done) break

                val nextCand = stack[level].candidates[stack[level].candIndex++]
                stack[level].sudoku.copyForBacktracking(stack[level - 1].sudoku)
                if (!stack[level].sudoku.setCell(stack[level].index, nextCand)) continue
                if (stack[level].sudoku.setAllExposedSingles()) break
            } while (true)

            if (done) break
        }
        return false
    }

    // --- Clue removal ---

    private fun generateInitPos(symmetric: Boolean) {
        val used = BooleanArray(81)
        var usedCount = 81

        newFullSudoku.copyInto(newValidSudoku)
        var remainingClues = 81

        while (remainingClues > 17 && usedCount > 1) {
            var i = random.nextInt(81)
            while (used[i]) {
                i = if (i < 80) i + 1 else 0
            }
            used[i] = true
            usedCount--

            if (newValidSudoku[i] == 0) continue

            val row = i / 9
            val col = i % 9
            val symm = (8 - row) * 9 + (8 - col)

            if (symmetric && (row != 4 || col != 4) && newValidSudoku[symm] == 0) continue

            // Try deleting
            newValidSudoku[i] = 0
            remainingClues--

            var symmDeleted = false
            if (symmetric && (row != 4 || col != 4)) {
                newValidSudoku[symm] = 0
                used[symm] = true
                usedCount--
                remainingClues--
                symmDeleted = true
            }

            solve(newValidSudoku, 1)

            if (solutionCount > 1) {
                // Restore
                newValidSudoku[i] = newFullSudoku[i]
                remainingClues++
                if (symmDeleted) {
                    newValidSudoku[symm] = newFullSudoku[symm]
                    remainingClues++
                }
            }
        }
    }
}

data class GeneratedPuzzle(
    val puzzle: String,
    val solution: IntArray,
    val difficulty: Difficulty,
    val score: Int
) {
    override fun equals(other: Any?): Boolean =
        other is GeneratedPuzzle && puzzle == other.puzzle

    override fun hashCode(): Int = puzzle.hashCode()
}
