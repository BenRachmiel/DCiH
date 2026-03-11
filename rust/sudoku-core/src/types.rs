use serde::{Deserialize, Serialize};

/// Difficulty tiers with score thresholds.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum Difficulty {
    Easy,
    Medium,
    Hard,
    Unfair,
    Extreme,
}

impl Difficulty {
    pub const fn max_score(self) -> i32 {
        match self {
            Difficulty::Easy => 800,
            Difficulty::Medium => 1000,
            Difficulty::Hard => 1600,
            Difficulty::Unfair => 1800,
            Difficulty::Extreme => i32::MAX,
        }
    }

    pub const fn from_score(score: i32) -> Self {
        if score < 800 {
            Difficulty::Easy
        } else if score < 1000 {
            Difficulty::Medium
        } else if score < 1600 {
            Difficulty::Hard
        } else if score < 1800 {
            Difficulty::Unfair
        } else {
            Difficulty::Extreme
        }
    }

    pub const fn label(self) -> &'static str {
        match self {
            Difficulty::Easy => "Easy",
            Difficulty::Medium => "Medium",
            Difficulty::Hard => "Hard",
            Difficulty::Unfair => "Unfair",
            Difficulty::Extreme => "Extreme",
        }
    }

    pub const fn ordinal(self) -> usize {
        match self {
            Difficulty::Easy => 0,
            Difficulty::Medium => 1,
            Difficulty::Hard => 2,
            Difficulty::Unfair => 3,
            Difficulty::Extreme => 4,
        }
    }

    pub const fn from_ordinal(ordinal: u8) -> Self {
        match ordinal {
            0 => Difficulty::Easy,
            1 => Difficulty::Medium,
            2 => Difficulty::Hard,
            3 => Difficulty::Unfair,
            _ => Difficulty::Extreme,
        }
    }
}

/// Grouping category for solving techniques.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum StrategyCategory {
    Singles,
    LockedCandidates,
    Subsets,
    Fish,
    SingleDigit,
    Wings,
    Coloring,
    Chains,
    BruteForce,
}

impl StrategyCategory {
    pub const fn display_name(self) -> &'static str {
        match self {
            StrategyCategory::Singles => "Singles",
            StrategyCategory::LockedCandidates => "Locked Candidates",
            StrategyCategory::Subsets => "Subsets",
            StrategyCategory::Fish => "Basic Fish",
            StrategyCategory::SingleDigit => "Single-Digit Patterns",
            StrategyCategory::Wings => "Wings",
            StrategyCategory::Coloring => "Coloring",
            StrategyCategory::Chains => "Chains",
            StrategyCategory::BruteForce => "Brute Force",
        }
    }
}

/// All 31 solving technique types.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SolutionType {
    FullHouse,
    NakedSingle,
    HiddenSingle,
    LockedCandidates1,
    LockedCandidates2,
    LockedPair,
    LockedTriple,
    NakedPair,
    NakedTriple,
    NakedQuadruple,
    HiddenPair,
    HiddenTriple,
    HiddenQuadruple,
    XWing,
    Swordfish,
    Jellyfish,
    Skyscraper,
    TwoStringKite,
    EmptyRectangle,
    TurbotFish,
    XyWing,
    XyzWing,
    WWing,
    RemotePair,
    SimpleColorsTrap,
    SimpleColorsWrap,
    MultiColors1,
    MultiColors2,
    XChain,
    XyChain,
    BruteForce,
}

impl SolutionType {
    pub const fn display_name(self) -> &'static str {
        match self {
            SolutionType::FullHouse => "Full House",
            SolutionType::NakedSingle => "Naked Single",
            SolutionType::HiddenSingle => "Hidden Single",
            SolutionType::LockedCandidates1 => "Locked Candidates (Pointing)",
            SolutionType::LockedCandidates2 => "Locked Candidates (Claiming)",
            SolutionType::LockedPair => "Locked Pair",
            SolutionType::LockedTriple => "Locked Triple",
            SolutionType::NakedPair => "Naked Pair",
            SolutionType::NakedTriple => "Naked Triple",
            SolutionType::NakedQuadruple => "Naked Quadruple",
            SolutionType::HiddenPair => "Hidden Pair",
            SolutionType::HiddenTriple => "Hidden Triple",
            SolutionType::HiddenQuadruple => "Hidden Quadruple",
            SolutionType::XWing => "X-Wing",
            SolutionType::Swordfish => "Swordfish",
            SolutionType::Jellyfish => "Jellyfish",
            SolutionType::Skyscraper => "Skyscraper",
            SolutionType::TwoStringKite => "2-String Kite",
            SolutionType::EmptyRectangle => "Empty Rectangle",
            SolutionType::TurbotFish => "Turbot Fish",
            SolutionType::XyWing => "XY-Wing",
            SolutionType::XyzWing => "XYZ-Wing",
            SolutionType::WWing => "W-Wing",
            SolutionType::RemotePair => "Remote Pair",
            SolutionType::SimpleColorsTrap => "Simple Colors (Trap)",
            SolutionType::SimpleColorsWrap => "Simple Colors (Wrap)",
            SolutionType::MultiColors1 => "Multi Colors 1",
            SolutionType::MultiColors2 => "Multi Colors 2",
            SolutionType::XChain => "X-Chain",
            SolutionType::XyChain => "XY-Chain",
            SolutionType::BruteForce => "Brute Force",
        }
    }

    pub const fn score(self) -> i32 {
        match self {
            SolutionType::FullHouse => 4,
            SolutionType::NakedSingle => 4,
            SolutionType::HiddenSingle => 14,
            SolutionType::LockedCandidates1 => 50,
            SolutionType::LockedCandidates2 => 50,
            SolutionType::LockedPair => 40,
            SolutionType::LockedTriple => 60,
            SolutionType::NakedPair => 60,
            SolutionType::NakedTriple => 80,
            SolutionType::NakedQuadruple => 120,
            SolutionType::HiddenPair => 70,
            SolutionType::HiddenTriple => 100,
            SolutionType::HiddenQuadruple => 150,
            SolutionType::XWing => 140,
            SolutionType::Swordfish => 150,
            SolutionType::Jellyfish => 160,
            SolutionType::Skyscraper => 130,
            SolutionType::TwoStringKite => 150,
            SolutionType::EmptyRectangle => 120,
            SolutionType::TurbotFish => 120,
            SolutionType::XyWing => 160,
            SolutionType::XyzWing => 180,
            SolutionType::WWing => 150,
            SolutionType::RemotePair => 110,
            SolutionType::SimpleColorsTrap => 150,
            SolutionType::SimpleColorsWrap => 150,
            SolutionType::MultiColors1 => 200,
            SolutionType::MultiColors2 => 200,
            SolutionType::XChain => 260,
            SolutionType::XyChain => 260,
            SolutionType::BruteForce => 10000,
        }
    }

    pub const fn category(self) -> StrategyCategory {
        match self {
            SolutionType::FullHouse | SolutionType::NakedSingle | SolutionType::HiddenSingle => {
                StrategyCategory::Singles
            }
            SolutionType::LockedCandidates1
            | SolutionType::LockedCandidates2
            | SolutionType::LockedPair
            | SolutionType::LockedTriple => StrategyCategory::LockedCandidates,
            SolutionType::NakedPair
            | SolutionType::NakedTriple
            | SolutionType::NakedQuadruple
            | SolutionType::HiddenPair
            | SolutionType::HiddenTriple
            | SolutionType::HiddenQuadruple => StrategyCategory::Subsets,
            SolutionType::XWing | SolutionType::Swordfish | SolutionType::Jellyfish => {
                StrategyCategory::Fish
            }
            SolutionType::Skyscraper
            | SolutionType::TwoStringKite
            | SolutionType::EmptyRectangle
            | SolutionType::TurbotFish => StrategyCategory::SingleDigit,
            SolutionType::XyWing
            | SolutionType::XyzWing
            | SolutionType::WWing
            | SolutionType::RemotePair => StrategyCategory::Wings,
            SolutionType::SimpleColorsTrap
            | SolutionType::SimpleColorsWrap
            | SolutionType::MultiColors1
            | SolutionType::MultiColors2 => StrategyCategory::Coloring,
            SolutionType::XChain | SolutionType::XyChain => StrategyCategory::Chains,
            SolutionType::BruteForce => StrategyCategory::BruteForce,
        }
    }

    pub const fn difficulty(self) -> Difficulty {
        match self {
            SolutionType::FullHouse | SolutionType::NakedSingle | SolutionType::HiddenSingle => {
                Difficulty::Easy
            }
            SolutionType::LockedCandidates1
            | SolutionType::LockedCandidates2
            | SolutionType::LockedPair
            | SolutionType::LockedTriple
            | SolutionType::NakedPair
            | SolutionType::NakedTriple
            | SolutionType::HiddenPair
            | SolutionType::HiddenTriple
            | SolutionType::RemotePair => Difficulty::Medium,
            SolutionType::NakedQuadruple
            | SolutionType::HiddenQuadruple
            | SolutionType::XWing
            | SolutionType::Swordfish
            | SolutionType::Jellyfish
            | SolutionType::Skyscraper
            | SolutionType::TwoStringKite
            | SolutionType::EmptyRectangle
            | SolutionType::TurbotFish
            | SolutionType::XyWing
            | SolutionType::XyzWing
            | SolutionType::WWing => Difficulty::Hard,
            SolutionType::SimpleColorsTrap
            | SolutionType::SimpleColorsWrap
            | SolutionType::MultiColors1
            | SolutionType::MultiColors2 => Difficulty::Unfair,
            SolutionType::XChain
            | SolutionType::XyChain
            | SolutionType::BruteForce => Difficulty::Extreme,
        }
    }

    pub const fn is_single(self) -> bool {
        matches!(
            self,
            SolutionType::FullHouse | SolutionType::NakedSingle | SolutionType::HiddenSingle
        )
    }

    pub const fn has_solver(self) -> bool {
        matches!(
            self,
            SolutionType::FullHouse
                | SolutionType::NakedSingle
                | SolutionType::HiddenSingle
                | SolutionType::LockedCandidates1
                | SolutionType::LockedCandidates2
                | SolutionType::LockedPair
                | SolutionType::LockedTriple
                | SolutionType::NakedPair
                | SolutionType::NakedTriple
                | SolutionType::NakedQuadruple
                | SolutionType::HiddenPair
                | SolutionType::HiddenTriple
                | SolutionType::HiddenQuadruple
                | SolutionType::XWing
                | SolutionType::Swordfish
                | SolutionType::Jellyfish
                | SolutionType::Skyscraper
                | SolutionType::TwoStringKite
                | SolutionType::EmptyRectangle
                | SolutionType::TurbotFish
                | SolutionType::XyWing
                | SolutionType::XyzWing
                | SolutionType::WWing
                | SolutionType::SimpleColorsTrap
                | SolutionType::SimpleColorsWrap
        )
    }
}

/// Semantic role for per-candidate highlighting.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum HighlightRole {
    Defining,
    Elimination,
    Secondary,
    Tertiary,
    ColorA,
    ColorB,
}
