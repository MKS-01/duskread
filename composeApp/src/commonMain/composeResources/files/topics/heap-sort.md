---
id: heap-sort
title: Heap Sort
tagline: Turn an array into a heap, then pop the max off the end every time.
level: intermediate
related: heaps, merge-sort
---

## Note
- Build a max-heap from the array in O(n), then repeatedly swap the root — the max — to the end and shrink the heap: O(n log n), in place, no scratch array.
- **O(n log n) in every case** — best, average and worst — because there is no adversarial input the way there is for quicksort.
- **O(1) auxiliary space.** The only common O(n log n) sort that needs neither a scratch array (merge sort) nor risks O(n) recursion depth (quicksort's worst case).
- **Not stable**, and **poor cache locality** relative to quicksort despite matching Big-O — sinking jumps by index-doubling rather than scanning sequentially.
- The classic fallback inside **introsort**: run quicksort, but switch to heap sort if the recursion goes deeper than expected, guaranteeing the bound never slips to O(n²).

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
