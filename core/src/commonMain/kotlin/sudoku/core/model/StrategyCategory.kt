package sudoku.core.model

/** Grouping category for Sudoku solving techniques. */
enum class StrategyCategory(val displayName: String) {
    SINGLES("Singles"),
    LOCKED_CANDIDATES("Locked Candidates"),
    SUBSETS("Subsets"),
    FISH("Basic Fish"),
    SINGLE_DIGIT("Single-Digit Patterns"),
    WINGS("Wings"),
    COLORING("Coloring"),
    CHAINS("Chains"),
    BRUTE_FORCE("Brute Force"),
}
