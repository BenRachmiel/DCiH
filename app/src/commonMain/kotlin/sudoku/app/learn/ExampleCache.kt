package sudoku.app.learn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sudoku.app.engine.NativeEngine
import sudoku.app.model.BoardExample
import sudoku.app.model.SolutionType

/**
 * On-demand example regeneration cache.
 * Pre-baked examples live in StrategyContent — this only stores on-demand overrides.
 */
object ExampleCache {
    private val _examples = MutableStateFlow<Map<SolutionType, BoardExample>>(emptyMap())
    val examples: StateFlow<Map<SolutionType, BoardExample>> = _examples.asStateFlow()

    private val _generating = MutableStateFlow<SolutionType?>(null)
    val generating: StateFlow<SolutionType?> = _generating.asStateFlow()

    /** True when the last generation attempt failed to find an example. */
    private val _lastFailed = MutableStateFlow(false)
    val lastFailed: StateFlow<Boolean> = _lastFailed.asStateFlow()

    private const val ATTEMPTS_PER_ROUND = 500
    private const val MAX_ROUNDS = 5

    /**
     * Generate a fresh example for one technique in the background.
     * Retries up to [MAX_ROUNDS] rounds of [ATTEMPTS_PER_ROUND] attempts each.
     * Sets [lastFailed] if all rounds fail.
     */
    fun regenerate(
        type: SolutionType,
        scope: CoroutineScope,
    ) {
        if (_generating.value != null) return
        _lastFailed.value = false
        scope.launch(Dispatchers.Default) {
            _generating.value = type
            try {
                for (round in 0 until MAX_ROUNDS) {
                    val example = NativeEngine.generateExample(type, ATTEMPTS_PER_ROUND)
                    if (example != null) {
                        _examples.value = _examples.value + (type to example)
                        return@launch
                    }
                }
                _lastFailed.value = true
            } finally {
                _generating.value = null
            }
        }
    }
}
