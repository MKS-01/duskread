---
id: two-pointers
title: Two Pointers
tagline: Walk two positions through the data at once, and most O(n²) problems collapse to O(n).
level: basic
related: arrays, sliding-window, binary-search
---

## Note
- Replace a nested loop with two indices moving through the data under a clear rule for when each one advances — turns many O(n²) scans into O(n).
- Works whenever there's a monotonic relationship to exploit: sortedness, palindromic symmetry, or a window that only ever needs to grow or shrink one way.
- Two shapes: pointers converging from opposite ends (pair-sum, palindrome checks), and pointers moving in the same direction at different speeds (cycle detection, dedup).
- Each pointer moves at most n times total, so the whole traversal is linear even though it looks like it's tracking two positions.
- **The correctness argument is the interview answer**, not the code — being able to say precisely why moving a given pointer can never skip over the correct answer is what's actually being tested.
- Sort first if the input isn't already sorted — the O(n log n) sort cost is usually still better than the O(n²) it replaces.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
