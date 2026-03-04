package sudoku.app.game

import sudoku.core.model.CandidateHighlight
import sudoku.core.model.Difficulty
import sudoku.core.model.SolutionStep

data class GameState(
    /** Cell values (0=empty, 1-9=set). Givens and user entries combined. */
    val values: IntArray = IntArray(81),
    /** Which cells are givens (immutable during play) */
    val fixed: BooleanArray = BooleanArray(81),
    /** The correct solution for error checking */
    val solution: IntArray = IntArray(81),
    /** Pencil marks: for each cell, a set of candidate digits */
    val pencilMarks: Array<MutableSet<Int>> = Array(81) { mutableSetOf() },
    /** Monotonic counter so MutableStateFlow detects pencil mark changes */
    val pencilMarkVersion: Int = 0,
    /** Currently selected cell (-1 = none) */
    val selectedRow: Int = -1,
    val selectedCol: Int = -1,
    /** Pencil mark mode active */
    val pencilMode: Boolean = false,
    /** Error checking enabled */
    val errorChecking: Boolean = true,
    /** Game difficulty */
    val difficulty: Difficulty = Difficulty.EASY,
    /** Timer */
    val elapsedSeconds: Long = 0L,
    /** Puzzle completed */
    val isWon: Boolean = false,
    /** Dialog states */
    val showNewGameDialog: Boolean = false,
    val showWinDialog: Boolean = false,
    /** Is generating a new puzzle */
    val isGenerating: Boolean = false,
    /** Candidate filter digit (0 = off, 1-9 = active filter) */
    val filterDigit: Int = 0,
    /** Bivalue cell highlighting enabled */
    val bivalueHighlight: Boolean = false,
    /** Trivalue cell highlighting enabled */
    val trivalueHighlight: Boolean = false,
    /** Peer (row/col/box) shading on selected cell */
    val peerHighlight: Boolean = true,
    /** Number of incorrect digits placed */
    val errorCount: Int = 0,
    /** Cells selected via click-and-drag (indices 0-80) */
    val multiSelectedCells: Set<Int> = emptySet(),
    /** Current hint progression level (0=none, 1=vague, 2=concrete, 3=full) */
    val hintLevel: Int = 0,
    /** Cached solver step for the current hint */
    val hintStep: SolutionStep? = null,
    /** Candidate highlights for hint level 3 (full visualization) */
    val hintHighlights: List<CandidateHighlight> = emptyList(),
    /** Total hints used this game */
    val hintCount: Int = 0,
) {
    val selectedIndex: Int get() = if (selectedRow >= 0 && selectedCol >= 0) selectedRow * 9 + selectedCol else -1

    fun isError(index: Int): Boolean =
        errorChecking && values[index] != 0 && !fixed[index] &&
            solution[index] != 0 && values[index] != solution[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return values.contentEquals(other.values) &&
            fixed.contentEquals(other.fixed) &&
            pencilMarkVersion == other.pencilMarkVersion &&
            selectedRow == other.selectedRow &&
            selectedCol == other.selectedCol &&
            pencilMode == other.pencilMode &&
            errorChecking == other.errorChecking &&
            difficulty == other.difficulty &&
            elapsedSeconds == other.elapsedSeconds &&
            isWon == other.isWon &&
            showNewGameDialog == other.showNewGameDialog &&
            showWinDialog == other.showWinDialog &&
            isGenerating == other.isGenerating &&
            filterDigit == other.filterDigit &&
            bivalueHighlight == other.bivalueHighlight &&
            trivalueHighlight == other.trivalueHighlight &&
            peerHighlight == other.peerHighlight &&
            errorCount == other.errorCount &&
            multiSelectedCells == other.multiSelectedCells &&
            hintLevel == other.hintLevel &&
            hintStep == other.hintStep &&
            hintHighlights == other.hintHighlights &&
            hintCount == other.hintCount
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        result = result * 31 + pencilMarkVersion
        result = result * 31 + selectedRow
        result = result * 31 + selectedCol
        result = result * 31 + filterDigit
        result = result * 31 + if (bivalueHighlight) 1 else 0
        result = result * 31 + if (trivalueHighlight) 1 else 0
        result = result * 31 + if (peerHighlight) 1 else 0
        result = result * 31 + errorCount
        result = result * 31 + multiSelectedCells.hashCode()
        result = result * 31 + hintLevel
        result = result * 31 + hintCount
        return result
    }
}

data class UndoEntry(
    val values: IntArray,
    val pencilMarks: Array<Set<Int>>,
) {
    override fun equals(other: Any?): Boolean = other is UndoEntry && values.contentEquals(other.values)

    override fun hashCode(): Int = values.contentHashCode()
}
