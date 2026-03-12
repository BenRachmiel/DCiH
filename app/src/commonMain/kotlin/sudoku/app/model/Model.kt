package sudoku.app.model

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    UNFAIR("Unfair"),
    EXTREME("Extreme"),
}

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

enum class SolutionType(
    val displayName: String,
    val category: StrategyCategory,
    val difficulty: Difficulty,
) {
    FULL_HOUSE("Full House", StrategyCategory.SINGLES, Difficulty.EASY),
    NAKED_SINGLE("Naked Single", StrategyCategory.SINGLES, Difficulty.EASY),
    HIDDEN_SINGLE("Hidden Single", StrategyCategory.SINGLES, Difficulty.EASY),

    LOCKED_CANDIDATES_1("Locked Candidates (Pointing)", StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_CANDIDATES_2("Locked Candidates (Claiming)", StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_PAIR("Locked Pair", StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_TRIPLE("Locked Triple", StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),

    NAKED_PAIR("Naked Pair", StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    NAKED_TRIPLE("Naked Triple", StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    NAKED_QUADRUPLE("Naked Quadruple", StrategyCategory.SUBSETS, Difficulty.HARD),
    HIDDEN_PAIR("Hidden Pair", StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    HIDDEN_TRIPLE("Hidden Triple", StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    HIDDEN_QUADRUPLE("Hidden Quadruple", StrategyCategory.SUBSETS, Difficulty.HARD),

    X_WING("X-Wing", StrategyCategory.FISH, Difficulty.HARD),
    SWORDFISH("Swordfish", StrategyCategory.FISH, Difficulty.HARD),
    JELLYFISH("Jellyfish", StrategyCategory.FISH, Difficulty.HARD),

    SKYSCRAPER("Skyscraper", StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    TWO_STRING_KITE("2-String Kite", StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    EMPTY_RECTANGLE("Empty Rectangle", StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    TURBOT_FISH("Turbot Fish", StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),

    XY_WING("XY-Wing", StrategyCategory.WINGS, Difficulty.HARD),
    XYZ_WING("XYZ-Wing", StrategyCategory.WINGS, Difficulty.HARD),
    W_WING("W-Wing", StrategyCategory.WINGS, Difficulty.HARD),
    REMOTE_PAIR("Remote Pair", StrategyCategory.WINGS, Difficulty.MEDIUM),

    SIMPLE_COLORS_TRAP("Simple Colors (Trap)", StrategyCategory.COLORING, Difficulty.UNFAIR),
    SIMPLE_COLORS_WRAP("Simple Colors (Wrap)", StrategyCategory.COLORING, Difficulty.UNFAIR),
    MULTI_COLORS_1("Multi Colors 1", StrategyCategory.COLORING, Difficulty.UNFAIR),
    MULTI_COLORS_2("Multi Colors 2", StrategyCategory.COLORING, Difficulty.UNFAIR),

    X_CHAIN("X-Chain", StrategyCategory.CHAINS, Difficulty.EXTREME),
    XY_CHAIN("XY-Chain", StrategyCategory.CHAINS, Difficulty.EXTREME),

    BRUTE_FORCE("Brute Force", StrategyCategory.BRUTE_FORCE, Difficulty.EXTREME),
    ;

    val isSingle: Boolean
        get() = this == FULL_HOUSE || this == NAKED_SINGLE || this == HIDDEN_SINGLE

    /** Whether this technique has a working solver implementation. */
    val hasSolver: Boolean
        get() =
            when (this) {
                FULL_HOUSE, NAKED_SINGLE, HIDDEN_SINGLE,
                LOCKED_CANDIDATES_1, LOCKED_CANDIDATES_2,
                LOCKED_PAIR, LOCKED_TRIPLE,
                NAKED_PAIR, NAKED_TRIPLE, NAKED_QUADRUPLE,
                HIDDEN_PAIR, HIDDEN_TRIPLE, HIDDEN_QUADRUPLE,
                X_WING, SWORDFISH, JELLYFISH,
                SKYSCRAPER, TWO_STRING_KITE, EMPTY_RECTANGLE, TURBOT_FISH,
                XY_WING, XYZ_WING, W_WING, REMOTE_PAIR,
                SIMPLE_COLORS_TRAP, SIMPLE_COLORS_WRAP,
                MULTI_COLORS_1, MULTI_COLORS_2,
                -> true

                else -> false
            }
}

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

data class SolutionStep(
    val type: SolutionType,
    val cellIndex: Int = -1,
    val value: Int = 0,
    val indices: List<Int> = emptyList(),
    val candidatesRemoved: List<Pair<Int, Int>> = emptyList(),
) {
    /** Vague hint: just the technique name. */
    fun describeVague(): String = type.displayName

    /** Concrete hint: technique name + relevant cells/digits/location. */
    fun describeConcrete(): String {
        fun cellRef(idx: Int) = "r${idx / 9 + 1}c${idx % 9 + 1}"
        fun boxOf(idx: Int) = (idx / 9 / 3) * 3 + (idx % 9 / 3) + 1

        return when (type) {
            SolutionType.FULL_HOUSE, SolutionType.NAKED_SINGLE, SolutionType.HIDDEN_SINGLE -> {
                "${type.displayName}: $value in ${cellRef(cellIndex)}"
            }

            SolutionType.LOCKED_CANDIDATES_1, SolutionType.LOCKED_CANDIDATES_2 -> {
                val box = if (indices.isNotEmpty()) boxOf(indices[0]) else 0
                "${type.displayName}: digit $value in box $box"
            }

            SolutionType.LOCKED_PAIR, SolutionType.LOCKED_TRIPLE -> {
                val cells = indices.joinToString(",") { cellRef(it) }
                "${type.displayName}: $cells"
            }

            SolutionType.NAKED_PAIR, SolutionType.NAKED_TRIPLE, SolutionType.NAKED_QUADRUPLE -> {
                val digits = candidatesRemoved.map { it.second }.distinct().sorted()
                val cells = indices.joinToString(",") { cellRef(it) }
                "${type.displayName}: {${digits.joinToString(",")}} in $cells"
            }

            SolutionType.HIDDEN_PAIR, SolutionType.HIDDEN_TRIPLE, SolutionType.HIDDEN_QUADRUPLE -> {
                val cells = indices.joinToString(",") { cellRef(it) }
                "${type.displayName}: in $cells"
            }

            SolutionType.X_WING, SolutionType.SWORDFISH, SolutionType.JELLYFISH -> {
                val rows = indices.map { it / 9 + 1 }.distinct().sorted()
                val cols = indices.map { it % 9 + 1 }.distinct().sorted()
                "${type.displayName}: digit $value in r${rows.joinToString(",")}/c${cols.joinToString(",")}"
            }

            SolutionType.SKYSCRAPER, SolutionType.TWO_STRING_KITE, SolutionType.TURBOT_FISH -> {
                if (indices.size == 4) {
                    "${type.displayName}: digit $value, ends ${cellRef(indices[0])},${cellRef(indices[3])}"
                } else {
                    "${type.displayName}: digit $value"
                }
            }

            SolutionType.EMPTY_RECTANGLE -> {
                if (indices.size > 2) {
                    val box = boxOf(indices[2])
                    "${type.displayName}: digit $value in box $box"
                } else {
                    "${type.displayName}: digit $value"
                }
            }

            SolutionType.XY_WING, SolutionType.XYZ_WING -> {
                if (indices.isNotEmpty()) {
                    "${type.displayName}: pivot ${cellRef(indices[0])}"
                } else {
                    type.displayName
                }
            }

            SolutionType.W_WING -> {
                if (indices.size >= 2) {
                    "${type.displayName}: ${cellRef(indices[0])},${cellRef(indices[1])}"
                } else {
                    type.displayName
                }
            }

            SolutionType.REMOTE_PAIR -> {
                val digits = candidatesRemoved.map { it.second }.distinct().sorted()
                "${type.displayName}: {${digits.joinToString(",")}} chain of ${indices.size}"
            }

            SolutionType.SIMPLE_COLORS_TRAP -> {
                val elimCell = candidatesRemoved.firstOrNull()?.first
                if (elimCell != null) {
                    "${type.displayName}: digit $value, ${cellRef(elimCell)} sees both colors"
                } else {
                    "${type.displayName}: digit $value"
                }
            }

            SolutionType.SIMPLE_COLORS_WRAP -> {
                "${type.displayName}: digit $value, color contradicts itself"
            }

            SolutionType.MULTI_COLORS_1, SolutionType.MULTI_COLORS_2 -> {
                "${type.displayName}: digit $value, ${candidatesRemoved.size} eliminations"
            }

            SolutionType.BRUTE_FORCE -> {
                if (cellIndex >= 0) {
                    "${type.displayName}: $value in ${cellRef(cellIndex)}"
                } else {
                    type.displayName
                }
            }

            else -> type.displayName
        }
    }
}

/**
 * A board snapshot for strategy illustration.
 *
 * @param puzzle 81-char string (digits 1-9 for givens, '0' or '.' for empty).
 * @param candidateMasks Explicit candidate bitmasks per cell (9-bit, bit 0 = digit 1).
 * @param highlights Per-candidate colour annotations.
 * @param cellHighlights Whole-cell indices to shade (e.g. locked-candidate box).
 */
data class BoardExample(
    val puzzle: String,
    val candidateMasks: IntArray,
    val highlights: List<CandidateHighlight> = emptyList(),
    val cellHighlights: Set<Int> = emptySet(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardExample) return false
        return puzzle == other.puzzle &&
                candidateMasks.contentEquals(other.candidateMasks) &&
                highlights == other.highlights &&
                cellHighlights == other.cellHighlights
    }

    override fun hashCode(): Int {
        var result = puzzle.hashCode()
        result = 31 * result + candidateMasks.contentHashCode()
        result = 31 * result + highlights.hashCode()
        result = 31 * result + cellHighlights.hashCode()
        return result
    }
}

/**
 * Wiki entry for a single solving technique.
 */
data class StrategyEntry(
    val type: SolutionType,
    val theory: String,
    val howToSpot: String,
    val example: BoardExample? = null,
    val relatedTypes: List<SolutionType> = emptyList(),
    val keywords: List<String> = emptyList(),
)
