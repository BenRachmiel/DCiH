//! Benchmark solver against the 3M Kaggle sudoku dataset.
//!
//! Run: cargo run --release --bin benchmark -- <csv-path> [--limit N] [--sample N]
//!
//! Options:
//!   --limit N    Stop after N puzzles (default: all)
//!   --sample N   Solve every Nth puzzle (default: 1 = all)
//!
//! CSV format: id,puzzle,solution,clues,difficulty

use std::collections::HashMap;
use std::env;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::time::Instant;

use sudoku_core::board::Board;
use sudoku_core::solver::SolverOrchestrator;
use sudoku_core::types::{Difficulty, SolutionType};

struct PuzzleRecord {
    puzzle: String,
    solution: String,
    clues: u8,
    difficulty: f64,
}

fn parse_line(line: &str) -> Option<PuzzleRecord> {
    let mut fields = line.splitn(5, ',');
    let _id = fields.next()?;
    let puzzle = fields.next()?;
    let solution = fields.next()?;
    let clues: u8 = fields.next()?.parse().ok()?;
    let difficulty: f64 = fields.next()?.parse().ok()?;
    if puzzle.len() != 81 || solution.len() != 81 {
        return None;
    }
    Some(PuzzleRecord {
        puzzle: puzzle.to_string(),
        solution: solution.to_string(),
        clues,
        difficulty,
    })
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let csv_path = args.get(1).unwrap_or_else(|| {
        eprintln!("Usage: benchmark <csv-path> [--limit N] [--sample N]");
        std::process::exit(1);
    });

    let mut limit: usize = usize::MAX;
    let mut sample: usize = 1;

    let mut i = 2;
    while i < args.len() {
        match args[i].as_str() {
            "--limit" => {
                i += 1;
                limit = args[i].parse().expect("--limit requires a number");
            }
            "--sample" => {
                i += 1;
                sample = args[i].parse().expect("--sample requires a number");
            }
            other => {
                eprintln!("Unknown argument: {other}");
                std::process::exit(1);
            }
        }
        i += 1;
    }

    let file = File::open(csv_path).unwrap_or_else(|e| {
        eprintln!("Cannot open {csv_path}: {e}");
        std::process::exit(1);
    });
    let reader = BufReader::new(file);
    let solver = SolverOrchestrator::new();

    let mut total = 0usize;
    let mut solved = 0usize;
    let mut correct = 0usize;
    let mut mismatches: Vec<(usize, String)> = Vec::new();
    let mut unsolved: Vec<(usize, String)> = Vec::new();
    let mut technique_counts: HashMap<SolutionType, usize> = HashMap::new();
    let mut difficulty_pairs: Vec<(f64, i32)> = Vec::new(); // (kaggle_rating, our_score)
    let mut tier_counts: HashMap<Difficulty, usize> = HashMap::new();

    let start = Instant::now();
    let mut line_num = 0usize;

    for line_result in reader.lines() {
        let line = line_result.expect("Failed to read line");
        line_num += 1;

        if line_num == 1 {
            continue; // skip header
        }

        if (line_num - 1) % sample != 0 {
            continue;
        }

        let record = match parse_line(&line) {
            Some(r) => r,
            None => {
                eprintln!("Skipping malformed line {line_num}");
                continue;
            }
        };

        let mut board = Board::new();
        board.load_from_string(&record.puzzle);
        board.solution_set = false;

        let result = solver.solve(&board, None);

        total += 1;

        if result.solved {
            solved += 1;

            // Compare solution
            let our_solution = result.steps.iter().fold(
                {
                    let mut b = Board::new();
                    b.load_from_string(&record.puzzle);
                    b
                },
                |mut b, step| {
                    if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
                        b.set_cell(step.cell_index as usize, step.value, false);
                        b.set_all_exposed_singles();
                    } else {
                        for &(cell_index, candidate) in &step.candidates_removed {
                            b.set_candidate(cell_index, candidate, false);
                        }
                        b.set_all_exposed_singles();
                    }
                    b
                },
            );

            let our_str = our_solution.to_string_compact();
            if our_str == record.solution {
                correct += 1;
            } else {
                mismatches.push((line_num, record.puzzle.clone()));
                if mismatches.len() <= 10 {
                    eprintln!(
                        "MISMATCH line {line_num}: puzzle={}\n  expected: {}\n  got:      {}",
                        record.puzzle, record.solution, our_str
                    );
                }
            }
        } else {
            unsolved.push((line_num, record.puzzle.clone()));
            if unsolved.len() <= 10 {
                eprintln!("UNSOLVED line {line_num}: puzzle={}", record.puzzle);
            }
        }

        for step in &result.steps {
            *technique_counts.entry(step.step_type).or_insert(0) += 1;
        }
        *tier_counts.entry(result.difficulty).or_insert(0) += 1;
        difficulty_pairs.push((record.difficulty, result.score));

        if total % 10000 == 0 {
            let elapsed = start.elapsed().as_secs_f64();
            eprintln!(
                "  ... {total} puzzles in {elapsed:.1}s ({:.0}/s) — {solved} solved, {} mismatches",
                total as f64 / elapsed,
                mismatches.len()
            );
        }

        if total >= limit {
            break;
        }
    }

    let elapsed = start.elapsed().as_secs_f64();

    println!("\n=== Benchmark Results ===");
    println!("Puzzles tested:  {total}");
    println!("Solved:          {solved} ({:.2}%)", solved as f64 / total as f64 * 100.0);
    println!("Correct:         {correct} ({:.2}%)", correct as f64 / total as f64 * 100.0);
    println!("Mismatches:      {}", mismatches.len());
    println!("Unsolved:        {}", unsolved.len());
    println!("Time:            {elapsed:.2}s ({:.0} puzzles/s)", total as f64 / elapsed);

    println!("\n--- Technique Usage ---");
    let mut techniques: Vec<_> = technique_counts.into_iter().collect();
    techniques.sort_by(|a, b| b.1.cmp(&a.1));
    for (tech, count) in &techniques {
        println!("  {tech:?}: {count}");
    }

    println!("\n--- Our Difficulty Tiers ---");
    let mut tiers: Vec<_> = tier_counts.into_iter().collect();
    tiers.sort_by_key(|(d, _)| *d);
    for (tier, count) in &tiers {
        println!("  {tier:?}: {count} ({:.1}%)", *count as f64 / total as f64 * 100.0);
    }

    // Difficulty correlation: bucket kaggle ratings and show average of our scores
    println!("\n--- Kaggle Rating vs Our Score (avg) ---");
    let mut buckets: HashMap<u32, (f64, usize)> = HashMap::new(); // key = kaggle_rating * 10
    for &(kaggle, ours) in &difficulty_pairs {
        let key = (kaggle * 2.0).round() as u32; // bucket by 0.5 increments
        let entry = buckets.entry(key).or_insert((0.0, 0));
        entry.0 += ours as f64;
        entry.1 += 1;
    }
    let mut bucket_keys: Vec<u32> = buckets.keys().copied().collect();
    bucket_keys.sort();
    for key in bucket_keys {
        let (sum, count) = buckets[&key];
        let kaggle_rating = key as f64 / 2.0;
        println!(
            "  Kaggle {kaggle_rating:.1}: avg_score={:.0}, count={count}",
            sum / count as f64
        );
    }
}
