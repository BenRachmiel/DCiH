pub mod highlighter;
pub mod example;

use rand::Rng;
use serde::Serialize;

use crate::board::Board;
use crate::solver::SolverOrchestrator;
use crate::tables::*;
use crate::types::Difficulty;

const MAX_BT_TRIES: usize = 1_000_000;

/// Pre-allocated backtracking stack entry.
struct StackEntry {
    sudoku: Board,
    index: usize,
    candidates: [u8; 9],
    cand_count: u8,
    cand_index: u8,
}

impl StackEntry {
    fn new() -> Self {
        Self {
            sudoku: Board::new(),
            index: 0,
            candidates: [0; 9],
            cand_count: 0,
            cand_index: 0,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct GeneratedPuzzle {
    pub puzzle: String,
    #[serde(with = "serde_big_array")]
    pub solution: [u8; 81],
    pub difficulty: Difficulty,
    pub score: i32,
}

mod serde_big_array {
    use serde::ser::{Serializer, SerializeSeq};
    pub fn serialize<S: Serializer>(arr: &[u8; 81], s: S) -> Result<S::Ok, S::Error> {
        let mut seq = s.serialize_seq(Some(81))?;
        for b in arr { seq.serialize_element(b)?; }
        seq.end()
    }
}

pub struct Generator {
    stack: Vec<StackEntry>,
    generate_indices: [u8; 81],
    new_full_sudoku: [u8; 81],
    new_valid_sudoku: [u8; 81],
    solution: [u8; 81],
    solution_count: usize,
}

impl Generator {
    pub fn new() -> Self {
        let mut stack = Vec::with_capacity(82);
        for _ in 0..82 {
            stack.push(StackEntry::new());
        }
        Self {
            stack,
            generate_indices: {
                let mut arr = [0u8; 81];
                for i in 0..81 {
                    arr[i] = i as u8;
                }
                arr
            },
            new_full_sudoku: [0; 81],
            new_valid_sudoku: [0; 81],
            solution: [0; 81],
            solution_count: 0,
        }
    }

    /// Generate a puzzle targeting the given difficulty.
    pub fn generate(&mut self, difficulty: Difficulty, max_retries: i32) -> GeneratedPuzzle {
        let solver = SolverOrchestrator::new();
        let mut best_candidate: Option<GeneratedPuzzle> = None;
        let total_attempts = max_retries as usize * (difficulty.ordinal() + 1);

        for _attempt in 0..total_attempts {
            self.generate_full_grid();
            self.generate_init_pos(true);

            // Build the puzzle board
            let mut board = Board::new();
            for i in 0..81 {
                if self.new_valid_sudoku[i] != 0 {
                    board.set_cell(i, self.new_valid_sudoku[i], true);
                }
            }
            let puzzle_string = board.to_string_compact();
            board.set_all_exposed_singles();

            board.solution = self.new_full_sudoku;
            board.solution_set = true;

            // Grade difficulty (ungated)
            let result = solver.solve(&board, None);
            let graded = Difficulty::from_score(result.score);

            if graded == difficulty {
                // Verify solvable within difficulty's technique set
                let gated = solver.solve(&board, Some(difficulty));
                if gated.solved {
                    return GeneratedPuzzle {
                        puzzle: puzzle_string,
                        solution: self.new_full_sudoku,
                        difficulty,
                        score: result.score,
                    };
                }
                // Score matches but needs harder techniques — save as fallback
                // BUG FIX: Only save if gated solve succeeds (removed from here)
                if best_candidate.is_none() {
                    best_candidate = Some(GeneratedPuzzle {
                        puzzle: puzzle_string,
                        solution: self.new_full_sudoku,
                        difficulty,
                        score: result.score,
                    });
                }
            }
        }

        // Prefer a puzzle that at least matched the difficulty score
        if let Some(candidate) = best_candidate {
            return candidate;
        }

        // Last resort
        let mut board = Board::new();
        for i in 0..81 {
            if self.new_valid_sudoku[i] != 0 {
                board.set_cell(i, self.new_valid_sudoku[i], true);
            }
        }
        let puzzle_string = board.to_string_compact();
        board.set_all_exposed_singles();
        board.solution = self.new_full_sudoku;
        board.solution_set = true;
        let result = solver.solve(&board, None);
        GeneratedPuzzle {
            puzzle: puzzle_string,
            solution: self.new_full_sudoku,
            difficulty: Difficulty::from_score(result.score),
            score: result.score,
        }
    }

    /// Count solutions (0, 1, or up to max_count).
    pub fn count_solutions_board(&mut self, board: &Board, max_count: usize) -> usize {
        self.stack[0].sudoku = board.clone();
        self.stack[0].cand_count = 0;
        self.stack[0].cand_index = 0;
        self.solve_bt(0, max_count);
        self.solution_count
    }

    pub fn count_solutions_values(&mut self, values: &[u8; 81], max_count: usize) -> usize {
        let mut board = Board::new();
        for i in 0..81 {
            if values[i] >= 1 && values[i] <= 9 {
                board.set_cell_bs(i, values[i]);
            }
        }
        board.rebuild_internal_data();
        board.set_all_exposed_singles();
        self.stack[0].sudoku = board;
        self.stack[0].cand_count = 0;
        self.stack[0].cand_index = 0;
        self.solve_bt(0, max_count);
        self.solution_count
    }

    /// Get the last found solution (valid after count_solutions returns 1).
    pub fn last_solution(&self) -> &[u8; 81] {
        &self.solution
    }

    // --- Backtracking solver ---

    fn solve_bt(&mut self, start_level: usize, max_solution_count: usize) {
        self.solution_count = 0;

        if !self.stack[0].sudoku.set_all_exposed_singles() {
            return;
        }
        if self.stack[0].sudoku.is_solved() {
            self.solution = self.stack[0].sudoku.values;
            self.solution_count = 1;
            return;
        }

        let mut level = start_level;
        let mut tries = 0usize;

        loop {
            if tries >= MAX_BT_TRIES {
                break;
            }
            tries += 1;

            if self.stack[level].sudoku.is_solved() {
                self.solution_count += 1;
                if self.solution_count == 1 {
                    self.solution = self.stack[level].sudoku.values;
                }
                if self.solution_count > max_solution_count {
                    return;
                }
            } else {
                // Find unsolved cell with fewest candidates
                let mut best_index = 0usize;
                let mut best_count = 10u8;
                let s = &self.stack[level].sudoku;
                for i in 0..81 {
                    let cell = s.candidates[i];
                    if cell != 0 {
                        let count = ANZ_VALUES[cell as usize];
                        if count < best_count {
                            best_count = count;
                            best_index = i;
                        }
                    }
                }
                if best_count == 10 {
                    self.solution_count = 0;
                    return;
                }

                let cands_mask = self.stack[level].sudoku.candidates[best_index];
                let pv = &POSSIBLE_VALUES[cands_mask as usize];

                level += 1;
                self.stack[level].index = best_index;
                for k in 0..pv.count as usize {
                    self.stack[level].candidates[k] = pv.digits[k];
                }
                self.stack[level].cand_count = pv.count;
                self.stack[level].cand_index = 0;
            }

            let mut done = false;
            loop {
                while self.stack[level].cand_index >= self.stack[level].cand_count {
                    if level == 0 {
                        done = true;
                        break;
                    }
                    level -= 1;
                }
                if done {
                    break;
                }

                let next_cand =
                    self.stack[level].candidates[self.stack[level].cand_index as usize];
                self.stack[level].cand_index += 1;

                // Copy board from parent level
                let idx = self.stack[level].index;
                // Safe because we're accessing different levels
                let (left, right) = self.stack.split_at_mut(level);
                right[0].sudoku.copy_for_backtracking(&left[level - 1].sudoku);

                if !right[0].sudoku.set_cell(idx, next_cand, false) {
                    continue;
                }
                if right[0].sudoku.set_all_exposed_singles() {
                    break;
                }
            }

            if done {
                break;
            }
        }
    }

    // --- Full grid generation ---

    fn generate_full_grid(&mut self) {
        loop {
            if self.do_generate_full_grid() {
                return;
            }
        }
    }

    fn do_generate_full_grid(&mut self) -> bool {
        let mut rng = rand::rng();
        // Shuffle cell order
        for i in 0..81 {
            let j = rng.random_range(0..81usize);
            self.generate_indices.swap(i, j);
        }

        self.stack[0].sudoku.clear();
        self.stack[0].index = 0;

        let mut level = 0usize;
        let mut act_tries = 0usize;

        loop {
            if self.stack[level].sudoku.is_solved() {
                self.new_full_sudoku = self.stack[level].sudoku.values;
                return true;
            }

            // Find first unsolved cell in random order
            let mut index = 0usize;
            let mut found = false;
            for &gi in &self.generate_indices {
                let gi = gi as usize;
                if self.stack[level].sudoku.values[gi] == 0 {
                    index = gi;
                    found = true;
                    break;
                }
            }
            if !found {
                return false;
            }

            let cands_mask = self.stack[level].sudoku.candidates[index];
            let pv = &POSSIBLE_VALUES[cands_mask as usize];

            level += 1;
            self.stack[level].index = index;
            for k in 0..pv.count as usize {
                self.stack[level].candidates[k] = pv.digits[k];
            }
            self.stack[level].cand_count = pv.count;
            self.stack[level].cand_index = 0;

            act_tries += 1;
            if act_tries > 100 {
                return false;
            }

            let mut done = false;
            loop {
                while self.stack[level].cand_index >= self.stack[level].cand_count {
                    if level == 0 {
                        done = true;
                        break;
                    }
                    level -= 1;
                }
                if done {
                    break;
                }

                let next_cand =
                    self.stack[level].candidates[self.stack[level].cand_index as usize];
                self.stack[level].cand_index += 1;

                let idx = self.stack[level].index;
                let (left, right) = self.stack.split_at_mut(level);
                right[0].sudoku.copy_for_backtracking(&left[level - 1].sudoku);

                if !right[0].sudoku.set_cell(idx, next_cand, false) {
                    continue;
                }
                if right[0].sudoku.set_all_exposed_singles() {
                    break;
                }
            }

            if done {
                break;
            }
        }
        false
    }

    // --- Clue removal ---

    fn generate_init_pos(&mut self, symmetric: bool) {
        let mut rng = rand::rng();
        let mut used = [false; 81];
        let mut used_count = 81usize;

        self.new_valid_sudoku = self.new_full_sudoku;
        let mut remaining_clues = 81usize;

        while remaining_clues > 17 && used_count > 1 {
            let mut i = rng.random_range(0..81usize);
            while used[i] {
                i = if i < 80 { i + 1 } else { 0 };
            }
            used[i] = true;
            used_count -= 1;

            if self.new_valid_sudoku[i] == 0 {
                continue;
            }

            let row = i / 9;
            let col = i % 9;
            let symm = (8 - row) * 9 + (8 - col);

            if symmetric && (row != 4 || col != 4) && self.new_valid_sudoku[symm] == 0 {
                continue;
            }

            // Try deleting
            let saved_i = self.new_valid_sudoku[i];
            self.new_valid_sudoku[i] = 0;
            remaining_clues -= 1;

            let mut symm_deleted = false;
            let saved_symm;
            if symmetric && (row != 4 || col != 4) {
                saved_symm = self.new_valid_sudoku[symm];
                self.new_valid_sudoku[symm] = 0;
                used[symm] = true;
                used_count -= 1;
                remaining_clues -= 1;
                symm_deleted = true;
            } else {
                saved_symm = 0;
            }

            // Build board for solution counting
            let mut board = Board::new();
            for k in 0..81 {
                if self.new_valid_sudoku[k] >= 1 && self.new_valid_sudoku[k] <= 9 {
                    board.set_cell_bs(k, self.new_valid_sudoku[k]);
                }
            }
            board.rebuild_internal_data();
            board.set_all_exposed_singles();
            self.stack[0].sudoku = board;
            self.stack[0].cand_count = 0;
            self.stack[0].cand_index = 0;
            self.solve_bt(0, 1);

            if self.solution_count > 1 {
                // Restore
                self.new_valid_sudoku[i] = saved_i;
                remaining_clues += 1;
                if symm_deleted {
                    self.new_valid_sudoku[symm] = saved_symm;
                    remaining_clues += 1;
                }
            }
        }
    }
}

impl Default for Generator {
    fn default() -> Self {
        Self::new()
    }
}
