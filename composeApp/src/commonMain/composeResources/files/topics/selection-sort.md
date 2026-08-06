---
id: selection-sort
title: Selection Sort
tagline: Find the smallest remaining element, and put it exactly where it belongs.
level: basic
related: arrays, bubble-sort, insertion-sort
---

## Note
- Repeatedly scan the unsorted remainder for its minimum and swap it into place — O(n²) comparisons every time, regardless of input.
- **O(n²) in every case** — best, average and worst. There is no shortcut for nearly-sorted input, unlike insertion sort or bubble sort.
- **Exactly n swaps total** — one per pass — the fewest writes of any common comparison sort. Matters when writes are expensive relative to comparisons.
- **Not stable** as usually implemented — swapping a distant minimum into place can reorder equal elements.
- In-place, O(1) auxiliary space — no scratch array needed.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
fun selectionSort(nums: IntArray) {
    for (i in nums.indices) {
        var minIndex = i
        for (j in i + 1 until nums.size) {
            if (nums[j] < nums[minIndex]) minIndex = j
        }
        // Exactly one swap per pass — this is the whole write budget.
        nums[i] = nums[minIndex].also { nums[minIndex] = nums[i] }
    }
}
```

## Code: Go
```go
func SelectionSort(nums []int) {
	for i := range nums {
		minIndex := i
		for j := i + 1; j < len(nums); j++ {
			if nums[j] < nums[minIndex] {
				minIndex = j
			}
		}
		// Exactly one swap per pass — this is the whole write budget.
		nums[i], nums[minIndex] = nums[minIndex], nums[i]
	}
}
```

## Questions
### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
A naive selection sort times out against this problem's constraints — the useful exercise is explaining precisely why (O(n²) comparisons with no early exit) before reaching for merge sort or heap sort instead.

### Kth Largest Element in an Array
id: 215
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Selection sort's core idea taken only partway: 'select the maximum' k times instead of n times gives O(n·k) — fine for small k, but a heap or quickselect beats it once k grows, which is worth being able to say out loud.

## References
