---
id: stacks-queues
title: Stacks & Queues
tagline: Same storage, opposite ends — and that changes everything.
level: basic
related: arrays, linked-lists, dfs, bfs
---

## Note
- Both are **access disciplines**, not storage. The array or list underneath is unchanged; what differs is which end you are allowed to touch.
- **Stack = LIFO** (push/pop/peek): the natural shape of anything nested.
- **Queue = FIFO** (enqueue/dequeue/peek): the natural shape of anything fair.
- All four operations are **O(1)** — on the right underlying structure. That qualifier is where the bugs live.
- **DFS and BFS are the same code** with a stack swapped for a queue.
- A queue on a plain array is the classic trap: removing from the front is **O(n)** because everything shifts. Use a ring buffer, a deque, or two pointers.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** The classic use: nesting is exactly what a stack is shaped for. */
fun isBalanced(text: String): Boolean {
    val pairs = mapOf(')' to '(', ']' to '[', '}' to '{')
    val open = ArrayDeque<Char>()
    for (character in text) {
        when (character) {
            '(', '[', '{' -> open.addLast(character)
            ')', ']', '}' -> if (open.removeLastOrNull() != pairs[character]) return false
        }
    }
    return open.isEmpty()
}
```

## Code: Go
```go
// Go has no built-in stack type — a slice is the idiomatic backing.
type Stack[T any] struct {
	items []T
}

func (s *Stack[T]) Push(item T) {
	s.items = append(s.items, item)
}

func (s *Stack[T]) Pop() (T, bool) {
	var zero T
	if len(s.items) == 0 {
		return zero, false
	}
	last := len(s.items) - 1
	item := s.items[last]
	s.items = s.items[:last]
	return item, true
}
```

## Questions
### Valid Parentheses
id: 20
difficulty: easy
askedAt: Almost universal as a warm-up
The canonical demonstration that nesting wants a stack. Push every opener; on a closer, pop and check it matches. The two traps are forgetting to verify the stack is empty at the end — "(((" is unbalanced — and popping from an empty stack when a closer arrives first.

### Min Stack
id: 155
difficulty: medium
askedAt: Amazon, Bloomberg
Getting the minimum in O(1) sounds impossible until you notice you may store more than the values. Keep a second stack of minima alongside, pushed and popped in lockstep, so the current minimum is always on top. The insight is that history can be stored, not recomputed.

### Implement Queue using Stacks
id: 232
difficulty: easy
askedAt: Microsoft, Meta — a favourite for probing amortised analysis
Two stacks, one for input and one for output. Only move elements across when the output stack is empty — reversing once flips LIFO into FIFO. It looks O(n) per operation, but each element is moved at most twice, so it is **amortised O(1)**. Naming that amortised bound is the point of the question.

## References
