---
id: linked-lists
title: Linked Lists
tagline: Give up contiguity, and insertion stops being expensive.
level: basic
related: arrays, stacks-queues, merge-sort, binary-trees
---

## Note
- Insertion and deletion are **O(1) once you hold the node**. Finding that node is a separate O(n) cost — never quote the two as one number.
- No random access. `list[500]` does not exist; it is 500 hops, so indexing is O(n).
- **Singly** linked nodes point forward only. **Doubly** linked nodes also point back, which makes deletion O(1) given only the victim.
- **Poor cache locality** is the hidden cost. Scattered nodes miss the cache in a way contiguous arrays do not, so lists lose to arrays on scans despite matching complexity.
- The **dummy head** (or sentinel) node removes the "what if we are deleting the first element" special case from almost every algorithm.
- **Two pointers at different speeds** solve a surprising share of list problems: cycle detection, finding the middle, and finding the nth node from the end all fall to it in one pass.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
class Node(val value: Int, var next: Node? = null)

/** Reverses the list in place — three references stay in flight: prev, current, next. */
fun reverse(head: Node?): Node? {
    var prev: Node? = null
    var current = head
    while (current != null) {
        val next = current.next // save it before we overwrite
        current.next = prev     // flip the link
        prev = current
        current = next
    }
    return prev // the old tail is the new head
}
```

## Code: Go
```go
type Node struct {
	Value int
	Next  *Node
}

// Reverse flips the list in place. Three references stay in flight:
// what came before, what we're on, and what comes next.
func Reverse(head *Node) *Node {
	var prev *Node
	current := head
	for current != nil {
		next := current.Next // save it before we overwrite
		current.Next = prev  // flip the link
		prev = current
		current = next
	}
	return prev // the old tail is the new head
}
```

## Questions
### Reverse Linked List
id: 206
difficulty: easy
askedAt: Effectively universal
The one everyone is expected to write without hesitating. Three pointers, one pass. Be ready to give the recursive version too — interviewers often ask for both, and the recursive one is harder than it looks.

### Linked List Cycle
id: 141
difficulty: easy
askedAt: Amazon, Microsoft, Bloomberg
Floyd's tortoise and hare. A hash set of seen nodes also works and is fine to mention, but they want the O(1) space answer. The follow-up — finding where the cycle starts — needs the trick of resetting one pointer to the head.

### Merge Two Sorted Lists
id: 21
difficulty: easy
askedAt: Amazon, Apple, Adobe
The merge step of merge sort, done by relinking rather than copying. A dummy head makes it clean — without one, the code doubles in size handling which list supplies the first node.

## References
