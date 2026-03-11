use jni::objects::{JByteArray, JClass, JShortArray, JString};
use jni::sys::jint;
use jni::JNIEnv;

use crate::board::Board;
use crate::generator::highlighter::build_highlights;
use crate::generator::Generator;
use crate::solver::{SolverOrchestrator, StepFinder};
use crate::step::SolutionStep;
use crate::types::Difficulty;

/// Reconstruct a Board from flat arrays passed over FFI.
fn board_from_arrays(values: &[u8], cand_masks: &[u16], solution: &[u8]) -> Board {
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

fn step_to_json(step: &SolutionStep) -> String {
    serde_json::to_string(step).unwrap_or_default()
}

fn extract_board_args(
    env: &mut JNIEnv,
    values: &JByteArray,
    cand_masks: &JShortArray,
    solution: &JByteArray,
) -> Option<(Vec<u8>, Vec<u16>, Vec<u8>)> {
    let values_vec = env.convert_byte_array(values).unwrap_or_default();
    let solution_vec = env.convert_byte_array(solution).unwrap_or_default();

    let cand_len = env.get_array_length(cand_masks).unwrap_or(0) as usize;
    let mut cand_buf = vec![0i16; cand_len];
    env.get_short_array_region(cand_masks, 0, &mut cand_buf).ok();
    let cand_u16: Vec<u16> = cand_buf.iter().map(|&v| v as u16).collect();

    if values_vec.len() < 81 || cand_u16.len() < 81 || solution_vec.len() < 81 {
        None
    } else {
        Some((values_vec, cand_u16, solution_vec))
    }
}

// ─── JNI Exports ────────────────────────────────────────────────────────────
// Kotlin class: sudoku.engine.RustEngine

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeGenerate<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    difficulty: jint,
    max_retries: jint,
) -> JString<'a> {
    let diff = Difficulty::from_ordinal(difficulty as u8);
    let mut generator = Generator::new();
    let puzzle = generator.generate(diff, max_retries);
    let json = serde_json::to_string(&puzzle).unwrap_or_default();
    env.new_string(&json).unwrap()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeFindNextStep<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    values: JByteArray<'a>,
    cand_masks: JShortArray<'a>,
    solution: JByteArray<'a>,
    max_diff: jint,
) -> JString<'a> {
    let Some((values_vec, cand_u16, solution_vec)) =
        extract_board_args(&mut env, &values, &cand_masks, &solution)
    else {
        return env.new_string("").unwrap();
    };

    let board = board_from_arrays(&values_vec, &cand_u16, &solution_vec);
    let max_difficulty = if max_diff < 0 {
        None
    } else {
        Some(Difficulty::from_ordinal(max_diff as u8))
    };

    let finder = StepFinder::new();
    let result = match finder.find_next_step(&board, max_difficulty) {
        Some(step) => step_to_json(&step),
        None => String::new(),
    };
    env.new_string(&result).unwrap()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeBuildHighlights<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    values: JByteArray<'a>,
    cand_masks: JShortArray<'a>,
    solution: JByteArray<'a>,
    step_json: JString<'a>,
) -> JString<'a> {
    let Some((values_vec, cand_u16, solution_vec)) =
        extract_board_args(&mut env, &values, &cand_masks, &solution)
    else {
        return env.new_string("[]").unwrap();
    };

    let step_str: String = env.get_string(&step_json).map(|s| s.into()).unwrap_or_default();

    let board = board_from_arrays(&values_vec, &cand_u16, &solution_vec);
    let step: SolutionStep = match serde_json::from_str(&step_str) {
        Ok(s) => s,
        Err(_) => return env.new_string("[]").unwrap(),
    };

    let highlights = build_highlights(&board, &step);
    let json = serde_json::to_string(&highlights).unwrap_or_else(|_| "[]".to_string());
    env.new_string(&json).unwrap()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeSolve<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    values: JByteArray<'a>,
    cand_masks: JShortArray<'a>,
    solution: JByteArray<'a>,
    max_diff: jint,
) -> JString<'a> {
    let Some((values_vec, cand_u16, solution_vec)) =
        extract_board_args(&mut env, &values, &cand_masks, &solution)
    else {
        return env.new_string("{}").unwrap();
    };

    let board = board_from_arrays(&values_vec, &cand_u16, &solution_vec);
    let max_difficulty = if max_diff < 0 {
        None
    } else {
        Some(Difficulty::from_ordinal(max_diff as u8))
    };

    let solver = SolverOrchestrator::new();
    let result = solver.solve(&board, max_difficulty);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    env.new_string(&json).unwrap()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeCountSolutions(
    mut env: JNIEnv,
    _class: JClass,
    values: JByteArray,
    max_count: jint,
) -> jint {
    let values_vec = env.convert_byte_array(&values).unwrap_or_default();
    if values_vec.len() < 81 {
        return 0;
    }
    let mut vals = [0u8; 81];
    vals.copy_from_slice(&values_vec[..81]);

    let mut generator = Generator::new();
    generator.count_solutions_values(&vals, max_count as usize) as jint
}
