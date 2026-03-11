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

    /// Concrete hint: technique name + relevant cells/digits.
    pub fn describe_concrete(&self) -> String {
        if self.step_type.is_single() {
            let r = self.cell_index as usize / 9 + 1;
            let c = self.cell_index as usize % 9 + 1;
            return format!("{}: {} in r{}c{}", self.step_type.display_name(), self.value, r, c);
        }
        let mut digits: Vec<u8> = self
            .candidates_removed
            .iter()
            .map(|&(_, d)| d)
            .collect();
        digits.sort();
        digits.dedup();
        if self.value != 0 {
            format!("{}: digit {}", self.step_type.display_name(), self.value)
        } else if !digits.is_empty() {
            let digit_str: Vec<String> = digits.iter().map(|d| d.to_string()).collect();
            format!("{}: digits {}", self.step_type.display_name(), digit_str.join(","))
        } else {
            self.step_type.display_name().to_string()
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
