/// Digit bitmasks: MASKS[1]=0x001, MASKS[2]=0x002, ..., MASKS[9]=0x100.
/// MASKS[0] = 0 (unused sentinel).
pub const MASKS: [u16; 10] = [0, 0x001, 0x002, 0x004, 0x008, 0x010, 0x020, 0x040, 0x080, 0x100];

/// All 9 bits set (digits 1-9).
pub const MAX_MASK: u16 = 0x1ff;

/// Number of set bits (candidate count) for each bitmask 0..0x1ff.
pub const ANZ_VALUES: [u8; 512] = {
    let mut table = [0u8; 512];
    let mut mask = 0u16;
    while (mask as usize) < 512 {
        let mut count = 0u8;
        let mut m = mask;
        while m != 0 {
            count += 1;
            m &= m - 1;
        }
        table[mask as usize] = count;
        mask += 1;
    }
    table
};

/// For single-candidate masks, the digit (1-9). 0 for empty/multi-candidate masks.
pub const CAND_FROM_MASK: [u8; 512] = {
    let mut table = [0u8; 512];
    let mut mask = 1u16;
    while (mask as usize) < 512 {
        // Find least significant set bit -> digit
        let mut d = 1u8;
        let mut bit = 1u16;
        while mask & bit == 0 {
            d += 1;
            bit <<= 1;
        }
        table[mask as usize] = d;
        mask += 1;
    }
    table
};

/// For each bitmask (0..0x1ff), the array of candidate digits as a compact struct.
/// `POSSIBLE_VALUES[mask]` gives digits and count.
pub struct CandidateList {
    pub digits: [u8; 9],
    pub count: u8,
}

pub const POSSIBLE_VALUES: [CandidateList; 512] = {
    let mut table: [CandidateList; 512] = unsafe { core::mem::zeroed() };
    let mut mask = 0usize;
    while mask < 512 {
        let mut digits = [0u8; 9];
        let mut count = 0u8;
        let mut d = 1u8;
        let mut bit = 1u16;
        while d <= 9 {
            if (mask as u16) & bit != 0 {
                digits[count as usize] = d;
                count += 1;
            }
            d += 1;
            bit <<= 1;
        }
        table[mask] = CandidateList { digits, count };
        mask += 1;
    }
    table
};

/// Row cell indices: ROWS[r] = [r*9, r*9+1, ..., r*9+8].
pub const ROWS: [[u8; 9]; 9] = {
    let mut rows = [[0u8; 9]; 9];
    let mut r = 0usize;
    while r < 9 {
        let mut c = 0usize;
        while c < 9 {
            rows[r][c] = (r * 9 + c) as u8;
            c += 1;
        }
        r += 1;
    }
    rows
};

/// Column cell indices: COLS[c] = [c, 9+c, 18+c, ..., 72+c].
pub const COLS: [[u8; 9]; 9] = {
    let mut cols = [[0u8; 9]; 9];
    let mut c = 0usize;
    while c < 9 {
        let mut r = 0usize;
        while r < 9 {
            cols[c][r] = (r * 9 + c) as u8;
            r += 1;
        }
        c += 1;
    }
    cols
};

/// Block cell indices: BLOCKS[b] = 9 cells in block b.
pub const BLOCKS: [[u8; 9]; 9] = {
    let mut blocks = [[0u8; 9]; 9];
    let mut b = 0usize;
    while b < 9 {
        let br = (b / 3) * 3;
        let bc = (b % 3) * 3;
        let mut i = 0usize;
        while i < 9 {
            blocks[b][i] = ((br + i / 3) * 9 + (bc + i % 3)) as u8;
            i += 1;
        }
        b += 1;
    }
    blocks
};

/// All 27 constraints: rows 0-8, cols 9-17, blocks 18-26.
pub const ALL_UNITS: [[u8; 9]; 27] = {
    let mut units = [[0u8; 9]; 27];
    let mut i = 0usize;
    while i < 9 {
        units[i] = ROWS[i];
        units[9 + i] = COLS[i];
        units[18 + i] = BLOCKS[i];
        i += 1;
    }
    units
};

/// Block index for each cell.
const BLOCK_FROM_INDEX: [u8; 81] = {
    let mut table = [0u8; 81];
    let mut i = 0usize;
    while i < 81 {
        table[i] = ((i / 9 / 3) * 3 + (i % 9 / 3)) as u8;
        i += 1;
    }
    table
};

/// Constraints for each cell: [row, col, block].
pub const CONSTRAINTS: [[u8; 3]; 81] = {
    let mut table = [[0u8; 3]; 81];
    let mut i = 0usize;
    while i < 81 {
        table[i][0] = (i / 9) as u8;
        table[i][1] = (9 + i % 9) as u8;
        table[i][2] = 18 + BLOCK_FROM_INDEX[i];
        i += 1;
    }
    table
};

/// Buddy cell indices for each cell (cells sharing row, col, or block).
/// Each cell has exactly 20 buddies.
pub const BUDDIES_ARRAY: [[u8; 20]; 81] = {
    let mut buddies = [[0u8; 20]; 81];
    let mut i = 0usize;
    while i < 81 {
        let mut buf = [0u8; 20];
        let mut count = 0usize;
        let mut j = 0usize;
        while j < 81 {
            if i != j
                && (i / 9 == j / 9
                    || i % 9 == j % 9
                    || BLOCK_FROM_INDEX[i] == BLOCK_FROM_INDEX[j])
            {
                buf[count] = j as u8;
                count += 1;
            }
            j += 1;
        }
        buddies[i] = buf;
        i += 1;
    }
    buddies
};

#[inline]
pub const fn get_row(index: usize) -> usize {
    index / 9
}

#[inline]
pub const fn get_col(index: usize) -> usize {
    index % 9
}

#[inline]
pub const fn get_block(index: usize) -> usize {
    BLOCK_FROM_INDEX[index] as usize
}

#[inline]
pub const fn get_index(row: usize, col: usize) -> usize {
    row * 9 + col
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_masks() {
        assert_eq!(MASKS[0], 0);
        assert_eq!(MASKS[1], 1);
        assert_eq!(MASKS[9], 0x100);
    }

    #[test]
    fn test_anz_values() {
        assert_eq!(ANZ_VALUES[0], 0);
        assert_eq!(ANZ_VALUES[1], 1);
        assert_eq!(ANZ_VALUES[0b111], 3);
        assert_eq!(ANZ_VALUES[MAX_MASK as usize], 9);
    }

    #[test]
    fn test_cand_from_mask() {
        assert_eq!(CAND_FROM_MASK[0], 0);
        assert_eq!(CAND_FROM_MASK[1], 1); // bit 0 = digit 1
        assert_eq!(CAND_FROM_MASK[2], 2); // bit 1 = digit 2
        assert_eq!(CAND_FROM_MASK[0x100], 9); // bit 8 = digit 9
    }

    #[test]
    fn test_possible_values() {
        let pv = &POSSIBLE_VALUES[0b101]; // digits 1,3
        assert_eq!(pv.count, 2);
        assert_eq!(pv.digits[0], 1);
        assert_eq!(pv.digits[1], 3);
    }

    #[test]
    fn test_rows_cols_blocks() {
        assert_eq!(ROWS[0], [0, 1, 2, 3, 4, 5, 6, 7, 8]);
        assert_eq!(COLS[0], [0, 9, 18, 27, 36, 45, 54, 63, 72]);
        assert_eq!(BLOCKS[0], [0, 1, 2, 9, 10, 11, 18, 19, 20]);
    }

    #[test]
    fn test_all_units() {
        // Row 0 = ALL_UNITS[0]
        assert_eq!(ALL_UNITS[0], ROWS[0]);
        // Col 0 = ALL_UNITS[9]
        assert_eq!(ALL_UNITS[9], COLS[0]);
        // Block 0 = ALL_UNITS[18]
        assert_eq!(ALL_UNITS[18], BLOCKS[0]);
    }

    #[test]
    fn test_constraints() {
        // Cell 0: row 0, col 0, block 0
        assert_eq!(CONSTRAINTS[0], [0, 9, 18]);
        // Cell 80: row 8, col 8, block 8
        assert_eq!(CONSTRAINTS[80], [8, 17, 26]);
    }

    #[test]
    fn test_buddies_count() {
        for i in 0..81 {
            // Every cell has exactly 20 buddies
            let mut unique = std::collections::HashSet::new();
            for &b in &BUDDIES_ARRAY[i] {
                unique.insert(b);
            }
            assert_eq!(unique.len(), 20, "Cell {i} should have 20 unique buddies");
        }
    }

    #[test]
    fn test_get_block() {
        assert_eq!(get_block(0), 0);
        assert_eq!(get_block(3), 1);
        assert_eq!(get_block(80), 8);
        assert_eq!(get_block(40), 4); // center cell
    }
}
