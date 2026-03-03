package sudoku.core

import sudoku.core.generator.Generator
import sudoku.core.model.*
import kotlin.test.*

class GeneratorTest {

    @Test
    fun testCountSolutions_validPuzzle() {
        // A well-known valid puzzle with unique solution
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = Board()
        board.loadFromString(puzzle)

        val gen = Generator()
        val count = gen.countSolutions(board, 2)
        assertEquals(1, count, "Puzzle should have exactly 1 solution")
    }

    @Test
    fun testCountSolutions_emptyBoard() {
        val board = Board()
        val gen = Generator()
        val count = gen.countSolutions(board, 2)
        assertTrue(count > 1, "Empty board should have multiple solutions")
    }

    @Test
    fun testSolverGrading() {
        // Easy puzzle solvable with singles only
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = Board()
        board.loadFromString(puzzle)

        // Set the solution
        val gen = Generator()
        gen.countSolutions(board, 1)

        val solver = sudoku.core.solver.Solver()
        val result = solver.solve(board)
        assertTrue(result.solved, "Should be able to solve")
        assertTrue(result.score > 0, "Score should be positive")
    }
}
