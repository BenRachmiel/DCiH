package sudoku.core

import kotlinx.coroutines.runBlocking
import sudoku.core.generator.ExampleGenerator
import sudoku.core.generator.Generator
import sudoku.core.model.*
import java.io.File
import kotlin.random.Random
import kotlin.test.Test

/**
 * JVM-only task that generates pre-baked [BoardExample] Kotlin source for all
 * [SolutionType.hasSolver] types. Run once, paste output into StrategyContent.kt.
 *
 * Uses curated puzzles first (fast), then generates random puzzles across multiple
 * seeds. Results from all runs are merged — only fully-solvable puzzles are accepted.
 *
 * Run: ./gradlew :core:jvmTest --tests "sudoku.core.GenerateExamplesTask.generateAllExamples"
 */
class GenerateExamplesTask {
    private val curatedPuzzles =
        listOf(
            // Easy — singles
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
            // Medium — locked candidates, subsets
            "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
            "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
            "003000200090000006000630040006003900050904020004100800020048000400000050005000300",
            "000100000420090001008000037000030060850000024040050000290000500500040013000002000",
            "700600008000030070090000200002009003010040060300100900006000080020060000500003001",
            "000008000065030180800500030010900005000010000500007060070002004098060750000300000",
            // Hard — fish, wings, subsets, hidden triples
            "000000012000035000000600070700000300060000010003000008000006200050070000480000000",
            "200080600000200090060000000003006200400000008007500100000000050010009000009040003",
            "100200000020010000003000400000030060070000050040060000009000700000080020000001003",
            "000400009200009100040000800009300050010080040030006200001000060002700003600005000",
            "050030002000005700006200000200000060030000010060000003000009800001600000700080040",
            "000000039000060050000001000020400300500000007003008040000200000090050000830000000",
            "600008000000070040050001000002600800000050000008003200000400060070010000000900003",
            "000301000010000060002000700070090050000804000040050030003000200090000010000503000",
            "000040700300900000006000040020060100000802000007010080090000600000003009001050000",
            // Jellyfish-prone
            "100000300057020600030006001000007890000000000078200000900300070001060820003000004",
            "000076000000000300830040000000100090408000701060003000000090074001000000000710000",
            "800000090020000050004000600000200708003080200607003000001000300060000070040000002",
            // Unfair — coloring
            "010000002090060050003000700060050040000301000020070080004000600050020090800000010",
            "000270000005000300830000051000000700020030040003000000970000086004000500000097000",
            "500001003002060700080000010060000020000508000010000030020000040003010200400700008",
            "000000000100203004003010560006000100050000030004000800091080700700309002000000000",
        )

    @Test
    fun generateAllExamples() {
        val targets = SolutionType.entries.filter { it.hasSolver }.toSet()
        val found = mutableMapOf<SolutionType, BoardExample>()

        // Phase 1: curated puzzles (instant)
        val gen = ExampleGenerator(Random(42))
        for (puzzle in curatedPuzzles) {
            val remaining = targets - found.keys
            if (remaining.isEmpty()) break
            found.putAll(gen.collectExamplesFromPuzzle(puzzle, remaining))
        }

        // Phase 2: generate random puzzles across multiple seeds
        val missing = targets - found.keys
        if (missing.isNotEmpty()) {
            runBlocking {
                for (seed in 0..99) {
                    val remaining = targets - found.keys
                    if (remaining.isEmpty()) break
                    val generator = Generator(Random(seed.toLong()))
                    val seedGen = ExampleGenerator(Random(seed.toLong()))
                    val targetDifficulty = remaining.maxOf { it.difficulty }
                    for (attempt in 0 until 100) {
                        if ((targets - found.keys).isEmpty()) break
                        val puzzle = generator.generate(targetDifficulty, maxRetries = 20)
                        found.putAll(
                            seedGen.collectExamplesFromPuzzle(
                                puzzle.puzzle,
                                targets - found.keys,
                            ),
                        )
                    }
                }
            }
        }

        // Output
        val out = StringBuilder()
        out.appendLine("// ═══ Generated Examples ═══")
        out.appendLine("// Found ${found.size} / ${targets.size} types\n")

        for (type in SolutionType.entries) {
            if (!type.hasSolver) continue
            val example = found[type]
            if (example == null) {
                out.appendLine("// ${type.name} — NOT FOUND")
                continue
            }
            appendExample(out, type, example)
        }

        val notFound = targets - found.keys
        if (notFound.isNotEmpty()) {
            out.appendLine("\n// ═══ MISSING (${notFound.size}) ═══")
            for (t in notFound) out.appendLine("// ${t.name}")
        }

        val outputFile = File("/tmp/generated-examples.kt")
        outputFile.writeText(out.toString())
        println("Written to ${outputFile.absolutePath} — ${found.size}/${targets.size} types")
    }

    private fun appendExample(
        out: StringBuilder,
        type: SolutionType,
        ex: BoardExample,
    ) {
        out.appendLine("// ${type.name}")
        out.appendLine("example = BoardExample(")
        out.appendLine("    puzzle = \"${ex.puzzle}\",")
        if (ex.candidateMasks != null) {
            out.appendLine("    candidateMasks = intArrayOf(")
            for (row in 0 until 9) {
                val start = row * 9
                val vals =
                    (start until start + 9).joinToString(", ") {
                        ex.candidateMasks[it].toString()
                    }
                val comma = if (row < 8) "," else ""
                out.appendLine("        $vals$comma")
            }
            out.appendLine("    ),")
        }
        out.appendLine("    highlights = listOf(")
        for (h in ex.highlights) {
            out.appendLine("        CandidateHighlight(${h.cellIndex}, ${h.value}, ${h.role.name}),")
        }
        out.appendLine("    ),")
        out.appendLine("),\n")
    }
}
