---
id: arrays
title: Arrays
tagline: One unbroken block of memory — and everything that follows from it.
level: basic
related: linked-lists, stacks-queues, binary-search, hash-tables, heaps, quicksort, counting-sort, radix-sort
---

## Note
- Random access is O(1) because the address is **computed, not searched** — `base + i × size`.
- Insert or delete anywhere but the end costs O(n): everything has to shift to keep the block unbroken.
- Dynamic arrays grow by doubling — appending is O(1) **amortised**, not guaranteed O(1) on every call.
- Unmatched **cache locality**: a linear scan of an array beats the same scan over a linked list, despite matching Big-O.
- A **two-dimensional array is still one-dimensional underneath**, laid out row by row — row-major iteration is much faster than columns-then-rows.
- Deleting when order does not matter: swap the victim with the last element and shrink. That turns an O(n) removal into O(1).

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
// Access is address arithmetic — no search, no comparison.
val nums = intArrayOf(7, 12, 19, 26, 33)
val third = nums[2] // base + 2 * 4 bytes

/** Removes [index] while preserving order — everything right shifts left, O(n). */
fun removeAt(nums: MutableList<Int>, index: Int) {
    for (i in index until nums.lastIndex) nums[i] = nums[i + 1]
    nums.removeAt(nums.lastIndex)
}
```

## Code: Go
```go
// Access is address arithmetic — no search, no comparison.
nums := []int{7, 12, 19, 26, 33}
third := nums[2] // base + 2 * 8 bytes

// RemoveAt deletes index while preserving order. Everything to the
// right shifts left by one, so this is O(n).
func RemoveAt(nums []int, index int) []int {
	return append(nums[:index], nums[index+1:]...)
}
```

## Questions
### Remove Element
id: 27
difficulty: easy
askedAt: Warm-up screens everywhere
The swap-with-last trick, or a two-pointer write cursor. The real lesson is that you never need a second array — one pointer reads while another writes, and the writer only advances on keepers.

### Product of Array Except Self
id: 238
difficulty: medium
askedAt: Amazon, Meta, Apple
Division is banned, so the trick is two passes: one accumulating products from the left, one from the right. Store the left pass in the output array itself and the right pass in a single running variable to hit O(1) extra space.

### Rotate Array
id: 189
difficulty: medium
askedAt: Microsoft, Amazon
The in-place solution is beautiful and almost impossible to guess cold: reverse the whole array, then reverse the first k, then reverse the rest. Worth memorising as a technique, not as a one-off.

## References
