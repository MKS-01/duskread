---
id: selection-sort
title: Selection Sort
tagline: Find the smallest remaining element, and put it exactly where it belongs.
level: basic
related: arrays, bubble-sort, insertion-sort
---

## Quick Summary
- Repeatedly scan the unsorted remainder for its minimum and swap it into place — O(n²) comparisons every time, regardless of input.
- The fewest writes of any common sort — exactly n swaps total — which matters when writes are far more expensive than comparisons.
- Not stable, and it never gets faster on nearly-sorted input, unlike insertion sort or bubble sort.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Selection sort is the most literal possible translation of "sort this" into an algorithm: find the smallest element, put it first; find the next smallest, put it second; repeat until done. Each pass scans the entire unsorted remainder to find its minimum, then swaps that minimum into the next open slot at the front.

That literalism is exactly why it costs O(n²) unconditionally — finding a minimum in an unsorted range fundamentally requires looking at every element in it, and the range shrinks by only one each pass. Unlike insertion sort or bubble sort, there is no shortcut for nearly-sorted input: selection sort scans the full remaining range on every single pass regardless of how close to sorted it already is.

What it does have going for it is the number of writes. Each pass does exactly one swap — the found minimum into its slot — so the entire sort performs at most n swaps total, dramatically fewer than most other O(n²) sorts. That property matters more than it sounds: on hardware where writes are expensive relative to reads, minimising writes at the cost of extra comparisons can be a real, deliberate trade.

It is not stable as usually implemented — swapping a distant minimum into place can jump it past equal elements it should have stayed behind — and there is no comparison-based improvement that fixes this without giving up the write-count advantage. In practice, selection sort is taught mainly as the simplest possible sorting algorithm to reason about correctness for, not as something to reach for.

## Origin
Unlike most structures in this curriculum, selection sort has **no single documented inventor or publication** — it is one of the earliest and most obvious ways to sort by hand, and it appears in computing literature from the 1950s onward as a baseline algorithm rather than a novel contribution attributed to any one person.

## Key Points
- **O(n²) comparisons in every case** — best, average and worst. There is no shortcut for nearly-sorted input, unlike insertion sort or bubble sort.
- **Exactly n swaps total** — one per pass — the fewest writes of any common comparison sort. Matters specifically when writes are expensive relative to comparisons.
- **Not stable** as usually implemented — swapping a distant minimum into place can reorder equal elements.
- **In-place, O(1) auxiliary space** — no scratch array needed.
- Rarely used in production; its value is almost entirely pedagogical — the simplest correctness argument of any sort in this curriculum.

## Complexity
Best / average / worst | O(n²) | O(1) | Every pass scans the full unsorted remainder regardless of input order.
Swaps | O(n) | — | Exactly one swap per pass — the fewest writes of any common O(n²) sort.

## Pitfalls
- Assuming it's stable — a swap can move a distant minimum past equal elements, changing their relative order.
- Using it on data where nearly-sorted input is common — it gets no benefit from that, unlike insertion sort, which drops close to O(n) on nearly-sorted input.
- Choosing it for anything performance-sensitive purely for its 'simplicity' — the O(n²) comparison count makes it a poor choice past a few hundred elements regardless of the low write count.

## Steps
1. Set a pointer to the start of the unsorted remainder.
2. Scan the entire remainder to find its minimum element.
3. Swap that minimum into the pointer's position.
4. Advance the pointer by one and repeat until the remainder is empty.

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
