---
id: quicksort
title: Quicksort
tagline: Pick a pivot, partition around it, and let the recursion do the rest.
level: intermediate
related: merge-sort, arrays
---

## Quick Summary
- Partition around a pivot so everything smaller ends up left of it and everything bigger ends up right — then recurse on each side, no merge step needed.
- O(n log n) average, but a poor pivot choice degrades to O(n²) — pivot strategy is the entire engineering problem.
- Sorts in place with O(log n) auxiliary space, which is why it usually beats merge sort in practice despite matching average complexity.
- Not stable by default — equal elements can be reordered by the partition step.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Merge sort splits first and pays for the split later, in the merge. Quicksort inverts that order: do the hard work up front by picking a pivot and partitioning around it, so everything smaller than the pivot ends up to its left and everything bigger ends up to its right. Once that is done, the pivot is already in its final sorted position, and the two sides can be sorted independently — with no merge step required at all.

That absence of a merge step is the whole appeal. Partitioning can be done in place, swapping elements past each other as you scan, so quicksort needs no auxiliary array the way merge sort does. That is the main reason it tends to win in practice: less memory traffic and better cache behaviour, despite an average complexity that matches merge sort exactly at O(n log n).

The entire engineering problem is choosing the pivot well. A pivot that lands near the median splits the array roughly in half each time, giving log n levels of recursion. A pivot that lands near an extreme — always picking the first element on an already-sorted array, say — produces a split of size 1 and size n − 1, and the recursion degrades to O(n²), the same as never splitting at all. That is why "always pick the first element" is a textbook bad idea, and randomised or median-of-three pivot selection are the standard defences.

That worst case is also why quicksort is not a safe default when an adversary controls the input — a solved problem for merge sort, since its bound holds unconditionally. Real-world quicksorts, including the ones behind most language standard libraries' sort for primitives, randomise the pivot specifically to make the worst case astronomically unlikely rather than merely possible.

Quicksort is also not stable out of the box: two equal elements can cross each other during a partition and come out in the opposite order they went in. Where stability matters, merge sort or a stability-patched quicksort variant is the right tool instead.

## Origin
**Quicksort was invented by Tony Hoare in 1959**, while he was a visiting student at Moscow State University working on a machine-translation project — he needed to sort Russian words quickly to look them up in a dictionary. He published it as **'Algorithm 64: Quicksort' in Communications of the ACM in 1961**. Hoare won the Turing Award in 1980, largely for work that grew out of reasoning carefully about exactly this kind of algorithm.

## Key Points
- **Partition first, recurse after** — the opposite order from merge sort. Once partitioning finishes, the pivot is already in its final sorted position.
- **O(n log n) average, O(n²) worst case.** The worst case happens when the pivot is consistently the smallest or largest element, which a fixed 'always pick the first element' strategy walks straight into on sorted or reverse-sorted input.
- **In-place, O(log n) auxiliary space** for the recursion stack — no scratch array, unlike merge sort. This is the main practical reason it tends to win despite matching average complexity.
- **Randomised or median-of-three pivot selection** turns the worst case from 'likely on common input shapes' into 'vanishingly unlikely regardless of input' — the standard real-world defence.
- **Not stable** by default — a partition step can reorder equal elements relative to each other.
- Below roughly 10–20 elements, insertion sort is faster; production quicksorts switch to it for small sub-arrays rather than recursing all the way down.

## Complexity
Best / average case | O(n log n) | O(log n) | Balanced partitions; space is the recursion stack.
Worst case | O(n²) | O(n) | Consistently unbalanced partitions — degenerate recursion depth of n.
Randomised pivot | O(n log n) expected | O(log n) | The worst case still exists but is astronomically unlikely.

## Pitfalls
- Always picking the first (or last) element as the pivot — on already-sorted or reverse-sorted input this hits the O(n²) worst case every time.
- Forgetting that quicksort is not stable — relying on it to preserve the relative order of equal elements is a real bug, not a theoretical nitpick.
- Skipping the base case check and recursing on empty or single-element sub-arrays needlessly.
- Using it where a hard O(n log n) bound is required regardless of input, e.g. real-time systems — merge sort or heap sort give that guarantee, quicksort does not.
- Deep recursion on worst-case input overflowing the call stack — 'introsort' (falling back to heap sort past a recursion depth limit) is the standard production fix.

## Steps
1. Pick a pivot — a random element or median-of-three, to avoid the worst case.
2. Partition: scan the array, moving everything smaller than the pivot to its left and everything bigger to its right.
3. The pivot is now in its final sorted position — recurse on the sub-array to its left.
4. Recurse on the sub-array to its right.
5. A sub-array of size 0 or 1 is already sorted — that is the base case.

## Code: Kotlin
```kotlin
fun quicksort(nums: IntArray, lo: Int = 0, hi: Int = nums.lastIndex) {
    if (lo >= hi) return
    val p = partition(nums, lo, hi)
    quicksort(nums, lo, p - 1)
    quicksort(nums, p + 1, hi)
}

/** Lomuto partition, with a randomised pivot to avoid the O(n²) worst case. */
private fun partition(nums: IntArray, lo: Int, hi: Int): Int {
    val pivotIndex = (lo..hi).random()
    nums[pivotIndex] = nums[hi].also { nums[hi] = nums[pivotIndex] }
    val pivot = nums[hi]

    var boundary = lo
    for (i in lo until hi) {
        if (nums[i] < pivot) {
            nums[i] = nums[boundary].also { nums[boundary] = nums[i] }
            boundary++
        }
    }
    nums[boundary] = nums[hi].also { nums[hi] = nums[boundary] }
    return boundary
}
```

## Code: Go
```go
func Quicksort(nums []int, lo, hi int) {
	if lo >= hi {
		return
	}
	p := partition(nums, lo, hi)
	Quicksort(nums, lo, p-1)
	Quicksort(nums, p+1, hi)
}

// partition is Lomuto's scheme, with a randomised pivot to avoid the
// O(n^2) worst case.
func partition(nums []int, lo, hi int) int {
	pivotIndex := lo + rand.Intn(hi-lo+1)
	nums[pivotIndex], nums[hi] = nums[hi], nums[pivotIndex]
	pivot := nums[hi]

	boundary := lo
	for i := lo; i < hi; i++ {
		if nums[i] < pivot {
			nums[i], nums[boundary] = nums[boundary], nums[i]
			boundary++
		}
	}
	nums[boundary], nums[hi] = nums[hi], nums[boundary]
	return boundary
}
```

## Questions
### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
Implement quicksort directly and the naive first-element pivot times out on the test suite's adversarial cases — randomising the pivot choice is the fix, and being asked to explain why is the actual point of the question.

### Sort Colors
id: 75
difficulty: medium
askedAt: Amazon, Meta, Microsoft
The Dutch national flag problem — a three-way partition, which is quicksort's partition step generalised from two buckets to three. One pass with low/mid/high pointers sorts 0s, 1s and 2s without a full comparison sort.

### Kth Largest Element in an Array
id: 215
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Quickselect: run quicksort's partition step, but only recurse into the one side that must contain the kth element. Average O(n), because each partition throws away the other half's work entirely instead of sorting it.

## References
