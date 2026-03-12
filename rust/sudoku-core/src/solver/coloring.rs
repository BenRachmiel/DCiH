use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::Solver;

pub struct ColoringSolver;

impl Solver for ColoringSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        let mut steps = Vec::new();
        for digit in 1..=9u8 {
            find_simple_colors(board, digit, &mut steps);
        }
        steps
    }
}

fn find_simple_colors(board: &Board, digit: u8, results: &mut Vec<SolutionStep>) {
    // Build strong links (conjugate pairs)
    let mut strong_links: Vec<Vec<usize>> = vec![Vec::new(); 81];
    for unit in &ALL_UNITS {
        let mut cells_with_digit = Vec::new();
        for &cell in unit {
            if board.is_candidate(cell as usize, digit) {
                cells_with_digit.push(cell as usize);
            }
        }
        if cells_with_digit.len() == 2 {
            let a = cells_with_digit[0];
            let b = cells_with_digit[1];
            if !strong_links[a].contains(&b) {
                strong_links[a].push(b);
            }
            if !strong_links[b].contains(&a) {
                strong_links[b].push(a);
            }
        }
    }

    // BFS to color connected components
    let mut color = [-1i8; 81];
    let mut visited = [false; 81];

    for start in 0..81 {
        if visited[start] || strong_links[start].is_empty() {
            continue;
        }

        // BFS color this component
        let mut color_a = Vec::new();
        let mut color_b = Vec::new();
        let mut queue = std::collections::VecDeque::new();
        color[start] = 0;
        visited[start] = true;
        queue.push_back(start);

        while let Some(cell) = queue.pop_front() {
            if color[cell] == 0 {
                color_a.push(cell);
            } else {
                color_b.push(cell);
            }
            for &neighbor in &strong_links[cell] {
                if !visited[neighbor] {
                    visited[neighbor] = true;
                    color[neighbor] = 1 - color[cell];
                    queue.push_back(neighbor);
                }
            }
        }

        if color_a.is_empty() || color_b.is_empty() {
            continue;
        }

        // Check Wrap: if same color cells see each other, that color is false
        for (false_color_cells, true_color_cells, step_type) in [
            (&color_a, &color_b, SolutionType::SimpleColorsWrap),
            (&color_b, &color_a, SolutionType::SimpleColorsWrap),
        ] {
            let mut has_conflict = false;
            for i in 0..false_color_cells.len() {
                for j in (i + 1)..false_color_cells.len() {
                    if BUDDIES_ARRAY[false_color_cells[i]]
                        .contains(&(false_color_cells[j] as u8))
                    {
                        has_conflict = true;
                        break;
                    }
                }
                if has_conflict {
                    break;
                }
            }
            if has_conflict {
                let elims: Vec<(usize, u8)> =
                    false_color_cells.iter().map(|&c| (c, digit)).collect();
                if !elims.is_empty() {
                    let mut indices = true_color_cells.clone();
                    indices.extend(false_color_cells);
                    results.push(SolutionStep::elimination(
                        step_type,
                        indices,
                        digit,
                        elims,
                    ));
                    return; // Wrap found for this digit — skip Trap check
                }
            }
        }

        // Check Trap: uncolored cell sees both colors -> eliminate
        for cell in 0..81 {
            if color[cell] != -1 || !board.is_candidate(cell, digit) {
                continue;
            }
            let sees_a = color_a
                .iter()
                .any(|&c| BUDDIES_ARRAY[cell].contains(&(c as u8)));
            let sees_b = color_b
                .iter()
                .any(|&c| BUDDIES_ARRAY[cell].contains(&(c as u8)));
            if sees_a && sees_b {
                let mut indices = color_a.clone();
                indices.extend(&color_b);
                results.push(SolutionStep::elimination(
                    SolutionType::SimpleColorsTrap,
                    indices,
                    digit,
                    vec![(cell, digit)],
                ));
                return; // One result per digit is enough
            }
        }
    }
}
