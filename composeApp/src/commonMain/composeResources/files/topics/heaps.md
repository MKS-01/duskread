---
id: heaps
title: Heaps
tagline: A tree that only promises the top is right — that weaker promise is what makes it fast.
level: intermediate
related: arrays, binary-trees, merge-sort, heap-sort
---

## Note
- A heap only guarantees a parent is smaller (or larger) than its children — weaker than full sortedness, but enough to make the min or max O(log n) to insert and remove.
- Almost always a **complete binary tree stored in a plain array**: node i's children sit at `2i+1` and `2i+2`, with no pointers needed.
- **Insert bubbles up**, swapping with the parent while smaller (min-heap); **extract-min sinks down** after swapping the root with the last element.
- **Peeking the min/max is O(1)** — it's always the root — but reading any other element, or asking whether a value is present at all, is O(n): a heap is not a search tree.
- **Heapify** (building a heap from an unordered array) is O(n), not O(n log n) — most nodes sit near the bottom and sink only a short distance.
- A **priority queue** is the abstract interface; a **binary heap** is the usual concrete implementation of it, not a synonym for it.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Array-backed min-heap. Node i's children live at 2i+1 and 2i+2. */
class MinHeap {
    private val items = mutableListOf<Int>()

    fun insert(value: Int) {
        items += value
        var i = items.lastIndex
        while (i > 0) {
            val parent = (i - 1) / 2
            if (items[i] >= items[parent]) break
            items[i] = items[parent].also { items[parent] = items[i] } // bubble up
            i = parent
        }
    }
}
```

## Code: Go
```go
// MinHeap is array-backed. Node i's children live at 2i+1 and 2i+2.
type MinHeap struct {
	items []int
}

func (h *MinHeap) Insert(value int) {
	h.items = append(h.items, value)
	i := len(h.items) - 1
	for i > 0 {
		parent := (i - 1) / 2
		if h.items[i] >= h.items[parent] {
			break
		}
		h.items[i], h.items[parent] = h.items[parent], h.items[i] // bubble up
		i = parent
	}
}
```

## Questions
### Kth Largest Element in an Array
id: 215
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Keep a min-heap of size k rather than sorting everything: push each value, and pop whenever the heap exceeds k. The root ends up as the kth largest, in O(n log k) instead of O(n log n).

### Top K Frequent Elements
id: 347
difficulty: medium
askedAt: Amazon, Meta, Yahoo
Count frequencies first, then use a heap of size k over the counts instead of sorting every distinct value. Bucket sort by frequency is the O(n) alternative worth mentioning as a follow-up.

### Find Median from Data Stream
id: 295
difficulty: hard
askedAt: Google, Amazon, Meta — a design-flavoured favourite
Two heaps: a max-heap for the lower half of the values seen so far, a min-heap for the upper half, kept within one element of each other in size. The median is then O(1) to read off the two roots, at O(log n) per insert.

## References
