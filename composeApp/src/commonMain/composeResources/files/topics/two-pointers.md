---
id: two-pointers
title: Two Pointers
tagline: Walk two positions through the data at once, and most O(n²) problems collapse to O(n).
level: basic
related: arrays, sliding-window, binary-search
---

## Quick Summary
- Replace a nested loop with two indices moving through the data under a clear rule for when each one advances — turns many O(n²) scans into O(n).
- Works whenever there's a monotonic relationship to exploit: sortedness, palindromic symmetry, or a window that only ever needs to grow or shrink one way.
- Two shapes: pointers converging from opposite ends (pair-sum, palindrome checks), and pointers moving in the same direction at different speeds (cycle detection, dedup).

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A huge number of array problems have an obvious O(n²) solution: for every element, scan the rest of the array looking for something. Two pointers is the realisation that, once the data is sorted or has some other monotonic structure, you almost never need to re-scan from the start — you can keep two positions moving through the array and use the relationship between what they point at to decide which one to move next, visiting each position a bounded number of times overall.

The clearest example is finding a pair that sums to a target in a sorted array. Start one pointer at the front, one at the back. If the pair's sum is too small, the only way to increase it is to move the left pointer right — the right pointer is already at the largest available value, so moving it wouldn't help. If the sum is too big, symmetric reasoning says move the right pointer left. Either way, exactly one pointer moves on each step, and the two can meet in at most n steps — one pass instead of a nested one.

That "opposite ends, converging" shape is one of two common patterns. The other is "same direction, different speeds": a slow pointer and a fast pointer both start at the front and advance under different rules, useful for removing duplicates in place (a write pointer only advances on a genuinely new value) or detecting a cycle in a linked list (a pointer moving twice as fast as another must lap it if a loop exists).

What both shapes share is a monotonic argument for why moving a pointer never throws away a valid answer — the same kind of reasoning that justifies binary search's halving. That's worth stating explicitly in an interview: two pointers isn't a trick to memorise per problem, it's a specific proof technique — "moving this pointer can never skip the answer, because..." — applied to array traversal.

## Origin
Two pointers is a **technique rather than a named, dated invention** — no single paper or person is credited with it. It emerges naturally once you're looking for ways to avoid re-scanning sorted or symmetric data, and versions of the idea appear scattered across algorithms literature from the earliest computing decades without a clean point of origin, much like insertion sort.

## Key Points
- **Two shapes cover most uses**: pointers converging from opposite ends (pair-sum, palindrome checks), and pointers moving in the same direction at different rates (cycle detection, in-place deduplication, fast/slow list traversal).
- **Requires a monotonic relationship to exploit** — usually sortedness, but symmetry (palindromes) or a one-directional window (see: sliding window) work the same way.
- **O(n) instead of O(n²)** — each pointer moves at most n times total, so the whole traversal is linear even though it looks like it's tracking two positions.
- **The correctness argument is the interview answer**, not the code — being able to say precisely why moving a given pointer can never skip over the correct answer is what's actually being tested.
- **Sort first if the input isn't already sorted** — the O(n log n) sort cost is usually still better than the O(n²) it replaces, and two pointers needs the sortedness to make its monotonic argument.

## Complexity
Two pointers over sorted/prepared data | O(n) | O(1) | Each pointer advances a bounded number of times total across the whole pass.
If a sort is needed first | O(n log n) | O(1) or O(n) | Dominated by the sort; the two-pointer pass itself stays O(n) afterward.

## Pitfalls
- Applying it to unsorted data without sorting first (when the problem allows it) — the monotonic argument that makes pointer movement safe depends on the sortedness existing in the first place.
- Moving the wrong pointer, or both pointers, when only one movement is justified by the current comparison — the single most common bug, and it usually produces a plausible-looking but wrong answer rather than a crash.
- Forgetting the boundary condition where the two pointers meet or cross — off-by-one errors here are extremely common and worth explicitly testing.
- Reaching for two pointers on a problem that needs non-adjacent, non-monotonic relationships — not every array problem fits the pattern, and forcing it produces contorted code that a hash set or brute force would have solved more simply.

## Steps
1. Identify the monotonic property the input has — sorted, symmetric, etc. — that makes a pointer's movement provably safe.
2. Place the two pointers according to the pattern: opposite ends for convergence, both at the start for different-speed traversal.
3. At each step, compare what the pointers point at and move exactly the pointer(s) justified by that comparison.
4. Stop when the pointers meet, cross, or one runs off the end, depending on the specific problem.

## Code: Kotlin
```kotlin
/** Opposite-ends convergence: pair summing to target in a sorted array. */
fun twoSumSorted(nums: IntArray, target: Int): Pair<Int, Int>? {
    var left = 0
    var right = nums.lastIndex

    while (left < right) {
        val sum = nums[left] + nums[right]
        when {
            sum == target -> return left to right
            sum < target -> left++  // only increasing left can raise the sum
            else -> right--         // only decreasing right can lower it
        }
    }
    return null
}

/** Same-direction, different speeds: dedupe a sorted array in place. */
fun removeDuplicates(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    var writer = 1 // only advances on a genuinely new value
    for (reader in 1 until nums.size) {
        if (nums[reader] != nums[writer - 1]) {
            nums[writer] = nums[reader]
            writer++
        }
    }
    return writer
}
```

## Code: Go
```go
// TwoSumSorted converges from opposite ends: pair summing to target in a
// sorted slice.
func TwoSumSorted(nums []int, target int) (int, int, bool) {
	left, right := 0, len(nums)-1

	for left < right {
		sum := nums[left] + nums[right]
		switch {
		case sum == target:
			return left, right, true
		case sum < target:
			left++ // only increasing left can raise the sum
		default:
			right-- // only decreasing right can lower it
		}
	}
	return 0, 0, false
}

// RemoveDuplicates dedupes a sorted slice in place: same direction,
// different speeds.
func RemoveDuplicates(nums []int) int {
	if len(nums) == 0 {
		return 0
	}
	writer := 1 // only advances on a genuinely new value
	for reader := 1; reader < len(nums); reader++ {
		if nums[reader] != nums[writer-1] {
			nums[writer] = nums[reader]
			writer++
		}
	}
	return writer
}
```

## Questions
### Two Sum II - Input Array Is Sorted
id: 167
difficulty: medium
askedAt: Amazon, Meta
The textbook opposite-ends convergence: the sortedness is what justifies moving exactly one pointer per step, turning an O(n²) or hash-table O(n) solution into O(n) time and O(1) space.

### Container With Most Water
id: 11
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Start at both ends and always move the shorter side inward — it's the bottleneck, so it's the only side that could possibly improve the answer. The taller side moving inward can only ever make things worse or equal.

### 3Sum
id: 15
difficulty: medium
askedAt: Amazon, Meta, Microsoft
Sort first, then fix one element and two-pointer the sorted remainder for the other two — turning what looks like an O(n³) triple loop into O(n²). Skipping duplicate values at each level is what avoids duplicate triplets in the output.

## References
