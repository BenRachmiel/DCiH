package sudoku.core.solver

import sudoku.core.model.Board
import sudoku.core.model.SolutionStep

abstract class AbstractSolver {
    abstract fun findSteps(board: Board): List<SolutionStep>
}
