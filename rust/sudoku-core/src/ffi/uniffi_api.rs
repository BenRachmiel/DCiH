use crate::board::{self, Board};
use crate::generator::example::generate_example as gen_example;
use crate::generator::Generator;
use crate::solver::StepFinder;
use crate::step::SolutionStep;
use crate::types::{Difficulty, HighlightRole, SolutionType};

// ─── FFI-specific record types (UniFFI-compatible) ──────────────────────────

#[derive(uniffi::Record)]
pub struct FfiCandidateRemoval {
    pub cell: u32,
    pub digit: u8,
}

#[derive(uniffi::Record)]
pub struct FfiSolutionStep {
    pub step_type: SolutionType,
    pub cell_index: i32,
    pub value: u8,
    pub indices: Vec<u32>,
    pub candidates_removed: Vec<FfiCandidateRemoval>,
}

#[derive(uniffi::Record)]
pub struct FfiCandidateHighlight {
    pub cell_index: u32,
    pub value: u8,
    pub role: HighlightRole,
}

#[derive(uniffi::Record)]
pub struct FfiGenerateResult {
    pub puzzle: String,
    pub solution: Vec<u8>,
    pub difficulty: Difficulty,
    pub score: i32,
}

#[derive(uniffi::Record)]
pub struct FfiBoardExample {
    pub puzzle: String,
    pub candidate_masks: Vec<u16>,
    pub highlights: Vec<FfiCandidateHighlight>,
}

#[derive(uniffi::Record)]
pub struct FfiPencilMarkErrors {
    pub to_remove: Vec<FfiCandidateRemoval>,
    pub to_add: Vec<FfiCandidateRemoval>,
}

// ─── Conversions ────────────────────────────────────────────────────────────

impl From<&SolutionStep> for FfiSolutionStep {
    fn from(s: &SolutionStep) -> Self {
        Self {
            step_type: s.step_type,
            cell_index: s.cell_index,
            value: s.value,
            indices: s.indices.iter().map(|&i| i as u32).collect(),
            candidates_removed: s
                .candidates_removed
                .iter()
                .map(|&(c, d)| FfiCandidateRemoval {
                    cell: c as u32,
                    digit: d,
                })
                .collect(),
        }
    }
}

impl From<&FfiSolutionStep> for SolutionStep {
    fn from(s: &FfiSolutionStep) -> Self {
        Self {
            step_type: s.step_type,
            cell_index: s.cell_index,
            value: s.value,
            indices: s.indices.iter().map(|&i| i as usize).collect(),
            candidates_removed: s
                .candidates_removed
                .iter()
                .map(|r| (r.cell as usize, r.digit))
                .collect(),
        }
    }
}

fn board_from_vecs(values: &[u8], cand_masks: &[u16], solution: &[u8]) -> Board {
    let mut board = Board::new();
    for i in 0..81 {
        board.values[i] = values[i];
        board.candidates[i] = cand_masks[i];
        board.solution[i] = solution[i];
        if values[i] != 0 {
            board.fixed[i] = true;
        }
    }
    board.solution_set = solution.iter().any(|&v| v != 0);
    board.rebuild_internal_data();
    board
}

// ─── Exported functions ─────────────────────────────────────────────────────

#[uniffi::export]
fn generate_puzzle(difficulty: Difficulty, max_retries: i32) -> FfiGenerateResult {
    let mut generator = Generator::new();
    let p = generator.generate(difficulty, max_retries);
    FfiGenerateResult {
        puzzle: p.puzzle,
        solution: p.solution.to_vec(),
        difficulty: p.difficulty,
        score: p.score,
    }
}

#[uniffi::export]
fn find_next_step(
    values: Vec<u8>,
    cand_masks: Vec<u16>,
    solution: Vec<u8>,
    max_difficulty: Option<Difficulty>,
) -> Option<FfiSolutionStep> {
    let board = board_from_vecs(&values, &cand_masks, &solution);
    let finder = StepFinder::new();
    finder
        .find_next_step(&board, max_difficulty)
        .as_ref()
        .map(FfiSolutionStep::from)
}

#[uniffi::export]
fn build_step_highlights(
    values: Vec<u8>,
    cand_masks: Vec<u16>,
    solution: Vec<u8>,
    step: FfiSolutionStep,
) -> Vec<FfiCandidateHighlight> {
    let board = board_from_vecs(&values, &cand_masks, &solution);
    let internal_step = SolutionStep::from(&step);
    crate::generator::highlighter::build_highlights(&board, &internal_step)
        .into_iter()
        .map(|h| FfiCandidateHighlight {
            cell_index: h.cell_index as u32,
            value: h.value,
            role: h.role,
        })
        .collect()
}

#[uniffi::export]
fn count_solutions(values: Vec<u8>, max_count: u32) -> u32 {
    let mut vals = [0u8; 81];
    vals.copy_from_slice(&values[..81]);
    let mut generator = Generator::new();
    generator.count_solutions_values(&vals, max_count as usize) as u32
}

#[uniffi::export]
fn generate_board_example(
    type_ordinal: u8,
    max_attempts: i32,
) -> Option<FfiBoardExample> {
    let target = SolutionType::from_ordinal(type_ordinal);
    gen_example(target, max_attempts).map(|ex| FfiBoardExample {
        puzzle: ex.puzzle,
        candidate_masks: ex.candidate_masks,
        highlights: ex
            .highlights
            .into_iter()
            .map(|h| FfiCandidateHighlight {
                cell_index: h.cell_index as u32,
                value: h.value,
                role: h.role,
            })
            .collect(),
    })
}

#[uniffi::export]
fn compute_all_candidates(values: Vec<u8>) -> Vec<u16> {
    let mut vals = [0u8; 81];
    vals.copy_from_slice(&values[..81]);
    board::compute_all_candidates(&vals).to_vec()
}

#[uniffi::export]
fn find_pencil_mark_errors(
    values: Vec<u8>,
    cand_masks: Vec<u16>,
    solution: Vec<u8>,
) -> Option<FfiPencilMarkErrors> {
    let mut vals = [0u8; 81];
    let mut cands = [0u16; 81];
    let mut sol = [0u8; 81];
    vals.copy_from_slice(&values[..81]);
    for i in 0..81 {
        cands[i] = cand_masks[i];
    }
    sol.copy_from_slice(&solution[..81]);

    board::find_pencil_mark_errors(&vals, &cands, &sol).map(|(to_remove, to_add)| {
        FfiPencilMarkErrors {
            to_remove: to_remove
                .into_iter()
                .map(|(c, d)| FfiCandidateRemoval {
                    cell: c as u32,
                    digit: d,
                })
                .collect(),
            to_add: to_add
                .into_iter()
                .map(|(c, d)| FfiCandidateRemoval {
                    cell: c as u32,
                    digit: d,
                })
                .collect(),
        }
    })
}

#[uniffi::export]
fn find_single_for_cell(values: Vec<u8>, cand_masks: Vec<u16>, cell_index: u32) -> u8 {
    let mut vals = [0u8; 81];
    let mut cands = [0u16; 81];
    vals.copy_from_slice(&values[..81]);
    for i in 0..81 {
        cands[i] = cand_masks[i];
    }
    board::find_single_for_cell(&vals, &cands, cell_index as usize)
}
