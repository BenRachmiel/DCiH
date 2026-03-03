package sudoku.core.model

/** Semantic role for per-candidate highlighting in strategy examples. */
enum class HighlightRole {
    /** Candidates forming the core pattern (e.g. X-Wing corners). */
    DEFINING,
    /** Candidates eliminated by this technique. */
    ELIMINATION,
    /** Structural support cells (e.g. pivot in XY-Wing). */
    SECONDARY,
    /** Auxiliary highlights. */
    TERTIARY,
    /** First chain color (coloring techniques). */
    COLOR_A,
    /** Second chain color (coloring techniques). */
    COLOR_B,
}

/** A single highlighted candidate digit within a cell. */
data class CandidateHighlight(
    val cellIndex: Int,
    val value: Int,
    val role: HighlightRole,
)
