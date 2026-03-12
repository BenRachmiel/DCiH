use sudoku_core::board::Board;
use sudoku_core::generator::Generator;
use sudoku_core::solver::StepFinder;
use sudoku_core::types::{Difficulty, SolutionType};

/// Solve a puzzle by following hints from start to finish.
/// Verifies:
/// - Every elimination is valid (never removes the solution digit)
/// - Every single placement matches the solution
/// - Every step changes the board (no no-op loops)
/// - The puzzle is fully solved at the end
fn solve_by_hints(puzzle: &str, solution: &str) {
    let mut board = Board::new();
    board.load_from_string(puzzle);
    for (i, ch) in solution.chars().enumerate() {
        if let Some(d) = ch.to_digit(10) {
            board.solution[i] = d as u8;
        }
    }
    board.solution_set = true;

    let finder = StepFinder::new();
    let mut b = board.clone();

    for iter in 0..500 {
        if b.is_solved() {
            return;
        }
        let step = match finder.find_next_step(&b, None) {
            Some(s) => s,
            None => panic!(
                "Solver stuck at step {} — no step found.\nPuzzle: {}\nCurrent: {}",
                iter, puzzle, b.to_string_compact()
            ),
        };

        for &(cell, digit) in &step.candidates_removed {
            assert_ne!(
                board.solution[cell], digit,
                "{:?}: eliminating solution digit {} from cell {} (r{}c{}). \
                 Pattern cells: {:?}, digit: {}.\nPuzzle: {}",
                step.step_type, digit, cell, cell / 9, cell % 9,
                step.indices, step.value, puzzle
            );
        }

        if step.step_type.is_single() {
            assert_eq!(
                board.solution[step.cell_index as usize], step.value,
                "{:?}: placing {} at cell {} but solution is {}.\nPuzzle: {}",
                step.step_type, step.value, step.cell_index,
                board.solution[step.cell_index as usize], puzzle
            );
        }

        let values_before = b.values;
        let cands_before = b.candidates;

        if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
            b.set_cell(step.cell_index as usize, step.value, false);
        } else {
            for &(cell, cand) in &step.candidates_removed {
                b.set_candidate(cell, cand, false);
            }
        }
        b.set_all_exposed_singles();

        assert!(
            b.values != values_before || b.candidates != cands_before,
            "{:?}: step {} was a no-op (board unchanged). \
             Pattern cells: {:?}, digit: {}, elims: {:?}.\nPuzzle: {}",
            step.step_type, iter, step.indices, step.value,
            step.candidates_removed, puzzle
        );
    }

    panic!(
        "Solver did not finish within 500 steps.\nPuzzle: {}\nCurrent: {}",
        puzzle,
        b.to_string_compact()
    );
}

// ── Regression: ER false elimination ──────────────────────────────────────────

#[test]
fn test_er_regression() {
    solve_by_hints(
        "800005460000030080602800903000000008080050090100008000914083206268040009375900841",
        "831295467497631582652874913543719628786352194129468735914583276268147359375926841",
    );
}

// ── Seeded generation: deterministic + reproducible ──────────────────────────

const SEEDS: &[u64] = &[
    42, 137, 271, 314, 577, 691, 853, 997, 1234, 1597,
    1729, 2048, 2718, 3141, 4096, 5381, 6174, 7919, 8675, 9973,
];

/// Generate puzzles from fixed seeds across all difficulties, then
/// solve each by following hints. Deterministic and reproducible.
#[test]
fn test_seeded_hint_following() {
    let difficulties = [
        Difficulty::Easy,
        Difficulty::Medium,
        Difficulty::Hard,
        Difficulty::Unfair,
        Difficulty::Extreme,
    ];

    for &seed in SEEDS {
        let mut generator = Generator::with_seed(seed);
        for &diff in &difficulties {
            let result = generator.generate(diff, 50);
            let solution_str: String =
                result.solution.iter().map(|&d| (b'0' + d) as char).collect();
            solve_by_hints(&result.puzzle, &solution_str);
        }
    }
}
