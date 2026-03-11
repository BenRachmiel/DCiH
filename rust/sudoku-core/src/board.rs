use crate::singles_queue::SinglesQueue;
use crate::tables::*;

const LENGTH: usize = 81;

/// Core Sudoku board — ~1.2KB, fully stack-allocated.
#[derive(Clone)]
pub struct Board {
    /// Cell values (0=empty, 1-9=set).
    pub values: [u8; LENGTH],
    /// Candidate bitmasks per cell (9-bit). 0 when cell is set.
    pub candidates: [u16; LENGTH],
    /// Which cells are givens.
    pub fixed: [bool; LENGTH],
    /// Free candidate count per constraint (27) per digit (0-9). free[constraint][digit].
    pub free: [[u8; 10]; 27],
    /// The unique solution (for error checking / hints).
    pub solution: [u8; LENGTH],
    /// Number of unsolved cells.
    pub unsolved: u8,
    /// Whether solution[] is populated.
    pub solution_set: bool,
    pub ns_queue: SinglesQueue,
    pub hs_queue: SinglesQueue,
}

impl Board {
    pub fn new() -> Self {
        let mut free = [[0u8; 10]; 27];
        for f in &mut free {
            for j in 1..=9 {
                f[j] = 9;
            }
        }
        Self {
            values: [0; LENGTH],
            candidates: [MAX_MASK; LENGTH],
            fixed: [false; LENGTH],
            free,
            solution: [0; LENGTH],
            unsolved: LENGTH as u8,
            solution_set: false,
            ns_queue: SinglesQueue::new(),
            hs_queue: SinglesQueue::new(),
        }
    }

    /// Reset to empty board.
    pub fn clear(&mut self) {
        self.values = [0; LENGTH];
        self.candidates = [MAX_MASK; LENGTH];
        self.fixed = [false; LENGTH];
        self.solution = [0; LENGTH];
        for f in &mut self.free {
            f[0] = 0;
            for j in 1..=9 {
                f[j] = 9;
            }
        }
        self.unsolved = LENGTH as u8;
        self.solution_set = false;
        self.ns_queue.clear();
        self.hs_queue.clear();
    }

    /// Fast copy for backtracking — skips fixed/solution/queues.
    pub fn copy_for_backtracking(&mut self, src: &Board) {
        self.values = src.values;
        self.candidates = src.candidates;
        self.free = src.free;
        self.unsolved = src.unsolved;
        self.ns_queue.clear();
        self.hs_queue.clear();
    }

    /// Set or remove a candidate. Returns false if puzzle becomes invalid.
    pub fn set_candidate(&mut self, index: usize, value: u8, set: bool) -> bool {
        let mask = MASKS[value as usize];
        if set {
            if self.candidates[index] & mask == 0 {
                self.candidates[index] |= mask;
                let new_anz = ANZ_VALUES[self.candidates[index] as usize];
                if new_anz == 1 {
                    self.ns_queue.add_single(index, value);
                } else if new_anz == 2 {
                    self.ns_queue.delete_naked_single(index);
                }
                for &c in &CONSTRAINTS[index] {
                    let cu = c as usize;
                    let vu = value as usize;
                    self.free[cu][vu] += 1;
                    let new_free = self.free[cu][vu];
                    if new_free == 1 {
                        self.add_hidden_single(cu, value);
                    } else if new_free == 2 {
                        self.hs_queue.delete_hidden_single(c, value);
                    }
                }
            }
        } else if self.candidates[index] & mask != 0 {
            self.candidates[index] &= !mask;
            if self.candidates[index] == 0 {
                return false; // invalid
            }
            if ANZ_VALUES[self.candidates[index] as usize] == 1 {
                self.ns_queue
                    .add_single(index, CAND_FROM_MASK[self.candidates[index] as usize]);
            }
            for &c in &CONSTRAINTS[index] {
                let cu = c as usize;
                let vu = value as usize;
                self.free[cu][vu] -= 1;
                let new_free = self.free[cu][vu];
                if new_free == 1 {
                    self.add_hidden_single(cu, value);
                } else if new_free == 0 {
                    self.hs_queue.delete_hidden_single(c, value);
                }
            }
        }
        true
    }

    /// Set a cell value. Returns false if puzzle becomes invalid.
    pub fn set_cell(&mut self, index: usize, value: u8, is_fixed: bool) -> bool {
        if self.values[index] == value {
            return true;
        }

        self.values[index] = value;
        self.fixed[index] = is_fixed;

        if value != 0 {
            // Save old candidates
            let old_cands_mask = self.candidates[index];
            self.candidates[index] = 0;
            self.unsolved -= 1;

            let mut valid = true;

            // Remove value from all buddies
            for &buddy in &BUDDIES_ARRAY[index] {
                let bi = buddy as usize;
                if !self.set_candidate(bi, value, false) {
                    valid = false;
                }
            }

            // Adjust free counts for all candidates that were in this cell
            let pv = &POSSIBLE_VALUES[old_cands_mask as usize];
            for i in 0..pv.count as usize {
                let cand = pv.digits[i];
                for &c in &CONSTRAINTS[index] {
                    let cu = c as usize;
                    let vu = cand as usize;
                    self.free[cu][vu] -= 1;
                    let new_free = self.free[cu][vu];
                    if new_free == 1 && cand != value {
                        self.add_hidden_single(cu, cand);
                    } else if new_free == 0 && cand != value {
                        valid = false;
                    }
                }
            }
            valid
        } else {
            // Removing a value (undo) — rebuild from scratch
            self.rebuild_internal_data();
            true
        }
    }

    /// Set cell without queue management (fast backtracking path).
    pub fn set_cell_bs(&mut self, index: usize, value: u8) {
        self.values[index] = value;
        self.candidates[index] = 0;
        let mask = MASKS[value as usize];
        for &buddy in &BUDDIES_ARRAY[index] {
            self.candidates[buddy as usize] &= !mask;
        }
    }

    #[inline]
    pub fn is_candidate(&self, index: usize, value: u8) -> bool {
        self.candidates[index] & MASKS[value as usize] != 0
    }

    pub fn is_valid_value(&self, index: usize, value: u8) -> bool {
        for &buddy in &BUDDIES_ARRAY[index] {
            if self.values[buddy as usize] == value {
                return false;
            }
        }
        true
    }

    #[inline]
    pub fn is_solved(&self) -> bool {
        self.unsolved == 0
    }

    /// Rebuild free counts, queues, and unsolved count from current state.
    pub fn rebuild_internal_data(&mut self) {
        self.ns_queue.clear();
        self.hs_queue.clear();
        for f in &mut self.free {
            for j in f.iter_mut() {
                *j = 0;
            }
        }

        let mut anz = 0u8;
        for index in 0..LENGTH {
            if self.values[index] != 0 {
                self.candidates[index] = 0;
            } else {
                anz += 1;
                let pv = &POSSIBLE_VALUES[self.candidates[index] as usize];
                for i in 0..pv.count as usize {
                    let cand = pv.digits[i];
                    for &c in &CONSTRAINTS[index] {
                        self.free[c as usize][cand as usize] += 1;
                    }
                }
                if ANZ_VALUES[self.candidates[index] as usize] == 1 {
                    self.ns_queue
                        .add_single(index, CAND_FROM_MASK[self.candidates[index] as usize]);
                }
            }
        }
        self.unsolved = anz;

        // Check for hidden singles
        for constraint in 0..27 {
            for value in 1..=9u8 {
                if self.free[constraint][value as usize] == 1 {
                    self.add_hidden_single(constraint, value);
                }
            }
        }
    }

    /// Rebuild all candidates from values only.
    pub fn rebuild_all_candidates(&mut self) {
        for i in 0..LENGTH {
            self.candidates[i] = if self.values[i] != 0 { 0 } else { MAX_MASK };
        }
        for i in 0..LENGTH {
            if self.values[i] != 0 {
                let mask = MASKS[self.values[i] as usize];
                for &buddy in &BUDDIES_ARRAY[i] {
                    self.candidates[buddy as usize] &= !mask;
                }
            }
        }
        self.rebuild_internal_data();
    }

    fn add_hidden_single(&mut self, constraint: usize, value: u8) -> bool {
        for &cell in &ALL_UNITS[constraint] {
            let ci = cell as usize;
            if self.is_candidate(ci, value) {
                self.hs_queue.add_single(ci, value);
                return true;
            }
        }
        false
    }

    /// Load from an 81-char string (digits and dots/zeros).
    pub fn load_from_string(&mut self, s: &str) {
        self.clear();
        for (i, ch) in s.chars().take(LENGTH).enumerate() {
            let v = match ch {
                '1'..='9' => ch as u8 - b'0',
                _ => 0,
            };
            if v != 0 {
                self.set_cell(i, v, true);
            }
        }
    }

    /// Export as 81-char string.
    pub fn to_string_compact(&self) -> String {
        let mut s = String::with_capacity(LENGTH);
        for i in 0..LENGTH {
            if self.values[i] != 0 {
                s.push((b'0' + self.values[i]) as char);
            } else {
                s.push('.');
            }
        }
        s
    }

    /// Process all queued naked and hidden singles until both queues are empty.
    /// Returns false if the puzzle becomes invalid.
    pub fn set_all_exposed_singles(&mut self) -> bool {
        let mut valid = true;
        loop {
            // Process naked singles first
            while valid {
                let Some((index, value)) = self.ns_queue.get_single() else {
                    break;
                };
                if self.candidates[index] & MASKS[value as usize] != 0 {
                    valid = self.set_cell(index, value, false);
                }
            }
            // Then hidden singles
            while valid {
                let Some((index, value)) = self.hs_queue.get_single() else {
                    break;
                };
                if self.candidates[index] & MASKS[value as usize] != 0 {
                    valid = self.set_cell(index, value, false);
                }
            }
            if !valid || (self.ns_queue.is_empty() && self.hs_queue.is_empty()) {
                break;
            }
        }
        valid
    }
}

/// Compute candidate bitmasks for all 81 cells from values only.
/// For each empty cell, eliminates digits seen in row/col/block peers.
pub fn compute_all_candidates(values: &[u8; 81]) -> [u16; 81] {
    let mut cands = [0u16; 81];
    for i in 0..81 {
        if values[i] != 0 {
            continue;
        }
        let mut mask = MAX_MASK;
        for &buddy in &BUDDIES_ARRAY[i] {
            let v = values[buddy as usize];
            if v != 0 {
                mask &= !MASKS[v as usize];
            }
        }
        cands[i] = mask;
    }
    cands
}

/// Find pencil mark errors: impossible candidates and missing solution digits.
///
/// Returns (to_remove, to_add) where:
/// - to_remove: (cell, digit) pairs for candidates that conflict with placed peers
/// - to_add: (cell, digit) pairs for solution digits missing from candidates
///
/// Returns None if no errors found.
pub fn find_pencil_mark_errors(
    values: &[u8; 81],
    cand_masks: &[u16; 81],
    solution: &[u8; 81],
) -> Option<(Vec<(usize, u8)>, Vec<(usize, u8)>)> {
    let valid = compute_all_candidates(values);
    let mut to_remove = Vec::new();
    let mut to_add = Vec::new();

    for i in 0..81 {
        if values[i] != 0 || cand_masks[i] == 0 {
            continue;
        }

        // Check for impossible candidates (set in user marks but not valid)
        let impossible = cand_masks[i] & !valid[i];
        if impossible != 0 {
            let pv = &POSSIBLE_VALUES[impossible as usize];
            for j in 0..pv.count as usize {
                to_remove.push((i, pv.digits[j]));
            }
        }

        // Check for missing solution digit
        if solution[i] != 0 {
            let sol_mask = MASKS[solution[i] as usize];
            if cand_masks[i] & sol_mask == 0 {
                to_add.push((i, solution[i]));
            }
        }
    }

    if to_remove.is_empty() && to_add.is_empty() {
        None
    } else {
        Some((to_remove, to_add))
    }
}

/// Find a naked or hidden single for a specific cell.
///
/// Returns the digit (1-9) if the cell has exactly one candidate (naked single)
/// or if a candidate is unique within a row/col/block (hidden single).
/// Returns 0 if no single found.
pub fn find_single_for_cell(values: &[u8; 81], cand_masks: &[u16; 81], cell: usize) -> u8 {
    if values[cell] != 0 || cand_masks[cell] == 0 {
        return 0;
    }

    // Naked single: exactly one candidate
    if ANZ_VALUES[cand_masks[cell] as usize] == 1 {
        return CAND_FROM_MASK[cand_masks[cell] as usize];
    }

    // Hidden single: check each candidate against row, col, block
    let pv = &POSSIBLE_VALUES[cand_masks[cell] as usize];
    for ci in 0..pv.count as usize {
        let digit = pv.digits[ci];
        let mask = MASKS[digit as usize];

        // Check all 3 constraints (row, col, block)
        for &constraint in &CONSTRAINTS[cell] {
            let mut unique = true;
            for &peer in &ALL_UNITS[constraint as usize] {
                let pi = peer as usize;
                if pi == cell {
                    continue;
                }
                if values[pi] == 0 && cand_masks[pi] & mask != 0 {
                    unique = false;
                    break;
                }
            }
            if unique {
                return digit;
            }
        }
    }

    0
}

impl Default for Board {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_load_and_compact() {
        let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
        let mut board = Board::new();
        board.load_from_string(puzzle);

        // Verify placed values
        assert_eq!(board.values[0], 5);
        assert_eq!(board.values[1], 3);
        assert_eq!(board.values[2], 0);

        // Round-trip: compact should reproduce the puzzle (with dots for zeros)
        let compact = board.to_string_compact();
        for (i, (a, b)) in puzzle.chars().zip(compact.chars()).enumerate() {
            let va = if a == '0' { '.' } else { a };
            assert_eq!(va, b, "Mismatch at position {i}");
        }
    }

    #[test]
    fn test_candidates_after_load() {
        let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
        let mut board = Board::new();
        board.load_from_string(puzzle);

        // Cell 2 (row 0, col 2) should have candidates — it's empty
        assert_ne!(board.candidates[2], 0);
        // Cell 0 is filled — no candidates
        assert_eq!(board.candidates[0], 0);
    }

    #[test]
    fn test_set_cell_removes_buddy_candidates() {
        let mut board = Board::new();
        // Place digit 5 at cell 0
        board.set_cell(0, 5, true);

        // All buddies of cell 0 should NOT have 5 as candidate
        for &buddy in &BUDDIES_ARRAY[0] {
            assert!(
                !board.is_candidate(buddy as usize, 5),
                "Buddy {} should not have candidate 5",
                buddy
            );
        }
    }

    #[test]
    fn test_singles_propagation() {
        let mut board = Board::new();
        // Set 8 of 9 digits in row 0
        for i in 0..8 {
            board.set_cell(i, (i + 1) as u8, true);
        }
        board.set_all_exposed_singles();

        // Cell 8 should be auto-filled with 9
        assert_eq!(board.values[8], 9);
    }

    #[test]
    fn test_is_solved() {
        let solved = "534678912672195348198342567859761423426853791713924856961537284287419635345286179";
        let mut board = Board::new();
        board.load_from_string(solved);
        board.set_all_exposed_singles();
        assert!(board.is_solved());
    }

    #[test]
    fn test_rebuild_all_candidates() {
        let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
        let mut board = Board::new();
        board.load_from_string(puzzle);

        // Save original candidates
        let orig_cands = board.candidates;

        // Rebuild
        board.rebuild_all_candidates();

        // Should match
        assert_eq!(board.candidates, orig_cands);
    }

    #[test]
    fn test_set_candidate() {
        let mut board = Board::new();
        // Cell 0 has all candidates initially
        assert!(board.is_candidate(0, 5));

        // Remove candidate 5
        board.set_candidate(0, 5, false);
        assert!(!board.is_candidate(0, 5));

        // Add it back
        board.set_candidate(0, 5, true);
        assert!(board.is_candidate(0, 5));
    }

    #[test]
    fn test_is_valid_value() {
        let mut board = Board::new();
        board.set_cell(0, 5, true);

        // 5 is not valid in any buddy of cell 0
        assert!(!board.is_valid_value(1, 5)); // same row
        assert!(!board.is_valid_value(9, 5)); // same col
        assert!(!board.is_valid_value(10, 5)); // same block

        // 5 is valid in a non-buddy cell
        assert!(board.is_valid_value(80, 5));
    }

    #[test]
    fn test_easy_puzzle_solvable_by_singles() {
        let puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
        let mut board = Board::new();
        board.load_from_string(puzzle);
        board.set_all_exposed_singles();
        assert!(board.is_solved(), "Easy puzzle should be solvable by singles propagation alone");
    }
}
