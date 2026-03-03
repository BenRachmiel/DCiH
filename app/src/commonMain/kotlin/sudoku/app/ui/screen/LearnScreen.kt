package sudoku.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sudoku.app.learn.allEntries
import sudoku.app.learn.searchStrategies
import sudoku.app.learn.strategyEntries
import sudoku.app.ui.component.ExampleBoard
import sudoku.core.model.HighlightRole
import sudoku.core.model.SolutionType
import sudoku.core.model.StrategyCategory
import sudoku.core.model.StrategyEntry

@Composable
fun LearnScreen(
    technique: SolutionType?,
    onBack: () -> Unit,
    onNavigateTechnique: (SolutionType) -> Unit,
) {
    if (technique != null) {
        val entry = strategyEntries[technique]
        if (entry != null) {
            DetailView(entry, onBack, onNavigateTechnique)
        } else {
            // Fallback for techniques without entries (BRUTE_FORCE)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${technique.displayName} — no entry available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    } else {
        ListView(onBack, onNavigateTechnique)
    }
}

// ── List View ────────────────────────────────────────────────────────────────

@Composable
private fun ListView(
    onBack: () -> Unit,
    onNavigateTechnique: (SolutionType) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    placeholder = { Text("Search techniques...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                )
            }

            if (searchQuery.isBlank()) {
                // Grouped by category
                CategorizedList(onNavigateTechnique)
            } else {
                // Fuzzy search results
                SearchResultsList(searchQuery, onNavigateTechnique)
            }
        }
    }
}

@Composable
private fun CategorizedList(onNavigateTechnique: (SolutionType) -> Unit) {
    val grouped = remember {
        allEntries.groupBy { it.type.category }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        for (category in StrategyCategory.entries) {
            val entries = grouped[category] ?: continue
            if (category == StrategyCategory.BRUTE_FORCE) continue

            item(key = "header_${category.name}") {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }

            items(entries, key = { it.type.name }) { entry ->
                StrategyListItem(entry, onNavigateTechnique)
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    query: String,
    onNavigateTechnique: (SolutionType) -> Unit,
) {
    val results = remember(query) { searchStrategies(query, allEntries) }

    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No techniques found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(results, key = { it.entry.type.name }) { result ->
                StrategyListItem(result.entry, onNavigateTechnique)
            }
        }
    }
}

@Composable
private fun StrategyListItem(
    entry: StrategyEntry,
    onNavigateTechnique: (SolutionType) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateTechnique(entry.type) }
            .padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.type.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = entry.type.score.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Detail View ──────────────────────────────────────────────────────────────

private val legendItems = listOf(
    HighlightRole.DEFINING to Pair(Color(0xFF4CAF50), "Pattern"),
    HighlightRole.ELIMINATION to Pair(Color(0xFFEF5350), "Eliminated"),
    HighlightRole.SECONDARY to Pair(Color(0xFF42A5F5), "Structure"),
    HighlightRole.COLOR_A to Pair(Color(0xFF66BB6A), "Color A"),
    HighlightRole.COLOR_B to Pair(Color(0xFFAB47BC), "Color B"),
)

@Composable
private fun DetailView(
    entry: StrategyEntry,
    onBack: () -> Unit,
    onNavigateTechnique: (SolutionType) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Back + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = entry.type.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Category chip + score
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(entry.type.category.displayName) },
                )
                Text(
                    text = "Score: ${entry.type.score}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Example board
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ExampleBoard(
                    example = entry.example,
                    modifier = Modifier.widthIn(max = 400.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Color legend
            val usedRoles = remember(entry.example.highlights) {
                entry.example.highlights.map { it.role }.toSet()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((role, colorAndLabel) in legendItems) {
                    if (role !in usedRoles) continue
                    val (color, label) = colorAndLabel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = color,
                        ) {}
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Theory section
            Text(
                text = "Why it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.theory,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // How to spot section
            Text(
                text = "How to spot it",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.howToSpot,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Related techniques
            if (entry.relatedTypes.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Related techniques",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (related in entry.relatedTypes) {
                        AssistChip(
                            onClick = { onNavigateTechnique(related) },
                            label = { Text(related.displayName) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
