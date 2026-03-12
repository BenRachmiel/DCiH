// ═══ Generated Examples ═══
// Found 25 / 25 types

// Full House
example = BoardExample(
    puzzle = "184537296536429..87296.853..753..9..413985762692174.533.87..41.941.536.72.7...38.",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 1, 64, 0,
        0, 0, 0, 0, 1, 0, 0, 0, 9,
        128, 0, 0, 0, 32, 34, 0, 8, 9,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 128, 0, 0,
        0, 48, 0, 0, 288, 34, 0, 0, 272,
        0, 0, 0, 130, 0, 0, 0, 2, 0,
        0, 48, 0, 0, 289, 33, 0, 0, 272
    ),
    highlights = listOf(
        CandidateHighlight(51, 8, HighlightRole.DEFINING),
    ),
),

// Naked Single
example = BoardExample(
    puzzle = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79",
    candidateMasks = intArrayOf(
        0, 0, 11, 34, 0, 170, 393, 267, 138,
        0, 74, 74, 0, 0, 0, 204, 14, 202,
        3, 0, 0, 6, 12, 10, 93, 0, 74,
        0, 19, 275, 336, 0, 73, 344, 282, 0,
        0, 18, 306, 0, 16, 0, 336, 274, 0,
        0, 17, 277, 272, 0, 9, 408, 280, 0,
        261, 0, 349, 84, 20, 64, 0, 0, 8,
        6, 194, 70, 0, 0, 0, 36, 4, 0,
        7, 27, 31, 54, 0, 34, 45, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(40, 5, HighlightRole.DEFINING),
    ),
),

// Hidden Single
example = BoardExample(
    puzzle = "1....7.9..3..2...8..96..5....53..9...1..8...26....4...3......1..4......7..7...3..",
    candidateMasks = intArrayOf(
        0, 178, 170, 152, 28, 0, 42, 0, 44,
        88, 0, 40, 281, 0, 273, 105, 104, 0,
        202, 194, 0, 0, 13, 133, 0, 78, 13,
        202, 194, 0, 0, 97, 35, 0, 232, 41,
        328, 0, 12, 336, 0, 304, 104, 124, 0,
        0, 450, 134, 339, 337, 0, 193, 212, 21,
        0, 434, 162, 474, 376, 434, 170, 0, 312,
        402, 0, 163, 403, 309, 439, 162, 178, 0,
        402, 434, 0, 411, 313, 435, 0, 186, 312
    ),
    highlights = listOf(
        CandidateHighlight(65, 1, HighlightRole.DEFINING),
    ),
),

// Locked Candidates (Pointing)
example = BoardExample(
    puzzle = "1...37.9..3..2...8..96..5....53..9...13.8...26....4...3......1..41..3..7..7...3..",
    candidateMasks = intArrayOf(
        0, 178, 170, 152, 0, 0, 42, 0, 40,
        88, 0, 40, 281, 0, 273, 105, 104, 0,
        202, 194, 0, 0, 9, 129, 0, 78, 13,
        202, 194, 0, 0, 97, 35, 0, 232, 41,
        328, 0, 0, 336, 0, 304, 104, 120, 0,
        0, 450, 130, 339, 337, 0, 193, 212, 21,
        0, 434, 162, 474, 376, 434, 170, 0, 312,
        402, 0, 0, 402, 304, 0, 162, 178, 0,
        402, 434, 0, 411, 313, 435, 0, 186, 312
    ),
    highlights = listOf(
        CandidateHighlight(27, 4, HighlightRole.DEFINING),
        CandidateHighlight(36, 4, HighlightRole.DEFINING),
        CandidateHighlight(9, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(18, 4, HighlightRole.ELIMINATION),
    ),
),

// Locked Candidates (Claiming)
example = BoardExample(
    puzzle = "1..53729.53..2...8..96.853...53..9...13.85..26....4.533......1..41.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 160, 168, 0, 0, 0, 0, 0, 40,
        0, 0, 40, 265, 0, 257, 105, 104, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 194, 0, 0, 97, 35, 0, 200, 9,
        328, 0, 0, 320, 0, 0, 104, 104, 0,
        0, 450, 130, 323, 321, 0, 193, 0, 0,
        0, 434, 162, 458, 360, 290, 168, 0, 312,
        386, 0, 0, 386, 0, 0, 160, 162, 0,
        386, 434, 0, 395, 297, 291, 0, 170, 312
    ),
    highlights = listOf(
        CandidateHighlight(69, 6, HighlightRole.DEFINING),
        CandidateHighlight(70, 6, HighlightRole.DEFINING),
        CandidateHighlight(60, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(62, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(79, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(80, 6, HighlightRole.ELIMINATION),
    ),
),

// Locked Pair
example = BoardExample(
    puzzle = "6437.529..9.4...36.12639.4.276853914.5.9746239341..87532.548.6.4893...52.6529.38.",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 129, 0, 0, 0, 129,
        208, 0, 192, 0, 131, 3, 81, 0, 0,
        208, 0, 0, 0, 0, 0, 80, 0, 192,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        129, 0, 129, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 34, 34, 0, 0, 0,
        0, 0, 65, 0, 0, 0, 0, 0, 65,
        0, 0, 0, 0, 33, 97, 65, 0, 0,
        65, 0, 0, 0, 0, 65, 0, 0, 65
    ),
    highlights = listOf(
        CandidateHighlight(62, 1, HighlightRole.DEFINING),
        CandidateHighlight(62, 7, HighlightRole.DEFINING),
        CandidateHighlight(80, 1, HighlightRole.DEFINING),
        CandidateHighlight(80, 7, HighlightRole.DEFINING),
        CandidateHighlight(69, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(69, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(8, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(26, 7, HighlightRole.ELIMINATION),
    ),
),

// Locked Triple
example = BoardExample(
    puzzle = "63....512....358.....6...737.....3...6.....17..3.....8.....62..356.7.1.9482...7..",
    candidateMasks = intArrayOf(
        0, 0, 456, 456, 392, 456, 0, 0, 0,
        3, 67, 65, 67, 0, 0, 0, 40, 40,
        403, 267, 409, 0, 395, 395, 0, 0, 0,
        0, 267, 409, 411, 443, 395, 0, 50, 48,
        402, 0, 408, 414, 410, 398, 264, 0, 0,
        275, 267, 0, 347, 315, 331, 264, 50, 0,
        257, 321, 321, 156, 152, 0, 0, 156, 24,
        0, 0, 0, 138, 0, 138, 0, 136, 0,
        0, 0, 0, 277, 273, 261, 0, 20, 0
    ),
    highlights = listOf(
        CandidateHighlight(9, 1, HighlightRole.DEFINING),
        CandidateHighlight(9, 2, HighlightRole.DEFINING),
        CandidateHighlight(10, 1, HighlightRole.DEFINING),
        CandidateHighlight(10, 2, HighlightRole.DEFINING),
        CandidateHighlight(10, 7, HighlightRole.DEFINING),
        CandidateHighlight(11, 1, HighlightRole.DEFINING),
        CandidateHighlight(11, 7, HighlightRole.DEFINING),
        CandidateHighlight(2, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(18, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(18, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(19, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(19, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(20, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(12, 1, HighlightRole.ELIMINATION),
        CandidateHighlight(12, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(12, 7, HighlightRole.ELIMINATION),
    ),
),

// Naked Pair
example = BoardExample(
    puzzle = "184537296536.2...8..96.853...53..9...13.85..26....4.533......1..41.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 265, 0, 257, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 97, 35, 0, 200, 9,
        328, 0, 0, 320, 0, 0, 104, 104, 0,
        0, 322, 130, 323, 321, 0, 193, 0, 0,
        0, 306, 130, 458, 360, 290, 136, 0, 280,
        386, 0, 0, 386, 0, 0, 160, 162, 0,
        386, 306, 0, 395, 297, 291, 0, 138, 280
    ),
    highlights = listOf(
        CandidateHighlight(19, 2, HighlightRole.DEFINING),
        CandidateHighlight(19, 7, HighlightRole.DEFINING),
        CandidateHighlight(28, 2, HighlightRole.DEFINING),
        CandidateHighlight(28, 7, HighlightRole.DEFINING),
        CandidateHighlight(46, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(46, 7, HighlightRole.ELIMINATION),
        CandidateHighlight(55, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(73, 2, HighlightRole.ELIMINATION),
    ),
),

// Naked Triple
example = BoardExample(
    puzzle = "184537296536.29..8..96.853...53..9...13985..269..74.533..7...1.941.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 9, 0, 0, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 33, 35, 0, 200, 9,
        72, 0, 0, 0, 0, 0, 104, 104, 0,
        0, 0, 130, 3, 0, 0, 129, 0, 0,
        0, 48, 130, 0, 296, 34, 136, 0, 272,
        0, 0, 0, 130, 0, 0, 160, 162, 0,
        130, 48, 0, 138, 297, 35, 0, 138, 272
    ),
    highlights = listOf(
        CandidateHighlight(72, 2, HighlightRole.DEFINING),
        CandidateHighlight(72, 8, HighlightRole.DEFINING),
        CandidateHighlight(75, 2, HighlightRole.DEFINING),
        CandidateHighlight(75, 4, HighlightRole.DEFINING),
        CandidateHighlight(75, 8, HighlightRole.DEFINING),
        CandidateHighlight(79, 2, HighlightRole.DEFINING),
        CandidateHighlight(79, 4, HighlightRole.DEFINING),
        CandidateHighlight(79, 8, HighlightRole.DEFINING),
        CandidateHighlight(76, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(77, 2, HighlightRole.ELIMINATION),
    ),
),

// Naked Quadruple
example = BoardExample(
    puzzle = ".3.....12....35......6...737.....3...6.....1...3.....8.....62..35..7....48.......",
    candidateMasks = intArrayOf(
        432, 0, 472, 456, 392, 456, 440, 0, 0,
        419, 331, 457, 459, 0, 0, 424, 424, 296,
        403, 267, 409, 0, 395, 395, 408, 0, 0,
        0, 267, 409, 411, 443, 395, 0, 314, 312,
        402, 0, 408, 478, 410, 462, 328, 0, 344,
        275, 267, 0, 347, 315, 331, 360, 314, 0,
        257, 321, 321, 156, 152, 0, 0, 156, 24,
        0, 0, 34, 395, 0, 395, 425, 424, 296,
        0, 0, 34, 279, 275, 263, 353, 308, 368
    ),
    highlights = listOf(
        CandidateHighlight(17, 4, HighlightRole.DEFINING),
        CandidateHighlight(17, 6, HighlightRole.DEFINING),
        CandidateHighlight(17, 9, HighlightRole.DEFINING),
        CandidateHighlight(35, 4, HighlightRole.DEFINING),
        CandidateHighlight(35, 5, HighlightRole.DEFINING),
        CandidateHighlight(35, 6, HighlightRole.DEFINING),
        CandidateHighlight(35, 9, HighlightRole.DEFINING),
        CandidateHighlight(62, 4, HighlightRole.DEFINING),
        CandidateHighlight(62, 5, HighlightRole.DEFINING),
        CandidateHighlight(71, 4, HighlightRole.DEFINING),
        CandidateHighlight(71, 6, HighlightRole.DEFINING),
        CandidateHighlight(71, 9, HighlightRole.DEFINING),
        CandidateHighlight(44, 4, HighlightRole.ELIMINATION),
        CandidateHighlight(44, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(44, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(80, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(80, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(80, 9, HighlightRole.ELIMINATION),
    ),
),

// Hidden Pair
example = BoardExample(
    puzzle = "643..52...9.4...36...63..4.2.6.53914.5.9.46239341..87532.548.6.4..3...52..52..3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 192, 449, 0, 0, 384, 449,
        209, 0, 195, 0, 195, 67, 81, 0, 0,
        209, 193, 195, 0, 0, 323, 81, 0, 449,
        0, 192, 0, 192, 0, 0, 0, 0, 0,
        193, 0, 193, 0, 192, 0, 0, 0, 0,
        0, 0, 0, 0, 34, 34, 0, 0, 0,
        0, 0, 321, 0, 0, 0, 0, 0, 321,
        0, 225, 449, 0, 353, 353, 65, 0, 0,
        65, 97, 0, 0, 353, 353, 0, 384, 449
    ),
    highlights = listOf(
        CandidateHighlight(56, 1, HighlightRole.DEFINING),
        CandidateHighlight(56, 7, HighlightRole.DEFINING),
        CandidateHighlight(62, 1, HighlightRole.DEFINING),
        CandidateHighlight(62, 7, HighlightRole.DEFINING),
        CandidateHighlight(56, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(62, 9, HighlightRole.ELIMINATION),
    ),
),

// Hidden Triple
example = BoardExample(
    puzzle = "...273...2.5...3..837...251......7...2..3..4...3......97235..863.4...5......97...",
    candidateMasks = intArrayOf(
        41, 297, 289, 0, 0, 0, 384, 288, 392,
        0, 296, 0, 129, 129, 0, 0, 352, 328,
        0, 0, 0, 296, 40, 296, 0, 0, 0,
        57, 441, 417, 441, 171, 427, 0, 7, 406,
        113, 0, 417, 497, 0, 417, 416, 0, 400,
        121, 441, 0, 505, 171, 427, 416, 3, 402,
        0, 0, 0, 0, 0, 9, 9, 0, 0,
        0, 161, 0, 161, 163, 163, 0, 320, 320,
        49, 177, 161, 169, 0, 0, 9, 6, 6
    ),
    highlights = listOf(
        CandidateHighlight(10, 4, HighlightRole.DEFINING),
        CandidateHighlight(10, 6, HighlightRole.DEFINING),
        CandidateHighlight(16, 6, HighlightRole.DEFINING),
        CandidateHighlight(16, 7, HighlightRole.DEFINING),
        CandidateHighlight(17, 4, HighlightRole.DEFINING),
        CandidateHighlight(17, 7, HighlightRole.DEFINING),
        CandidateHighlight(10, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(16, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(17, 9, HighlightRole.ELIMINATION),
    ),
),

// Hidden Quadruple
example = BoardExample(
    puzzle = ".3.....12....35......6...737.....3...6.....17..3.....8.....62..35..7.1..48....7..",
    candidateMasks = intArrayOf(
        432, 0, 472, 456, 392, 456, 440, 0, 0,
        419, 331, 457, 459, 0, 0, 424, 296, 296,
        403, 267, 409, 0, 395, 395, 408, 0, 0,
        0, 267, 409, 411, 443, 395, 0, 314, 312,
        402, 0, 408, 414, 410, 398, 264, 0, 0,
        275, 267, 0, 347, 315, 331, 296, 314, 0,
        257, 321, 321, 156, 152, 0, 0, 156, 24,
        0, 0, 34, 394, 0, 394, 0, 424, 296,
        0, 0, 34, 279, 275, 263, 0, 308, 0
    ),
    highlights = listOf(
        CandidateHighlight(75, 1, HighlightRole.DEFINING),
        CandidateHighlight(75, 3, HighlightRole.DEFINING),
        CandidateHighlight(75, 5, HighlightRole.DEFINING),
        CandidateHighlight(75, 9, HighlightRole.DEFINING),
        CandidateHighlight(76, 1, HighlightRole.DEFINING),
        CandidateHighlight(76, 5, HighlightRole.DEFINING),
        CandidateHighlight(76, 9, HighlightRole.DEFINING),
        CandidateHighlight(77, 1, HighlightRole.DEFINING),
        CandidateHighlight(77, 3, HighlightRole.DEFINING),
        CandidateHighlight(77, 9, HighlightRole.DEFINING),
        CandidateHighlight(79, 3, HighlightRole.DEFINING),
        CandidateHighlight(79, 5, HighlightRole.DEFINING),
        CandidateHighlight(79, 9, HighlightRole.DEFINING),
        CandidateHighlight(75, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(76, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(77, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(79, 6, HighlightRole.ELIMINATION),
    ),
),

// X-Wing
example = BoardExample(
    puzzle = "184537296536.29..8..96.853...53..9...13985..269..74.533..7...1.941.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 9, 0, 0, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 33, 35, 0, 200, 9,
        72, 0, 0, 0, 0, 0, 104, 104, 0,
        0, 0, 130, 3, 0, 0, 129, 0, 0,
        0, 48, 130, 0, 296, 34, 136, 0, 272,
        0, 0, 0, 130, 0, 0, 160, 162, 0,
        130, 48, 0, 139, 297, 35, 0, 138, 272
    ),
    highlights = listOf(
        CandidateHighlight(12, 1, HighlightRole.DEFINING),
        CandidateHighlight(15, 1, HighlightRole.DEFINING),
        CandidateHighlight(48, 1, HighlightRole.DEFINING),
        CandidateHighlight(51, 1, HighlightRole.DEFINING),
        CandidateHighlight(75, 1, HighlightRole.ELIMINATION),
    ),
),

// Swordfish
example = BoardExample(
    puzzle = ".3.....12....35......6...737.....3...6.....17..3.....8.....62..356.7.1..482...7..",
    candidateMasks = intArrayOf(
        432, 0, 472, 456, 392, 456, 440, 0, 0,
        419, 331, 457, 459, 0, 0, 424, 296, 296,
        403, 267, 409, 0, 395, 395, 408, 0, 0,
        0, 267, 409, 411, 443, 395, 0, 314, 312,
        402, 0, 408, 414, 410, 398, 264, 0, 0,
        275, 267, 0, 347, 315, 331, 296, 314, 0,
        257, 321, 321, 156, 152, 0, 0, 156, 24,
        0, 0, 0, 394, 0, 394, 0, 392, 264,
        0, 0, 0, 277, 273, 261, 0, 276, 0
    ),
    highlights = listOf(
        CandidateHighlight(31, 6, HighlightRole.DEFINING),
        CandidateHighlight(49, 6, HighlightRole.DEFINING),
        CandidateHighlight(16, 6, HighlightRole.DEFINING),
        CandidateHighlight(34, 6, HighlightRole.DEFINING),
        CandidateHighlight(52, 6, HighlightRole.DEFINING),
        CandidateHighlight(17, 6, HighlightRole.DEFINING),
        CandidateHighlight(35, 6, HighlightRole.DEFINING),
        CandidateHighlight(51, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(9, 6, HighlightRole.ELIMINATION),
        CandidateHighlight(15, 6, HighlightRole.ELIMINATION),
    ),
),

// Jellyfish
example = BoardExample(
    puzzle = "639...512....358...4.6...737.....3...6.....17..3.....8.....62..356.7.1.9482...7..",
    candidateMasks = intArrayOf(
        0, 0, 0, 200, 136, 200, 0, 0, 0,
        3, 67, 65, 0, 0, 0, 0, 40, 40,
        144, 0, 144, 0, 3, 3, 0, 0, 0,
        0, 259, 152, 411, 443, 395, 0, 50, 48,
        402, 0, 152, 414, 410, 398, 264, 0, 0,
        275, 259, 0, 347, 315, 331, 264, 50, 0,
        257, 321, 65, 156, 152, 0, 0, 156, 24,
        0, 0, 0, 138, 0, 138, 0, 136, 0,
        0, 0, 0, 277, 273, 261, 0, 20, 0
    ),
    highlights = listOf(
        CandidateHighlight(30, 9, HighlightRole.DEFINING),
        CandidateHighlight(39, 9, HighlightRole.DEFINING),
        CandidateHighlight(48, 9, HighlightRole.DEFINING),
        CandidateHighlight(75, 9, HighlightRole.DEFINING),
        CandidateHighlight(31, 9, HighlightRole.DEFINING),
        CandidateHighlight(40, 9, HighlightRole.DEFINING),
        CandidateHighlight(49, 9, HighlightRole.DEFINING),
        CandidateHighlight(76, 9, HighlightRole.DEFINING),
        CandidateHighlight(32, 9, HighlightRole.DEFINING),
        CandidateHighlight(41, 9, HighlightRole.DEFINING),
        CandidateHighlight(50, 9, HighlightRole.DEFINING),
        CandidateHighlight(77, 9, HighlightRole.DEFINING),
        CandidateHighlight(42, 9, HighlightRole.DEFINING),
        CandidateHighlight(51, 9, HighlightRole.DEFINING),
        CandidateHighlight(28, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(36, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(45, 9, HighlightRole.ELIMINATION),
        CandidateHighlight(46, 9, HighlightRole.ELIMINATION),
    ),
),

// Skyscraper
example = BoardExample(
    puzzle = "184537296536.29..8..96.853...53..9...13985.6269..74.533..7...1.941.536.7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 9, 0, 0, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 33, 35, 0, 200, 9,
        72, 0, 0, 0, 0, 0, 72, 0, 0,
        0, 0, 130, 3, 0, 0, 129, 0, 0,
        0, 48, 130, 0, 296, 34, 136, 0, 272,
        0, 0, 0, 130, 0, 0, 0, 130, 0,
        130, 48, 0, 138, 289, 33, 0, 138, 272
    ),
    highlights = listOf(
        CandidateHighlight(48, 2, HighlightRole.DEFINING),
        CandidateHighlight(47, 2, HighlightRole.SECONDARY),
        CandidateHighlight(56, 2, HighlightRole.SECONDARY),
        CandidateHighlight(59, 2, HighlightRole.DEFINING),
        CandidateHighlight(32, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(66, 2, HighlightRole.ELIMINATION),
        CandidateHighlight(75, 2, HighlightRole.ELIMINATION),
    ),
),

// 2-String Kite
example = BoardExample(
    puzzle = "184537296536.29..8..96.853...53..9...13985..269...4.533......1.941.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 9, 0, 0, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 97, 35, 0, 200, 9,
        72, 0, 0, 0, 0, 0, 104, 104, 0,
        0, 0, 130, 67, 65, 0, 193, 0, 0,
        0, 48, 130, 202, 360, 34, 136, 0, 272,
        0, 0, 0, 130, 0, 0, 160, 162, 0,
        130, 48, 0, 139, 297, 35, 0, 138, 272
    ),
    highlights = listOf(
        CandidateHighlight(22, 1, HighlightRole.DEFINING),
        CandidateHighlight(26, 1, HighlightRole.SECONDARY),
        CandidateHighlight(15, 1, HighlightRole.SECONDARY),
        CandidateHighlight(51, 1, HighlightRole.DEFINING),
        CandidateHighlight(49, 1, HighlightRole.ELIMINATION),
    ),
),

// Empty Rectangle
example = BoardExample(
    puzzle = "1....7.9..3..2...8..96..5....53..9...1..8...26....4...3......1..41.....7..7...3..",
    candidateMasks = intArrayOf(
        0, 178, 170, 152, 28, 0, 42, 0, 44,
        88, 0, 40, 281, 0, 273, 105, 104, 0,
        202, 194, 0, 0, 13, 133, 0, 78, 13,
        202, 194, 0, 0, 97, 35, 0, 232, 41,
        328, 0, 12, 336, 0, 304, 104, 124, 0,
        0, 450, 134, 339, 337, 0, 193, 212, 21,
        0, 434, 162, 474, 376, 434, 170, 0, 312,
        402, 0, 0, 402, 308, 438, 162, 178, 0,
        402, 434, 0, 411, 313, 435, 0, 186, 312
    ),
    highlights = listOf(
        CandidateHighlight(67, 3, HighlightRole.DEFINING),
        CandidateHighlight(68, 3, HighlightRole.DEFINING),
        CandidateHighlight(4, 3, HighlightRole.SECONDARY),
        CandidateHighlight(22, 3, HighlightRole.SECONDARY),
        CandidateHighlight(23, 3, HighlightRole.SECONDARY),
        CandidateHighlight(23, 3, HighlightRole.ELIMINATION),
    ),
),

// Turbot Fish
example = BoardExample(
    puzzle = "184537296536.29..8..96.853...53..9...13985..269...4.533......1.941.53..7..7...3..",
    candidateMasks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 9, 0, 0, 73, 72, 0,
        66, 66, 0, 0, 9, 0, 0, 0, 9,
        202, 66, 0, 0, 97, 35, 0, 200, 9,
        72, 0, 0, 0, 0, 0, 104, 104, 0,
        0, 0, 130, 67, 65, 0, 193, 0, 0,
        0, 48, 130, 202, 360, 34, 136, 0, 272,
        0, 0, 0, 130, 0, 0, 160, 162, 0,
        130, 48, 0, 139, 297, 35, 0, 138, 272
    ),
    highlights = listOf(
        CandidateHighlight(48, 2, HighlightRole.DEFINING),
        CandidateHighlight(47, 2, HighlightRole.SECONDARY),
        CandidateHighlight(56, 2, HighlightRole.SECONDARY),
        CandidateHighlight(72, 2, HighlightRole.DEFINING),
        CandidateHighlight(75, 2, HighlightRole.ELIMINATION),
    ),
),

// XY-Wing
example = BoardExample(
    puzzle = "...58.7..8...27..535764192847.81...2..37.28.428..54..71682354797..4.8..1...17..8.",
    candidateMasks = intArrayOf(
        288, 11, 11, 0, 0, 260, 0, 9, 36,
        0, 264, 297, 260, 0, 0, 37, 45, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 304, 0, 0, 292, 52, 308, 0,
        304, 257, 0, 0, 288, 0, 0, 305, 0,
        0, 0, 289, 260, 0, 0, 37, 293, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 262, 18, 0, 288, 0, 54, 52, 0,
        272, 270, 282, 0, 0, 288, 54, 0, 36
    ),
    highlights = listOf(
        CandidateHighlight(5, 3, HighlightRole.SECONDARY),
        CandidateHighlight(5, 9, HighlightRole.SECONDARY),
        CandidateHighlight(8, 3, HighlightRole.DEFINING),
        CandidateHighlight(8, 6, HighlightRole.DEFINING),
        CandidateHighlight(77, 6, HighlightRole.DEFINING),
        CandidateHighlight(77, 9, HighlightRole.DEFINING),
        CandidateHighlight(80, 6, HighlightRole.ELIMINATION),
    ),
),

// XYZ-Wing
example = BoardExample(
    puzzle = "6.9.12..7.5.69.12.2.1.8.96...4263..1..2..16..16.87.2...18.2.3.6.26138.5.3....6812",
    candidateMasks = intArrayOf(
        0, 140, 0, 28, 0, 0, 24, 132, 0,
        200, 0, 68, 0, 0, 72, 0, 0, 132,
        0, 76, 0, 92, 0, 88, 0, 0, 24,
        464, 448, 0, 0, 0, 0, 80, 448, 0,
        448, 324, 0, 280, 24, 0, 0, 460, 392,
        0, 0, 20, 0, 0, 264, 0, 264, 20,
        344, 0, 0, 344, 0, 344, 0, 328, 0,
        328, 0, 0, 0, 0, 0, 72, 0, 264,
        0, 328, 80, 344, 24, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(23, 4, HighlightRole.SECONDARY),
        CandidateHighlight(23, 5, HighlightRole.SECONDARY),
        CandidateHighlight(23, 7, HighlightRole.SECONDARY),
        CandidateHighlight(14, 4, HighlightRole.DEFINING),
        CandidateHighlight(14, 7, HighlightRole.DEFINING),
        CandidateHighlight(26, 4, HighlightRole.DEFINING),
        CandidateHighlight(26, 5, HighlightRole.DEFINING),
        CandidateHighlight(21, 4, HighlightRole.ELIMINATION),
    ),
),

// W-Wing
example = BoardExample(
    puzzle = "6.2.381.48..9.46...4.....8..1.85.2434283.1..63...4281..8....46....4.7..82.4.8.9.1",
    candidateMasks = intArrayOf(
        0, 336, 0, 80, 0, 0, 0, 336, 0,
        0, 84, 85, 0, 67, 0, 0, 86, 82,
        337, 0, 341, 3, 99, 48, 84, 0, 338,
        320, 0, 352, 0, 0, 288, 0, 0, 0,
        0, 0, 0, 0, 320, 0, 80, 336, 0,
        0, 48, 304, 96, 0, 0, 0, 0, 320,
        337, 0, 84, 3, 259, 276, 0, 0, 82,
        273, 308, 309, 0, 291, 0, 20, 22, 0,
        0, 116, 0, 48, 0, 20, 0, 84, 0
    ),
    highlights = listOf(
        CandidateHighlight(23, 5, HighlightRole.DEFINING),
        CandidateHighlight(23, 6, HighlightRole.DEFINING),
        CandidateHighlight(75, 5, HighlightRole.DEFINING),
        CandidateHighlight(75, 6, HighlightRole.DEFINING),
        CandidateHighlight(22, 6, HighlightRole.SECONDARY),
        CandidateHighlight(67, 6, HighlightRole.SECONDARY),
        CandidateHighlight(3, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(59, 5, HighlightRole.ELIMINATION),
        CandidateHighlight(77, 5, HighlightRole.ELIMINATION),
    ),
),

// Simple Colors (Trap)
example = BoardExample(
    puzzle = "...12.568.51846.3286253.41.58..612...26...851.1.285..66.8.12.7519..5862.2.56..18.",
    candidateMasks = intArrayOf(
        332, 76, 332, 0, 0, 320, 0, 0, 0,
        320, 0, 0, 0, 0, 0, 320, 0, 0,
        0, 0, 0, 0, 0, 320, 0, 0, 320,
        0, 0, 332, 332, 0, 0, 0, 264, 68,
        332, 0, 0, 332, 320, 12, 0, 0, 0,
        332, 0, 332, 0, 0, 0, 68, 264, 0,
        0, 12, 0, 268, 0, 0, 260, 0, 0,
        0, 0, 76, 68, 0, 0, 0, 0, 12,
        0, 68, 0, 0, 320, 12, 0, 0, 268
    ),
    highlights = listOf(
        CandidateHighlight(5, 9, HighlightRole.COLOR_A),
        CandidateHighlight(26, 9, HighlightRole.COLOR_A),
        CandidateHighlight(76, 9, HighlightRole.COLOR_A),
        CandidateHighlight(60, 9, HighlightRole.COLOR_A),
        CandidateHighlight(9, 9, HighlightRole.COLOR_A),
        CandidateHighlight(23, 9, HighlightRole.COLOR_B),
        CandidateHighlight(80, 9, HighlightRole.COLOR_B),
        CandidateHighlight(15, 9, HighlightRole.COLOR_B),
        CandidateHighlight(40, 9, HighlightRole.COLOR_B),
        CandidateHighlight(57, 9, HighlightRole.COLOR_B),
        CandidateHighlight(36, 9, HighlightRole.ELIMINATION),
    ),
),

// Simple Colors (Wrap)
example = BoardExample(
    puzzle = ".836257.464..7823525743.8.6762.8345.5.476.328.3825467.4765921838253169473..847562",
    candidateMasks = intArrayOf(
        257, 0, 0, 0, 0, 0, 0, 257, 0,
        0, 0, 257, 257, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 257, 0, 257, 0,
        0, 0, 0, 257, 0, 0, 0, 0, 257,
        0, 257, 0, 0, 0, 257, 0, 0, 0,
        257, 0, 0, 0, 0, 0, 0, 0, 257,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 257, 257, 0, 0, 0, 0, 0, 0
    ),
    highlights = listOf(
        CandidateHighlight(0, 1, HighlightRole.COLOR_A),
        CandidateHighlight(25, 1, HighlightRole.COLOR_A),
        CandidateHighlight(53, 1, HighlightRole.COLOR_A),
        CandidateHighlight(37, 1, HighlightRole.COLOR_A),
        CandidateHighlight(12, 1, HighlightRole.COLOR_A),
        CandidateHighlight(74, 1, HighlightRole.COLOR_A),
        CandidateHighlight(7, 1, HighlightRole.COLOR_B),
        CandidateHighlight(45, 1, HighlightRole.COLOR_B),
        CandidateHighlight(11, 1, HighlightRole.COLOR_B),
        CandidateHighlight(23, 1, HighlightRole.COLOR_B),
        CandidateHighlight(35, 1, HighlightRole.COLOR_B),
        CandidateHighlight(41, 1, HighlightRole.COLOR_B),
        CandidateHighlight(73, 1, HighlightRole.COLOR_B),
        CandidateHighlight(30, 1, HighlightRole.COLOR_B),
    ),
),

