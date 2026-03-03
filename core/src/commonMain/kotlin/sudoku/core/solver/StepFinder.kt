package sudoku.core.solver

import sudoku.core.model.Board
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionStep

/**
 * Dispatches to specialized solvers in order of increasing difficulty.
 */
class StepFinder {
    private val solvers = listOf(
        SimpleSolver(),
        FishSolver(),
        WingSolver(),
        ColoringSolver(),
        BruteForceSolver()
    )

    fun findNextStep(board: Board, maxDifficulty: Difficulty? = null): SolutionStep? {
        for (solver in solvers) {
            val steps = solver.findSteps(board)
            val step = if (maxDifficulty != null) {
                steps.firstOrNull { it.type.difficulty <= maxDifficulty }
            } else {
                steps.firstOrNull()
            }
            if (step != null) return step
        }
        return null
    }
}
