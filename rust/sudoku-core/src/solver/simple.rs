use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::{Solver, combinations};

pub struct SimpleSolver;

impl Solver for SimpleSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        if let Some(s) = find_full_house(board) {
            return vec![s];
        }
        if let Some(s) = find_naked_single(board) {
            return vec![s];
        }
        if let Some(s) = find_hidden_single(board) {
            return vec![s];
        }
        if let Some(s) = find_locked_candidates(board) {
            return vec![s];
        }
        if let Some(s) = find_locked_subset(board, 2) {
            return vec![s];
        }
        if let Some(s) = find_naked_subset(board, 2) {
            return vec![s];
        }
        if let Some(s) = find_hidden_subset(board, 2) {
            return vec![s];
        }
        if let Some(s) = find_locked_subset(board, 3) {
            return vec![s];
        }
        if let Some(s) = find_naked_subset(board, 3) {
            return vec![s];
        }
        if let Some(s) = find_hidden_subset(board, 3) {
            return vec![s];
        }
        if let Some(s) = find_naked_subset(board, 4) {
            return vec![s];
        }
        if let Some(s) = find_hidden_subset(board, 4) {
            return vec![s];
        }
        vec![]
    }
}

/// Full House: a unit with exactly one empty cell.
fn find_full_house(board: &Board) -> Option<SolutionStep> {
    for unit in &ALL_UNITS {
        let mut empty_index = 0usize;
        let mut empty_count = 0u8;
        let mut digit_mask = [false; 10];
        for &cell in unit {
            let ci = cell as usize;
            if board.values[ci] == 0 {
                empty_count += 1;
                empty_index = ci;
            } else {
                digit_mask[board.values[ci] as usize] = true;
            }
        }
        if empty_count == 1 {
            for d in 1..=9u8 {
                if !digit_mask[d as usize] {
                    return Some(SolutionStep::single(SolutionType::FullHouse, empty_index, d));
                }
            }
        }
    }
    None
}

/// Naked Single: cell with exactly one candidate.
fn find_naked_single(board: &Board) -> Option<SolutionStep> {
    for i in 0..81 {
        if board.candidates[i] != 0 && ANZ_VALUES[board.candidates[i] as usize] == 1 {
            let value = CAND_FROM_MASK[board.candidates[i] as usize];
            return Some(SolutionStep::single(SolutionType::NakedSingle, i, value));
        }
    }
    None
}

/// Hidden Single: candidate appears only once in a unit.
fn find_hidden_single(board: &Board) -> Option<SolutionStep> {
    for unit in &ALL_UNITS {
        for digit in 1..=9u8 {
            let mut count = 0u8;
            let mut last_index = 0usize;
            for &cell in unit {
                let ci = cell as usize;
                if board.is_candidate(ci, digit) {
                    count += 1;
                    last_index = ci;
                }
            }
            if count == 1 && board.values[last_index] == 0 {
                return Some(SolutionStep::single(
                    SolutionType::HiddenSingle,
                    last_index,
                    digit,
                ));
            }
        }
    }
    None
}

/// Locked Candidates Type 1 (Pointing) and Type 2 (Claiming).
fn find_locked_candidates(board: &Board) -> Option<SolutionStep> {
    // Type 1: Pointing — candidate in a block confined to one row/col
    for block in 0..9 {
        let block_cells = &BLOCKS[block];
        for digit in 1..=9u8 {
            let mut positions = Vec::new();
            for &cell in block_cells {
                if board.is_candidate(cell as usize, digit) {
                    positions.push(cell as usize);
                }
            }
            if positions.len() < 2 {
                continue;
            }

            // All in same row?
            let row = positions[0] / 9;
            if positions.iter().all(|&p| p / 9 == row) {
                let mut eliminations = Vec::new();
                for &cell in &ROWS[row] {
                    let ci = cell as usize;
                    if !positions.contains(&ci) && board.is_candidate(ci, digit) {
                        eliminations.push((ci, digit));
                    }
                }
                if !eliminations.is_empty() {
                    return Some(SolutionStep::elimination(
                        SolutionType::LockedCandidates1,
                        positions,
                        digit,
                        eliminations,
                    ));
                }
            }

            // All in same col?
            let col = positions[0] % 9;
            if positions.iter().all(|&p| p % 9 == col) {
                let mut eliminations = Vec::new();
                for &cell in &COLS[col] {
                    let ci = cell as usize;
                    if !positions.contains(&ci) && board.is_candidate(ci, digit) {
                        eliminations.push((ci, digit));
                    }
                }
                if !eliminations.is_empty() {
                    return Some(SolutionStep::elimination(
                        SolutionType::LockedCandidates1,
                        positions,
                        digit,
                        eliminations,
                    ));
                }
            }
        }
    }

    // Type 2: Claiming — candidate in a row/col confined to one block
    for line_type in 0..2u8 {
        let lines = if line_type == 0 { &ROWS } else { &COLS };
        for line in lines {
            for digit in 1..=9u8 {
                let mut positions = Vec::new();
                for &cell in line {
                    if board.is_candidate(cell as usize, digit) {
                        positions.push(cell as usize);
                    }
                }
                if positions.len() < 2 {
                    continue;
                }

                let block = get_block(positions[0]);
                if positions.iter().all(|&p| get_block(p) == block) {
                    let mut eliminations = Vec::new();
                    for &cell in &BLOCKS[block] {
                        let ci = cell as usize;
                        if !positions.contains(&ci) && board.is_candidate(ci, digit) {
                            eliminations.push((ci, digit));
                        }
                    }
                    if !eliminations.is_empty() {
                        return Some(SolutionStep::elimination(
                            SolutionType::LockedCandidates2,
                            positions,
                            digit,
                            eliminations,
                        ));
                    }
                }
            }
        }
    }
    None
}

/// Locked Pair/Triple: naked subset at block-line intersection.
fn find_locked_subset(board: &Board, size: usize) -> Option<SolutionStep> {
    let step_type = if size == 2 {
        SolutionType::LockedPair
    } else {
        SolutionType::LockedTriple
    };

    for block in 0..9 {
        let br = (block / 3) * 3;
        let bc = (block % 3) * 3;

        for line_idx in 0..6 {
            let (line, intersection): (&[u8; 9], [usize; 3]) = if line_idx < 3 {
                let row = br + line_idx;
                (
                    &ROWS[row],
                    [row * 9 + bc, row * 9 + bc + 1, row * 9 + bc + 2],
                )
            } else {
                let col = bc + (line_idx - 3);
                (
                    &COLS[col],
                    [br * 9 + col, (br + 1) * 9 + col, (br + 2) * 9 + col],
                )
            };

            // Unsolved cells in intersection
            let unsolved: Vec<usize> = intersection
                .iter()
                .copied()
                .filter(|&c| board.candidates[c] != 0)
                .collect();
            if unsolved.len() < size {
                continue;
            }

            // Try all combinations
            let combos = combinations(&unsolved, size);
            for combo in combos {
                let mut combined_mask = 0u16;
                for &cell in &combo {
                    combined_mask |= board.candidates[cell];
                }
                if ANZ_VALUES[combined_mask as usize] as usize != size {
                    continue;
                }

                // Eliminations from rest of block AND rest of line
                let mut eliminations = Vec::new();
                let mut seen = std::collections::HashSet::new();
                for &cell in &BLOCKS[block] {
                    let ci = cell as usize;
                    if !combo.contains(&ci) && board.candidates[ci] != 0 {
                        let pv = &POSSIBLE_VALUES[combined_mask as usize];
                        for j in 0..pv.count as usize {
                            let d = pv.digits[j];
                            if board.is_candidate(ci, d) && seen.insert((ci, d)) {
                                eliminations.push((ci, d));
                            }
                        }
                    }
                }
                for &cell in line {
                    let ci = cell as usize;
                    if !combo.contains(&ci) && board.candidates[ci] != 0 {
                        let pv = &POSSIBLE_VALUES[combined_mask as usize];
                        for j in 0..pv.count as usize {
                            let d = pv.digits[j];
                            if board.is_candidate(ci, d) && seen.insert((ci, d)) {
                                eliminations.push((ci, d));
                            }
                        }
                    }
                }

                if !eliminations.is_empty() {
                    return Some(SolutionStep::elimination(step_type, combo, 0, eliminations));
                }
            }
        }
    }
    None
}

/// Naked Pair/Triple/Quad: N cells in a unit with exactly N candidates combined.
fn find_naked_subset(board: &Board, size: usize) -> Option<SolutionStep> {
    let step_type = match size {
        2 => SolutionType::NakedPair,
        3 => SolutionType::NakedTriple,
        _ => SolutionType::NakedQuadruple,
    };

    for unit in &ALL_UNITS {
        let unsolved: Vec<usize> = unit
            .iter()
            .map(|&c| c as usize)
            .filter(|&c| board.candidates[c] != 0)
            .collect();
        if unsolved.len() < size {
            continue;
        }

        let combos = combinations(&unsolved, size);
        for combo in combos {
            let mut combined_mask = 0u16;
            for &cell in &combo {
                combined_mask |= board.candidates[cell];
            }
            if ANZ_VALUES[combined_mask as usize] as usize != size {
                continue;
            }

            let mut eliminations = Vec::new();
            for &cell in unit {
                let ci = cell as usize;
                if board.candidates[ci] != 0 && !combo.contains(&ci) {
                    let pv = &POSSIBLE_VALUES[combined_mask as usize];
                    for j in 0..pv.count as usize {
                        let d = pv.digits[j];
                        if board.is_candidate(ci, d) {
                            eliminations.push((ci, d));
                        }
                    }
                }
            }

            if !eliminations.is_empty() {
                return Some(SolutionStep::elimination(step_type, combo, 0, eliminations));
            }
        }
    }
    None
}

/// Hidden Pair/Triple/Quad: N candidates in a unit confined to N cells.
fn find_hidden_subset(board: &Board, size: usize) -> Option<SolutionStep> {
    let step_type = match size {
        2 => SolutionType::HiddenPair,
        3 => SolutionType::HiddenTriple,
        _ => SolutionType::HiddenQuadruple,
    };

    for unit in &ALL_UNITS {
        // Find digits appearing 2..=size times
        let mut available_digits = Vec::new();
        for digit in 1..=9u8 {
            let mut count = 0u8;
            for &cell in unit {
                if board.is_candidate(cell as usize, digit) {
                    count += 1;
                }
            }
            if count >= 2 && count <= size as u8 {
                available_digits.push(digit);
            }
        }
        if available_digits.len() < size {
            continue;
        }

        let digit_combos = combinations(&available_digits, size);
        for digit_combo in digit_combos {
            // Find cells containing any of these digits
            let mut cell_set = Vec::new();
            for &cell in unit {
                let ci = cell as usize;
                for &d in &digit_combo {
                    if board.is_candidate(ci, d) {
                        if !cell_set.contains(&ci) {
                            cell_set.push(ci);
                        }
                        break;
                    }
                }
            }
            if cell_set.len() != size {
                continue;
            }

            // Check for eliminations
            let mut eliminations = Vec::new();
            for &cell in &cell_set {
                for digit in 1..=9u8 {
                    if !digit_combo.contains(&digit) && board.is_candidate(cell, digit) {
                        eliminations.push((cell, digit));
                    }
                }
            }

            if !eliminations.is_empty() {
                return Some(SolutionStep::elimination(
                    step_type,
                    cell_set,
                    0,
                    eliminations,
                ));
            }
        }
    }
    None
}

