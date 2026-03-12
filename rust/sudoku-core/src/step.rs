use serde::{Deserialize, Serialize};

use crate::types::{HighlightRole, SolutionType};

/// A single solving step found by a solver.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SolutionStep {
    #[serde(rename = "type")]
    pub step_type: SolutionType,
    /// Cell index for single placements (-1 if not applicable).
    #[serde(rename = "cellIndex", default)]
    pub cell_index: i32,
    /// The digit value (0 if not applicable).
    #[serde(default)]
    pub value: u8,
    /// Cells involved in the pattern.
    #[serde(default)]
    pub indices: Vec<usize>,
    /// (cell_index, candidate) pairs to eliminate.
    #[serde(rename = "candidatesRemoved", default)]
    pub candidates_removed: Vec<(usize, u8)>,
}

impl SolutionStep {
    /// Create a step for placing a single digit.
    pub fn single(step_type: SolutionType, cell_index: usize, value: u8) -> Self {
        Self {
            step_type,
            cell_index: cell_index as i32,
            value,
            indices: Vec::new(),
            candidates_removed: Vec::new(),
        }
    }

    /// Create a step for an elimination technique.
    pub fn elimination(
        step_type: SolutionType,
        indices: Vec<usize>,
        value: u8,
        candidates_removed: Vec<(usize, u8)>,
    ) -> Self {
        Self {
            step_type,
            cell_index: -1,
            value,
            indices,
            candidates_removed,
        }
    }

    /// Vague hint: just the technique name.
    pub fn describe_vague(&self) -> &'static str {
        self.step_type.display_name()
    }

    /// Concrete hint: technique name + relevant cells/digits/location.
    pub fn describe_concrete(&self) -> String {
        fn cell_ref(idx: usize) -> String {
            format!("r{}c{}", idx / 9 + 1, idx % 9 + 1)
        }
        fn box_of(idx: usize) -> usize {
            (idx / 9 / 3) * 3 + (idx % 9 / 3) + 1
        }

        let name = self.step_type.display_name();

        match self.step_type {
            SolutionType::FullHouse | SolutionType::NakedSingle | SolutionType::HiddenSingle => {
                let idx = self.cell_index as usize;
                format!("{}: {} in {}", name, self.value, cell_ref(idx))
            }

            SolutionType::LockedCandidates1 | SolutionType::LockedCandidates2 => {
                let bx = if !self.indices.is_empty() { box_of(self.indices[0]) } else { 0 };
                format!("{}: digit {} in box {}", name, self.value, bx)
            }

            SolutionType::LockedPair | SolutionType::LockedTriple => {
                let cells: Vec<String> = self.indices.iter().map(|&i| cell_ref(i)).collect();
                format!("{}: {}", name, cells.join(","))
            }

            SolutionType::NakedPair | SolutionType::NakedTriple | SolutionType::NakedQuadruple => {
                let mut digits: Vec<u8> = self.candidates_removed.iter().map(|&(_, d)| d).collect();
                digits.sort();
                digits.dedup();
                let cells: Vec<String> = self.indices.iter().map(|&i| cell_ref(i)).collect();
                let ds: Vec<String> = digits.iter().map(|d| d.to_string()).collect();
                format!("{}: {{{}}} in {}", name, ds.join(","), cells.join(","))
            }

            SolutionType::HiddenPair | SolutionType::HiddenTriple | SolutionType::HiddenQuadruple => {
                let cells: Vec<String> = self.indices.iter().map(|&i| cell_ref(i)).collect();
                format!("{}: in {}", name, cells.join(","))
            }

            SolutionType::XWing | SolutionType::Swordfish | SolutionType::Jellyfish => {
                let mut rows: Vec<usize> = self.indices.iter().map(|&i| i / 9 + 1).collect();
                let mut cols: Vec<usize> = self.indices.iter().map(|&i| i % 9 + 1).collect();
                rows.sort();
                rows.dedup();
                cols.sort();
                cols.dedup();
                let rs: Vec<String> = rows.iter().map(|r| r.to_string()).collect();
                let cs: Vec<String> = cols.iter().map(|c| c.to_string()).collect();
                format!("{}: digit {} in r{}/c{}", name, self.value, rs.join(","), cs.join(","))
            }

            SolutionType::Skyscraper | SolutionType::TwoStringKite | SolutionType::TurbotFish => {
                if self.indices.len() == 4 {
                    format!("{}: digit {}, ends {},{}", name, self.value,
                        cell_ref(self.indices[0]), cell_ref(self.indices[3]))
                } else {
                    format!("{}: digit {}", name, self.value)
                }
            }

            SolutionType::EmptyRectangle => {
                if self.indices.len() > 2 {
                    let bx = box_of(self.indices[2]);
                    format!("{}: digit {} in box {}", name, self.value, bx)
                } else {
                    format!("{}: digit {}", name, self.value)
                }
            }

            SolutionType::XyWing | SolutionType::XyzWing => {
                if !self.indices.is_empty() {
                    format!("{}: pivot {}", name, cell_ref(self.indices[0]))
                } else {
                    name.to_string()
                }
            }

            SolutionType::WWing => {
                if self.indices.len() >= 2 {
                    format!("{}: {},{}", name, cell_ref(self.indices[0]), cell_ref(self.indices[1]))
                } else {
                    name.to_string()
                }
            }

            SolutionType::RemotePair => {
                let mut digits: Vec<u8> = self.candidates_removed.iter().map(|&(_, d)| d).collect();
                digits.sort();
                digits.dedup();
                let ds: Vec<String> = digits.iter().map(|d| d.to_string()).collect();
                format!("{}: {{{}}} chain of {}", name, ds.join(","), self.indices.len())
            }

            SolutionType::SimpleColorsTrap => {
                if let Some(&(cell, _)) = self.candidates_removed.first() {
                    format!("{}: digit {}, {} sees both colors", name, self.value, cell_ref(cell))
                } else {
                    format!("{}: digit {}", name, self.value)
                }
            }

            SolutionType::SimpleColorsWrap => {
                format!("{}: digit {}, color contradicts itself", name, self.value)
            }

            SolutionType::MultiColors1 | SolutionType::MultiColors2 => {
                format!("{}: digit {}, {} eliminations", name, self.value, self.candidates_removed.len())
            }

            SolutionType::BruteForce => {
                if self.cell_index >= 0 {
                    format!("{}: {} in {}", name, self.value, cell_ref(self.cell_index as usize))
                } else {
                    name.to_string()
                }
            }

            _ => name.to_string(),
        }
    }
}

/// A single highlighted candidate digit within a cell.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CandidateHighlight {
    #[serde(rename = "cellIndex")]
    pub cell_index: usize,
    pub value: u8,
    pub role: HighlightRole,
}
