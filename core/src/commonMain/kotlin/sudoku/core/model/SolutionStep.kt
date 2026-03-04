package sudoku.core.model

data class SolutionStep(
    val type: SolutionType,
    val cellIndex: Int = -1,
    val value: Int = 0,
    val indices: List<Int> = emptyList(),
    val candidatesRemoved: List<Pair<Int, Int>> = emptyList(), // (cellIndex, candidate)
) {
    /** Vague hint: just the technique name. */
    fun describeVague(): String = type.displayName

    /** Concrete hint: technique name + relevant cells/digits. */
    fun describeConcrete(): String {
        if (type.isSingle) {
            val r = cellIndex / 9 + 1
            val c = cellIndex % 9 + 1
            return "${type.displayName}: $value in r${r}c$c"
        }
        val digits = candidatesRemoved.map { it.second }.distinct().sorted()
        val digitStr = digits.joinToString(",")
        return if (value != 0) {
            "${type.displayName}: digit $value"
        } else if (digits.isNotEmpty()) {
            "${type.displayName}: digits $digitStr"
        } else {
            type.displayName
        }
    }
}
