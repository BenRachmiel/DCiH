use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::{Solver, combinations};

pub struct FishSolver;

impl Solver for FishSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        for size in 2..=4 {
            if let Some(s) = find_fish(board, size, true) {
                return vec![s];
            }
            if let Some(s) = find_fish(board, size, false) {
                return vec![s];
            }
        }
        vec![]
    }
}

fn find_fish(board: &Board, size: usize, use_rows: bool) -> Option<SolutionStep> {
    let step_type = match size {
        2 => SolutionType::XWing,
        3 => SolutionType::Swordfish,
        _ => SolutionType::Jellyfish,
    };

    let base_lines = if use_rows { &ROWS } else { &COLS };
    let cover_lines = if use_rows { &COLS } else { &ROWS };

    for digit in 1..=9u8 {
        // Find base lines with 2..=size candidates
        let mut candidate_lines: Vec<(usize, Vec<usize>)> = Vec::new();
        for (line_idx, line) in base_lines.iter().enumerate() {
            let positions: Vec<usize> = line
                .iter()
                .map(|&c| c as usize)
                .filter(|&c| board.is_candidate(c, digit))
                .collect();
            if positions.len() >= 2 && positions.len() <= size {
                candidate_lines.push((line_idx, positions));
            }
        }
        if candidate_lines.len() < size {
            continue;
        }

        // Try all size-combinations
        let combos = combinations(&candidate_lines, size);
        for combo in combos {
            // Collect all fish cells and determine cover set
            let mut fish_cells = Vec::new();
            let mut cover_set = Vec::new();
            for &(_, ref positions) in &combo {
                for &pos in positions {
                    fish_cells.push(pos);
                    let cover_idx = if use_rows {
                        pos % 9
                    } else {
                        pos / 9
                    };
                    if !cover_set.contains(&cover_idx) {
                        cover_set.push(cover_idx);
                    }
                }
            }

            if cover_set.len() != size {
                continue;
            }

            // Find eliminations: cells in cover lines but not in fish
            let mut eliminations = Vec::new();
            for &cover_idx in &cover_set {
                for &cell in &cover_lines[cover_idx] {
                    let ci = cell as usize;
                    if !fish_cells.contains(&ci) && board.is_candidate(ci, digit) {
                        eliminations.push((ci, digit));
                    }
                }
            }

            if !eliminations.is_empty() {
                return Some(SolutionStep::elimination(
                    step_type,
                    fish_cells,
                    digit,
                    eliminations,
                ));
            }
        }
    }
    None
}

