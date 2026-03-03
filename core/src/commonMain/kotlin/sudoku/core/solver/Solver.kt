package sudoku.core.solver

import sudoku.core.model.*

/**
 * Orchestrator: solves a puzzle step-by-step using logical techniques,
 * accumulating a score for difficulty grading.
 */
class Solver {

    data class Result(
        val steps: List<SolutionStep>,
        val score: Int,
        val difficulty: Difficulty,
        val solved: Boolean
    )

    private val stepFinder = StepFinder()

    /**
     * Solve the given board step-by-step, grading difficulty.
     * The board is cloned internally - the original is not modified.
     *
     * When [maxDifficulty] is set, only techniques at or below that level are used.
     * If the solver gets stuck, it returns with `solved = false`.
     */
    fun solve(board: Board, maxDifficulty: Difficulty? = null): Result {
        val work = Board().apply { copyFrom(board) }
        val steps = mutableListOf<SolutionStep>()
        var totalScore = 0

        while (!work.isSolved) {
            val step = stepFinder.findNextStep(work, maxDifficulty) ?: break

            steps.add(step)
            totalScore += step.type.score

            // Apply the step
            if (step.type.isSingle || step.type == SolutionType.BRUTE_FORCE) {
                work.setCell(step.cellIndex, step.value)
                work.setAllExposedSingles()
            } else {
                // Elimination step - remove candidates
                for ((cellIndex, candidate) in step.candidatesRemoved) {
                    work.setCandidate(cellIndex, candidate, false)
                }
                work.setAllExposedSingles()
            }
        }

        return Result(
            steps = steps,
            score = totalScore,
            difficulty = Difficulty.fromScore(totalScore),
            solved = work.isSolved
        )
    }
}
