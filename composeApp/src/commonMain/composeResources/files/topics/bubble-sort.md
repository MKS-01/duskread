---
id: bubble-sort
title: Bubble Sort
tagline: Swap adjacent out-of-order pairs, over and over, until nothing moves.
level: basic
related: arrays, selection-sort, insertion-sort
---

## Quick Summary
- Repeatedly walk the array swapping adjacent out-of-order pairs — after each full pass, the largest remaining element has 'bubbled' to its final position.
- O(n²) worst and average case, but O(n) best case on already-sorted input if you track whether any swap happened and stop early.
- Stable by construction — equal elements are never swapped past each other, since a swap only ever happens on a strict inequality.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Bubble sort's whole idea is in its name: repeatedly scan adjacent pairs, and whenever a pair is out of order, swap them. After one full pass across the array, the single largest element is guaranteed to have been swapped all the way to the end — it "bubbles up" past everything smaller than it, one adjacent swap at a time, because whatever it's compared against is smaller and loses the swap.

Repeating that pass n times guarantees the whole array is sorted, because each pass settles at least one more element — the current maximum of whatever's left — into its final position at the end. That is also exactly why the inner loop can shrink by one each pass: the tail is already correct and never needs re-checking.

The one improvement worth knowing is a flag: track whether any swap happened during a pass, and stop immediately if not. An already-sorted array then finishes in a single O(n) pass instead of grinding through all n passes doing nothing — the only common O(n²) sort with that specific best-case behaviour built in this simply.

Bubble sort is stable for a subtle but clean reason: a swap only ever happens when one element is strictly greater than its neighbour. Two equal elements are never swapped past each other, so their relative order survives untouched — the same property merge sort has to engineer deliberately by choosing which side to prefer on ties, bubble sort gets for free from the comparison itself.

## Origin
The earliest known description of bubble sort appears in **E.H. Friend's 1956 paper 'Sorting on Electronic Computer Systems'** in the Journal of the ACM, though the name 'bubble sort' itself didn't appear in print until the 1960s. Donald Knuth later devoted several pages of *The Art of Computer Programming* to analysing why it performs worse in practice than insertion sort despite the identical O(n²) bound — a rare case of a sort's own textbook analysis actively steering people away from using it.

## Key Points
- **O(n²) worst and average case** — every pair gets compared, repeatedly, across up to n passes.
- **O(n) best case with an early-exit flag**: if a full pass makes no swaps, the array is already sorted and the algorithm can stop immediately.
- **Stable by construction** — a swap only happens on a strict inequality, so equal elements are never swapped past each other.
- **In-place, O(1) auxiliary space.**
- Almost never used past a classroom setting — insertion sort dominates it in practice at every input size, with the same O(n²) worst case but fewer total operations.

## Complexity
Best case (with early exit) | O(n) | O(1) | One pass with no swaps confirms the array is already sorted.
Average / worst case | O(n²) | O(1) | Up to n passes, each shrinking by one settled element at the tail.

## Pitfalls
- Forgetting the early-exit flag — without it, bubble sort grinds through all n passes even on already-sorted input, wasting the one case where it could be fast.
- Assuming O(n²) sorts are interchangeable — bubble sort does strictly more swaps than insertion sort for the same input in the general case, so there's rarely a reason to prefer it.
- Confusing 'stable' with 'fast' — bubble sort's stability is a genuine property, but it doesn't compensate for its practical slowness.

## Steps
1. Walk the array comparing each adjacent pair.
2. If a pair is out of order, swap it.
3. After one full pass, the largest unsettled element has bubbled to the end — shrink the range by one.
4. Repeat until a full pass makes no swaps, or the range is empty.

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
        // No swaps this pass means the array is already sorted.
        if (!swapped) break
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
		// No swaps this pass means the array is already sorted.
		if !swapped {
			break
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
