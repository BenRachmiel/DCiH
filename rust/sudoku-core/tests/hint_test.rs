use sudoku_core::board::Board;
use sudoku_core::generator::highlighter::build_highlights;
use sudoku_core::solver::StepFinder;
use sudoku_core::step::SolutionStep;
use sudoku_core::types::{HighlightRole, SolutionType};

const EASY_PUZZLE: &str = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
const HARD_PUZZLE: &str = "000801000000000043500000000000070800000000100020030000600000075003400000000200600";

#[test]
fn test_step_finder_returns_step_for_valid_board() {
    let mut board = Board::new();
    board.load_from_string(EASY_PUZZLE);
    let finder = StepFinder::new();
    let step = finder.find_next_step(&board, None);
    assert!(step.is_some(), "StepFinder should find a step for a valid puzzle");
    assert!(step.unwrap().step_type.is_single(), "Easy puzzle first step should be a single");
}

#[test]
fn test_step_finder_returns_none_for_solved_board() {
    let solved = "534678912672195348198342567859761423426853791713924856961537284287419635345286179";
    let mut board = Board::new();
    board.load_from_string(solved);
    let finder = StepFinder::new();
    let step = finder.find_next_step(&board, None);
    assert!(step.is_none(), "StepFinder should return None for a solved board");
}

#[test]
fn test_describe_vague() {
    let step = SolutionStep::single(SolutionType::NakedSingle, 10, 5);
    assert_eq!(step.describe_vague(), "Naked Single");
}

#[test]
fn test_describe_concrete_single() {
    let step = SolutionStep::single(SolutionType::HiddenSingle, 20, 7);
    let desc = step.describe_concrete();
    assert!(desc.contains("Hidden Single"));
    assert!(desc.contains("7"));
    assert!(desc.contains("r3c3"));
}

#[test]
fn test_describe_concrete_elimination() {
    let step = SolutionStep::elimination(
        SolutionType::NakedPair,
        vec![0, 1],
        0,
        vec![(2, 3), (5, 3), (2, 7), (5, 7)],
    );
    let desc = step.describe_concrete();
    assert!(desc.contains("Naked Pair"));
    assert!(desc.contains("3") && desc.contains("7"));
}

#[test]
fn test_describe_concrete_fish() {
    let step = SolutionStep::elimination(
        SolutionType::XWing,
        vec![0, 8, 72, 80],
        4,
        vec![(3, 4), (6, 4)],
    );
    let desc = step.describe_concrete();
    assert!(desc.contains("X-Wing"));
    assert!(desc.contains("4"));
}

#[test]
fn test_build_highlights_for_single() {
    let mut board = Board::new();
    board.load_from_string(EASY_PUZZLE);
    let finder = StepFinder::new();
    let step = finder.find_next_step(&board, None).unwrap();
    let highlights = build_highlights(&board, &step);
    assert!(!highlights.is_empty());
    assert!(highlights.iter().any(|h| h.role == HighlightRole::Defining));
}

#[test]
fn test_build_highlights_for_elimination() {
    let mut board = Board::new();
    board.load_from_string(HARD_PUZZLE);
    let finder = StepFinder::new();

    // Advance past singles
    let mut step_opt;
    loop {
        step_opt = finder.find_next_step(&board, None);
        match &step_opt {
            None => break,
            Some(step) if !step.step_type.is_single() => break,
            Some(step) => {
                board.set_cell(step.cell_index as usize, step.value, false);
                board.set_all_exposed_singles();
            }
        }
    }

    if let Some(step) = step_opt {
        if !step.step_type.is_single() {
            let highlights = build_highlights(&board, &step);
            assert!(!highlights.is_empty(), "Elimination step should produce highlights");
            assert!(
                highlights.iter().any(|h| h.role == HighlightRole::Elimination),
                "Should have ELIMINATION highlights for {:?}",
                step.step_type
            );
        }
    }
}

#[test]
fn test_build_solver_board_from_values() {
    let puzzle = EASY_PUZZLE;
    let mut values = [0u8; 81];
    for (i, ch) in puzzle.chars().enumerate() {
        if ch >= '1' && ch <= '9' {
            values[i] = ch as u8 - b'0';
        }
    }

    let mut board = Board::new();
    for i in 0..81 {
        if values[i] != 0 {
            board.set_cell(i, values[i], true);
        }
    }

    for i in 0..81 {
        assert_eq!(board.values[i], values[i], "Cell {} should match", i);
    }
    for i in 0..81 {
        if values[i] == 0 {
            assert_ne!(board.candidates[i], 0, "Empty cell {} should have candidates", i);
        }
    }

    let finder = StepFinder::new();
    assert!(finder.find_next_step(&board, None).is_some());
}
