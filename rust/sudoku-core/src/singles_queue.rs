use crate::tables::CONSTRAINTS;

const CAPACITY: usize = 81 * 3;

/// Stack-allocated queue for deferred naked/hidden singles propagation.
#[derive(Clone)]
pub struct SinglesQueue {
    indices: [u8; CAPACITY],
    values: [u8; CAPACITY],
    put_index: usize,
    get_index: usize,
}

impl SinglesQueue {
    pub const fn new() -> Self {
        Self {
            indices: [0; CAPACITY],
            values: [0; CAPACITY],
            put_index: 0,
            get_index: 0,
        }
    }

    pub const fn is_empty(&self) -> bool {
        self.get_index >= self.put_index
    }

    pub fn add_single(&mut self, index: usize, value: u8) {
        self.indices[self.put_index] = index as u8;
        self.values[self.put_index] = value;
        self.put_index += 1;
    }

    /// Returns (index, value) or None if empty.
    pub fn get_single(&mut self) -> Option<(usize, u8)> {
        if self.get_index >= self.put_index {
            return None;
        }
        let qi = self.get_index;
        self.get_index += 1;
        if self.get_index >= self.put_index {
            self.get_index = 0;
            self.put_index = 0;
        }
        Some((self.indices[qi] as usize, self.values[qi]))
    }

    pub fn delete_naked_single(&mut self, cell_index: usize) {
        let ci = cell_index as u8;
        for i in self.get_index..self.put_index {
            if self.indices[i] == ci {
                for j in (i + 1)..self.put_index {
                    self.indices[j - 1] = self.indices[j];
                    self.values[j - 1] = self.values[j];
                }
                self.put_index -= 1;
                break;
            }
        }
    }

    pub fn delete_hidden_single(&mut self, constraint: u8, value: u8) {
        for i in self.get_index..self.put_index {
            if self.values[i] == value {
                let idx = self.indices[i] as usize;
                let c = &CONSTRAINTS[idx];
                if c[0] == constraint || c[1] == constraint || c[2] == constraint {
                    for j in (i + 1)..self.put_index {
                        self.indices[j - 1] = self.indices[j];
                        self.values[j - 1] = self.values[j];
                    }
                    self.put_index -= 1;
                    break;
                }
            }
        }
    }

    pub fn clear(&mut self) {
        self.get_index = 0;
        self.put_index = 0;
    }
}
