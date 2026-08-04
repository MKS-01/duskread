---
id: stacks-queues
title: Stacks & Queues
tagline: Same storage, opposite ends — and that changes everything.
level: basic
scene: stackQueueScene
related: arrays, linked-lists, dfs, bfs
---

## Quick Summary
- Same storage underneath — the only difference is which end you're allowed to touch.
- **Stack = LIFO** (push/pop): the natural shape of anything nested. **Queue = FIFO** (enqueue/dequeue): the shape of anything fair.
- Both are O(1) at every operation, on the right underlying structure — no searching, no shifting.
- DFS and BFS are the same code with a stack swapped for a queue.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Arrays and linked lists answer *where* things live. Stacks and queues answer a different question: **which one do you get next?** Neither adds any storage capability the underlying structure did not already have. All they do is refuse to let you reach the middle — and that restriction is the entire value.

A **stack** hands back the most recent thing you gave it. Last in, first out. That sounds like an arbitrary rule until you notice it is exactly how nested work behaves: the innermost thing you started is the first thing that must finish. Function calls, bracket matching, undo history, backtracking out of a wrong turn — all of them are naturally last-in-first-out, which is why a stack keeps appearing whenever a problem nests.

A **queue** hands back the oldest thing instead. First in, first out — fairness, in structure form. Anything where waiting order should be honoured is a queue: a print spooler, a task pipeline, requests hitting a server, or exploring a graph outward in rings rather than plunging down one path.

The pairing is worth holding onto, because the two are the *same algorithm* in a surprising number of places. Depth-first search and breadth-first search differ only in which of these you put the frontier in. Swap the container and the traversal order changes completely, with no other edit. That is a genuinely useful thing to know when you are staring at a graph problem.

The other thing to appreciate is what the restriction buys you: **everything is O(1)**. No searching, no shifting, no index arithmetic. Push, pop, enqueue and dequeue all touch one end and nothing else. Giving up random access is what makes those guarantees possible — you trade reach for speed, which is the same bargain as almost every data structure decision.

## Origin
The stack shows up in **Alan Turing's 1946 report on the ACE**, where he described subroutine calls using the instructions *bury* and *unbury* — an intuition for saving a return address and restoring it that predates most of computing. The idea was independently formalised in Germany a decade later by **Klaus Samelson and Friedrich L. Bauer**, whose *Kellerprinzip* — "cellar principle" — was filed as a patent in **1957** for evaluating arithmetic expressions in a compiler; the two received the IEEE Computer Pioneer Award for it. The name **push-down** comes from the spring-loaded plate dispensers in cafeterias, where taking the top plate raises the next one into place — the mental picture the word *stack* still relies on. *Queue* needed no invention at all; it is simply the English word for a line of people, borrowed intact.

## Key Points
- Both are **access disciplines**, not storage. The array or list underneath is unchanged; what differs is which end you are allowed to touch.
- **Stack = LIFO.** `push`, `pop`, `peek`. The natural shape of anything nested.
- **Queue = FIFO.** `enqueue`, `dequeue`, `peek`. The natural shape of anything fair.
- All four operations are **O(1)** — on the right underlying structure. That qualifier is where the bugs live.
- **DFS and BFS are the same code** with a stack swapped for a queue. Recursion is just using the call stack instead of one you declared.
- A queue on a plain array is the classic trap: removing from the front is **O(n)** because everything shifts. Use a ring buffer, a deque, or two pointers.
- The call stack is a real stack with a real limit. Deep recursion is a **stack overflow**, which is precisely what happens when you push more frames than it holds.

## Complexity
Stack push / pop / peek | O(1) | O(1) | Amortised for an array-backed stack, because of occasional doubling.
Queue enqueue / dequeue | O(1) | O(1) | Only with a deque or ring buffer. A naive array front-removal is O(n).
Search | O(n) | O(1) | You must drain the structure to find something — if you need this, the wrong type was chosen.
Storage | — | O(n) | Plus whatever spare capacity the backing array keeps.

## Pitfalls
- Implementing a queue with `list.removeAt(0)` or JavaScript's `shift()`. Both are O(n): every remaining element slides down one place, so a loop over the queue quietly becomes O(n²).
- Popping without checking for empty. On an empty stack this is an exception or, worse in some languages, undefined behaviour returning garbage.
- Reaching for `java.util.Stack` on the JVM. It is a legacy class, synchronised on every call, and it iterates in the wrong order. Use `ArrayDeque`.
- Assuming recursion is free. Every recursive call is a real stack frame; a deep or unbalanced input overflows it. Converting to an explicit stack is the standard fix.
- Using a stack when the problem wants fairness, or a queue when it wants nesting. The traversal order flips and the bug looks like a logic error rather than a container choice.
- Forgetting that a `Deque` can do both. Calling the wrong end's method turns your queue into a stack silently, with no type error to catch it.

## Steps
1. **Stack push** — put the new element on the end you already hold a reference to. Nothing else moves.
2. **Stack pop** — take from that same end and shrink by one. The element beneath becomes the new top.
3. **Queue enqueue** — add at the back, exactly as a stack would.
4. **Queue dequeue** — take from the *front*. Doing this without a head pointer or ring buffer is what silently costs O(n).
5. **Empty check first.** Both structures are defined by having one reachable element, and neither has one when empty.

## Code: Kotlin
```kotlin
/**
 * Both structures, backed by ArrayDeque — the correct choice on the JVM for
 * each. java.util.Stack is legacy: synchronised, and it iterates bottom-up.
 */
class Stack<T> {
    private val items = ArrayDeque<T>()

    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()

    fun push(item: T) = items.addLast(item)

    /** Null rather than an exception, so callers must acknowledge empty. */
    fun pop(): T? = items.removeLastOrNull()

    fun peek(): T? = items.lastOrNull()
}

class Queue<T> {
    private val items = ArrayDeque<T>()

    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()

    fun enqueue(item: T) = items.addLast(item)

    // Removing from the front is O(1) here. On a plain List it would be
    // O(n), because every remaining element shifts down one slot.
    fun dequeue(): T? = items.removeFirstOrNull()

    fun peek(): T? = items.firstOrNull()
}

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
// Go has no stack or queue type — a slice is the idiomatic backing for a
// stack, and a ring buffer for a queue.

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

func (s *Stack[T]) Peek() (T, bool) {
	var zero T
	if len(s.items) == 0 {
		return zero, false
	}
	return s.items[len(s.items)-1], true
}

// A ring buffer keeps Dequeue at O(1). Slicing off the front instead
// (q.items = q.items[1:]) leaks the backing array indefinitely.
type Queue[T any] struct {
	items []T
	head  int
}

func (q *Queue[T]) Enqueue(item T) {
	q.items = append(q.items, item)
}

func (q *Queue[T]) Dequeue() (T, bool) {
	var zero T
	if q.head >= len(q.items) {
		return zero, false
	}

	item := q.items[q.head]
	q.items[q.head] = zero // release the reference so the GC can collect it
	q.head++

	// Compact once the dead prefix outgrows the live part.
	if q.head > len(q.items)/2 {
		q.items = append(q.items[:0], q.items[q.head:]...)
		q.head = 0
	}
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
