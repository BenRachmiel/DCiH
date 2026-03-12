use crate::board::Board;
use crate::step::{CandidateHighlight, SolutionStep};
use crate::tables::*;
use crate::types::{HighlightRole, SolutionType};

fn add_eliminations(highlights: &mut Vec<CandidateHighlight>, step: &SolutionStep) {
    for &(cell, digit) in &step.candidates_removed {
        highlights.push(CandidateHighlight {
            cell_index: cell,
            value: digit,
            role: HighlightRole::Elimination,
        });
    }
}

fn add_cell_candidates(
    highlights: &mut Vec<CandidateHighlight>,
    board: &Board,
    cell: usize,
    role: HighlightRole,
) {
    let pv = &POSSIBLE_VALUES[board.candidates[cell] as usize];
    for i in 0..pv.count as usize {
        highlights.push(CandidateHighlight {
            cell_index: cell,
            value: pv.digits[i],
            role,
        });
    }
}

/// Convert a SolutionStep into visual highlights.
pub fn build_highlights(board: &Board, step: &SolutionStep) -> Vec<CandidateHighlight> {
    match step.step_type {
        SolutionType::FullHouse | SolutionType::NakedSingle | SolutionType::HiddenSingle => {
            vec![CandidateHighlight {
                cell_index: step.cell_index as usize,
                value: step.value,
                role: HighlightRole::Defining,
            }]
        }

        SolutionType::LockedCandidates1 | SolutionType::LockedCandidates2 => {
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role: HighlightRole::Defining,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::LockedPair
        | SolutionType::LockedTriple
        | SolutionType::NakedPair
        | SolutionType::NakedTriple
        | SolutionType::NakedQuadruple => {
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                add_cell_candidates(&mut highlights, board, idx, HighlightRole::Defining);
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::HiddenPair
        | SolutionType::HiddenTriple
        | SolutionType::HiddenQuadruple => {
            let mut highlights = Vec::new();
            let removed_digits: Vec<u8> = step
                .candidates_removed
                .iter()
                .map(|&(_, d)| d)
                .collect();
            // The hidden digits (those NOT being removed) are the core pattern
            for &idx in &step.indices {
                for d in 1..=9u8 {
                    if board.is_candidate(idx, d) && !removed_digits.contains(&d) {
                        highlights.push(CandidateHighlight {
                            cell_index: idx,
                            value: d,
                            role: HighlightRole::Defining,
                        });
                    }
                }
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::XWing | SolutionType::Swordfish | SolutionType::Jellyfish => {
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role: HighlightRole::Defining,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::Skyscraper | SolutionType::TwoStringKite | SolutionType::TurbotFish => {
            let mut highlights = Vec::new();
            if step.indices.len() == 4 {
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[0],
                    value: step.value,
                    role: HighlightRole::Defining,
                });
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[1],
                    value: step.value,
                    role: HighlightRole::Secondary,
                });
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[2],
                    value: step.value,
                    role: HighlightRole::Secondary,
                });
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[3],
                    value: step.value,
                    role: HighlightRole::Defining,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::EmptyRectangle => {
            let mut highlights = Vec::new();
            if step.indices.len() >= 2 {
                // indices[0..2] = strong link endpoints, indices[2..] = ER box cells
                // The box cells are the core pattern; strong link provides the logic
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[0],
                    value: step.value,
                    role: HighlightRole::Secondary,
                });
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[1],
                    value: step.value,
                    role: HighlightRole::Secondary,
                });
                for &idx in &step.indices[2..] {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: step.value,
                        role: HighlightRole::Defining,
                    });
                }
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::XyWing | SolutionType::XyzWing => {
            let mut highlights = Vec::new();
            if step.indices.len() == 3 {
                add_cell_candidates(&mut highlights, board, step.indices[0], HighlightRole::Secondary);
                for &pincer in &step.indices[1..] {
                    add_cell_candidates(&mut highlights, board, pincer, HighlightRole::Defining);
                }
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::RemotePair => {
            let mut highlights = Vec::new();
            for (i, &idx) in step.indices.iter().enumerate() {
                let role = if i % 2 == 0 {
                    HighlightRole::ColorA
                } else {
                    HighlightRole::ColorB
                };
                add_cell_candidates(&mut highlights, board, idx, role);
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::WWing => {
            let mut highlights = Vec::new();
            if step.indices.len() >= 2 {
                for &idx in &step.indices[..2] {
                    add_cell_candidates(&mut highlights, board, idx, HighlightRole::Defining);
                }
                for &idx in &step.indices[2..] {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: step.value,
                        role: HighlightRole::Secondary,
                    });
                }
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::SimpleColorsWrap => {
            let mut highlights = Vec::new();
            let elim_cells: Vec<usize> = step.candidates_removed.iter().map(|&(c, _)| c).collect();
            for &idx in &step.indices {
                let role = if elim_cells.contains(&idx) {
                    HighlightRole::ColorB
                } else {
                    HighlightRole::ColorA
                };
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role,
                });
            }
            for &(cell, digit) in &step.candidates_removed {
                if !step.indices.contains(&cell) {
                    highlights.push(CandidateHighlight {
                        cell_index: cell,
                        value: digit,
                        role: HighlightRole::Elimination,
                    });
                }
            }
            highlights
        }

        SolutionType::MultiColors1 | SolutionType::MultiColors2 => {
            // Indices: cells from two clusters. Use ColorA/ColorB for alternating clusters.
            // The elimination cells get Elimination role.
            let mut highlights = Vec::new();
            let elim_cells: Vec<usize> = step.candidates_removed.iter().map(|&(c, _)| c).collect();
            for &idx in &step.indices {
                let role = if elim_cells.contains(&idx) {
                    HighlightRole::ColorB
                } else {
                    HighlightRole::ColorA
                };
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        SolutionType::SimpleColorsTrap => {
            let mut highlights = Vec::new();
            let half = step.indices.len() / 2;
            for (i, &idx) in step.indices.iter().enumerate() {
                let role = if i < half {
                    HighlightRole::ColorA
                } else {
                    HighlightRole::ColorB
                };
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }

        _ => {
            // Generic fallback
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                highlights.push(CandidateHighlight {
                    cell_index: idx,
                    value: step.value,
                    role: HighlightRole::Defining,
                });
            }
            add_eliminations(&mut highlights, step);
            highlights
        }
    }
}
