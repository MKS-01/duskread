---
id: sliding-window
title: Sliding Window
tagline: Slide a window's edges instead of re-scanning it from scratch.
level: intermediate
related: two-pointers, arrays, hash-tables
---

## Note
- Maintain a contiguous window and slide its edges one step at a time, reusing the previous window's work instead of recomputing from scratch — O(n) instead of O(n·k) or O(n²).
- Fixed-size windows just shift by one; variable-size windows grow the right edge greedily and shrink the left edge only when a constraint is violated.
- Lives or dies on incremental updates: whatever the window is tracking needs to be cheaply updatable as elements enter and leave, not recomputed each slide.
- Still O(n) overall, even though the left edge inside a variable window looks like a second loop — it only ever moves forward, at most n times total across the run.
- The data structure tracking the window's state has to support O(1) or O(log n) add/remove — a running sum, a hash map of counts, or a deque for a running max are the standard choices.
- A hash map of character/element counts is the default state for "window contains at most k distinct things" or substring-matching problems specifically.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Variable-size window: longest substring with no repeated characters. */
fun longestUniqueSubstring(s: String): Int {
    val lastSeen = mutableMapOf<Char, Int>()
    var left = 0
    var best = 0
    for (right in s.indices) {
        val char = s[right]
        // Shrink only if the repeat is inside the current window, not before it.
        if (lastSeen.getOrDefault(char, -1) >= left) left = lastSeen[char]!! + 1
        lastSeen[char] = right
        best = maxOf(best, right - left + 1)
    }
    return best
}
```

## Code: Go
```go
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
