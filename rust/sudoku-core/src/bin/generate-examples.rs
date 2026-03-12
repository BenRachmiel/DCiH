//! Generates pre-baked BoardExample Kotlin source for StrategyContent.kt.
//!
//! Run: cargo run --release --bin generate-examples
//! Output: /tmp/generated-examples.kt
//!
//! Usage:
//!   generate-examples                                         # all techniques, default attempts
//!   generate-examples --progress                              # with progress bars
//!   generate-examples --only TurbotFish,XYZWing --attempts 1000000 --progress

use std::env;
use std::io::{self, Write};
use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use std::sync::Arc;
use std::time::Instant;

use sudoku_core::generator::example::{
    example_from_puzzle, generate_example, generate_example_progress,
};
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

fn all_targets() -> Vec<SolutionType> {
    (0..=30u8)
        .map(SolutionType::from_ordinal)
        .filter(|t| t.has_solver())
        .collect()
}

fn find_target_by_name(name: &str) -> Option<SolutionType> {
    let lower = name.to_lowercase().replace(['-', '_', ' '], "");
    all_targets().into_iter().find(|t| {
        t.display_name()
            .to_lowercase()
            .replace(['-', '_', ' ', '(', ')'], "")
            == lower
    })
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

fn format_example(
    typ: SolutionType,
    puzzle: &str,
    masks: &[u16],
    highlights: &[CandidateHighlight],
) -> String {
    let mut out = String::new();
    out.push_str(&format!("// {}\n", typ.display_name()));
    out.push_str("example = BoardExample(\n");
    out.push_str(&format!("    puzzle = \"{}\",\n", puzzle));
    out.push_str("    candidateMasks = intArrayOf(\n");
    for row in 0..9 {
        let start = row * 9;
        let vals: Vec<String> = (start..start + 9).map(|i| masks[i].to_string()).collect();
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
    out.push_str("),\n");
    out
}

fn print_progress(name: &str, current: u32, total: u32, elapsed: f64) {
    let pct = (current as f64 / total as f64 * 100.0).min(100.0);
    let rate = current as f64 / elapsed;
    let bar_width = 30;
    let filled = ((pct / 100.0 * bar_width as f64) as usize).min(bar_width);
    let bar: String = "\u{2588}".repeat(filled) + &"\u{2591}".repeat(bar_width - filled);
    eprint!(
        "\r  {:<28} [{bar}] {current:>8}/{total} ({pct:5.1}%) {rate:>7.0}/s",
        name
    );
    io::stderr().flush().ok();
}

fn main() {
    let args: Vec<String> = env::args().collect();

    let mut only: Option<Vec<String>> = None;
    let mut max_attempts: Option<i32> = None;
    let mut progress = false;

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--only" => {
                i += 1;
                if i < args.len() {
                    only = Some(args[i].split(',').map(|s| s.to_string()).collect());
                }
            }
            "--attempts" => {
                i += 1;
                if i < args.len() {
                    max_attempts = Some(args[i].parse().expect("--attempts needs a number"));
                }
            }
            "--progress" => {
                progress = true;
            }
            _ => {
                eprintln!("Unknown arg: {}", args[i]);
                eprintln!("Usage: generate-examples [--progress] [--only Type1,Type2] [--attempts N]");
                std::process::exit(1);
            }
        }
        i += 1;
    }

    let targets: Vec<SolutionType> = if let Some(names) = &only {
        names
            .iter()
            .map(|n| find_target_by_name(n).unwrap_or_else(|| panic!("Unknown technique: {n}")))
            .collect()
    } else {
        all_targets()
    };

    let mut found: Vec<(SolutionType, String, Vec<u16>, Vec<CandidateHighlight>)> = Vec::new();
    let mut missing: Vec<SolutionType> = Vec::new();
    let total_techniques = all_targets().len();

    eprintln!("Generating examples for {} techniques...\n", targets.len());

    for &target in &targets {
        // Phase 1: try generation first for fresh examples
        let attempts = max_attempts.unwrap_or(match target.difficulty() {
            Difficulty::Easy => 500,
            Difficulty::Medium => 2000,
            Difficulty::Hard => 5000,
            _ => 10000,
        });

        let start = Instant::now();

        let result = if progress {
            let counter = Arc::new(AtomicU32::new(0));
            let done = Arc::new(AtomicBool::new(false));
            let total = attempts as u32;

            let counter_clone = counter.clone();
            let done_clone = done.clone();
            let name = target.display_name().to_string();
            let progress_start = start;
            let progress_handle = std::thread::spawn(move || {
                while !done_clone.load(Ordering::Relaxed) {
                    let current = counter_clone.load(Ordering::Relaxed);
                    let elapsed = progress_start.elapsed().as_secs_f64();
                    if elapsed > 0.1 {
                        print_progress(&name, current, total, elapsed);
                    }
                    std::thread::sleep(std::time::Duration::from_millis(100));
                }
            });

            let result = generate_example_progress(target, attempts, &counter);

            done.store(true, Ordering::Relaxed);
            progress_handle.join().ok();

            let final_count = counter.load(Ordering::Relaxed);
            let elapsed = start.elapsed().as_secs_f64();
            print_progress(target.display_name(), final_count, total, elapsed);

            result
        } else {
            eprint!("  {:<28} ... ", target.display_name());
            io::stderr().flush().ok();
            generate_example(target, attempts)
        };

        let elapsed = start.elapsed().as_secs_f64();

        if let Some(result) = result {
            if progress {
                eprintln!(" FOUND ({elapsed:.1}s)");
            } else {
                eprintln!("found ({elapsed:.1}s)");
            }
            found.push((target, result.puzzle, result.candidate_masks, result.highlights));
        } else {
            // Phase 2: fall back to curated puzzles
            let curated = CURATED_PUZZLES
                .iter()
                .find_map(|p| example_from_puzzle(p, target));
            if let Some(result) = curated {
                if progress {
                    eprintln!(" found (curated fallback)");
                } else {
                    eprintln!("found (curated fallback)");
                }
                found.push((target, result.puzzle, result.candidate_masks, result.highlights));
            } else {
                if progress {
                    eprintln!(" NOT FOUND ({elapsed:.1}s)");
                } else {
                    eprintln!("NOT FOUND ({elapsed:.1}s)");
                }
                missing.push(target);
            }
        }
    }

    // Output Kotlin source
    let mut out = String::new();
    out.push_str("// \u{2550}\u{2550}\u{2550} Generated Examples \u{2550}\u{2550}\u{2550}\n");
    out.push_str(&format!(
        "// Found {} / {} types\n\n",
        found.len(),
        total_techniques
    ));

    for (typ, puzzle, masks, highlights) in &found {
        out.push_str(&format_example(*typ, puzzle, masks, highlights));
        out.push('\n');
    }

    if !missing.is_empty() {
        out.push_str(&format!(
            "\n// \u{2550}\u{2550}\u{2550} MISSING ({}) \u{2550}\u{2550}\u{2550}\n",
            missing.len()
        ));
        for t in &missing {
            out.push_str(&format!("// {}\n", t.display_name()));
        }
    }

    let output_path = "/tmp/generated-examples.kt";
    std::fs::write(output_path, &out).expect("Failed to write output");
    eprintln!(
        "\nWritten to {} \u{2014} {}/{} types",
        output_path,
        found.len(),
        total_techniques
    );
}
