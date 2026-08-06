---
id: counting-sort
title: Counting Sort
tagline: Skip comparisons entirely — count occurrences and place each value directly.
level: intermediate
related: radix-sort, arrays
---

## Note
- No comparisons at all: count how many times each value occurs, then use those counts to place every element directly — O(n + k) for a value range of size k.
- Only works on small non-negative integers, or anything mappable to them (characters, bounded scores) — not arbitrary comparable objects.
- **The stable version uses a running prefix sum over the counts** to compute each value's starting position in the output, then places elements back-to-front to preserve relative order.
- The count array's cost is driven by the **range k**, not by how many elements n you actually have — a huge range defeats the whole idea.
- **Radix sort is counting sort applied once per digit** — the stability of each pass is what lets the next digit's pass build correctly on top of it.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Stable counting sort. Assumes non-negative values; offset first if not. */
fun countingSort(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    val max = nums.max()

    val counts = IntArray(max + 1)
    for (value in nums) counts[value]++

    // Running prefix sum: count[v] becomes "how many elements are <= v".
    for (v in 1..max) counts[v] += counts[v - 1]

    val output = IntArray(nums.size)
    // Walking backwards through the input is what makes this stable —
    // equal values keep their original relative order.
    for (i in nums.indices.reversed()) {
        val value = nums[i]
        counts[value]--
        output[counts[value]] = value
    }
    return output
}
```

## Code: Go
```go
// CountingSort is stable. Assumes non-negative values; offset first if not.
func CountingSort(nums []int) []int {
	if len(nums) == 0 {
		return nums
	}
	max := nums[0]
	for _, v := range nums {
		if v > max {
			max = v
		}
	}

	counts := make([]int, max+1)
	for _, v := range nums {
		counts[v]++
	}

	// Running prefix sum: counts[v] becomes "how many elements are <= v".
	for v := 1; v <= max; v++ {
		counts[v] += counts[v-1]
	}

	output := make([]int, len(nums))
	// Walking backwards through the input is what makes this stable.
	for i := len(nums) - 1; i >= 0; i-- {
		v := nums[i]
		counts[v]--
		output[counts[v]] = v
	}
	return output
}
```

## Questions
### Sort Colors
id: 75
difficulty: medium
askedAt: Amazon, Meta, Microsoft
With only 3 possible values, this is counting sort with 3 buckets: count the 0s, 1s and 2s, then overwrite the array from the counts. The one-pass Dutch-flag partition is the fancier answer, but naming this as counting sort first shows you understand why it works.

### Top K Frequent Elements
id: 347
difficulty: medium
askedAt: Amazon, Meta, Yahoo
Bucket sort by frequency — index a bucket array by count (0 to n) and drop each value into its bucket — reads off the top k in O(n), beating both a full sort and a heap-based O(n log k) approach.

### Maximum Gap
id: 164
difficulty: hard
askedAt: Google, Meta — a favourite for testing non-comparison sorting
The pigeonhole principle bounds the minimum possible maximum gap given n elements across a known range, which sizes buckets so bucket sort finds the answer in O(n) — no full comparison sort required.

## References
