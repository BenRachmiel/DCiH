package sudoku.core.solver

import sudoku.core.model.*

/**
 * Brute force solver: if no logical technique applies, just solve one cell
 * by comparing with the known solution.
 */
class BruteForceSolver : AbstractSolver() {

    override fun findSteps(board: Board): List<SolutionStep> {
        if (!board.solutionSet) return emptyList()
        for (i in 0 until Board.LENGTH) {
            if (board.values[i] == 0) {
                return listOf(
                    SolutionStep(
                        type = SolutionType.BRUTE_FORCE,
                        cellIndex = i,
                        value = board.solution[i]
                    )
                )
            }
        }
        return emptyList()
    }
}
