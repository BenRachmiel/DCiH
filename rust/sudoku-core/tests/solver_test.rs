use sudoku_core::board::Board;
use sudoku_core::solver::{SolverOrchestrator, StepFinder};
use sudoku_core::types::{Difficulty, SolutionType};
use sudoku_core::step::SolutionStep;

const EASY_PUZZLE: &str = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";

#[test]
fn test_solve_easy_puzzle() {
    let mut board = Board::new();
    board.load_from_string(EASY_PUZZLE);
    board.solution_set = false;

    let solver = SolverOrchestrator::new();
    let result = solver.solve(&board, None);
    assert!(result.solved);
    for step in &result.steps {
        assert!(
            step.step_type == SolutionType::FullHouse
                || step.step_type == SolutionType::NakedSingle
                || step.step_type == SolutionType::HiddenSingle,
            "Easy puzzle should only use singles, got: {:?}",
            step.step_type
        );
    }
}

#[test]
fn test_difficulty_from_score() {
    assert_eq!(Difficulty::from_score(100), Difficulty::Easy);
    assert_eq!(Difficulty::from_score(799), Difficulty::Easy);
    assert_eq!(Difficulty::from_score(800), Difficulty::Medium);
    assert_eq!(Difficulty::from_score(999), Difficulty::Medium);
    assert_eq!(Difficulty::from_score(1000), Difficulty::Hard);
    assert_eq!(Difficulty::from_score(1599), Difficulty::Hard);
    assert_eq!(Difficulty::from_score(1600), Difficulty::Unfair);
    assert_eq!(Difficulty::from_score(1800), Difficulty::Extreme);
    assert_eq!(Difficulty::from_score(10000), Difficulty::Extreme);
}

#[test]
fn test_difficulty_enum_comparison() {
    assert!(Difficulty::Easy < Difficulty::Medium);
    assert!(Difficulty::Medium < Difficulty::Hard);
    assert!(Difficulty::Hard < Difficulty::Unfair);
    assert!(Difficulty::Unfair < Difficulty::Extreme);
    assert!(Difficulty::Easy <= Difficulty::Easy);
}

#[test]
fn test_solution_type_difficulty_consistency() {
    assert_eq!(SolutionType::FullHouse.difficulty(), Difficulty::Easy);
    assert_eq!(SolutionType::NakedSingle.difficulty(), Difficulty::Easy);
    assert_eq!(SolutionType::HiddenSingle.difficulty(), Difficulty::Easy);

    assert_eq!(SolutionType::LockedCandidates1.difficulty(), Difficulty::Medium);
    assert_eq!(SolutionType::NakedPair.difficulty(), Difficulty::Medium);
    assert_eq!(SolutionType::HiddenTriple.difficulty(), Difficulty::Medium);

    assert_eq!(SolutionType::XWing.difficulty(), Difficulty::Hard);
    assert_eq!(SolutionType::XyWing.difficulty(), Difficulty::Hard);
    assert_eq!(SolutionType::NakedQuadruple.difficulty(), Difficulty::Hard);

    assert_eq!(SolutionType::SimpleColorsTrap.difficulty(), Difficulty::Unfair);
    assert_eq!(SolutionType::MultiColors1.difficulty(), Difficulty::Unfair);

    assert_eq!(SolutionType::XChain.difficulty(), Difficulty::Extreme);
    assert_eq!(SolutionType::BruteForce.difficulty(), Difficulty::Extreme);
}

#[test]
fn test_gated_solver_easy_puzzle() {
    let mut board = Board::new();
    board.load_from_string(EASY_PUZZLE);
    board.solution_set = false;

    let solver = SolverOrchestrator::new();
    let result = solver.solve(&board, Some(Difficulty::Easy));
    assert!(result.solved, "Easy puzzle should be solvable with EASY gate");
}

#[test]
fn test_gated_solver_stops_when_stuck() {
    let puzzle = "000000010400000000020000000000050407008000300001090000300400200050100000000806000";
    let mut board = Board::new();
    board.load_from_string(puzzle);

    let solver = SolverOrchestrator::new();

    let ungated = solver.solve(&board, None);
    assert!(ungated.solved, "Should be solvable ungated");

    let uses_advanced = ungated.steps.iter().any(|s| s.step_type.difficulty() > Difficulty::Easy);
    if uses_advanced {
        let gated = solver.solve(&board, Some(Difficulty::Easy));
        assert!(!gated.solved, "Should not solve with EASY gate when advanced techniques needed");
    }
}

// Helper: replay solver to find a specific technique type
fn find_step_of_type(puzzle: &str, target: SolutionType) -> Option<(Board, SolutionStep)> {
    let finder = StepFinder::new();
    let mut board = Board::new();
    board.load_from_string(puzzle);

    while !board.is_solved() {
        let step = finder.find_next_step(&board, None)?;
        if step.step_type == target {
            return Some((board, step));
        }

        if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
            board.set_cell(step.cell_index as usize, step.value, false);
            board.set_all_exposed_singles();
        } else {
            for &(cell_index, candidate) in &step.candidates_removed {
                board.set_candidate(cell_index, candidate, false);
            }
            board.set_all_exposed_singles();
        }
    }
    None
}

#[test]
fn test_solver_finds_locked_pair() {
    let puzzles = [
        "000000010400000000020000000000050407008000300001090000300400200050100000000806000",
        "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
        "003000200090000006000630040006003900050904020004100800020048000400000050005000300",
    ];
    let mut found = None;
    for puzzle in &puzzles {
        found = find_step_of_type(puzzle, SolutionType::LockedPair);
        if found.is_some() { break; }
    }
    if let Some((board, step)) = found {
        assert_eq!(step.step_type, SolutionType::LockedPair);
        assert_eq!(step.indices.len(), 2, "Locked pair should have 2 cells");
        assert!(!step.candidates_removed.is_empty(), "Should have eliminations");

        // All in same block
        let blocks: std::collections::HashSet<_> = step.indices.iter()
            .map(|&i| sudoku_core::tables::get_block(i))
            .collect();
        assert_eq!(blocks.len(), 1, "All cells must be in the same block");

        // All in same row or col
        let rows: std::collections::HashSet<_> = step.indices.iter().map(|&i| i / 9).collect();
        let cols: std::collections::HashSet<_> = step.indices.iter().map(|&i| i % 9).collect();
        assert!(rows.len() == 1 || cols.len() == 1, "All cells must be in same row or col");

        // Combined candidates = exactly 2
        let mut combined = 0u16;
        for &cell in &step.indices {
            combined |= board.candidates[cell];
        }
        assert_eq!(
            sudoku_core::tables::ANZ_VALUES[combined as usize], 2,
            "Combined candidates should be exactly 2"
        );
    }
}

#[test]
fn test_locked_pair_has_solver() {
    assert!(SolutionType::LockedPair.has_solver());
    assert!(SolutionType::LockedTriple.has_solver());
}

#[test]
fn test_has_solver_flags() {
    assert!(SolutionType::Skyscraper.has_solver());
    assert!(SolutionType::TwoStringKite.has_solver());
    assert!(SolutionType::EmptyRectangle.has_solver());
    assert!(SolutionType::TurbotFish.has_solver());
}
