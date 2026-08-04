---
id: counting-sort
title: Counting Sort
tagline: Skip comparisons entirely — count occurrences and place each value directly.
level: intermediate
related: radix-sort, arrays
---

## Quick Summary
- No comparisons at all: count how many times each value occurs, then use those counts to place every element directly — O(n + k) for a value range of size k.
- Only works when values are small non-negative integers (or map cleanly to them) — a huge range defeats the whole idea, since the count array's cost is driven by the range, not by n.
- The stable variant, built on a running prefix sum over the counts, is the version that matters — radix sort is built directly on top of it.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Every comparison-based sort — merge sort, quicksort, heap sort — is bound by the same wall: Ω(n log n) comparisons, because there is no faster way to distinguish n! possible orderings. Counting sort escapes that wall entirely by not comparing elements to each other at all. If the values being sorted are integers in a known, small range, you can count how many times each value occurs and then read the counts back out in order — no comparisons required.

Concretely: allocate a count array sized to the range of possible values, scan the input once incrementing the count for each value seen, then walk the count array in order, emitting each value as many times as it occurred. Two linear passes, total cost O(n + k), where k is the size of the value range — genuinely faster than O(n log n) when k is small relative to n.

That "when k is small" is the entire catch, and it is why counting sort is not a general-purpose sort. If the values range from 0 to a billion, the count array itself costs O(billion) regardless of how few elements you are actually sorting — the algorithm's cost is driven by the *range* of possible values, not by n alone. Sorting ages (0–120) or exam scores (0–100) is exactly what this is for; sorting arbitrary 64-bit integers is not.

The version worth knowing precisely is the **stable** one, built with a running prefix sum over the counts rather than the naive "just re-emit" approach. Converting counts to prefix sums gives, for each value, the index its first occurrence should land at in the output — and placing elements from the end of the input backwards through that index preserves their original relative order. That stability is not a nice-to-have; it is the exact property **radix sort** depends on, since radix sort is counting sort run once per digit, and each digit's pass has to preserve the order the previous one already established.

## Origin
Counting sort was described by **Harold H. Seward in his 1954 master's thesis at MIT**, developed as the tool that makes each digit's pass of radix sort possible. It predates almost every other named sort in this curriculum, including quicksort and merge sort's popularisation, precisely because trading comparisons for direct counting is such a direct exploitation of a small, known value range.

## Key Points
- **No comparisons** — the value itself is used as (or maps to) an index, which is how it beats the Ω(n log n) comparison-sort lower bound.
- **O(n + k)**, where k is the size of the value range. Fast when k is small relative to n; the count array's size is driven by the range, not by how many elements you actually have.
- **The stable version uses a running prefix sum over the counts** to compute each value's starting position in the output, then places elements back-to-front to preserve their relative order.
- Only sorts **small non-negative integers**, or anything mappable to them (characters, bounded scores) — not arbitrary comparable objects.
- **Radix sort is counting sort applied once per digit** — the stability of each pass is what lets the next digit's pass build correctly on top of it.

## Complexity
Count + place | O(n + k) | O(n + k) | n input elements, k possible distinct values — the count array plus (for the stable version) the output array.
vs. comparison sorts | — | — | Beats the Ω(n log n) comparison lower bound only because it never compares elements — it exploits a bounded value range instead.

## Pitfalls
- Using it on a value range far larger than the input size — the count array's cost is driven by the range, not n, so this can be far slower and more memory-hungry than a comparison sort.
- Using the naive 'just re-emit counted values' version when stability matters — that version loses the original relative order of equal elements; the prefix-sum version preserves it.
- Forgetting to offset by the minimum value when the input includes negatives — counts need a non-negative index, so shift by `-min` first.
- Reaching for it on floating-point or arbitrary object data — it fundamentally requires values that map to small integer indices.

## Steps
1. Find the range of values — minimum and maximum — to size the count array.
2. Scan the input once, incrementing `count[value]` for each element.
3. Convert counts to a running prefix sum, so `count[v]` becomes the number of elements ≤ v.
4. Walk the input from the end backwards, placing each element at `count[value] - 1` in the output and decrementing that count — this is what preserves stability.

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
