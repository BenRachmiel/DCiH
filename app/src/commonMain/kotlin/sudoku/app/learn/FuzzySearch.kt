package sudoku.app.learn

import sudoku.core.model.StrategyEntry

/**
 * Fuzzy substring matching for strategy search.
 * Matches query characters in order within target (case-insensitive).
 * Score bonuses for consecutive matches, word-start matches, and string-start matches.
 */
fun fuzzyMatch(query: String, target: String): Int {
    if (query.isEmpty()) return 0
    val q = query.lowercase()
    val t = target.lowercase()

    var qi = 0
    var score = 0
    var consecutive = 0
    var lastMatchIdx = -2

    for (ti in t.indices) {
        if (qi < q.length && t[ti] == q[qi]) {
            score += 1
            // Consecutive bonus
            if (ti == lastMatchIdx + 1) {
                consecutive++
                score += consecutive * 2
            } else {
                consecutive = 0
            }
            // Word-start bonus
            if (ti == 0 || t[ti - 1] == ' ' || t[ti - 1] == '-' || t[ti - 1] == '(') {
                score += 5
            }
            // String-start bonus
            if (ti == 0) {
                score += 10
            }
            lastMatchIdx = ti
            qi++
        }
    }

    return if (qi == q.length) score else 0
}

/** Result of a fuzzy search, pairing a strategy entry with its relevance score. */
data class SearchResult(
    val entry: StrategyEntry,
    val score: Int,
)

/**
 * Search strategy entries by fuzzy matching across multiple fields.
 * Returns entries sorted by relevance (highest first).
 */
fun searchStrategies(query: String, entries: List<StrategyEntry>): List<SearchResult> {
    if (query.isBlank()) return emptyList()
    val trimmed = query.trim()

    return entries.mapNotNull { entry ->
        val nameScore = fuzzyMatch(trimmed, entry.type.displayName) * 4
        val categoryScore = fuzzyMatch(trimmed, entry.type.category.displayName) * 2
        val keywordScore = entry.keywords.maxOfOrNull { fuzzyMatch(trimmed, it) * 2 } ?: 0
        val theoryScore = fuzzyMatch(trimmed, entry.theory)
        val spotScore = fuzzyMatch(trimmed, entry.howToSpot)

        val total = nameScore + categoryScore + keywordScore + theoryScore + spotScore
        if (total > 0) SearchResult(entry, total) else null
    }.sortedByDescending { it.score }
}
