---
id: heaps
title: Heaps
tagline: A tree that only promises the top is right — that weaker promise is what makes it fast.
level: intermediate
related: arrays, binary-trees, merge-sort, heap-sort
---

## Quick Summary
- A heap only guarantees a parent is smaller (or larger) than its children — weaker than full sortedness, but enough to make the min or max O(log n) to insert and remove.
- Almost always a complete binary tree stored in a plain array — node i's children live at 2i+1 and 2i+2, no pointers needed.
- Peeking the min/max is O(1); searching for anything else is O(n) — a heap is not a search tree.
- Building a heap from an existing array is O(n), not O(n log n) — a common surprise worth being able to justify.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Sorting an entire collection just to repeatedly grab the minimum is wasteful — it pays O(n log n) up front for an order that is only ever needed at the top. A **heap** asks for less and gets more speed in return: it guarantees only that every parent is smaller (or, for a max-heap, larger) than its children, never that the whole collection is sorted. That weaker promise is still enough to make "give me the smallest" and "give me the largest" both O(log n), regardless of how many elements are inside.

The heap-order invariant says nothing about how a node's two children compare to *each other* — only that both are ≥ (or ≤) their parent. That is the entire trick: it is a far cheaper property to maintain than full sortedness, yet it is still enough to guarantee the minimum sits at the root, because every path from the root only increases (or only decreases).

Heaps are almost always **complete binary trees** — every level full except possibly the last, filled left to right — and that shape is dense enough to store with no pointers at all: node i's children live at indices `2i+1` and `2i+2` of a plain array. That is why "heap" so often means "array kept in heap order" rather than an actual pointer-based tree — smaller, faster, and simpler to implement than the tree it represents.

Insert and extract-min both work by breaking the invariant at exactly one spot and repairing it along a single path. Inserting appends the new value at the end of the array and **bubbles it up**, swapping with its parent while it is smaller than that parent. Removing the root swaps in the very last element and **sinks it down**, swapping with the smaller child until the invariant holds again. Both are O(log n), because that is the height of a complete tree holding n nodes.

The interview-relevant consequence is that a heap is the right structure whenever a problem needs the running min or max under a stream of insertions and removals, but never needs the full order — the kth largest element, merging sorted streams, or scheduling by priority. The moment a solution involves sorting something just to repeatedly look at one end of it, a heap is almost always the faster shape.

## Origin
The heap and **heapsort were invented by J.W.J. Williams**, published as **Algorithm 232 in Communications of the ACM in 1964**. Robert Floyd improved the construction step later that same year, contributing the bottom-up **heapify** that runs in O(n) and is still the standard way to build one — a rare case where a data structure and the most efficient way to construct it were separate insights, published only months apart. The name is the plain English word for a disordered pile, chosen precisely because a heap's internal order is loose — the opposite of "sorted".

## Key Points
- The **heap-order invariant** — a min-heap's parent ≤ both children — is weaker than full sortedness, and that weakness is exactly what makes insert and extract both O(log n).
- Heaps are almost always **complete binary trees stored in a plain array**: node i's children sit at `2i+1` and `2i+2`, with no pointers needed.
- **Insert bubbles up**, swapping with the parent while smaller (min-heap); **extract-min sinks down** after swapping the root with the last element, always toward the smaller child.
- **Peeking the min/max is O(1)** — it's always the root — but reading any other element, or asking whether a value is present at all, is O(n): a heap has no search structure beyond the root ordering.
- **Heapify** (building a heap from an unordered array) is O(n), not O(n log n) — it looks like n inserts at O(log n) each, but most nodes sit near the bottom and sink only a short distance, so a tighter accounting gives a linear bound.
- A **priority queue** is the abstract interface; a **binary heap** is the usual concrete implementation of it, not a synonym for it.

## Complexity
Peek min/max | O(1) | O(1) | Always the root.
Insert | O(log n) | O(1) | Bubble up at most the height of the tree.
Extract min/max | O(log n) | O(1) | Sink down after swapping in the last element.
Build heap from array | O(n) | O(1) | Bottom-up heapify — tighter than n × O(log n) despite appearances.
Search for arbitrary value | O(n) | O(1) | No structure beyond the root ordering — a heap is not a search tree.

## Pitfalls
- Expecting O(1) or even O(log n) search for an arbitrary value — a heap only orders the path to the root; anything else needs a full O(n) scan.
- Confusing 'heap' the data structure with the 'heap' memory region used for dynamic allocation — same word, entirely unrelated concepts.
- Building a heap by inserting n elements one at a time when all of them are already available — that costs O(n log n); heapify the whole array instead for O(n).
- Assuming a min-heap's second level is meaningfully ordered, e.g. that the second-smallest overall element must be one of the root's two children — it is guaranteed to be *a* descendant, not necessarily one at that exact level.
- Using a plain array with a linear scan for removal 'because it's simple' when a priority queue is needed — that is O(n) per extraction, which defeats the entire reason to reach for a heap.

## Steps
1. To insert: append the new element at the end of the array.
2. While it is smaller than its parent (min-heap), swap with the parent and repeat — this is 'bubbling up'.
3. To extract the min: save the root's value, move the last element into the root position, and shrink the array by one.
4. Sink the new root down: repeatedly swap with the smaller of its two children until it is no longer bigger than either — this is 'sinking down'.

## Code: Kotlin
```kotlin
/**
 * Array-backed min-heap. Node i's children live at 2i+1 and 2i+2 — no
 * pointers needed, because the tree is always complete.
 */
class MinHeap {
    private val items = mutableListOf<Int>()

    fun peek(): Int = items.first()

    fun insert(value: Int) {
        items += value
        bubbleUp(items.lastIndex)
    }

    fun extractMin(): Int {
        val min = items[0]
        items[0] = items[items.lastIndex]
        items.removeAt(items.lastIndex)
        if (items.isNotEmpty()) sinkDown(0)
        return min
    }

    private fun bubbleUp(start: Int) {
        var i = start
        while (i > 0) {
            val parent = (i - 1) / 2
            if (items[i] >= items[parent]) break
            items[i] = items[parent].also { items[parent] = items[i] }
            i = parent
        }
    }

    private fun sinkDown(start: Int) {
        var i = start
        while (true) {
            val left = 2 * i + 1
            val right = 2 * i + 2
            var smallest = i
            if (left < items.size && items[left] < items[smallest]) smallest = left
            if (right < items.size && items[right] < items[smallest]) smallest = right
            if (smallest == i) break
            items[i] = items[smallest].also { items[smallest] = items[i] }
            i = smallest
        }
    }
}
```

## Code: Go
```go
// MinHeap is array-backed. Node i's children live at 2i+1 and 2i+2 — no
// pointers needed, because the tree is always complete.
type MinHeap struct {
	items []int
}

func (h *MinHeap) Peek() int { return h.items[0] }

func (h *MinHeap) Insert(value int) {
	h.items = append(h.items, value)
	h.bubbleUp(len(h.items) - 1)
}

func (h *MinHeap) ExtractMin() int {
	min := h.items[0]
	last := len(h.items) - 1
	h.items[0] = h.items[last]
	h.items = h.items[:last]
	if len(h.items) > 0 {
		h.sinkDown(0)
	}
	return min
}

func (h *MinHeap) bubbleUp(i int) {
	for i > 0 {
		parent := (i - 1) / 2
		if h.items[i] >= h.items[parent] {
			break
		}
		h.items[i], h.items[parent] = h.items[parent], h.items[i]
		i = parent
	}
}

func (h *MinHeap) sinkDown(i int) {
	for {
		left, right := 2*i+1, 2*i+2
		smallest := i
		if left < len(h.items) && h.items[left] < h.items[smallest] {
			smallest = left
		}
		if right < len(h.items) && h.items[right] < h.items[smallest] {
			smallest = right
		}
		if smallest == i {
			break
		}
		h.items[i], h.items[smallest] = h.items[smallest], h.items[i]
		i = smallest
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
