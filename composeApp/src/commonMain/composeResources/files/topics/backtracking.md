---
id: backtracking
title: Backtracking
tagline: Try a choice, recurse, and undo it the moment it can't work.
level: advanced
related: dfs, coin-change
---

## Quick Summary
- Explore choices one at a time via recursion, undoing ('backtracking') the moment a partial choice can't lead anywhere valid — prunes whole branches instead of generating every possibility first.
- Always the same three steps: choose, explore, un-choose — the un-choose step is what separates it from plain recursive enumeration.
- Pruning early is the entire performance story: checking a constraint before recursing avoids generating exponentially many dead branches.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Some problems can only be solved by trying possibilities — placing queens on a board, choosing a subset, building a valid permutation — and the number of possibilities is exponential. Backtracking is the standard way to explore that space without generating every possibility up front: make one choice, recurse into the consequences of that choice, and if it turns out to be a dead end, undo it and try the next option instead of ever having built the full tree of possibilities in memory.

The pattern is always the same three-step shape: **choose** an option, **explore** by recursing with that choice in effect, and **un-choose** by undoing it before trying the next option at the same level. That un-choose step is the entire idea and the thing that's easy to forget — without it, a mutable data structure used to track the current partial solution (a board, a path, a set of used elements) keeps accumulating stale state from abandoned branches into the next one it tries.

What makes backtracking fast in practice — as opposed to just "recursive enumeration with extra bookkeeping" — is **pruning**: checking whether a partial choice can possibly lead to a valid solution *before* recursing into it, rather than after. Placing a queen where it immediately attacks another queen is checked and rejected in O(1), instead of recursing several levels deep into a board that was already doomed. The difference between "prune early" and "generate everything, then filter" is frequently the difference between a solution that finishes and one that never does, even though both are technically correct.

Backtracking and DFS are close relatives for the same reason quicksort and merge sort both recurse: both explore a tree of possibilities depth-first. What's different is the goal — DFS is usually looking for reachability or an existing path, while backtracking is actively constructing candidate solutions and needs the explicit undo step because it mutates shared state (a board, a running combination) rather than just visiting nodes.

## Origin
The term **'backtrack' is credited to D.H. Lehmer**, who used it in the 1950s to describe the general technique of systematically abandoning partial solutions and trying the next option, as documented by Donald Knuth in *The Art of Computer Programming*. The technique itself long predates the name — it is essentially the formalisation of exhaustive trial-and-error search with the explicit discipline of undoing a choice the moment it's known to fail.

## Key Points
- **Choose, explore, un-choose** — the fixed shape of every backtracking solution. Skipping the un-choose step is the most common bug, leaking state from abandoned branches into sibling attempts.
- **Pruning before recursing, not after**, is what makes it fast — checking a constraint early avoids generating and then discarding exponentially many dead branches.
- **Shares its recursive skeleton with DFS**, but actively builds and mutates a candidate solution as it goes, which is exactly why the undo step exists and DFS-for-reachability doesn't need one.
- **Time complexity is usually exponential** in the size of the choice space — the practical question is always how much pruning cuts that exponent down, not whether it's exponential in theory.
- **Passing an index or a 'used' set explicitly**, rather than mutating and forgetting to restore, is the safer default when a bug in the undo step would otherwise be easy to introduce.

## Complexity
Generic backtracking | O(b^d) | O(d) | b = branching factor, d = depth — exponential in the worst case; pruning reduces the effective branching factor.
Permutations of n items | O(n!) | O(n) | Every ordering is a valid leaf — no pruning is possible because every partial choice can still lead somewhere.

## Pitfalls
- Forgetting to un-choose — undo the mutation — before trying the next option at the same level. This leaks state from one branch into its siblings, producing subtly wrong results rather than a crash.
- Pruning after recursing instead of before — checking a constraint only once you're already several levels deep wastes all the work of getting there.
- Generating every full candidate before checking validity, instead of checking incrementally as each choice is made — the difference between exponential-with-pruning and just exponential.
- Using a mutable shared structure (like a `visited` set) without a matching removal on the way back out — the classic partner bug to forgetting the un-choose step generally.

## Steps
1. Check whether the current partial choice is already invalid — if so, prune: return immediately without recursing further.
2. If the partial choice is a complete, valid solution, record it.
3. Otherwise, for each available next choice: make the choice, recurse, then undo the choice before trying the next one.

## Code: Kotlin
```kotlin
/** All subsets of nums, built by choosing/skipping each element in turn. */
fun subsets(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val current = mutableListOf<Int>()

    fun backtrack(index: Int) {
        if (index == nums.size) {
            result.add(current.toList()) // snapshot — current keeps mutating
            return
        }
        backtrack(index + 1) // choice 1: skip nums[index]

        current.add(nums[index]) // choice 2: include nums[index]
        backtrack(index + 1)
        current.removeAt(current.lastIndex) // the un-choose step
    }

    backtrack(0)
    return result
}

/** N-Queens: prune the moment a placement attacks an existing queen. */
fun solveNQueens(n: Int): Int {
    val cols = BooleanArray(n)
    val diag1 = BooleanArray(2 * n) // row + col
    val diag2 = BooleanArray(2 * n) // row - col + n
    var solutions = 0

    fun backtrack(row: Int) {
        if (row == n) {
            solutions++
            return
        }
        for (col in 0 until n) {
            val d1 = row + col
            val d2 = row - col + n
            if (cols[col] || diag1[d1] || diag2[d2]) continue // pruned before recursing

            cols[col] = true; diag1[d1] = true; diag2[d2] = true
            backtrack(row + 1)
            cols[col] = false; diag1[d1] = false; diag2[d2] = false // un-choose
        }
    }

    backtrack(0)
    return solutions
}
```

## Code: Go
```go
// Subsets returns all subsets of nums, built by choosing/skipping each
// element in turn.
func Subsets(nums []int) [][]int {
	var result [][]int
	var current []int

	var backtrack func(index int)
	backtrack = func(index int) {
		if index == len(nums) {
			result = append(result, append([]int(nil), current...)) // snapshot
			return
		}
		backtrack(index + 1) // choice 1: skip nums[index]

		current = append(current, nums[index]) // choice 2: include nums[index]
		backtrack(index + 1)
		current = current[:len(current)-1] // the un-choose step
	}

	backtrack(0)
	return result
}

// SolveNQueens prunes the moment a placement attacks an existing queen.
func SolveNQueens(n int) int {
	cols := make([]bool, n)
	diag1 := make([]bool, 2*n) // row + col
	diag2 := make([]bool, 2*n) // row - col + n
	solutions := 0

	var backtrack func(row int)
	backtrack = func(row int) {
		if row == n {
			solutions++
			return
		}
		for col := 0; col < n; col++ {
			d1, d2 := row+col, row-col+n
			if cols[col] || diag1[d1] || diag2[d2] {
				continue // pruned before recursing
			}
			cols[col], diag1[d1], diag2[d2] = true, true, true
			backtrack(row + 1)
			cols[col], diag1[d1], diag2[d2] = false, false, false // un-choose
		}
	}

	backtrack(0)
	return solutions
}
```

## Questions
### Subsets
id: 78
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
The cleanest choose/skip backtracking skeleton — no pruning is even possible, since every partial subset is valid. Good for drilling the choose-explore-un-choose shape before adding constraints.

### Permutations
id: 46
difficulty: medium
askedAt: Amazon, Microsoft, Bloomberg
A 'used' set (or boolean array) tracks which elements are already placed — the un-choose step here means removing the element from 'used' after backtracking out of that branch, not just popping it from the current path.

### N-Queens
id: 51
difficulty: hard
askedAt: Amazon, Google, Microsoft
The canonical pruning example: checking column and both diagonal attacks in O(1) before recursing avoids ever descending into a doomed board, which is the entire difference between this finishing and not.

## References
