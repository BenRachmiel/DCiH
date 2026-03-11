//! Generates pre-baked BoardExample Kotlin source for StrategyContent.kt.
//!
//! Run: cargo run --bin generate-examples
//! Output: /tmp/generated-examples.kt

use sudoku_core::board::Board;
use sudoku_core::generator::example::generate_example;
use sudoku_core::generator::highlighter::build_highlights;
use sudoku_core::solver::SolverOrchestrator;
use sudoku_core::step::CandidateHighlight;
use sudoku_core::types::{Difficulty, HighlightRole, SolutionType};

const CURATED_PUZZLES: &[&str] = &[
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
];

/// Try to find an example for `target_type` by solving curated puzzles.
fn find_from_curated(target_type: SolutionType) -> Option<(String, Vec<u16>, Vec<CandidateHighlight>)> {
    let orchestrator = SolverOrchestrator::new();
    let threshold = completeness_threshold(target_type);

    for &puzzle_str in CURATED_PUZZLES {
        let mut board = Board::new();
        board.load_from_string(puzzle_str);

        let result = orchestrator.solve(&board, None);
        if !result.solved {
            continue;
        }

        // Replay to capture state at the right moment
        let mut replay = Board::new();
        replay.load_from_string(puzzle_str);

        for step in &result.steps {
            if step.step_type == target_type {
                let filled = 81 - replay.unsolved as usize;
                if filled <= threshold {
                    let highlights = build_highlights(&replay, step);
                    return Some((
                        replay.to_string_compact(),
                        replay.candidates.to_vec(),
                        highlights,
                    ));
                }
            }

            if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
                replay.set_cell(step.cell_index as usize, step.value, false);
                replay.set_all_exposed_singles();
            } else {
                for &(cell, cand) in &step.candidates_removed {
                    replay.set_candidate(cell, cand, false);
                }
                replay.set_all_exposed_singles();
            }
        }
    }

    None
}

fn completeness_threshold(t: SolutionType) -> usize {
    if t == SolutionType::FullHouse {
        return 81;
    }
    match t.difficulty() {
        Difficulty::Easy => 60,
        Difficulty::Medium => 65,
        Difficulty::Hard => 70,
        _ => 75,
    }
}

fn role_name(role: HighlightRole) -> &'static str {
    match role {
        HighlightRole::Defining => "HighlightRole.DEFINING",
        HighlightRole::Elimination => "HighlightRole.ELIMINATION",
        HighlightRole::Secondary => "HighlightRole.SECONDARY",
        HighlightRole::Tertiary => "HighlightRole.TERTIARY",
        HighlightRole::ColorA => "HighlightRole.COLOR_A",
        HighlightRole::ColorB => "HighlightRole.COLOR_B",
    }
}

fn main() {
    let mut found: Vec<(SolutionType, String, Vec<u16>, Vec<CandidateHighlight>)> = Vec::new();
    let mut missing: Vec<SolutionType> = Vec::new();

    // Collect all types with solvers
    let targets: Vec<SolutionType> = (0..=30u8)
        .map(SolutionType::from_ordinal)
        .filter(|t| t.has_solver())
        .collect();

    eprintln!("Generating examples for {} techniques...", targets.len());

    for &target in &targets {
        eprint!("  {} ... ", target.display_name());

        // Phase 1: curated puzzles
        if let Some((puzzle, masks, highlights)) = find_from_curated(target) {
            eprintln!("found (curated)");
            found.push((target, puzzle, masks, highlights));
            continue;
        }

        // Phase 2: random generation — scale attempts by difficulty
        let max_attempts = match target.difficulty() {
            Difficulty::Easy => 500,
            Difficulty::Medium => 2000,
            Difficulty::Hard => 5000,
            _ => 10000, // Unfair, Extreme
        };
        if let Some(result) = generate_example(target, max_attempts) {
            eprintln!("found (random)");
            found.push((target, result.puzzle, result.candidate_masks, result.highlights));
            continue;
        }

        eprintln!("NOT FOUND");
        missing.push(target);
    }

    // Output Kotlin source
    let mut out = String::new();
    out.push_str("// ═══ Generated Examples ═══\n");
    out.push_str(&format!(
        "// Found {} / {} types\n\n",
        found.len(),
        targets.len()
    ));

    for (typ, puzzle, masks, highlights) in &found {
        out.push_str(&format!("// {}\n", typ.display_name()));
        out.push_str("example = BoardExample(\n");
        out.push_str(&format!("    puzzle = \"{}\",\n", puzzle));
        out.push_str("    candidateMasks = intArrayOf(\n");
        for row in 0..9 {
            let start = row * 9;
            let vals: Vec<String> = (start..start + 9)
                .map(|i| masks[i].to_string())
                .collect();
            let comma = if row < 8 { "," } else { "" };
            out.push_str(&format!("        {}{}\n", vals.join(", "), comma));
        }
        out.push_str("    ),\n");
        out.push_str("    highlights = listOf(\n");
        for h in highlights {
            out.push_str(&format!(
                "        CandidateHighlight({}, {}, {}),\n",
                h.cell_index,
                h.value,
                role_name(h.role),
            ));
        }
        out.push_str("    ),\n");
        out.push_str("),\n\n");
    }

    if !missing.is_empty() {
        out.push_str(&format!("\n// ═══ MISSING ({}) ═══\n", missing.len()));
        for t in &missing {
            out.push_str(&format!("// {}\n", t.display_name()));
        }
    }

    let output_path = "/tmp/generated-examples.kt";
    std::fs::write(output_path, &out).expect("Failed to write output");
    eprintln!(
        "\nWritten to {} — {}/{} types",
        output_path,
        found.len(),
        targets.len()
    );
}
