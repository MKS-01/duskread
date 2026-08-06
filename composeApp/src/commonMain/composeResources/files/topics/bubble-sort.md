---
id: bubble-sort
title: Bubble Sort
tagline: Swap adjacent out-of-order pairs, over and over, until nothing moves.
level: basic
related: arrays, selection-sort, insertion-sort
---

## Note
- Repeatedly walk the array swapping adjacent out-of-order pairs — after each full pass, the largest remaining element has "bubbled" to its final position.
- O(n²) worst and average case, but O(n) best case on already-sorted input if you track whether any swap happened and stop early.
- **Stable by construction** — equal elements are never swapped past each other, since a swap only ever happens on a strict inequality.
- In-place, O(1) auxiliary space.
- Almost never used past a classroom setting — insertion sort dominates it in practice at every input size, with the same O(n²) worst case but fewer total operations.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
fun bubbleSort(nums: IntArray) {
    for (end in nums.lastIndex downTo 1) {
        var swapped = false
        for (i in 0 until end) {
            if (nums[i] > nums[i + 1]) {
                nums[i] = nums[i + 1].also { nums[i + 1] = nums[i] }
                swapped = true
            }
        }
        if (!swapped) break // no swaps means the array is already sorted
    }
}
```

## Code: Go
```go
func BubbleSort(nums []int) {
	for end := len(nums) - 1; end >= 1; end-- {
		swapped := false
		for i := 0; i < end; i++ {
			if nums[i] > nums[i+1] {
				nums[i], nums[i+1] = nums[i+1], nums[i]
				swapped = true
			}
		}
		if !swapped {
			break // no swaps means the array is already sorted
		}
	}
}
```

## Questions
### Sort Array By Parity
id: 905
difficulty: easy
askedAt: Amazon, warm-up screens
A bubble-pass-style adjacent swap works, but a two-pointer partition (swap evens to the front, odds to the back) does it in one pass instead of many — worth showing you know the O(n²) bubble approach and why the two-pointer one beats it.

### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
Bubble sort passes correctness easily and fails performance immediately — a clean way to demonstrate you understand the gap between 'works' and 'works at this input size'.

## References
