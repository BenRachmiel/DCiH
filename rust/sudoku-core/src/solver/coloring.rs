use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::Solver;

pub struct ColoringSolver;

impl Solver for ColoringSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        let mut steps = Vec::new();
        for digit in 1..=9u8 {
            let clusters = build_clusters(board, digit);
            find_simple_colors(board, digit, &clusters, &mut steps);
            if steps.is_empty() {
                find_multi_colors(board, digit, &clusters, &mut steps);
            }
        }
        steps
    }
}

/// A cluster is a connected component of the strong-link graph, split into two color groups.
struct Cluster {
    color_a: Vec<usize>,
    color_b: Vec<usize>,
}

/// Build the strong-link (conjugate pair) graph and BFS-color each connected component.
fn build_clusters(board: &Board, digit: u8) -> Vec<Cluster> {
    let mut strong_links: Vec<Vec<usize>> = vec![Vec::new(); 81];
    for unit in &ALL_UNITS {
        let mut cells_with_digit = Vec::new();
        for &cell in unit {
            if board.is_candidate(cell as usize, digit) {
                cells_with_digit.push(cell as usize);
            }
        }
        if cells_with_digit.len() == 2 {
            let a = cells_with_digit[0];
            let b = cells_with_digit[1];
            if !strong_links[a].contains(&b) {
                strong_links[a].push(b);
            }
            if !strong_links[b].contains(&a) {
                strong_links[b].push(a);
            }
        }
    }

    let mut visited = [false; 81];
    let mut clusters = Vec::new();

    for start in 0..81 {
        if visited[start] || strong_links[start].is_empty() {
            continue;
        }

        let mut color_a = Vec::new();
        let mut color_b = Vec::new();
        let mut queue = std::collections::VecDeque::new();
        let mut cell_color = [0u8; 81];
        cell_color[start] = 1; // 1 = color A, 2 = color B
        visited[start] = true;
        queue.push_back(start);

        while let Some(cell) = queue.pop_front() {
            if cell_color[cell] == 1 {
                color_a.push(cell);
            } else {
                color_b.push(cell);
            }
            for &neighbor in &strong_links[cell] {
                if !visited[neighbor] {
                    visited[neighbor] = true;
                    cell_color[neighbor] = if cell_color[cell] == 1 { 2 } else { 1 };
                    queue.push_back(neighbor);
                }
            }
        }

        if !color_a.is_empty() && !color_b.is_empty() {
            clusters.push(Cluster { color_a, color_b });
        }
    }

    clusters
}

fn find_simple_colors(
    board: &Board,
    digit: u8,
    clusters: &[Cluster],
    results: &mut Vec<SolutionStep>,
) {
    // Build per-cell color map for trap checks
    let mut color = [-1i8; 81];
    for (i, cluster) in clusters.iter().enumerate() {
        for &c in &cluster.color_a {
            color[c] = (i * 2) as i8;
        }
        for &c in &cluster.color_b {
            color[c] = (i * 2 + 1) as i8;
        }
    }

    for cluster in clusters {
        // Check Wrap: if same-color cells see each other, that color is false
        for (false_color_cells, true_color_cells) in [
            (&cluster.color_a, &cluster.color_b),
            (&cluster.color_b, &cluster.color_a),
        ] {
            let mut has_conflict = false;
            for i in 0..false_color_cells.len() {
                for j in (i + 1)..false_color_cells.len() {
                    if BUDDIES_ARRAY[false_color_cells[i]]
                        .contains(&(false_color_cells[j] as u8))
                    {
                        has_conflict = true;
                        break;
                    }
                }
                if has_conflict {
                    break;
                }
            }
            if has_conflict {
                let elims: Vec<(usize, u8)> =
                    false_color_cells.iter().map(|&c| (c, digit)).collect();
                if !elims.is_empty() {
                    let mut indices = true_color_cells.clone();
                    indices.extend(false_color_cells);
                    results.push(SolutionStep::elimination(
                        SolutionType::SimpleColorsWrap,
                        indices,
                        digit,
                        elims,
                    ));
                    return;
                }
            }
        }

        // Check Trap: uncolored cell sees both colors of this cluster -> eliminate
        for cell in 0..81 {
            if color[cell] != -1 || !board.is_candidate(cell, digit) {
                continue;
            }
            let sees_a = cluster
                .color_a
                .iter()
                .any(|&c| BUDDIES_ARRAY[cell].contains(&(c as u8)));
            let sees_b = cluster
                .color_b
                .iter()
                .any(|&c| BUDDIES_ARRAY[cell].contains(&(c as u8)));
            if sees_a && sees_b {
                let mut indices = cluster.color_a.clone();
                indices.extend(&cluster.color_b);
                results.push(SolutionStep::elimination(
                    SolutionType::SimpleColorsTrap,
                    indices,
                    digit,
                    vec![(cell, digit)],
                ));
                return;
            }
        }
    }
}

/// Returns true if any cell in `cells_a` can see any cell in `cells_b`.
fn any_sees_any(cells_a: &[usize], cells_b: &[usize]) -> bool {
    for &a in cells_a {
        for &b in cells_b {
            if BUDDIES_ARRAY[a].contains(&(b as u8)) {
                return true;
            }
        }
    }
    false
}

/// Returns true if the given cell can see at least one cell in `cells`.
fn cell_sees_any(cell: usize, cells: &[usize]) -> bool {
    cells.iter().any(|&c| BUDDIES_ARRAY[cell].contains(&(c as u8)))
}

fn find_multi_colors(
    board: &Board,
    digit: u8,
    clusters: &[Cluster],
    results: &mut Vec<SolutionStep>,
) {
    if clusters.len() < 2 {
        return;
    }

    // Collect all colored cells for "uncolored" checks
    let mut is_colored = [false; 81];
    for cluster in clusters {
        for &c in &cluster.color_a {
            is_colored[c] = true;
        }
        for &c in &cluster.color_b {
            is_colored[c] = true;
        }
    }

    // Multi Colors 1: If one color of cluster A sees BOTH colors of cluster B,
    // then that color of A is false — eliminate all its cells as candidates.
    for (i, ca) in clusters.iter().enumerate() {
        for cb in clusters.iter().skip(i + 1) {
            // Check each color of ca against both colors of cb
            for (false_cells, true_cells) in [
                (&ca.color_a, &ca.color_b),
                (&ca.color_b, &ca.color_a),
            ] {
                if any_sees_any(false_cells, &cb.color_a)
                    && any_sees_any(false_cells, &cb.color_b)
                {
                    let elims: Vec<(usize, u8)> =
                        false_cells.iter().map(|&c| (c, digit)).collect();
                    if !elims.is_empty() {
                        let mut indices = true_cells.clone();
                        indices.extend(false_cells);
                        indices.extend(&cb.color_a);
                        indices.extend(&cb.color_b);
                        results.push(SolutionStep::elimination(
                            SolutionType::MultiColors1,
                            indices,
                            digit,
                            elims,
                        ));
                        return;
                    }
                }
            }
            // Same check but with cb's colors against ca
            for (false_cells, true_cells) in [
                (&cb.color_a, &cb.color_b),
                (&cb.color_b, &cb.color_a),
            ] {
                if any_sees_any(false_cells, &ca.color_a)
                    && any_sees_any(false_cells, &ca.color_b)
                {
                    let elims: Vec<(usize, u8)> =
                        false_cells.iter().map(|&c| (c, digit)).collect();
                    if !elims.is_empty() {
                        let mut indices = true_cells.clone();
                        indices.extend(false_cells);
                        indices.extend(&ca.color_a);
                        indices.extend(&ca.color_b);
                        results.push(SolutionStep::elimination(
                            SolutionType::MultiColors1,
                            indices,
                            digit,
                            elims,
                        ));
                        return;
                    }
                }
            }
        }
    }

    // Multi Colors 2: If color A1 sees color B1 (from different clusters),
    // then A2 or B2 must be true. Eliminate any uncolored candidate cell
    // that sees both A2 and B2.
    for (i, ca) in clusters.iter().enumerate() {
        for cb in clusters.iter().skip(i + 1) {
            // Try all four cross-cluster pairings: (a_a, b_a), (a_a, b_b), (a_b, b_a), (a_b, b_b)
            let pairings: [(&[usize], &[usize], &[usize], &[usize]); 4] = [
                (&ca.color_a, &ca.color_b, &cb.color_a, &cb.color_b),
                (&ca.color_a, &ca.color_b, &cb.color_b, &cb.color_a),
                (&ca.color_b, &ca.color_a, &cb.color_a, &cb.color_b),
                (&ca.color_b, &ca.color_a, &cb.color_b, &cb.color_a),
            ];
            for &(seeing_a, opposite_a, seeing_b, opposite_b) in &pairings {
                if !any_sees_any(seeing_a, seeing_b) {
                    continue;
                }
                // seeing_a sees seeing_b => they can't both be true
                // => opposite_a or opposite_b must be true
                // => eliminate uncolored cells that see both opposite_a and opposite_b
                for cell in 0..81 {
                    if is_colored[cell] || !board.is_candidate(cell, digit) {
                        continue;
                    }
                    if cell_sees_any(cell, opposite_a) && cell_sees_any(cell, opposite_b) {
                        let mut indices = ca.color_a.to_vec();
                        indices.extend(&ca.color_b);
                        indices.extend(&cb.color_a);
                        indices.extend(&cb.color_b);
                        results.push(SolutionStep::elimination(
                            SolutionType::MultiColors2,
                            indices,
                            digit,
                            vec![(cell, digit)],
                        ));
                        return;
                    }
                }
            }
        }
    }
}
