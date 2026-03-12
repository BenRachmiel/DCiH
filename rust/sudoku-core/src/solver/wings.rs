use crate::board::Board;
use crate::step::SolutionStep;
use crate::tables::*;
use crate::types::SolutionType;

use super::Solver;

pub struct WingSolver;

impl Solver for WingSolver {
    fn find_steps(&self, board: &Board) -> Vec<SolutionStep> {
        let mut steps = Vec::new();
        if let Some(s) = find_xy_wing(board) {
            steps.push(s);
        }
        if let Some(s) = find_xyz_wing(board) {
            steps.push(s);
        }
        if let Some(s) = find_w_wing(board) {
            steps.push(s);
        }
        if let Some(s) = find_remote_pair(board) {
            steps.push(s);
        }
        steps
    }
}

/// XY-Wing: pivot (bivalue {a,b}) + 2 pincers ({a,c}, {b,c}). Eliminates c.
fn find_xy_wing(board: &Board) -> Option<SolutionStep> {
    // Find all bivalue cells
    let bivalues: Vec<usize> = (0..81)
        .filter(|&i| board.candidates[i] != 0 && ANZ_VALUES[board.candidates[i] as usize] == 2)
        .collect();

    for &pivot in &bivalues {
        let pivot_mask = board.candidates[pivot];
        let pv = &POSSIBLE_VALUES[pivot_mask as usize];
        let a = pv.digits[0];
        let b = pv.digits[1];

        // Find buddies of pivot that are bivalue and share exactly one candidate
        let mut pincers_a = Vec::new(); // cells with {a, c} where c != b
        let mut pincers_b = Vec::new(); // cells with {b, c} where c != a

        for &buddy in &BUDDIES_ARRAY[pivot] {
            let bi = buddy as usize;
            if board.candidates[bi] == 0 || ANZ_VALUES[board.candidates[bi] as usize] != 2 {
                continue;
            }
            let has_a = board.is_candidate(bi, a);
            let has_b = board.is_candidate(bi, b);
            if has_a && !has_b {
                pincers_a.push(bi);
            } else if has_b && !has_a {
                pincers_b.push(bi);
            }
        }

        for &pa in &pincers_a {
            let pa_mask = board.candidates[pa];
            // c is the other digit in pa (not a)
            // The other candidate of pa that isn't a
            let pa_other = {
                let pv2 = &POSSIBLE_VALUES[pa_mask as usize];
                if pv2.digits[0] == a {
                    pv2.digits[1]
                } else {
                    pv2.digits[0]
                }
            };

            for &pb in &pincers_b {
                // pb must have {b, c} where c == pa_other
                let pb_mask = board.candidates[pb];
                let pb_pv = &POSSIBLE_VALUES[pb_mask as usize];
                let pb_other = if pb_pv.digits[0] == b {
                    pb_pv.digits[1]
                } else {
                    pb_pv.digits[0]
                };

                if pb_other != pa_other {
                    continue;
                }
                let c = pa_other;

                // pa and pb must NOT be buddies of each other (optional in some impls, but standard)
                // Actually in standard XY-Wing they don't need to be non-buddies.
                // Eliminations: cells seeing both pa and pb that have candidate c
                let mut elims = Vec::new();
                for &buddy_a in &BUDDIES_ARRAY[pa] {
                    let bai = buddy_a as usize;
                    if bai != pivot
                        && bai != pb
                        && BUDDIES_ARRAY[pb].contains(&buddy_a)
                        && board.is_candidate(bai, c)
                    {
                        elims.push((bai, c));
                    }
                }

                if !elims.is_empty() {
                    return Some(SolutionStep::elimination(
                        SolutionType::XyWing,
                        vec![pivot, pa, pb],
                        c,
                        elims,
                    ));
                }
            }
        }
    }
    None
}

/// XYZ-Wing: pivot (trivalue {a,b,c}) + 2 bivalue buddies.
fn find_xyz_wing(board: &Board) -> Option<SolutionStep> {
    for pivot in 0..81 {
        if board.candidates[pivot] == 0 || ANZ_VALUES[board.candidates[pivot] as usize] != 3 {
            continue;
        }
        let pivot_mask = board.candidates[pivot];
        let pv = &POSSIBLE_VALUES[pivot_mask as usize];
        let (_a, _b, _c) = (pv.digits[0], pv.digits[1], pv.digits[2]);

        // Find bivalue buddies sharing exactly 2 candidates with pivot
        let mut bivalue_buddies = Vec::new();
        for &buddy in &BUDDIES_ARRAY[pivot] {
            let bi = buddy as usize;
            if board.candidates[bi] == 0 || ANZ_VALUES[board.candidates[bi] as usize] != 2 {
                continue;
            }
            // Must be a subset of pivot's candidates
            if board.candidates[bi] & pivot_mask == board.candidates[bi] {
                bivalue_buddies.push(bi);
            }
        }

        if bivalue_buddies.len() < 2 {
            continue;
        }

        for i in 0..bivalue_buddies.len() {
            for j in (i + 1)..bivalue_buddies.len() {
                let p1 = bivalue_buddies[i];
                let p2 = bivalue_buddies[j];
                // Combined mask of pincers should cover all 3 candidates
                let combined = board.candidates[p1] | board.candidates[p2];
                if combined != pivot_mask {
                    continue;
                }
                // Shared candidate between p1 and p2
                let shared_mask = board.candidates[p1] & board.candidates[p2];
                if ANZ_VALUES[shared_mask as usize] != 1 {
                    continue;
                }
                let shared = CAND_FROM_MASK[shared_mask as usize];

                // Eliminations: cells seeing pivot, p1, and p2
                let mut elims = Vec::new();
                for &buddy in &BUDDIES_ARRAY[pivot] {
                    let bi = buddy as usize;
                    if bi != p1
                        && bi != p2
                        && BUDDIES_ARRAY[p1].contains(&buddy)
                        && BUDDIES_ARRAY[p2].contains(&buddy)
                        && board.is_candidate(bi, shared)
                    {
                        elims.push((bi, shared));
                    }
                }

                if !elims.is_empty() {
                    return Some(SolutionStep::elimination(
                        SolutionType::XyzWing,
                        vec![pivot, p1, p2],
                        shared,
                        elims,
                    ));
                }
            }
        }
    }
    None
}

/// W-Wing: two bivalue cells with same candidates, connected by strong link.
fn find_w_wing(board: &Board) -> Option<SolutionStep> {
    // Find all bivalue cells
    let bivalues: Vec<usize> = (0..81)
        .filter(|&i| board.candidates[i] != 0 && ANZ_VALUES[board.candidates[i] as usize] == 2)
        .collect();

    for i in 0..bivalues.len() {
        for j in (i + 1)..bivalues.len() {
            let c1 = bivalues[i];
            let c2 = bivalues[j];
            if board.candidates[c1] != board.candidates[c2] {
                continue;
            }
            // Must not see each other
            if BUDDIES_ARRAY[c1].contains(&(c2 as u8)) {
                continue;
            }

            let mask = board.candidates[c1];
            let pv = &POSSIBLE_VALUES[mask as usize];
            let a = pv.digits[0];
            let b = pv.digits[1];

            // Try connecting via strong link on digit a, eliminating digit b
            if let Some(s) = try_w_wing_link(board, c1, c2, a, b) {
                return Some(s);
            }
            // Try connecting via strong link on digit b, eliminating digit a
            if let Some(s) = try_w_wing_link(board, c1, c2, b, a) {
                return Some(s);
            }
        }
    }
    None
}

fn try_w_wing_link(
    board: &Board,
    c1: usize,
    c2: usize,
    link_digit: u8,
    elim_digit: u8,
) -> Option<SolutionStep> {
    // Find strong link on link_digit connecting buddies of c1 and c2
    for unit in &ALL_UNITS {
        // Check if this unit has exactly 2 candidates for link_digit
        let mut count = 0;
        let mut link1 = 0usize;
        let mut link2 = 0usize;
        for &cell in unit {
            if board.is_candidate(cell as usize, link_digit) {
                count += 1;
                if count == 1 {
                    link1 = cell as usize;
                } else if count == 2 {
                    link2 = cell as usize;
                }
            }
        }
        if count != 2 {
            continue;
        }

        // Case 1: link1 sees c1, link2 sees c2
        if BUDDIES_ARRAY[c1].contains(&(link1 as u8))
            && BUDDIES_ARRAY[c2].contains(&(link2 as u8))
            && link1 != c1
            && link2 != c2
        {
            let mut elims = Vec::new();
            for &buddy in &BUDDIES_ARRAY[c1] {
                let bi = buddy as usize;
                if bi != c2
                    && BUDDIES_ARRAY[c2].contains(&buddy)
                    && board.is_candidate(bi, elim_digit)
                {
                    elims.push((bi, elim_digit));
                }
            }
            if !elims.is_empty() {
                return Some(SolutionStep::elimination(
                    SolutionType::WWing,
                    vec![c1, c2, link1, link2],
                    link_digit,
                    elims,
                ));
            }
        }

        // Case 2: link1 sees c2, link2 sees c1
        if BUDDIES_ARRAY[c2].contains(&(link1 as u8))
            && BUDDIES_ARRAY[c1].contains(&(link2 as u8))
            && link1 != c2
            && link2 != c1
        {
            let mut elims = Vec::new();
            for &buddy in &BUDDIES_ARRAY[c1] {
                let bi = buddy as usize;
                if bi != c2
                    && BUDDIES_ARRAY[c2].contains(&buddy)
                    && board.is_candidate(bi, elim_digit)
                {
                    elims.push((bi, elim_digit));
                }
            }
            if !elims.is_empty() {
                return Some(SolutionStep::elimination(
                    SolutionType::WWing,
                    vec![c1, c2, link2, link1],
                    link_digit,
                    elims,
                ));
            }
        }
    }
    None
}

/// Remote Pair: chain of 4+ bivalue cells (even length) all with the same {a,b} pair,
/// consecutive cells are buddies. Cells seeing both endpoints can have a and b eliminated.
fn find_remote_pair(board: &Board) -> Option<SolutionStep> {
    let mut by_mask: std::collections::HashMap<u16, Vec<usize>> = std::collections::HashMap::new();
    for i in 0..81 {
        let mask = board.candidates[i];
        if mask != 0 && ANZ_VALUES[mask as usize] == 2 {
            by_mask.entry(mask).or_default().push(i);
        }
    }

    for (&mask, cells) in &by_mask {
        if cells.len() < 4 {
            continue;
        }

        let pv = &POSSIBLE_VALUES[mask as usize];
        let a = pv.digits[0];
        let b = pv.digits[1];

        // Build adjacency: which cells in this group are buddies
        let n = cells.len();
        let mut adj: Vec<Vec<usize>> = vec![Vec::new(); n];
        for i in 0..n {
            for j in (i + 1)..n {
                if BUDDIES_ARRAY[cells[i]].contains(&(cells[j] as u8)) {
                    adj[i].push(j);
                    adj[j].push(i);
                }
            }
        }

        // DFS from each cell to find even-length chains >= 4 with eliminations
        for start in 0..n {
            let mut stack: Vec<(usize, Vec<usize>)> = vec![(start, vec![start])];
            while let Some((current, chain)) = stack.pop() {
                if chain.len() >= 4 && chain.len() % 2 == 0 {
                    let first = cells[chain[0]];
                    let last = cells[*chain.last().unwrap()];

                    let mut elims = Vec::new();
                    for &buddy in &BUDDIES_ARRAY[first] {
                        let bi = buddy as usize;
                        if bi == last || chain.iter().any(|&ci| cells[ci] == bi) {
                            continue;
                        }
                        if BUDDIES_ARRAY[last].contains(&buddy) {
                            if board.is_candidate(bi, a) {
                                elims.push((bi, a));
                            }
                            if board.is_candidate(bi, b) {
                                elims.push((bi, b));
                            }
                        }
                    }

                    if !elims.is_empty() {
                        let indices: Vec<usize> = chain.iter().map(|&ci| cells[ci]).collect();
                        return Some(SolutionStep::elimination(
                            SolutionType::RemotePair,
                            indices,
                            a,
                            elims,
                        ));
                    }
                }

                // Cap chain length to avoid combinatorial explosion
                if chain.len() >= 8 {
                    continue;
                }

                for &next in &adj[current] {
                    if !chain.contains(&next) {
                        let mut new_chain = chain.clone();
                        new_chain.push(next);
                        stack.push((next, new_chain));
                    }
                }
            }
        }
    }
    None
}
