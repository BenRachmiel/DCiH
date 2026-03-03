package sudoku.app.learn

import sudoku.core.model.*
import sudoku.core.model.HighlightRole.*

/**
 * Strategy encyclopedia content — theory, spotting tips, and board examples for
 * every SolutionType (except BRUTE_FORCE).
 *
 * Each board example is a full 81-cell board: cells are either filled digits or
 * show candidates. No blank cells. Highlights mark the key candidates for
 * each technique.
 *
 * Content derived from HoDoKu (GPL) solving documentation.
 */

/** All strategy entries indexed by SolutionType. */
val strategyEntries: Map<SolutionType, StrategyEntry> by lazy {
    allEntries.associateBy { it.type }
}

/** Ordered list of all entries (follows SolutionType declaration order). */
val allEntries: List<StrategyEntry> by lazy { buildEntries() }

// Helper: create bitmask for a set of candidate digits
private fun mask(vararg digits: Int): Int {
    var m = 0
    for (d in digits) m = m or (1 shl (d - 1))
    return m
}

// Helper: cell index from row,col (0-based)
private fun cell(row: Int, col: Int): Int = row * 9 + col

/**
 * Build a full candidateMasks array from a puzzle string plus a sparse
 * map of cell->mask overrides. Any empty cell (0) not in the overrides
 * gets its candidates computed from the puzzle automatically.
 */
private fun fullBoard(puzzle: String, overrides: Map<Int, Int> = emptyMap()): IntArray {
    val values = IntArray(81) { i ->
        val ch = puzzle[i]
        if (ch in '1'..'9') ch - '0' else 0
    }
    return IntArray(81) { i ->
        if (values[i] != 0) 0
        else overrides[i] ?: computeCandidateMask(values, i)
    }
}

private fun computeCandidateMask(values: IntArray, index: Int): Int {
    val row = index / 9
    val col = index % 9
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    var m = 0x1FF
    for (c in 0..8) { val d = values[row * 9 + c]; if (d != 0) m = m and (1 shl (d - 1)).inv() }
    for (r in 0..8) { val d = values[r * 9 + col]; if (d != 0) m = m and (1 shl (d - 1)).inv() }
    for (r in boxRow until boxRow + 3) for (c in boxCol until boxCol + 3) {
        val d = values[r * 9 + c]; if (d != 0) m = m and (1 shl (d - 1)).inv()
    }
    return m
}

@Suppress("LongMethod")
private fun buildEntries(): List<StrategyEntry> = listOf(
    // ── Singles ──────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.FULL_HOUSE,
        theory = "A Full House occurs when a row, column, or box has exactly one empty cell remaining. " +
                "Since all other eight digits are placed, the missing digit is forced into the last cell.\n\n" +
                "This is the simplest possible solving technique — no candidate analysis is needed. " +
                "Just count the placed digits in the unit and fill in the missing one.",
        howToSpot = "• Scan each row, column, and box for units with only one empty cell.\n" +
                "• Count placed digits: if eight are present, the ninth is determined.\n" +
                "• These are most common in the late stages of a solve.",
        example = BoardExample(
            //       c0 c1 c2  c3 c4 c5  c6 c7 c8
            puzzle = "123456780" + // r0: missing 9 at r0c8
                    "456789123" + // r1
                    "789123456" + // r2
                    "214365897" + // r3
                    "365897214" + // r4
                    "897214365" + // r5
                    "531642978" + // r6
                    "642978531" + // r7
                    "978531642",   // r8
            candidateMasks = IntArray(81) { i -> if (i == 8) mask(9) else 0 },
            highlights = listOf(
                CandidateHighlight(8, 9, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.NAKED_SINGLE, SolutionType.HIDDEN_SINGLE),
        keywords = listOf("last digit", "single empty", "forced"),
    ),

    StrategyEntry(
        type = SolutionType.NAKED_SINGLE,
        theory = "A Naked Single is a cell that has only one candidate remaining after all peer eliminations. " +
                "Every other digit 1–9 appears in the cell's row, column, or box, leaving exactly one possibility.\n\n" +
                "This is the most fundamental technique after Full House and is the backbone of easy puzzles. " +
                "Whenever you place a digit, check if the affected peers now have naked singles.",
        howToSpot = "• Look for cells with only one pencil mark.\n" +
                "• After placing a digit, re-scan the 20 peer cells.\n" +
                "• In candidate mode, naked singles stand out visually as cells with a single small digit.",
        example = BoardExample(
            // r1c4 is the naked single — all peers eliminate everything except 3
            puzzle = "529010000" +
                    "010005000" +
                    "000000000" +
                    "200000000" +
                    "060000000" +
                    "008000000" +
                    "000300000" +
                    "000000000" +
                    "000000000",
            candidateMasks = fullBoard(
                "529010000" +
                "010005000" +
                "000000000" +
                "200000000" +
                "060000000" +
                "008000000" +
                "000300000" +
                "000000000" +
                "000000000",
                mapOf(cell(1, 1) to mask(3)),
            ),
            highlights = listOf(
                CandidateHighlight(cell(1, 1), 3, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.FULL_HOUSE, SolutionType.HIDDEN_SINGLE),
        keywords = listOf("sole candidate", "one candidate", "singles"),
    ),

    StrategyEntry(
        type = SolutionType.HIDDEN_SINGLE,
        theory = "A Hidden Single occurs when a digit can only go in one cell within a row, column, or box, " +
                "even though that cell may have other candidates too. The digit is 'hidden' among other possibilities.\n\n" +
                "For each unit (row/col/box), check where each digit 1–9 can be placed. " +
                "If a digit has only one possible cell in a unit, it must go there regardless of other candidates in that cell.",
        howToSpot = "• For each unit, count the possible positions for each digit.\n" +
                "• A digit with exactly one possible position in any unit is a hidden single.\n" +
                "• Use candidate filtering to highlight a single digit and look for isolated occurrences in a unit.",
        example = BoardExample(
            // In box 0, digit 9 can only go in r0c0 (hidden single in box)
            puzzle = "000832516" +
                    "123000489" +
                    "456000723" +
                    "789365142" +
                    "214578396" +
                    "365921857" +
                    "531246978" +
                    "642789135" +
                    "897153264",
            candidateMasks = fullBoard(
                "000832516" +
                "123000489" +
                "456000723" +
                "789365142" +
                "214578396" +
                "365921857" +
                "531246978" +
                "642789135" +
                "897153264",
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 9, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.FULL_HOUSE, SolutionType.NAKED_SINGLE),
        keywords = listOf("unique position", "sole position", "hidden"),
    ),

    // ── Locked Candidates ───────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.LOCKED_CANDIDATES_1,
        theory = "Pointing occurs when all candidates for a digit within a box are confined to a single row or column. " +
                "Since one of those cells must contain the digit, it can be eliminated from all other cells in that row or column outside the box.\n\n" +
                "Think of it this way: the box 'claims' that digit for that row/column, so the rest of the row/column can't have it.",
        howToSpot = "• In each box, check if a candidate digit appears only in one row or one column.\n" +
                "• If so, that digit can be eliminated from the same row/column in other boxes.\n" +
                "• Filter candidates by digit — look for box-aligned patterns.",
        example = BoardExample(
            // In box 0, digit 7 only appears in row 0 (r0c0, r0c2) → eliminate 7 from row 0 outside box 0
            puzzle = "000904850" +
                    "904850001" +
                    "850001904" +
                    "089045010" +
                    "045010089" +
                    "010089045" +
                    "098540100" +
                    "540100098" +
                    "100098540",
            candidateMasks = fullBoard(
                "000904850" +
                "904850001" +
                "850001904" +
                "089045010" +
                "045010089" +
                "010089045" +
                "098540100" +
                "540100098" +
                "100098540",
                mapOf(
                    cell(0, 0) to mask(2, 3, 6, 7),
                    cell(0, 1) to mask(1, 2, 3, 6),
                    cell(0, 2) to mask(1, 2, 3, 6, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 7, DEFINING),
                CandidateHighlight(cell(0, 2), 7, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.LOCKED_CANDIDATES_2, SolutionType.LOCKED_PAIR),
        keywords = listOf("pointing", "box-line", "intersection"),
    ),

    StrategyEntry(
        type = SolutionType.LOCKED_CANDIDATES_2,
        theory = "Claiming (also called Box/Line Reduction) is the reverse of Pointing. When all candidates for a " +
                "digit in a row or column fall within a single box, that digit can be eliminated from other cells in that box.\n\n" +
                "The row/column 'claims' the digit must be in that intersection, so the rest of the box can't have it.",
        howToSpot = "• In each row/column, check if a candidate digit appears only within one box.\n" +
                "• If so, eliminate that digit from other cells in that box.\n" +
                "• Often found when a row or column has few remaining cells for a digit.",
        example = BoardExample(
            // In row 0, digit 5 only appears in box 0 → eliminate 5 from rest of box 0
            puzzle = "000608412" +
                    "006412000" +
                    "412000006" +
                    "060824100" +
                    "824100060" +
                    "100060824" +
                    "068241000" +
                    "241000068" +
                    "000068241",
            candidateMasks = fullBoard(
                "000608412" +
                "006412000" +
                "412000006" +
                "060824100" +
                "824100060" +
                "100060824" +
                "068241000" +
                "241000068" +
                "000068241",
                mapOf(
                    cell(0, 0) to mask(3, 5, 9),
                    cell(0, 1) to mask(5, 7, 9),
                    cell(0, 2) to mask(3, 7, 9),
                    cell(1, 0) to mask(3, 8, 9),
                    cell(1, 1) to mask(7, 8, 9),
                    cell(1, 2) to mask(3, 7, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 5, DEFINING),
                CandidateHighlight(cell(0, 1), 5, DEFINING),
                CandidateHighlight(cell(1, 0), 8, SECONDARY),
                CandidateHighlight(cell(1, 1), 8, SECONDARY),
            ),
        ),
        relatedTypes = listOf(SolutionType.LOCKED_CANDIDATES_1, SolutionType.LOCKED_PAIR),
        keywords = listOf("claiming", "box-line reduction", "intersection"),
    ),

    StrategyEntry(
        type = SolutionType.LOCKED_PAIR,
        theory = "A Locked Pair is a Naked Pair that is confined to the intersection of a box and a row/column. " +
                "Because the two digits are locked in that intersection, they can be eliminated from the rest of " +
                "both the box and the line simultaneously.\n\n" +
                "This combines the logic of Naked Pairs with Locked Candidates for stronger eliminations.",
        howToSpot = "• Find a Naked Pair (two cells with the same two candidates) in a box.\n" +
                "• Check if both cells share the same row or column.\n" +
                "• If so, eliminate those digits from both the rest of the row/column AND the rest of the box.",
        example = BoardExample(
            puzzle = "006050030" +
                    "950030006" +
                    "030006950" +
                    "005060300" +
                    "060300005" +
                    "300005060" +
                    "009050600" +
                    "050600009" +
                    "600009050",
            candidateMasks = fullBoard(
                "006050030" +
                "950030006" +
                "030006950" +
                "005060300" +
                "060300005" +
                "300005060" +
                "009050600" +
                "050600009" +
                "600009050",
                mapOf(
                    cell(0, 0) to mask(1, 4),
                    cell(0, 1) to mask(1, 4),
                    cell(0, 3) to mask(1, 4, 8, 9),
                    cell(0, 5) to mask(1, 4, 8, 9),
                    cell(0, 7) to mask(1, 2, 4, 7, 8),
                    cell(0, 8) to mask(1, 2, 4, 7, 8),
                    cell(1, 4) to mask(1, 4, 7, 8),
                    cell(2, 0) to mask(1, 2, 4, 7, 8),
                    cell(2, 2) to mask(1, 2, 4, 7, 8),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 1, DEFINING),
                CandidateHighlight(cell(0, 0), 4, DEFINING),
                CandidateHighlight(cell(0, 1), 1, DEFINING),
                CandidateHighlight(cell(0, 1), 4, DEFINING),
                CandidateHighlight(cell(0, 3), 1, ELIMINATION),
                CandidateHighlight(cell(0, 3), 4, ELIMINATION),
                CandidateHighlight(cell(0, 5), 1, ELIMINATION),
                CandidateHighlight(cell(0, 5), 4, ELIMINATION),
                CandidateHighlight(cell(0, 7), 1, ELIMINATION),
                CandidateHighlight(cell(0, 7), 4, ELIMINATION),
                CandidateHighlight(cell(2, 0), 1, ELIMINATION),
                CandidateHighlight(cell(2, 0), 4, ELIMINATION),
                CandidateHighlight(cell(2, 2), 1, ELIMINATION),
                CandidateHighlight(cell(2, 2), 4, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.LOCKED_CANDIDATES_1, SolutionType.NAKED_PAIR),
        keywords = listOf("locked pair", "box-line pair"),
    ),

    StrategyEntry(
        type = SolutionType.LOCKED_TRIPLE,
        theory = "A Locked Triple is a Naked Triple confined to the intersection of a box and a line. " +
                "Three cells in the intersection share three candidates among them, allowing eliminations " +
                "from both the remaining box cells and the remaining line cells.\n\n" +
                "This is rarer than Locked Pairs but follows the same principle with three candidates.",
        howToSpot = "• Find three cells in a box-line intersection whose combined candidates total exactly three digits.\n" +
                "• Not all three candidates need to appear in every cell.\n" +
                "• Eliminate those three digits from the rest of both the box and the line.",
        example = BoardExample(
            puzzle = "000700089" +
                    "700089000" +
                    "089000700" +
                    "006800090" +
                    "800090006" +
                    "090006800" +
                    "008900060" +
                    "900060008" +
                    "060008900",
            candidateMasks = fullBoard(
                "000700089" +
                "700089000" +
                "089000700" +
                "006800090" +
                "800090006" +
                "090006800" +
                "008900060" +
                "900060008" +
                "060008900",
                mapOf(
                    cell(0, 0) to mask(2, 3, 5),
                    cell(0, 1) to mask(2, 5),
                    cell(0, 2) to mask(3, 5),
                    cell(0, 5) to mask(1, 2, 3, 4, 5, 6),
                    cell(0, 6) to mask(1, 2, 3, 4, 5, 6),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 2, DEFINING),
                CandidateHighlight(cell(0, 0), 3, DEFINING),
                CandidateHighlight(cell(0, 0), 5, DEFINING),
                CandidateHighlight(cell(0, 1), 2, DEFINING),
                CandidateHighlight(cell(0, 1), 5, DEFINING),
                CandidateHighlight(cell(0, 2), 3, DEFINING),
                CandidateHighlight(cell(0, 2), 5, DEFINING),
                CandidateHighlight(cell(0, 5), 2, ELIMINATION),
                CandidateHighlight(cell(0, 5), 3, ELIMINATION),
                CandidateHighlight(cell(0, 5), 5, ELIMINATION),
                CandidateHighlight(cell(0, 6), 2, ELIMINATION),
                CandidateHighlight(cell(0, 6), 3, ELIMINATION),
                CandidateHighlight(cell(0, 6), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.LOCKED_PAIR, SolutionType.NAKED_TRIPLE),
        keywords = listOf("locked triple", "box-line triple"),
    ),

    // ── Subsets ──────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.NAKED_PAIR,
        theory = "A Naked Pair occurs when two cells in the same unit (row, column, or box) contain exactly the " +
                "same two candidates and nothing else. Since those two digits must occupy those two cells, " +
                "they can be eliminated from all other cells in the unit.\n\n" +
                "This is the simplest subset technique and one of the most commonly used strategies in intermediate puzzles.",
        howToSpot = "• Look for two cells in a unit each containing exactly two candidates.\n" +
                "• If both cells have the identical pair, it's a Naked Pair.\n" +
                "• Eliminate both digits from all other cells in that unit.",
        example = BoardExample(
            // Row 0: naked pair {3,7} at r0c2 and r0c6
            puzzle = "150000200" +
                    "829451376" +
                    "463279581" +
                    "285713694" +
                    "314968725" +
                    "697524813" +
                    "741895062" +
                    "936142050" +
                    "500006140",
            candidateMasks = fullBoard(
                "150000200" +
                "829451376" +
                "463279581" +
                "285713694" +
                "314968725" +
                "697524813" +
                "741895062" +
                "936142050" +
                "500006140",
                mapOf(
                    cell(0, 2) to mask(3, 7),
                    cell(0, 6) to mask(3, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 2), 3, DEFINING),
                CandidateHighlight(cell(0, 2), 7, DEFINING),
                CandidateHighlight(cell(0, 6), 3, DEFINING),
                CandidateHighlight(cell(0, 6), 7, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.HIDDEN_PAIR, SolutionType.NAKED_TRIPLE),
        keywords = listOf("pair", "subset", "naked subset"),
    ),

    StrategyEntry(
        type = SolutionType.NAKED_TRIPLE,
        theory = "A Naked Triple occurs when three cells in a unit collectively contain exactly three candidates. " +
                "Not every cell needs all three digits — the union of their candidates must equal exactly three.\n\n" +
                "For example, cells with {1,3}, {1,7}, and {3,7} form a naked triple on digits 1,3,7. " +
                "Those three digits can be eliminated from all other cells in the unit.",
        howToSpot = "• Find three cells in a unit whose combined candidates total exactly three digits.\n" +
                "• Each cell has 2 or 3 candidates, all drawn from the same three-digit set.\n" +
                "• Eliminate those three digits from the remaining cells in the unit.",
        example = BoardExample(
            // Row 0: naked triple {2,5,8} at r0c0, r0c3, r0c6
            puzzle = "000103000" +
                    "913478562" +
                    "476952813" +
                    "064215039" +
                    "025396040" +
                    "039847025" +
                    "000521000" +
                    "052734091" +
                    "091683050",
            candidateMasks = fullBoard(
                "000103000" +
                "913478562" +
                "476952813" +
                "064215039" +
                "025396040" +
                "039847025" +
                "000521000" +
                "052734091" +
                "091683050",
                mapOf(
                    cell(0, 0) to mask(2, 5),
                    cell(0, 1) to mask(2, 5, 6, 8),
                    cell(0, 2) to mask(6, 8),
                    cell(0, 3) to mask(5, 8),
                    cell(0, 6) to mask(2, 8),
                    cell(0, 7) to mask(2, 4, 6, 7, 9),
                    cell(0, 8) to mask(4, 6, 7, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 2, DEFINING),
                CandidateHighlight(cell(0, 0), 5, DEFINING),
                CandidateHighlight(cell(0, 3), 5, DEFINING),
                CandidateHighlight(cell(0, 3), 8, DEFINING),
                CandidateHighlight(cell(0, 6), 2, DEFINING),
                CandidateHighlight(cell(0, 6), 8, DEFINING),
                CandidateHighlight(cell(0, 1), 2, ELIMINATION),
                CandidateHighlight(cell(0, 1), 5, ELIMINATION),
                CandidateHighlight(cell(0, 1), 8, ELIMINATION),
                CandidateHighlight(cell(0, 7), 2, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.NAKED_PAIR, SolutionType.NAKED_QUADRUPLE, SolutionType.HIDDEN_TRIPLE),
        keywords = listOf("triple", "naked subset", "three candidates"),
    ),

    StrategyEntry(
        type = SolutionType.NAKED_QUADRUPLE,
        theory = "A Naked Quadruple extends the naked subset concept to four cells containing exactly four candidates. " +
                "The four digits are locked into those four cells, so they can be eliminated from all other cells in the unit.\n\n" +
                "While conceptually simple, quads are harder to spot visually because each cell may contain 2, 3, or 4 of the four digits.",
        howToSpot = "• Find four cells in a unit whose combined candidates total exactly four digits.\n" +
                "• Often easier to find by looking for the complementary hidden quad instead.\n" +
                "• Eliminate those four digits from remaining cells in the unit.",
        example = BoardExample(
            // Col 0: naked quad {1,3,6,9} at r0, r2, r5, r8
            puzzle = "002845007" +
                    "845007002" +
                    "007002845" +
                    "200780050" +
                    "780050200" +
                    "050200780" +
                    "500070020" +
                    "070020500" +
                    "020500070",
            candidateMasks = fullBoard(
                "002845007" +
                "845007002" +
                "007002845" +
                "200780050" +
                "780050200" +
                "050200780" +
                "500070020" +
                "070020500" +
                "020500070",
                mapOf(
                    cell(0, 0) to mask(1, 6, 9),
                    cell(2, 0) to mask(1, 3, 6),
                    cell(5, 0) to mask(1, 3, 9),
                    cell(8, 0) to mask(3, 6, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 1, DEFINING),
                CandidateHighlight(cell(0, 0), 6, DEFINING),
                CandidateHighlight(cell(0, 0), 9, DEFINING),
                CandidateHighlight(cell(2, 0), 1, DEFINING),
                CandidateHighlight(cell(2, 0), 3, DEFINING),
                CandidateHighlight(cell(2, 0), 6, DEFINING),
                CandidateHighlight(cell(5, 0), 1, DEFINING),
                CandidateHighlight(cell(5, 0), 3, DEFINING),
                CandidateHighlight(cell(5, 0), 9, DEFINING),
                CandidateHighlight(cell(8, 0), 3, DEFINING),
                CandidateHighlight(cell(8, 0), 6, DEFINING),
                CandidateHighlight(cell(8, 0), 9, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.NAKED_TRIPLE, SolutionType.HIDDEN_QUADRUPLE),
        keywords = listOf("quad", "quadruple", "naked subset", "four candidates"),
    ),

    StrategyEntry(
        type = SolutionType.HIDDEN_PAIR,
        theory = "A Hidden Pair occurs when two candidates appear in exactly two cells within a unit, " +
                "even though those cells may contain other candidates. Since those two digits must go in those " +
                "two cells, all other candidates can be eliminated from them.\n\n" +
                "Hidden Pairs are the 'complement' of Naked Pairs: instead of cells defining the pattern, " +
                "the digits define it. In a unit of 9 cells, a hidden pair of size 2 implies a naked subset of size 7.",
        howToSpot = "• For each unit, check which cells contain each digit.\n" +
                "• If two digits appear in exactly the same two cells, it's a hidden pair.\n" +
                "• Remove all other candidates from those two cells.",
        example = BoardExample(
            // In row 0, digits 5 and 7 only appear in r0c1 and r0c4 → hidden pair
            puzzle = "000200000" +
                    "283941576" +
                    "194576238" +
                    "400829060" +
                    "829060400" +
                    "060400829" +
                    "340082960" +
                    "082960340" +
                    "960340082",
            candidateMasks = fullBoard(
                "000200000" +
                "283941576" +
                "194576238" +
                "400829060" +
                "829060400" +
                "060400829" +
                "340082960" +
                "082960340" +
                "960340082",
                mapOf(
                    cell(0, 0) to mask(6),
                    cell(0, 1) to mask(3, 5, 6, 7),
                    cell(0, 2) to mask(6),
                    cell(0, 4) to mask(1, 3, 5, 7, 8),
                    cell(0, 5) to mask(1, 3, 8),
                    cell(0, 6) to mask(1, 9),
                    cell(0, 7) to mask(1, 4, 9),
                    cell(0, 8) to mask(1, 4, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 1), 5, DEFINING),
                CandidateHighlight(cell(0, 1), 7, DEFINING),
                CandidateHighlight(cell(0, 4), 5, DEFINING),
                CandidateHighlight(cell(0, 4), 7, DEFINING),
                CandidateHighlight(cell(0, 1), 3, ELIMINATION),
                CandidateHighlight(cell(0, 1), 6, ELIMINATION),
                CandidateHighlight(cell(0, 4), 1, ELIMINATION),
                CandidateHighlight(cell(0, 4), 3, ELIMINATION),
                CandidateHighlight(cell(0, 4), 8, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.NAKED_PAIR, SolutionType.HIDDEN_TRIPLE),
        keywords = listOf("hidden pair", "hidden subset", "complement"),
    ),

    StrategyEntry(
        type = SolutionType.HIDDEN_TRIPLE,
        theory = "A Hidden Triple occurs when three candidates are confined to exactly three cells in a unit. " +
                "Other candidates in those cells can be eliminated, leaving only the triple's digits.\n\n" +
                "Like Hidden Pairs, hidden triples are harder to spot than their naked counterparts because " +
                "the pattern cells contain 'extra' candidates that obscure the triple.",
        howToSpot = "• For each unit, map each digit to its possible cells.\n" +
                "• If three digits share exactly the same three cells, it's a hidden triple.\n" +
                "• Remove all non-triple candidates from those three cells.",
        example = BoardExample(
            // In row 0, digits 1,4,6 only in r0c0, r0c3, r0c7 → hidden triple
            puzzle = "000800000" +
                    "389020004" +
                    "520004389" +
                    "004389020" +
                    "800020400" +
                    "020400800" +
                    "400800020" +
                    "003200800" +
                    "200008403",
            candidateMasks = fullBoard(
                "000800000" +
                "389020004" +
                "520004389" +
                "004389020" +
                "800020400" +
                "020400800" +
                "400800020" +
                "003200800" +
                "200008403",
                mapOf(
                    cell(0, 0) to mask(1, 6, 7),
                    cell(0, 1) to mask(7, 9),
                    cell(0, 2) to mask(7, 9),
                    cell(0, 3) to mask(1, 4, 6),
                    cell(0, 5) to mask(2, 3, 5, 9),
                    cell(0, 6) to mask(2, 3, 5),
                    cell(0, 7) to mask(1, 2, 3, 4, 5, 6),
                    cell(0, 8) to mask(2, 3, 5),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 1, DEFINING),
                CandidateHighlight(cell(0, 0), 6, DEFINING),
                CandidateHighlight(cell(0, 3), 1, DEFINING),
                CandidateHighlight(cell(0, 3), 4, DEFINING),
                CandidateHighlight(cell(0, 3), 6, DEFINING),
                CandidateHighlight(cell(0, 7), 1, DEFINING),
                CandidateHighlight(cell(0, 7), 4, DEFINING),
                CandidateHighlight(cell(0, 7), 6, DEFINING),
                CandidateHighlight(cell(0, 0), 7, ELIMINATION),
                CandidateHighlight(cell(0, 7), 2, ELIMINATION),
                CandidateHighlight(cell(0, 7), 3, ELIMINATION),
                CandidateHighlight(cell(0, 7), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.HIDDEN_PAIR, SolutionType.HIDDEN_QUADRUPLE, SolutionType.NAKED_TRIPLE),
        keywords = listOf("hidden triple", "hidden subset"),
    ),

    StrategyEntry(
        type = SolutionType.HIDDEN_QUADRUPLE,
        theory = "A Hidden Quadruple occurs when four candidates are confined to exactly four cells in a unit. " +
                "All other candidates can be eliminated from those four cells.\n\n" +
                "Hidden quads are extremely rare in practice because they imply a naked quintuple in the remaining " +
                "five cells, which is usually easier to spot. They are included for completeness.",
        howToSpot = "• Map each digit to its possible cells in the unit.\n" +
                "• If four digits share exactly four cells, it's a hidden quad.\n" +
                "• Usually easier to find the complementary naked subset instead.",
        example = BoardExample(
            // In row 0, digits 1,2,8,9 confined to r0c0,c2,c5,c7 → hidden quad
            puzzle = "000300000" +
                    "375040060" +
                    "046060375" +
                    "060375040" +
                    "300040006" +
                    "040006300" +
                    "006300040" +
                    "630004700" +
                    "004700630",
            candidateMasks = fullBoard(
                "000300000" +
                "375040060" +
                "046060375" +
                "060375040" +
                "300040006" +
                "040006300" +
                "006300040" +
                "630004700" +
                "004700630",
                mapOf(
                    cell(0, 0) to mask(1, 2, 5, 8, 9),
                    cell(0, 1) to mask(5, 6, 7),
                    cell(0, 2) to mask(1, 2, 7, 8),
                    cell(0, 4) to mask(4, 5, 6, 7),
                    cell(0, 5) to mask(2, 5, 6, 9),
                    cell(0, 6) to mask(4, 5, 7),
                    cell(0, 7) to mask(1, 4, 5, 8, 9),
                    cell(0, 8) to mask(4, 5, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 1, DEFINING),
                CandidateHighlight(cell(0, 0), 2, DEFINING),
                CandidateHighlight(cell(0, 0), 8, DEFINING),
                CandidateHighlight(cell(0, 0), 9, DEFINING),
                CandidateHighlight(cell(0, 2), 1, DEFINING),
                CandidateHighlight(cell(0, 2), 2, DEFINING),
                CandidateHighlight(cell(0, 2), 8, DEFINING),
                CandidateHighlight(cell(0, 5), 2, DEFINING),
                CandidateHighlight(cell(0, 5), 9, DEFINING),
                CandidateHighlight(cell(0, 7), 1, DEFINING),
                CandidateHighlight(cell(0, 7), 8, DEFINING),
                CandidateHighlight(cell(0, 7), 9, DEFINING),
                CandidateHighlight(cell(0, 0), 5, ELIMINATION),
                CandidateHighlight(cell(0, 2), 7, ELIMINATION),
                CandidateHighlight(cell(0, 5), 5, ELIMINATION),
                CandidateHighlight(cell(0, 5), 6, ELIMINATION),
                CandidateHighlight(cell(0, 7), 4, ELIMINATION),
                CandidateHighlight(cell(0, 7), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.HIDDEN_TRIPLE, SolutionType.NAKED_QUADRUPLE),
        keywords = listOf("hidden quad", "hidden quadruple", "hidden subset"),
    ),

    // ── Fish ─────────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.X_WING,
        theory = "An X-Wing is formed when a candidate digit appears in exactly two cells in each of two different " +
                "rows, and those cells align on the same two columns (forming a rectangle). The digit must occupy " +
                "two of the four corners, so it can be eliminated from all other cells in those two columns.\n\n" +
                "The pattern also works transposed: two columns with matching pairs eliminate from two rows.",
        howToSpot = "• Filter candidates for a single digit.\n" +
                "• Find two rows where that digit appears in exactly two positions.\n" +
                "• If the column positions match, you have an X-Wing.\n" +
                "• Eliminate the digit from other cells in those two columns.",
        example = BoardExample(
            // X-Wing on digit 5: rows 1,7 × cols 2,6
            puzzle = "934001862" +
                    "000926000" +
                    "826000934" +
                    "293864517" +
                    "517293648" +
                    "648517293" +
                    "480009320" +
                    "000340000" +
                    "362008479",
            candidateMasks = fullBoard(
                "934001862" +
                "000926000" +
                "826000934" +
                "293864517" +
                "517293648" +
                "648517293" +
                "480009320" +
                "000340000" +
                "362008479",
                mapOf(
                    cell(1, 0) to mask(1, 5),
                    cell(1, 1) to mask(1, 7),
                    cell(1, 2) to mask(5, 7),
                    cell(1, 6) to mask(1, 5),
                    cell(1, 7) to mask(1, 3, 7),
                    cell(1, 8) to mask(3),
                    cell(7, 0) to mask(1, 5, 9),
                    cell(7, 1) to mask(1, 7, 9),
                    cell(7, 2) to mask(5, 7, 9),
                    cell(7, 5) to mask(1, 6),
                    cell(7, 6) to mask(1, 5, 6),
                    cell(7, 7) to mask(1, 6, 8),
                    cell(7, 8) to mask(6),
                    cell(0, 3) to mask(5, 7),
                    cell(0, 4) to mask(5, 7),
                    cell(2, 3) to mask(1, 5, 7),
                    cell(2, 4) to mask(1, 3, 5, 7),
                    cell(2, 5) to mask(1, 3),
                    cell(6, 4) to mask(1, 5, 6, 7),
                    cell(6, 7) to mask(1, 6),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(1, 2), 5, DEFINING),
                CandidateHighlight(cell(1, 6), 5, DEFINING),
                CandidateHighlight(cell(7, 2), 5, DEFINING),
                CandidateHighlight(cell(7, 6), 5, DEFINING),
                CandidateHighlight(cell(0, 3), 5, ELIMINATION),
                CandidateHighlight(cell(2, 3), 5, ELIMINATION),
                CandidateHighlight(cell(6, 4), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SWORDFISH, SolutionType.JELLYFISH),
        keywords = listOf("x-wing", "fish", "basic fish", "two rows two columns"),
    ),

    StrategyEntry(
        type = SolutionType.SWORDFISH,
        theory = "A Swordfish extends the X-Wing concept to three rows and three columns. A candidate digit " +
                "appears in 2–3 cells per row across exactly three rows, and all those cells fall in the same " +
                "three columns. The digit is locked in those intersections and can be eliminated from other cells " +
                "in the three columns.\n\n" +
                "Not all nine intersection cells need to contain the candidate — the key is that each row's " +
                "candidates are a subset of the three columns.",
        howToSpot = "• Filter for a single digit.\n" +
                "• Find three rows where the digit appears in 2–3 cells each.\n" +
                "• If all candidate positions fall within the same three columns, it's a Swordfish.\n" +
                "• Eliminate the digit from other cells in those three columns.",
        example = BoardExample(
            // Swordfish on digit 3: rows 0,4,8 × cols 1,4,7
            puzzle = "900060080" +
                    "246891537" +
                    "871523946" +
                    "620957014" +
                    "509010062" +
                    "714236859" +
                    "460079020" +
                    "187342695" +
                    "090600070",
            candidateMasks = fullBoard(
                "900060080" +
                "246891537" +
                "871523946" +
                "620957014" +
                "509010062" +
                "714236859" +
                "460079020" +
                "187342695" +
                "090600070",
                mapOf(
                    cell(0, 0) to mask(5),
                    cell(0, 1) to mask(3, 5),
                    cell(0, 4) to mask(3, 4),
                    cell(0, 6) to mask(1, 2),
                    cell(0, 7) to mask(1, 2, 3),
                    cell(0, 8) to mask(1, 2),
                    cell(4, 1) to mask(3, 8),
                    cell(4, 3) to mask(4, 8),
                    cell(4, 4) to mask(3, 4, 8),
                    cell(4, 7) to mask(3, 7, 8),
                    cell(8, 0) to mask(2, 3, 5),
                    cell(8, 1) to mask(2, 3, 5),
                    cell(8, 4) to mask(1, 4, 5, 8),
                    cell(8, 6) to mask(1, 4),
                    cell(8, 7) to mask(1, 3, 4, 8),
                    cell(8, 8) to mask(1, 8),
                    cell(3, 7) to mask(3, 8),
                    cell(6, 5) to mask(1, 8),
                    cell(6, 7) to mask(1, 3, 8),
                    cell(6, 8) to mask(1, 8),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 1), 3, DEFINING),
                CandidateHighlight(cell(0, 7), 3, DEFINING),
                CandidateHighlight(cell(4, 1), 3, DEFINING),
                CandidateHighlight(cell(4, 4), 3, DEFINING),
                CandidateHighlight(cell(8, 1), 3, DEFINING),
                CandidateHighlight(cell(8, 7), 3, DEFINING),
                CandidateHighlight(cell(3, 7), 3, ELIMINATION),
                CandidateHighlight(cell(6, 7), 3, ELIMINATION),
                CandidateHighlight(cell(0, 4), 3, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.X_WING, SolutionType.JELLYFISH),
        keywords = listOf("swordfish", "fish", "three rows three columns"),
    ),

    StrategyEntry(
        type = SolutionType.JELLYFISH,
        theory = "A Jellyfish extends the fish concept to four rows and four columns. A candidate digit appears " +
                "in 2–4 cells per row across exactly four rows, all falling within the same four columns.\n\n" +
                "Jellyfish are rare in practice but follow the same logic as X-Wing and Swordfish. " +
                "The digit is eliminated from other cells in the four cover columns.",
        howToSpot = "• Filter for a single digit.\n" +
                "• Find four rows where the digit appears in 2–4 cells each.\n" +
                "• If all positions fall within four columns, it's a Jellyfish.\n" +
                "• Very rare — most puzzles can be solved without needing one.",
        example = BoardExample(
            // Jellyfish on digit 7: rows 0,2,5,8 × cols 0,3,5,8
            puzzle = "006040030" +
                    "943175826" +
                    "005020040" +
                    "468213579" +
                    "231759468" +
                    "009060010" +
                    "314982657" +
                    "652437981" +
                    "000500000",
            candidateMasks = fullBoard(
                "006040030" +
                "943175826" +
                "005020040" +
                "468213579" +
                "231759468" +
                "009060010" +
                "314982657" +
                "652437981" +
                "000500000",
                mapOf(
                    cell(0, 0) to mask(1, 7, 8),
                    cell(0, 1) to mask(1, 2, 8),
                    cell(0, 3) to mask(7, 8, 9),
                    cell(0, 5) to mask(7, 8, 9),
                    cell(0, 7) to mask(1, 2, 5, 9),
                    cell(0, 8) to mask(1, 2, 5),
                    cell(2, 0) to mask(7, 8),
                    cell(2, 1) to mask(1, 2, 8),
                    cell(2, 3) to mask(6, 7, 8, 9),
                    cell(2, 5) to mask(6, 7, 8, 9),
                    cell(2, 7) to mask(1, 9),
                    cell(2, 8) to mask(1),
                    cell(5, 0) to mask(5, 7),
                    cell(5, 2) to mask(5),
                    cell(5, 3) to mask(4, 7, 8),
                    cell(5, 5) to mask(2, 4, 7, 8),
                    cell(5, 7) to mask(2, 3, 4),
                    cell(5, 8) to mask(2, 3),
                    cell(8, 0) to mask(7, 8, 9),
                    cell(8, 2) to mask(8),
                    cell(8, 4) to mask(6, 8),
                    cell(8, 5) to mask(6),
                    cell(8, 6) to mask(3, 4),
                    cell(8, 7) to mask(3, 4),
                    cell(8, 8) to mask(2, 3, 4),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 7, DEFINING),
                CandidateHighlight(cell(0, 3), 7, DEFINING),
                CandidateHighlight(cell(0, 5), 7, DEFINING),
                CandidateHighlight(cell(2, 0), 7, DEFINING),
                CandidateHighlight(cell(2, 3), 7, DEFINING),
                CandidateHighlight(cell(2, 5), 7, DEFINING),
                CandidateHighlight(cell(5, 0), 7, DEFINING),
                CandidateHighlight(cell(5, 3), 7, DEFINING),
                CandidateHighlight(cell(5, 5), 7, DEFINING),
                CandidateHighlight(cell(8, 0), 7, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.SWORDFISH, SolutionType.X_WING),
        keywords = listOf("jellyfish", "fish", "four rows four columns"),
    ),

    // ── Single-Digit Patterns ────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.SKYSCRAPER,
        theory = "A Skyscraper is a single-digit pattern built from two conjugate pairs (strong links) that share " +
                "one endpoint's row or column. It looks like two 'towers' of equal height connected at their bases.\n\n" +
                "If a digit has exactly two positions in each of two rows (or columns), and one position from each " +
                "row aligns in the same column, then cells that see both 'top' positions can have the digit eliminated.",
        howToSpot = "• Filter for a single digit and find rows/columns with exactly two candidates.\n" +
                "• Look for two such rows sharing a column position (the 'base').\n" +
                "• Cells seeing both non-base endpoints can be eliminated.",
        example = BoardExample(
            // Skyscraper on digit 4: rows 1,6 share col 2 (base). Tips: (1,7) and (6,5)
            puzzle = "895321670" +
                    "320000819" +
                    "176089325" +
                    "918265034" +
                    "263894157" +
                    "754013968" +
                    "580000291" +
                    "631928740" +
                    "049071586",
            candidateMasks = fullBoard(
                "895321670" +
                "320000819" +
                "176089325" +
                "918265034" +
                "263894157" +
                "754013968" +
                "580000291" +
                "631928740" +
                "049071586",
                mapOf(
                    cell(0, 7) to mask(4),
                    cell(1, 1) to mask(4, 6),
                    cell(1, 2) to mask(4, 6),
                    cell(1, 3) to mask(4, 5, 6, 7),
                    cell(1, 4) to mask(5, 6, 7),
                    cell(1, 7) to mask(4),
                    cell(3, 7) to mask(7),
                    cell(3, 8) to mask(7),
                    cell(5, 4) to mask(2),
                    cell(6, 1) to mask(4, 6, 7),
                    cell(6, 2) to mask(4, 6, 7),
                    cell(6, 3) to mask(3, 4, 6, 7),
                    cell(6, 4) to mask(3, 6, 7),
                    cell(6, 5) to mask(4, 6),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(1, 2), 4, SECONDARY),
                CandidateHighlight(cell(6, 2), 4, SECONDARY),
                CandidateHighlight(cell(1, 7), 4, DEFINING),
                CandidateHighlight(cell(6, 5), 4, DEFINING),
                CandidateHighlight(cell(6, 3), 4, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.TWO_STRING_KITE, SolutionType.TURBOT_FISH),
        keywords = listOf("skyscraper", "single digit", "turbot", "conjugate pair"),
    ),

    StrategyEntry(
        type = SolutionType.TWO_STRING_KITE,
        theory = "A 2-String Kite uses a box as a bridge between a row strong link and a column strong link for the same digit. " +
                "One endpoint from the row link and one from the column link are in the same box, connecting them.\n\n" +
                "The two 'free' endpoints (outside the box) form the elimination zone: any cell seeing both can lose the digit.",
        howToSpot = "• Filter for one digit. Find a row with exactly two positions and a column with exactly two.\n" +
                "• Check if one cell from each shares a box.\n" +
                "• The other two cells are the 'kite tips' — eliminate from cells seeing both.",
        example = BoardExample(
            // 2-String Kite on digit 6: row link (0,1)-(0,7), col link (1,1)-(7,1), share box at (0,1)/(1,1)
            puzzle = "900800090" +
                    "804090000" +
                    "372641580" +
                    "743568219" +
                    "681924357" +
                    "259317468" +
                    "036059020" +
                    "090206030" +
                    "520083940",
            candidateMasks = fullBoard(
                "900800090" +
                "804090000" +
                "372641580" +
                "743568219" +
                "681924357" +
                "259317468" +
                "036059020" +
                "090206030" +
                "520083940",
                mapOf(
                    cell(0, 0) to mask(5),
                    cell(0, 1) to mask(1, 5, 6),
                    cell(0, 3) to mask(2, 3, 5),
                    cell(0, 5) to mask(2, 3, 5),
                    cell(0, 7) to mask(1, 2, 3, 6),
                    cell(0, 8) to mask(1, 2, 3),
                    cell(1, 1) to mask(1, 5, 6),
                    cell(1, 3) to mask(2, 3, 5),
                    cell(1, 5) to mask(2, 3, 5, 7),
                    cell(1, 6) to mask(1, 2, 3, 6, 7),
                    cell(1, 7) to mask(1, 2, 3, 6, 7),
                    cell(1, 8) to mask(1, 2, 3),
                    cell(2, 7) to mask(9),
                    cell(6, 4) to mask(7),
                    cell(6, 6) to mask(1, 7, 8),
                    cell(6, 8) to mask(1, 7),
                    cell(7, 0) to mask(1, 4, 8),
                    cell(7, 1) to mask(1, 4, 7, 8),
                    cell(7, 4) to mask(4, 7),
                    cell(7, 6) to mask(1, 5, 7, 8),
                    cell(7, 8) to mask(1, 5),
                    cell(8, 4) to mask(1, 7),
                    cell(8, 6) to mask(1, 7),
                    cell(8, 8) to mask(1, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 7), 6, DEFINING),
                CandidateHighlight(cell(7, 1), 6, DEFINING),
                CandidateHighlight(cell(0, 1), 6, SECONDARY),
                CandidateHighlight(cell(1, 1), 6, SECONDARY),
                CandidateHighlight(cell(7, 6), 6, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SKYSCRAPER, SolutionType.EMPTY_RECTANGLE),
        keywords = listOf("kite", "2-string kite", "single digit"),
    ),

    StrategyEntry(
        type = SolutionType.EMPTY_RECTANGLE,
        theory = "An Empty Rectangle uses a box where a candidate digit is confined to one row and one column within the box " +
                "(forming an 'L' or cross shape), combined with a strong link in another row or column.\n\n" +
                "The box's 'empty rectangle' guarantees the digit is in one of the intersection cells, which " +
                "connects to the strong link to force an elimination.",
        howToSpot = "• Filter for one digit. Find a box where the digit's candidates form a cross/L shape.\n" +
                "• Find a strong link (conjugate pair) in a row or column that intersects the box's arm.\n" +
                "• The other endpoint of the strong link sees the box's other arm for elimination.",
        example = BoardExample(
            // ER in box 4 on digit 2: cross at (3,4),(4,3),(5,4). Strong link in row 3: (3,4)-(3,8)
            puzzle = "751489326" +
                    "489326751" +
                    "326751489" +
                    "500040000" +
                    "040000500" +
                    "000500040" +
                    "265098174" +
                    "098174265" +
                    "174265098",
            candidateMasks = fullBoard(
                "751489326" +
                "489326751" +
                "326751489" +
                "500040000" +
                "040000500" +
                "000500040" +
                "265098174" +
                "098174265" +
                "174265098",
                mapOf(
                    cell(3, 0) to mask(6, 8),
                    cell(3, 2) to mask(1, 2, 6, 8),
                    cell(3, 3) to mask(1, 6, 7, 8),
                    cell(3, 4) to mask(2, 3, 6, 7),
                    cell(3, 6) to mask(1, 7, 8, 9),
                    cell(3, 7) to mask(1, 2, 7, 8, 9),
                    cell(3, 8) to mask(1, 2, 3, 7, 9),
                    cell(4, 0) to mask(6),
                    cell(4, 2) to mask(1, 2, 6),
                    cell(4, 3) to mask(1, 2, 6, 7, 8),
                    cell(4, 4) to mask(1, 2, 3, 6, 7, 8, 9),
                    cell(4, 5) to mask(1, 2, 3, 6, 7, 8, 9),
                    cell(4, 7) to mask(1, 2, 7, 8, 9),
                    cell(4, 8) to mask(1, 2, 3, 7, 9),
                    cell(5, 0) to mask(1, 6, 8, 9),
                    cell(5, 1) to mask(1, 2, 3, 6, 7, 8, 9),
                    cell(5, 2) to mask(1, 2, 6, 8, 9),
                    cell(5, 4) to mask(2, 3, 6, 7, 8, 9),
                    cell(5, 5) to mask(1, 2, 3, 6, 7, 8),
                    cell(5, 7) to mask(1, 2, 7, 8, 9),
                    cell(5, 8) to mask(1, 2, 3, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(3, 4), 2, SECONDARY),
                CandidateHighlight(cell(4, 3), 2, SECONDARY),
                CandidateHighlight(cell(5, 4), 2, SECONDARY),
                CandidateHighlight(cell(3, 8), 2, DEFINING),
                CandidateHighlight(cell(5, 8), 2, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SKYSCRAPER, SolutionType.TWO_STRING_KITE),
        keywords = listOf("empty rectangle", "ER", "single digit", "box cross"),
    ),

    StrategyEntry(
        type = SolutionType.TURBOT_FISH,
        theory = "The Turbot Fish (or Turbot Crane) is a generalized single-digit chain of length 4. " +
                "It uses two strong links connected by a weak link to create an elimination.\n\n" +
                "Skyscrapers, 2-String Kites, and Empty Rectangles are all specific geometric arrangements " +
                "of the Turbot Fish pattern. The general case links any two conjugate pairs via a weak inference.",
        howToSpot = "• Filter for one digit.\n" +
                "• Find two strong links (conjugate pairs) for that digit.\n" +
                "• If an endpoint of one pair sees an endpoint of the other pair (weak link), " +
                "the remaining two endpoints form the elimination zone.",
        example = BoardExample(
            // Turbot Fish on digit 8: strong links (0,3)-(0,7) and (5,3)-(8,3), weak via col 3
            puzzle = "512000946" +
                    "946512037" +
                    "370946512" +
                    "201354769" +
                    "769201354" +
                    "354000201" +
                    "493625100" +
                    "625100493" +
                    "100493625",
            candidateMasks = fullBoard(
                "512000946" +
                "946512037" +
                "370946512" +
                "201354769" +
                "769201354" +
                "354000201" +
                "493625100" +
                "625100493" +
                "100493625",
                mapOf(
                    cell(0, 3) to mask(3, 7, 8),
                    cell(0, 4) to mask(3, 7),
                    cell(0, 5) to mask(3, 7),
                    cell(0, 7) to mask(8),
                    cell(1, 7) to mask(8),
                    cell(1, 8) to mask(8),
                    cell(5, 3) to mask(6, 7, 8, 9),
                    cell(5, 4) to mask(6, 7, 8, 9),
                    cell(5, 5) to mask(6, 7, 8, 9),
                    cell(6, 6) to mask(7, 8),
                    cell(6, 7) to mask(7, 8),
                    cell(6, 8) to mask(8),
                    cell(8, 0) to mask(7, 8),
                    cell(8, 3) to mask(8),
                    cell(8, 7) to mask(7, 8),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 3), 8, SECONDARY),
                CandidateHighlight(cell(5, 3), 8, SECONDARY),
                CandidateHighlight(cell(0, 7), 8, DEFINING),
                CandidateHighlight(cell(8, 3), 8, DEFINING),
                CandidateHighlight(cell(8, 7), 8, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SKYSCRAPER, SolutionType.TWO_STRING_KITE, SolutionType.EMPTY_RECTANGLE),
        keywords = listOf("turbot fish", "turbot crane", "single digit chain"),
    ),

    // ── Wings ────────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.XY_WING,
        theory = "An XY-Wing consists of three bivalue cells: a pivot with candidates {X,Y} and two pincers, " +
                "one with {X,Z} and one with {Y,Z}. The pivot sees both pincers. If the pivot is X, the {Y,Z} " +
                "pincer becomes Z. If the pivot is Y, the {X,Z} pincer becomes Z. Either way, Z is true in one pincer.\n\n" +
                "Any cell that sees both pincers can have Z eliminated, since Z must be in at least one of them.",
        howToSpot = "• Look for a bivalue cell (the pivot) that sees two other bivalue cells.\n" +
                "• The pivot shares one digit with each pincer, and the pincers share a third digit Z.\n" +
                "• Eliminate Z from cells seeing both pincers.",
        example = BoardExample(
            // Pivot (4,4)={3,7}, pincers (4,1)={3,5} and (1,4)={5,7}. Eliminate 5.
            puzzle = "826910354" +
                    "910000826" +
                    "354826910" +
                    "269183045" +
                    "183000269" +
                    "045269183" +
                    "692351048" +
                    "351048692" +
                    "048692351",
            candidateMasks = fullBoard(
                "826910354" +
                "910000826" +
                "354826910" +
                "269183045" +
                "183000269" +
                "045269183" +
                "692351048" +
                "351048692" +
                "048692351",
                mapOf(
                    cell(1, 3) to mask(3, 4, 5, 7),
                    cell(1, 4) to mask(5, 7),
                    cell(1, 5) to mask(3, 4, 5, 7),
                    cell(3, 7) to mask(7),
                    cell(3, 8) to mask(7),
                    cell(4, 3) to mask(4, 5, 7),
                    cell(4, 1) to mask(3, 5),
                    cell(4, 4) to mask(3, 7),
                    cell(4, 5) to mask(4, 5, 7),
                    cell(6, 6) to mask(7),
                    cell(6, 8) to mask(7),
                    cell(7, 6) to mask(7),
                    cell(7, 7) to mask(7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(4, 4), 3, SECONDARY),
                CandidateHighlight(cell(4, 4), 7, SECONDARY),
                CandidateHighlight(cell(4, 1), 3, DEFINING),
                CandidateHighlight(cell(4, 1), 5, DEFINING),
                CandidateHighlight(cell(1, 4), 5, DEFINING),
                CandidateHighlight(cell(1, 4), 7, DEFINING),
                CandidateHighlight(cell(1, 3), 5, ELIMINATION),
                CandidateHighlight(cell(1, 5), 5, ELIMINATION),
                CandidateHighlight(cell(4, 3), 5, ELIMINATION),
                CandidateHighlight(cell(4, 5), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.XYZ_WING, SolutionType.W_WING),
        keywords = listOf("xy-wing", "y-wing", "pivot", "pincer", "bivalue"),
    ),

    StrategyEntry(
        type = SolutionType.XYZ_WING,
        theory = "An XYZ-Wing extends the XY-Wing by allowing the pivot to have three candidates {X,Y,Z}. " +
                "One pincer has {X,Z} and the other has {Y,Z}. The same logic applies: no matter what the pivot " +
                "becomes, Z ends up in at least one pincer.\n\n" +
                "The elimination is more restricted: only cells seeing the pivot AND both pincers can lose Z, " +
                "since the pivot itself contains Z.",
        howToSpot = "• Find a cell with three candidates that sees two bivalue cells.\n" +
                "• The pivot's three digits must be distributed: one shared with each pincer, Z in all three.\n" +
                "• Eliminate Z from cells seeing all three cells (pivot + both pincers).",
        example = BoardExample(
            // Pivot (0,0)={2,5,8}, pincers (0,3)={2,8} and (1,1)={5,8}. Eliminate 8.
            puzzle = "000493167" +
                    "004167000" +
                    "167000493" +
                    "493016700" +
                    "016700493" +
                    "700493016" +
                    "030671940" +
                    "671940030" +
                    "940030671",
            candidateMasks = fullBoard(
                "000493167" +
                "004167000" +
                "167000493" +
                "493016700" +
                "016700493" +
                "700493016" +
                "030671940" +
                "671940030" +
                "940030671",
                mapOf(
                    cell(0, 0) to mask(2, 5, 8),
                    cell(0, 1) to mask(2, 5, 8),
                    cell(0, 2) to mask(2, 8),
                    cell(1, 0) to mask(2, 3, 5, 8, 9),
                    cell(1, 1) to mask(5, 8),
                    cell(1, 2) to mask(2, 3, 8, 9),
                    cell(1, 6) to mask(2, 5, 8, 9),
                    cell(1, 7) to mask(2, 5, 8, 9),
                    cell(1, 8) to mask(2, 5, 9),
                    cell(2, 3) to mask(2, 5, 8),
                    cell(2, 4) to mask(2, 5, 8),
                    cell(2, 5) to mask(2, 5, 8),
                    cell(3, 8) to mask(2, 5, 8),
                    cell(4, 8) to mask(2, 5, 8),
                    cell(6, 0) to mask(2, 5, 8),
                    cell(6, 7) to mask(2, 5, 8),
                    cell(7, 6) to mask(2, 5, 8),
                    cell(7, 7) to mask(2, 5, 8),
                    cell(8, 4) to mask(2, 5, 8),
                    cell(8, 5) to mask(2, 5, 8),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 2, SECONDARY),
                CandidateHighlight(cell(0, 0), 5, SECONDARY),
                CandidateHighlight(cell(0, 0), 8, SECONDARY),
                CandidateHighlight(cell(0, 2), 2, DEFINING),
                CandidateHighlight(cell(0, 2), 8, DEFINING),
                CandidateHighlight(cell(1, 1), 5, DEFINING),
                CandidateHighlight(cell(1, 1), 8, DEFINING),
                CandidateHighlight(cell(0, 1), 8, ELIMINATION),
                CandidateHighlight(cell(1, 0), 8, ELIMINATION),
                CandidateHighlight(cell(1, 2), 8, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.XY_WING, SolutionType.W_WING),
        keywords = listOf("xyz-wing", "pivot", "three candidates"),
    ),

    StrategyEntry(
        type = SolutionType.W_WING,
        theory = "A W-Wing consists of two bivalue cells with the same pair {X,Z} connected by a strong link on X. " +
                "If cell A is Z then cell B must also contain Z (since X is forced through the chain). " +
                "If cell A is X, then through the strong link, X propagates, and cell B becomes Z.\n\n" +
                "Either way, at least one of them is Z, so any cell seeing both can have Z eliminated.",
        howToSpot = "• Find two bivalue cells with identical candidates {X,Z}.\n" +
                "• Check if there's a strong link on digit X that connects a peer of cell A to a peer of cell B.\n" +
                "• Eliminate Z from cells seeing both bivalue cells.",
        example = BoardExample(
            // W-Wing: (1,1)={4,9} and (5,7)={4,9}, strong link on 4 via col 4: (1,4)-(5,4)
            puzzle = "837210564" +
                    "200564837" +
                    "564837210" +
                    "621340958" +
                    "340958621" +
                    "958002340" +
                    "076423185" +
                    "423185076" +
                    "185076423",
            candidateMasks = fullBoard(
                "837210564" +
                "200564837" +
                "564837210" +
                "621340958" +
                "340958621" +
                "958002340" +
                "076423185" +
                "423185076" +
                "185076423",
                mapOf(
                    cell(0, 3) to mask(9),
                    cell(1, 0) to mask(1, 9),
                    cell(1, 1) to mask(4, 9),
                    cell(1, 4) to mask(4),
                    cell(3, 4) to mask(7),
                    cell(3, 5) to mask(7),
                    cell(4, 4) to mask(7),
                    cell(5, 3) to mask(1, 6, 7),
                    cell(5, 4) to mask(4, 6, 7),
                    cell(5, 7) to mask(4, 9),
                    cell(5, 8) to mask(6, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(1, 1), 4, DEFINING),
                CandidateHighlight(cell(1, 1), 9, DEFINING),
                CandidateHighlight(cell(5, 7), 4, DEFINING),
                CandidateHighlight(cell(5, 7), 9, DEFINING),
                CandidateHighlight(cell(1, 4), 4, SECONDARY),
                CandidateHighlight(cell(5, 4), 4, SECONDARY),
                CandidateHighlight(cell(1, 0), 9, ELIMINATION),
                CandidateHighlight(cell(5, 3), 9, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.XY_WING, SolutionType.REMOTE_PAIR),
        keywords = listOf("w-wing", "bivalue", "strong link"),
    ),

    StrategyEntry(
        type = SolutionType.REMOTE_PAIR,
        theory = "A Remote Pair is a chain of bivalue cells all containing the same two candidates {X,Y}, " +
                "connected through shared units. When the chain has an even number of cells, " +
                "the two endpoints must contain different digits. Any cell seeing both endpoints can have " +
                "both X and Y eliminated.\n\n" +
                "Think of it as alternating colours along the chain: each link flips which digit is chosen.",
        howToSpot = "• Find bivalue cells with the same pair, forming a chain through shared rows/cols/boxes.\n" +
                "• Count the chain length. If even (4, 6, ...), the endpoints differ.\n" +
                "• Eliminate both digits from cells seeing both chain endpoints.",
        example = BoardExample(
            // Remote pair chain {3,8}: (0,0)-(0,4)-(4,4)-(4,8) — 4 cells, even
            puzzle = "000105000" +
                    "105926437" +
                    "926437105" +
                    "214350769" +
                    "350769214" +
                    "769214350" +
                    "431572096" +
                    "572096431" +
                    "096431572",
            candidateMasks = fullBoard(
                "000105000" +
                "105926437" +
                "926437105" +
                "214350769" +
                "350769214" +
                "769214350" +
                "431572096" +
                "572096431" +
                "096431572",
                mapOf(
                    cell(0, 0) to mask(3, 8),
                    cell(0, 1) to mask(4, 7, 8),
                    cell(0, 2) to mask(4, 7, 8),
                    cell(0, 4) to mask(3, 8),
                    cell(0, 5) to mask(6, 8),
                    cell(0, 6) to mask(2, 6, 8),
                    cell(0, 7) to mask(2, 6, 8),
                    cell(0, 8) to mask(2, 6),
                    cell(3, 3) to mask(8),
                    cell(6, 6) to mask(8),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 3, COLOR_A),
                CandidateHighlight(cell(0, 0), 8, COLOR_B),
                CandidateHighlight(cell(0, 4), 3, COLOR_B),
                CandidateHighlight(cell(0, 4), 8, COLOR_A),
                CandidateHighlight(cell(0, 1), 8, ELIMINATION),
                CandidateHighlight(cell(0, 2), 8, ELIMINATION),
                CandidateHighlight(cell(0, 5), 8, ELIMINATION),
                CandidateHighlight(cell(0, 6), 8, ELIMINATION),
                CandidateHighlight(cell(0, 7), 8, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.W_WING, SolutionType.XY_WING, SolutionType.SIMPLE_COLORS_TRAP),
        keywords = listOf("remote pair", "bivalue chain", "alternating"),
    ),

    // ── Coloring ─────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.SIMPLE_COLORS_TRAP,
        theory = "Simple Coloring (Trap) assigns two colours to a single-digit strong-link network. " +
                "Start from any conjugate pair and alternate colours along strong links (if the digit is in " +
                "cell A (green) of a conjugate pair, it's not in cell B (purple), and vice versa).\n\n" +
                "If a non-coloured cell sees cells of BOTH colours, the digit can be eliminated from that cell — " +
                "one colour must be true, and either way that cell can't have the digit. This is the 'trap' rule.",
        howToSpot = "• Filter for one digit. Build the conjugate-pair network.\n" +
                "• Colour alternating cells green and purple.\n" +
                "• Any uncoloured cell seeing both a green and a purple cell loses the digit.",
        example = BoardExample(
            // Coloring on digit 6: chain through conjugate pairs, trap at (3,7)
            puzzle = "915348270" +
                    "348270915" +
                    "270915348" +
                    "593001782" +
                    "001782593" +
                    "782593001" +
                    "150034820" +
                    "034820150" +
                    "820150034",
            candidateMasks = fullBoard(
                "915348270" +
                "348270915" +
                "270915348" +
                "593001782" +
                "001782593" +
                "782593001" +
                "150034820" +
                "034820150" +
                "820150034",
                mapOf(
                    cell(0, 7) to mask(6),
                    cell(3, 3) to mask(4, 6),
                    cell(3, 4) to mask(4, 6),
                    cell(4, 0) to mask(4, 6),
                    cell(4, 1) to mask(4, 6),
                    cell(5, 7) to mask(4, 6),
                    cell(5, 8) to mask(4, 6),
                    cell(6, 4) to mask(6, 7),
                    cell(6, 5) to mask(6, 7),
                    cell(6, 8) to mask(6, 7, 9),
                    cell(7, 0) to mask(6, 7),
                    cell(7, 5) to mask(6, 7),
                    cell(7, 6) to mask(6, 7),
                    cell(8, 3) to mask(6, 7, 9),
                    cell(8, 7) to mask(6, 7),
                    cell(8, 8) to mask(6, 7, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(3, 3), 6, COLOR_A),
                CandidateHighlight(cell(3, 4), 6, COLOR_B),
                CandidateHighlight(cell(4, 1), 6, COLOR_A),
                CandidateHighlight(cell(4, 0), 6, COLOR_B),
                CandidateHighlight(cell(5, 7), 6, COLOR_A),
                CandidateHighlight(cell(5, 8), 6, COLOR_B),
                CandidateHighlight(cell(3, 7), 6, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SIMPLE_COLORS_WRAP, SolutionType.MULTI_COLORS_1),
        keywords = listOf("coloring", "trap", "conjugate", "strong link", "colour"),
    ),

    StrategyEntry(
        type = SolutionType.SIMPLE_COLORS_WRAP,
        theory = "Simple Coloring (Wrap) uses the same two-colour network as the Trap rule, but detects a contradiction. " +
                "If two cells of the SAME colour see each other, that colour is impossible — so the opposite colour " +
                "must be the true one.\n\n" +
                "When a wrap is found, ALL cells of the contradicted colour can have the digit eliminated, " +
                "and all cells of the surviving colour are set to that digit.",
        howToSpot = "• Build the single-digit coloring network.\n" +
                "• Check if any two same-colour cells share a unit (row/col/box).\n" +
                "• If so, that colour is false: eliminate the digit from ALL cells of that colour.",
        example = BoardExample(
            // Coloring on digit 3: color A at (0,0),(4,8),(8,0) — (0,0) and (8,0) share col 0 → wrap
            puzzle = "006040802" +
                    "040802006" +
                    "802006040" +
                    "065020801" +
                    "020801065" +
                    "801065020" +
                    "004080200" +
                    "080200004" +
                    "200004080",
            candidateMasks = fullBoard(
                "006040802" +
                "040802006" +
                "802006040" +
                "065020801" +
                "020801065" +
                "801065020" +
                "004080200" +
                "080200004" +
                "200004080",
                mapOf(
                    cell(0, 0) to mask(1, 3, 5, 9),
                    cell(0, 2) to mask(1, 5, 9),
                    cell(0, 4) to mask(1, 3, 5, 7, 9),
                    cell(0, 6) to mask(7, 9),
                    cell(1, 1) to mask(1, 3, 7, 9),
                    cell(1, 5) to mask(3, 7, 9),
                    cell(1, 7) to mask(1, 3, 7, 9),
                    cell(1, 8) to mask(1, 3, 7, 9),
                    cell(2, 3) to mask(1, 3, 5, 7, 9),
                    cell(2, 5) to mask(1, 3, 5, 7, 9),
                    cell(2, 7) to mask(1, 3, 5, 7, 9),
                    cell(3, 2) to mask(3, 4, 7, 9),
                    cell(3, 4) to mask(3, 4, 7, 9),
                    cell(3, 6) to mask(3, 4, 7, 9),
                    cell(4, 0) to mask(3, 4, 7, 9),
                    cell(4, 3) to mask(3, 4, 7, 9),
                    cell(4, 5) to mask(3, 4, 7),
                    cell(5, 3) to mask(3, 4, 7, 9),
                    cell(5, 5) to mask(3, 4, 7, 9),
                    cell(5, 7) to mask(3, 4, 7, 9),
                    cell(6, 1) to mask(1, 3, 5, 6, 7, 9),
                    cell(6, 4) to mask(1, 3, 5, 6, 7, 9),
                    cell(6, 6) to mask(1, 3, 5, 6, 7, 9),
                    cell(7, 0) to mask(1, 3, 5, 6, 7, 9),
                    cell(7, 3) to mask(1, 3, 5, 6, 7, 9),
                    cell(7, 5) to mask(1, 3, 5, 6, 7, 9),
                    cell(7, 7) to mask(1, 3, 5, 6, 7),
                    cell(7, 8) to mask(1, 3, 5, 6, 7),
                    cell(8, 2) to mask(1, 3, 5, 6, 7, 9),
                    cell(8, 4) to mask(1, 3, 5, 6, 7),
                    cell(8, 6) to mask(1, 3, 5, 6, 7, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 3, COLOR_A),
                CandidateHighlight(cell(0, 4), 3, COLOR_B),
                CandidateHighlight(cell(4, 0), 3, COLOR_B),
                CandidateHighlight(cell(4, 5), 3, COLOR_A),
                CandidateHighlight(cell(8, 0), 3, COLOR_A),
                CandidateHighlight(cell(8, 4), 3, COLOR_B),
                // Color A wraps: (0,0) and (8,0) share col 0
                CandidateHighlight(cell(0, 0), 3, ELIMINATION),
                CandidateHighlight(cell(4, 5), 3, ELIMINATION),
                CandidateHighlight(cell(8, 0), 3, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SIMPLE_COLORS_TRAP, SolutionType.MULTI_COLORS_1),
        keywords = listOf("coloring", "wrap", "contradiction", "colour"),
    ),

    StrategyEntry(
        type = SolutionType.MULTI_COLORS_1,
        theory = "Multi-Coloring Type 1 extends Simple Coloring by linking two separate coloring clusters. " +
                "If a colour from cluster 1 sees a colour from cluster 2, one of those colours must be false. " +
                "This 'bridges' the clusters, allowing eliminations in the combined network.\n\n" +
                "Type 1 handles the case where a colour in one cluster directly contradicts a colour in another.",
        howToSpot = "• Build coloring clusters for a single digit (separate connected components).\n" +
                "• Check inter-cluster visibility between opposite colours.\n" +
                "• When colours from different clusters conflict, merge the information for elimination.",
        example = BoardExample(
            puzzle = "917438265" +
                    "438265917" +
                    "265917438" +
                    "000143000" +
                    "143000002" +
                    "002000143" +
                    "324806571" +
                    "806571324" +
                    "571324806",
            candidateMasks = fullBoard(
                "917438265" +
                "438265917" +
                "265917438" +
                "000143000" +
                "143000002" +
                "002000143" +
                "324806571" +
                "806571324" +
                "571324806",
                mapOf(
                    cell(3, 0) to mask(6, 8),
                    cell(3, 1) to mask(5, 6, 8),
                    cell(3, 2) to mask(6, 8, 9),
                    cell(3, 6) to mask(6, 7, 8, 9),
                    cell(3, 7) to mask(7, 8),
                    cell(3, 8) to mask(6, 8, 9),
                    cell(4, 3) to mask(5, 6, 7, 8, 9),
                    cell(4, 4) to mask(5, 6, 7, 8, 9),
                    cell(4, 5) to mask(5, 6, 7, 8, 9),
                    cell(5, 0) to mask(6, 8, 9),
                    cell(5, 1) to mask(5, 6, 7, 8, 9),
                    cell(5, 2) to mask(6, 8, 9),
                    cell(5, 3) to mask(5, 6, 7, 8, 9),
                    cell(5, 4) to mask(5, 6, 7, 8, 9),
                    cell(5, 5) to mask(5, 6, 7, 8, 9),
                ),
            ),
            highlights = listOf(
                // Cluster 1 on digit 9
                CandidateHighlight(cell(3, 2), 9, COLOR_A),
                CandidateHighlight(cell(3, 8), 9, COLOR_B),
                CandidateHighlight(cell(5, 0), 9, COLOR_A),
                CandidateHighlight(cell(5, 2), 9, COLOR_B),
                // Cluster 2
                CandidateHighlight(cell(4, 3), 9, COLOR_A),
                CandidateHighlight(cell(4, 5), 9, COLOR_B),
                CandidateHighlight(cell(5, 3), 9, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.SIMPLE_COLORS_TRAP, SolutionType.MULTI_COLORS_2),
        keywords = listOf("multi-coloring", "multi colors", "cluster bridge"),
    ),

    StrategyEntry(
        type = SolutionType.MULTI_COLORS_2,
        theory = "Multi-Coloring Type 2 handles a different inter-cluster interaction. When a colour from one cluster " +
                "sees both colours of another cluster, that first colour must be false (since one of the second " +
                "cluster's colours must be true).\n\n" +
                "This eliminates the digit from all cells of the contradicted colour in the first cluster.",
        howToSpot = "• Build separate coloring clusters for a digit.\n" +
                "• Check if a colour in cluster A sees both colours of cluster B.\n" +
                "• If so, that colour in A is false — eliminate the digit from all its cells.",
        example = BoardExample(
            puzzle = "829415736" +
                    "415736829" +
                    "736829415" +
                    "000143000" +
                    "143000000" +
                    "000000143" +
                    "361278594" +
                    "278594361" +
                    "594361278",
            candidateMasks = fullBoard(
                "829415736" +
                "415736829" +
                "736829415" +
                "000143000" +
                "143000000" +
                "000000143" +
                "361278594" +
                "278594361" +
                "594361278",
                mapOf(
                    cell(3, 0) to mask(5, 6, 7),
                    cell(3, 1) to mask(5, 6, 7, 8),
                    cell(3, 2) to mask(2, 6, 8),
                    cell(3, 6) to mask(2, 5, 6, 7, 8, 9),
                    cell(3, 7) to mask(2, 5, 6, 7, 8, 9),
                    cell(3, 8) to mask(2, 5, 6, 7, 8, 9),
                    cell(4, 3) to mask(2, 5, 6, 7, 8, 9),
                    cell(4, 4) to mask(2, 5, 6, 7, 8, 9),
                    cell(4, 5) to mask(2, 5, 6, 7, 8, 9),
                    cell(4, 6) to mask(2, 5, 6, 7, 8),
                    cell(4, 7) to mask(2, 5, 6, 7, 8),
                    cell(4, 8) to mask(2, 5, 6, 7, 8),
                    cell(5, 0) to mask(5, 6, 7),
                    cell(5, 1) to mask(2, 5, 6, 7, 8, 9),
                    cell(5, 2) to mask(2, 6, 8, 9),
                    cell(5, 3) to mask(2, 5, 6, 7, 8, 9),
                    cell(5, 4) to mask(2, 5, 6, 7, 8, 9),
                    cell(5, 5) to mask(2, 5, 6, 7, 8, 9),
                ),
            ),
            highlights = listOf(
                // Cluster A on digit 5
                CandidateHighlight(cell(3, 0), 5, COLOR_A),
                CandidateHighlight(cell(3, 1), 5, COLOR_B),
                CandidateHighlight(cell(5, 0), 5, COLOR_B),
                CandidateHighlight(cell(5, 1), 5, COLOR_A),
                // Cluster B
                CandidateHighlight(cell(4, 3), 5, COLOR_A),
                CandidateHighlight(cell(4, 6), 5, COLOR_B),
                // A's COLOR_A sees both B colors → eliminate A's COLOR_A
                CandidateHighlight(cell(3, 0), 5, ELIMINATION),
                CandidateHighlight(cell(5, 1), 5, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.MULTI_COLORS_1, SolutionType.SIMPLE_COLORS_TRAP),
        keywords = listOf("multi-coloring", "multi colors type 2", "cluster"),
    ),

    // ── Chains ───────────────────────────────────────────────────────────────

    StrategyEntry(
        type = SolutionType.X_CHAIN,
        theory = "An X-Chain is a single-digit chain of alternating strong and weak links. Starting from a strong link, " +
                "the chain proceeds: strong → weak → strong → weak → ... → strong. Because the chain starts and " +
                "ends with strong links, both endpoints can't be false simultaneously.\n\n" +
                "Any cell that sees both endpoints can have the digit eliminated.",
        howToSpot = "• Filter for one digit. Build the conjugate-pair (strong link) network.\n" +
                "• Find chains that alternate strong-weak links and have an odd number of links.\n" +
                "• The endpoints are guaranteed: at least one is true.\n" +
                "• Eliminate from cells seeing both endpoints.",
        example = BoardExample(
            // X-Chain on digit 2: (0,1)=(0,6)-(3,6)=(3,2)-(7,2)=(7,8)
            puzzle = "900000503" +
                    "645397218" +
                    "318254679" +
                    "870000456" +
                    "456138792" +
                    "231769840" +
                    "790000364" +
                    "560000920" +
                    "184923507",
            candidateMasks = fullBoard(
                "900000503" +
                "645397218" +
                "318254679" +
                "870000456" +
                "456138792" +
                "231769840" +
                "790000364" +
                "560000920" +
                "184923507",
                mapOf(
                    cell(0, 0) to mask(2),
                    cell(0, 1) to mask(2, 7),
                    cell(0, 2) to mask(7),
                    cell(0, 3) to mask(1, 4, 6, 8),
                    cell(0, 4) to mask(1, 4, 6, 8),
                    cell(0, 5) to mask(1, 4, 6, 8),
                    cell(0, 8) to mask(1),
                    cell(3, 3) to mask(2, 9),
                    cell(3, 4) to mask(2, 9),
                    cell(3, 5) to mask(1, 2, 9),
                    cell(5, 8) to mask(5),
                    cell(6, 3) to mask(1, 2, 5, 8),
                    cell(6, 4) to mask(1, 2, 5, 8),
                    cell(6, 5) to mask(1, 2, 5, 8),
                    cell(7, 0) to mask(3),
                    cell(7, 3) to mask(1, 4, 7, 8),
                    cell(7, 4) to mask(1, 4, 7, 8),
                    cell(7, 5) to mask(1, 4, 7, 8),
                    cell(7, 7) to mask(1, 7, 8),
                    cell(7, 8) to mask(1, 7, 8),
                    cell(8, 6) to mask(6),
                    cell(8, 8) to mask(6),
                    cell(0, 6) to mask(2),
                    cell(3, 6) to mask(1, 2),
                    cell(3, 2) to mask(2, 9),
                    cell(7, 2) to mask(2, 7),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 1), 2, DEFINING),
                CandidateHighlight(cell(0, 6), 2, SECONDARY),
                CandidateHighlight(cell(3, 6), 2, SECONDARY),
                CandidateHighlight(cell(3, 2), 2, SECONDARY),
                CandidateHighlight(cell(7, 2), 2, SECONDARY),
                CandidateHighlight(cell(7, 8), 2, DEFINING),
            ),
        ),
        relatedTypes = listOf(SolutionType.XY_CHAIN, SolutionType.SIMPLE_COLORS_TRAP),
        keywords = listOf("x-chain", "single digit chain", "alternating inference"),
    ),

    StrategyEntry(
        type = SolutionType.XY_CHAIN,
        theory = "An XY-Chain is a chain of bivalue cells where each consecutive pair shares a digit through a " +
                "common unit (weak link), and within each cell the two candidates form a strong link. " +
                "The chain alternates: one digit is 'on' at each end.\n\n" +
                "If the chain starts with digit X at cell A and ends with digit X at cell B, then at least one " +
                "endpoint has X, so cells seeing both A and B can have X eliminated.",
        howToSpot = "• Find sequences of bivalue cells connected through shared units.\n" +
                "• Each cell's internal strong link alternates truth values along the chain.\n" +
                "• Matching digits at both endpoints allow elimination from cells seeing both.",
        example = BoardExample(
            // XY-Chain: (0,0){1,4}-(0,5){4,7}-(3,5){7,2}-(3,8){2,1}
            // Starts/ends with 1 → eliminate 1 from cells seeing both (0,0) and (3,8)
            puzzle = "000836002" +
                    "836002000" +
                    "002000836" +
                    "000648200" +
                    "648200003" +
                    "203003648" +
                    "064382510" +
                    "382510064" +
                    "510064382",
            candidateMasks = fullBoard(
                "000836002" +
                "836002000" +
                "002000836" +
                "000648200" +
                "648200003" +
                "203003648" +
                "064382510" +
                "382510064" +
                "510064382",
                mapOf(
                    cell(0, 0) to mask(1, 4),
                    cell(0, 1) to mask(5, 7, 9),
                    cell(0, 2) to mask(5, 7, 9),
                    cell(0, 5) to mask(4, 7),
                    cell(0, 7) to mask(5, 9),
                    cell(0, 8) to mask(5, 9),
                    cell(1, 3) to mask(1, 4, 5, 7, 9),
                    cell(1, 4) to mask(1, 4, 5, 7, 9),
                    cell(1, 6) to mask(1, 5, 7, 9),
                    cell(1, 7) to mask(1, 5, 7, 9),
                    cell(1, 8) to mask(1, 5, 7, 9),
                    cell(2, 3) to mask(1, 4, 5, 7, 9),
                    cell(2, 4) to mask(1, 4, 5, 7, 9),
                    cell(2, 5) to mask(1, 4, 5, 7, 9),
                    cell(3, 0) to mask(1, 5, 7, 9),
                    cell(3, 1) to mask(1, 5, 7, 9),
                    cell(3, 2) to mask(1, 5, 7, 9),
                    cell(3, 5) to mask(2, 7),
                    cell(3, 7) to mask(3, 5, 7, 9),
                    cell(3, 8) to mask(1, 2),
                    cell(4, 3) to mask(5, 7, 9),
                    cell(4, 4) to mask(1, 5, 7, 9),
                    cell(4, 8) to mask(1, 5, 7, 9),
                    cell(5, 0) to mask(5, 7, 9),
                    cell(5, 2) to mask(5, 7, 9),
                    cell(5, 3) to mask(1, 5, 7, 9),
                ),
            ),
            highlights = listOf(
                CandidateHighlight(cell(0, 0), 1, DEFINING),
                CandidateHighlight(cell(0, 0), 4, SECONDARY),
                CandidateHighlight(cell(0, 5), 4, SECONDARY),
                CandidateHighlight(cell(0, 5), 7, SECONDARY),
                CandidateHighlight(cell(3, 5), 7, SECONDARY),
                CandidateHighlight(cell(3, 5), 2, SECONDARY),
                CandidateHighlight(cell(3, 8), 2, SECONDARY),
                CandidateHighlight(cell(3, 8), 1, DEFINING),
                CandidateHighlight(cell(3, 0), 1, ELIMINATION),
                CandidateHighlight(cell(3, 1), 1, ELIMINATION),
                CandidateHighlight(cell(3, 2), 1, ELIMINATION),
            ),
        ),
        relatedTypes = listOf(SolutionType.X_CHAIN, SolutionType.XY_WING),
        keywords = listOf("xy-chain", "bivalue chain", "alternating inference"),
    ),
)
