package sudoku.core.model

enum class SolutionType(
    val displayName: String,
    val score: Int,
    val category: StrategyCategory,
    val difficulty: Difficulty,
) {
    FULL_HOUSE("Full House", 4, StrategyCategory.SINGLES, Difficulty.EASY),
    NAKED_SINGLE("Naked Single", 4, StrategyCategory.SINGLES, Difficulty.EASY),
    HIDDEN_SINGLE("Hidden Single", 14, StrategyCategory.SINGLES, Difficulty.EASY),

    LOCKED_CANDIDATES_1("Locked Candidates (Pointing)", 50, StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_CANDIDATES_2("Locked Candidates (Claiming)", 50, StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_PAIR("Locked Pair", 40, StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),
    LOCKED_TRIPLE("Locked Triple", 60, StrategyCategory.LOCKED_CANDIDATES, Difficulty.MEDIUM),

    NAKED_PAIR("Naked Pair", 60, StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    NAKED_TRIPLE("Naked Triple", 80, StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    NAKED_QUADRUPLE("Naked Quadruple", 120, StrategyCategory.SUBSETS, Difficulty.HARD),
    HIDDEN_PAIR("Hidden Pair", 70, StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    HIDDEN_TRIPLE("Hidden Triple", 100, StrategyCategory.SUBSETS, Difficulty.MEDIUM),
    HIDDEN_QUADRUPLE("Hidden Quadruple", 150, StrategyCategory.SUBSETS, Difficulty.HARD),

    X_WING("X-Wing", 140, StrategyCategory.FISH, Difficulty.HARD),
    SWORDFISH("Swordfish", 150, StrategyCategory.FISH, Difficulty.HARD),
    JELLYFISH("Jellyfish", 160, StrategyCategory.FISH, Difficulty.HARD),

    SKYSCRAPER("Skyscraper", 130, StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    TWO_STRING_KITE("2-String Kite", 150, StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    EMPTY_RECTANGLE("Empty Rectangle", 120, StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),
    TURBOT_FISH("Turbot Fish", 120, StrategyCategory.SINGLE_DIGIT, Difficulty.HARD),

    XY_WING("XY-Wing", 160, StrategyCategory.WINGS, Difficulty.HARD),
    XYZ_WING("XYZ-Wing", 180, StrategyCategory.WINGS, Difficulty.HARD),
    W_WING("W-Wing", 150, StrategyCategory.WINGS, Difficulty.HARD),
    REMOTE_PAIR("Remote Pair", 110, StrategyCategory.WINGS, Difficulty.MEDIUM),

    SIMPLE_COLORS_TRAP("Simple Colors (Trap)", 150, StrategyCategory.COLORING, Difficulty.UNFAIR),
    SIMPLE_COLORS_WRAP("Simple Colors (Wrap)", 150, StrategyCategory.COLORING, Difficulty.UNFAIR),
    MULTI_COLORS_1("Multi Colors 1", 200, StrategyCategory.COLORING, Difficulty.UNFAIR),
    MULTI_COLORS_2("Multi Colors 2", 200, StrategyCategory.COLORING, Difficulty.UNFAIR),

    X_CHAIN("X-Chain", 260, StrategyCategory.CHAINS, Difficulty.EXTREME),
    XY_CHAIN("XY-Chain", 260, StrategyCategory.CHAINS, Difficulty.EXTREME),

    BRUTE_FORCE("Brute Force", 10000, StrategyCategory.BRUTE_FORCE, Difficulty.EXTREME),
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
                XY_WING, XYZ_WING, W_WING,
                SIMPLE_COLORS_TRAP, SIMPLE_COLORS_WRAP,
                -> true

                else -> false
            }
}
