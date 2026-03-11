package sudoku.engine

import sudoku.core.model.CandidateHighlight
import sudoku.core.model.Difficulty
import sudoku.core.model.HighlightRole
import sudoku.core.model.SolutionStep
import sudoku.core.model.SolutionType

/**
 * JNI bridge to the Rust sudoku-core engine.
 * All methods are blocking — call from a background dispatcher.
 */
object RustEngine {
    private var loaded = false

    fun ensureLoaded() {
        if (!loaded) {
            val libName = System.mapLibraryName("sudoku_core")
            val stream = RustEngine::class.java.getResourceAsStream("/native/$libName")
            if (stream != null) {
                // Desktop: extract from classpath resources to temp dir
                val tempDir = java.nio.file.Files.createTempDirectory("sudoku-native")
                val tempFile = tempDir.resolve(libName).toFile()
                tempFile.deleteOnExit()
                tempDir.toFile().deleteOnExit()
                stream.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                System.load(tempFile.absolutePath)
            } else {
                // Android: .so is in jniLibs, loaded by name
                System.loadLibrary("sudoku_core")
            }
            loaded = true
        }
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    fun generatePuzzle(difficulty: Int, maxRetries: Int): String {
        ensureLoaded()
        return nativeGenerate(difficulty, maxRetries)
    }

    fun findNextStep(
        values: ByteArray,
        candMasks: ShortArray,
        solution: ByteArray,
        maxDiff: Int,
    ): SolutionStep? {
        ensureLoaded()
        val json = nativeFindNextStep(values, candMasks, solution, maxDiff)
        if (json.isEmpty()) return null
        return parseSolutionStep(json)
    }

    fun buildHighlights(
        values: ByteArray,
        candMasks: ShortArray,
        solution: ByteArray,
        step: SolutionStep,
    ): List<CandidateHighlight> {
        ensureLoaded()
        val stepJson = stepToJson(step)
        val json = nativeBuildHighlights(values, candMasks, solution, stepJson)
        return parseHighlights(json)
    }

    // ─── JNI declarations ───────────────────────────────────────────────────

    @JvmStatic
    private external fun nativeGenerate(difficulty: Int, maxRetries: Int): String

    @JvmStatic
    private external fun nativeFindNextStep(
        values: ByteArray,
        candMasks: ShortArray,
        solution: ByteArray,
        maxDiff: Int,
    ): String

    @JvmStatic
    private external fun nativeBuildHighlights(
        values: ByteArray,
        candMasks: ShortArray,
        solution: ByteArray,
        stepJson: String,
    ): String

    @JvmStatic
    private external fun nativeSolve(
        values: ByteArray,
        candMasks: ShortArray,
        solution: ByteArray,
        maxDiff: Int,
    ): String

    @JvmStatic
    private external fun nativeCountSolutions(values: ByteArray, maxCount: Int): Int

    // ─── JSON marshalling ───────────────────────────────────────────────────

    fun parseGenerateResult(json: String): Triple<String, IntArray, Pair<Difficulty, Int>> {
        val puzzle = extractString(json, "puzzle")
        val score = extractInt(json, "score")
        val diffStr = extractString(json, "difficulty")

        val solStart = json.indexOf("\"solution\":[")
        val solution = if (solStart >= 0) {
            val arrStart = json.indexOf('[', solStart) + 1
            val arrEnd = json.indexOf(']', arrStart)
            json.substring(arrStart, arrEnd)
                .split(',')
                .map { it.trim().toInt() }
                .toIntArray()
        } else {
            IntArray(81)
        }

        val difficulty = when (diffStr) {
            "Easy" -> Difficulty.EASY
            "Medium" -> Difficulty.MEDIUM
            "Hard" -> Difficulty.HARD
            "Unfair" -> Difficulty.UNFAIR
            else -> Difficulty.EXTREME
        }

        return Triple(puzzle, solution, difficulty to score)
    }

    private val solutionTypeMap: Map<String, SolutionType> by lazy {
        SolutionType.entries.associateBy { rustName(it) }
    }

    private fun rustName(type: SolutionType): String = when (type) {
        SolutionType.FULL_HOUSE -> "FullHouse"
        SolutionType.NAKED_SINGLE -> "NakedSingle"
        SolutionType.HIDDEN_SINGLE -> "HiddenSingle"
        SolutionType.LOCKED_CANDIDATES_1 -> "LockedCandidates1"
        SolutionType.LOCKED_CANDIDATES_2 -> "LockedCandidates2"
        SolutionType.LOCKED_PAIR -> "LockedPair"
        SolutionType.LOCKED_TRIPLE -> "LockedTriple"
        SolutionType.NAKED_PAIR -> "NakedPair"
        SolutionType.NAKED_TRIPLE -> "NakedTriple"
        SolutionType.NAKED_QUADRUPLE -> "NakedQuadruple"
        SolutionType.HIDDEN_PAIR -> "HiddenPair"
        SolutionType.HIDDEN_TRIPLE -> "HiddenTriple"
        SolutionType.HIDDEN_QUADRUPLE -> "HiddenQuadruple"
        SolutionType.X_WING -> "XWing"
        SolutionType.SWORDFISH -> "Swordfish"
        SolutionType.JELLYFISH -> "Jellyfish"
        SolutionType.SKYSCRAPER -> "Skyscraper"
        SolutionType.TWO_STRING_KITE -> "TwoStringKite"
        SolutionType.EMPTY_RECTANGLE -> "EmptyRectangle"
        SolutionType.TURBOT_FISH -> "TurbotFish"
        SolutionType.XY_WING -> "XyWing"
        SolutionType.XYZ_WING -> "XyzWing"
        SolutionType.W_WING -> "WWing"
        SolutionType.REMOTE_PAIR -> "RemotePair"
        SolutionType.SIMPLE_COLORS_TRAP -> "SimpleColorsTrap"
        SolutionType.SIMPLE_COLORS_WRAP -> "SimpleColorsWrap"
        SolutionType.MULTI_COLORS_1 -> "MultiColors1"
        SolutionType.MULTI_COLORS_2 -> "MultiColors2"
        SolutionType.X_CHAIN -> "XChain"
        SolutionType.XY_CHAIN -> "XyChain"
        SolutionType.BRUTE_FORCE -> "BruteForce"
    }

    private fun parseSolutionStep(json: String): SolutionStep {
        val typeStr = extractString(json, "type")
        val type = solutionTypeMap[typeStr] ?: SolutionType.BRUTE_FORCE

        val cellIndex = extractInt(json, "cellIndex")
        val value = extractInt(json, "value")
        val indices = extractIntArray(json, "indices")
        val candidatesRemoved = extractPairArray(json, "candidatesRemoved")

        return SolutionStep(
            type = type,
            cellIndex = cellIndex,
            value = value,
            indices = indices,
            candidatesRemoved = candidatesRemoved,
        )
    }

    private fun parseHighlights(json: String): List<CandidateHighlight> {
        if (json == "[]" || json.isBlank()) return emptyList()
        val highlights = mutableListOf<CandidateHighlight>()

        var pos = 0
        while (pos < json.length) {
            val objStart = json.indexOf('{', pos)
            if (objStart < 0) break
            val objEnd = json.indexOf('}', objStart)
            if (objEnd < 0) break
            val obj = json.substring(objStart, objEnd + 1)

            val cellIndex = extractInt(obj, "cellIndex")
            val value = extractInt(obj, "value")
            val roleStr = extractString(obj, "role")
            val role = when (roleStr) {
                "Defining" -> HighlightRole.DEFINING
                "Elimination" -> HighlightRole.ELIMINATION
                "Secondary" -> HighlightRole.SECONDARY
                "Tertiary" -> HighlightRole.TERTIARY
                "ColorA" -> HighlightRole.COLOR_A
                "ColorB" -> HighlightRole.COLOR_B
                else -> HighlightRole.DEFINING
            }
            highlights.add(CandidateHighlight(cellIndex, value, role))
            pos = objEnd + 1
        }
        return highlights
    }

    fun stepToJson(step: SolutionStep): String = buildString {
        append("{\"type\":\"")
        append(rustName(step.type))
        append("\",\"cellIndex\":")
        append(step.cellIndex)
        append(",\"value\":")
        append(step.value)
        append(",\"indices\":[")
        append(step.indices.joinToString(","))
        append("],\"candidatesRemoved\":[")
        step.candidatesRemoved.forEachIndexed { i, (cell, digit) ->
            if (i > 0) append(",")
            append("[$cell,$digit]")
        }
        append("]}")
    }

    // ─── Minimal JSON helpers ───────────────────────────────────────────────

    private fun extractString(json: String, key: String): String {
        val marker = "\"$key\":\""
        val start = json.indexOf(marker)
        if (start < 0) return ""
        val valueStart = start + marker.length
        val valueEnd = json.indexOf('"', valueStart)
        if (valueEnd < 0) return ""
        return json.substring(valueStart, valueEnd)
    }

    private fun extractInt(json: String, key: String): Int {
        val marker = "\"$key\":"
        val start = json.indexOf(marker)
        if (start < 0) return 0
        val valueStart = start + marker.length
        val sb = StringBuilder()
        for (i in valueStart until json.length) {
            val c = json[i]
            if (c == '-' || c in '0'..'9') sb.append(c) else break
        }
        return sb.toString().toIntOrNull() ?: 0
    }

    private fun extractIntArray(json: String, key: String): List<Int> {
        val marker = "\"$key\":["
        val start = json.indexOf(marker)
        if (start < 0) return emptyList()
        val arrStart = start + marker.length
        val arrEnd = json.indexOf(']', arrStart)
        if (arrEnd < 0 || arrEnd == arrStart) return emptyList()
        return json.substring(arrStart, arrEnd)
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
    }

    private fun extractPairArray(json: String, key: String): List<Pair<Int, Int>> {
        val marker = "\"$key\":["
        val start = json.indexOf(marker)
        if (start < 0) return emptyList()
        val arrStart = start + marker.length
        val arrEnd = findMatchingBracket(json, arrStart - 1)
        if (arrEnd < 0) return emptyList()
        val content = json.substring(arrStart, arrEnd)
        if (content.isBlank()) return emptyList()

        val pairs = mutableListOf<Pair<Int, Int>>()
        var pos = 0
        while (pos < content.length) {
            val pairStart = content.indexOf('[', pos)
            if (pairStart < 0) break
            val pairEnd = content.indexOf(']', pairStart)
            if (pairEnd < 0) break
            val nums = content.substring(pairStart + 1, pairEnd).split(',')
            if (nums.size == 2) {
                val a = nums[0].trim().toIntOrNull()
                val b = nums[1].trim().toIntOrNull()
                if (a != null && b != null) pairs.add(a to b)
            }
            pos = pairEnd + 1
        }
        return pairs
    }

    private fun findMatchingBracket(json: String, openPos: Int): Int {
        var depth = 0
        for (i in openPos until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }
}
