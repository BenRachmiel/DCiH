pub mod simple;
pub mod fish;
pub mod wings;
pub mod single_digit;
pub mod coloring;
pub mod brute_force;

use crate::board::Board;
use crate::step::SolutionStep;
use crate::types::{Difficulty, SolutionType};

pub fn combinations<T: Clone>(items: &[T], size: usize) -> Vec<Vec<T>> {
    let mut result = Vec::new();
    let mut current = Vec::with_capacity(size);
    fn recurse<T: Clone>(
        items: &[T],
        start: usize,
        size: usize,
        current: &mut Vec<T>,
        result: &mut Vec<Vec<T>>,
    ) {
        if current.len() == size {
            result.push(current.clone());
            return;
        }
        for i in start..items.len() {
            current.push(items[i].clone());
            recurse(items, i + 1, size, current, result);
            current.pop();
        }
    }
    recurse(items, 0, size, &mut current, &mut result);
    result
}

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

    /// Check if a specific technique pattern exists on the board.
    /// Runs all solvers and returns the first step matching the target type.
    /// Does NOT respect priority order — just checks if the pattern exists.
    pub fn find_technique(&self, board: &Board, target: SolutionType) -> Option<SolutionStep> {
        for solver in &self.solvers {
            let steps = solver.find_steps(board);
            if let Some(step) = steps.into_iter().find(|s| s.step_type == target) {
                return Some(step);
            }
        }
        None
    }

    pub fn find_next_step(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
    ) -> Option<SolutionStep> {
        self.find_next_step_inner(board, max_difficulty, None)
    }

    /// Like `find_next_step`, but prefers `preferred` when it exists at the
    /// same difficulty tier as the step that would normally be chosen.
    pub fn find_next_step_preferring(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
        preferred: SolutionType,
    ) -> Option<SolutionStep> {
        self.find_next_step_inner(board, max_difficulty, Some(preferred))
    }

    fn find_next_step_inner(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
        preferred: Option<SolutionType>,
    ) -> Option<SolutionStep> {
        for solver in &self.solvers {
            let steps = solver.find_steps(board);
            if steps.is_empty() {
                continue;
            }
            let eligible: Vec<SolutionStep> = if let Some(max_diff) = max_difficulty {
                steps.into_iter().filter(|s| s.step_type.difficulty() <= max_diff).collect()
            } else {
                steps
            };
            if eligible.is_empty() {
                continue;
            }
            // If we have a preferred type and it's among eligible steps, pick it
            if let Some(pref) = preferred {
                if let Some(step) = eligible.iter().find(|s| s.step_type == pref) {
                    return Some(step.clone());
                }
            }
            return eligible.into_iter().next();
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
        self.solve_inner(board, max_difficulty, None)
    }

    /// Like `solve`, but when multiple steps of equal difficulty are available,
    /// prefer the given technique. Used by example generation to ensure rare
    /// techniques appear in the solve path.
    pub fn solve_preferring(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
        preferred: SolutionType,
    ) -> SolveResult {
        self.solve_inner(board, max_difficulty, Some(preferred))
    }

    fn solve_inner(
        &self,
        board: &Board,
        max_difficulty: Option<Difficulty>,
        preferred: Option<SolutionType>,
    ) -> SolveResult {
        let mut work = board.clone();
        let mut steps = Vec::new();
        let mut total_score = 0i32;

        while !work.is_solved() {
            let step = if let Some(pref) = preferred {
                self.step_finder.find_next_step_preferring(&work, max_difficulty, pref)
            } else {
                self.step_finder.find_next_step(&work, max_difficulty)
            };
            let Some(step) = step else {
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
