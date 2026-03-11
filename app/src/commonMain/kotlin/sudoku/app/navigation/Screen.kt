package sudoku.app.navigation

import sudoku.app.model.SolutionType

sealed class Screen {
    data object Home : Screen()
    data object Play : Screen()
    data class Learn(val technique: SolutionType? = null) : Screen()
    data class Practice(val technique: SolutionType? = null) : Screen()
}
