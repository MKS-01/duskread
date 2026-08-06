---
id: insertion-sort
title: Insertion Sort
tagline: Grow a sorted prefix one element at a time, sliding each new value into place.
level: basic
related: arrays, linked-lists, selection-sort, bubble-sort, quicksort
---

## Note
- Grow a sorted prefix one element at a time: take the next element and slide it left past everything bigger — exactly like sorting a hand of playing cards.
- **O(n²) worst case, O(n) best case** — and, unusually for an O(n²) sort, genuinely fast in practice on *nearly*-sorted data, not just perfectly sorted data.
- **In-place, O(1) auxiliary space**, and **stable** — an element only ever slides past strictly larger neighbours, never past equal ones.
- **Adaptive**: the number of swaps is proportional to the number of *inversions* in the input, so 'almost sorted' data genuinely does less work.
- **The standard cutover target for hybrid sorts.** Quicksort and merge sort switch to insertion sort on small sub-arrays (roughly 10-20 elements), because its low overhead wins at that size.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
