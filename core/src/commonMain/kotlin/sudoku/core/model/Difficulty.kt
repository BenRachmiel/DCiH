package sudoku.core.model

enum class Difficulty(val label: String, val maxScore: Int) {
    EASY("Easy", 800),
    MEDIUM("Medium", 1000),
    HARD("Hard", 1600),
    UNFAIR("Unfair", 1800),
    EXTREME("Extreme", Int.MAX_VALUE);

    companion object {
        fun fromScore(score: Int): Difficulty = when {
            score < EASY.maxScore -> EASY
            score < MEDIUM.maxScore -> MEDIUM
            score < HARD.maxScore -> HARD
            score < UNFAIR.maxScore -> UNFAIR
            else -> EXTREME
        }
    }
}
