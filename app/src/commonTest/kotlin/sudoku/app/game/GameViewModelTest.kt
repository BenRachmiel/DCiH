package sudoku.app.game

import kotlin.test.*

class GameViewModelTest {

    private fun viewModelWithBoard(): GameViewModel {
        val vm = GameViewModel()
        // Set up a minimal board so we can test interactions
        val values = IntArray(81)
        val fixed = BooleanArray(81)
        val solution = IntArray(81) { it % 9 + 1 } // dummy solution
        values[0] = 5
        fixed[0] = true
        vm.onAction(GameAction.DismissNewGameDialog)
        // Inject state directly via actions — select a cell to verify highlights
        return vm
    }

    @Test
    fun peerHighlightDefaultsToTrue() {
        val vm = GameViewModel()
        assertTrue(vm.state.value.peerHighlight)
    }

    @Test
    fun togglePeerHighlightFlipsState() {
        val vm = GameViewModel()
        assertTrue(vm.state.value.peerHighlight)

        vm.onAction(GameAction.TogglePeerHighlight)
        assertFalse(vm.state.value.peerHighlight)

        vm.onAction(GameAction.TogglePeerHighlight)
        assertTrue(vm.state.value.peerHighlight)
    }

    @Test
    fun togglePeerHighlightDoesNotAffectOtherHighlights() {
        val vm = GameViewModel()
        val before = vm.state.value

        vm.onAction(GameAction.TogglePeerHighlight)
        val after = vm.state.value

        assertEquals(before.bivalueHighlight, after.bivalueHighlight)
        assertEquals(before.trivalueHighlight, after.trivalueHighlight)
        assertEquals(before.filterDigit, after.filterDigit)
    }

    @Test
    fun peerHighlightIncludedInEquality() {
        val a = GameState(peerHighlight = true)
        val b = GameState(peerHighlight = false)
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun peerHighlightPreservedAcrossOtherToggles() {
        val vm = GameViewModel()
        vm.onAction(GameAction.TogglePeerHighlight)
        assertFalse(vm.state.value.peerHighlight)

        // Toggle pencil mode — peerHighlight should stay off
        vm.onAction(GameAction.TogglePencilMode)
        assertFalse(vm.state.value.peerHighlight)

        // Toggle error checking — peerHighlight should stay off
        vm.onAction(GameAction.ToggleErrorChecking)
        assertFalse(vm.state.value.peerHighlight)
    }
}
