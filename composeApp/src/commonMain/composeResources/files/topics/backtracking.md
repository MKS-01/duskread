---
id: backtracking
title: Backtracking
tagline: Try a choice, recurse, and undo it the moment it can't work.
level: advanced
related: dfs, coin-change
---

## Note
- Explore choices one at a time via recursion, undoing ("backtracking") the moment a partial choice can't lead anywhere valid — prunes whole branches instead of generating every possibility first.
- Always the same three steps: choose, explore, un-choose — the un-choose step is what separates it from plain recursive enumeration.
- Pruning before recursing, not after, is the entire performance story: checking a constraint early avoids generating exponentially many dead branches.
- Skipping the un-choose step is the most common bug — it leaks state from abandoned branches into sibling attempts.
- Shares its recursive skeleton with DFS, but actively builds and mutates a candidate solution as it goes — that's exactly why the undo step exists and DFS-for-reachability doesn't need one.
- Time complexity is usually exponential in the size of the choice space — the practical question is how much pruning cuts that exponent down.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
