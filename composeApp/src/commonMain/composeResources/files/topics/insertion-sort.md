---
id: insertion-sort
title: Insertion Sort
tagline: Grow a sorted prefix one element at a time, sliding each new value into place.
level: basic
related: arrays, linked-lists, selection-sort, bubble-sort, quicksort
---

## Quick Summary
- Grow a sorted prefix one element at a time: take the next element and slide it left past everything bigger — exactly like sorting a hand of playing cards.
- O(n²) worst case, but O(n) best case on already-sorted input, and noticeably fast in practice on *nearly*-sorted data, which is rarer for other O(n²) sorts.
- The standard cutover target for hybrid sorts: quicksort and merge sort implementations switch to insertion sort on small sub-arrays, because its low overhead wins below roughly 10-20 elements.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Insertion sort is the algorithm most people already use without thinking about it: sorting a hand of playing cards by picking up each new card and sliding it into the correct place among the cards already sorted in your hand. Formalised, that means maintaining a sorted prefix of the array and, for each new element, sliding it leftward past every already-sorted element bigger than it until it lands in the right spot.

That sliding is where the cost comes from. In the worst case — a reverse-sorted array — every new element has to slide all the way to the front, past everything already placed, giving the same O(n²) bound as selection or bubble sort. But unlike those two, insertion sort's cost is genuinely sensitive to how sorted the input already is: an element that's already close to its correct position slides only a short distance, so a nearly-sorted array finishes close to O(n) rather than grinding through the full O(n²).

That input-sensitivity is precisely why insertion sort is the standard choice for small sub-arrays inside hybrid sorts. Once quicksort's or merge sort's recursion narrows down to a handful of elements, insertion sort's low constant-factor overhead — no recursive calls, no partition or merge bookkeeping — beats the asymptotically better algorithms outright, which is why production sort implementations switch over below roughly 10-20 elements rather than recursing all the way down.

It is also stable, and for the same structural reason bubble sort is: an element only ever slides past strictly larger elements, never past equal ones, so equal elements keep their relative order automatically, with no extra bookkeeping required.

## Origin
Insertion sort has **no single documented inventor**, unlike most algorithms in this curriculum — it is the formalisation of how people have manually sorted objects for as long as sorting has been a task at all, and it appears in early computing literature from the 1940s and 50s as an obvious baseline technique rather than a novel contribution. What is well documented is its practical role: it is the textbook example of an algorithm whose worst-case complexity understates its real-world usefulness, precisely because of how it behaves on nearly-sorted data.

## Key Points
- **O(n²) worst case, O(n) best case** — and, unusually for an O(n²) sort, genuinely fast in practice on *nearly*-sorted data, not just perfectly sorted data.
- **In-place, O(1) auxiliary space**, and **stable** — an element only ever slides past strictly larger neighbours, never past equal ones.
- **The standard cutover target for hybrid sorts.** Quicksort and merge sort switch to insertion sort on small sub-arrays (roughly 10-20 elements), because its low overhead wins at that size despite the worse asymptotic bound.
- **Adaptive**: the number of swaps is proportional to the number of *inversions* in the input — pairs out of order relative to each other — so 'almost sorted' data does genuinely less work, not just the same work with a smaller constant.
- Online-friendly: it can sort a stream as elements arrive, inserting each new one into the already-sorted prefix — something selection sort and simple bubble sort can't do as naturally.

## Complexity
Best case | O(n) | O(1) | Already-sorted input — every new element slides zero positions.
Average / worst case | O(n²) | O(1) | Reverse-sorted input forces every new element all the way to the front.

## Pitfalls
- Reaching for it on large, randomly-ordered data — the O(n²) worst case is real, and it will lose badly to merge sort or quicksort at scale.
- Forgetting the input-sensitivity is about *inversions*, not just 'is it sorted' — data can look unsorted overall while having few inversions (e.g. a sorted array with one element moved), and insertion sort still handles that fast.
- Implementing the inner slide with a series of full swaps instead of one shift-and-place — correct either way, but a single assignment per shifted element avoids unnecessary writes.

## Steps
1. Treat the first element as a sorted prefix of length 1.
2. Take the next element and compare it against the end of the sorted prefix.
3. Shift every element in the prefix bigger than it one slot to the right.
4. Insert the element into the gap that shift created.
5. Repeat, growing the sorted prefix by one each time, until the whole array is covered.

## Code: Kotlin
```kotlin
fun insertionSort(nums: IntArray) {
    for (i in 1 until nums.size) {
        val current = nums[i]
        var j = i - 1
        // Shift everything bigger than current one slot right to make room.
        while (j >= 0 && nums[j] > current) {
            nums[j + 1] = nums[j]
            j--
        }
        nums[j + 1] = current
    }
}
```

## Code: Go
```go
func InsertionSort(nums []int) {
	for i := 1; i < len(nums); i++ {
		current := nums[i]
		j := i - 1
		// Shift everything bigger than current one slot right to make room.
		for j >= 0 && nums[j] > current {
			nums[j+1] = nums[j]
			j--
		}
		nums[j+1] = current
	}
}
```

## Questions
### Insertion Sort List
id: 147
difficulty: medium
askedAt: Amazon, Microsoft
The same idea applied to a linked list: relink nodes into a growing sorted prefix instead of shifting array slots. The usual trap is losing the head reference — a dummy head node removes that special case.

### Sort an Array
id: 912
difficulty: medium
askedAt: The standard "implement a sort" screen
A useful contrast question: naive insertion sort fails this problem's constraints outright, which is exactly the input-size threshold where switching to merge sort or quicksort stops being optional.

## References
