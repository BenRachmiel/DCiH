package sudoku.core

import sudoku.core.model.*
import kotlin.test.*

class BoardTest {

    @Test
    fun testEmptyBoard() {
        val board = Board()
        assertEquals(81, board.unsolvedCellsAnz)
        assertFalse(board.isSolved)
        for (i in 0 until Board.LENGTH) {
            assertEquals(0, board.values[i])
            assertEquals(Board.MAX_MASK, board.cells[i])
        }
    }

    @Test
    fun testSetCell() {
        val board = Board()
        val valid = board.setCell(0, 5)
        assertTrue(valid)
        assertEquals(5, board.values[0])
        assertEquals(0, board.cells[0])
        assertEquals(80, board.unsolvedCellsAnz)

        // Check that 5 is removed from buddies
        for (buddy in Board.BUDDIES_ARRAY[0]) {
            assertFalse(board.isCandidate(buddy, 5), "Cell $buddy should not have candidate 5")
        }

        // Non-buddies should still have candidate 5
        val nonBuddies = (0 until 81).filter { it !in Board.BUDDIES_ARRAY[0] && it != 0 }
        for (cell in nonBuddies) {
            assertTrue(board.isCandidate(cell, 5), "Cell $cell should still have candidate 5")
        }
    }

    @Test
    fun testLookupTables() {
        // ROWS
        assertEquals(0, Board.ROWS[0][0])
        assertEquals(8, Board.ROWS[0][8])
        assertEquals(72, Board.ROWS[8][0])
        assertEquals(80, Board.ROWS[8][8])

        // COLS
        assertEquals(0, Board.COLS[0][0])
        assertEquals(72, Board.COLS[0][8])
        assertEquals(8, Board.COLS[8][0])
        assertEquals(80, Board.COLS[8][8])

        // BLOCKS
        assertEquals(0, Board.BLOCKS[0][0])
        assertContains(Board.BLOCKS[0].toList(), 10)
        assertContains(Board.BLOCKS[0].toList(), 20)

        // CONSTRAINTS
        assertEquals(0, Board.CONSTRAINTS[0][0]) // row 0
        assertEquals(9, Board.CONSTRAINTS[0][1]) // col 0
        assertEquals(18, Board.CONSTRAINTS[0][2]) // block 0

        assertEquals(8, Board.CONSTRAINTS[80][0]) // row 8
        assertEquals(17, Board.CONSTRAINTS[80][1]) // col 8
        assertEquals(26, Board.CONSTRAINTS[80][2]) // block 8

        // MASKS
        assertEquals(0x001, Board.MASKS[1])
        assertEquals(0x100, Board.MASKS[9])

        // ANZ_VALUES
        assertEquals(0, Board.ANZ_VALUES[0])
        assertEquals(1, Board.ANZ_VALUES[1])
        assertEquals(2, Board.ANZ_VALUES[3])
        assertEquals(9, Board.ANZ_VALUES[0x1ff])

        // POSSIBLE_VALUES
        assertContentEquals(intArrayOf(1), Board.POSSIBLE_VALUES[1])
        assertContentEquals(intArrayOf(1, 2), Board.POSSIBLE_VALUES[3])
    }

    @Test
    fun testBuddies() {
        // Cell 0 (row 0, col 0, block 0) should have 20 buddies
        assertEquals(20, Board.BUDDIES_ARRAY[0].size)
        // Cell 40 (center, row 4, col 4, block 4) should have 20 buddies
        assertEquals(20, Board.BUDDIES_ARRAY[40].size)
    }

    @Test
    fun testLoadFromString() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = Board()
        board.loadFromString(puzzle)

        assertEquals(5, board.values[0])
        assertEquals(3, board.values[1])
        assertTrue(board.fixed[0])
        assertTrue(board.fixed[1])
        assertEquals(0, board.values[2])
        assertFalse(board.fixed[2])
    }

    @Test
    fun testCopyFrom() {
        val board = Board()
        board.setCell(0, 5, isFixed = true)
        board.setCell(10, 3)

        val copy = Board()
        copy.copyFrom(board)

        assertEquals(board.values[0], copy.values[0])
        assertEquals(board.values[10], copy.values[10])
        assertEquals(board.cells[1], copy.cells[1])
        assertTrue(copy.fixed[0])
    }

    @Test
    fun testSetAllExposedSingles() {
        // A nearly-complete row should trigger singles
        val board = Board()
        // Set row 0 to 1,2,3,4,5,6,7,8,_ (missing 9)
        for (i in 0..7) {
            board.setCell(i, i + 1, isFixed = true)
        }
        board.setAllExposedSingles()
        // Cell 8 should be set to 9
        assertEquals(9, board.values[8])
    }

    @Test
    fun testCellSet() {
        val set = CellSet()
        assertTrue(set.isEmpty)

        set.add(0)
        set.add(40)
        set.add(80)

        assertFalse(set.isEmpty)
        assertTrue(0 in set)
        assertTrue(40 in set)
        assertTrue(80 in set)
        assertFalse(1 in set)
        assertEquals(3, set.size())

        set.remove(40)
        assertFalse(40 in set)
        assertEquals(2, set.size())

        val arr = set.toArray()
        assertEquals(2, arr.size)
        assertEquals(0, arr[0])
        assertEquals(80, arr[1])
    }
}
