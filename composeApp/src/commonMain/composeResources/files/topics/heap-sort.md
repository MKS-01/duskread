---
id: heap-sort
title: Heap Sort
tagline: Turn an array into a heap, then pop the max off the end every time.
level: intermediate
related: heaps, merge-sort
---

## Quick Summary
- Build a max-heap from the array in O(n), then repeatedly swap the root — the max — to the end and shrink the heap: O(n log n), in place, no scratch array.
- The only common O(n log n) sort that needs O(1) extra space — merge sort needs O(n), quicksort's worst case needs O(n) of recursion stack.
- Not stable, and slower in practice than quicksort despite matching Big-O, because sinking jumps around the array rather than scanning it.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Heaps already give O(1) access to the maximum and O(log n) removal of it. Heap sort just uses that directly: build a max-heap out of the whole array, then repeatedly take the max off the top and place it at the end of the still-shrinking heap. Do that n times and the array ends up fully sorted, largest at the end, smallest at index 0.

The trick that makes it in-place is that the removed maximum does not need anywhere new to go — the heap only occupies the front part of the array, so the space vacated by shrinking it by one is exactly where the extracted max belongs. Swap the root with the last live element, shrink the heap's boundary by one, and sink the new root down to restore the heap property; repeat.

That is also exactly why heap sort needs no auxiliary array, unlike merge sort — everything happens by swapping elements within the one array you started with. The price for that space guarantee is speed in practice: each sink-down jumps around the array by index-doubling (`2i+1`, `2i+2`) rather than scanning sequentially, which is much harder on the CPU cache than quicksort's mostly-sequential partitioning. Same O(n log n) on paper, noticeably slower on real hardware.

Heap sort's one unconditional guarantee is what makes it valuable despite that: no adversarial input pushes it to O(n²) the way quicksort's does, and it needs no scratch memory the way merge sort does. That is precisely why it is the fallback inside "introsort" hybrids — quicksort's usual approach, with a switch to heap sort if the recursion depth suggests the worst case has been hit.

## Origin
Heap sort was **published by J.W.J. Williams in the same 1964 paper that introduced the heap itself** — Algorithm 232 in Communications of the ACM — making the data structure and the sorting algorithm built on it a single original contribution rather than two separate discoveries. Robert Floyd's O(n) heapify improvement, published later that same year, is what turned the construction step from the paper's original approach into the O(n) one taught today.

## Key Points
- **Build a max-heap in O(n)**, then repeatedly swap the root into the last live slot and sink the new root down — n extractions, each O(log n).
- **O(n log n) in every case** — best, average and worst — because there is no adversarial input the way there is for quicksort.
- **O(1) auxiliary space.** It is the only common O(n log n) sort that needs neither a scratch array (merge sort) nor risks O(n) recursion depth (quicksort's worst case).
- **Not stable** — swapping the max into place can reorder equal elements.
- **Poor cache locality** relative to quicksort, despite matching Big-O — sinking jumps by index-doubling rather than scanning sequentially.
- The classic fallback inside **introsort**: run quicksort, but switch to heap sort if the recursion goes deeper than expected, guaranteeing the O(n log n) bound never slips to O(n²).

## Complexity
Build heap | O(n) | O(1) | Bottom-up heapify — see the Heaps topic for why this isn't O(n log n).
Sort (all cases) | O(n log n) | O(1) | n extractions, each an O(log n) sink-down — no adversarial input changes this.

## Pitfalls
- Forgetting to re-sink after swapping the max to the end — the new root is very likely out of place, and skipping the sink-down silently breaks the sort.
- Assuming heap sort beats quicksort in practice because they share Big-O — quicksort's better cache locality usually wins on real data despite the matching complexity.
- Building the heap with n individual inserts instead of bottom-up heapify — that costs O(n log n) for construction alone, throwing away the whole point of an O(n) build.
- Expecting stability — equal elements can and do get reordered by the swap-and-sink process.

## Steps
1. Build a max-heap from the whole array using bottom-up heapify.
2. Swap the root (the maximum) with the last element of the current heap region.
3. Shrink the heap's boundary by one — the swapped-in element is now permanently sorted.
4. Sink the new root down to restore the heap property within the smaller heap.
5. Repeat until the heap region is a single element.

## Code: Kotlin
```kotlin
fun heapSort(nums: IntArray) {
    // Bottom-up heapify: start at the last parent, work back to the root.
    for (i in nums.size / 2 - 1 downTo 0) sinkDown(nums, i, nums.size)

    for (end in nums.lastIndex downTo 1) {
        nums[0] = nums[end].also { nums[end] = nums[0] }
        sinkDown(nums, 0, end)
    }
}

/** Restores the max-heap property at [start], within the live region [0, size). */
private fun sinkDown(nums: IntArray, start: Int, size: Int) {
    var i = start
    while (true) {
        val left = 2 * i + 1
        val right = 2 * i + 2
        var largest = i
        if (left < size && nums[left] > nums[largest]) largest = left
        if (right < size && nums[right] > nums[largest]) largest = right
        if (largest == i) break
        nums[i] = nums[largest].also { nums[largest] = nums[i] }
        i = largest
    }
}
```

## Code: Go
```go
func HeapSort(nums []int) {
	// Bottom-up heapify: start at the last parent, work back to the root.
	for i := len(nums)/2 - 1; i >= 0; i-- {
		sinkDown(nums, i, len(nums))
	}

	for end := len(nums) - 1; end >= 1; end-- {
		nums[0], nums[end] = nums[end], nums[0]
		sinkDown(nums, 0, end)
	}
}

// sinkDown restores the max-heap property at start, within the live
// region [0, size).
func sinkDown(nums []int, start, size int) {
	i := start
	for {
		left, right := 2*i+1, 2*i+2
		largest := i
		if left < size && nums[left] > nums[largest] {
			largest = left
		}
		if right < size && nums[right] > nums[largest] {
			largest = right
		}
		if largest == i {
			break
		}
		nums[i], nums[largest] = nums[largest], nums[i]
		i = largest
	}
}
```

## Questions
### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
The one implementation that never degrades: no scratch array like merge sort, no adversarial input like quicksort. The trade-off worth naming is heap sort's cache-unfriendly access pattern, usually visibly slower in practice despite identical Big-O.

### Kth Largest Element in an Array
id: 215
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Heapify the whole array in O(n), then extract the max only k times rather than sorting everything — O(n + k log n). Contrast this with the size-k min-heap approach: heapify-and-extract wins when k is close to n, the running min-heap wins when k is small.

## References
