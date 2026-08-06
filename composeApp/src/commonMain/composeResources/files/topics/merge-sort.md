---
id: merge-sort
title: Merge Sort
tagline: Divide until trivial, then merge sorted runs back together.
level: intermediate
related: binary-search, coin-change, arrays, linked-lists, heaps, quicksort, heap-sort
---

## Note
- Split until trivial, then merge sorted runs back together — **O(n log n) in every case**, no adversarial input.
- **Stable** by construction, provided merge ties break toward the left run (strict `<`) — the default behind Java's and Go's stable sorts.
- Needs **O(n) auxiliary space** for arrays; on a linked list it needs only O(log n) for the stack, since splitting and merging are free.
- Allocate the scratch buffer **once** at the top and thread it through the recursion — allocating inside merge is the common performance mistake.
- Skip the merge entirely when `a[mid] <= a[mid + 1]` — the two runs are already in order, so an already-sorted array costs only O(n).
- The natural choice for **external sorting**, where data doesn't fit in memory, because merging is sequential and streams well from disk.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Sorts a[lo..hi] in place; the scratch buffer is threaded through the recursion. */
fun mergeSort(a: IntArray, buf: IntArray, lo: Int, hi: Int) {
    if (lo >= hi) return
    val mid = lo + (hi - lo) / 2
    mergeSort(a, buf, lo, mid)
    mergeSort(a, buf, mid + 1, hi)
    if (a[mid] <= a[mid + 1]) return // already in order

    for (i in lo..hi) buf[i] = a[i]
    var i = lo
    var j = mid + 1
    for (k in lo..hi) {
        a[k] = when {
            i > mid -> buf[j++]
            j > hi -> buf[i++]
            buf[j] < buf[i] -> buf[j++] // strict < preserves stability
            else -> buf[i++]
        }
    }
}
```

## Code: Go
```go
// MergeSort sorts a[lo:hi+1] in place using buf as scratch space.
func MergeSort(a, buf []int, lo, hi int) {
	if lo >= hi {
		return
	}
	mid := lo + (hi-lo)/2
	MergeSort(a, buf, lo, mid)
	MergeSort(a, buf, mid+1, hi)
	if a[mid] <= a[mid+1] {
		return // already in order
	}

	copy(buf[lo:hi+1], a[lo:hi+1])
	i, j := lo, mid+1
	for k := lo; k <= hi; k++ {
		switch {
		case i > mid:
			a[k], j = buf[j], j+1
		case j > hi:
			a[k], i = buf[i], i+1
		case buf[j] < buf[i]: // strict < preserves stability
			a[k], j = buf[j], j+1
		default:
			a[k], i = buf[i], i+1
		}
	}
}
```

## Questions
### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
The one that asks you to actually write it — built-in sorts are disallowed. Merge sort is the safest answer because quicksort times out on the adversarial all-equal and already-sorted cases in the test set.

### Sort List
id: 148
difficulty: medium
askedAt: Amazon, Microsoft, Bloomberg
Merge sort is the right tool precisely because it is a linked list: find the middle with slow/fast pointers, split, sort each side, then merge by relinking nodes. O(1) extra space beyond the stack, which quicksort cannot match here.

### Merge k Sorted Lists
id: 23
difficulty: hard
askedAt: Google, Amazon, Meta — extremely common
Merging lists pairwise in a tournament — the merge half of merge sort applied k ways — gives O(N log k). Merging them one at a time into an accumulator is the trap: that degrades to O(N k). A min-heap over the k heads reaches the same bound.

## References
