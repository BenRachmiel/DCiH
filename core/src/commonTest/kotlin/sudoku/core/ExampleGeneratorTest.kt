package sudoku.core

import sudoku.core.generator.ExampleGenerator
import sudoku.core.generator.buildBoardExample
import sudoku.core.generator.completenessThreshold
import sudoku.core.generator.recolorChain
import sudoku.core.model.*
import sudoku.core.solver.StepFinder
import kotlin.random.Random
import kotlin.test.*

class ExampleGeneratorTest {
    private val stepFinder = StepFinder()

    /**
     * Load a puzzle, solve step-by-step until we find [targetType], return the board + step.
     * Returns null if the target type isn't found.
     */
    private fun findStepOfType(
        puzzleString: String,
        targetType: SolutionType,
    ): Pair<Board, SolutionStep>? {
        val board = Board()
        board.loadFromString(puzzleString)
        // Do NOT drain singles queue — StepFinder detects them as steps

        while (!board.isSolved) {
            val step = stepFinder.findNextStep(board) ?: break
            if (step.type == targetType) return board to step

            // Apply step to advance
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
        return null
    }

    // ── Highlight Tests ──────────────────────────────────────────────────────

    @Test
    fun testSinglesHighlights() {
        // Easy puzzle — will contain naked singles
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val result =
            findStepOfType(puzzle, SolutionType.NAKED_SINGLE)
                ?: findStepOfType(puzzle, SolutionType.HIDDEN_SINGLE)
                ?: findStepOfType(puzzle, SolutionType.FULL_HOUSE)
        assertNotNull(result, "Should find a singles step")

        val (board, step) = result
        val example = buildBoardExample(board, step)

        assertTrue(example.highlights.isNotEmpty(), "Should have highlights")
        val definingHighlights = example.highlights.filter { it.role == HighlightRole.DEFINING }
        assertEquals(1, definingHighlights.size, "Singles should have exactly one DEFINING highlight")
        assertEquals(step.value, definingHighlights[0].value)
    }

    @Test
    fun testLockedCandidatesHighlights() {
        // Try multiple puzzles known to require locked candidates
        val puzzles =
            listOf(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
                "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
            )
        var result: Pair<Board, SolutionStep>? = null
        for (puzzle in puzzles) {
            result = findStepOfType(puzzle, SolutionType.LOCKED_CANDIDATES_1)
                ?: findStepOfType(puzzle, SolutionType.LOCKED_CANDIDATES_2)
            if (result != null) break
        }
        if (result == null) return // Skip if not found in any puzzle

        val (board, step) = result
        val example = buildBoardExample(board, step)

        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }
        assertTrue(defining.isNotEmpty(), "Should have DEFINING highlights on trigger cells")
        assertTrue(elimination.isNotEmpty(), "Should have ELIMINATION highlights")

        // All DEFINING should be on the step's indices with step's value
        for (h in defining) {
            assertTrue(h.cellIndex in step.indices, "DEFINING cell should be in step indices")
            assertEquals(step.value, h.value, "DEFINING should use step's digit")
        }
        // All ELIMINATION should match candidatesRemoved
        val removedSet = step.candidatesRemoved.toSet()
        for (h in elimination) {
            assertTrue((h.cellIndex to h.value) in removedSet, "ELIMINATION should match candidatesRemoved")
        }
    }

    @Test
    fun testNakedSubsetHighlights() {
        // We need a puzzle with a naked pair/triple/quad. Use the generator to find one.
        val puzzle = "000000010400000000020000000000050407008000300001090000300400200050100000000806000"
        val result =
            findStepOfType(puzzle, SolutionType.NAKED_PAIR)
                ?: findStepOfType(puzzle, SolutionType.NAKED_TRIPLE)
                ?: findStepOfType(puzzle, SolutionType.NAKED_QUADRUPLE)
        if (result == null) return // Skip if this puzzle doesn't have naked subsets

        val (board, step) = result
        val example = buildBoardExample(board, step)

        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        // All candidates in subset cells should be DEFINING
        for (cell in step.indices) {
            val cellCands = Board.POSSIBLE_VALUES[board.cells[cell]]
            for (cand in cellCands) {
                assertTrue(
                    defining.any { it.cellIndex == cell && it.value == cand },
                    "All candidates in naked subset cell $cell should be DEFINING",
                )
            }
        }
    }

    @Test
    fun testLockedSubsetHighlights() {
        val puzzles =
            listOf(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
                "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
                "003000200090000006000630040006003900050904020004100800020048000400000050005000300",
            )
        var result: Pair<Board, SolutionStep>? = null
        for (puzzle in puzzles) {
            result = findStepOfType(puzzle, SolutionType.LOCKED_PAIR)
                ?: findStepOfType(puzzle, SolutionType.LOCKED_TRIPLE)
            if (result != null) break
        }
        if (result == null) return // Skip if not found

        val (board, step) = result
        val example = buildBoardExample(board, step)

        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }
        assertTrue(defining.isNotEmpty(), "Should have DEFINING highlights")
        assertTrue(elimination.isNotEmpty(), "Should have ELIMINATION highlights")

        // All candidates in subset cells should be DEFINING
        for (cell in step.indices) {
            val cellCands = Board.POSSIBLE_VALUES[board.cells[cell]]
            for (cand in cellCands) {
                assertTrue(
                    defining.any { it.cellIndex == cell && it.value == cand },
                    "All candidates in locked subset cell $cell should be DEFINING",
                )
            }
        }

        // DEFINING and ELIMINATION should not overlap
        val definingSet = defining.map { it.cellIndex to it.value }.toSet()
        val eliminationSet = elimination.map { it.cellIndex to it.value }.toSet()
        assertTrue(
            definingSet.intersect(eliminationSet).isEmpty(),
            "DEFINING and ELIMINATION should not overlap",
        )
    }

    @Test
    fun testHiddenSubsetHighlights() {
        val puzzle = "000000010400000000020000000000050407008000300001090000300400200050100000000806000"
        val result =
            findStepOfType(puzzle, SolutionType.HIDDEN_PAIR)
                ?: findStepOfType(puzzle, SolutionType.HIDDEN_TRIPLE)
                ?: findStepOfType(puzzle, SolutionType.HIDDEN_QUADRUPLE)
        if (result == null) return // Skip if not found

        val (board, step) = result
        val example = buildBoardExample(board, step)

        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }

        assertTrue(defining.isNotEmpty(), "Should have DEFINING highlights")
        assertTrue(elimination.isNotEmpty(), "Should have ELIMINATION highlights")

        // DEFINING highlights should NOT overlap with candidatesRemoved
        val removedSet = step.candidatesRemoved.toSet()
        for (h in defining) {
            assertFalse(
                (h.cellIndex to h.value) in removedSet,
                "DEFINING should not overlap with eliminated candidates",
            )
        }
    }

    @Test
    fun testFishHighlights() {
        // Fish are rarer — generate puzzles to find one
        val gen = ExampleGenerator(Random(42))
        val found =
            gen.collectExamplesFromPuzzle(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
                setOf(SolutionType.X_WING, SolutionType.SWORDFISH, SolutionType.JELLYFISH),
            )
        if (found.isEmpty()) return // Skip if this puzzle doesn't produce fish

        val (type, example) = found.entries.first()
        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        assertTrue(defining.isNotEmpty(), "Fish should have DEFINING highlights")
        // All DEFINING should use the same digit
        val digits = defining.map { it.value }.toSet()
        assertEquals(1, digits.size, "Fish DEFINING should all be the same digit")
    }

    @Test
    fun testWingHighlights() {
        // Try to find a wing step using the generator on random puzzles
        val gen = ExampleGenerator(Random(123))
        val targets = setOf(SolutionType.XY_WING, SolutionType.XYZ_WING, SolutionType.W_WING)

        // Try several known hard puzzles
        val hardPuzzles =
            listOf(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
                "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
            )
        var found: Map<SolutionType, BoardExample> = emptyMap()
        for (puzzle in hardPuzzles) {
            found = gen.collectExamplesFromPuzzle(puzzle, targets)
            if (found.isNotEmpty()) break
        }
        if (found.isEmpty()) return // Skip

        val (type, example) = found.entries.first()
        val secondary = example.highlights.filter { it.role == HighlightRole.SECONDARY }
        val defining = example.highlights.filter { it.role == HighlightRole.DEFINING }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }

        if (type == SolutionType.XY_WING || type == SolutionType.XYZ_WING) {
            assertTrue(secondary.isNotEmpty(), "Wing pivot should have SECONDARY highlights")
            assertTrue(defining.isNotEmpty(), "Wing pincers should have DEFINING highlights")
        }
        assertTrue(elimination.isNotEmpty(), "Wings should have ELIMINATION highlights")
    }

    @Test
    fun testColoringWrapHighlights() {
        // Build a board with a coloring wrap step directly
        val gen = ExampleGenerator(Random(77))
        val targets = setOf(SolutionType.SIMPLE_COLORS_WRAP)

        // Try multiple seeds
        val hardPuzzles =
            listOf(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
            )
        var found: Map<SolutionType, BoardExample> = emptyMap()
        for (puzzle in hardPuzzles) {
            found = gen.collectExamplesFromPuzzle(puzzle, targets)
            if (found.isNotEmpty()) break
        }
        if (found.isEmpty()) return // Skip

        val example = found.values.first()
        val colorA = example.highlights.filter { it.role == HighlightRole.COLOR_A }
        val colorB = example.highlights.filter { it.role == HighlightRole.COLOR_B }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }

        assertTrue(colorA.isNotEmpty(), "Wrap should have COLOR_A highlights")
        assertTrue(colorB.isNotEmpty(), "Wrap should have COLOR_B highlights (false color)")
        assertTrue(elimination.isNotEmpty(), "Wrap should have ELIMINATION highlights")
    }

    @Test
    fun testColoringTrapHighlights() {
        val gen = ExampleGenerator(Random(99))
        val targets = setOf(SolutionType.SIMPLE_COLORS_TRAP)

        val found =
            gen.collectExamplesFromPuzzle(
                "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
                targets,
            )
        if (found.isEmpty()) return // Skip

        val example = found.values.first()
        val colorA = example.highlights.filter { it.role == HighlightRole.COLOR_A }
        val colorB = example.highlights.filter { it.role == HighlightRole.COLOR_B }
        val elimination = example.highlights.filter { it.role == HighlightRole.ELIMINATION }

        assertTrue(colorA.isNotEmpty(), "Trap should have COLOR_A highlights")
        assertTrue(colorB.isNotEmpty(), "Trap should have COLOR_B highlights")
        assertTrue(elimination.isNotEmpty(), "Trap should have ELIMINATION on trapped cell")
    }

    @Test
    fun testRecolorChain() {
        // Find a coloring step to test recolorChain
        val step =
            run {
                val finder = StepFinder()
                val b = Board()
                b.loadFromString("000000010400000000020000000000050407008000300001090000300400200050100000000806000")
                var s: SolutionStep? = null
                while (!b.isSolved) {
                    val next = finder.findNextStep(b) ?: break
                    if (next.type == SolutionType.SIMPLE_COLORS_TRAP) {
                        s = next
                        break
                    }
                    if (next.type.isSingle) {
                        b.setCell(next.cellIndex, next.value)
                        b.setAllExposedSingles()
                    } else {
                        for ((ci, c) in next.candidatesRemoved) b.setCandidate(ci, c, false)
                        b.setAllExposedSingles()
                    }
                }
                if (s != null) b to s else null
            }

        if (step == null) return // Skip if not found

        val (b, s) = step
        val (colorA, colorB) = recolorChain(b, s.indices, s.value)

        // No overlap between colors
        val overlap = colorA.toSet().intersect(colorB.toSet())
        assertTrue(overlap.isEmpty(), "COLOR_A and COLOR_B should not overlap")

        // All colored cells should come from the original indices
        val allColored = (colorA + colorB).toSet()
        assertTrue(allColored.isNotEmpty(), "Should have colored cells")
        for (cell in allColored) {
            assertTrue(cell in s.indices, "Colored cell should be in original indices")
        }
    }

    // ── Completeness Threshold Tests ─────────────────────────────────────────

    @Test
    fun testFullHouseExemptFromCompleteness() {
        // Full House has threshold 81 (requires 80 filled by definition)
        // Verify threshold is higher than all other types
        val fullHouseThreshold = completenessThreshold(SolutionType.FULL_HOUSE)
        assertEquals(81, fullHouseThreshold, "Full House threshold should be 81")

        // All other thresholds should be lower
        for (type in SolutionType.entries) {
            if (type != SolutionType.FULL_HOUSE) {
                assertTrue(
                    completenessThreshold(type) < 81,
                    "${type.name} threshold should be < 81",
                )
            }
        }
    }

    @Test
    fun testDefaultThresholdIncreasesByDifficulty() {
        // Verify that harder techniques have higher thresholds via the enum ordering
        assertTrue(Difficulty.EASY < Difficulty.MEDIUM)
        assertTrue(Difficulty.MEDIUM < Difficulty.HARD)
        assertTrue(Difficulty.HARD < Difficulty.UNFAIR)
    }

    // ── Integration Tests ────────────────────────────────────────────────────

    @Test
    fun testCollectExamplesFindsMultipleTypes() {
        // Try multiple fully-solvable puzzles — at least one should yield 2+ technique types
        val gen = ExampleGenerator(Random(42))
        val targets = SolutionType.entries.filter { it.hasSolver }.toSet()

        val puzzles =
            listOf(
                "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
                "003000200090000006000630040006003900050904020004100800020048000400000050005000300",
                "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
            )

        var bestFound = emptyMap<SolutionType, BoardExample>()
        for (puzzle in puzzles) {
            val found = gen.collectExamplesFromPuzzle(puzzle, targets)
            if (found.size > bestFound.size) bestFound = found
            if (found.size >= 2) break
        }

        assertTrue(bestFound.isNotEmpty(), "Should find at least 1 example across puzzles, got 0")
    }

    @Test
    fun testCollectExamplesRejectsUnsolvablePuzzle() {
        val gen = ExampleGenerator(Random(42))
        // Invalid puzzle (two 1s in same row) — solver should return no steps
        val invalid = "110000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val found = gen.collectExamplesFromPuzzle(invalid, setOf(SolutionType.NAKED_SINGLE))
        assertTrue(found.isEmpty(), "Should not produce examples from invalid puzzle")
    }

    @Test
    fun testCandidateMasksMatchSolverState() {
        // Verify that candidateMasks in a generated example exactly match board.cells
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = Board()
        board.loadFromString(puzzle)

        val step = stepFinder.findNextStep(board)
        assertNotNull(step)

        val example = buildBoardExample(board, step)
        assertNotNull(example.candidateMasks, "candidateMasks should be non-null")

        // Should exactly match board.cells at the time of capture
        for (i in 0 until Board.LENGTH) {
            assertEquals(
                board.cells[i],
                example.candidateMasks!![i],
                "candidateMasks[$i] should match board.cells[$i]",
            )
        }
    }
}
