package sudoku.app.learn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sudoku.core.generator.ExampleGenerator
import sudoku.core.model.BoardExample
import sudoku.core.model.SolutionType

/**
 * On-demand example regeneration cache.
 * Pre-baked examples live in StrategyContent — this only stores on-demand overrides.
 */
object ExampleCache {
    private val _examples = MutableStateFlow<Map<SolutionType, BoardExample>>(emptyMap())
    val examples: StateFlow<Map<SolutionType, BoardExample>> = _examples.asStateFlow()

    private val _generating = MutableStateFlow<SolutionType?>(null)
    val generating: StateFlow<SolutionType?> = _generating.asStateFlow()

    /**
     * Generate a fresh example for one technique in the background.
     * Result overrides the pre-baked example in the UI.
     */
    fun regenerate(
        type: SolutionType,
        scope: CoroutineScope,
    ) {
        if (_generating.value != null) return
        scope.launch(Dispatchers.Default) {
            _generating.value = type
            val example = ExampleGenerator().generateExample(type)
            if (example != null) {
                _examples.value = _examples.value + (type to example)
            }
            _generating.value = null
        }
    }
}
