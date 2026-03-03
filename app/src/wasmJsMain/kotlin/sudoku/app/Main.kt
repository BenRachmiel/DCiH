package sudoku.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import sudoku.app.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.title = "Don't Call it Hodoku"
    val body = document.body ?: return
    ComposeViewport(body) {
        App()
    }
}
