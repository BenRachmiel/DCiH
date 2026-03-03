package sudoku.core.model

data class SolutionStep(
    val type: SolutionType,
    val cellIndex: Int = -1,
    val value: Int = 0,
    val indices: List<Int> = emptyList(),
    val candidatesRemoved: List<Pair<Int, Int>> = emptyList() // (cellIndex, candidate)
)
