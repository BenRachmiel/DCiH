package sudoku.core.model

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
