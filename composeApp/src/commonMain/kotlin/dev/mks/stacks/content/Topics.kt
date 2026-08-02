package dev.mks.stacks.content

import dev.mks.stacks.model.ComplexityRow
import dev.mks.stacks.model.Difficulty
import dev.mks.stacks.model.Lang
import dev.mks.stacks.model.Level
import dev.mks.stacks.model.Question
import dev.mks.stacks.model.Topic
import dev.mks.stacks.viz.arrayScene
import dev.mks.stacks.viz.bfsScene
import dev.mks.stacks.viz.binarySearchScene
import dev.mks.stacks.viz.coinChangeScene
import dev.mks.stacks.viz.dfsScene
import dev.mks.stacks.viz.hashTableScene
import dev.mks.stacks.viz.linkedListScene
import dev.mks.stacks.viz.mergeSortScene
import dev.mks.stacks.viz.stackQueueScene

/**
 * Every topic's content, in one file.
 *
 * Nine files that each held one `val Topic` used to make "add a topic" mean
 * "add a file" — a distinction with no payoff, since nothing here is loaded
 * independently or lazily. One file, one place to scan the whole curriculum.
 */

val Arrays = Topic(
    id = "arrays",
    title = "Arrays",
    tagline = "One unbroken block of memory — and everything that follows from it.",
    level = Level.BASIC,
    scene = { arrayScene() },

    quickSummary = listOf(
        "One contiguous block of equal-sized slots — access is address arithmetic, O(1), not a search.",
        "Insert or delete anywhere but the end costs O(n): everything has to shift to keep the block unbroken.",
        "Dynamic arrays grow by doubling — appending is O(1) **amortised**, not guaranteed O(1) on every call.",
        "Unmatched cache locality: a linear scan of an array beats the same scan over a linked list, despite matching Big-O.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Almost every data structure you will ever use is either an array underneath or a reaction against being one. So it is worth being precise about what an array actually is: a single contiguous block of memory, divided into equal-sized slots.",
        "Both halves of that sentence do real work. Because the slots are equal-sized, the computer knows exactly how far apart they are. Because the block is contiguous, it knows they all follow from one starting address. Put those together and finding element `i` is not a search at all — it is one multiplication and one addition: `address = base + i × size`. That is the entire reason array access is O(1), and it is why array indices start at zero: index 0 sits zero slots away from the base address.",
        "Every weakness of arrays is the same fact seen from the other side. The block cannot bend. Inserting into the middle means physically shifting everything after it to make room, and deleting means shifting everything back to close the gap — both O(n). Growing past the allocated block means asking for a bigger one and copying the whole thing across.",
        "That last point is where dynamic arrays come in — `ArrayList`, Go slices, JavaScript arrays. When they run out of room they allocate a larger block, usually double, and copy. Any single append can therefore cost O(n), but because doubling makes those copies exponentially rare, the cost spread over many appends is O(1). That is called **amortised** O(1), and it is a different claim from plain O(1): it promises the average is cheap, not that any individual call is.",
    ),

    origin = "The word predates computing entirely — an \"array\" was an ordered arrangement of troops, from the Old French *areer*, to put in order. The idea of *subscripting* one in a program arrived with **Fortran in 1957**, where John Backus's team at IBM let you write `A(I)` and have the compiler do the address arithmetic for you. Before that, programmers computed those memory offsets by hand. Zero-based indexing became the norm much later through C, where `a[i]` is defined as literally meaning \"the value at address a plus i\".",

    keyPoints = listOf(
        "Random access is O(1) because the address is **computed, not searched** — `base + i × size`.",
        "Insertion and deletion anywhere except the end are O(n), because the contiguity has to be restored by shifting.",
        "Appending to a dynamic array is **amortised** O(1). Growth by doubling makes the copies rare enough to average out; a single append can still cost O(n).",
        "Arrays have unmatched **cache locality**. Neighbouring elements share cache lines, so a linear scan of an array is dramatically faster in practice than the same scan over scattered nodes — even though both are O(n).",
        "A **two-dimensional array is still one-dimensional underneath**, laid out row by row. Iterating rows-then-columns is much faster than columns-then-rows for exactly that reason.",
        "Deleting when order does not matter: swap the victim with the last element and shrink. That turns an O(n) removal into O(1).",
    ),

    complexity = listOf(
        ComplexityRow("Access by index", "O(1)", "O(1)", "One multiply and one add — no comparison involved."),
        ComplexityRow("Search (unsorted)", "O(n)", "O(1)", "No structure to exploit, so every element may need checking."),
        ComplexityRow("Insert / delete at end", "O(1) amortised", "O(1)", "Occasionally O(n) when the block has to grow and be copied."),
        ComplexityRow("Insert / delete at front", "O(n)", "O(1)", "Every following element shifts by one slot."),
        ComplexityRow("Storage", "—", "O(n)", "Dynamic arrays over-allocate, so real usage is typically 1–2× the element count."),
    ),

    pitfalls = listOf(
        "Removing elements inside a forward loop. Each removal shifts everything left, so the loop skips the next element. Iterate backwards, or build a new array.",
        "Treating amortised O(1) as a latency guarantee. In a real-time path, the one append that triggers a resize is the one that misses your deadline.",
        "Building a list by repeatedly prepending. That is O(n²) overall — append and reverse at the end instead.",
        "Iterating a 2D array column-major. Same complexity, several times slower, because every step jumps a full row in memory and misses the cache.",
        "Assuming JavaScript arrays are arrays. They are objects with integer-ish keys, and holes or non-numeric keys quietly demote them to a dictionary representation.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
// Access is address arithmetic — no search, no comparison.
val nums = intArrayOf(7, 12, 19, 26, 33)
val third = nums[2]   // base + 2 * 4 bytes

/**
 * Removes the element at [index] while preserving order.
 * Everything to the right shifts left by one, so this is O(n).
 */
fun removeAt(nums: MutableList<Int>, index: Int) {
    for (i in index until nums.lastIndex) {
        nums[i] = nums[i + 1]
    }
    nums.removeAt(nums.lastIndex)
}

/**
 * Removes in O(1) by swapping the victim with the last element.
 * Only valid when the ordering does not matter.
 */
fun removeUnordered(nums: MutableList<Int>, index: Int) {
    nums[index] = nums[nums.lastIndex]
    nums.removeAt(nums.lastIndex)
}

/** Row-major iteration: neighbours in memory, so the cache stays warm. */
fun sumGrid(grid: Array<IntArray>): Long {
    var total = 0L
    for (row in grid) {
        for (value in row) total += value
    }
    return total
}
        """.trim(),

        Lang.GO to """
// Access is address arithmetic — no search, no comparison.
nums := []int{7, 12, 19, 26, 33}
third := nums[2] // base + 2 * 8 bytes

// RemoveAt deletes index while preserving order. Everything to the
// right shifts left by one, so this is O(n).
func RemoveAt(nums []int, index int) []int {
	return append(nums[:index], nums[index+1:]...)
}

// RemoveUnordered deletes in O(1) by swapping in the last element.
// Only valid when the ordering does not matter.
func RemoveUnordered(nums []int, index int) []int {
	nums[index] = nums[len(nums)-1]
	return nums[:len(nums)-1]
}

// Preallocating capacity avoids the repeated grow-and-copy cycle
// entirely when the final size is known up front.
func Squares(n int) []int {
	out := make([]int, 0, n) // len 0, cap n
	for i := 0; i < n; i++ {
		out = append(out, i*i)
	}
	return out
}
        """.trim(),

        Lang.JAVASCRIPT to """
// Access is address arithmetic — no search, no comparison.
const nums = [7, 12, 19, 26, 33];
const third = nums[2];

/**
 * Removes the element at index while preserving order.
 * splice shifts everything to the right, so this is O(n).
 */
function removeAt(nums, index) {
  nums.splice(index, 1);
  return nums;
}

/**
 * Removes in O(1) by swapping in the last element.
 * Only valid when the ordering does not matter.
 */
function removeUnordered(nums, index) {
  nums[index] = nums[nums.length - 1];
  nums.pop();
  return nums;
}

/**
 * Filtering backwards is safe: removing an element only shifts
 * indices that have already been visited.
 */
function removeEvens(nums) {
  for (let i = nums.length - 1; i >= 0; i--) {
    if (nums[i] % 2 === 0) nums.splice(i, 1);
  }
  return nums;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 27,
            title = "Remove Element",
            difficulty = Difficulty.EASY,
            idea = "The swap-with-last trick, or a two-pointer write cursor. The real lesson is that you never need a second array — one pointer reads while another writes, and the writer only advances on keepers.",
            askedAt = "Warm-up screens everywhere",
        ),
        Question(
            id = 238,
            title = "Product of Array Except Self",
            difficulty = Difficulty.MEDIUM,
            idea = "Division is banned, so the trick is two passes: one accumulating products from the left, one from the right. Store the left pass in the output array itself and the right pass in a single running variable to hit O(1) extra space.",
            askedAt = "Amazon, Meta, Apple",
        ),
        Question(
            id = 189,
            title = "Rotate Array",
            difficulty = Difficulty.MEDIUM,
            idea = "The in-place solution is beautiful and almost impossible to guess cold: reverse the whole array, then reverse the first k, then reverse the rest. Worth memorising as a technique, not as a one-off.",
            askedAt = "Microsoft, Amazon",
        ),
    ),

    related = listOf("linked-lists", "stacks-queues", "binary-search", "hash-tables"),

    references = Refs.basecs(),
)

val LinkedLists = Topic(
    id = "linked-lists",
    title = "Linked Lists",
    tagline = "Give up contiguity, and insertion stops being expensive.",
    level = Level.BASIC,
    scene = { linkedListScene() },

    quickSummary = listOf(
        "Give up contiguity: each node just points to the next, so insertion is O(1) once you already hold the node.",
        "No random access — reaching element `i` means walking `i` hops, so indexing is O(n).",
        "Scattered nodes miss the CPU cache, so lists lose to arrays on scans despite matching Big-O.",
        "The dummy head node removes the \"deleting the first element\" special case almost everywhere.",
    ),
    readMore = Refs.BasecsHome,

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

val StacksQueues = Topic(
    id = "stacks-queues",
    title = "Stacks & Queues",
    tagline = "Same storage, opposite ends — and that changes everything.",
    level = Level.BASIC,
    scene = { stackQueueScene() },

    quickSummary = listOf(
        "Same storage underneath — the only difference is which end you're allowed to touch.",
        "**Stack = LIFO** (push/pop): the natural shape of anything nested. **Queue = FIFO** (enqueue/dequeue): the shape of anything fair.",
        "Both are O(1) at every operation, on the right underlying structure — no searching, no shifting.",
        "DFS and BFS are the same code with a stack swapped for a queue.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Arrays and linked lists answer *where* things live. Stacks and queues answer a different question: **which one do you get next?** Neither adds any storage capability the underlying structure did not already have. All they do is refuse to let you reach the middle — and that restriction is the entire value.",
        "A **stack** hands back the most recent thing you gave it. Last in, first out. That sounds like an arbitrary rule until you notice it is exactly how nested work behaves: the innermost thing you started is the first thing that must finish. Function calls, bracket matching, undo history, backtracking out of a wrong turn — all of them are naturally last-in-first-out, which is why a stack keeps appearing whenever a problem nests.",
        "A **queue** hands back the oldest thing instead. First in, first out — fairness, in structure form. Anything where waiting order should be honoured is a queue: a print spooler, a task pipeline, requests hitting a server, or exploring a graph outward in rings rather than plunging down one path.",
        "The pairing is worth holding onto, because the two are the *same algorithm* in a surprising number of places. Depth-first search and breadth-first search differ only in which of these you put the frontier in. Swap the container and the traversal order changes completely, with no other edit. That is a genuinely useful thing to know when you are staring at a graph problem.",
        "The other thing to appreciate is what the restriction buys you: **everything is O(1)**. No searching, no shifting, no index arithmetic. Push, pop, enqueue and dequeue all touch one end and nothing else. Giving up random access is what makes those guarantees possible — you trade reach for speed, which is the same bargain as almost every data structure decision.",
    ),

    origin = "The stack shows up in **Alan Turing's 1946 report on the ACE**, where he described subroutine calls using the instructions *bury* and *unbury* — an intuition for saving a return address and restoring it that predates most of computing. The idea was independently formalised in Germany a decade later by **Klaus Samelson and Friedrich L. Bauer**, whose *Kellerprinzip* — \"cellar principle\" — was filed as a patent in **1957** for evaluating arithmetic expressions in a compiler; the two received the IEEE Computer Pioneer Award for it. The name **push-down** comes from the spring-loaded plate dispensers in cafeterias, where taking the top plate raises the next one into place — the mental picture the word *stack* still relies on. *Queue* needed no invention at all; it is simply the English word for a line of people, borrowed intact.",

    keyPoints = listOf(
        "Both are **access disciplines**, not storage. The array or list underneath is unchanged; what differs is which end you are allowed to touch.",
        "**Stack = LIFO.** `push`, `pop`, `peek`. The natural shape of anything nested.",
        "**Queue = FIFO.** `enqueue`, `dequeue`, `peek`. The natural shape of anything fair.",
        "All four operations are **O(1)** — on the right underlying structure. That qualifier is where the bugs live.",
        "**DFS and BFS are the same code** with a stack swapped for a queue. Recursion is just using the call stack instead of one you declared.",
        "A queue on a plain array is the classic trap: removing from the front is **O(n)** because everything shifts. Use a ring buffer, a deque, or two pointers.",
        "The call stack is a real stack with a real limit. Deep recursion is a **stack overflow**, which is precisely what happens when you push more frames than it holds.",
    ),

    complexity = listOf(
        ComplexityRow("Stack push / pop / peek", "O(1)", "O(1)", "Amortised for an array-backed stack, because of occasional doubling."),
        ComplexityRow("Queue enqueue / dequeue", "O(1)", "O(1)", "Only with a deque or ring buffer. A naive array front-removal is O(n)."),
        ComplexityRow("Search", "O(n)", "O(1)", "You must drain the structure to find something — if you need this, the wrong type was chosen."),
        ComplexityRow("Storage", "—", "O(n)", "Plus whatever spare capacity the backing array keeps."),
    ),

    pitfalls = listOf(
        "Implementing a queue with `list.removeAt(0)` or JavaScript's `shift()`. Both are O(n): every remaining element slides down one place, so a loop over the queue quietly becomes O(n²).",
        "Popping without checking for empty. On an empty stack this is an exception or, worse in some languages, undefined behaviour returning garbage.",
        "Reaching for `java.util.Stack` on the JVM. It is a legacy class, synchronised on every call, and it iterates in the wrong order. Use `ArrayDeque`.",
        "Assuming recursion is free. Every recursive call is a real stack frame; a deep or unbalanced input overflows it. Converting to an explicit stack is the standard fix.",
        "Using a stack when the problem wants fairness, or a queue when it wants nesting. The traversal order flips and the bug looks like a logic error rather than a container choice.",
        "Forgetting that a `Deque` can do both. Calling the wrong end's method turns your queue into a stack silently, with no type error to catch it.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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
        """.trim(),

        Lang.GO to """
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
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * An array is a fine stack in JavaScript — push and pop both work on the
 * cheap end. It is a poor queue, because shift() is O(n).
 */
class Stack {
  #items = [];

  get size() { return this.#items.length; }
  isEmpty() { return this.#items.length === 0; }

  push(item) { this.#items.push(item); return this; }
  pop() { return this.#items.pop(); }
  peek() { return this.#items.at(-1); }
}

/**
 * A head index instead of shift(). Dequeue stays O(1) rather than sliding
 * every remaining element down one place on every call.
 */
class Queue {
  #items = [];
  #head = 0;

  get size() { return this.#items.length - this.#head; }
  isEmpty() { return this.size === 0; }

  enqueue(item) { this.#items.push(item); return this; }

  dequeue() {
    if (this.isEmpty()) return undefined;

    const item = this.#items[this.#head];
    this.#items[this.#head] = undefined; // let the GC reclaim it
    this.#head++;

    // Compact once the dead prefix outgrows the live part.
    if (this.#head > this.#items.length / 2) {
      this.#items = this.#items.slice(this.#head);
      this.#head = 0;
    }
    return item;
  }

  peek() { return this.#items[this.#head]; }
}

/** The classic use: nesting is exactly what a stack is shaped for. */
function isBalanced(text) {
  const pairs = { ")": "(", "]": "[", "}": "{" };
  const open = [];

  for (const character of text) {
    if ("([{".includes(character)) open.push(character);
    else if (character in pairs && open.pop() !== pairs[character]) return false;
  }
  return open.length === 0;
}
        """.trim(),
    ),

    steps = listOf(
        "**Stack push** — put the new element on the end you already hold a reference to. Nothing else moves.",
        "**Stack pop** — take from that same end and shrink by one. The element beneath becomes the new top.",
        "**Queue enqueue** — add at the back, exactly as a stack would.",
        "**Queue dequeue** — take from the *front*. Doing this without a head pointer or ring buffer is what silently costs O(n).",
        "**Empty check first.** Both structures are defined by having one reachable element, and neither has one when empty.",
    ),

    questions = listOf(
        Question(
            id = 20,
            title = "Valid Parentheses",
            difficulty = Difficulty.EASY,
            idea = "The canonical demonstration that nesting wants a stack. Push every opener; on a closer, pop and check it matches. The two traps are forgetting to verify the stack is empty at the end — \"(((\" is unbalanced — and popping from an empty stack when a closer arrives first.",
            askedAt = "Almost universal as a warm-up",
        ),
        Question(
            id = 155,
            title = "Min Stack",
            difficulty = Difficulty.MEDIUM,
            idea = "Getting the minimum in O(1) sounds impossible until you notice you may store more than the values. Keep a second stack of minima alongside, pushed and popped in lockstep, so the current minimum is always on top. The insight is that history can be stored, not recomputed.",
            askedAt = "Amazon, Bloomberg",
        ),
        Question(
            id = 232,
            title = "Implement Queue using Stacks",
            difficulty = Difficulty.EASY,
            idea = "Two stacks, one for input and one for output. Only move elements across when the output stack is empty — reversing once flips LIFO into FIFO. It looks O(n) per operation, but each element is moved at most twice, so it is **amortised O(1)**. Naming that amortised bound is the point of the question.",
            askedAt = "Microsoft, Meta — a favourite for probing amortised analysis",
        ),
    ),

    related = listOf("arrays", "linked-lists", "dfs", "bfs"),
    references = Refs.basecs(),
)

val HashTables = Topic(
    id = "hash-tables",
    title = "Hash Tables",
    tagline = "Let the key compute its own address.",
    level = Level.INTERMEDIATE,
    scene = { hashTableScene() },

    quickSummary = listOf(
        "Let the key compute its own bucket instead of scanning or sorting — O(1) average for insert, lookup and delete.",
        "Collisions are a mathematical certainty, not a bug — chaining or open addressing decide how they're handled.",
        "Crossing a load factor of roughly 0.75 triggers a resize; amortises to O(1), but the worst case is O(n).",
        "No ordering guarantee — reach for a tree-backed map when iteration order matters.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Arrays gave us O(1) access, but only if you know the *index*. Usually you do not — you know a username, a URL, a word. The obvious approach is to scan until you find it, which is O(n), or to keep things sorted and binary search, which is O(log n). A hash table asks a better question: what if the key could tell us its own index?",
        "That is all a hash function is. Feed it a key, get back a number, take that number modulo the table size, and you have a bucket. No searching happened — the location was computed. Insert, lookup and delete all become O(1), which is genuinely remarkable given we started with arbitrary strings.",
        "The catch is unavoidable and it is worth being precise about why. There are infinitely many possible keys and only finitely many buckets, so some pair of distinct keys must land in the same bucket. This is the pigeonhole principle, and it means **collisions are not a bug to be engineered away — they are a mathematical certainty**. The entire design problem is what to do when one happens.",
        "The common answer is **chaining**: each bucket holds a small list, and colliding keys join it. Lookups then cost one hash plus a short walk. The alternative is **open addressing** — when a bucket is taken, probe onward for the next free slot. Chaining is simpler and degrades gracefully; open addressing is more cache-friendly because everything stays in one block, which is why high-performance implementations tend to prefer it.",
        "This is also where the O(1) claim earns its asterisk. It is an *average*, and it holds only while chains stay short. If every key collides, the table degenerates into one long list and every operation becomes O(n). Real implementations defend this by tracking the **load factor** — elements divided by buckets — and rehashing everything into a larger table when it crosses a threshold, typically around 0.75.",
        "Two consequences follow that interviews probe. First, hash tables have **no order**; if you need sorted iteration you want a tree-backed map instead. Second, the quality of your hash function is a performance decision, and for anything exposed to user input it is a **security** one too — attackers who can force collisions can turn your O(1) service into an O(n) one on purpose.",
    ),

    origin = "The idea came from **Hans Peter Luhn at IBM in an internal memo in January 1953**, while working on the problem of finding records fast without scanning them all. \"Hashing\" was borrowed from the kitchen sense of the word — to chop up and mix — because a good hash function scatters and recombines the bits of a key until the output looks nothing like the input. Luhn also invented the checksum algorithm that still validates every credit-card number you type. **Open addressing** followed shortly at IBM through Gene Amdahl and colleagues, and Donald Knuth's later analysis in *The Art of Computer Programming* turned the load-factor mathematics into the engineering rule that implementations still follow.",

    keyPoints = listOf(
        "Average O(1) for insert, lookup and delete. The **worst case is O(n)** when everything collides — the average is a statement about good hash distribution, not a guarantee.",
        "**Collisions are inevitable**, not a design flaw: infinitely many keys, finitely many buckets.",
        "**Chaining** puts a list in each bucket. **Open addressing** probes for the next free slot. Chaining is simpler; open addressing is faster in cache terms.",
        "The **load factor** (elements ÷ buckets) drives resizing. Crossing roughly 0.75 triggers a rehash into a bigger table, which is O(n) but rare enough to amortise away.",
        "Keys must be **immutable** while stored. Mutating a key changes its hash, and the entry becomes unreachable in a bucket it no longer belongs to.",
        "If two objects are equal they **must** hash equal. Overriding `equals` without `hashCode` is the classic Java/Kotlin bug — the object goes into the map and can never be found again.",
        "**No ordering guarantee.** Never rely on iteration order; use a tree map or a linked variant when order matters.",
    ),

    complexity = listOf(
        ComplexityRow("Insert / lookup / delete", "O(1) average", "O(1)", "Assumes a well-distributed hash and a controlled load factor."),
        ComplexityRow("Same, worst case", "O(n)", "O(1)", "All keys in one bucket — the table has become a linked list."),
        ComplexityRow("Resize / rehash", "O(n)", "O(n)", "Every key is rehashed into the new table. Amortises to O(1) per insert."),
        ComplexityRow("Iteration", "O(n + b)", "O(1)", "Elements plus buckets — a sparse table costs time to walk even when nearly empty."),
        ComplexityRow("Storage", "—", "O(n)", "Plus the empty buckets kept spare to hold the load factor down."),
    ),

    pitfalls = listOf(
        "Overriding `equals` but not `hashCode`. The object lands in one bucket and is looked up in another, so it is silently unfindable.",
        "Using a mutable object as a key and then mutating it. Same failure, harder to spot.",
        "Assuming iteration order is stable. It is not, and it can change between runs, versions, or after a resize.",
        "Trusting O(1) on adversarial input. If users control the keys, they can force collisions and degrade the table deliberately — a real denial-of-service vector.",
        "Writing a hash function that only samples part of the key, such as the first few characters. Any shared prefix then collides.",
        "Reaching for a hash table when the key space is small and dense. If your keys are integers 0..1000, a plain array is faster and simpler.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/**
 * A hash table with separate chaining, written out to show the moving
 * parts. In production use the standard HashMap.
 */
class SimpleHashMap<K, V>(initialCapacity: Int = 16) {
    private data class Entry<K, V>(val key: K, var value: V)

    private var buckets = Array(initialCapacity) { mutableListOf<Entry<K, V>>() }
    private var size = 0

    /** Above this fill ratio, chains get long enough to hurt lookups. */
    private val loadFactorLimit = 0.75

    private fun bucketFor(key: K): MutableList<Entry<K, V>> {
        // Guard against negative hashes and Int.MIN_VALUE.
        val index = (key.hashCode().toLong() and 0x7fffffffL) % buckets.size
        return buckets[index.toInt()]
    }

    operator fun set(key: K, value: V) {
        val bucket = bucketFor(key)
        val existing = bucket.firstOrNull { it.key == key }

        if (existing != null) {
            existing.value = value   // same key: overwrite, do not append
            return
        }

        bucket += Entry(key, value)
        size++
        if (size.toDouble() / buckets.size > loadFactorLimit) resize()
    }

    operator fun get(key: K): V? = bucketFor(key).firstOrNull { it.key == key }?.value

    fun remove(key: K): Boolean {
        val bucket = bucketFor(key)
        val removed = bucket.removeAll { it.key == key }
        if (removed) size--
        return removed
    }

    /** O(n), but doubling makes it rare enough to amortise to O(1) per insert. */
    private fun resize() {
        val old = buckets
        buckets = Array(old.size * 2) { mutableListOf() }
        size = 0
        for (bucket in old) {
            for (entry in bucket) set(entry.key, entry.value)
        }
    }
}
        """.trim(),

        Lang.GO to """
// SimpleHashMap is a hash table with separate chaining, written out to
// show the moving parts. In production use the built-in map.
type entry struct {
	key   string
	value int
}

type SimpleHashMap struct {
	buckets [][]entry
	size    int
}

const loadFactorLimit = 0.75

func NewHashMap(capacity int) *SimpleHashMap {
	return &SimpleHashMap{buckets: make([][]entry, capacity)}
}

// FNV-1a: cheap, and it mixes every byte of the key.
func hash(key string) uint32 {
	var h uint32 = 2166136261
	for i := 0; i < len(key); i++ {
		h ^= uint32(key[i])
		h *= 16777619
	}
	return h
}

func (m *SimpleHashMap) index(key string) int {
	return int(hash(key) % uint32(len(m.buckets)))
}

func (m *SimpleHashMap) Set(key string, value int) {
	i := m.index(key)
	for j := range m.buckets[i] {
		if m.buckets[i][j].key == key {
			m.buckets[i][j].value = value // overwrite, do not append
			return
		}
	}

	m.buckets[i] = append(m.buckets[i], entry{key, value})
	m.size++
	if float64(m.size)/float64(len(m.buckets)) > loadFactorLimit {
		m.resize()
	}
}

func (m *SimpleHashMap) Get(key string) (int, bool) {
	for _, e := range m.buckets[m.index(key)] {
		if e.key == key {
			return e.value, true
		}
	}
	return 0, false
}

// O(n), but doubling makes it rare enough to amortise to O(1) per insert.
func (m *SimpleHashMap) resize() {
	old := m.buckets
	m.buckets = make([][]entry, len(old)*2)
	m.size = 0
	for _, bucket := range old {
		for _, e := range bucket {
			m.Set(e.key, e.value)
		}
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * A hash table with separate chaining, written out to show the moving
 * parts. In production use Map.
 */
class SimpleHashMap {
  #buckets;
  #size = 0;
  static #LOAD_FACTOR_LIMIT = 0.75;

  constructor(capacity = 16) {
    this.#buckets = Array.from({ length: capacity }, () => []);
  }

  // FNV-1a: cheap, and it mixes every character of the key.
  #hash(key) {
    let h = 2166136261;
    const text = String(key);
    for (let i = 0; i < text.length; i++) {
      h ^= text.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return (h >>> 0) % this.#buckets.length;
  }

  set(key, value) {
    const bucket = this.#buckets[this.#hash(key)];
    const existing = bucket.find((e) => e.key === key);

    if (existing) {
      existing.value = value; // overwrite, do not append
      return this;
    }

    bucket.push({ key, value });
    this.#size++;
    if (this.#size / this.#buckets.length > SimpleHashMap.#LOAD_FACTOR_LIMIT) {
      this.#resize();
    }
    return this;
  }

  get(key) {
    return this.#buckets[this.#hash(key)].find((e) => e.key === key)?.value;
  }

  // O(n), but doubling makes it rare enough to amortise to O(1) per insert.
  #resize() {
    const old = this.#buckets;
    this.#buckets = Array.from({ length: old.length * 2 }, () => []);
    this.#size = 0;
    for (const bucket of old) {
      for (const { key, value } of bucket) this.set(key, value);
    }
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 1,
            title = "Two Sum",
            difficulty = Difficulty.EASY,
            idea = "The canonical demonstration of what hash tables buy you: one pass, storing each value's index as you go and asking whether the complement has already been seen. Turns the obvious O(n²) double loop into O(n).",
            askedAt = "The single most asked interview question",
        ),
        Question(
            id = 49,
            title = "Group Anagrams",
            difficulty = Difficulty.MEDIUM,
            idea = "The insight is designing the key, not the lookup. Anagrams share a sorted-letter signature, so use that as the key. For a better answer, a 26-length character count avoids the O(k log k) sort per word.",
            askedAt = "Amazon, Meta, Uber",
        ),
        Question(
            id = 146,
            title = "LRU Cache",
            difficulty = Difficulty.MEDIUM,
            idea = "The problem that forces you to combine two structures: a hash map for O(1) lookup, plus a doubly linked list for O(1) eviction of the oldest entry. Neither alone can do both — this is why you learn them together.",
            askedAt = "Amazon, Meta, Microsoft — a design-flavoured favourite",
        ),
    ),

    related = listOf("arrays", "linked-lists"),
    references = Refs.basecs(),
)

val BinarySearch = Topic(
    id = "binary-search",
    title = "Binary Search",
    tagline = "Halve the search space on every comparison.",
    level = Level.BASIC,
    scene = { binarySearchScene() },

    quickSummary = listOf(
        "Halve the search space on every comparison — O(log n) instead of O(n), but only on sorted data.",
        "Reframe it as finding the boundary of a monotonic predicate, not just a value in an array — that unlocks 'search the answer' problems.",
        "Write `lo + (hi - lo) / 2`, never `(lo + hi) / 2` — the latter can silently overflow.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Binary search is the pay-off for keeping data sorted. One comparison against the middle element tells you which half the answer cannot be in, so you throw that half away and repeat. Ten elements takes four comparisons; a billion takes thirty.",
        "The mental model that generalises best is not \"find a value in an array\" — it is \"find the boundary in a monotonic predicate\". Imagine mapping every index to true or false, where the sequence looks like false, false, false, true, true. Binary search finds the first true. Once you can phrase a problem that way, it does not matter whether you are searching an array, a range of answers, or a rotated list.",
        "That reframing is what turns binary search from a library call into an interview weapon. \"Koko eating bananas\" has no sorted array anywhere in the statement, but \"can she finish at speed k?\" is monotonic in k — false for small speeds, true for large ones — so you binary search the answer itself.",
    ),

    keyPoints = listOf(
        "The input must be **sorted** with respect to whatever you are comparing — otherwise halving is unjustified.",
        "Write `mid = lo + (hi - lo) / 2`, not `(lo + hi) / 2`. The latter overflows once `lo + hi` exceeds `Int.MAX_VALUE`, a bug that sat in the JDK for nine years.",
        "Prefer the **lower-bound** form (half-open range, `lo < hi`, no equality check) as your default. It returns the insertion point, handles duplicates, and has no special case for \"not found\".",
        "Every loop iteration must strictly shrink the range, or you spin forever. Check that each branch either raises `lo` or lowers `hi`.",
        "Binary search on the *answer* applies whenever a predicate is monotonic, even with no array in sight.",
    ),

    steps = listOf(
        "Set `lo = 0` and `hi = n - 1`, making the whole array live.",
        "While `lo <= hi`, compute `mid` without overflowing.",
        "If `nums[mid] == target`, you are done — return `mid`.",
        "If `nums[mid] < target`, the target must be to the right, so set `lo = mid + 1`.",
        "Otherwise the target is to the left, so set `hi = mid - 1`.",
        "If the loop exits, the range is empty and the target is absent. `lo` now holds the index where it would be inserted.",
    ),

    complexity = listOf(
        ComplexityRow("Search", "O(log n)", "O(1)", "Iterative form. The range halves each step, so at most ⌈log₂ n⌉ + 1 comparisons."),
        ComplexityRow("Search (recursive)", "O(log n)", "O(log n)", "Call stack depth. Prefer the iterative form unless the recursion reads better."),
        ComplexityRow("Sorting first", "O(n log n)", "O(1)–O(n)", "If you sort only to search once, a linear scan at O(n) is cheaper."),
    ),

    pitfalls = listOf(
        "Using `(lo + hi) / 2` on large ranges — silent integer overflow, negative index, crash.",
        "Mixing an inclusive `hi = n - 1` with a half-open loop condition `lo < hi`, or vice versa. Pick one convention and keep it consistent.",
        "Writing `lo = mid` instead of `lo = mid + 1`, which makes no progress when `hi == lo + 1` and hangs the loop.",
        "Assuming a returned index is unique when the array holds duplicates — plain binary search may return any matching index.",
        "Applying it to data that is only *nearly* sorted. Any inversion breaks the halving argument.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Returns the index of [target] in the sorted array, or -1 if absent. */
fun binarySearch(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.lastIndex          // inclusive

    while (lo <= hi) {
        val mid = lo + (hi - lo) / 2 // never overflows
        when {
            nums[mid] == target -> return mid
            nums[mid] < target  -> lo = mid + 1
            else                -> hi = mid - 1
        }
    }
    return -1
}

/**
 * The form worth memorising: the first index whose value is >= target.
 * Returns nums.size when every element is smaller, which is exactly the
 * insertion point that keeps the array sorted.
 */
fun lowerBound(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.size               // exclusive

    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (nums[mid] < target) lo = mid + 1 else hi = mid
    }
    return lo
}

/** Binary search over an answer range, given a monotonic predicate. */
fun firstTrue(lo: Int, hi: Int, predicate: (Int) -> Boolean): Int {
    var low = lo
    var high = hi
    while (low < high) {
        val mid = low + (high - low) / 2
        if (predicate(mid)) high = mid else low = mid + 1
    }
    return low
}
        """.trim(),

        Lang.GO to """
// BinarySearch returns the index of target in the sorted slice, or -1.
func BinarySearch(nums []int, target int) int {
	lo, hi := 0, len(nums)-1 // hi inclusive

	for lo <= hi {
		mid := lo + (hi-lo)/2 // never overflows
		switch {
		case nums[mid] == target:
			return mid
		case nums[mid] < target:
			lo = mid + 1
		default:
			hi = mid - 1
		}
	}
	return -1
}

// LowerBound returns the first index whose value is >= target, or len(nums)
// if every element is smaller. This is the insertion point.
func LowerBound(nums []int, target int) int {
	lo, hi := 0, len(nums) // hi exclusive

	for lo < hi {
		mid := lo + (hi-lo)/2
		if nums[mid] < target {
			lo = mid + 1
		} else {
			hi = mid
		}
	}
	return lo
}

// FirstTrue binary searches an answer range with a monotonic predicate.
// The standard library also offers sort.Search, which does exactly this.
func FirstTrue(lo, hi int, predicate func(int) bool) int {
	for lo < hi {
		mid := lo + (hi-lo)/2
		if predicate(mid) {
			hi = mid
		} else {
			lo = mid + 1
		}
	}
	return lo
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Returns the index of target in the sorted array, or -1 if absent. */
function binarySearch(nums, target) {
  let lo = 0;
  let hi = nums.length - 1;          // inclusive

  while (lo <= hi) {
    const mid = (lo + hi) >>> 1;     // unsigned shift, safe below 2^31
    if (nums[mid] === target) return mid;
    if (nums[mid] < target) lo = mid + 1;
    else hi = mid - 1;
  }
  return -1;
}

/**
 * First index whose value is >= target, or nums.length if none.
 * This is the insertion point that keeps the array sorted.
 */
function lowerBound(nums, target) {
  let lo = 0;
  let hi = nums.length;              // exclusive

  while (lo < hi) {
    const mid = (lo + hi) >>> 1;
    if (nums[mid] < target) lo = mid + 1;
    else hi = mid;
  }
  return lo;
}

/** Binary search over an answer range with a monotonic predicate. */
function firstTrue(lo, hi, predicate) {
  while (lo < hi) {
    const mid = Math.floor(lo + (hi - lo) / 2);
    if (predicate(mid)) hi = mid;
    else lo = mid + 1;
  }
  return lo;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 704,
            title = "Binary Search",
            difficulty = Difficulty.EASY,
            idea = "The template itself. Worth writing from memory until the boundary conditions stop needing thought.",
            askedAt = "Warm-up at almost every company",
        ),
        Question(
            id = 33,
            title = "Search in Rotated Sorted Array",
            difficulty = Difficulty.MEDIUM,
            idea = "A rotated array still has one sorted half at every step. Work out which half is sorted by comparing nums[lo] with nums[mid], then check whether the target lies inside that half's range — if it does, search there, otherwise search the other side.",
            askedAt = "Amazon, Meta, Microsoft",
        ),
        Question(
            id = 875,
            title = "Koko Eating Bananas",
            difficulty = Difficulty.MEDIUM,
            idea = "There is no sorted array here. Binary search the answer: \"can Koko finish at speed k?\" is false for small k and true for all larger k, so search that predicate over 1..max(piles) for the first true.",
            askedAt = "Google, Meta — the classic binary-search-on-answer test",
        ),
    ),

    related = listOf("arrays", "merge-sort"),
    references = Refs.basecs(),
)

val MergeSort = Topic(
    id = "merge-sort",
    title = "Merge Sort",
    tagline = "Divide until trivial, then merge sorted runs back together.",
    level = Level.INTERMEDIATE,
    scene = { mergeSortScene() },

    quickSummary = listOf(
        "Split until trivial, then merge sorted runs back together — O(n log n) in every case, no adversarial input.",
        "Needs O(n) scratch space for arrays; on a linked list it needs almost none, since splitting and merging are free.",
        "Stable by construction if merge ties break toward the left run — the default choice behind Java's and Go's stable sorts.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Merging two already-sorted lists is easy: look at the front of each, take the smaller, repeat. That costs one pass. Merge sort is what you get when you take that observation seriously — if merging is cheap, make everything sorted by splitting until the pieces are trivially sorted (one element), then merge your way back up.",
        "The cost falls out of the shape of the recursion. Halving takes log n levels to reach single elements, and every level touches all n elements exactly once during its merges. n work per level times log n levels is O(n log n), and unlike quicksort that bound holds no matter what the input looks like.",
        "The price is memory. You cannot merge two runs in place without either heroics or losing the linear-time merge, so the practical implementation keeps an n-sized scratch buffer. That trade is why quicksort usually wins on flat arrays while merge sort wins on linked lists, where splitting is free and merging needs no extra space at all.",
        "Its real superpower in interviews is that the merge step *sees* pairs of elements from opposite halves. When you take an element from the right run, you learn how many elements in the left run were greater than it — which counts inversions for free. That is the trick behind the hardest problems in this family.",
    ),

    keyPoints = listOf(
        "**Stable**: equal elements keep their original relative order, provided you break merge ties toward the left run (`if (right < left) take right`, using a strict comparison).",
        "**O(n log n) in every case** — best, average, and worst. There is no adversarial input, which is why it backs `Arrays.sort` for objects in Java and `sort.Stable` in Go.",
        "Needs **O(n) auxiliary space** for arrays. On a linked list it needs only O(log n) for the stack.",
        "Allocate the scratch buffer **once** at the top and pass it down. Allocating inside the recursion is the single most common performance mistake here.",
        "Skip the merge entirely when `a[mid] <= a[mid + 1]` — the two runs are already in order. This makes an already-sorted array cost O(n).",
        "It is the natural choice for **external sorting**, where data does not fit in memory, because merging is sequential and streams well from disk.",
    ),

    steps = listOf(
        "If the range holds zero or one element it is already sorted — return.",
        "Split the range at `mid = lo + (hi - lo) / 2`.",
        "Recursively sort the left half `[lo..mid]`.",
        "Recursively sort the right half `[mid + 1..hi]`.",
        "If `a[mid] <= a[mid + 1]` the halves are already in order; skip the merge.",
        "Otherwise merge: copy the range to the buffer, then walk both runs, repeatedly writing back whichever front element is smaller.",
        "When one run is exhausted, the remainder of the other is copied across as-is.",
    ),

    complexity = listOf(
        ComplexityRow("Best case", "O(n log n)", "O(n)", "O(n) with the already-ordered shortcut, since each merge becomes a single comparison."),
        ComplexityRow("Average case", "O(n log n)", "O(n)", "log n levels, n work per level."),
        ComplexityRow("Worst case", "O(n log n)", "O(n)", "No adversarial input exists — the bound is unconditional."),
        ComplexityRow("Linked list", "O(n log n)", "O(log n)", "Only the recursion stack. Splitting and merging need no extra nodes."),
    ),

    pitfalls = listOf(
        "Allocating a new buffer inside `merge`. It turns a fast sort into a garbage-collection benchmark — hoist it to the top-level call.",
        "Using `<=` when choosing from the right run, which silently destroys stability. Take from the right only on a strict `<`.",
        "Computing `mid` as `(lo + hi) / 2` — the same overflow bug as binary search.",
        "Forgetting that the recursion bottoms out at `lo >= hi`, not `lo == hi`, when the range can be empty.",
        "Reaching for merge sort on a small array. Below roughly 32 elements, insertion sort is faster in practice; real implementations cut over.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/**
 * Sorts [nums] in place. The scratch buffer is allocated once here and
 * threaded through the recursion — never allocate inside merge().
 */
fun mergeSort(nums: IntArray) {
    if (nums.size <= 1) return
    sort(nums, IntArray(nums.size), 0, nums.lastIndex)
}

private fun sort(a: IntArray, buf: IntArray, lo: Int, hi: Int) {
    if (lo >= hi) return

    val mid = lo + (hi - lo) / 2
    sort(a, buf, lo, mid)
    sort(a, buf, mid + 1, hi)

    // Already in order end-to-end: the merge would be a no-op.
    if (a[mid] <= a[mid + 1]) return

    merge(a, buf, lo, mid, hi)
}

private fun merge(a: IntArray, buf: IntArray, lo: Int, mid: Int, hi: Int) {
    for (i in lo..hi) buf[i] = a[i]

    var i = lo       // cursor into the left run
    var j = mid + 1  // cursor into the right run

    for (k in lo..hi) {
        a[k] = when {
            i > mid          -> buf[j++]   // left exhausted
            j > hi           -> buf[i++]   // right exhausted
            buf[j] < buf[i]  -> buf[j++]   // strict < preserves stability
            else             -> buf[i++]
        }
    }
}
        """.trim(),

        Lang.GO to """
// MergeSort sorts nums in place. The scratch buffer is allocated once and
// threaded through the recursion — never allocate inside merge.
func MergeSort(nums []int) {
	if len(nums) <= 1 {
		return
	}
	buf := make([]int, len(nums))
	sortRange(nums, buf, 0, len(nums)-1)
}

func sortRange(a, buf []int, lo, hi int) {
	if lo >= hi {
		return
	}

	mid := lo + (hi-lo)/2
	sortRange(a, buf, lo, mid)
	sortRange(a, buf, mid+1, hi)

	// Already in order end-to-end: the merge would be a no-op.
	if a[mid] <= a[mid+1] {
		return
	}

	merge(a, buf, lo, mid, hi)
}

func merge(a, buf []int, lo, mid, hi int) {
	copy(buf[lo:hi+1], a[lo:hi+1])

	i, j := lo, mid+1 // cursors into the left and right runs

	for k := lo; k <= hi; k++ {
		switch {
		case i > mid: // left exhausted
			a[k] = buf[j]
			j++
		case j > hi: // right exhausted
			a[k] = buf[i]
			i++
		case buf[j] < buf[i]: // strict < preserves stability
			a[k] = buf[j]
			j++
		default:
			a[k] = buf[i]
			i++
		}
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * Sorts nums in place. The scratch buffer is allocated once here and
 * threaded through the recursion — never allocate inside merge().
 */
function mergeSort(nums) {
  if (nums.length <= 1) return nums;
  sortRange(nums, new Array(nums.length), 0, nums.length - 1);
  return nums;
}

function sortRange(a, buf, lo, hi) {
  if (lo >= hi) return;

  const mid = (lo + hi) >>> 1;
  sortRange(a, buf, lo, mid);
  sortRange(a, buf, mid + 1, hi);

  // Already in order end-to-end: the merge would be a no-op.
  if (a[mid] <= a[mid + 1]) return;

  merge(a, buf, lo, mid, hi);
}

function merge(a, buf, lo, mid, hi) {
  for (let i = lo; i <= hi; i++) buf[i] = a[i];

  let i = lo;      // cursor into the left run
  let j = mid + 1; // cursor into the right run

  for (let k = lo; k <= hi; k++) {
    if (i > mid) a[k] = buf[j++];              // left exhausted
    else if (j > hi) a[k] = buf[i++];          // right exhausted
    else if (buf[j] < buf[i]) a[k] = buf[j++]; // strict < preserves stability
    else a[k] = buf[i++];
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "The one that asks you to actually write it — built-in sorts are disallowed. Merge sort is the safest answer because quicksort times out on the adversarial all-equal and already-sorted cases in the test set.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
        Question(
            id = 148,
            title = "Sort List",
            difficulty = Difficulty.MEDIUM,
            idea = "Merge sort is the right tool precisely because it is a linked list: find the middle with slow/fast pointers, split, sort each side, then merge by relinking nodes. O(1) extra space beyond the stack, which quicksort cannot match here.",
            askedAt = "Amazon, Microsoft, Bloomberg",
        ),
        Question(
            id = 23,
            title = "Merge k Sorted Lists",
            difficulty = Difficulty.HARD,
            idea = "Merging lists pairwise in a tournament — the merge half of merge sort applied k ways — gives O(N log k). Merging them one at a time into an accumulator is the trap: that degrades to O(N k). A min-heap over the k heads reaches the same bound.",
            askedAt = "Google, Amazon, Meta — extremely common",
        ),
    ),

    related = listOf("binary-search", "coin-change", "arrays", "linked-lists"),
    references = Refs.basecs(),
)

val Bfs = Topic(
    id = "bfs",
    title = "Breadth-First Search",
    tagline = "Explore in rings of increasing distance — the shortest-path tool for unweighted graphs.",
    level = Level.INTERMEDIATE,
    scene = { bfsScene() },

    quickSummary = listOf(
        "Explores in rings of increasing distance using a FIFO queue — the first time you reach a node, it's via the fewest edges.",
        "Mark visited **on enqueue**, not dequeue, or the same node re-enters the queue and blows up the runtime.",
        "Only gives shortest paths on unweighted graphs; weighted graphs need Dijkstra instead.",
        "Multi-source BFS seeds every source at distance 0, finding nearest-source distance for every node in one pass.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "BFS spreads outward from a source like ripples on water. It finishes every node at distance 1 before touching anything at distance 2, and so on. The queue is what enforces that discipline: first in, first out means nodes leave the queue in the same order they were discovered, which is non-decreasing distance order.",
        "That ordering is the entire correctness argument for shortest paths. The first time you reach a node, you have reached it by the fewest possible edges — no later route can be shorter, because any later route was discovered from a node at least as far away. This is why you mark a node visited **when you enqueue it, not when you dequeue it**; delaying the mark lets the same node enter the queue several times and quietly turns O(V + E) into something much worse.",
        "It only gives shortest paths when every edge costs the same. The moment edges carry different weights, the ripple metaphor breaks — a two-hop cheap path can beat a one-hop expensive one — and you need Dijkstra, which is BFS with a priority queue instead of a plain one.",
        "Most grid problems are BFS problems in disguise. A grid is just a graph where each cell has up to four neighbours, and you never need to build an adjacency list — you compute neighbours on the fly from the coordinates.",
    ),

    keyPoints = listOf(
        "Uses a **FIFO queue**. Swapping it for a stack gives you DFS, and that one-line change is worth internalising.",
        "Mark visited **on enqueue**. Marking on dequeue admits duplicates into the queue and breaks the complexity bound.",
        "Gives **shortest paths only on unweighted graphs** (or where all edges share one weight). Weighted graphs need Dijkstra.",
        "**Multi-source BFS**: seed the queue with every source at distance 0 and the algorithm computes, for each node, the distance to its *nearest* source — in one pass, not one pass per source.",
        "Processing the queue one **level at a time** (snapshot `queue.size` before the inner loop) gives you level-order traversal and lets you count how many rings you have expanded.",
        "To recover the actual path, store a `parent` pointer as you discover each node, then walk it back from the target.",
    ),

    steps = listOf(
        "Put the source in the queue, set its distance to 0, and mark it visited.",
        "While the queue is not empty, remove the node at the front.",
        "For each neighbour of that node, skip it if it is already visited.",
        "Otherwise mark it visited, set its distance to the current node's distance plus one, record its parent, and push it to the back of the queue.",
        "When the queue empties, every reachable node has been assigned its true shortest distance from the source.",
    ),

    complexity = listOf(
        ComplexityRow("Traversal", "O(V + E)", "O(V)", "Each node is enqueued once and each edge inspected once (twice if undirected)."),
        ComplexityRow("Grid of R × C", "O(R · C)", "O(R · C)", "Every cell is a node with at most four edges, so E is proportional to V."),
        ComplexityRow("Worst-case queue", "—", "O(V)", "A star graph puts every neighbour in the queue at once."),
    ),

    pitfalls = listOf(
        "Marking visited on dequeue rather than enqueue — the classic bug. Nodes get queued repeatedly and the run time blows up.",
        "Using a list and calling `removeAt(0)` as the dequeue. That is O(n) per operation and turns the whole traversal quadratic. Use `ArrayDeque`.",
        "Reaching for BFS on a weighted graph. It returns fewest *edges*, not lowest *cost*.",
        "Forgetting bounds checks on grid neighbours, or revisiting the cell you came from.",
        "Losing the level boundary when you need it — capture `queue.size` before the inner loop, because the queue grows while you iterate.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Shortest distance in edges from [source] to every reachable node. */
fun bfs(graph: Map<Int, List<Int>>, source: Int): Map<Int, Int> {
    val dist = mutableMapOf(source to 0)
    val parent = mutableMapOf<Int, Int>()
    val queue = ArrayDeque<Int>()
    queue.addLast(source)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()

        for (next in graph[node].orEmpty()) {
            if (next in dist) continue   // visited — mark on enqueue, not dequeue

            dist[next] = dist.getValue(node) + 1
            parent[next] = node
            queue.addLast(next)
        }
    }
    return dist
}

/**
 * Multi-source BFS on a grid: distance from every cell to the nearest source.
 * Seeding all sources at distance 0 costs one pass, not one pass per source.
 */
fun nearestSource(grid: Array<IntArray>, sources: List<Pair<Int, Int>>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val dist = Array(rows) { IntArray(cols) { -1 } }
    val queue = ArrayDeque<Pair<Int, Int>>()

    for ((r, c) in sources) {
        dist[r][c] = 0
        queue.addLast(r to c)
    }

    val moves = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()

        for ((dr, dc) in moves) {
            val nr = r + dr
            val nc = c + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            if (dist[nr][nc] != -1) continue

            dist[nr][nc] = dist[r][c] + 1
            queue.addLast(nr to nc)
        }
    }
    return dist
}
        """.trim(),

        Lang.GO to """
// BFS returns the shortest distance in edges from source to every
// reachable node.
func BFS(graph map[int][]int, source int) map[int]int {
	dist := map[int]int{source: 0}
	parent := map[int]int{}
	queue := []int{source}

	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]

		for _, next := range graph[node] {
			if _, seen := dist[next]; seen {
				continue // mark on enqueue, not dequeue
			}

			dist[next] = dist[node] + 1
			parent[next] = node
			queue = append(queue, next)
		}
	}
	return dist
}

// LevelOrder walks the graph one ring at a time, returning the nodes
// grouped by their distance from the source.
func LevelOrder(graph map[int][]int, source int) [][]int {
	visited := map[int]bool{source: true}
	queue := []int{source}
	levels := [][]int{}

	for len(queue) > 0 {
		size := len(queue) // snapshot: the queue grows while we iterate
		level := make([]int, 0, size)

		for i := 0; i < size; i++ {
			node := queue[0]
			queue = queue[1:]
			level = append(level, node)

			for _, next := range graph[node] {
				if visited[next] {
					continue
				}
				visited[next] = true
				queue = append(queue, next)
			}
		}
		levels = append(levels, level)
	}
	return levels
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Shortest distance in edges from source to every reachable node. */
function bfs(graph, source) {
  const dist = new Map([[source, 0]]);
  const parent = new Map();
  const queue = [source];
  let head = 0; // index cursor — shift() would be O(n) per call

  while (head < queue.length) {
    const node = queue[head++];

    for (const next of graph.get(node) ?? []) {
      if (dist.has(next)) continue; // mark on enqueue, not dequeue

      dist.set(next, dist.get(node) + 1);
      parent.set(next, node);
      queue.push(next);
    }
  }
  return dist;
}

/**
 * Walks the graph one ring at a time, returning nodes grouped by their
 * distance from the source.
 */
function levelOrder(graph, source) {
  const visited = new Set([source]);
  let frontier = [source];
  const levels = [];

  while (frontier.length > 0) {
    levels.push(frontier);
    const next = [];

    for (const node of frontier) {
      for (const neighbour of graph.get(node) ?? []) {
        if (visited.has(neighbour)) continue;
        visited.add(neighbour);
        next.push(neighbour);
      }
    }
    frontier = next;
  }
  return levels;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 102,
            title = "Binary Tree Level Order Traversal",
            difficulty = Difficulty.MEDIUM,
            idea = "BFS with an explicit level boundary. Snapshot the queue size before the inner loop — that count is exactly one level, because everything added during the loop belongs to the next one.",
            askedAt = "Amazon, Microsoft, LinkedIn",
        ),
        Question(
            id = 994,
            title = "Rotting Oranges",
            difficulty = Difficulty.MEDIUM,
            idea = "Multi-source BFS. Seed the queue with every rotten orange at time 0 and expand in levels; the answer is the number of levels. Running a separate BFS per source is the trap. Remember to check for fresh oranges left over at the end.",
            askedAt = "Amazon — a favourite",
        ),
        Question(
            id = 127,
            title = "Word Ladder",
            difficulty = Difficulty.HARD,
            idea = "The graph is implicit: words are nodes and an edge means \"differs by one letter\". Do not build the adjacency list by comparing all pairs — generate neighbours by replacing each position with every letter and testing membership in the word set. Bidirectional BFS from both ends is the follow-up they want.",
            askedAt = "Google, Amazon, Meta",
        ),
    ),

    related = listOf("dfs", "stacks-queues", "hash-tables", "linked-lists"),
    references = Refs.basecs(),
)

val Dfs = Topic(
    id = "dfs",
    title = "Depth-First Search",
    tagline = "Commit to a path, and only back up when it runs out.",
    level = Level.INTERMEDIATE,
    scene = { dfsScene() },

    quickSummary = listOf(
        "Same traversal as BFS with a stack instead of a queue — commit to a path, back up only when it runs out.",
        "Recursion *is* the stack — that's why the recursive version is only a few lines long.",
        "Finishing a branch before moving on is what powers cycle detection, topological sort and connected components.",
        "Finds *a* path, never reliably the shortest one — that's still BFS's job.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Breadth-first search explores cautiously, finishing everything one step away before looking two steps away. Depth-first search does the opposite: it picks a direction and *commits*, following it as far as it goes, and only when that path is exhausted does it retreat to the last junction and try the next option. It is the strategy of someone walking a maze with one hand on the wall.",
        "The only structural difference from BFS is the container holding the frontier. BFS uses a queue, so the oldest discovery comes back first. DFS uses a **stack**, so the newest one does. Swap those two and the traversal order changes completely with no other edit — which is the clearest illustration of why data structures matter that this curriculum has.",
        "Most of the time you never declare that stack, because recursion already provides one. Calling `dfs(neighbour)` pushes a frame; returning pops it. That makes DFS extraordinarily short to write — often four or five lines — and it is why the recursive version is the one people reach for. It is also why DFS blows up on deep graphs in a way BFS does not: the stack is real, and it is finite.",
        "What DFS is *good* at is a specific class of questions. Because it fully finishes a branch before moving on, it knows when a subtree is complete — and that is exactly what you need for **cycle detection**, **topological ordering**, **connected components**, and anything where an answer for a node depends on answers for everything below it. BFS cannot easily tell you that, because it never finishes a branch before starting others.",
        "The single most important detail is the **visited set**. Without one, any cycle turns DFS into an infinite loop, and even in an acyclic graph you will revisit shared nodes exponentially often. Mark a node when you first reach it, not when you finish it, or two paths arriving at the same node will both descend into it.",
        "One thing DFS does **not** give you is shortest paths. It finds *a* path readily, but the first one it stumbles onto is whichever direction it happened to try first — which can be arbitrarily worse than the best. If the question says shortest and the edges are unweighted, you want BFS.",
    ),

    origin = "The method is older than computing. **Charles Pierre Trémaux**, a French engineer, described a systematic way to walk a maze in the 19th century — mark each passage as you enter it, never take a marked passage twice, and retreat when you run out of new ones. It was written up by **Édouard Lucas in *Récréations Mathématiques* (1882)**, and it is depth-first search with a visited set, several decades before there was a machine to run it on. Its modern importance came from **John Hopcroft and Robert Tarjan in the early 1970s**, who showed that DFS was not merely one traversal among several but the engine behind efficient algorithms for biconnectivity, strongly connected components and planarity testing. The two shared the **1986 Turing Award**, with this work among the cited contributions.",

    keyPoints = listOf(
        "DFS and BFS are **the same algorithm** with a different container: a stack rather than a queue.",
        "Recursion *is* the stack. The explicit-stack version exists mainly to survive graphs deeper than the call stack.",
        "**O(V + E)** time — every vertex and every edge is considered once. Space is O(V) for the visited set plus the stack.",
        "Mark visited **on discovery**, not on completion, or shared nodes get explored more than once.",
        "It finds **a** path, never reliably the shortest. Unweighted shortest path is BFS's job.",
        "Finishing a branch before moving on is what enables **topological sort**, **cycle detection** and **components**.",
        "Cycle detection on a *directed* graph needs three states, not two: unvisited, in-progress, done. Meeting an in-progress node is a cycle; meeting a done one is not.",
    ),

    complexity = listOf(
        ComplexityRow("Traversal", "O(V + E)", "O(V)", "Each vertex is visited once and each edge examined once, from each end for an undirected graph."),
        ComplexityRow("Recursive space", "—", "O(V)", "Worst case the call stack holds every vertex — a path graph, or a long chain."),
        ComplexityRow("Cycle detection", "O(V + E)", "O(V)", "Same traversal; the three-state colouring is what reads the answer off it."),
        ComplexityRow("Topological sort", "O(V + E)", "O(V)", "Push each vertex when it *finishes*, then reverse the resulting order."),
        ComplexityRow("Path finding", "O(V + E)", "O(V)", "Finds a path quickly. Says nothing about it being short."),
    ),

    pitfalls = listOf(
        "Leaving out the visited set. On any graph with a cycle this never terminates; on a DAG with shared nodes it silently does exponential work.",
        "Marking visited when a node *finishes* rather than when it is discovered. Two paths reaching the same node then both descend into it.",
        "Using DFS for shortest path. It returns the first path it happens to find, which can be arbitrarily longer than the best one.",
        "Recursing on a graph deep enough to overflow the call stack. A linked-list-shaped graph of 100,000 nodes will do it — rewrite with an explicit stack.",
        "Using two states for cycle detection in a *directed* graph. A node you have already fully finished is not a cycle; only one still on the current path is.",
        "Pushing a node's neighbours and expecting the visit order to match the recursive version. A stack reverses them, so the iterative form explores the last neighbour first unless you push in reverse.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Recursive: the call stack is doing the work, which is why this is so short. */
fun dfs(
    graph: Map<String, List<String>>,
    start: String,
    visited: MutableSet<String> = mutableSetOf(),
): List<String> {
    // Marked on discovery. Marking on completion lets two paths into the
    // same node before either finishes.
    if (!visited.add(start)) return emptyList()

    val order = mutableListOf(start)
    for (neighbour in graph[start].orEmpty()) {
        order += dfs(graph, neighbour, visited)
    }
    return order
}

/** Iterative: identical, but survives graphs deeper than the call stack. */
fun dfsIterative(graph: Map<String, List<String>>, start: String): List<String> {
    val visited = mutableSetOf<String>()
    val order = mutableListOf<String>()
    val stack = ArrayDeque<String>()
    stack.addLast(start)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (!visited.add(node)) continue
        order += node

        // Reversed, so the first neighbour is on top and the visit order
        // matches the recursive version.
        for (neighbour in graph[node].orEmpty().reversed()) {
            if (neighbour !in visited) stack.addLast(neighbour)
        }
    }
    return order
}

/**
 * Cycle detection needs three states, not two: a node still on the current
 * path is a cycle, a node already finished is not.
 */
fun hasCycle(graph: Map<String, List<String>>): Boolean {
    val inProgress = mutableSetOf<String>()
    val done = mutableSetOf<String>()

    fun visit(node: String): Boolean {
        if (node in inProgress) return true   // back edge — a real cycle
        if (node in done) return false        // already settled, not a cycle

        inProgress += node
        val found = graph[node].orEmpty().any { visit(it) }
        inProgress -= node
        done += node
        return found
    }

    return graph.keys.any { it !in done && visit(it) }
}
        """.trim(),

        Lang.GO to """
// Recursive: the call stack is doing the work, which is why this is so short.
func DFS(graph map[string][]string, start string, visited map[string]bool) []string {
	// Marked on discovery. Marking on completion lets two paths into the
	// same node before either finishes.
	if visited[start] {
		return nil
	}
	visited[start] = true

	order := []string{start}
	for _, neighbour := range graph[start] {
		order = append(order, DFS(graph, neighbour, visited)...)
	}
	return order
}

// Iterative: identical, but survives graphs deeper than the call stack.
func DFSIterative(graph map[string][]string, start string) []string {
	visited := map[string]bool{}
	order := []string{}
	stack := []string{start}

	for len(stack) > 0 {
		node := stack[len(stack)-1]
		stack = stack[:len(stack)-1]

		if visited[node] {
			continue
		}
		visited[node] = true
		order = append(order, node)

		// Reversed, so the first neighbour ends up on top.
		neighbours := graph[node]
		for i := len(neighbours) - 1; i >= 0; i-- {
			if !visited[neighbours[i]] {
				stack = append(stack, neighbours[i])
			}
		}
	}
	return order
}

// Cycle detection needs three states: unvisited, in progress, done.
func HasCycle(graph map[string][]string) bool {
	const (
		unvisited = iota
		inProgress
		done
	)
	state := map[string]int{}

	var visit func(string) bool
	visit = func(node string) bool {
		if state[node] == inProgress {
			return true // back edge — a real cycle
		}
		if state[node] == done {
			return false // already settled
		}

		state[node] = inProgress
		for _, neighbour := range graph[node] {
			if visit(neighbour) {
				return true
			}
		}
		state[node] = done
		return false
	}

	for node := range graph {
		if state[node] == unvisited && visit(node) {
			return true
		}
	}
	return false
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Recursive: the call stack is doing the work, which is why this is so short. */
function dfs(graph, start, visited = new Set()) {
  // Marked on discovery. Marking on completion lets two paths into the same
  // node before either finishes.
  if (visited.has(start)) return [];
  visited.add(start);

  const order = [start];
  for (const neighbour of graph[start] ?? []) {
    order.push(...dfs(graph, neighbour, visited));
  }
  return order;
}

/** Iterative: identical, but survives graphs deeper than the call stack. */
function dfsIterative(graph, start) {
  const visited = new Set();
  const order = [];
  const stack = [start];

  while (stack.length > 0) {
    const node = stack.pop();
    if (visited.has(node)) continue;

    visited.add(node);
    order.push(node);

    // Reversed, so the first neighbour is on top and the order matches
    // the recursive version.
    for (const neighbour of [...(graph[node] ?? [])].reverse()) {
      if (!visited.has(neighbour)) stack.push(neighbour);
    }
  }
  return order;
}

/**
 * Cycle detection needs three states, not two: a node still on the current
 * path is a cycle, a node already finished is not.
 */
function hasCycle(graph) {
  const inProgress = new Set();
  const done = new Set();

  const visit = (node) => {
    if (inProgress.has(node)) return true; // back edge — a real cycle
    if (done.has(node)) return false;      // already settled

    inProgress.add(node);
    const found = (graph[node] ?? []).some(visit);
    inProgress.delete(node);
    done.add(node);
    return found;
  };

  return Object.keys(graph).some((node) => !done.has(node) && visit(node));
}
        """.trim(),
    ),

    steps = listOf(
        "**Push the start** onto the stack — or make the first call, which does the same thing.",
        "**Pop a node.** If it has already been visited, discard it and pop again; duplicates reach the stack legitimately.",
        "**Mark it visited immediately**, before touching its neighbours.",
        "**Push every unvisited neighbour.** Because a stack is last in, first out, the most recently pushed is explored next — that is the depth.",
        "**Repeat until the stack empties.** Popping past a dead end is exactly the backtracking; nothing extra is needed to make it happen.",
    ),

    questions = listOf(
        Question(
            id = 200,
            title = "Number of Islands",
            difficulty = Difficulty.MEDIUM,
            idea = "The grid is a graph in disguise — each cell has up to four neighbours. Scan for an unvisited piece of land, run one DFS to drown the entire island, and increment a counter. The insight is that the number of DFS *launches* is the answer, not anything the traversal itself returns.",
            askedAt = "Amazon, Meta, Google — the most asked graph question there is",
        ),
        Question(
            id = 207,
            title = "Course Schedule",
            difficulty = Difficulty.MEDIUM,
            idea = "\"Can all courses be finished\" is asking whether the prerequisite graph has a cycle. The trap is using a plain visited set: a node you have already fully explored is not a cycle. You need three states, and only a node still on the current path counts as one.",
            askedAt = "Amazon, Meta, Uber",
        ),
        Question(
            id = 133,
            title = "Clone Graph",
            difficulty = Difficulty.MEDIUM,
            idea = "The visited set has to become a map from original node to its copy. That map is doing double duty — it prevents infinite recursion on cycles *and* it is how you rewire each clone's neighbours to point at clones rather than originals. Miss the second job and you get a copy that still references the input.",
            askedAt = "Meta, Bloomberg",
        ),
    ),

    related = listOf("bfs", "stacks-queues", "coin-change"),
    references = Refs.basecs(),
)

val CoinChange = Topic(
    id = "coin-change",
    title = "Coin Change",
    tagline = "Where greed fails, and a table succeeds.",
    level = Level.ADVANCED,
    scene = { coinChangeScene() },

    quickSummary = listOf(
        "Greedy fails here: with coins 1, 3, 4 and a target of 6, greedy takes three coins where two suffice.",
        "DP applies when a problem has **optimal substructure** and **overlapping subproblems** — cache each subproblem's answer once.",
        "The table gives a count, not the coins themselves — recovering the actual coins needs a backwards walk or a parent array.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Making change with the fewest coins feels like it should be easy, because you already do it. Take the largest coin that fits, repeat. That is the greedy strategy, and with British or American currency it happens to be correct — which is exactly why the failure is so instructive when it comes.",
        "Take coins of **1, 3 and 4**, and try to make **6**. Greedy grabs the 4, then needs 2 more and can only manage 1 + 1: three coins. But **3 + 3** is two coins. Greedy did not make an arithmetic error; it made a *structural* one. Taking the biggest coin looked best locally and closed off the better answer, and no amount of care at each step recovers it. Whether greedy works on a coin system is a property of that system, not of the algorithm.",
        "So if you cannot decide one coin at a time, what can you do? Consider every first coin, and trust that the smaller problem left behind is already solved. The fewest coins for 6 is one more than the fewest coins for 6−1, 6−3 or 6−4, whichever is smallest. That recursion is correct immediately — the trouble is that it recomputes the same subproblems endlessly, and the work explodes exponentially.",
        "**Dynamic programming is that recursion, with the answers written down.** The subproblems overlap heavily — the fewest coins for 2 is needed by 3, 5 and 6 alike — so computing it once and storing it collapses exponential work into a table you fill in one pass. Two ways to arrange the same idea: **memoisation** keeps the recursion and caches results on the way down, and **tabulation** starts from the smallest case and builds up. The table is the honest picture, and it is what the visualisation shows.",
        "The condition that makes this legal is worth naming, because it is what interviewers are really testing. A problem yields to DP when it has **optimal substructure** — the best answer is built from best answers to smaller versions — and **overlapping subproblems**, so caching actually saves work. Merge sort has the first and not the second, which is why divide and conquer suits it and a table would be pointless.",
        "Reading the answer off the table is the last trick. `best[6] = 2` tells you *how many* coins, not which. If you need the coins themselves, either store the choice made at each cell or walk backwards afterwards, checking which predecessor cell is exactly one less. Interviews ask for the count; production usually wants the coins.",
    ),

    origin = "**Richard Bellman** developed dynamic programming at the **RAND Corporation in the 1950s**, and named it with a caution that had nothing to do with mathematics. In his autobiography *Eye of the Hurricane* (1984) he explains that the Secretary of Defense at the time was hostile to anything resembling research, so a word was needed that could not be objected to. *Programming* meant planning and scheduling, as it still does in *linear programming* — nothing to do with writing code. *Dynamic* was chosen partly because it described the multi-stage decision processes he was studying, and partly, in his own account, because it was impossible to use in a pejorative sense. The **Bellman equation** that came out of this work is still the foundation of reinforcement learning, so the name outlived the politics by a considerable margin.",

    keyPoints = listOf(
        "**Greedy is not universally correct.** With coins 1, 3, 4 and a target of 6 it returns three coins where two suffice.",
        "DP applies when there is **optimal substructure** *and* **overlapping subproblems**. Without the overlap, caching buys nothing.",
        "**Memoisation** is top-down: keep the recursion, cache each result. **Tabulation** is bottom-up: fill smallest to largest. Same answers, same complexity.",
        "Initialise the impossible cells to a sentinel — infinity, or `target + 1`. Using `-1` and then adding one to it is a common source of silent wrongness.",
        "`best[0] = 0` is the base case that makes the whole table work: zero coins make zero.",
        "The table gives **counts**, not the coins themselves. Recovering the actual coins needs a parent array or a backwards walk.",
        "Complexity is **O(target × coins)** time and O(target) space — pseudo-polynomial, because it scales with the *value* of the target, not the number of digits in it.",
    ),

    complexity = listOf(
        ComplexityRow("Tabulation", "O(target × coins)", "O(target)", "One pass per amount, considering each coin. The standard answer."),
        ComplexityRow("Memoisation", "O(target × coins)", "O(target)", "Same bound, plus recursion frames — which can overflow for a large target."),
        ComplexityRow("Naive recursion", "O(coins^target)", "O(target)", "Correct and unusable. Every subproblem is recomputed from scratch."),
        ComplexityRow("Greedy", "O(coins log coins)", "O(1)", "Fast, and **wrong** on coin systems where the largest coin is not always safe."),
        ComplexityRow("Recovering the coins", "O(target)", "O(target)", "Walk back through the table, or store the choice made at each cell."),
    ),

    pitfalls = listOf(
        "Assuming greedy works because it works with real money. It is a property of the coin system, not of the approach — and interviewers pick systems where it breaks.",
        "Using `-1` or `0` as the \"impossible\" marker and then adding one to it. Use `target + 1` as infinity: it is larger than any real answer and never overflows.",
        "Forgetting `best[0] = 0`. Every other cell is derived from it, so the whole table comes out wrong rather than slightly off.",
        "Swapping the loop order in **Coin Change II**. Coins outside, amounts inside counts *combinations*; the reverse counts *permutations*, and 1+3 becomes different from 3+1.",
        "Reaching for memoised recursion on a large target and overflowing the call stack. Tabulation has no stack to overflow.",
        "Reporting the table value as the answer when the question asked which coins. `best[n]` is a count; the coins need reconstructing.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/**
 * Bottom-up: fewest coins for every amount from 0 to target.
 * Returns -1 when the target cannot be made at all.
 */
fun coinChange(coins: List<Int>, target: Int): Int {
    // target + 1 is larger than any real answer, and unlike Int.MAX_VALUE
    // it cannot overflow when we add one to it.
    val impossible = target + 1
    val best = IntArray(target + 1) { impossible }
    best[0] = 0 // zero coins make zero — the base case everything rests on

    for (amount in 1..target) {
        for (coin in coins) {
            if (coin > amount) continue
            // One more coin than whatever was left after taking this one.
            best[amount] = minOf(best[amount], best[amount - coin] + 1)
        }
    }

    return if (best[target] == impossible) -1 else best[target]
}

/** Top-down: the same recursion, with results remembered. */
fun coinChangeMemo(coins: List<Int>, target: Int): Int {
    val cache = HashMap<Int, Int>()

    fun fewest(amount: Int): Int {
        if (amount == 0) return 0
        if (amount < 0) return -1
        cache[amount]?.let { return it }

        var best = -1
        for (coin in coins) {
            val rest = fewest(amount - coin)
            if (rest >= 0 && (best < 0 || rest + 1 < best)) best = rest + 1
        }

        cache[amount] = best
        return best
    }

    return fewest(target)
}

/** Which coins, not just how many — walk the table backwards. */
fun coinsUsed(coins: List<Int>, target: Int): List<Int> {
    val impossible = target + 1
    val best = IntArray(target + 1) { impossible }
    best[0] = 0
    for (amount in 1..target) {
        for (coin in coins) {
            if (coin <= amount) best[amount] = minOf(best[amount], best[amount - coin] + 1)
        }
    }
    if (best[target] == impossible) return emptyList()

    val used = mutableListOf<Int>()
    var amount = target
    while (amount > 0) {
        // The coin that took us here is the one whose predecessor is exactly
        // one cheaper.
        val coin = coins.first { it <= amount && best[amount - it] == best[amount] - 1 }
        used += coin
        amount -= coin
    }
    return used
}
        """.trim(),

        Lang.GO to """
// CoinChange returns the fewest coins making target, or -1 if impossible.
func CoinChange(coins []int, target int) int {
	// target+1 is larger than any real answer, and unlike math.MaxInt it
	// cannot overflow when we add one to it.
	impossible := target + 1
	best := make([]int, target+1)
	for i := range best {
		best[i] = impossible
	}
	best[0] = 0 // zero coins make zero — the base case everything rests on

	for amount := 1; amount <= target; amount++ {
		for _, coin := range coins {
			if coin > amount {
				continue
			}
			// One more coin than whatever was left after taking this one.
			if best[amount-coin]+1 < best[amount] {
				best[amount] = best[amount-coin] + 1
			}
		}
	}

	if best[target] == impossible {
		return -1
	}
	return best[target]
}

// CoinsUsed returns which coins, not just how many.
func CoinsUsed(coins []int, target int) []int {
	impossible := target + 1
	best := make([]int, target+1)
	for i := range best {
		best[i] = impossible
	}
	best[0] = 0

	for amount := 1; amount <= target; amount++ {
		for _, coin := range coins {
			if coin <= amount && best[amount-coin]+1 < best[amount] {
				best[amount] = best[amount-coin] + 1
			}
		}
	}
	if best[target] == impossible {
		return nil
	}

	used := []int{}
	for amount := target; amount > 0; {
		for _, coin := range coins {
			// The coin that took us here is the one whose predecessor is
			// exactly one cheaper.
			if coin <= amount && best[amount-coin] == best[amount]-1 {
				used = append(used, coin)
				amount -= coin
				break
			}
		}
	}
	return used
}
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * Bottom-up: fewest coins for every amount from 0 to target.
 * Returns -1 when the target cannot be made at all.
 */
function coinChange(coins, target) {
  // target + 1 is larger than any real answer, and unlike Infinity it stays
  // an integer when we add one to it.
  const impossible = target + 1;
  const best = new Array(target + 1).fill(impossible);
  best[0] = 0; // zero coins make zero — the base case everything rests on

  for (let amount = 1; amount <= target; amount++) {
    for (const coin of coins) {
      if (coin > amount) continue;
      // One more coin than whatever was left after taking this one.
      best[amount] = Math.min(best[amount], best[amount - coin] + 1);
    }
  }

  return best[target] === impossible ? -1 : best[target];
}

/** Top-down: the same recursion, with results remembered. */
function coinChangeMemo(coins, target, cache = new Map()) {
  if (target === 0) return 0;
  if (target < 0) return -1;
  if (cache.has(target)) return cache.get(target);

  let best = -1;
  for (const coin of coins) {
    const rest = coinChangeMemo(coins, target - coin, cache);
    if (rest >= 0 && (best < 0 || rest + 1 < best)) best = rest + 1;
  }

  cache.set(target, best);
  return best;
}

/** Which coins, not just how many — walk the table backwards. */
function coinsUsed(coins, target) {
  const impossible = target + 1;
  const best = new Array(target + 1).fill(impossible);
  best[0] = 0;
  for (let amount = 1; amount <= target; amount++) {
    for (const coin of coins) {
      if (coin <= amount) best[amount] = Math.min(best[amount], best[amount - coin] + 1);
    }
  }
  if (best[target] === impossible) return [];

  const used = [];
  let amount = target;
  while (amount > 0) {
    // The coin that took us here is the one whose predecessor is exactly
    // one cheaper.
    const coin = coins.find((c) => c <= amount && best[amount - c] === best[amount] - 1);
    used.push(coin);
    amount -= coin;
  }
  return used;
}
        """.trim(),
    ),

    steps = listOf(
        "**Make a table** with one cell per amount from 0 up to the target.",
        "**Set every cell to infinity** — here `target + 1`, which is larger than any real answer and safe to add one to.",
        "**Set `best[0] = 0`.** Zero coins make zero; every other cell is eventually derived from this.",
        "**For each amount, try every coin** that is not larger than it. The candidate answer is `best[amount − coin] + 1`.",
        "**Keep the smallest candidate.** That cell is now final and will never need revisiting — which is what makes one pass enough.",
        "**Read `best[target]`.** If it is still infinity, the target cannot be made from these coins at all.",
    ),

    questions = listOf(
        Question(
            id = 322,
            title = "Coin Change",
            difficulty = Difficulty.MEDIUM,
            idea = "The problem this topic is built on. The whole test is whether you notice greedy is wrong — say so explicitly with a counterexample like coins 1, 3, 4 and target 6, then build the table. Remember to return -1 rather than the sentinel when the target is unreachable.",
            askedAt = "Amazon, Google, Meta — the standard first DP question",
        ),
        Question(
            id = 70,
            title = "Climbing Stairs",
            difficulty = Difficulty.EASY,
            idea = "The gentlest possible DP, and worth doing first: ways to reach step n is ways to reach n−1 plus ways to reach n−2. That is Fibonacci wearing a hat. Since only the last two values matter, the array collapses to two variables and O(1) space — recognising that is the follow-up they want.",
            askedAt = "Very common as an opener",
        ),
        Question(
            id = 518,
            title = "Coin Change II",
            difficulty = Difficulty.MEDIUM,
            idea = "Counting combinations rather than minimising coins, and it hides the sharpest trap in beginner DP: **loop order decides the answer**. Coins in the outer loop counts combinations; amounts outer counts permutations, so 1+3 and 3+1 are double-counted. Being able to explain *why* is the real question.",
            askedAt = "Amazon, Google",
        ),
    ),

    related = listOf("dfs", "merge-sort"),
    references = Refs.basecs(),
)
