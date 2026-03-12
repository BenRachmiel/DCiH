use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use std::sync::Mutex;

use rand::Rng;
use serde::Serialize;

use crate::board::Board;
use crate::generator::highlighter::build_highlights;
use crate::generator::{GeneratedPuzzle, Generator};
use crate::solver::{SolverOrchestrator, StepFinder};
use crate::step::{CandidateHighlight, SolutionStep};
use crate::types::{Difficulty, StrategyCategory, SolutionType};

#[derive(Debug, Clone, Serialize)]
pub struct BoardExampleResult {
    pub puzzle: String,
    #[serde(rename = "candidateMasks")]
    pub candidate_masks: Vec<u16>,
    pub highlights: Vec<CandidateHighlight>,
}

/// Result of technique-targeted puzzle generation.
#[derive(Debug, Clone)]
pub struct TechniquePuzzle {
    /// The generated puzzle (clues only).
    pub puzzle: GeneratedPuzzle,
    /// Index of the target technique's step in the solve path.
    pub step_index: usize,
    /// All solve steps.
    pub steps: Vec<SolutionStep>,
}

/// Generate a puzzle that requires a specific solving technique.
///
/// For Hard+ techniques, tries targeted generation first (detecting the technique
/// during clue removal). Falls back to random generate-and-check for Easy/Medium
/// or if targeted generation fails.
pub fn generate_with_technique(
    target_type: SolutionType,
    max_attempts: i32,
) -> Option<TechniquePuzzle> {
    generate_with_technique_inner(target_type, max_attempts, None)
}

/// Like `generate_with_technique`, but increments `counter` on each attempt.
pub fn generate_with_technique_progress(
    target_type: SolutionType,
    max_attempts: i32,
    counter: &AtomicU32,
) -> Option<TechniquePuzzle> {
    generate_with_technique_inner(target_type, max_attempts, Some(counter))
}

fn generate_with_technique_inner(
    target_type: SolutionType,
    max_attempts: i32,
    counter: Option<&AtomicU32>,
) -> Option<TechniquePuzzle> {
    let n_threads = std::thread::available_parallelism()
        .map(|n| n.get())
        .unwrap_or(4);
    let found = AtomicBool::new(false);
    let result: Mutex<Option<TechniquePuzzle>> = Mutex::new(None);
    let attempts_per_thread = max_attempts / n_threads as i32;
    let target_diff = target_type.difficulty();
    let check_threshold = check_clue_threshold(target_type);

    let worker = |found: &AtomicBool, counter: Option<&AtomicU32>| -> Option<TechniquePuzzle> {
        let mut generator = Generator::new();
        let step_finder = StepFinder::new();
        let orchestrator = SolverOrchestrator::new();
        let mut rng = rand::rng();

        for _ in 0..attempts_per_thread {
            if found.load(Ordering::Relaxed) {
                return None;
            }
            if let Some(c) = counter {
                c.fetch_add(1, Ordering::Relaxed);
            }

            // Try targeted for Hard+
            if target_diff >= Difficulty::Hard {
                if let Some(tp) = generate_targeted_single(
                    target_type,
                    &mut generator,
                    &step_finder,
                    &orchestrator,
                    check_threshold,
                    &mut rng,
                ) {
                    return Some(tp);
                }
                continue; // targeted attempt consumed this iteration
            }

            // Random generate-and-check
            let puzzle = generator.generate(target_diff, 50);
            let mut board = Board::new();
            board.load_from_string(&puzzle.puzzle);
            let solve = orchestrator.solve(&board, None);
            if !solve.solved {
                continue;
            }
            if let Some(idx) = solve.steps.iter().position(|s| s.step_type == target_type) {
                return Some(TechniquePuzzle {
                    puzzle,
                    step_index: idx,
                    steps: solve.steps,
                });
            }
        }
        None
    };

    std::thread::scope(|s| {
        for _ in 0..n_threads {
            s.spawn(|| {
                if let Some(tp) = worker(&found, counter) {
                    found.store(true, Ordering::Relaxed);
                    let mut lock = result.lock().unwrap();
                    if lock.is_none() {
                        *lock = Some(tp);
                    }
                }
            });
        }
    });

    result.into_inner().unwrap()
}

/// Maximum remaining clues at which to start checking for the technique.
/// The technique needs enough empty cells (with candidates) to manifest.
fn check_clue_threshold(target: SolutionType) -> usize {
    match target.category() {
        StrategyCategory::Singles | StrategyCategory::LockedCandidates => 45,
        StrategyCategory::Subsets => 40,
        StrategyCategory::Fish => 35,
        StrategyCategory::SingleDigit | StrategyCategory::Wings => 33,
        StrategyCategory::Coloring | StrategyCategory::Chains => 30,
        StrategyCategory::BruteForce => 25,
    }
}

fn board_from_values(values: &[u8; 81]) -> Board {
    let mut board = Board::new();
    for i in 0..81 {
        if values[i] != 0 {
            board.set_cell(i, values[i], true);
        }
    }
    board.set_all_exposed_singles();
    board
}

fn puzzle_to_string(values: &[u8; 81]) -> String {
    values.iter().map(|&v| (b'0' + v) as char).collect()
}

/// Generate a board example showcasing a specific solving technique.
///
/// Wraps `generate_with_technique` and captures the board state (candidates,
/// highlights) at the point where the target technique first appears.
pub fn generate_example(
    target_type: SolutionType,
    max_attempts: i32,
) -> Option<BoardExampleResult> {
    generate_example_inner(target_type, max_attempts, None)
}

/// Like `generate_example`, but increments `counter` on each generation attempt.
pub fn generate_example_progress(
    target_type: SolutionType,
    max_attempts: i32,
    counter: &AtomicU32,
) -> Option<BoardExampleResult> {
    generate_example_inner(target_type, max_attempts, Some(counter))
}

fn generate_example_inner(
    target_type: SolutionType,
    max_attempts: i32,
    counter: Option<&AtomicU32>,
) -> Option<BoardExampleResult> {
    let n_threads = std::thread::available_parallelism()
        .map(|n| n.get())
        .unwrap_or(4);
    let found = AtomicBool::new(false);
    let result: Mutex<Option<BoardExampleResult>> = Mutex::new(None);
    let attempts_per_thread = max_attempts / n_threads as i32;
    let threshold = completeness_threshold(target_type);

    // Closure that checks a TechniquePuzzle for example quality
    let try_as_example = |tp: &TechniquePuzzle| -> Option<BoardExampleResult> {
        let mut replay = Board::new();
        replay.load_from_string(&tp.puzzle.puzzle);
        for (i, step) in tp.steps.iter().enumerate() {
            if i == tp.step_index {
                let filled = 81 - replay.unsolved as usize;
                if filled <= threshold {
                    let highlights = build_highlights(&replay, step);
                    return Some(BoardExampleResult {
                        puzzle: replay.to_string_compact(),
                        candidate_masks: replay.candidates.to_vec(),
                        highlights,
                    });
                }
                return None; // Too full
            }
            apply_step(&mut replay, step);
        }
        None
    };

    // Worker: runs targeted then random, keeps retrying rejected examples
    let worker = |attempts: i32, found: &AtomicBool, counter: Option<&AtomicU32>| {
        let mut generator = Generator::new();
        let step_finder = StepFinder::new();
        let orchestrator = SolverOrchestrator::new();
        let check_threshold = check_clue_threshold(target_type);
        let target_diff = target_type.difficulty();
        let mut rng = rand::rng();

        for _ in 0..attempts {
            if found.load(Ordering::Relaxed) {
                return None;
            }
            if let Some(c) = counter {
                c.fetch_add(1, Ordering::Relaxed);
            }

            // Try targeted generation for Hard+ techniques
            if target_diff >= Difficulty::Hard {
                if let Some(tp) = generate_targeted_single(
                    target_type,
                    &mut generator,
                    &step_finder,
                    &orchestrator,
                    check_threshold,
                    &mut rng,
                ) {
                    if let Some(ex) = try_as_example(&tp) {
                        return Some(ex);
                    }
                }
                continue; // targeted is faster than random for Hard+
            }

            // Random generate-and-check (Easy/Medium only)
            let puzzle = generator.generate(target_diff, 50);
            let mut board = Board::new();
            board.load_from_string(&puzzle.puzzle);
            let solve = orchestrator.solve(&board, None);
            if !solve.solved {
                continue;
            }
            if let Some(idx) = solve.steps.iter().position(|s| s.step_type == target_type) {
                let tp = TechniquePuzzle {
                    puzzle,
                    step_index: idx,
                    steps: solve.steps,
                };
                if let Some(ex) = try_as_example(&tp) {
                    return Some(ex);
                }
            }
        }
        None
    };

    std::thread::scope(|s| {
        for _ in 0..n_threads {
            s.spawn(|| {
                if let Some(ex) = worker(attempts_per_thread, &found, counter) {
                    found.store(true, Ordering::Relaxed);
                    let mut lock = result.lock().unwrap();
                    if lock.is_none() {
                        *lock = Some(ex);
                    }
                }
            });
        }
    });

    result.into_inner().unwrap()
}

/// Single attempt at targeted generation (no threading, no loop).
/// Returns Some if technique found in solve path, None otherwise.
fn generate_targeted_single(
    target_type: SolutionType,
    generator: &mut Generator,
    step_finder: &StepFinder,
    orchestrator: &SolverOrchestrator,
    check_threshold: usize,
    rng: &mut impl Rng,
) -> Option<TechniquePuzzle> {
    generator.generate_full_grid();
    let solution = generator.new_full_sudoku;
    let mut puzzle = solution;

    let mut order: Vec<usize> = (0..81).collect();
    for i in (1..81).rev() {
        let j = rng.random_range(0..=i);
        order.swap(i, j);
    }

    let mut remaining = 81usize;
    let mut used = [false; 81];
    let mut technique_found = false;

    for &idx in &order {
        if puzzle[idx] == 0 || used[idx] {
            continue;
        }
        used[idx] = true;

        let row = idx / 9;
        let col = idx % 9;
        let mirror = (8 - row) * 9 + (8 - col);

        let saved = puzzle[idx];
        puzzle[idx] = 0;
        remaining -= 1;

        let mut mirror_removed = false;
        let saved_mirror;
        if idx != mirror && puzzle[mirror] != 0 {
            saved_mirror = puzzle[mirror];
            puzzle[mirror] = 0;
            used[mirror] = true;
            remaining -= 1;
            mirror_removed = true;
        } else {
            saved_mirror = 0;
        }

        let count = generator.count_solutions_values(&puzzle, 2);
        if count > 1 {
            puzzle[idx] = saved;
            remaining += 1;
            if mirror_removed {
                puzzle[mirror] = saved_mirror;
                remaining += 1;
            }
            continue;
        }

        if remaining <= check_threshold {
            let board = board_from_values(&puzzle);
            if step_finder.find_technique(&board, target_type).is_some() {
                technique_found = true;
                break;
            }
        }

        if remaining <= 20 {
            break;
        }
    }

    if !technique_found {
        return None;
    }

    // Verify technique in solve path (prefer target type over same-difficulty alternatives)
    let puzzle_str = puzzle_to_string(&puzzle);
    let mut board = Board::new();
    board.load_from_string(&puzzle_str);
    let result = orchestrator.solve_preferring(&board, None, target_type);
    if !result.solved {
        return None;
    }

    if let Some(idx) = result.steps.iter().position(|s| s.step_type == target_type) {
        let graded = Difficulty::from_score(result.score);
        return Some(TechniquePuzzle {
            puzzle: GeneratedPuzzle {
                puzzle: puzzle_str,
                solution,
                difficulty: graded,
                score: result.score,
            },
            step_index: idx,
            steps: result.steps,
        });
    }

    // Try removing more clues to break simpler path
    for &idx in &order {
        if puzzle[idx] == 0 || used[idx] {
            continue;
        }
        used[idx] = true;

        let saved = puzzle[idx];
        puzzle[idx] = 0;

        let count = generator.count_solutions_values(&puzzle, 2);
        if count != 1 {
            puzzle[idx] = saved;
            continue;
        }

        let ps = puzzle_to_string(&puzzle);
        let mut b = Board::new();
        b.load_from_string(&ps);
        let r = orchestrator.solve_preferring(&b, None, target_type);
        if r.solved {
            if let Some(step_idx) = r.steps.iter().position(|s| s.step_type == target_type) {
                let graded = Difficulty::from_score(r.score);
                return Some(TechniquePuzzle {
                    puzzle: GeneratedPuzzle {
                        puzzle: ps,
                        solution,
                        difficulty: graded,
                        score: r.score,
                    },
                    step_index: step_idx,
                    steps: r.steps,
                });
            }
        }

        puzzle[idx] = saved;
    }

    None
}

/// Generate an example by solving a specific puzzle string.
///
/// Used with curated puzzles known to produce certain techniques.
pub fn example_from_puzzle(
    puzzle_str: &str,
    target_type: SolutionType,
) -> Option<BoardExampleResult> {
    let orchestrator = SolverOrchestrator::new();
    let threshold = completeness_threshold(target_type);

    let mut board = Board::new();
    board.load_from_string(puzzle_str);
    let result = orchestrator.solve_preferring(&board, None, target_type);
    if !result.solved {
        return None;
    }

    let mut replay = Board::new();
    replay.load_from_string(puzzle_str);

    for step in &result.steps {
        if step.step_type == target_type {
            let filled = 81 - replay.unsolved as usize;
            if filled <= threshold {
                let highlights = build_highlights(&replay, step);
                return Some(BoardExampleResult {
                    puzzle: replay.to_string_compact(),
                    candidate_masks: replay.candidates.to_vec(),
                    highlights,
                });
            }
        }
        apply_step(&mut replay, step);
    }

    None
}

fn apply_step(board: &mut Board, step: &SolutionStep) {
    if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
        board.set_cell(step.cell_index as usize, step.value, false);
        board.set_all_exposed_singles();
    } else {
        for &(cell, cand) in &step.candidates_removed {
            board.set_candidate(cell, cand, false);
        }
        board.set_all_exposed_singles();
    }
}

/// Maximum filled cells for an educationally useful example.
fn completeness_threshold(t: SolutionType) -> usize {
    if t == SolutionType::FullHouse {
        return 81;
    }
    match t.difficulty() {
        Difficulty::Easy => 60,
        Difficulty::Medium => 65,
        Difficulty::Hard => 70,
        _ => 75, // Unfair, Extreme
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_naked_single_example() {
        let result = generate_example(SolutionType::NakedSingle, 100);
        assert!(result.is_some(), "Should find a Naked Single example");
        let ex = result.unwrap();
        assert_eq!(ex.puzzle.len(), 81);
        assert_eq!(ex.candidate_masks.len(), 81);
        assert!(!ex.highlights.is_empty());
    }

    #[test]
    fn test_generate_hidden_single_example() {
        let result = generate_example(SolutionType::HiddenSingle, 100);
        assert!(result.is_some(), "Should find a Hidden Single example");
    }

    #[test]
    fn test_generate_locked_candidates_example() {
        let result = generate_example(SolutionType::LockedCandidates1, 200);
        assert!(result.is_some(), "Should find a Locked Candidates example");
    }

    #[test]
    fn test_generate_with_technique() {
        let result = generate_with_technique(SolutionType::NakedPair, 500);
        assert!(result.is_some(), "Should find a puzzle requiring Naked Pair");
        let tp = result.unwrap();
        assert_eq!(tp.steps[tp.step_index].step_type, SolutionType::NakedPair);
    }

    #[test]
    fn test_generate_targeted_x_wing() {
        let mut generator = Generator::new();
        let step_finder = StepFinder::new();
        let orchestrator = SolverOrchestrator::new();
        let threshold = check_clue_threshold(SolutionType::XWing);
        let mut rng = rand::rng();
        let mut found = false;
        for _ in 0..500 {
            if let Some(tp) = generate_targeted_single(
                SolutionType::XWing,
                &mut generator,
                &step_finder,
                &orchestrator,
                threshold,
                &mut rng,
            ) {
                assert_eq!(tp.steps[tp.step_index].step_type, SolutionType::XWing);
                found = true;
                break;
            }
        }
        assert!(found, "Targeted generation should find X-Wing");
    }

    /// Skyscraper has a low detection rate (~1%), so this test needs
    /// many attempts and is marked #[ignore] to avoid slowing CI.
    /// Run explicitly with: cargo test -- --ignored
    #[test]
    #[ignore]
    fn test_generate_targeted_skyscraper() {
        let mut generator = Generator::new();
        let step_finder = StepFinder::new();
        let orchestrator = SolverOrchestrator::new();
        let threshold = check_clue_threshold(SolutionType::Skyscraper);
        let mut rng = rand::rng();
        let mut found = false;
        for _ in 0..5000 {
            if let Some(tp) = generate_targeted_single(
                SolutionType::Skyscraper,
                &mut generator,
                &step_finder,
                &orchestrator,
                threshold,
                &mut rng,
            ) {
                assert_eq!(tp.steps[tp.step_index].step_type, SolutionType::Skyscraper);
                found = true;
                break;
            }
        }
        assert!(found, "Targeted generation should find Skyscraper");
    }
}
