---
id: binary-search
title: Binary Search
tagline: Halve the search space on every comparison.
level: basic
scene: binarySearchScene
related: arrays, merge-sort
---

## Quick Summary
- Halve the search space on every comparison — O(log n) instead of O(n), but only on sorted data.
- Reframe it as finding the boundary of a monotonic predicate, not just a value in an array — that unlocks 'search the answer' problems.
- Write `lo + (hi - lo) / 2`, never `(lo + hi) / 2` — the latter can silently overflow.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Binary search is the pay-off for keeping data sorted. One comparison against the middle element tells you which half the answer cannot be in, so you throw that half away and repeat. Ten elements takes four comparisons; a billion takes thirty.

The mental model that generalises best is not "find a value in an array" — it is "find the boundary in a monotonic predicate". Imagine mapping every index to true or false, where the sequence looks like false, false, false, true, true. Binary search finds the first true. Once you can phrase a problem that way, it does not matter whether you are searching an array, a range of answers, or a rotated list.

That reframing is what turns binary search from a library call into an interview weapon. "Koko eating bananas" has no sorted array anywhere in the statement, but "can she finish at speed k?" is monotonic in k — false for small speeds, true for large ones — so you binary search the answer itself.

## Key Points
- The input must be **sorted** with respect to whatever you are comparing — otherwise halving is unjustified.
- Write `mid = lo + (hi - lo) / 2`, not `(lo + hi) / 2`. The latter overflows once `lo + hi` exceeds `Int.MAX_VALUE`, a bug that sat in the JDK for nine years.
- Prefer the **lower-bound** form (half-open range, `lo < hi`, no equality check) as your default. It returns the insertion point, handles duplicates, and has no special case for "not found".
- Every loop iteration must strictly shrink the range, or you spin forever. Check that each branch either raises `lo` or lowers `hi`.
- Binary search on the *answer* applies whenever a predicate is monotonic, even with no array in sight.

## Complexity
Search | O(log n) | O(1) | Iterative form. The range halves each step, so at most ⌈log₂ n⌉ + 1 comparisons.
Search (recursive) | O(log n) | O(log n) | Call stack depth. Prefer the iterative form unless the recursion reads better.
Sorting first | O(n log n) | O(1)–O(n) | If you sort only to search once, a linear scan at O(n) is cheaper.

## Pitfalls
- Using `(lo + hi) / 2` on large ranges — silent integer overflow, negative index, crash.
- Mixing an inclusive `hi = n - 1` with a half-open loop condition `lo < hi`, or vice versa. Pick one convention and keep it consistent.
- Writing `lo = mid` instead of `lo = mid + 1`, which makes no progress when `hi == lo + 1` and hangs the loop.
- Assuming a returned index is unique when the array holds duplicates — plain binary search may return any matching index.
- Applying it to data that is only *nearly* sorted. Any inversion breaks the halving argument.

## Steps
1. Set `lo = 0` and `hi = n - 1`, making the whole array live.
2. While `lo <= hi`, compute `mid` without overflowing.
3. If `nums[mid] == target`, you are done — return `mid`.
4. If `nums[mid] < target`, the target must be to the right, so set `lo = mid + 1`.
5. Otherwise the target is to the left, so set `hi = mid - 1`.
6. If the loop exits, the range is empty and the target is absent. `lo` now holds the index where it would be inserted.

## Code: Kotlin
```kotlin
/** Returns the index of [target] in the sorted array, or -1 if absent. */
fun binarySearch(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.lastIndex          // inclusive

    while (lo <= hi) {
        val mid = lo + (hi - lo) / 2 // never overflows
        when {
            nums[mid] == target -> return mid
            nums[mid] < target  -> lo = mid + 1
            else                -> hi = mid - 1
        }
    }
    return -1
}

/**
 * The form worth memorising: the first index whose value is >= target.
 * Returns nums.size when every element is smaller, which is exactly the
 * insertion point that keeps the array sorted.
 */
fun lowerBound(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.size               // exclusive

    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (nums[mid] < target) lo = mid + 1 else hi = mid
    }
    return lo
}

/** Binary search over an answer range, given a monotonic predicate. */
fun firstTrue(lo: Int, hi: Int, predicate: (Int) -> Boolean): Int {
    var low = lo
    var high = hi
    while (low < high) {
        val mid = low + (high - low) / 2
        if (predicate(mid)) high = mid else low = mid + 1
    }
    return low
}
```

## Code: Go
```go
// BinarySearch returns the index of target in the sorted slice, or -1.
func BinarySearch(nums []int, target int) int {
	lo, hi := 0, len(nums)-1 // hi inclusive

	for lo <= hi {
		mid := lo + (hi-lo)/2 // never overflows
		switch {
		case nums[mid] == target:
			return mid
		case nums[mid] < target:
			lo = mid + 1
		default:
			hi = mid - 1
		}
	}
	return -1
}

// LowerBound returns the first index whose value is >= target, or len(nums)
// if every element is smaller. This is the insertion point.
func LowerBound(nums []int, target int) int {
	lo, hi := 0, len(nums) // hi exclusive

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

// FirstTrue binary searches an answer range with a monotonic predicate.
// The standard library also offers sort.Search, which does exactly this.
func FirstTrue(lo, hi int, predicate func(int) bool) int {
	for lo < hi {
		mid := lo + (hi-lo)/2
		if predicate(mid) {
			hi = mid
		} else {
			lo = mid + 1
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
