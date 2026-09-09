# Bolt's Journal - Critical Learnings

## 2026-09-09 - Avoid Allocations in Inner Loops of Shuffle Operations
**Learning:** `IntRange` instantiation inside hot loops (such as `shuffled[i] in indexFrom until indexToExclusive`) creates unnecessary short-lived objects during queue modification operations. Replacing range checks with simple numerical comparisons (`shuffled[i] >= indexFrom && shuffled[i] < indexToExclusive`) avoids GC overhead.
**Action:** Always prefer direct integer bounds comparisons (`x >= min && x < max`) over `in min until max` inside loop iterations.
