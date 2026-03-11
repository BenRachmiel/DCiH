use crate::tables::BUDDIES_ARRAY;

/// 81-bit bitset for cell indices (0-80).
/// Uses u64 for cells 0-63 and u32 for cells 64-80 (17 bits).
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug, Default)]
pub struct CellSet {
    pub mask1: u64, // cells 0-63
    pub mask2: u32, // cells 64-80 (17 bits used)
}

const MAX_MASK2: u32 = 0x1FFFF; // 17 bits

impl CellSet {
    pub const fn new() -> Self {
        Self { mask1: 0, mask2: 0 }
    }

    pub const fn is_empty(self) -> bool {
        self.mask1 == 0 && self.mask2 == 0
    }

    #[inline]
    pub fn add(&mut self, value: usize) {
        if value >= 64 {
            self.mask2 |= 1 << (value - 64);
        } else {
            self.mask1 |= 1 << value;
        }
    }

    #[inline]
    pub fn remove(&mut self, value: usize) {
        if value >= 64 {
            self.mask2 &= !(1 << (value - 64));
        } else {
            self.mask1 &= !(1 << value);
        }
    }

    #[inline]
    pub const fn contains(self, value: usize) -> bool {
        if value >= 64 {
            (self.mask2 & (1 << (value - 64))) != 0
        } else {
            (self.mask1 & (1 << value)) != 0
        }
    }

    pub fn clear(&mut self) {
        self.mask1 = 0;
        self.mask2 = 0;
    }

    pub fn set_all(&mut self) {
        self.mask1 = u64::MAX;
        self.mask2 = MAX_MASK2;
    }

    pub fn or_assign(&mut self, other: CellSet) {
        self.mask1 |= other.mask1;
        self.mask2 |= other.mask2;
    }

    pub fn and_assign(&mut self, other: CellSet) {
        self.mask1 &= other.mask1;
        self.mask2 &= other.mask2;
    }

    pub fn and_not(&mut self, other: CellSet) {
        self.mask1 &= !other.mask1;
        self.mask2 &= !other.mask2;
    }

    pub const fn intersects(self, other: CellSet) -> bool {
        (self.mask1 & other.mask1) != 0 || (self.mask2 & other.mask2) != 0
    }

    pub const fn contains_all(self, other: CellSet) -> bool {
        (other.mask1 & !self.mask1) == 0 && (other.mask2 & !self.mask2) == 0
    }

    pub fn size(self) -> u32 {
        self.mask1.count_ones() + self.mask2.count_ones()
    }

    /// Returns indices of all set bits.
    pub fn to_vec(self) -> Vec<usize> {
        let mut result = Vec::with_capacity(self.size() as usize);
        let mut m = self.mask1;
        while m != 0 {
            let bit = m.trailing_zeros() as usize;
            result.push(bit);
            m &= m - 1;
        }
        let mut m = self.mask2;
        while m != 0 {
            let bit = m.trailing_zeros() as usize + 64;
            result.push(bit);
            m &= m - 1;
        }
        result
    }

    /// Iterate over set bits.
    pub fn iter(self) -> CellSetIter {
        CellSetIter {
            mask1: self.mask1,
            mask2: self.mask2,
        }
    }
}

pub struct CellSetIter {
    mask1: u64,
    mask2: u32,
}

impl Iterator for CellSetIter {
    type Item = usize;

    #[inline]
    fn next(&mut self) -> Option<usize> {
        if self.mask1 != 0 {
            let bit = self.mask1.trailing_zeros() as usize;
            self.mask1 &= self.mask1 - 1;
            Some(bit)
        } else if self.mask2 != 0 {
            let bit = self.mask2.trailing_zeros() as usize + 64;
            self.mask2 &= self.mask2 - 1;
            Some(bit)
        } else {
            None
        }
    }
}

/// Pre-computed CellSet buddies for each cell.
pub const BUDDIES: [CellSet; 81] = {
    let mut buddies = [CellSet::new(); 81];
    let mut i = 0usize;
    while i < 81 {
        let mut j = 0usize;
        while j < 20 {
            let b = BUDDIES_ARRAY[i][j] as usize;
            if b >= 64 {
                buddies[i].mask2 |= 1 << (b - 64);
            } else {
                buddies[i].mask1 |= 1 << b;
            }
            j += 1;
        }
        i += 1;
    }
    buddies
};

/// Pre-computed CellSet for each of the 27 constraints.
pub const ALL_CONSTRAINTS_SETS: [CellSet; 27] = {
    let mut sets = [CellSet::new(); 27];
    let mut i = 0usize;
    while i < 27 {
        let unit = crate::tables::ALL_UNITS[i];
        let mut j = 0usize;
        while j < 9 {
            let cell = unit[j] as usize;
            if cell >= 64 {
                sets[i].mask2 |= 1 << (cell - 64);
            } else {
                sets[i].mask1 |= 1 << cell;
            }
            j += 1;
        }
        i += 1;
    }
    sets
};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic_ops() {
        let mut s = CellSet::new();
        assert!(s.is_empty());
        s.add(0);
        s.add(80);
        assert!(s.contains(0));
        assert!(s.contains(80));
        assert!(!s.contains(1));
        assert_eq!(s.size(), 2);

        s.remove(0);
        assert!(!s.contains(0));
        assert_eq!(s.size(), 1);
    }

    #[test]
    fn test_set_ops() {
        let mut a = CellSet::new();
        let mut b = CellSet::new();
        a.add(0);
        a.add(5);
        b.add(5);
        b.add(10);

        assert!(a.intersects(b));

        let mut c = a;
        c.and_assign(b);
        assert!(c.contains(5));
        assert!(!c.contains(0));
        assert!(!c.contains(10));
    }

    #[test]
    fn test_iter() {
        let mut s = CellSet::new();
        s.add(3);
        s.add(65);
        s.add(70);
        let v: Vec<_> = s.iter().collect();
        assert_eq!(v, vec![3, 65, 70]);
    }

    #[test]
    fn test_buddies_consistency() {
        // Cell 0's buddies should match BUDDIES_ARRAY[0]
        let buddy_set = BUDDIES[0];
        for &b in &BUDDIES_ARRAY[0] {
            assert!(
                buddy_set.contains(b as usize),
                "BUDDIES[0] missing {}",
                b
            );
        }
        assert_eq!(buddy_set.size(), 20);
    }

    #[test]
    fn test_constraint_sets() {
        // Row 0 set should contain cells 0-8
        let row0 = ALL_CONSTRAINTS_SETS[0];
        for i in 0..9 {
            assert!(row0.contains(i));
        }
        assert!(!row0.contains(9));
        assert_eq!(row0.size(), 9);
    }
}
