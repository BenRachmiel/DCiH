package sudoku.app.game

import sudoku.core.model.Difficulty

sealed class GameAction {
    data class SelectCell(val row: Int, val col: Int) : GameAction()
    data class EnterDigit(val digit: Int) : GameAction()
    data object Erase : GameAction()
    data object TogglePencilMode : GameAction()
    data object Undo : GameAction()
    data object Redo : GameAction()
    data object ToggleErrorChecking : GameAction()
    data class NewGame(val difficulty: Difficulty) : GameAction()
    data object ShowNewGameDialog : GameAction()
    data object DismissNewGameDialog : GameAction()
    data object DismissWinDialog : GameAction()
    data object FillAllCandidates : GameAction()
    data class ToggleFilterDigit(val digit: Int) : GameAction()
    data class DoubleTapCell(val row: Int, val col: Int) : GameAction()
    data object ToggleBivalueHighlight : GameAction()
    data object ToggleTrovalueHighlight : GameAction()
    data object TogglePeerHighlight : GameAction()
    data class DragSelectCells(val cells: Set<Int>) : GameAction()
}
