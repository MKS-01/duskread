---
id: radix-sort
title: Radix Sort
tagline: Sort by one digit at a time, least significant first, and the whole number falls into order.
level: intermediate
related: counting-sort, arrays
---

## Note
- Sort by the least significant digit first, most significant last — running a stable counting sort once per digit position leaves the whole number correctly ordered.
- **O(d × (n + k))** for d digits and a base-k counting sort per pass — linear in n when the digit count is bounded, beating O(n log n) comparison sorts.
- **Every pass must be stable**, using prefix-sum counting sort — an unstable pass destroys the ordering that earlier, less-significant passes already established.
- Works cleanly on **fixed-width keys**: integers with a bounded digit count, fixed-length strings. Variable-length keys need padding or a most-significant-digit variant.
- **Base choice is a real trade-off.** Base 10 needs 10 counting buckets and more passes; base 256 (byte-at-a-time) needs fewer passes but a bigger count array per pass.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
