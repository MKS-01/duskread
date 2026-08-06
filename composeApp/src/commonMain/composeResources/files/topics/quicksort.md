---
id: quicksort
title: Quicksort
tagline: Pick a pivot, partition around it, and let the recursion do the rest.
level: intermediate
related: merge-sort, arrays
---

## Note
- Partition around a pivot so everything smaller ends up left of it and everything bigger ends up right — then recurse on each side, no merge step needed.
- **O(n log n) average, O(n²) worst case.** The worst case happens when the pivot is consistently the smallest or largest element — pivot strategy is the entire engineering problem.
- **In-place, O(log n) auxiliary space** for the recursion stack — no scratch array, unlike merge sort. This is the main practical reason it tends to win despite matching average complexity.
- **Randomised or median-of-three pivot selection** turns the worst case from 'likely on common input shapes' into 'vanishingly unlikely regardless of input'.
- **Not stable** by default — a partition step can reorder equal elements relative to each other.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
