---
id: binary-search
title: Binary Search
tagline: Halve the search space on every comparison.
level: basic
related: arrays, merge-sort
---

## Note
- Halve the search space on every comparison — O(log n) instead of O(n), but only on sorted data.
- Write `mid = lo + (hi - lo) / 2`, never `(lo + hi) / 2` — the latter can silently overflow.
- Reframe it as finding the boundary of a monotonic predicate, not just a value in an array — that unlocks "search the answer" problems.
- Prefer the **lower-bound** form (half-open range, `lo < hi`, no equality check) as your default. It returns the insertion point and has no special case for "not found".
- Every loop iteration must strictly shrink the range, or you spin forever. Check that each branch either raises `lo` or lowers `hi`.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** First index whose value is >= target — the insertion point that keeps the array sorted. */
fun lowerBound(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.size // exclusive
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (nums[mid] < target) lo = mid + 1 else hi = mid
    }
    return lo
}
```

## Code: Go
```go
// LowerBound returns the first index whose value is >= target, or len(nums)
// if every element is smaller — the insertion point.
func LowerBound(nums []int, target int) int {
	lo, hi := 0, len(nums)
	for lo < hi {
		mid := lo + (hi-lo)/2
		if nums[mid] < target {
			lo = mid + 1
		} else {
			hi = mid
		}
	}
	return lo
}
```

## Questions
### Binary Search
id: 704
difficulty: easy
askedAt: Warm-up at almost every company
The template itself. Worth writing from memory until the boundary conditions stop needing thought.

### Search in Rotated Sorted Array
id: 33
difficulty: medium
askedAt: Amazon, Meta, Microsoft
A rotated array still has one sorted half at every step. Work out which half is sorted by comparing nums[lo] with nums[mid], then check whether the target lies inside that half's range — if it does, search there, otherwise search the other side.

### Koko Eating Bananas
id: 875
difficulty: medium
askedAt: Google, Meta — the classic binary-search-on-answer test
There is no sorted array here. Binary search the answer: "can Koko finish at speed k?" is false for small k and true for all larger k, so search that predicate over 1..max(piles) for the first true.

## References
