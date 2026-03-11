use jni::objects::{JByteArray, JClass, JShortArray, JString};
use jni::sys::{jint, jshortArray};
use jni::JNIEnv;

use crate::board::{self, Board};
use crate::generator::example::generate_example;
use crate::generator::highlighter::build_highlights;
use crate::generator::Generator;
use crate::solver::{SolverOrchestrator, StepFinder};
use crate::step::SolutionStep;
use crate::types::{Difficulty, SolutionType};

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

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeGenerateExample<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    type_ordinal: jint,
    max_attempts: jint,
) -> JString<'a> {
    let target = SolutionType::from_ordinal(type_ordinal as u8);
    let result = match generate_example(target, max_attempts) {
        Some(ex) => serde_json::to_string(&ex).unwrap_or_default(),
        None => String::new(),
    };
    env.new_string(&result).unwrap()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeComputeAllCandidates<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    values: JByteArray<'a>,
) -> jshortArray {
    let values_vec = env.convert_byte_array(&values).unwrap_or_default();
    if values_vec.len() < 81 {
        return env.new_short_array(0).unwrap().into_raw();
    }
    let mut vals = [0u8; 81];
    vals.copy_from_slice(&values_vec[..81]);

    let cands = board::compute_all_candidates(&vals);
    let shorts: Vec<i16> = cands.iter().map(|&v| v as i16).collect();

    let arr = env.new_short_array(81).unwrap();
    env.set_short_array_region(&arr, 0, &shorts).unwrap();
    arr.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeFindPencilMarkErrors<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    values: JByteArray<'a>,
    cand_masks: JShortArray<'a>,
    solution: JByteArray<'a>,
) -> JString<'a> {
    let values_vec = env.convert_byte_array(&values).unwrap_or_default();
    let solution_vec = env.convert_byte_array(&solution).unwrap_or_default();
    let cand_len = env.get_array_length(&cand_masks).unwrap_or(0) as usize;
    let mut cand_buf = vec![0i16; cand_len];
    env.get_short_array_region(&cand_masks, 0, &mut cand_buf).ok();

    if values_vec.len() < 81 || cand_buf.len() < 81 || solution_vec.len() < 81 {
        return env.new_string("").unwrap();
    }

    let mut vals = [0u8; 81];
    let mut cands = [0u16; 81];
    let mut sol = [0u8; 81];
    vals.copy_from_slice(&values_vec[..81]);
    for i in 0..81 {
        cands[i] = cand_buf[i] as u16;
    }
    sol.copy_from_slice(&solution_vec[..81]);

    match board::find_pencil_mark_errors(&vals, &cands, &sol) {
        Some((to_remove, to_add)) => {
            #[derive(serde::Serialize)]
            struct PencilMarkErrors {
                #[serde(rename = "toRemove")]
                to_remove: Vec<(usize, u8)>,
                #[serde(rename = "toAdd")]
                to_add: Vec<(usize, u8)>,
            }
            let result = PencilMarkErrors { to_remove, to_add };
            let json = serde_json::to_string(&result).unwrap_or_default();
            env.new_string(&json).unwrap()
        }
        None => env.new_string("").unwrap(),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_sudoku_engine_RustEngine_nativeFindSingleForCell(
    mut env: JNIEnv,
    _class: JClass,
    values: JByteArray,
    cand_masks: JShortArray,
    cell_index: jint,
) -> jint {
    let values_vec = env.convert_byte_array(&values).unwrap_or_default();
    let cand_len = env.get_array_length(&cand_masks).unwrap_or(0) as usize;
    let mut cand_buf = vec![0i16; cand_len];
    env.get_short_array_region(&cand_masks, 0, &mut cand_buf).ok();

    if values_vec.len() < 81 || cand_buf.len() < 81 {
        return 0;
    }

    let mut vals = [0u8; 81];
    let mut cands = [0u16; 81];
    vals.copy_from_slice(&values_vec[..81]);
    for i in 0..81 {
        cands[i] = cand_buf[i] as u16;
    }

    board::find_single_for_cell(&vals, &cands, cell_index as usize) as jint
}
