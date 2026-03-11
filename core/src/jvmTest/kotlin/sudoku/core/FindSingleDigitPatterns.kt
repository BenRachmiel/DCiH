package sudoku.core

import sudoku.core.generator.Generator
import sudoku.core.model.*
import sudoku.core.solver.StepFinder
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

/**
 * Utility to find puzzles containing single-digit patterns.
 * Not a real test — prints puzzle strings for use in tests.
 */
class FindSingleDigitPatterns {
    @Test
    fun findPuzzles() = runBlocking {
        val stepFinder = StepFinder()
        val targets = setOf(
            SolutionType.SKYSCRAPER,
            SolutionType.TWO_STRING_KITE,
            SolutionType.EMPTY_RECTANGLE,
            SolutionType.TURBOT_FISH,
        )
        val found = mutableMapOf<SolutionType, String>()

        for (diff in listOf(Difficulty.HARD, Difficulty.UNFAIR, Difficulty.EXTREME)) {
            for (seed in 0L..20L) {
            val gen = Generator(Random(seed))
            for (attempt in 0 until 100) {
                if (found.keys.containsAll(targets)) break
                val puzzle = gen.generate(diff, maxRetries = 20)
                val board = Board()
                board.loadFromString(puzzle.puzzle)

                while (!board.isSolved) {
                    val step = stepFinder.findNextStep(board) ?: break
                    if (step.type in targets && step.type !in found) {
                        found[step.type] = puzzle.puzzle
                        println("Found ${step.type.name}: \"${puzzle.puzzle}\"")
                    }
                    if (step.type.isSingle || step.type == SolutionType.BRUTE_FORCE) {
                        board.setCell(step.cellIndex, step.value)
                        board.setAllExposedSingles()
                    } else {
                        for ((cellIndex, candidate) in step.candidatesRemoved) {
                            board.setCandidate(cellIndex, candidate, false)
                        }
                        board.setAllExposedSingles()
                    }
                }
            }
            }
        }

        println("\n=== Results ===")
        for ((type, puzzle) in found) {
            println("\"$puzzle\", // ${type.name}")
        }
        val missing = targets - found.keys
        if (missing.isNotEmpty()) {
            println("Missing: $missing")
        }
    }
}
