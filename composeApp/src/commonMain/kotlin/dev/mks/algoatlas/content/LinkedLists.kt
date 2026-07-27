package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.linkedListScene

val LinkedLists = Topic(
    id = "linked-lists",
    title = "Linked Lists",
    tagline = "Give up contiguity, and insertion stops being expensive.",
    level = Level.BASIC,
    scene = { linkedListScene() },

    intuition = listOf(
        "Arrays are fast to read and slow to modify, and both facts come from the same source: the elements sit in one unbroken block. A linked list is the obvious question that follows — what if we simply stopped requiring that? Let each element live wherever memory happens to have room, and have it remember the address of the next one.",
        "The consequence is an exact inversion of the array's trade-offs. Inserting no longer means shifting anything, because there is nothing to shift; you point the new node at the rest of the list and point its predecessor at the new node. Two writes, done, O(1). But reading element `i` is no longer arithmetic. You hold only the head, and the only way to reach the fifth node is to walk through the first four. That is O(n), and no cleverness removes it.",
        "So the honest summary is that the *insert itself* is O(1), while *getting to the insertion point* is O(n). This distinction matters enormously and interviews test it constantly. \"Insert into a linked list\" is O(1) only if you already hold a reference to the node you are inserting after. If you have to search for it first, you have paid O(n) regardless of the structure.",
        "There is a practical caveat worth knowing beyond the complexity table. Array elements sit next to each other and arrive in the CPU cache together; linked-list nodes are scattered, so each hop is potentially a cache miss. A linear scan of an array can be several times faster than the same scan over a list of identical length, even though both are O(n). Big-O hides constant factors, and here the constant is large enough that arrays win most real workloads.",
        "Where lists genuinely shine is when you are already holding the node: LRU caches, adjacency lists, free lists in allocators, and anywhere you need to splice items between structures without copying. They are also the backbone of interview questions, because pointer manipulation is easy to get subtly wrong and therefore easy to test.",
    ),

    origin = "Linked lists were invented around **1955–56 by Allen Newell, Cliff Shaw and Herbert Simon at RAND**, as part of the list-processing language IPL for their Logic Theorist — arguably the first artificial-intelligence program. They needed to build structures whose size was not known in advance, on machines with tiny memories, and shuffling contiguous blocks around was unaffordable. Their solution was to let each cell carry a pointer to the next. The idea proved so central that John McCarthy built **Lisp** around it a couple of years later; the name is short for *list processing*, and Newell and Simon later shared a Turing Award.",

    keyPoints = listOf(
        "Insertion and deletion are **O(1) once you hold the node**. Finding that node is a separate O(n) cost — never quote the two as one number.",
        "**Singly** linked nodes point forward only. **Doubly** linked nodes also point back, which makes deletion O(1) given only the victim, at the cost of an extra pointer per node.",
        "There is no random access. `list[500]` does not exist; it is 500 hops.",
        "**Poor cache locality** is the hidden cost. Scattered nodes miss the cache in a way contiguous arrays do not, so lists lose to arrays on scans despite matching complexity.",
        "The **dummy head** (or sentinel) node is the single best trick here — it removes the \"what if we are deleting the first element\" special case from almost every algorithm.",
        "**Two pointers at different speeds** solve a surprising share of list problems: cycle detection, finding the middle, and finding the nth node from the end all fall to it in one pass.",
    ),

    steps = listOf(
        "To insert after a node you already hold: create the new node.",
        "Point the new node's `next` at the current node's `next`.",
        "Point the current node's `next` at the new node. Order matters — reverse these two and you lose the rest of the list.",
        "To delete the node after one you hold: point its `next` past the victim, to `victim.next`.",
        "In a garbage-collected language the orphaned node is now unreachable and will be collected; in C you would free it here.",
    ),

    complexity = listOf(
        ComplexityRow("Access by index", "O(n)", "O(1)", "No address arithmetic is possible — you have to walk."),
        ComplexityRow("Search", "O(n)", "O(1)", "Same walk, comparing as you go."),
        ComplexityRow("Insert / delete at head", "O(1)", "O(1)", "The head reference is always in hand."),
        ComplexityRow("Insert / delete given the node", "O(1)", "O(1)", "Pure pointer rewiring, nothing shifts."),
        ComplexityRow("Insert / delete by value", "O(n)", "O(1)", "Dominated by the search, not the write."),
        ComplexityRow("Storage", "—", "O(n)", "Each node carries a pointer, so more memory per element than an array."),
    ),

    pitfalls = listOf(
        "Rewiring pointers in the wrong order and losing the tail. Always attach the new node to the rest of the list *before* detaching anything.",
        "Forgetting the head case. Deleting the first element has no predecessor to rewire — a dummy head node makes the special case disappear.",
        "Dereferencing `next` without a null check at the tail. This is the most common crash in list code.",
        "Reversing a list without a `prev` variable. You need three references in flight — previous, current, and next — or you drop the remainder.",
        "Reaching for a linked list because insertion is \"O(1)\". If you have to search for the position first, an array is usually faster in practice thanks to cache behaviour.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
class Node(val value: Int, var next: Node? = null)

/**
 * Reverses the list in place.
 *
 * Three references are always in flight: what came before, what we are
 * looking at, and what comes next. Drop any one and the list is lost.
 */
fun reverse(head: Node?): Node? {
    var prev: Node? = null
    var current = head

    while (current != null) {
        val next = current.next  // save it before we overwrite
        current.next = prev      // flip the link
        prev = current           // advance both cursors
        current = next
    }
    return prev                  // the old tail is the new head
}

/**
 * Floyd's cycle detection. If a loop exists the fast pointer laps the
 * slow one; if it does not, fast simply runs off the end.
 */
fun hasCycle(head: Node?): Boolean {
    var slow = head
    var fast = head

    while (fast?.next != null) {
        slow = slow?.next        // one step
        fast = fast.next?.next   // two steps
        if (slow === fast) return true
    }
    return false
}

/** The dummy head removes the "deleting the first node" special case. */
fun removeValue(head: Node?, target: Int): Node? {
    val dummy = Node(0, head)
    var current: Node = dummy

    while (current.next != null) {
        if (current.next!!.value == target) {
            current.next = current.next!!.next
        } else {
            current = current.next!!
        }
    }
    return dummy.next
}
        """.trim(),

        Lang.GO to """
type Node struct {
	Value int
	Next  *Node
}

// Reverse flips the list in place. Three references are always in
// flight: what came before, what we are on, and what comes next.
func Reverse(head *Node) *Node {
	var prev *Node
	current := head

	for current != nil {
		next := current.Next // save it before we overwrite
		current.Next = prev  // flip the link
		prev = current       // advance both cursors
		current = next
	}
	return prev // the old tail is the new head
}

// HasCycle is Floyd's tortoise and hare. If a loop exists the fast
// pointer laps the slow one; otherwise fast runs off the end.
func HasCycle(head *Node) bool {
	slow, fast := head, head

	for fast != nil && fast.Next != nil {
		slow = slow.Next      // one step
		fast = fast.Next.Next // two steps
		if slow == fast {
			return true
		}
	}
	return false
}

// RemoveValue uses a dummy head so deleting the first node needs
// no special case.
func RemoveValue(head *Node, target int) *Node {
	dummy := &Node{Next: head}
	current := dummy

	for current.Next != nil {
		if current.Next.Value == target {
			current.Next = current.Next.Next
		} else {
			current = current.Next
		}
	}
	return dummy.Next
}
        """.trim(),

        Lang.JAVASCRIPT to """
class Node {
  constructor(value, next = null) {
    this.value = value;
    this.next = next;
  }
}

/**
 * Reverses the list in place. Three references are always in flight:
 * what came before, what we are on, and what comes next.
 */
function reverse(head) {
  let prev = null;
  let current = head;

  while (current !== null) {
    const next = current.next; // save it before we overwrite
    current.next = prev;       // flip the link
    prev = current;            // advance both cursors
    current = next;
  }
  return prev;                 // the old tail is the new head
}

/**
 * Floyd's cycle detection. If a loop exists the fast pointer laps
 * the slow one; if not, fast runs off the end.
 */
function hasCycle(head) {
  let slow = head;
  let fast = head;

  while (fast !== null && fast.next !== null) {
    slow = slow.next;      // one step
    fast = fast.next.next; // two steps
    if (slow === fast) return true;
  }
  return false;
}

/** The dummy head removes the "deleting the first node" special case. */
function removeValue(head, target) {
  const dummy = new Node(0, head);
  let current = dummy;

  while (current.next !== null) {
    if (current.next.value === target) current.next = current.next.next;
    else current = current.next;
  }
  return dummy.next;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 206,
            title = "Reverse Linked List",
            difficulty = Difficulty.EASY,
            idea = "The one everyone is expected to write without hesitating. Three pointers, one pass. Be ready to give the recursive version too — interviewers often ask for both, and the recursive one is harder than it looks.",
            askedAt = "Effectively universal",
        ),
        Question(
            id = 141,
            title = "Linked List Cycle",
            difficulty = Difficulty.EASY,
            idea = "Floyd's tortoise and hare. A hash set of seen nodes also works and is fine to mention, but they want the O(1) space answer. The follow-up — finding where the cycle starts — needs the trick of resetting one pointer to the head.",
            askedAt = "Amazon, Microsoft, Bloomberg",
        ),
        Question(
            id = 21,
            title = "Merge Two Sorted Lists",
            difficulty = Difficulty.EASY,
            idea = "The merge step of merge sort, done by relinking rather than copying. A dummy head makes it clean — without one, the code doubles in size handling which list supplies the first node.",
            askedAt = "Amazon, Apple, Adobe",
        ),
    ),

    related = listOf("arrays", "stacks-queues", "merge-sort"),
    references = Refs.basecs(),
)
