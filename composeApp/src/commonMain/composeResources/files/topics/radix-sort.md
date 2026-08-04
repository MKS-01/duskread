---
id: radix-sort
title: Radix Sort
tagline: Sort by one digit at a time, least significant first, and the whole number falls into order.
level: intermediate
related: counting-sort, arrays
---

## Quick Summary
- Sort by the least significant digit first, most significant last — running a stable counting sort once per digit position leaves the whole number correctly ordered.
- O(d × (n + k)) for d digits and a base-k counting sort per pass — linear in n when the digit count is bounded, beating O(n log n) comparison sorts.
- Every pass must be stable, or the ordering established by earlier, less significant digits gets destroyed by a later pass.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Counting sort is fast, but limited to a small range of values — sorting large numbers directly by their full value is out of the question. Radix sort's trick is not to sort by the full value at all: sort by one digit at a time, using counting sort as the tool for each pass, starting from the least significant digit and working up to the most significant.

The order the digits are processed in is not incidental — it is the entire mechanism. After sorting by the ones digit, numbers ending in the same digit are grouped together, in whatever relative order they arrived in. Sort that result by the tens digit next, and because the pass is *stable*, ties on the tens digit fall back to the order the ones-digit pass already established. By the time the most significant digit's pass finishes, every digit position has been correctly resolved in the right priority — most significant wins overall, but only because every less-significant tie-break survived intact from the earlier passes.

That stability requirement is not a minor implementation detail — it is why radix sort has to be built on the prefix-sum version of counting sort specifically, not the naive "recount and re-emit" one. A single unstable pass anywhere in the sequence would scramble every ordering decision the earlier passes made.

The payoff is a sort that runs in O(d × (n + k)) for d digits and a base-k counting sort per digit — genuinely linear in n once the digit count is bounded, which beats every comparison sort's O(n log n) floor. The catch mirrors counting sort's: this only works cleanly on fixed-width keys with a small number of digits — 32-bit integers, fixed-length strings — not on arbitrary comparable values.

## Origin
Radix sort predates electronic computing entirely. **Herman Hollerith's punch-card tabulating machines**, built for the **1890 US Census**, sorted cards mechanically one column — one digit — at a time, running each card through a sorter for the current digit before moving to the next. That is a physical, mechanical radix sort, decades before the term or a formal computer algorithm existed. **Harold Seward's 1954 MIT thesis**, alongside describing counting sort, formalised the digit-by-digit computer version still used today.

## Key Points
- **Least significant digit first.** Sorting most-significant-digit first would need to reopen and re-sort each group by the next digit — LSD-first with stable passes avoids that entirely.
- **Every pass must be stable**, using prefix-sum counting sort. An unstable pass destroys the ordering that earlier, less-significant passes already established.
- **O(d × (n + k))** for d digits and a base-k counting sort per digit — linear in n when d is bounded, which is how it beats the O(n log n) comparison-sort floor.
- Works cleanly on **fixed-width keys**: integers with a bounded digit count, fixed-length strings. Variable-length keys need padding or a most-significant-digit variant.
- **Base choice is a real trade-off.** Base 10 needs 10 counting buckets and more passes; base 256 (byte-at-a-time) needs fewer passes but a bigger count array per pass.

## Complexity
Sort | O(d × (n + k)) | O(n + k) | d = number of digits, k = base (bucket count per pass). Linear in n when d is bounded.
vs. comparison sorts | — | — | Beats O(n log n) only when d is small relative to log n — very large or unbounded-length keys erase the advantage.

## Pitfalls
- Sorting most-significant-digit first without a plan for re-partitioning each group by the next digit — LSD-first with stable passes is simpler and is almost always what 'radix sort' means in practice.
- Using an unstable counting sort for any pass — it silently destroys the work of every earlier, less-significant pass.
- Applying it to keys with wildly varying digit counts without padding — shorter keys need to be treated as having leading zeros, or they sort incorrectly relative to longer ones.
- Choosing a base without thinking about the trade-off — a tiny base means many passes; a huge base means a large count array rebuilt on every pass.

## Steps
1. Find the maximum number of digits among all values, to know how many passes are needed.
2. For each digit position, starting from the least significant: run a stable counting sort keyed on just that digit.
3. Use the result of each pass as the input to the next — ties are already correctly broken by every earlier, less-significant pass.
4. After the most significant digit's pass, the array is fully sorted.

## Code: Kotlin
```kotlin
/** LSD radix sort for non-negative integers, base 10. */
fun radixSort(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    var result = nums.copyOf()
    var placeValue = 1
    while (result.max() / placeValue > 0) {
        result = countingSortByDigit(result, placeValue)
        placeValue *= 10
    }
    return result
}

/** Stable counting sort keyed on the digit at [placeValue] (1, 10, 100, ...). */
private fun countingSortByDigit(nums: IntArray, placeValue: Int): IntArray {
    val counts = IntArray(10)
    for (value in nums) counts[(value / placeValue) % 10]++
    for (d in 1..9) counts[d] += counts[d - 1]

    val output = IntArray(nums.size)
    for (i in nums.indices.reversed()) {
        val digit = (nums[i] / placeValue) % 10
        counts[digit]--
        output[counts[digit]] = nums[i]
    }
    return output
}
```

## Code: Go
```go
// RadixSort is LSD radix sort for non-negative integers, base 10.
func RadixSort(nums []int) []int {
	if len(nums) == 0 {
		return nums
	}
	result := append([]int(nil), nums...)
	max := result[0]
	for _, v := range result {
		if v > max {
			max = v
		}
	}

	for placeValue := 1; max/placeValue > 0; placeValue *= 10 {
		result = countingSortByDigit(result, placeValue)
	}
	return result
}

// countingSortByDigit is a stable counting sort keyed on the digit at
// placeValue (1, 10, 100, ...).
func countingSortByDigit(nums []int, placeValue int) []int {
	var counts [10]int
	for _, v := range nums {
		counts[(v/placeValue)%10]++
	}
	for d := 1; d <= 9; d++ {
		counts[d] += counts[d-1]
	}

	output := make([]int, len(nums))
	for i := len(nums) - 1; i >= 0; i-- {
		digit := (nums[i] / placeValue) % 10
		counts[digit]--
		output[counts[digit]] = nums[i]
	}
	return output
}
```

## Questions
### Maximum Gap
id: 164
difficulty: hard
askedAt: Google, Meta — a favourite for testing non-comparison sorting
Radix sort the integers in O(n) instead of comparison-sorting them in O(n log n), then a single linear scan finds the maximum gap. The insight worth stating out loud is that the sort itself, not the scan, is what the O(n) requirement is really testing.

### Two Sum
id: 1
difficulty: easy
askedAt: The single most asked interview question
Not a radix-sort problem itself, but a useful contrast: if the values were bounded integers instead of arbitrary ones, a radix or counting sort followed by a two-pointer scan is a valid O(n)-ish alternative to the hash-table approach — worth mentioning to show you know when non-comparison sorting applies.

## References
