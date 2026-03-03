package sudoku.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import sudoku.app.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Don't Call it Hodoku",
        state = rememberWindowState(width = 600.dp, height = 900.dp)
    ) {
        App()
    }
}
