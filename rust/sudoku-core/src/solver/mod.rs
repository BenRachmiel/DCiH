pub mod simple;
pub mod fish;
pub mod wings;
pub mod single_digit;
pub mod coloring;
pub mod brute_force;

use crate::board::Board;
use crate::step::SolutionStep;
use crate::types::{Difficulty, SolutionType};

/// Trait for all solver strategies.
pub trait Solver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep>;
}

/// Dispatches to specialized solvers in order of increasing difficulty.
pub struct StepFinder {
    solvers: Vec<Box<dyn Solver>>,
}

impl StepFinder {
    pub fn new() -> Self {
        Self {
            solvers: vec![
                Box::new(simple::SimpleSolver),
                Box::new(fish::FishSolver),
                Box::new(single_digit::SingleDigitPatternSolver),
                Box::new(wings::WingSolver),
                Box::new(coloring::ColoringSolver),
                Box::new(brute_force::BruteForceSolver),
            ],
        }
    }

    pub fn find_next_step(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
    ) -> Option<SolutionStep> {
        for solver in &self.solvers {
            let steps = solver.find_steps(board);
            let step = if let Some(max_diff) = max_difficulty {
                steps.into_iter().find(|s| s.step_type.difficulty() <= max_diff)
            } else {
                steps.into_iter().next()
            };
            if step.is_some() {
                return step;
            }
        }
        None
    }
}

impl Default for StepFinder {
    fn default() -> Self {
        Self::new()
    }
}

/// Orchestrator: solves a puzzle step-by-step, accumulating score.
pub struct SolverOrchestrator {
    step_finder: StepFinder,
}

#[derive(Debug, Clone, serde::Serialize)]
pub struct SolveResult {
    pub steps: Vec<SolutionStep>,
    pub score: i32,
    pub difficulty: Difficulty,
    pub solved: bool,
}

impl SolverOrchestrator {
    pub fn new() -> Self {
        Self {
            step_finder: StepFinder::new(),
        }
    }

    /// Solve the given board step-by-step, grading difficulty.
    /// The board is cloned internally — the original is not modified.
    pub fn solve(&self, board: &Board, max_difficulty: Option<Difficulty>) -> SolveResult {
        let mut work = board.clone();
        let mut steps = Vec::new();
        let mut total_score = 0i32;

        while !work.is_solved() {
            let Some(step) = self.step_finder.find_next_step(&work, max_difficulty) else {
                break;
            };

            total_score += step.step_type.score();

            // Apply the step
            if step.step_type.is_single() || step.step_type == SolutionType::BruteForce {
                work.set_cell(step.cell_index as usize, step.value, false);
                work.set_all_exposed_singles();
            } else {
                for &(cell_index, candidate) in &step.candidates_removed {
                    work.set_candidate(cell_index, candidate, false);
                }
                work.set_all_exposed_singles();
            }

            steps.push(step);
        }

        SolveResult {
            steps,
            score: total_score,
            difficulty: Difficulty::from_score(total_score),
            solved: work.is_solved(),
        }
    }
}

impl Default for SolverOrchestrator {
    fn default() -> Self {
        Self::new()
    }
}
