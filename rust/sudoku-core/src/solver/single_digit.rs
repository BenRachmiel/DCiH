use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::Solver;

pub struct SingleDigitPatternSolver;

impl Solver for SingleDigitPatternSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        let mut steps = Vec::new();
        for digit in 1..=9u8 {
            let links = find_strong_links(board, digit);
            if let Some(s) = find_empty_rectangle(board, digit, &links) {
                steps.push(s);
            }
            if let Some(s) = find_two_link_pattern(board, digit, &links) {
                steps.push(s);
            }
        }
        steps
    }
}

/// A strong link: unit index + two cell indices.
struct StrongLink {
    unit: usize,
    cell_a: usize,
    cell_b: usize,
}

fn find_strong_links(board: &Board, digit: u8) -> Vec<StrongLink> {
    let mut links = Vec::new();
    for (unit_idx, unit) in ALL_UNITS.iter().enumerate() {
        let pair = find_two_candidates(board, unit, digit);
        if let Some((a, b)) = pair {
            links.push(StrongLink {
                unit: unit_idx,
                cell_a: a,
                cell_b: b,
            });
        }
    }
    links
}

fn find_two_candidates(board: &Board, unit: &[u8; 9], digit: u8) -> Option<(usize, usize)> {
    let mut first = None;
    let mut second = None;
    let mut count = 0;
    for &cell in unit {
        if board.is_candidate(cell as usize, digit) {
            count += 1;
            if count > 2 {
                return None;
            }
            if first.is_none() {
                first = Some(cell as usize);
            } else {
                second = Some(cell as usize);
            }
        }
    }
    if count == 2 {
        Some((first.unwrap(), second.unwrap()))
    } else {
        None
    }
}

fn is_buddy(a: usize, b: usize) -> bool {
    BUDDIES_ARRAY[a].contains(&(b as u8))
}

fn find_common_eliminations(board: &Board, digit: u8, a: usize, b: usize) -> Vec<(usize, u8)> {
    let mut elims = Vec::new();
    for &buddy in &BUDDIES_ARRAY[a] {
        let bi = buddy as usize;
        if bi != b && is_buddy(bi, b) && board.is_candidate(bi, digit) {
            elims.push((bi, digit));
        }
    }
    elims
}

fn classify_pattern(unit1: usize, unit2: usize) -> SolutionType {
    let is_box1 = unit1 >= 18;
    let is_box2 = unit2 >= 18;
    if is_box1 || is_box2 {
        SolutionType::TurbotFish
    } else {
        // Both are lines
        let same_type = (unit1 < 9) == (unit2 < 9);
        if same_type {
            SolutionType::Skyscraper
        } else {
            SolutionType::TwoStringKite
        }
    }
}

fn find_two_link_pattern(
    board: &Board,
    digit: u8,
    links: &[StrongLink],
) -> Option<SolutionStep> {
    for (i, link1) in links.iter().enumerate() {
        for link2 in &links[i + 1..] {
            // Try all 4 endpoint combinations for weak link
            for &(end, start1, conn, far) in &[
                (link1.cell_a, link1.cell_b, link2.cell_a, link2.cell_b),
                (link1.cell_a, link1.cell_b, link2.cell_b, link2.cell_a),
                (link1.cell_b, link1.cell_a, link2.cell_a, link2.cell_b),
                (link1.cell_b, link1.cell_a, link2.cell_b, link2.cell_a),
            ] {
                // 4 distinct cells
                let mut cells = [start1, end, conn, far];
                cells.sort();
                if cells[0] == cells[1] || cells[1] == cells[2] || cells[2] == cells[3] {
                    continue;
                }

                // end and conn must be buddies (weak link)
                if !is_buddy(end, conn) {
                    continue;
                }

                // Eliminations: cells seeing both start1 and far
                let elims = find_common_eliminations(board, digit, start1, far);
                if !elims.is_empty() {
                    let step_type = classify_pattern(link1.unit, link2.unit);
                    return Some(SolutionStep::elimination(
                        step_type,
                        vec![start1, end, conn, far],
                        digit,
                        elims,
                    ));
                }
            }
        }
    }
    None
}

fn find_empty_rectangle(
    board: &Board,
    digit: u8,
    links: &[StrongLink],
) -> Option<SolutionStep> {
    for block in 0..9 {
        // Find cells with candidate digit in this block
        let mut box_cells = Vec::new();
        for &cell in &BLOCKS[block] {
            if board.is_candidate(cell as usize, digit) {
                box_cells.push(cell as usize);
            }
        }
        if box_cells.len() < 2 {
            continue;
        }

        let br = (block / 3) * 3;
        let bc = (block % 3) * 3;

        // Check for ER pattern: all cells aligned to one row AND one column (L-shape)
        for er_row in br..br + 3 {
            for er_col in bc..bc + 3 {
                // Check: all box_cells are in er_row or er_col
                if !box_cells
                    .iter()
                    .all(|&c| c / 9 == er_row || c % 9 == er_col)
                {
                    continue;
                }

                // Case 1: strong link in row outside box
                for link in links {
                    if link.unit >= 18 {
                        continue; // skip box links
                    }
                    if link.unit < 9 {
                        // Row link
                        let link_row = link.unit;
                        if link_row < br || link_row >= br + 3 {
                            // Outside the box
                            continue;
                        }
                        // skip — row link must be outside the box rows... wait, no.
                        // Actually we need the link OUTSIDE the block
                    }
                }

                // Case 1: Strong link in a column outside the box, one end in row er_row
                for link in links {
                    if link.unit < 9 || link.unit >= 18 {
                        continue; // only column links
                    }
                    let link_col = link.unit - 9;
                    if link_col >= bc && link_col < bc + 3 {
                        continue; // column must be outside the block columns
                    }

                    // One end must be in er_row
                    let (in_row_cell, other_cell) =
                        if link.cell_a / 9 == er_row {
                            (link.cell_a, link.cell_b)
                        } else if link.cell_b / 9 == er_row {
                            (link.cell_b, link.cell_a)
                        } else {
                            continue;
                        };

                    // Elimination target: intersection of other_cell's row and er_col
                    let target = (other_cell / 9) * 9 + er_col;
                    if target != in_row_cell
                        && target != other_cell
                        && board.is_candidate(target, digit)
                    {
                        let mut indices = vec![in_row_cell, other_cell];
                        for &c in &box_cells {
                            if !indices.contains(&c) {
                                indices.push(c);
                            }
                        }
                        return Some(SolutionStep::elimination(
                            SolutionType::EmptyRectangle,
                            indices,
                            digit,
                            vec![(target, digit)],
                        ));
                    }
                }

                // Case 2: Strong link in a row outside the box, one end in col er_col
                for link in links {
                    if link.unit >= 9 {
                        continue; // only row links
                    }
                    let link_row = link.unit;
                    if link_row >= br && link_row < br + 3 {
                        continue; // row must be outside the block rows
                    }

                    let (in_col_cell, other_cell) =
                        if link.cell_a % 9 == er_col {
                            (link.cell_a, link.cell_b)
                        } else if link.cell_b % 9 == er_col {
                            (link.cell_b, link.cell_a)
                        } else {
                            continue;
                        };

                    let target = er_row * 9 + (other_cell % 9);
                    if target != in_col_cell
                        && target != other_cell
                        && board.is_candidate(target, digit)
                    {
                        let mut indices = vec![in_col_cell, other_cell];
                        for &c in &box_cells {
                            if !indices.contains(&c) {
                                indices.push(c);
                            }
                        }
                        return Some(SolutionStep::elimination(
                            SolutionType::EmptyRectangle,
                            indices,
                            digit,
                            vec![(target, digit)],
                        ));
                    }
                }
            }
        }
    }
    None
}
