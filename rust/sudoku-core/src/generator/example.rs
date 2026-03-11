use serde::Serialize;

use crate::board::Board;
use crate::generator::highlighter::build_highlights;
use crate::generator::Generator;
use crate::solver::SolverOrchestrator;
use crate::step::CandidateHighlight;
use crate::types::{Difficulty, SolutionType};

#[derive(Debug, Clone, Serialize)]
pub struct BoardExampleResult {
    pub puzzle: String,
    #[serde(rename = "candidateMasks")]
    pub candidate_masks: Vec<u16>,
    pub highlights: Vec<CandidateHighlight>,
}

/// Generate a board example showcasing a specific solving technique.
///
/// Generates puzzles, solves them step-by-step, and captures a board snapshot
/// when the target technique appears. Returns None if no example found.
pub fn generate_example(
    target_type: SolutionType,
    max_attempts: i32,
) -> Option<BoardExampleResult> {
    let target_diff = target_type.difficulty();
    let mut generator = Generator::new();
    let orchestrator = SolverOrchestrator::new();
    let threshold = completeness_threshold(target_type);

    for _ in 0..max_attempts {
        let puzzle = generator.generate(target_diff, 50);
        let solve_result = orchestrator.solve(
            &{
                let mut b = Board::new();
                b.load_from_string(&puzzle.puzzle);
                b
            },
            None,
        );
        if !solve_result.solved {
            continue;
        }

        // Replay steps on a fresh board to capture accurate state
        let mut replay = Board::new();
        replay.load_from_string(&puzzle.puzzle);

        for step in &solve_result.steps {
            if step.step_type == target_type {
                let filled = 81 - replay.unsolved as usize;
                if filled <= threshold {
                    let highlights = build_highlights(&replay, step);
                    let puzzle_str = replay.to_string_compact();
                    let candidate_masks = replay.candidates.to_vec();
                    return Some(BoardExampleResult {
                        puzzle: puzzle_str,
                        candidate_masks,
                        highlights,
                    });
                }
            }

            // Advance replay board
            if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
                replay.set_cell(step.cell_index as usize, step.value, false);
                replay.set_all_exposed_singles();
            } else {
                for &(cell, cand) in &step.candidates_removed {
                    replay.set_candidate(cell, cand, false);
                }
                replay.set_all_exposed_singles();
            }
        }
    }

    None
}

/// Maximum filled cells for an educationally useful example.
fn completeness_threshold(t: SolutionType) -> usize {
    if t == SolutionType::FullHouse {
        return 81;
    }
    match t.difficulty() {
        Difficulty::Easy => 60,
        Difficulty::Medium => 65,
        Difficulty::Hard => 70,
        _ => 75, // Unfair, Extreme
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_naked_single_example() {
        let result = generate_example(SolutionType::NakedSingle, 100);
        assert!(result.is_some(), "Should find a Naked Single example");
        let ex = result.unwrap();
        assert_eq!(ex.puzzle.len(), 81);
        assert_eq!(ex.candidate_masks.len(), 81);
        assert!(!ex.highlights.is_empty());
    }

    #[test]
    fn test_generate_hidden_single_example() {
        let result = generate_example(SolutionType::HiddenSingle, 100);
        assert!(result.is_some(), "Should find a Hidden Single example");
    }

    #[test]
    fn test_generate_locked_candidates_example() {
        let result = generate_example(SolutionType::LockedCandidates1, 200);
        assert!(result.is_some(), "Should find a Locked Candidates example");
    }
}
