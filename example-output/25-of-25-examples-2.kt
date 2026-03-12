// ═══ Generated Examples ═══
// Found 25 / 25 types

// Full House
example = BoardExample(
    puzzle = "..9..........6..155..4.8...61.....8.9846.2531.3.....42...3.5..612..8..........4..",
    candidateMasks = intArrayOf(
        206, 104, 0, 83, 87, 69, 230, 98, 204,
        206, 72, 198, 322, 0, 324, 454, 0, 0,
        0, 96, 103, 0, 327, 0, 358, 354, 324,
        0, 0, 82, 336, 348, 332, 320, 0, 320,
        0, 0, 0, 0, 64, 0, 0, 0, 0,
        64, 0, 80, 465, 337, 321, 352, 0, 0,
        200, 328, 192, 0, 331, 0, 451, 322, 0,
        0, 0, 116, 320, 0, 360, 324, 336, 324,
        196, 368, 244, 323, 323, 353, 0, 338, 452
    ),
    highlights = listOf(
        CandidateHighlight(40, 7, HighlightRole.DEFINING),
    ),
),

// Naked Single
example = BoardExample(
    puzzle = ".5.1....7....2.....83.54...8.7..54...32...86...59..2.1...47.15.....1....1....6.9.",
    candidateMasks = intArrayOf(
        298, 0, 296, 0, 420, 388, 292, 142, 0,
        360, 361, 297, 228, 0, 452, 308, 141, 444,
        354, 0, 0, 96, 0, 0, 288, 3, 290,
        0, 289, 0, 38, 36, 0, 0, 4, 260,
        264, 0, 0, 64, 8, 65, 0, 0, 272,
        40, 40, 0, 0, 172, 196, 0, 68, 0,
        294, 290, 416, 0, 0, 390, 0, 0, 166,
        382, 362, 424, 150, 0, 390, 100, 206, 174,
        0, 74, 136, 150, 132, 0, 68, 0, 142
    ),
    highlights = listOf(
        CandidateHighlight(34, 3, HighlightRole.DEFINING),
    ),
),

// Hidden Single
example = BoardExample(
    puzzle = "6..19.54..97....2....5..6..2.98.........1.........49.8..4..3....1....27..82.61..9",
    candidateMasks = intArrayOf(
        0, 6, 132, 0, 0, 194, 0, 0, 68,
        157, 0, 0, 44, 140, 160, 133, 0, 5,
        141, 14, 133, 0, 206, 194, 0, 389, 69,
        0, 124, 0, 0, 84, 112, 77, 53, 125,
        220, 124, 180, 358, 0, 370, 76, 52, 126,
        85, 116, 53, 102, 86, 0, 0, 53, 0,
        336, 112, 0, 322, 210, 0, 129, 177, 49,
        276, 0, 52, 264, 152, 400, 0, 0, 60,
        84, 0, 0, 72, 0, 0, 12, 20, 0
    ),
    highlights = listOf(
        CandidateHighlight(9, 5, HighlightRole.DEFINING),
    ),
),

// Locked Candidates (Pointing)
example = BoardExample(
    puzzle = "3.6...915..96.53.....391..6.7381265.862....3...5.6382.2...36.9..371.9.6.691...4.3",
    candidateMasks = intArrayOf(
        0, 138, 0, 74, 202, 200, 0, 0, 0,
        73, 139, 0, 0, 202, 0, 0, 200, 202,
        88, 154, 136, 0, 0, 0, 66, 200, 0,
        264, 0, 0, 0, 0, 0, 0, 0, 264,
        0, 0, 0, 344, 88, 72, 65, 0, 329,
        265, 9, 0, 328, 0, 0, 0, 0, 329,
        0, 152, 136, 88, 0, 0, 81, 0, 193,
        24, 0, 0, 0, 154, 0, 18, 0, 130,
        0, 0, 0, 82, 210, 192, 0, 192, 0
    ),
    highlights = listOf(
        CandidateHighlight(45, 1, HighlightRole.DEFINING),
        CandidateHighlight(46, 1, HighlightRole.DEFINING),
        CandidateHighlight(53, 1, HighlightRole.ELIMINATION),
    ),
),

// Locked Candidates (Claiming)
example = BoardExample(
    puzzle = ".8..91...9...4813..4.3268...94287..3...639...6..4159..4671532...29874..1...962.4.",
    candidateMasks = intArrayOf(
        86, 0, 54, 80, 0, 0, 120, 114, 122,
        0, 80, 50, 80, 0, 0, 0, 0, 114,
        81, 0, 17, 0, 0, 0, 0, 336, 336,
        17, 0, 0, 0, 0, 0, 48, 49, 0,
        211, 81, 147, 0, 0, 0, 88, 211, 218,
        0, 68, 134, 0, 0, 0, 0, 194, 194,
        0, 0, 0, 0, 0, 0, 0, 384, 384,
        20, 0, 0, 0, 0, 0, 52, 48, 0,
        149, 21, 149, 0, 0, 0, 84, 0, 80
    ),
    highlights = listOf(
        CandidateHighlight(8, 6, HighlightRole.DEFINING),
        CandidateHighlight(17, 6, HighlightRole.DEFINING),
        CandidateHighlight(6, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(7, 6, HighlightRole.ELIMINATION),
    ),
),

// Locked Pair
example = BoardExample(
    puzzle = ".4.3.6.....381..646....43.2.3...817...8...43..915.3.2.3.92....7.5..37......6...53",
    candidateMasks = intArrayOf(
        467, 0, 82, 0, 338, 0, 464, 385, 129,
        338, 66, 0, 0, 0, 274, 336, 0, 0,
        0, 193, 80, 320, 336, 0, 0, 385, 0,
        26, 0, 58, 264, 298, 0, 0, 0, 304,
        82, 98, 0, 321, 354, 259, 0, 0, 304,
        72, 0, 0, 0, 104, 0, 160, 0, 160,
        0, 161, 0, 0, 152, 17, 160, 137, 0,
        139, 0, 42, 265, 0, 0, 418, 393, 161,
        203, 195, 74, 0, 392, 257, 386, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(51, 6, HighlightRole.DEFINING),
        CandidateHighlight(51, 8, HighlightRole.DEFINING),
        CandidateHighlight(53, 6, HighlightRole.DEFINING),
        CandidateHighlight(53, 8, HighlightRole.DEFINING),
        CandidateHighlight(35, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(44, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(49, 6, HighlightRole.ELIMINATION),
    ),
),

// Locked Triple
example = BoardExample(
    puzzle = "....83...5.....18.84...1..6..16...979.......173...94..3..9..258..2.....9...72....",
    candidateMasks = intArrayOf(
        35, 355, 352, 26, 0, 0, 336, 74, 26,
        0, 322, 324, 10, 360, 106, 0, 0, 14,
        0, 0, 324, 18, 336, 0, 340, 70, 0,
        10, 146, 0, 0, 28, 154, 148, 0, 0,
        0, 178, 184, 158, 92, 218, 180, 38, 0,
        0, 0, 176, 147, 17, 0, 0, 34, 18,
        0, 97, 104, 0, 41, 40, 0, 0, 0,
        41, 177, 0, 157, 61, 184, 96, 105, 0,
        41, 433, 440, 0, 0, 184, 36, 45, 12
    ),
    highlights = listOf(
        CandidateHighlight(3, 2, HighlightRole.DEFINING),
        CandidateHighlight(3, 4, HighlightRole.DEFINING),
        CandidateHighlight(3, 5, HighlightRole.DEFINING),
        CandidateHighlight(12, 2, HighlightRole.DEFINING),
        CandidateHighlight(12, 4, HighlightRole.DEFINING),
        CandidateHighlight(21, 2, HighlightRole.DEFINING),
        CandidateHighlight(21, 5, HighlightRole.DEFINING),
        CandidateHighlight(13, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(14, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(14, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(22, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(39, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(39, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(39, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(48, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(48, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(66, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(66, 5, HighlightRole.ELIMINATION),
    ),
),

// Naked Pair
example = BoardExample(
    puzzle = ".346...598....46..6....14..5..369247479.1.3653624579....6.....4.4.1....625.74689.",
    candidateMasks = intArrayOf(
        65, 0, 0, 0, 130, 130, 65, 0, 0,
        0, 259, 81, 272, 68, 0, 0, 69, 7,
        0, 258, 80, 272, 68, 0, 0, 196, 134,
        0, 129, 129, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 130, 0, 130, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 129, 129,
        321, 129, 0, 130, 386, 150, 81, 71, 0,
        320, 0, 196, 0, 386, 150, 80, 70, 0,
        0, 0, 5, 0, 0, 0, 0, 0, 5
    ),
    highlights = listOf(
        CandidateHighlight(28, 1, HighlightRole.DEFINING),
        CandidateHighlight(28, 8, HighlightRole.DEFINING),
        CandidateHighlight(55, 1, HighlightRole.DEFINING),
        CandidateHighlight(55, 8, HighlightRole.DEFINING),
        CandidateHighlight(10, 1, HighlightRole.ELIMINATION),
    ),
),

// Naked Triple
example = BoardExample(
    puzzle = "....5....3271...95.85.3......86139....14276....25987....9.4136.2.39.51.781.37.5.9",
    candidateMasks = intArrayOf(
        297, 296, 40, 194, 0, 266, 138, 69, 37,
        0, 0, 0, 0, 160, 40, 136, 0, 0,
        297, 0, 0, 66, 0, 266, 10, 65, 33,
        88, 88, 0, 0, 0, 0, 0, 18, 10,
        272, 276, 0, 0, 0, 0, 0, 148, 132,
        40, 44, 0, 0, 0, 0, 0, 5, 13,
        80, 80, 0, 130, 0, 0, 0, 0, 130,
        0, 40, 0, 0, 160, 0, 0, 136, 0,
        0, 0, 40, 0, 0, 34, 0, 10, 0
    ),
    highlights = listOf(
        CandidateHighlight(7, 1, HighlightRole.DEFINING),
        CandidateHighlight(7, 3, HighlightRole.DEFINING),
        CandidateHighlight(7, 7, HighlightRole.DEFINING),
        CandidateHighlight(25, 1, HighlightRole.DEFINING),
        CandidateHighlight(25, 7, HighlightRole.DEFINING),
        CandidateHighlight(52, 1, HighlightRole.DEFINING),
        CandidateHighlight(52, 3, HighlightRole.DEFINING),
        CandidateHighlight(43, 3, HighlightRole.ELIMINATION),
    ),
),

// Naked Quadruple
example = BoardExample(
    puzzle = "4.5.6.8....2..71.41.78..9..7.3..62..5.6...7.9..47..3.63416.25..6785..4..259.7.6.3",
    candidateMasks = intArrayOf(
        0, 260, 0, 263, 0, 261, 0, 70, 66,
        384, 420, 0, 260, 276, 0, 0, 52, 0,
        0, 36, 0, 0, 30, 28, 0, 54, 18,
        0, 385, 0, 265, 409, 0, 0, 153, 145,
        0, 131, 0, 15, 143, 141, 0, 137, 0,
        384, 387, 0, 0, 403, 401, 0, 145, 0,
        0, 0, 0, 0, 384, 0, 0, 448, 192,
        0, 0, 0, 0, 261, 261, 0, 259, 3,
        0, 0, 0, 9, 0, 137, 0, 129, 0
    ),
    highlights = listOf(
        CandidateHighlight(34, 1, HighlightRole.DEFINING),
        CandidateHighlight(34, 4, HighlightRole.DEFINING),
        CandidateHighlight(34, 5, HighlightRole.DEFINING),
        CandidateHighlight(34, 8, HighlightRole.DEFINING),
        CandidateHighlight(43, 1, HighlightRole.DEFINING),
        CandidateHighlight(43, 4, HighlightRole.DEFINING),
        CandidateHighlight(43, 8, HighlightRole.DEFINING),
        CandidateHighlight(52, 1, HighlightRole.DEFINING),
        CandidateHighlight(52, 5, HighlightRole.DEFINING),
        CandidateHighlight(52, 8, HighlightRole.DEFINING),
        CandidateHighlight(79, 1, HighlightRole.DEFINING),
        CandidateHighlight(79, 8, HighlightRole.DEFINING),
        CandidateHighlight(16, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(25, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(61, 8, HighlightRole.ELIMINATION),
        CandidateHighlight(70, 1, HighlightRole.ELIMINATION),
    ),
),

// Hidden Pair
example = BoardExample(
    puzzle = "2....83...397...4.....9.7..5.....634...426...641...9.2..5.1.....1...7865...5....3",
    candidateMasks = intArrayOf(
        0, 112, 104, 33, 56, 0, 0, 273, 289,
        129, 0, 0, 0, 48, 19, 19, 0, 161,
        137, 176, 168, 39, 0, 31, 0, 147, 161,
        0, 194, 194, 385, 192, 257, 0, 0, 0,
        452, 448, 196, 0, 0, 0, 17, 209, 193,
        0, 0, 0, 132, 212, 20, 0, 192, 0,
        460, 482, 0, 422, 0, 270, 10, 322, 320,
        268, 0, 14, 262, 12, 0, 0, 0, 0,
        456, 482, 234, 0, 168, 266, 11, 323, 0
    ),
    highlights = listOf(
        CandidateHighlight(30, 1, HighlightRole.DEFINING),
        CandidateHighlight(30, 9, HighlightRole.DEFINING),
        CandidateHighlight(32, 1, HighlightRole.DEFINING),
        CandidateHighlight(32, 9, HighlightRole.DEFINING),
        CandidateHighlight(30, 8, HighlightRole.ELIMINATION),
    ),
),

// Hidden Triple
example = BoardExample(
    puzzle = ".9.2.....28694.....75.8..29.1.8...755...9...884.5...9.73..591...5.7289.3.....3.5.",
    candidateMasks = intArrayOf(
        13, 0, 13, 0, 101, 113, 248, 169, 105,
        0, 0, 0, 0, 0, 81, 84, 5, 65,
        13, 0, 0, 37, 0, 33, 40, 0, 0,
        292, 0, 262, 0, 36, 42, 46, 0, 0,
        0, 34, 70, 37, 0, 107, 46, 45, 0,
        0, 0, 70, 0, 101, 99, 38, 0, 35,
        0, 0, 138, 40, 0, 0, 0, 168, 42,
        41, 0, 9, 0, 0, 0, 0, 40, 0,
        296, 34, 394, 41, 33, 0, 234, 0, 106
    ),
    highlights = listOf(
        CandidateHighlight(6, 5, HighlightRole.DEFINING),
        CandidateHighlight(6, 7, HighlightRole.DEFINING),
        CandidateHighlight(6, 8, HighlightRole.DEFINING),
        CandidateHighlight(15, 5, HighlightRole.DEFINING),
        CandidateHighlight(15, 7, HighlightRole.DEFINING),
        CandidateHighlight(78, 7, HighlightRole.DEFINING),
        CandidateHighlight(78, 8, HighlightRole.DEFINING),
        CandidateHighlight(6, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(6, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(15, 3, HighlightRole.ELIMINATION),
        CandidateHighlight(78, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(78, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(78, 6, HighlightRole.ELIMINATION),
    ),
),

// Hidden Quadruple
example = BoardExample(
    puzzle = ".9..35271213497685.5.1.2943.2.94..6...........6..58.....5..6194689314752142579836",
    candidateMasks = intArrayOf(
        136, 0, 168, 160, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        192, 0, 224, 0, 160, 0, 0, 0, 0,
        212, 0, 193, 0, 0, 5, 20, 0, 192,
        476, 68, 201, 98, 34, 5, 28, 3, 448,
        332, 0, 73, 66, 0, 0, 12, 3, 320,
        68, 68, 0, 130, 130, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(36, 4, HighlightRole.DEFINING),
        CandidateHighlight(36, 5, HighlightRole.DEFINING),
        CandidateHighlight(36, 8, HighlightRole.DEFINING),
        CandidateHighlight(36, 9, HighlightRole.DEFINING),
        CandidateHighlight(38, 4, HighlightRole.DEFINING),
        CandidateHighlight(38, 8, HighlightRole.DEFINING),
        CandidateHighlight(42, 4, HighlightRole.DEFINING),
        CandidateHighlight(42, 5, HighlightRole.DEFINING),
        CandidateHighlight(44, 8, HighlightRole.DEFINING),
        CandidateHighlight(44, 9, HighlightRole.DEFINING),
        CandidateHighlight(36, 3, HighlightRole.ELIMINATION),
        CandidateHighlight(36, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(38, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(38, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(42, 3, HighlightRole.ELIMINATION),
        CandidateHighlight(44, 7, HighlightRole.ELIMINATION),
    ),
),

// X-Wing
example = BoardExample(
    puzzle = ".1.54..6.5.698.4..8.4261.95.8.752634342816579765439....536.894.6.8194.5349.3.5.86",
    candidateMasks = intArrayOf(
        258, 0, 320, 0, 0, 68, 198, 0, 194,
        0, 70, 0, 0, 0, 68, 0, 3, 67,
        0, 68, 0, 0, 0, 0, 68, 0, 0,
        257, 0, 257, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 131, 3, 131,
        3, 0, 0, 0, 66, 0, 0, 0, 67,
        0, 66, 0, 0, 0, 0, 66, 0, 0,
        0, 0, 65, 0, 66, 0, 67, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(19, 7, HighlightRole.DEFINING),
        CandidateHighlight(24, 7, HighlightRole.DEFINING),
        CandidateHighlight(64, 7, HighlightRole.DEFINING),
        CandidateHighlight(69, 7, HighlightRole.DEFINING),
        CandidateHighlight(10, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(6, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(78, 7, HighlightRole.ELIMINATION),
    ),
),

// Swordfish
example = BoardExample(
    puzzle = "87362..5.1.25.3.784.5178.2373425.8.665..8.73228.36754.9167352843478.2.65528..63.7",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 264, 265, 0, 257,
        0, 288, 0, 0, 264, 0, 296, 0, 0,
        0, 288, 0, 0, 0, 0, 288, 0, 0,
        0, 0, 0, 0, 0, 257, 0, 257, 0,
        0, 0, 257, 264, 0, 265, 0, 0, 0,
        0, 0, 257, 0, 0, 0, 0, 0, 257,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 257, 0, 257, 0, 0,
        0, 0, 0, 264, 265, 0, 0, 257, 0
    ),
    highlights = listOf(
        CandidateHighlight(10, 9, HighlightRole.DEFINING),
        CandidateHighlight(13, 9, HighlightRole.DEFINING),
        CandidateHighlight(15, 9, HighlightRole.DEFINING),
        CandidateHighlight(19, 9, HighlightRole.DEFINING),
        CandidateHighlight(24, 9, HighlightRole.DEFINING),
        CandidateHighlight(67, 9, HighlightRole.DEFINING),
        CandidateHighlight(69, 9, HighlightRole.DEFINING),
        CandidateHighlight(76, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(6, 9, HighlightRole.ELIMINATION),
    ),
),

// Jellyfish
example = BoardExample(
    puzzle = "57.43.2..124..537.3..127.459.731.524.152.47392437596...915724.3452..31.773..41.52",
    candidateMasks = intArrayOf(
        0, 0, 384, 0, 0, 160, 0, 385, 161,
        0, 0, 0, 416, 416, 0, 0, 0, 160,
        0, 160, 416, 0, 0, 0, 384, 0, 0,
        0, 160, 0, 0, 0, 160, 0, 0, 0,
        160, 0, 0, 0, 160, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 129, 129,
        160, 0, 0, 0, 0, 0, 0, 160, 0,
        0, 0, 0, 416, 416, 0, 0, 416, 0,
        0, 0, 160, 416, 0, 0, 384, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(19, 8, HighlightRole.DEFINING),
        CandidateHighlight(28, 8, HighlightRole.DEFINING),
        CandidateHighlight(2, 8, HighlightRole.DEFINING),
        CandidateHighlight(20, 8, HighlightRole.DEFINING),
        CandidateHighlight(74, 8, HighlightRole.DEFINING),
        CandidateHighlight(5, 8, HighlightRole.DEFINING),
        CandidateHighlight(32, 8, HighlightRole.DEFINING),
        CandidateHighlight(24, 8, HighlightRole.DEFINING),
        CandidateHighlight(78, 8, HighlightRole.DEFINING),
        CandidateHighlight(7, 8, HighlightRole.ELIMINATION),
        CandidateHighlight(8, 8, HighlightRole.ELIMINATION),
        CandidateHighlight(75, 8, HighlightRole.ELIMINATION),
    ),
),

// Skyscraper
example = BoardExample(
    puzzle = "172...3.99..173....349.287142.79618.7..8219.489153472624.3.9517..9.17...317...69.",
    candidateMasks = intArrayOf(
        0, 0, 0, 40, 184, 144, 0, 48, 0,
        0, 176, 176, 0, 0, 0, 10, 56, 18,
        48, 0, 0, 0, 48, 0, 0, 0, 0,
        0, 0, 20, 0, 0, 0, 0, 0, 20,
        0, 48, 52, 0, 0, 0, 0, 20, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 160, 0, 160, 0, 0, 0, 0,
        48, 176, 0, 34, 0, 0, 10, 12, 134,
        0, 0, 0, 10, 152, 144, 0, 0, 130
    ),
    highlights = listOf(
        CandidateHighlight(18, 6, HighlightRole.DEFINING),
        CandidateHighlight(22, 6, HighlightRole.SECONDARY),
        CandidateHighlight(58, 6, HighlightRole.SECONDARY),
        CandidateHighlight(56, 6, HighlightRole.DEFINING),
        CandidateHighlight(11, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(63, 6, HighlightRole.ELIMINATION),
    ),
),

// 2-String Kite
example = BoardExample(
    puzzle = ".76985.435..34....4.32.1.5..6253..1435149.7...4...253...56.34..63..54..8..48.9365",
    candidateMasks = intArrayOf(
        3, 0, 0, 0, 0, 0, 3, 0, 0,
        0, 3, 384, 0, 0, 96, 419, 450, 323,
        0, 384, 0, 0, 96, 0, 416, 0, 320,
        320, 0, 0, 0, 0, 192, 384, 0, 0,
        0, 0, 0, 0, 0, 160, 0, 130, 34,
        384, 0, 448, 65, 33, 0, 0, 0, 288,
        387, 384, 0, 0, 3, 0, 0, 320, 321,
        0, 0, 320, 65, 0, 0, 259, 258, 0,
        67, 3, 0, 0, 67, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(0, 1, HighlightRole.DEFINING),
        CandidateHighlight(6, 1, HighlightRole.SECONDARY),
        CandidateHighlight(17, 1, HighlightRole.SECONDARY),
        CandidateHighlight(62, 1, HighlightRole.DEFINING),
        CandidateHighlight(54, 1, HighlightRole.ELIMINATION),
    ),
),

// Empty Rectangle
example = BoardExample(
    puzzle = "5...3...21.26.984...6..2.9.2.19.5...6...2...1...4.12.9.6.2..91.41389752692..16..4",
    candidateMasks = intArrayOf(
        0, 392, 264, 65, 0, 136, 97, 96, 0,
        0, 68, 0, 0, 80, 0, 0, 0, 84,
        196, 204, 0, 81, 136, 0, 69, 0, 84,
        0, 204, 0, 0, 224, 0, 108, 228, 196,
        0, 472, 264, 68, 0, 132, 72, 208, 0,
        196, 212, 192, 0, 224, 0, 0, 244, 0,
        192, 0, 208, 0, 24, 12, 0, 0, 196,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 208, 20, 0, 0, 68, 196, 0
    ),
    highlights = listOf(
        CandidateHighlight(6, 1, HighlightRole.DEFINING),
        CandidateHighlight(24, 1, HighlightRole.DEFINING),
        CandidateHighlight(3, 1, HighlightRole.SECONDARY),
        CandidateHighlight(21, 1, HighlightRole.SECONDARY),
        CandidateHighlight(21, 1, HighlightRole.ELIMINATION),
    ),
),

// Turbot Fish
example = BoardExample(
    puzzle = "1.5.7296393.6.5.72726..35..25..64.97.7.1296.569.5.72....2..6751367251..9519748326",
    candidateMasks = intArrayOf(
        0, 136, 0, 136, 0, 0, 0, 0, 0,
        0, 0, 136, 0, 129, 0, 137, 0, 0,
        0, 0, 0, 392, 385, 0, 0, 137, 136,
        0, 0, 133, 132, 0, 0, 129, 0, 0,
        136, 0, 140, 0, 0, 0, 0, 140, 0,
        0, 0, 141, 0, 132, 0, 0, 141, 136,
        136, 136, 0, 260, 260, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 136, 136, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(26, 8, HighlightRole.DEFINING),
        CandidateHighlight(53, 8, HighlightRole.SECONDARY),
        CandidateHighlight(49, 8, HighlightRole.SECONDARY),
        CandidateHighlight(30, 8, HighlightRole.DEFINING),
        CandidateHighlight(21, 8, HighlightRole.ELIMINATION),
    ),
),

// XY-Wing
example = BoardExample(
    puzzle = "6748..9...32.4967.9....6..4...4...9649..6..8..26.984.73..6.4..9.459..36..69..3741",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 23, 19, 0, 23, 22,
        145, 0, 0, 17, 0, 0, 0, 0, 144,
        0, 145, 129, 87, 87, 0, 147, 23, 0,
        81, 145, 197, 0, 23, 83, 19, 0, 0,
        0, 0, 69, 83, 0, 83, 19, 0, 22,
        17, 0, 0, 21, 0, 0, 0, 21, 0,
        0, 129, 193, 0, 67, 0, 146, 18, 0,
        195, 0, 0, 0, 195, 67, 0, 0, 130,
        130, 0, 0, 18, 146, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(71, 2, HighlightRole.SECONDARY),
        CandidateHighlight(71, 8, HighlightRole.SECONDARY),
        CandidateHighlight(61, 2, HighlightRole.DEFINING),
        CandidateHighlight(61, 5, HighlightRole.DEFINING),
        CandidateHighlight(17, 5, HighlightRole.DEFINING),
        CandidateHighlight(17, 8, HighlightRole.DEFINING),
        CandidateHighlight(7, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(25, 5, HighlightRole.ELIMINATION),
    ),
),

// XYZ-Wing
example = BoardExample(
    puzzle = "..38..9..8...69.3.9..3...853..9..8..187.325.9..9..8..3298..735.73.28..916.1.93728",
    candidateMasks = intArrayOf(
        24, 107, 0, 0, 91, 25, 0, 105, 106,
        0, 91, 10, 89, 0, 0, 11, 0, 74,
        0, 107, 42, 0, 75, 9, 43, 0, 0,
        0, 42, 58, 0, 89, 57, 0, 105, 74,
        0, 0, 0, 40, 0, 0, 0, 40, 0,
        24, 42, 0, 105, 89, 0, 43, 105, 0,
        0, 0, 0, 41, 9, 0, 0, 0, 40,
        0, 0, 24, 0, 0, 56, 40, 0, 0,
        0, 24, 0, 24, 0, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(5, 1, HighlightRole.SECONDARY),
        CandidateHighlight(5, 4, HighlightRole.SECONDARY),
        CandidateHighlight(5, 5, HighlightRole.SECONDARY),
        CandidateHighlight(0, 4, HighlightRole.DEFINING),
        CandidateHighlight(0, 5, HighlightRole.DEFINING),
        CandidateHighlight(23, 1, HighlightRole.DEFINING),
        CandidateHighlight(23, 4, HighlightRole.DEFINING),
        CandidateHighlight(4, 4, HighlightRole.ELIMINATION),
    ),
),

// W-Wing
example = BoardExample(
    puzzle = "8975..3..3...79.85.5.83.7.954396.87.679.485.31827539469.5.87.3.71.39..58.38..5697",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 3, 42, 0, 33, 10,
        0, 34, 9, 34, 0, 0, 9, 0, 0,
        10, 0, 33, 0, 0, 41, 0, 34, 0,
        0, 0, 0, 0, 0, 3, 0, 0, 3,
        0, 0, 0, 3, 0, 0, 0, 3, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 34, 0, 42, 0, 0, 3, 0, 9,
        0, 0, 40, 0, 0, 34, 10, 0, 0,
        10, 0, 0, 9, 3, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(4, 1, HighlightRole.DEFINING),
        CandidateHighlight(4, 2, HighlightRole.DEFINING),
        CandidateHighlight(32, 1, HighlightRole.DEFINING),
        CandidateHighlight(32, 2, HighlightRole.DEFINING),
        CandidateHighlight(8, 2, HighlightRole.SECONDARY),
        CandidateHighlight(35, 2, HighlightRole.SECONDARY),
        CandidateHighlight(23, 1, HighlightRole.ELIMINATION),
    ),
),

// Simple Colors (Trap)
example = BoardExample(
    puzzle = "6.4..5712.1.4.....5....1..8.6.178...3..259..6...643...97.5....3.....6.9.8.69..2..",
    candidateMasks = intArrayOf(
        0, 388, 0, 132, 388, 0, 0, 0, 0,
        66, 0, 388, 0, 420, 66, 308, 52, 272,
        0, 262, 326, 68, 294, 0, 300, 44, 0,
        10, 0, 274, 0, 0, 0, 284, 30, 280,
        0, 136, 193, 0, 0, 0, 137, 200, 0,
        67, 402, 467, 0, 0, 0, 401, 210, 337,
        0, 0, 3, 0, 131, 10, 169, 168, 0,
        11, 30, 23, 196, 135, 0, 152, 0, 89,
        0, 28, 0, 0, 5, 72, 0, 88, 89
    ),
    highlights = listOf(
        CandidateHighlight(9, 7, HighlightRole.COLOR_A),
        CandidateHighlight(77, 7, HighlightRole.COLOR_A),
        CandidateHighlight(21, 7, HighlightRole.COLOR_A),
        CandidateHighlight(71, 7, HighlightRole.COLOR_A),
        CandidateHighlight(14, 7, HighlightRole.COLOR_B),
        CandidateHighlight(45, 7, HighlightRole.COLOR_B),
        CandidateHighlight(20, 7, HighlightRole.COLOR_B),
        CandidateHighlight(66, 7, HighlightRole.COLOR_B),
        CandidateHighlight(53, 7, HighlightRole.ELIMINATION),
    ),
),

// Simple Colors (Wrap)
example = BoardExample(
    puzzle = "7129..4854938571628562417392.549.817174.8259..8971524.9.1574.28548.29.71.271.8954",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 36, 36, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 36, 0, 0, 0, 36, 0, 0, 0,
        0, 0, 0, 36, 0, 0, 0, 0, 36,
        36, 0, 0, 0, 0, 0, 0, 0, 36,
        0, 36, 0, 0, 0, 0, 36, 0, 0,
        0, 0, 0, 36, 0, 0, 36, 0, 0,
        36, 0, 0, 0, 36, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(5, 3, HighlightRole.COLOR_A),
        CandidateHighlight(76, 3, HighlightRole.COLOR_A),
        CandidateHighlight(28, 3, HighlightRole.COLOR_A),
        CandidateHighlight(39, 3, HighlightRole.COLOR_A),
        CandidateHighlight(45, 3, HighlightRole.COLOR_A),
        CandidateHighlight(55, 3, HighlightRole.COLOR_A),
        CandidateHighlight(69, 3, HighlightRole.COLOR_A),
        CandidateHighlight(4, 3, HighlightRole.COLOR_B),
        CandidateHighlight(32, 3, HighlightRole.COLOR_B),
        CandidateHighlight(72, 3, HighlightRole.COLOR_B),
        CandidateHighlight(66, 3, HighlightRole.COLOR_B),
        CandidateHighlight(44, 3, HighlightRole.COLOR_B),
        CandidateHighlight(53, 3, HighlightRole.COLOR_B),
        CandidateHighlight(60, 3, HighlightRole.COLOR_B),
    ),
),

