package sudoku.core

import sudoku.core.model.*
import kotlin.test.*

class PuzzleJsonTest {
    private val sampleGivens = "000601349000030060060000001008006003003900007900003200030000090090060000241308000"
    private val sampleSolution = "285671349714239568369854721428716953653948187971583246836127495197465832542398617"

    @Test
    fun testRoundTrip() {
        val values = IntArray(81) { sampleGivens[it] - '0' }
        val fixed = BooleanArray(81) { values[it] != 0 }
        val solution = IntArray(81) { sampleSolution[it] - '0' }
        val pencilMarks = Array<MutableSet<Int>>(81) { mutableSetOf() }
        pencilMarks[0] = mutableSetOf(2, 5, 8)
        pencilMarks[1] = mutableSetOf(7, 8)

        val pj = PuzzleJson.fromGameState(values, fixed, solution, pencilMarks, Difficulty.HARD)
        val json = pj.toJson()
        val parsed = PuzzleJson.fromJson(json)

        assertEquals(pj.puzzle, parsed.puzzle)
        assertEquals(pj.givens, parsed.givens)
        assertEquals(pj.solution, parsed.solution)
        assertEquals(pj.difficulty, parsed.difficulty)
        assertEquals(pj.candidates, parsed.candidates)
    }

    @Test
    fun testFieldValues() {
        val values = IntArray(81) { sampleGivens[it] - '0' }
        // Simulate user placing digit 2 at cell 0
        values[0] = 2
        val fixed = BooleanArray(81) { sampleGivens[it] != '0' }
        val solution = IntArray(81) { sampleSolution[it] - '0' }
        val pencilMarks = Array<MutableSet<Int>>(81) { mutableSetOf() }

        val pj = PuzzleJson.fromGameState(values, fixed, solution, pencilMarks, Difficulty.HARD)

        // puzzle reflects user entry
        assertEquals('2', pj.puzzle[0])
        // givens does not — cell 0 is not a given
        assertEquals('0', pj.givens[0])
        assertEquals(sampleSolution, pj.solution)
        assertEquals("Hard", pj.difficulty)
        assertTrue(pj.candidates.isEmpty())
    }

    @Test
    fun testEmptyCandidatesRoundTrip() {
        val values = IntArray(81) { sampleGivens[it] - '0' }
        val fixed = BooleanArray(81) { values[it] != 0 }
        val solution = IntArray(81)
        val pencilMarks = Array<MutableSet<Int>>(81) { mutableSetOf() }

        val pj = PuzzleJson.fromGameState(values, fixed, solution, pencilMarks, Difficulty.EASY)
        val json = pj.toJson()
        val parsed = PuzzleJson.fromJson(json)

        assertTrue(parsed.candidates.isEmpty())
        // Solution all zeros
        assertTrue(parsed.solution.all { it == '0' })
        assertEquals("Easy", parsed.difficulty)
    }

    @Test
    fun testLoadFromPuzzleJson() {
        val values = IntArray(81) { sampleGivens[it] - '0' }
        val fixed = BooleanArray(81) { values[it] != 0 }
        val solution = IntArray(81) { sampleSolution[it] - '0' }
        val pencilMarks = Array<MutableSet<Int>>(81) { mutableSetOf() }

        // Determine valid candidates for cell 0 from constraint propagation
        val refBoard = Board()
        refBoard.loadFromString(sampleGivens)
        val validCands = Board.POSSIBLE_VALUES[refBoard.cells[0]].toList()
        // Use a subset (first two) as pencil marks
        val subset = validCands.take(2).toMutableSet()
        pencilMarks[0] = subset

        val pj = PuzzleJson.fromGameState(values, fixed, solution, pencilMarks, Difficulty.HARD)
        val board = Board()
        board.loadFromPuzzleJson(pj)

        // Givens are fixed
        for (i in 0 until 81) {
            if (sampleGivens[i] != '0') {
                assertTrue(board.fixed[i], "Cell $i should be fixed")
                assertEquals(sampleGivens[i] - '0', board.values[i])
            }
        }

        // Solution loaded
        for (i in 0 until 81) {
            assertEquals(sampleSolution[i] - '0', board.solution[i])
        }
        assertTrue(board.solutionSet)

        // Candidates restricted for cell 0 to the subset
        for (d in subset) {
            assertTrue(board.isCandidate(0, d), "Cell 0 should have candidate $d")
        }
        for (d in 1..9) {
            if (d !in subset) {
                assertFalse(board.isCandidate(0, d), "Cell 0 should not have candidate $d")
            }
        }
    }

    @Test
    fun testJsonContainsExpectedStructure() {
        val values = IntArray(81) { sampleGivens[it] - '0' }
        val fixed = BooleanArray(81) { values[it] != 0 }
        val solution = IntArray(81) { sampleSolution[it] - '0' }
        val pencilMarks = Array<MutableSet<Int>>(81) { mutableSetOf() }
        pencilMarks[3] = mutableSetOf(4, 7)

        val json = PuzzleJson.fromGameState(values, fixed, solution, pencilMarks, Difficulty.MEDIUM).toJson()

        assertTrue(json.contains("\"puzzle\":"))
        assertTrue(json.contains("\"givens\":"))
        assertTrue(json.contains("\"solution\":"))
        assertTrue(json.contains("\"difficulty\": \"Medium\""))
        assertTrue(json.contains("\"3\": [4,7]"))
    }
}
