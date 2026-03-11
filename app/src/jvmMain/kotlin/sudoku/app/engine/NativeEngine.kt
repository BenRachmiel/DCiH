package sudoku.app.engine

import sudoku.app.game.PencilMarkErrors
import sudoku.app.model.BoardExample
import sudoku.app.model.CandidateHighlight
import sudoku.app.model.Difficulty
import sudoku.app.model.HighlightRole
import sudoku.app.model.SolutionStep
import sudoku.app.model.SolutionType
import sudoku.engine.uniffi.FfiSolutionStep
import sudoku.engine.uniffi.Difficulty as FfiDifficulty
import sudoku.engine.uniffi.HighlightRole as FfiHighlightRole
import sudoku.engine.uniffi.SolutionType as FfiSolutionType

actual object NativeEngine {

    actual fun ensureLoaded() {
        // Pre-extract the native library so JNA can find it.
        // On Android, JNA uses System.loadLibrary which finds jniLibs automatically.
        // On Desktop, we extract from classpath resources to a temp dir and set the search path.
        if (!loaded) {
            val libName = System.mapLibraryName("sudoku_core")
            val stream = NativeEngine::class.java.getResourceAsStream("/native/$libName")
            if (stream != null) {
                val tempDir = java.nio.file.Files.createTempDirectory("sudoku-native")
                val tempFile = tempDir.resolve(libName).toFile()
                tempFile.deleteOnExit()
                tempDir.toFile().deleteOnExit()
                stream.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                // Tell JNA where to find the library
                System.setProperty("jna.library.path", tempDir.toString())
            }
            // Trigger the lazy load inside generated bindings by calling a cheap function
            sudoku.engine.uniffi.computeAllCandidates(ByteArray(81))
            loaded = true
        }
    }

    private var loaded = false

    actual fun generatePuzzle(difficulty: Difficulty, maxRetries: Int): GenerateResult {
        ensureLoaded()
        val r = sudoku.engine.uniffi.generatePuzzle(difficulty.toFfi(), maxRetries)
        return GenerateResult(
            puzzle = r.puzzle,
            solution = r.solution.map { it.toInt() }.toIntArray(),
            difficulty = r.difficulty.toApp(),
            score = r.score,
        )
    }

    actual fun findNextStep(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        maxDifficulty: Difficulty?,
    ): SolutionStep? {
        ensureLoaded()
        val (byteValues, candMasks, byteSolution) = buildArrays(values, pencilMarks, solution)
        val step = sudoku.engine.uniffi.findNextStep(
            byteValues, candMasks, byteSolution, maxDifficulty?.toFfi(),
        ) ?: return null
        return step.toApp()
    }

    actual fun buildHighlights(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
        step: SolutionStep,
    ): List<CandidateHighlight> {
        ensureLoaded()
        val (byteValues, candMasks, byteSolution) = buildArrays(values, pencilMarks, solution)
        return sudoku.engine.uniffi.buildStepHighlights(
            byteValues, candMasks, byteSolution, step.toFfi(),
        ).map { it.toApp() }
    }

    actual fun generateExample(type: SolutionType, maxAttempts: Int): BoardExample? {
        ensureLoaded()
        val ex = sudoku.engine.uniffi.generateBoardExample(
            type.ordinal.toUByte(), maxAttempts,
        ) ?: return null
        return BoardExample(
            puzzle = ex.puzzle,
            candidateMasks = ex.candidateMasks.map { it.toInt() }.toIntArray(),
            highlights = ex.highlights.map { it.toApp() },
        )
    }

    actual fun computeAllCandidates(values: IntArray): Array<MutableSet<Int>> {
        ensureLoaded()
        val byteValues = ByteArray(81) { values[it].toByte() }
        val masks = sudoku.engine.uniffi.computeAllCandidates(byteValues)
        return Array(81) { i ->
            val mask = masks[i].toInt()
            val set = mutableSetOf<Int>()
            for (d in 1..9) {
                if (mask and (1 shl (d - 1)) != 0) set.add(d)
            }
            set
        }
    }

    actual fun findPencilMarkErrors(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
    ): PencilMarkErrors? {
        ensureLoaded()
        val (byteValues, candMasks, byteSolution) = buildArrays(values, pencilMarks, solution)
        val errors = sudoku.engine.uniffi.findPencilMarkErrors(
            byteValues, candMasks, byteSolution,
        ) ?: return null
        return PencilMarkErrors(
            toRemove = errors.toRemove.map { it.cell.toInt() to it.digit.toInt() },
            toAdd = errors.toAdd.map { it.cell.toInt() to it.digit.toInt() },
        )
    }

    actual fun findSingleForCell(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        cellIndex: Int,
    ): Int {
        ensureLoaded()
        val byteValues = ByteArray(81) { values[it].toByte() }
        val candMasks = buildCandMasks(values, pencilMarks)
        return sudoku.engine.uniffi.findSingleForCell(
            byteValues, candMasks, cellIndex.toUInt(),
        ).toInt()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildArrays(
        values: IntArray,
        pencilMarks: Array<out Set<Int>>,
        solution: IntArray,
    ): Triple<ByteArray, List<UShort>, ByteArray> {
        val byteValues = ByteArray(81) { values[it].toByte() }
        val byteSolution = ByteArray(81) { solution[it].toByte() }
        return Triple(byteValues, buildCandMasks(values, pencilMarks), byteSolution)
    }

    private fun buildCandMasks(values: IntArray, pencilMarks: Array<out Set<Int>>): List<UShort> =
        List(81) { i ->
            if (values[i] != 0) {
                0.toUShort()
            } else if (pencilMarks[i].isNotEmpty()) {
                var mask = 0
                for (d in pencilMarks[i]) mask = mask or (1 shl (d - 1))
                mask.toUShort()
            } else {
                val used = BooleanArray(10)
                for (buddy in Buddies.ARRAY[i]) {
                    val v = values[buddy]
                    if (v != 0) used[v] = true
                }
                var mask = 0
                for (d in 1..9) if (!used[d]) mask = mask or (1 shl (d - 1))
                mask.toUShort()
            }
        }

    // ─── Type conversions ────────────────────────────────────────────────────

    private fun Difficulty.toFfi(): FfiDifficulty = FfiDifficulty.entries[ordinal]
    private fun FfiDifficulty.toApp(): Difficulty = Difficulty.entries[ordinal]

    private fun FfiHighlightRole.toApp(): HighlightRole = HighlightRole.entries[ordinal]

    private fun sudoku.engine.uniffi.FfiCandidateHighlight.toApp(): CandidateHighlight =
        CandidateHighlight(cellIndex.toInt(), value.toInt(), role.toApp())

    private fun FfiSolutionType.toApp(): SolutionType = SolutionType.entries[ordinal]

    private fun FfiSolutionStep.toApp(): SolutionStep = SolutionStep(
        type = stepType.toApp(),
        cellIndex = cellIndex,
        value = value.toInt(),
        indices = indices.map { it.toInt() },
        candidatesRemoved = candidatesRemoved.map { it.cell.toInt() to it.digit.toInt() },
    )

    private fun SolutionStep.toFfi(): FfiSolutionStep = FfiSolutionStep(
        stepType = FfiSolutionType.entries[type.ordinal],
        cellIndex = cellIndex,
        value = value.toUByte(),
        indices = indices.map { it.toUInt() },
        candidatesRemoved = candidatesRemoved.map { (c, d) ->
            sudoku.engine.uniffi.FfiCandidateRemoval(c.toUInt(), d.toUByte())
        },
    )
}
