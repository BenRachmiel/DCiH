use crate::board::Board;
use crate::step::{CandidateHighlight, SolutionStep};
use crate::tables::*;
use crate::types::{HighlightRole, SolutionType};

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
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::LockedPair | SolutionType::LockedTriple => {
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                let pv = &POSSIBLE_VALUES[board.candidates[idx] as usize];
                for i in 0..pv.count as usize {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: pv.digits[i],
                        role: HighlightRole::Defining,
                    });
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::NakedPair
        | SolutionType::NakedTriple
        | SolutionType::NakedQuadruple => {
            let mut highlights = Vec::new();
            for &idx in &step.indices {
                let pv = &POSSIBLE_VALUES[board.candidates[idx] as usize];
                for i in 0..pv.count as usize {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: pv.digits[i],
                        role: HighlightRole::Defining,
                    });
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::HiddenPair
        | SolutionType::HiddenTriple
        | SolutionType::HiddenQuadruple => {
            let mut highlights = Vec::new();
            // Defining: the hidden digits in the subset cells
            let kept_digits: Vec<u8> = step
                .candidates_removed
                .iter()
                .map(|&(_, d)| d)
                .collect();
            for &idx in &step.indices {
                for d in 1..=9u8 {
                    if board.is_candidate(idx, d) && !kept_digits.contains(&d) {
                        highlights.push(CandidateHighlight {
                            cell_index: idx,
                            value: d,
                            role: HighlightRole::Defining,
                        });
                    }
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
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
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::Skyscraper | SolutionType::TwoStringKite | SolutionType::TurbotFish => {
            let mut highlights = Vec::new();
            if step.indices.len() == 4 {
                // [start, end, conn, far] — start/far are DEFINING, end/conn are SECONDARY
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
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::EmptyRectangle => {
            let mut highlights = Vec::new();
            // First 2 indices are the strong link endpoints (DEFINING)
            // Rest are ER box cells (SECONDARY)
            if step.indices.len() >= 2 {
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[0],
                    value: step.value,
                    role: HighlightRole::Defining,
                });
                highlights.push(CandidateHighlight {
                    cell_index: step.indices[1],
                    value: step.value,
                    role: HighlightRole::Defining,
                });
                for &idx in &step.indices[2..] {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: step.value,
                        role: HighlightRole::Secondary,
                    });
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::XyWing => {
            let mut highlights = Vec::new();
            if step.indices.len() == 3 {
                // [pivot, pincer1, pincer2]
                let pivot = step.indices[0];
                let pv = &POSSIBLE_VALUES[board.candidates[pivot] as usize];
                for i in 0..pv.count as usize {
                    highlights.push(CandidateHighlight {
                        cell_index: pivot,
                        value: pv.digits[i],
                        role: HighlightRole::Secondary,
                    });
                }
                for &pincer in &step.indices[1..] {
                    let ppv = &POSSIBLE_VALUES[board.candidates[pincer] as usize];
                    for i in 0..ppv.count as usize {
                        highlights.push(CandidateHighlight {
                            cell_index: pincer,
                            value: ppv.digits[i],
                            role: HighlightRole::Defining,
                        });
                    }
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::XyzWing => {
            let mut highlights = Vec::new();
            if step.indices.len() == 3 {
                let pivot = step.indices[0];
                let ppv = &POSSIBLE_VALUES[board.candidates[pivot] as usize];
                for i in 0..ppv.count as usize {
                    highlights.push(CandidateHighlight {
                        cell_index: pivot,
                        value: ppv.digits[i],
                        role: HighlightRole::Secondary,
                    });
                }
                for &pincer in &step.indices[1..] {
                    let ppv = &POSSIBLE_VALUES[board.candidates[pincer] as usize];
                    for i in 0..ppv.count as usize {
                        highlights.push(CandidateHighlight {
                            cell_index: pincer,
                            value: ppv.digits[i],
                            role: HighlightRole::Defining,
                        });
                    }
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::WWing => {
            let mut highlights = Vec::new();
            if step.indices.len() >= 2 {
                for &idx in &step.indices[..2] {
                    let ppv = &POSSIBLE_VALUES[board.candidates[idx] as usize];
                    for i in 0..ppv.count as usize {
                        highlights.push(CandidateHighlight {
                            cell_index: idx,
                            value: ppv.digits[i],
                            role: HighlightRole::Defining,
                        });
                    }
                }
                for &idx in &step.indices[2..] {
                    highlights.push(CandidateHighlight {
                        cell_index: idx,
                        value: step.value,
                        role: HighlightRole::Secondary,
                    });
                }
            }
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }

        SolutionType::SimpleColorsWrap => {
            let mut highlights = Vec::new();
            // First portion are true-color cells (COLOR_A), rest + elims are false (COLOR_B)
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

        SolutionType::SimpleColorsTrap => {
            let mut highlights = Vec::new();
            // All indices split into two color groups
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
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
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
            for &(cell, digit) in &step.candidates_removed {
                highlights.push(CandidateHighlight {
                    cell_index: cell,
                    value: digit,
                    role: HighlightRole::Elimination,
                });
            }
            highlights
        }
    }
}
