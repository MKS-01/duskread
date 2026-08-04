---
id: sliding-window
title: Sliding Window
tagline: Slide a window's edges instead of re-scanning it from scratch.
level: intermediate
related: two-pointers, arrays, hash-tables
---

## Quick Summary
- Maintain a contiguous window and slide its edges one step at a time, reusing the previous window's work instead of recomputing from scratch — O(n) instead of O(n·k) or O(n²).
- Fixed-size windows just shift by one; variable-size windows grow the right edge greedily and shrink the left edge only when a constraint is violated.
- Lives or dies on incremental updates: whatever the window is tracking needs to be cheaply updatable as elements enter and leave, not recomputed each slide.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A lot of subarray or substring problems have an obvious brute-force shape: for every possible window start, scan forward to check every possible window end. That's O(n²) at best, or O(n·k) if the window size k is fixed, and almost all of that work is wasted — each window overlaps heavily with its neighbours, so most of what you'd recompute was already computed one step ago.

Sliding window's insight is to never throw that overlap away. Keep a window defined by a left and right edge, and instead of re-scanning it after every move, update it incrementally: when the right edge advances, add the newly included element's contribution; when the left edge advances, remove the newly excluded element's contribution. Whatever the window tracks — a running sum, a count of characters, a maximum — gets updated in O(1) per edge move rather than recomputed over the whole window.

There are two shapes this takes. A **fixed-size window** simply slides: both edges move together, one step at a time, the natural fit for "find the best window of exactly size k." A **variable-size window** is more common in interviews: the right edge advances greedily to grow the window, and the left edge only advances when the window violates some constraint — too many distinct characters, a sum that's too large — shrinking just enough to satisfy it again before continuing to grow.

The reason variable windows are still O(n) despite two nested-looking loops is that the left edge, across the entire algorithm's run, can only ever move forward and can only move at most n times total — it never resets or moves backward. Two pointers that each individually move at most n times, combined, is still O(n) overall, the same argument that makes two pointers linear rather than quadratic.

The part that actually needs care is picking a data structure for "what the window is tracking" that supports both adding and removing in O(1) or O(log n) — a running integer sum, a hash map of character counts, or a deque for a running maximum are the usual choices. Get that part wrong — recomputing a max by scanning the window every slide, say — and the window mechanism doesn't save you anything.

## Origin
Sliding window is a **technique rather than a dated, single-author invention**, in the same category as two pointers — it's the natural response to noticing that adjacent windows over the same array share almost all their contents, and versions of the idea appear throughout algorithms literature, particularly signal processing and string matching, without one clean point of origin.

## Key Points
- **Reuse the previous window's work.** Update incrementally as edges move — add what enters, remove what leaves — rather than recomputing the window's tracked value from scratch on every slide.
- **Fixed-size windows slide**; **variable-size windows grow greedily and shrink only when a constraint is violated** — the second is the more common interview shape.
- **Still O(n) overall**, even though the left edge inside a variable window looks like a second loop — it only ever moves forward, and moves at most n times total across the entire run.
- **The data structure tracking the window's state has to support O(1) or O(log n) add/remove** — a running sum, a hash map of counts, or a deque for a running max are the standard choices.
- **A hash map of character/element counts is the default state** for 'window contains at most k distinct things' or substring-matching problems specifically.

## Complexity
Fixed-size window | O(n) | O(k) or O(1) | One pass; each slide does O(1) incremental work if the tracked value supports it.
Variable-size window | O(n) | O(k) | Amortised: the left edge moves at most n times total across the whole run, same argument as two pointers.

## Pitfalls
- Recomputing the window's tracked value from scratch after every slide instead of updating incrementally — this silently turns an O(n) sliding window into an O(n·k) brute force with extra bookkeeping.
- Shrinking the left edge by more than one step, or under the wrong condition — the left edge should advance exactly until the constraint is satisfied again, not further.
- Forgetting to update the tracked state when an element leaves the window, only when one enters — a classic source of a window that silently drifts wrong after a few shrinks.
- Using an O(n) scan to find a running maximum/minimum inside the window on every slide — a monotonic deque keeps that operation O(1) amortised instead.

## Steps
1. Initialise the window's left and right edges at the start, along with whatever state tracks the window's contents.
2. Advance the right edge, adding the new element's contribution to the tracked state.
3. If the window now violates a constraint, advance the left edge — removing each excluded element's contribution — until the constraint holds again.
4. Record the window as a candidate answer if applicable, then continue advancing the right edge.

## Code: Kotlin
```kotlin
/** Fixed-size window: max sum of any k consecutive elements. */
fun maxSumWindow(nums: IntArray, k: Int): Int {
    var windowSum = nums.take(k).sum()
    var best = windowSum

    for (right in k until nums.size) {
        windowSum += nums[right] - nums[right - k] // add entering, remove leaving
        best = maxOf(best, windowSum)
    }
    return best
}

/** Variable-size window: longest substring with no repeated characters. */
fun longestUniqueSubstring(s: String): Int {
    val lastSeen = mutableMapOf<Char, Int>()
    var left = 0
    var best = 0

    for (right in s.indices) {
        val char = s[right]
        // Shrink only if the repeat is inside the current window, not before it.
        if (lastSeen.getOrDefault(char, -1) >= left) {
            left = lastSeen[char]!! + 1
        }
        lastSeen[char] = right
        best = maxOf(best, right - left + 1)
    }
    return best
}
```

## Code: Go
```go
// MaxSumWindow is a fixed-size window: max sum of any k consecutive elements.
func MaxSumWindow(nums []int, k int) int {
	windowSum := 0
	for i := 0; i < k; i++ {
		windowSum += nums[i]
	}
	best := windowSum

	for right := k; right < len(nums); right++ {
		windowSum += nums[right] - nums[right-k] // add entering, remove leaving
		if windowSum > best {
			best = windowSum
		}
	}
	return best
}

// LongestUniqueSubstring is a variable-size window: longest substring with
// no repeated characters.
func LongestUniqueSubstring(s string) int {
	lastSeen := make(map[byte]int)
	left, best := 0, 0

	for right := 0; right < len(s); right++ {
		c := s[right]
		// Shrink only if the repeat is inside the current window, not before it.
		if idx, ok := lastSeen[c]; ok && idx >= left {
			left = idx + 1
		}
		lastSeen[c] = right
		if right-left+1 > best {
			best = right - left + 1
		}
	}
	return best
}
```

## Questions
### Longest Substring Without Repeating Characters
id: 3
difficulty: medium
askedAt: Amazon, Meta, Bloomberg — extremely common
A variable window with a hash map of last-seen indices. The trap is shrinking on any previously-seen character instead of only one whose last occurrence is inside the current window — a repeat from before the window started is irrelevant.

### Minimum Window Substring
id: 76
difficulty: hard
askedAt: Meta, Uber, Google
Grow the window until it satisfies the character-count requirement, then shrink it as far as possible while it still does, recording the smallest valid window seen. Shrinking-while-valid rather than shrinking-until-valid is the subtlety that trips people up.

### Sliding Window Maximum
id: 239
difficulty: hard
askedAt: Amazon, Google, Meta
A monotonic deque keeps the current window's maximum accessible in O(1): pop smaller elements from the back before pushing a new one, since they can never be the max while the new, larger element is still in the window.

## References
