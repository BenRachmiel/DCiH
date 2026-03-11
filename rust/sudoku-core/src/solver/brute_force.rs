use crate::board::Board;
use crate::step::SolutionStep;
use crate::types::SolutionType;

use super::Solver;

pub struct BruteForceSolver;

impl Solver for BruteForceSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        if !board.solution_set {
            return vec![];
        }
        for i in 0..81 {
            if board.values[i] == 0 {
                return vec![SolutionStep::single(
                    SolutionType::BruteForce,
                    i,
                    board.solution[i],
                )];
            }
        }
        vec![]
    }
}
