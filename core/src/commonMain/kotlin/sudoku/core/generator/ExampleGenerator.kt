package sudoku.core.generator

import kotlinx.coroutines.yield
import sudoku.core.model.*
import sudoku.core.solver.Solver
import kotlin.random.Random

/**
 * Generates [BoardExample]s by running the solver on generated puzzles and
 * capturing board snapshots when target techniques are found.
 */
class ExampleGenerator(
    private val random: Random = Random,
) {
    /**
     * Generate examples for all requested [targets].
     * One puzzle solve can yield examples for multiple techniques (efficient batching).
     */
    suspend fun generateExamples(
        targets: Set<SolutionType>,
        maxAttempts: Int = 500,
    ): Map<SolutionType, BoardExample> {
        val result = mutableMapOf<SolutionType, BoardExample>()
        val remaining = targets.toMutableSet()
        val generator = Generator(random)

        for (attempt in 0 until maxAttempts) {
            if (remaining.isEmpty()) break
            if (attempt % 5 == 4) yield()

            // Target difficulty of the hardest remaining technique
            val targetDifficulty = remaining.maxOf { it.difficulty }
            val puzzle = generator.generate(targetDifficulty, maxRetries = 50)

            val found = collectExamplesFromPuzzle(puzzle.puzzle, remaining)
            for ((type, example) in found) {
                result[type] = example
                remaining.remove(type)
            }
        }

        return result
    }

    /**
     * Generate a single example for one [type].
     */
    suspend fun generateExample(
        type: SolutionType,
        maxAttempts: Int = 200,
    ): BoardExample? {
        val result = generateExamples(setOf(type), maxAttempts)
        return result[type]
    }

    /**
     * Solve a puzzle step-by-step via [Solver] and capture examples for any matching targets.
     * Uses Solver.solve() to get the full step list, then replays on a fresh board to
     * capture accurate board states at each technique. This avoids the cascade problem
     * where setAllExposedSingles() would skip over advanced techniques.
     * Visible for testing.
     */
    internal fun collectExamplesFromPuzzle(
        puzzleString: String,
        targets: Set<SolutionType>,
    ): Map<SolutionType, BoardExample> {
        val result = mutableMapOf<SolutionType, BoardExample>()
        val board = Board()
        board.loadFromString(puzzleString)

        val solveResult = Solver().solve(board)
        if (!solveResult.solved) return result

        // Replay steps on a fresh board, capturing snapshots at target points
        val replay = Board()
        replay.loadFromString(puzzleString)

        for (step in solveResult.steps) {
            if (step.type in targets && step.type !in result) {
                val filledCells = Board.LENGTH - replay.unsolvedCellsAnz
                if (filledCells <= completenessThreshold(step.type)) {
                    result[step.type] = buildBoardExample(replay, step)
                }
            }

            // Apply step to advance the replay board
            if (step.type.isSingle || step.type == SolutionType.BRUTE_FORCE) {
                replay.setCell(step.cellIndex, step.value)
                replay.setAllExposedSingles()
            } else {
                for ((ci, c) in step.candidatesRemoved) {
                    replay.setCandidate(ci, c, false)
                }
                replay.setAllExposedSingles()
            }
        }

        return result
    }
}

/**
 * Maximum filled cells for an example to be considered educationally useful.
 * Sparser boards are better for illustration.
 */
internal fun completenessThreshold(type: SolutionType): Int =
    when {
        type == SolutionType.FULL_HOUSE -> 81
        type.difficulty == Difficulty.EASY -> 60
        type.difficulty == Difficulty.MEDIUM -> 65
        type.difficulty == Difficulty.HARD -> 70
        else -> 75 // UNFAIR, EXTREME
    }
