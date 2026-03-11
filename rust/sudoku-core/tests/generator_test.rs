use sudoku_core::board::Board;
use sudoku_core::generator::Generator;
use sudoku_core::solver::SolverOrchestrator;
use sudoku_core::types::Difficulty;

#[test]
fn test_count_solutions_valid_puzzle() {
    let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
    let mut board = Board::new();
    board.load_from_string(puzzle);

    let mut generator = Generator::new();
    let count = generator.count_solutions_board(&board, 2);
    assert_eq!(count, 1, "Puzzle should have exactly 1 solution");
}

#[test]
fn test_count_solutions_empty_board() {
    let board = Board::new();
    let mut generator = Generator::new();
    let count = generator.count_solutions_board(&board, 2);
    assert!(count > 1, "Empty board should have multiple solutions");
}

#[test]
fn test_solver_grading() {
    let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
    let mut board = Board::new();
    board.load_from_string(puzzle);

    let mut generator = Generator::new();
    generator.count_solutions_board(&board, 1);

    let solver = SolverOrchestrator::new();
    let result = solver.solve(&board, None);
    assert!(result.solved);
    assert!(result.score > 0);
}

#[test]
fn test_generate_returns_valid_puzzle() {
    let mut generator = Generator::new();
    let puzzle = generator.generate(Difficulty::Easy, 200);
    assert_eq!(puzzle.puzzle.len(), 81);
    assert_eq!(puzzle.solution.len(), 81);

    // Verify solution is complete
    assert!(puzzle.solution.iter().all(|&v| v >= 1 && v <= 9));

    // Verify puzzle string has some clues and some blanks
    let clue_count = puzzle.puzzle.chars().filter(|&c| c >= '1' && c <= '9').count();
    assert!(clue_count >= 17, "Should have at least 17 clues");
    assert!(clue_count < 81, "Should have some blanks");
}

#[test]
fn test_generate_easy_returns_easy() {
    let mut generator = Generator::new();
    let puzzle = generator.generate(Difficulty::Easy, 200);
    assert_eq!(puzzle.difficulty, Difficulty::Easy, "Requesting EASY should return EASY");
}
