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

    related = listOf("linked-lists", "stacks-queues", "binary-search", "hash-tables", "heaps", "quicksort", "counting-sort", "radix-sort"),

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

    related = listOf("arrays", "stacks-queues", "merge-sort", "binary-trees"),
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

    related = listOf("arrays", "linked-lists", "tries"),
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

val TwoPointers = Topic(
    id = "two-pointers",
    title = "Two Pointers",
    tagline = "Walk two positions through the data at once, and most O(n²) problems collapse to O(n).",
    level = Level.BASIC,

    quickSummary = listOf(
        "Replace a nested loop with two indices moving through the data under a clear rule for when each one advances — turns many O(n²) scans into O(n).",
        "Works whenever there's a monotonic relationship to exploit: sortedness, palindromic symmetry, or a window that only ever needs to grow or shrink one way.",
        "Two shapes: pointers converging from opposite ends (pair-sum, palindrome checks), and pointers moving in the same direction at different speeds (cycle detection, dedup).",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A huge number of array problems have an obvious O(n²) solution: for every element, scan the rest of the array looking for something. Two pointers is the realisation that, once the data is sorted or has some other monotonic structure, you almost never need to re-scan from the start — you can keep two positions moving through the array and use the relationship between what they point at to decide which one to move next, visiting each position a bounded number of times overall.",
        "The clearest example is finding a pair that sums to a target in a sorted array. Start one pointer at the front, one at the back. If the pair's sum is too small, the only way to increase it is to move the left pointer right — the right pointer is already at the largest available value, so moving it wouldn't help. If the sum is too big, symmetric reasoning says move the right pointer left. Either way, exactly one pointer moves on each step, and the two can meet in at most n steps — one pass instead of a nested one.",
        "That \"opposite ends, converging\" shape is one of two common patterns. The other is \"same direction, different speeds\": a slow pointer and a fast pointer both start at the front and advance under different rules, useful for removing duplicates in place (a write pointer only advances on a genuinely new value) or detecting a cycle in a linked list (a pointer moving twice as fast as another must lap it if a loop exists).",
        "What both shapes share is a monotonic argument for why moving a pointer never throws away a valid answer — the same kind of reasoning that justifies binary search's halving. That's worth stating explicitly in an interview: two pointers isn't a trick to memorise per problem, it's a specific proof technique — \"moving this pointer can never skip the answer, because...\" — applied to array traversal.",
    ),

    origin = "Two pointers is a **technique rather than a named, dated invention** — no single paper or person is credited with it. It emerges naturally once you're looking for ways to avoid re-scanning sorted or symmetric data, and versions of the idea appear scattered across algorithms literature from the earliest computing decades without a clean point of origin, much like insertion sort.",

    keyPoints = listOf(
        "**Two shapes cover most uses**: pointers converging from opposite ends (pair-sum, palindrome checks), and pointers moving in the same direction at different rates (cycle detection, in-place deduplication, fast/slow list traversal).",
        "**Requires a monotonic relationship to exploit** — usually sortedness, but symmetry (palindromes) or a one-directional window (see: sliding window) work the same way.",
        "**O(n) instead of O(n²)** — each pointer moves at most n times total, so the whole traversal is linear even though it looks like it's tracking two positions.",
        "**The correctness argument is the interview answer**, not the code — being able to say precisely why moving a given pointer can never skip over the correct answer is what's actually being tested.",
        "**Sort first if the input isn't already sorted** — the O(n log n) sort cost is usually still better than the O(n²) it replaces, and two pointers needs the sortedness to make its monotonic argument.",
    ),

    complexity = listOf(
        ComplexityRow("Two pointers over sorted/prepared data", "O(n)", "O(1)", "Each pointer advances a bounded number of times total across the whole pass."),
        ComplexityRow("If a sort is needed first", "O(n log n)", "O(1) or O(n)", "Dominated by the sort; the two-pointer pass itself stays O(n) afterward."),
    ),

    pitfalls = listOf(
        "Applying it to unsorted data without sorting first (when the problem allows it) — the monotonic argument that makes pointer movement safe depends on the sortedness existing in the first place.",
        "Moving the wrong pointer, or both pointers, when only one movement is justified by the current comparison — the single most common bug, and it usually produces a plausible-looking but wrong answer rather than a crash.",
        "Forgetting the boundary condition where the two pointers meet or cross — off-by-one errors here are extremely common and worth explicitly testing.",
        "Reaching for two pointers on a problem that needs non-adjacent, non-monotonic relationships — not every array problem fits the pattern, and forcing it produces contorted code that a hash set or brute force would have solved more simply.",
    ),

    steps = listOf(
        "Identify the monotonic property the input has — sorted, symmetric, etc. — that makes a pointer's movement provably safe.",
        "Place the two pointers according to the pattern: opposite ends for convergence, both at the start for different-speed traversal.",
        "At each step, compare what the pointers point at and move exactly the pointer(s) justified by that comparison.",
        "Stop when the pointers meet, cross, or one runs off the end, depending on the specific problem.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Opposite-ends convergence: pair summing to target in a sorted array. */
fun twoSumSorted(nums: IntArray, target: Int): Pair<Int, Int>? {
    var left = 0
    var right = nums.lastIndex

    while (left < right) {
        val sum = nums[left] + nums[right]
        when {
            sum == target -> return left to right
            sum < target -> left++  // only increasing left can raise the sum
            else -> right--         // only decreasing right can lower it
        }
    }
    return null
}

/** Same-direction, different speeds: dedupe a sorted array in place. */
fun removeDuplicates(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    var writer = 1 // only advances on a genuinely new value
    for (reader in 1 until nums.size) {
        if (nums[reader] != nums[writer - 1]) {
            nums[writer] = nums[reader]
            writer++
        }
    }
    return writer
}
        """.trim(),

        Lang.GO to """
// TwoSumSorted converges from opposite ends: pair summing to target in a
// sorted slice.
func TwoSumSorted(nums []int, target int) (int, int, bool) {
	left, right := 0, len(nums)-1

	for left < right {
		sum := nums[left] + nums[right]
		switch {
		case sum == target:
			return left, right, true
		case sum < target:
			left++ // only increasing left can raise the sum
		default:
			right-- // only decreasing right can lower it
		}
	}
	return 0, 0, false
}

// RemoveDuplicates dedupes a sorted slice in place: same direction,
// different speeds.
func RemoveDuplicates(nums []int) int {
	if len(nums) == 0 {
		return 0
	}
	writer := 1 // only advances on a genuinely new value
	for reader := 1; reader < len(nums); reader++ {
		if nums[reader] != nums[writer-1] {
			nums[writer] = nums[reader]
			writer++
		}
	}
	return writer
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Opposite-ends convergence: pair summing to target in a sorted array. */
function twoSumSorted(nums, target) {
  let left = 0;
  let right = nums.length - 1;

  while (left < right) {
    const sum = nums[left] + nums[right];
    if (sum === target) return [left, right];
    if (sum < target) left++;   // only increasing left can raise the sum
    else right--;               // only decreasing right can lower it
  }
  return null;
}

/** Same-direction, different speeds: dedupe a sorted array in place. */
function removeDuplicates(nums) {
  if (nums.length === 0) return 0;
  let writer = 1; // only advances on a genuinely new value
  for (let reader = 1; reader < nums.length; reader++) {
    if (nums[reader] !== nums[writer - 1]) {
      nums[writer] = nums[reader];
      writer++;
    }
  }
  return writer;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 167,
            title = "Two Sum II - Input Array Is Sorted",
            difficulty = Difficulty.MEDIUM,
            idea = "The textbook opposite-ends convergence: the sortedness is what justifies moving exactly one pointer per step, turning an O(n²) or hash-table O(n) solution into O(n) time and O(1) space.",
            askedAt = "Amazon, Meta",
        ),
        Question(
            id = 11,
            title = "Container With Most Water",
            difficulty = Difficulty.MEDIUM,
            idea = "Start at both ends and always move the shorter side inward — it's the bottleneck, so it's the only side that could possibly improve the answer. The taller side moving inward can only ever make things worse or equal.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
        Question(
            id = 15,
            title = "3Sum",
            difficulty = Difficulty.MEDIUM,
            idea = "Sort first, then fix one element and two-pointer the sorted remainder for the other two — turning what looks like an O(n³) triple loop into O(n²). Skipping duplicate values at each level is what avoids duplicate triplets in the output.",
            askedAt = "Amazon, Meta, Microsoft",
        ),
    ),

    related = listOf("arrays", "sliding-window", "binary-search"),
    references = Refs.basecs(),
)

val SlidingWindow = Topic(
    id = "sliding-window",
    title = "Sliding Window",
    tagline = "Slide a window's edges instead of re-scanning it from scratch.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Maintain a contiguous window and slide its edges one step at a time, reusing the previous window's work instead of recomputing from scratch — O(n) instead of O(n·k) or O(n²).",
        "Fixed-size windows just shift by one; variable-size windows grow the right edge greedily and shrink the left edge only when a constraint is violated.",
        "Lives or dies on incremental updates: whatever the window is tracking needs to be cheaply updatable as elements enter and leave, not recomputed each slide.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A lot of subarray or substring problems have an obvious brute-force shape: for every possible window start, scan forward to check every possible window end. That's O(n²) at best, or O(n·k) if the window size k is fixed, and almost all of that work is wasted — each window overlaps heavily with its neighbours, so most of what you'd recompute was already computed one step ago.",
        "Sliding window's insight is to never throw that overlap away. Keep a window defined by a left and right edge, and instead of re-scanning it after every move, update it incrementally: when the right edge advances, add the newly included element's contribution; when the left edge advances, remove the newly excluded element's contribution. Whatever the window tracks — a running sum, a count of characters, a maximum — gets updated in O(1) per edge move rather than recomputed over the whole window.",
        "There are two shapes this takes. A **fixed-size window** simply slides: both edges move together, one step at a time, the natural fit for \"find the best window of exactly size k.\" A **variable-size window** is more common in interviews: the right edge advances greedily to grow the window, and the left edge only advances when the window violates some constraint — too many distinct characters, a sum that's too large — shrinking just enough to satisfy it again before continuing to grow.",
        "The reason variable windows are still O(n) despite two nested-looking loops is that the left edge, across the entire algorithm's run, can only ever move forward and can only move at most n times total — it never resets or moves backward. Two pointers that each individually move at most n times, combined, is still O(n) overall, the same argument that makes two pointers linear rather than quadratic.",
        "The part that actually needs care is picking a data structure for \"what the window is tracking\" that supports both adding and removing in O(1) or O(log n) — a running integer sum, a hash map of character counts, or a deque for a running maximum are the usual choices. Get that part wrong — recomputing a max by scanning the window every slide, say — and the window mechanism doesn't save you anything.",
    ),

    origin = "Sliding window is a **technique rather than a dated, single-author invention**, in the same category as two pointers — it's the natural response to noticing that adjacent windows over the same array share almost all their contents, and versions of the idea appear throughout algorithms literature, particularly signal processing and string matching, without one clean point of origin.",

    keyPoints = listOf(
        "**Reuse the previous window's work.** Update incrementally as edges move — add what enters, remove what leaves — rather than recomputing the window's tracked value from scratch on every slide.",
        "**Fixed-size windows slide**; **variable-size windows grow greedily and shrink only when a constraint is violated** — the second is the more common interview shape.",
        "**Still O(n) overall**, even though the left edge inside a variable window looks like a second loop — it only ever moves forward, and moves at most n times total across the entire run.",
        "**The data structure tracking the window's state has to support O(1) or O(log n) add/remove** — a running sum, a hash map of counts, or a deque for a running max are the standard choices.",
        "**A hash map of character/element counts is the default state** for 'window contains at most k distinct things' or substring-matching problems specifically.",
    ),

    complexity = listOf(
        ComplexityRow("Fixed-size window", "O(n)", "O(k) or O(1)", "One pass; each slide does O(1) incremental work if the tracked value supports it."),
        ComplexityRow("Variable-size window", "O(n)", "O(k)", "Amortised: the left edge moves at most n times total across the whole run, same argument as two pointers."),
    ),

    pitfalls = listOf(
        "Recomputing the window's tracked value from scratch after every slide instead of updating incrementally — this silently turns an O(n) sliding window into an O(n·k) brute force with extra bookkeeping.",
        "Shrinking the left edge by more than one step, or under the wrong condition — the left edge should advance exactly until the constraint is satisfied again, not further.",
        "Forgetting to update the tracked state when an element leaves the window, only when one enters — a classic source of a window that silently drifts wrong after a few shrinks.",
        "Using an O(n) scan to find a running maximum/minimum inside the window on every slide — a monotonic deque keeps that operation O(1) amortised instead.",
    ),

    steps = listOf(
        "Initialise the window's left and right edges at the start, along with whatever state tracks the window's contents.",
        "Advance the right edge, adding the new element's contribution to the tracked state.",
        "If the window now violates a constraint, advance the left edge — removing each excluded element's contribution — until the constraint holds again.",
        "Record the window as a candidate answer if applicable, then continue advancing the right edge.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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
        """.trim(),

        Lang.GO to """
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
        """.trim(),

        Lang.JAVASCRIPT to """
/** Fixed-size window: max sum of any k consecutive elements. */
function maxSumWindow(nums, k) {
  let windowSum = nums.slice(0, k).reduce((a, b) => a + b, 0);
  let best = windowSum;

  for (let right = k; right < nums.length; right++) {
    windowSum += nums[right] - nums[right - k]; // add entering, remove leaving
    best = Math.max(best, windowSum);
  }
  return best;
}

/** Variable-size window: longest substring with no repeated characters. */
function longestUniqueSubstring(s) {
  const lastSeen = new Map();
  let left = 0;
  let best = 0;

  for (let right = 0; right < s.length; right++) {
    const char = s[right];
    // Shrink only if the repeat is inside the current window, not before it.
    if (lastSeen.has(char) && lastSeen.get(char) >= left) {
      left = lastSeen.get(char) + 1;
    }
    lastSeen.set(char, right);
    best = Math.max(best, right - left + 1);
  }
  return best;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 3,
            title = "Longest Substring Without Repeating Characters",
            difficulty = Difficulty.MEDIUM,
            idea = "A variable window with a hash map of last-seen indices. The trap is shrinking on any previously-seen character instead of only one whose last occurrence is inside the current window — a repeat from before the window started is irrelevant.",
            askedAt = "Amazon, Meta, Bloomberg — extremely common",
        ),
        Question(
            id = 76,
            title = "Minimum Window Substring",
            difficulty = Difficulty.HARD,
            idea = "Grow the window until it satisfies the character-count requirement, then shrink it as far as possible while it still does, recording the smallest valid window seen. Shrinking-while-valid rather than shrinking-until-valid is the subtlety that trips people up.",
            askedAt = "Meta, Uber, Google",
        ),
        Question(
            id = 239,
            title = "Sliding Window Maximum",
            difficulty = Difficulty.HARD,
            idea = "A monotonic deque keeps the current window's maximum accessible in O(1): pop smaller elements from the back before pushing a new one, since they can never be the max while the new, larger element is still in the window.",
            askedAt = "Amazon, Google, Meta",
        ),
    ),

    related = listOf("two-pointers", "arrays", "hash-tables"),
    references = Refs.basecs(),
)

val Backtracking = Topic(
    id = "backtracking",
    title = "Backtracking",
    tagline = "Try a choice, recurse, and undo it the moment it can't work.",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "Explore choices one at a time via recursion, undoing ('backtracking') the moment a partial choice can't lead anywhere valid — prunes whole branches instead of generating every possibility first.",
        "Always the same three steps: choose, explore, un-choose — the un-choose step is what separates it from plain recursive enumeration.",
        "Pruning early is the entire performance story: checking a constraint before recursing avoids generating exponentially many dead branches.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Some problems can only be solved by trying possibilities — placing queens on a board, choosing a subset, building a valid permutation — and the number of possibilities is exponential. Backtracking is the standard way to explore that space without generating every possibility up front: make one choice, recurse into the consequences of that choice, and if it turns out to be a dead end, undo it and try the next option instead of ever having built the full tree of possibilities in memory.",
        "The pattern is always the same three-step shape: **choose** an option, **explore** by recursing with that choice in effect, and **un-choose** by undoing it before trying the next option at the same level. That un-choose step is the entire idea and the thing that's easy to forget — without it, a mutable data structure used to track the current partial solution (a board, a path, a set of used elements) keeps accumulating stale state from abandoned branches into the next one it tries.",
        "What makes backtracking fast in practice — as opposed to just \"recursive enumeration with extra bookkeeping\" — is **pruning**: checking whether a partial choice can possibly lead to a valid solution *before* recursing into it, rather than after. Placing a queen where it immediately attacks another queen is checked and rejected in O(1), instead of recursing several levels deep into a board that was already doomed. The difference between \"prune early\" and \"generate everything, then filter\" is frequently the difference between a solution that finishes and one that never does, even though both are technically correct.",
        "Backtracking and DFS are close relatives for the same reason quicksort and merge sort both recurse: both explore a tree of possibilities depth-first. What's different is the goal — DFS is usually looking for reachability or an existing path, while backtracking is actively constructing candidate solutions and needs the explicit undo step because it mutates shared state (a board, a running combination) rather than just visiting nodes.",
    ),

    origin = "The term **'backtrack' is credited to D.H. Lehmer**, who used it in the 1950s to describe the general technique of systematically abandoning partial solutions and trying the next option, as documented by Donald Knuth in *The Art of Computer Programming*. The technique itself long predates the name — it is essentially the formalisation of exhaustive trial-and-error search with the explicit discipline of undoing a choice the moment it's known to fail.",

    keyPoints = listOf(
        "**Choose, explore, un-choose** — the fixed shape of every backtracking solution. Skipping the un-choose step is the most common bug, leaking state from abandoned branches into sibling attempts.",
        "**Pruning before recursing, not after**, is what makes it fast — checking a constraint early avoids generating and then discarding exponentially many dead branches.",
        "**Shares its recursive skeleton with DFS**, but actively builds and mutates a candidate solution as it goes, which is exactly why the undo step exists and DFS-for-reachability doesn't need one.",
        "**Time complexity is usually exponential** in the size of the choice space — the practical question is always how much pruning cuts that exponent down, not whether it's exponential in theory.",
        "**Passing an index or a 'used' set explicitly**, rather than mutating and forgetting to restore, is the safer default when a bug in the undo step would otherwise be easy to introduce.",
    ),

    complexity = listOf(
        ComplexityRow("Generic backtracking", "O(b^d)", "O(d)", "b = branching factor, d = depth — exponential in the worst case; pruning reduces the effective branching factor."),
        ComplexityRow("Permutations of n items", "O(n!)", "O(n)", "Every ordering is a valid leaf — no pruning is possible because every partial choice can still lead somewhere."),
    ),

    pitfalls = listOf(
        "Forgetting to un-choose — undo the mutation — before trying the next option at the same level. This leaks state from one branch into its siblings, producing subtly wrong results rather than a crash.",
        "Pruning after recursing instead of before — checking a constraint only once you're already several levels deep wastes all the work of getting there.",
        "Generating every full candidate before checking validity, instead of checking incrementally as each choice is made — the difference between exponential-with-pruning and just exponential.",
        "Using a mutable shared structure (like a `visited` set) without a matching removal on the way back out — the classic partner bug to forgetting the un-choose step generally.",
    ),

    steps = listOf(
        "Check whether the current partial choice is already invalid — if so, prune: return immediately without recursing further.",
        "If the partial choice is a complete, valid solution, record it.",
        "Otherwise, for each available next choice: make the choice, recurse, then undo the choice before trying the next one.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** All subsets of nums, built by choosing/skipping each element in turn. */
fun subsets(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val current = mutableListOf<Int>()

    fun backtrack(index: Int) {
        if (index == nums.size) {
            result.add(current.toList()) // snapshot — current keeps mutating
            return
        }
        backtrack(index + 1) // choice 1: skip nums[index]

        current.add(nums[index]) // choice 2: include nums[index]
        backtrack(index + 1)
        current.removeAt(current.lastIndex) // the un-choose step
    }

    backtrack(0)
    return result
}

/** N-Queens: prune the moment a placement attacks an existing queen. */
fun solveNQueens(n: Int): Int {
    val cols = BooleanArray(n)
    val diag1 = BooleanArray(2 * n) // row + col
    val diag2 = BooleanArray(2 * n) // row - col + n
    var solutions = 0

    fun backtrack(row: Int) {
        if (row == n) {
            solutions++
            return
        }
        for (col in 0 until n) {
            val d1 = row + col
            val d2 = row - col + n
            if (cols[col] || diag1[d1] || diag2[d2]) continue // pruned before recursing

            cols[col] = true; diag1[d1] = true; diag2[d2] = true
            backtrack(row + 1)
            cols[col] = false; diag1[d1] = false; diag2[d2] = false // un-choose
        }
    }

    backtrack(0)
    return solutions
}
        """.trim(),

        Lang.GO to """
// Subsets returns all subsets of nums, built by choosing/skipping each
// element in turn.
func Subsets(nums []int) [][]int {
	var result [][]int
	var current []int

	var backtrack func(index int)
	backtrack = func(index int) {
		if index == len(nums) {
			result = append(result, append([]int(nil), current...)) // snapshot
			return
		}
		backtrack(index + 1) // choice 1: skip nums[index]

		current = append(current, nums[index]) // choice 2: include nums[index]
		backtrack(index + 1)
		current = current[:len(current)-1] // the un-choose step
	}

	backtrack(0)
	return result
}

// SolveNQueens prunes the moment a placement attacks an existing queen.
func SolveNQueens(n int) int {
	cols := make([]bool, n)
	diag1 := make([]bool, 2*n) // row + col
	diag2 := make([]bool, 2*n) // row - col + n
	solutions := 0

	var backtrack func(row int)
	backtrack = func(row int) {
		if row == n {
			solutions++
			return
		}
		for col := 0; col < n; col++ {
			d1, d2 := row+col, row-col+n
			if cols[col] || diag1[d1] || diag2[d2] {
				continue // pruned before recursing
			}
			cols[col], diag1[d1], diag2[d2] = true, true, true
			backtrack(row + 1)
			cols[col], diag1[d1], diag2[d2] = false, false, false // un-choose
		}
	}

	backtrack(0)
	return solutions
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** All subsets of nums, built by choosing/skipping each element in turn. */
function subsets(nums) {
  const result = [];
  const current = [];

  function backtrack(index) {
    if (index === nums.length) {
      result.push([...current]); // snapshot — current keeps mutating
      return;
    }
    backtrack(index + 1); // choice 1: skip nums[index]

    current.push(nums[index]); // choice 2: include nums[index]
    backtrack(index + 1);
    current.pop(); // the un-choose step
  }

  backtrack(0);
  return result;
}

/** N-Queens: prune the moment a placement attacks an existing queen. */
function solveNQueens(n) {
  const cols = new Array(n).fill(false);
  const diag1 = new Array(2 * n).fill(false); // row + col
  const diag2 = new Array(2 * n).fill(false); // row - col + n
  let solutions = 0;

  function backtrack(row) {
    if (row === n) {
      solutions++;
      return;
    }
    for (let col = 0; col < n; col++) {
      const d1 = row + col;
      const d2 = row - col + n;
      if (cols[col] || diag1[d1] || diag2[d2]) continue; // pruned before recursing

      cols[col] = diag1[d1] = diag2[d2] = true;
      backtrack(row + 1);
      cols[col] = diag1[d1] = diag2[d2] = false; // un-choose
    }
  }

  backtrack(0);
  return solutions;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 78,
            title = "Subsets",
            difficulty = Difficulty.MEDIUM,
            idea = "The cleanest choose/skip backtracking skeleton — no pruning is even possible, since every partial subset is valid. Good for drilling the choose-explore-un-choose shape before adding constraints.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
        Question(
            id = 46,
            title = "Permutations",
            difficulty = Difficulty.MEDIUM,
            idea = "A 'used' set (or boolean array) tracks which elements are already placed — the un-choose step here means removing the element from 'used' after backtracking out of that branch, not just popping it from the current path.",
            askedAt = "Amazon, Microsoft, Bloomberg",
        ),
        Question(
            id = 51,
            title = "N-Queens",
            difficulty = Difficulty.HARD,
            idea = "The canonical pruning example: checking column and both diagonal attacks in O(1) before recursing avoids ever descending into a doomed board, which is the entire difference between this finishing and not.",
            askedAt = "Amazon, Google, Microsoft",
        ),
    ),

    related = listOf("dfs", "coin-change"),
    references = Refs.basecs(),
)

val SelectionSort = Topic(
    id = "selection-sort",
    title = "Selection Sort",
    tagline = "Find the smallest remaining element, and put it exactly where it belongs.",
    level = Level.BASIC,

    quickSummary = listOf(
        "Repeatedly scan the unsorted remainder for its minimum and swap it into place — O(n²) comparisons every time, regardless of input.",
        "The fewest writes of any common sort — exactly n swaps total — which matters when writes are far more expensive than comparisons.",
        "Not stable, and it never gets faster on nearly-sorted input, unlike insertion sort or bubble sort.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Selection sort is the most literal possible translation of \"sort this\" into an algorithm: find the smallest element, put it first; find the next smallest, put it second; repeat until done. Each pass scans the entire unsorted remainder to find its minimum, then swaps that minimum into the next open slot at the front.",
        "That literalism is exactly why it costs O(n²) unconditionally — finding a minimum in an unsorted range fundamentally requires looking at every element in it, and the range shrinks by only one each pass. Unlike insertion sort or bubble sort, there is no shortcut for nearly-sorted input: selection sort scans the full remaining range on every single pass regardless of how close to sorted it already is.",
        "What it does have going for it is the number of writes. Each pass does exactly one swap — the found minimum into its slot — so the entire sort performs at most n swaps total, dramatically fewer than most other O(n²) sorts. That property matters more than it sounds: on hardware where writes are expensive relative to reads, minimising writes at the cost of extra comparisons can be a real, deliberate trade.",
        "It is not stable as usually implemented — swapping a distant minimum into place can jump it past equal elements it should have stayed behind — and there is no comparison-based improvement that fixes this without giving up the write-count advantage. In practice, selection sort is taught mainly as the simplest possible sorting algorithm to reason about correctness for, not as something to reach for.",
    ),

    origin = "Unlike most structures in this curriculum, selection sort has **no single documented inventor or publication** — it is one of the earliest and most obvious ways to sort by hand, and it appears in computing literature from the 1950s onward as a baseline algorithm rather than a novel contribution attributed to any one person.",

    keyPoints = listOf(
        "**O(n²) comparisons in every case** — best, average and worst. There is no shortcut for nearly-sorted input, unlike insertion sort or bubble sort.",
        "**Exactly n swaps total** — one per pass — the fewest writes of any common comparison sort. Matters specifically when writes are expensive relative to comparisons.",
        "**Not stable** as usually implemented — swapping a distant minimum into place can reorder equal elements.",
        "**In-place, O(1) auxiliary space** — no scratch array needed.",
        "Rarely used in production; its value is almost entirely pedagogical — the simplest correctness argument of any sort in this curriculum.",
    ),

    complexity = listOf(
        ComplexityRow("Best / average / worst", "O(n²)", "O(1)", "Every pass scans the full unsorted remainder regardless of input order."),
        ComplexityRow("Swaps", "O(n)", "—", "Exactly one swap per pass — the fewest writes of any common O(n²) sort."),
    ),

    pitfalls = listOf(
        "Assuming it's stable — a swap can move a distant minimum past equal elements, changing their relative order.",
        "Using it on data where nearly-sorted input is common — it gets no benefit from that, unlike insertion sort, which drops close to O(n) on nearly-sorted input.",
        "Choosing it for anything performance-sensitive purely for its 'simplicity' — the O(n²) comparison count makes it a poor choice past a few hundred elements regardless of the low write count.",
    ),

    steps = listOf(
        "Set a pointer to the start of the unsorted remainder.",
        "Scan the entire remainder to find its minimum element.",
        "Swap that minimum into the pointer's position.",
        "Advance the pointer by one and repeat until the remainder is empty.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
fun selectionSort(nums: IntArray) {
    for (i in nums.indices) {
        var minIndex = i
        for (j in i + 1 until nums.size) {
            if (nums[j] < nums[minIndex]) minIndex = j
        }
        // Exactly one swap per pass — this is the whole write budget.
        nums[i] = nums[minIndex].also { nums[minIndex] = nums[i] }
    }
}
        """.trim(),

        Lang.GO to """
func SelectionSort(nums []int) {
	for i := range nums {
		minIndex := i
		for j := i + 1; j < len(nums); j++ {
			if nums[j] < nums[minIndex] {
				minIndex = j
			}
		}
		// Exactly one swap per pass — this is the whole write budget.
		nums[i], nums[minIndex] = nums[minIndex], nums[i]
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
function selectionSort(nums) {
  for (let i = 0; i < nums.length; i++) {
    let minIndex = i;
    for (let j = i + 1; j < nums.length; j++) {
      if (nums[j] < nums[minIndex]) minIndex = j;
    }
    // Exactly one swap per pass — this is the whole write budget.
    [nums[i], nums[minIndex]] = [nums[minIndex], nums[i]];
  }
  return nums;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "A naive selection sort times out against this problem's constraints — the useful exercise is explaining precisely why (O(n²) comparisons with no early exit) before reaching for merge sort or heap sort instead.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
        Question(
            id = 215,
            title = "Kth Largest Element in an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Selection sort's core idea taken only partway: 'select the maximum' k times instead of n times gives O(n·k) — fine for small k, but a heap or quickselect beats it once k grows, which is worth being able to say out loud.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
    ),

    related = listOf("arrays", "bubble-sort", "insertion-sort"),
    references = Refs.basecs(),
)

val BubbleSort = Topic(
    id = "bubble-sort",
    title = "Bubble Sort",
    tagline = "Swap adjacent out-of-order pairs, over and over, until nothing moves.",
    level = Level.BASIC,

    quickSummary = listOf(
        "Repeatedly walk the array swapping adjacent out-of-order pairs — after each full pass, the largest remaining element has 'bubbled' to its final position.",
        "O(n²) worst and average case, but O(n) best case on already-sorted input if you track whether any swap happened and stop early.",
        "Stable by construction — equal elements are never swapped past each other, since a swap only ever happens on a strict inequality.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Bubble sort's whole idea is in its name: repeatedly scan adjacent pairs, and whenever a pair is out of order, swap them. After one full pass across the array, the single largest element is guaranteed to have been swapped all the way to the end — it \"bubbles up\" past everything smaller than it, one adjacent swap at a time, because whatever it's compared against is smaller and loses the swap.",
        "Repeating that pass n times guarantees the whole array is sorted, because each pass settles at least one more element — the current maximum of whatever's left — into its final position at the end. That is also exactly why the inner loop can shrink by one each pass: the tail is already correct and never needs re-checking.",
        "The one improvement worth knowing is a flag: track whether any swap happened during a pass, and stop immediately if not. An already-sorted array then finishes in a single O(n) pass instead of grinding through all n passes doing nothing — the only common O(n²) sort with that specific best-case behaviour built in this simply.",
        "Bubble sort is stable for a subtle but clean reason: a swap only ever happens when one element is strictly greater than its neighbour. Two equal elements are never swapped past each other, so their relative order survives untouched — the same property merge sort has to engineer deliberately by choosing which side to prefer on ties, bubble sort gets for free from the comparison itself.",
    ),

    origin = "The earliest known description of bubble sort appears in **E.H. Friend's 1956 paper 'Sorting on Electronic Computer Systems'** in the Journal of the ACM, though the name 'bubble sort' itself didn't appear in print until the 1960s. Donald Knuth later devoted several pages of *The Art of Computer Programming* to analysing why it performs worse in practice than insertion sort despite the identical O(n²) bound — a rare case of a sort's own textbook analysis actively steering people away from using it.",

    keyPoints = listOf(
        "**O(n²) worst and average case** — every pair gets compared, repeatedly, across up to n passes.",
        "**O(n) best case with an early-exit flag**: if a full pass makes no swaps, the array is already sorted and the algorithm can stop immediately.",
        "**Stable by construction** — a swap only happens on a strict inequality, so equal elements are never swapped past each other.",
        "**In-place, O(1) auxiliary space.**",
        "Almost never used past a classroom setting — insertion sort dominates it in practice at every input size, with the same O(n²) worst case but fewer total operations.",
    ),

    complexity = listOf(
        ComplexityRow("Best case (with early exit)", "O(n)", "O(1)", "One pass with no swaps confirms the array is already sorted."),
        ComplexityRow("Average / worst case", "O(n²)", "O(1)", "Up to n passes, each shrinking by one settled element at the tail."),
    ),

    pitfalls = listOf(
        "Forgetting the early-exit flag — without it, bubble sort grinds through all n passes even on already-sorted input, wasting the one case where it could be fast.",
        "Assuming O(n²) sorts are interchangeable — bubble sort does strictly more swaps than insertion sort for the same input in the general case, so there's rarely a reason to prefer it.",
        "Confusing 'stable' with 'fast' — bubble sort's stability is a genuine property, but it doesn't compensate for its practical slowness.",
    ),

    steps = listOf(
        "Walk the array comparing each adjacent pair.",
        "If a pair is out of order, swap it.",
        "After one full pass, the largest unsettled element has bubbled to the end — shrink the range by one.",
        "Repeat until a full pass makes no swaps, or the range is empty.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
fun bubbleSort(nums: IntArray) {
    for (end in nums.lastIndex downTo 1) {
        var swapped = false
        for (i in 0 until end) {
            if (nums[i] > nums[i + 1]) {
                nums[i] = nums[i + 1].also { nums[i + 1] = nums[i] }
                swapped = true
            }
        }
        // No swaps this pass means the array is already sorted.
        if (!swapped) break
    }
}
        """.trim(),

        Lang.GO to """
func BubbleSort(nums []int) {
	for end := len(nums) - 1; end >= 1; end-- {
		swapped := false
		for i := 0; i < end; i++ {
			if nums[i] > nums[i+1] {
				nums[i], nums[i+1] = nums[i+1], nums[i]
				swapped = true
			}
		}
		// No swaps this pass means the array is already sorted.
		if !swapped {
			break
		}
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
function bubbleSort(nums) {
  for (let end = nums.length - 1; end >= 1; end--) {
    let swapped = false;
    for (let i = 0; i < end; i++) {
      if (nums[i] > nums[i + 1]) {
        [nums[i], nums[i + 1]] = [nums[i + 1], nums[i]];
        swapped = true;
      }
    }
    // No swaps this pass means the array is already sorted.
    if (!swapped) break;
  }
  return nums;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 905,
            title = "Sort Array By Parity",
            difficulty = Difficulty.EASY,
            idea = "A bubble-pass-style adjacent swap works, but a two-pointer partition (swap evens to the front, odds to the back) does it in one pass instead of many — worth showing you know the O(n²) bubble approach and why the two-pointer one beats it.",
            askedAt = "Amazon, warm-up screens",
        ),
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Bubble sort passes correctness easily and fails performance immediately — a clean way to demonstrate you understand the gap between 'works' and 'works at this input size'.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
    ),

    related = listOf("arrays", "selection-sort", "insertion-sort"),
    references = Refs.basecs(),
)

val InsertionSort = Topic(
    id = "insertion-sort",
    title = "Insertion Sort",
    tagline = "Grow a sorted prefix one element at a time, sliding each new value into place.",
    level = Level.BASIC,

    quickSummary = listOf(
        "Grow a sorted prefix one element at a time: take the next element and slide it left past everything bigger — exactly like sorting a hand of playing cards.",
        "O(n²) worst case, but O(n) best case on already-sorted input, and noticeably fast in practice on *nearly*-sorted data, which is rarer for other O(n²) sorts.",
        "The standard cutover target for hybrid sorts: quicksort and merge sort implementations switch to insertion sort on small sub-arrays, because its low overhead wins below roughly 10-20 elements.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Insertion sort is the algorithm most people already use without thinking about it: sorting a hand of playing cards by picking up each new card and sliding it into the correct place among the cards already sorted in your hand. Formalised, that means maintaining a sorted prefix of the array and, for each new element, sliding it leftward past every already-sorted element bigger than it until it lands in the right spot.",
        "That sliding is where the cost comes from. In the worst case — a reverse-sorted array — every new element has to slide all the way to the front, past everything already placed, giving the same O(n²) bound as selection or bubble sort. But unlike those two, insertion sort's cost is genuinely sensitive to how sorted the input already is: an element that's already close to its correct position slides only a short distance, so a nearly-sorted array finishes close to O(n) rather than grinding through the full O(n²).",
        "That input-sensitivity is precisely why insertion sort is the standard choice for small sub-arrays inside hybrid sorts. Once quicksort's or merge sort's recursion narrows down to a handful of elements, insertion sort's low constant-factor overhead — no recursive calls, no partition or merge bookkeeping — beats the asymptotically better algorithms outright, which is why production sort implementations switch over below roughly 10-20 elements rather than recursing all the way down.",
        "It is also stable, and for the same structural reason bubble sort is: an element only ever slides past strictly larger elements, never past equal ones, so equal elements keep their relative order automatically, with no extra bookkeeping required.",
    ),

    origin = "Insertion sort has **no single documented inventor**, unlike most algorithms in this curriculum — it is the formalisation of how people have manually sorted objects for as long as sorting has been a task at all, and it appears in early computing literature from the 1940s and 50s as an obvious baseline technique rather than a novel contribution. What is well documented is its practical role: it is the textbook example of an algorithm whose worst-case complexity understates its real-world usefulness, precisely because of how it behaves on nearly-sorted data.",

    keyPoints = listOf(
        "**O(n²) worst case, O(n) best case** — and, unusually for an O(n²) sort, genuinely fast in practice on *nearly*-sorted data, not just perfectly sorted data.",
        "**In-place, O(1) auxiliary space**, and **stable** — an element only ever slides past strictly larger neighbours, never past equal ones.",
        "**The standard cutover target for hybrid sorts.** Quicksort and merge sort switch to insertion sort on small sub-arrays (roughly 10-20 elements), because its low overhead wins at that size despite the worse asymptotic bound.",
        "**Adaptive**: the number of swaps is proportional to the number of *inversions* in the input — pairs out of order relative to each other — so 'almost sorted' data does genuinely less work, not just the same work with a smaller constant.",
        "Online-friendly: it can sort a stream as elements arrive, inserting each new one into the already-sorted prefix — something selection sort and simple bubble sort can't do as naturally.",
    ),

    complexity = listOf(
        ComplexityRow("Best case", "O(n)", "O(1)", "Already-sorted input — every new element slides zero positions."),
        ComplexityRow("Average / worst case", "O(n²)", "O(1)", "Reverse-sorted input forces every new element all the way to the front."),
    ),

    pitfalls = listOf(
        "Reaching for it on large, randomly-ordered data — the O(n²) worst case is real, and it will lose badly to merge sort or quicksort at scale.",
        "Forgetting the input-sensitivity is about *inversions*, not just 'is it sorted' — data can look unsorted overall while having few inversions (e.g. a sorted array with one element moved), and insertion sort still handles that fast.",
        "Implementing the inner slide with a series of full swaps instead of one shift-and-place — correct either way, but a single assignment per shifted element avoids unnecessary writes.",
    ),

    steps = listOf(
        "Treat the first element as a sorted prefix of length 1.",
        "Take the next element and compare it against the end of the sorted prefix.",
        "Shift every element in the prefix bigger than it one slot to the right.",
        "Insert the element into the gap that shift created.",
        "Repeat, growing the sorted prefix by one each time, until the whole array is covered.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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
        """.trim(),

        Lang.GO to """
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
        """.trim(),

        Lang.JAVASCRIPT to """
function insertionSort(nums) {
  for (let i = 1; i < nums.length; i++) {
    const current = nums[i];
    let j = i - 1;
    // Shift everything bigger than current one slot right to make room.
    while (j >= 0 && nums[j] > current) {
      nums[j + 1] = nums[j];
      j--;
    }
    nums[j + 1] = current;
  }
  return nums;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 147,
            title = "Insertion Sort List",
            difficulty = Difficulty.MEDIUM,
            idea = "The same idea applied to a linked list: relink nodes into a growing sorted prefix instead of shifting array slots. The usual trap is losing the head reference — a dummy head node removes that special case.",
            askedAt = "Amazon, Microsoft",
        ),
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "A useful contrast question: naive insertion sort fails this problem's constraints outright, which is exactly the input-size threshold where switching to merge sort or quicksort stops being optional.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
    ),

    related = listOf("arrays", "linked-lists", "selection-sort", "bubble-sort", "quicksort"),
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

    related = listOf("binary-search", "coin-change", "arrays", "linked-lists", "heaps", "quicksort", "heap-sort"),
    references = Refs.basecs(),
)

val GraphRepresentation = Topic(
    id = "graph-representation",
    title = "Representing Graphs",
    tagline = "Adjacency list or adjacency matrix — the choice decides what's cheap and what's expensive.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Adjacency list and adjacency matrix trade off exactly opposite things: a list is compact and fast to iterate neighbours, a matrix is fast to check 'is there an edge here?' at the cost of O(V²) space.",
        "Most real graphs are sparse — E is much smaller than V² — which is why adjacency lists are the default; matrices earn their keep on dense graphs or when edge-existence checks dominate.",
        "The representation choice changes the complexity of every traversal built on it — BFS and DFS are O(V + E) on a list but O(V²) on a matrix.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A graph is just a set of nodes and the connections between them, but \"just\" hides a real decision: how do you actually store which nodes connect to which? The two standard answers pull in opposite directions. An **adjacency matrix** is a V×V grid where cell (i, j) says whether an edge exists between i and j — checking for a specific edge is O(1), but the grid costs O(V²) space no matter how few edges actually exist. An **adjacency list** instead gives each node its own list of neighbours — checking for a specific edge means scanning that list, but the total space is O(V + E), proportional to what's actually there.",
        "That distinction matters enormously in practice because most real graphs are **sparse**: a social network with millions of users has nowhere near a million² friendships. Storing a million-node graph as a matrix would need a trillion cells, the overwhelming majority holding \"no edge\" — a spectacular waste next to an adjacency list's proportional cost. Dense graphs, where E approaches V², are the exception where a matrix's O(1) edge lookups start to pay for themselves.",
        "The representation choice isn't cosmetic — it changes the complexity of every algorithm built on top of it. Visiting every neighbour of every node during a BFS or DFS costs O(V + E) total on an adjacency list, because each node's neighbour list is walked exactly once across the whole traversal. The same traversal on a matrix costs O(V²), because finding a node's neighbours means scanning an entire row of size V, edge or no edge. Same traversal, same graph, different asymptotic cost — purely from the underlying representation.",
        "Weighted graphs extend either representation the same way: a matrix cell holds the edge's weight instead of a boolean, and a list entry pairs each neighbour with its weight. Directed graphs are handled identically by both — a matrix simply loses its symmetry across the diagonal, and a list only records the direction each edge actually points.",
    ),

    origin = "Representing a network as a grid of connections traces to **Dénes König's 1936 book *Theorie der endlichen und unendlichen Graphen***, the first systematic textbook treatment of graph theory, which formalised the matrix view mathematically. **Adjacency lists have no comparable single-inventor origin** — they are simply the natural computational answer once programmers needed to avoid paying O(V²) space for graphs that were mostly empty, and appear informally throughout early graph-algorithm literature from the 1950s and 60s without a clean point of origin.",

    keyPoints = listOf(
        "**Adjacency list**: O(V + E) space, O(1) amortised to list a node's neighbours, O(degree) to check a specific edge. The default for sparse graphs — which is most real graphs.",
        "**Adjacency matrix**: O(V²) space, O(1) to check a specific edge, O(V) to list a node's neighbours (scan the row). Wins when the graph is dense or edge-existence checks dominate.",
        "**Traversal cost inherits the representation's cost**: BFS/DFS are O(V + E) on a list, O(V²) on a matrix — the same algorithm, different asymptotic bound.",
        "**Weighted graphs**: a matrix cell holds the weight instead of a boolean; a list entry pairs each neighbour with its weight.",
        "**Directed graphs**: a matrix loses symmetry across the diagonal; a list simply records only the direction each edge points.",
    ),

    complexity = listOf(
        ComplexityRow("Adjacency list — space", "—", "O(V + E)", "Proportional to what actually exists — the default for sparse graphs."),
        ComplexityRow("Adjacency matrix — space", "—", "O(V²)", "Fixed cost regardless of how many edges actually exist."),
        ComplexityRow("List a node's neighbours", "O(degree)", "—", "List: direct. Matrix: O(V), scanning the full row regardless of degree."),
        ComplexityRow("Check a specific edge", "O(degree)", "—", "List: scan the node's neighbours. Matrix: O(1), direct lookup."),
    ),

    pitfalls = listOf(
        "Defaulting to an adjacency matrix out of habit on a sparse graph — the O(V²) space cost is real and often dwarfs what an adjacency list would need.",
        "Forgetting that a matrix-backed traversal is O(V²), not O(V + E) — the two representations aren't interchangeable without changing the complexity of everything built on top.",
        "Using an adjacency list but repeatedly checking 'is there an edge from A to B' in a hot loop — that's an O(degree) scan every time; a hash set of neighbours per node fixes it at the cost of extra memory.",
        "Mismanaging directed vs. undirected edges — an undirected edge needs to be recorded (or checked) in both directions, easy to forget in a hand-rolled adjacency list.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Adjacency list: the default for sparse graphs. O(V + E) space. */
fun buildAdjacencyList(vertexCount: Int, edges: List<Pair<Int, Int>>): List<MutableList<Int>> {
    val adj = List(vertexCount) { mutableListOf<Int>() }
    for ((u, v) in edges) {
        adj[u].add(v)
        adj[v].add(u) // omit this line for a directed graph
    }
    return adj
}

/** Adjacency matrix: O(V²) space, O(1) edge lookups regardless of density. */
fun buildAdjacencyMatrix(vertexCount: Int, edges: List<Pair<Int, Int>>): Array<BooleanArray> {
    val matrix = Array(vertexCount) { BooleanArray(vertexCount) }
    for ((u, v) in edges) {
        matrix[u][v] = true
        matrix[v][u] = true // omit this line for a directed graph
    }
    return matrix
}
        """.trim(),

        Lang.GO to """
// BuildAdjacencyList is the default for sparse graphs. O(V + E) space.
func BuildAdjacencyList(vertexCount int, edges [][2]int) [][]int {
	adj := make([][]int, vertexCount)
	for _, e := range edges {
		u, v := e[0], e[1]
		adj[u] = append(adj[u], v)
		adj[v] = append(adj[v], u) // omit this line for a directed graph
	}
	return adj
}

// BuildAdjacencyMatrix costs O(V^2) space but gives O(1) edge lookups
// regardless of density.
func BuildAdjacencyMatrix(vertexCount int, edges [][2]int) [][]bool {
	matrix := make([][]bool, vertexCount)
	for i := range matrix {
		matrix[i] = make([]bool, vertexCount)
	}
	for _, e := range edges {
		u, v := e[0], e[1]
		matrix[u][v] = true
		matrix[v][u] = true // omit this line for a directed graph
	}
	return matrix
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Adjacency list: the default for sparse graphs. O(V + E) space. */
function buildAdjacencyList(vertexCount, edges) {
  const adj = Array.from({ length: vertexCount }, () => []);
  for (const [u, v] of edges) {
    adj[u].push(v);
    adj[v].push(u); // omit this line for a directed graph
  }
  return adj;
}

/** Adjacency matrix: O(V^2) space, O(1) edge lookups regardless of density. */
function buildAdjacencyMatrix(vertexCount, edges) {
  const matrix = Array.from({ length: vertexCount }, () => new Array(vertexCount).fill(false));
  for (const [u, v] of edges) {
    matrix[u][v] = true;
    matrix[v][u] = true; // omit this line for a directed graph
  }
  return matrix;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 997,
            title = "Find the Town Judge",
            difficulty = Difficulty.EASY,
            idea = "No need to build a full graph structure — track in-degree and out-degree per person with two count arrays. The judge is the one person with in-degree n-1 and out-degree 0, found in one pass over the edges.",
            askedAt = "Amazon, Facebook",
        ),
        Question(
            id = 547,
            title = "Number of Provinces",
            difficulty = Difficulty.MEDIUM,
            idea = "The input is literally an adjacency matrix — a direct test of reading that representation. DFS or union-find over it counts connected components; the only real trap is treating the matrix as an edge list instead of indexing into it.",
            askedAt = "Amazon, Bloomberg",
        ),
        Question(
            id = 133,
            title = "Clone Graph",
            difficulty = Difficulty.MEDIUM,
            idea = "Building a new adjacency-list representation while traversing the old one — a hash map from original node to clone is what prevents infinite loops on cycles and ensures each node is cloned exactly once.",
            askedAt = "Amazon, Meta, Microsoft",
        ),
    ),

    related = listOf("bfs", "dfs", "dijkstra", "dags"),
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

    related = listOf("bfs", "stacks-queues", "coin-change", "dags"),
    references = Refs.basecs(),
)

val Dags = Topic(
    id = "dags",
    title = "Directed Acyclic Graphs",
    tagline = "No cycles means every dependency can be lined up in one valid order.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "A DAG is a directed graph with no cycles — exactly the condition that guarantees a valid dependency ordering exists.",
        "Topological sort produces that ordering: Kahn's algorithm repeatedly removes nodes with no remaining incoming edges, in O(V + E).",
        "If a topological sort can't place every node, the graph has a cycle — this is the standard way to detect one.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Plenty of real problems are really \"put these things in a valid order, respecting dependencies\": course prerequisites, build steps, spreadsheet formula evaluation, package installation. Model each dependency as a directed edge — \"A must happen before B\" becomes an edge from A to B — and the question \"is there a valid order at all?\" becomes a graph question: does this directed graph contain a cycle?",
        "If A depends on B and B depends on A, no order can satisfy both, and the graph has a cycle. A **directed acyclic graph (DAG)** is exactly a directed graph without that problem, and the reason DAGs matter so much is that acyclic is precisely the condition under which a valid dependency order — a **topological sort** — is guaranteed to exist at all.",
        "**Kahn's algorithm** builds that order directly from the dependency structure: track how many unresolved incoming edges (the in-degree) each node has, start with every node whose in-degree is already zero — nothing blocks them — and repeatedly remove one, appending it to the order and decrementing the in-degree of everything it pointed to. Whenever that decrement drops a neighbour's in-degree to zero, it becomes newly available and joins the queue. If the graph is acyclic, every node eventually gets placed.",
        "That \"if acyclic\" is also a free cycle detector: if the algorithm finishes and fewer than V nodes made it into the order, some group of nodes never reached in-degree zero, which can only happen if they depend on each other in a loop. This is precisely how build systems and compilers detect circular dependencies — running (or failing to complete) a topological sort *is* the cycle check, not a separate step.",
        "Multiple valid topological orders usually exist for the same DAG — anything that respects every edge's direction counts — which is why \"topological sort\" describes a family of valid answers, not a single canonical one, in contrast to something like sorting numbers where there's exactly one correct output.",
    ),

    origin = "**Topological sorting via in-degree tracking is known as Kahn's algorithm**, after **Arthur B. Kahn's 1962 paper 'Topological Sorting of Large Networks'** in Communications of the ACM, which addressed the practical problem of ordering large sets of interdependent tasks. The DFS-based alternative — post-order traversal, then reversed — is older in spirit, tracing to standard depth-first search techniques formalised around the same period.",

    keyPoints = listOf(
        "**A DAG is exactly the class of directed graphs where a valid dependency order exists** — cycles are the only thing that can make no valid order possible.",
        "**Kahn's algorithm**: track in-degree per node, start from every node already at in-degree zero, and repeatedly remove one, decrementing its neighbours' in-degrees — O(V + E).",
        "**A topological sort that places fewer than V nodes proves a cycle exists** — this doubles as the standard cycle-detection technique for directed graphs.",
        "**Multiple valid orderings usually exist.** Any order respecting every edge's direction is correct — there's no single canonical answer the way there is for sorting numbers.",
        "The DFS-based alternative (post-order, then reverse) works too, and is often simpler to reach for recursively — Kahn's is worth knowing by name because it's also the cycle-detection technique.",
    ),

    complexity = listOf(
        ComplexityRow("Topological sort (Kahn's)", "O(V + E)", "O(V)", "Every node and edge is processed exactly once; extra space is the in-degree array and queue."),
        ComplexityRow("Cycle detection", "O(V + E)", "O(V)", "A free byproduct of an incomplete topological sort — no separate pass needed."),
    ),

    pitfalls = listOf(
        "Forgetting that a topological order is not unique — comparing your output against a single 'expected' order rather than verifying it respects every edge is a common test-writing mistake.",
        "Assuming a topological sort always succeeds — on a graph with a cycle it cannot place every node, and that failure is itself the useful signal, not a bug to work around.",
        "Using DFS-based topological sort without handling disconnected components — every unvisited node needs its own DFS start, or part of the graph gets silently skipped.",
        "Confusing 'no valid order exists' (a cycle) with 'multiple valid orders exist' (completely normal, expected, and not an error).",
    ),

    steps = listOf(
        "Compute the in-degree of every node — how many edges point into it.",
        "Put every node with in-degree zero into a queue.",
        "Repeatedly remove a node from the queue, append it to the result, and decrement the in-degree of every node it points to.",
        "Whenever a neighbour's in-degree drops to zero, add it to the queue.",
        "If the result contains all V nodes, it's a valid topological order; if not, the graph has a cycle.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Kahn's algorithm. Returns null if the graph has a cycle. */
fun topologicalSort(vertexCount: Int, edges: List<Pair<Int, Int>>): List<Int>? {
    val adj = List(vertexCount) { mutableListOf<Int>() }
    val inDegree = IntArray(vertexCount)
    for ((from, to) in edges) {
        adj[from].add(to)
        inDegree[to]++
    }

    val queue = ArrayDeque((0 until vertexCount).filter { inDegree[it] == 0 })
    val order = mutableListOf<Int>()

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        order += node
        for (next in adj[node]) {
            inDegree[next]--
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }

    // Fewer nodes than V made it in: some group never reached in-degree
    // zero, which only happens if they depend on each other in a cycle.
    return order.takeIf { it.size == vertexCount }
}
        """.trim(),

        Lang.GO to """
// TopologicalSort runs Kahn's algorithm. Returns nil if the graph has a cycle.
func TopologicalSort(vertexCount int, edges [][2]int) []int {
	adj := make([][]int, vertexCount)
	inDegree := make([]int, vertexCount)
	for _, e := range edges {
		from, to := e[0], e[1]
		adj[from] = append(adj[from], to)
		inDegree[to]++
	}

	queue := []int{}
	for i := 0; i < vertexCount; i++ {
		if inDegree[i] == 0 {
			queue = append(queue, i)
		}
	}

	order := []int{}
	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]
		order = append(order, node)
		for _, next := range adj[node] {
			inDegree[next]--
			if inDegree[next] == 0 {
				queue = append(queue, next)
			}
		}
	}

	// Fewer nodes than vertexCount made it in: some group never reached
	// in-degree zero, which only happens if they depend on each other in
	// a cycle.
	if len(order) != vertexCount {
		return nil
	}
	return order
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Kahn's algorithm. Returns null if the graph has a cycle. */
function topologicalSort(vertexCount, edges) {
  const adj = Array.from({ length: vertexCount }, () => []);
  const inDegree = new Array(vertexCount).fill(0);
  for (const [from, to] of edges) {
    adj[from].push(to);
    inDegree[to]++;
  }

  const queue = [];
  for (let i = 0; i < vertexCount; i++) if (inDegree[i] === 0) queue.push(i);

  const order = [];
  while (queue.length > 0) {
    const node = queue.shift();
    order.push(node);
    for (const next of adj[node]) {
      inDegree[next]--;
      if (inDegree[next] === 0) queue.push(next);
    }
  }

  // Fewer nodes than vertexCount made it in: some group never reached
  // in-degree zero, which only happens if they depend on each other in
  // a cycle.
  return order.length === vertexCount ? order : null;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 207,
            title = "Course Schedule",
            difficulty = Difficulty.MEDIUM,
            idea = "Direct cycle detection: model prerequisites as directed edges and run Kahn's algorithm. If it can't place every course, a prerequisite cycle exists and no valid schedule is possible.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
        Question(
            id = 210,
            title = "Course Schedule II",
            difficulty = Difficulty.MEDIUM,
            idea = "Same graph, but return the actual order rather than just yes/no — Kahn's algorithm already produces it as a side effect of the cycle check, no extra work needed.",
            askedAt = "Amazon, Meta",
        ),
        Question(
            id = 269,
            title = "Alien Dictionary",
            difficulty = Difficulty.HARD,
            idea = "The graph isn't given — it has to be inferred: compare adjacent words to extract letter-ordering constraints, build edges between letters, then topologically sort the alphabet itself. The real difficulty is the graph construction, not the sort.",
            askedAt = "Meta, Airbnb, Google — a classic (if often locked) topological-sort question",
        ),
    ),

    related = listOf("bfs", "dfs", "graph-representation"),
    references = Refs.basecs(),
)

val Dijkstra = Topic(
    id = "dijkstra",
    title = "Dijkstra's Algorithm",
    tagline = "Always expand the closest unfinished node next, and shortest paths fall out in order.",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "BFS finds shortest paths by hop count; Dijkstra finds shortest paths by total weight, always expanding whichever unfinished node is currently closest.",
        "A min-heap of (distance, node) pairs makes 'always expand the closest' cheap — O((V + E) log V) instead of the naive O(V²).",
        "Only correct with non-negative weights — a negative edge can make a 'finished' node's distance wrong after the fact, which is exactly what Bellman-Ford exists to handle instead.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "BFS finds the shortest path in an unweighted graph by expanding outward in rings, one hop at a time — the first time you reach a node is guaranteed to be via the fewest hops. Dijkstra's algorithm generalises that idea to weighted graphs, where \"fewest hops\" isn't the same as \"least total distance.\" It keeps the same core idea — always finish with the node currently believed closest before moving on — but replaces \"closest by hop count\" with \"closest by total edge weight so far.\"",
        "Concretely: track a running best-known distance to every node (infinity until discovered otherwise), and repeatedly pick the unfinished node with the smallest known distance, mark it finished, and use its edges to try to improve its neighbours' distances — a step called **relaxation**. Because the algorithm always finishes the closest remaining node next, once a node is finished its distance can never be improved again: anything that could beat it would have to arrive via a node that isn't finished yet, which by definition is not closer.",
        "That correctness argument is also exactly where the requirement for **non-negative weights** comes from. It depends on \"nothing unfinished can be closer than what we just finished\" — and a negative edge can violate that outright, letting a path through an unfinished, seemingly-farther node beat a path through one already marked finished and closed off. **Bellman-Ford** exists specifically to handle graphs where that assumption doesn't hold, at the cost of being slower.",
        "Doing \"pick the smallest known distance\" efficiently is precisely a job for a **min-heap**: push every relaxation as a candidate `(distance, node)` pair, and always pop the smallest. Because a node can be pushed multiple times before it's finished (each relaxation is a fresh candidate), the standard implementation just skips a popped entry if that node has already been finished with a smaller distance — cheaper than removing stale entries from the heap outright. That heap-backed version runs in O((V + E) log V), a direct improvement over the naive O(V²) approach of scanning every unfinished node each round.",
        "Dijkstra's algorithm and BFS being close relatives is not a coincidence worth glossing over: run Dijkstra on a graph where every edge weight is 1, and it behaves identically to BFS, because \"closest by weight\" and \"closest by hop count\" become the same statement. That's a genuinely useful way to remember why the two exist and how they relate, rather than as two unrelated algorithms to memorise.",
    ),

    origin = "**Dijkstra's algorithm was conceived by Edsger W. Dijkstra in 1956**, reportedly in about twenty minutes while he was having coffee with his fiancée in Amsterdam, thinking about the shortest route between two Dutch cities as a demonstration for a new computer. He published it in **1959 as 'A note on two problems in connexion with graphs'** — a two-page paper that also introduced an algorithm for minimum spanning trees. Dijkstra later won the Turing Award in 1972 for foundational contributions to programming as a discipline.",

    keyPoints = listOf(
        "**Always finish the closest unfinished node next** — the same core idea as BFS, generalised from hop count to total edge weight.",
        "**Relaxation**: when finishing a node, check whether reaching each neighbour through it beats that neighbour's current best-known distance, and update it if so.",
        "**Once a node is finished, its distance is final** — the correctness argument, and it depends entirely on non-negative weights.",
        "**Requires non-negative edge weights.** A negative edge can let an unfinished path beat an already-finished one, breaking the whole argument — use **Bellman-Ford** instead when negative weights are possible.",
        "**A min-heap of (distance, node) pairs** makes 'pick the smallest known distance' efficient: O((V + E) log V) instead of an O(V²) linear scan every round.",
        "**Dijkstra on unit-weight edges behaves identically to BFS** — 'closest by weight' and 'closest by hop count' collapse into the same statement.",
    ),

    complexity = listOf(
        ComplexityRow("Heap-backed", "O((V + E) log V)", "O(V)", "Standard implementation: a min-heap of (distance, node) candidates, lazily skipping stale entries."),
        ComplexityRow("Naive (no heap)", "O(V²)", "O(V)", "Scans every unfinished node each round to find the minimum — fine for dense graphs, wasteful for sparse ones."),
    ),

    pitfalls = listOf(
        "Running it on a graph with negative edge weights — the correctness argument breaks silently, producing a wrong answer with no error raised. Use Bellman-Ford instead.",
        "Removing stale entries from the heap instead of just checking-and-skipping on pop — it's simpler and no more expensive to let stale entries sit in the heap and discard them lazily.",
        "Re-relaxing a node's neighbours after it's already finished — once finished, a node's distance is final, and revisiting it wastes work without changing correctness.",
        "Reaching for Dijkstra when the graph is unweighted — plain BFS is simpler and does the identical job for that special case.",
        "Forgetting to track the actual path, not just the distance, when the problem asks for the path itself — that needs a parent pointer updated alongside every relaxation.",
    ),

    steps = listOf(
        "Set every node's distance to infinity except the source, which is zero.",
        "Push the source onto a min-heap as (0, source).",
        "Repeatedly pop the smallest (distance, node) pair. If that node is already finished with a smaller distance, skip it.",
        "Otherwise, mark it finished, and relax every edge out of it: if going through this node beats a neighbour's current best distance, update it and push the improved (distance, neighbour) pair.",
        "Stop when the heap is empty, or as soon as the target node is popped, if only one destination matters.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
data class Edge(val to: Int, val weight: Int)

/** Returns the shortest distance from source to every node, or Int.MAX_VALUE if unreachable. */
fun dijkstra(vertexCount: Int, adj: List<List<Edge>>, source: Int): IntArray {
    val distances = IntArray(vertexCount) { Int.MAX_VALUE }
    distances[source] = 0

    val heap = java.util.PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
    heap.add(0 to source) // (distance, node)
    val finished = BooleanArray(vertexCount)

    while (heap.isNotEmpty()) {
        val (dist, node) = heap.poll()
        if (finished[node]) continue // a stale, already-beaten entry
        finished[node] = true

        for (edge in adj[node]) {
            val candidate = dist + edge.weight
            if (candidate < distances[edge.to]) {
                distances[edge.to] = candidate
                heap.add(candidate to edge.to)
            }
        }
    }
    return distances
}
        """.trim(),

        Lang.GO to """
type Edge struct {
	To     int
	Weight int
}

type heapItem struct{ dist, node int }
type minHeap []heapItem

func (h minHeap) Len() int           { return len(h) }
func (h minHeap) Less(i, j int) bool { return h[i].dist < h[j].dist }
func (h minHeap) Swap(i, j int)      { h[i], h[j] = h[j], h[i] }
func (h *minHeap) Push(x any)        { *h = append(*h, x.(heapItem)) }
func (h *minHeap) Pop() any {
	old := *h
	item := old[len(old)-1]
	*h = old[:len(old)-1]
	return item
}

// Dijkstra returns the shortest distance from source to every node, using
// math.MaxInt to mean "unreachable".
func Dijkstra(vertexCount int, adj [][]Edge, source int) []int {
	distances := make([]int, vertexCount)
	for i := range distances {
		distances[i] = math.MaxInt
	}
	distances[source] = 0

	h := &minHeap{{0, source}}
	finished := make([]bool, vertexCount)

	for h.Len() > 0 {
		item := heap.Pop(h).(heapItem)
		if finished[item.node] {
			continue // a stale, already-beaten entry
		}
		finished[item.node] = true

		for _, edge := range adj[item.node] {
			candidate := item.dist + edge.Weight
			if candidate < distances[edge.To] {
				distances[edge.To] = candidate
				heap.Push(h, heapItem{candidate, edge.To})
			}
		}
	}
	return distances
}
        """.trim(),

        Lang.JAVASCRIPT to """
class MinHeap {
  #items = [];
  get size() { return this.#items.length; }

  push(item) {
    this.#items.push(item);
    this.#bubbleUp(this.#items.length - 1);
  }

  pop() {
    const min = this.#items[0];
    const last = this.#items.pop();
    if (this.#items.length > 0) {
      this.#items[0] = last;
      this.#sinkDown(0);
    }
    return min;
  }

  #bubbleUp(i) {
    while (i > 0) {
      const parent = Math.floor((i - 1) / 2);
      if (this.#items[i][0] >= this.#items[parent][0]) break;
      [this.#items[i], this.#items[parent]] = [this.#items[parent], this.#items[i]];
      i = parent;
    }
  }

  #sinkDown(i) {
    while (true) {
      const left = 2 * i + 1;
      const right = 2 * i + 2;
      let smallest = i;
      if (left < this.#items.length && this.#items[left][0] < this.#items[smallest][0]) smallest = left;
      if (right < this.#items.length && this.#items[right][0] < this.#items[smallest][0]) smallest = right;
      if (smallest === i) break;
      [this.#items[i], this.#items[smallest]] = [this.#items[smallest], this.#items[i]];
      i = smallest;
    }
  }
}

/** Returns the shortest distance from source to every node, or Infinity if unreachable. */
function dijkstra(vertexCount, adj, source) {
  const distances = new Array(vertexCount).fill(Infinity);
  distances[source] = 0;

  const heap = new MinHeap();
  heap.push([0, source]); // [distance, node]
  const finished = new Array(vertexCount).fill(false);

  while (heap.size > 0) {
    const [dist, node] = heap.pop();
    if (finished[node]) continue; // a stale, already-beaten entry
    finished[node] = true;

    for (const { to, weight } of adj[node]) {
      const candidate = dist + weight;
      if (candidate < distances[to]) {
        distances[to] = candidate;
        heap.push([candidate, to]);
      }
    }
  }
  return distances;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 743,
            title = "Network Delay Time",
            difficulty = Difficulty.MEDIUM,
            idea = "The direct application: shortest weighted path from one source to every node, then the answer is the maximum of those distances — the time for the signal to reach everyone.",
            askedAt = "Google, Amazon",
        ),
        Question(
            id = 787,
            title = "Cheapest Flights Within K Stops",
            difficulty = Difficulty.MEDIUM,
            idea = "Plain Dijkstra's 'once finished, never revisit' breaks once a hop limit is added, because a longer-but-fewer-hops path can be the only valid one. Bellman-Ford-style relaxation, bounded to k+1 rounds, handles the constraint more naturally than patching Dijkstra.",
            askedAt = "Amazon, Meta — a favourite for testing the limits of Dijkstra",
        ),
        Question(
            id = 1514,
            title = "Path with Maximum Probability",
            difficulty = Difficulty.MEDIUM,
            idea = "The same algorithm with the comparison flipped: a max-heap instead of a min-heap, and multiplying edge probabilities instead of summing weights. Shows Dijkstra generalises to any relaxation rule that only ever improves monotonically, not just addition.",
            askedAt = "Google",
        ),
    ),

    related = listOf("bfs", "dags", "graph-representation", "heaps", "kruskal"),
    references = Refs.basecs(),
)

val UnionFindTopic = Topic(
    id = "union-find",
    title = "Union-Find",
    tagline = "Track which things are connected, without ever walking the whole group to check.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Answers 'are these two things in the same group?' and 'merge these two groups' in close to O(1) each, without ever traversing a group to check its members.",
        "Two optimisations — union by rank/size and path compression — turn a naive O(n) find into amortised O(α(n)), effectively constant for any input size that could exist.",
        "The standard tool for Kruskal's MST, detecting cycles in an undirected graph, and 'how many connected components' problems — anywhere the question is about grouping, not paths.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Some problems are purely about grouping: are these two nodes in the same connected component? If I merge these two groups, what does the result look like? A graph traversal (BFS/DFS) can answer \"are A and B connected\" by searching, but that's O(V + E) per query — wasteful if you're going to ask the question many times as the graph is built up incrementally. **Union-Find** (or **disjoint-set union**) is a structure purpose-built for exactly that repeated-query shape: track group membership directly, and answer both \"which group is X in?\" and \"merge X's group with Y's group\" in close to O(1) each.",
        "The underlying idea is a forest, but a strange one: each element points to a parent, and following those parent pointers to the top gives a group's representative — the root of that particular tree. Two elements are in the same group exactly when following their parent chains lands on the same root. **Union** just makes one root point at the other, merging the two trees — and therefore the two groups — with a single pointer change.",
        "Done naively, this degrades exactly like an unbalanced binary search tree: repeatedly unioning in an unlucky order can build one long chain, making `find` an O(n) walk instead of a fast one. Two independent fixes solve this. **Union by rank (or size)** always attaches the smaller tree under the bigger one's root during a union, keeping trees shallow instead of letting them grow into chains. **Path compression** goes further: every time `find` walks a chain to the root, it rewrites every node it passed through to point directly at that root, flattening future lookups for free as a side effect of doing the current one.",
        "Combined, those two optimisations give an amortised time per operation of O(α(n)) — the inverse Ackermann function, which grows so slowly that it's smaller than 5 for any n you could ever actually construct in a real computer. That is, for every practical purpose, O(1) — a structure that started out looking like it might need O(log n) or worse ends up being essentially free per operation once both fixes are in place.",
        "The classic application is **Kruskal's algorithm** for a minimum spanning tree: sort edges by weight, and greedily add each one unless its two endpoints are already in the same union-find group — which would mean adding it creates a cycle instead of connecting something new. Union-find turns \"does this edge create a cycle\" from an O(V + E) traversal question into a near-O(1) lookup, which is exactly why Kruskal's algorithm can afford to consider every edge individually.",
    ),

    origin = "The tree-based union-find structure, with union by rank, is credited to **Bernard A. Galler and Michael J. Fischer's 1964 paper 'An Improved Equivalence Algorithm.'** **Path compression's amortised analysis — proving the combined O(α(n)) bound — is due to Robert Tarjan's 1975 paper 'Efficiency of a Good But Not Linear Set Union Algorithm,'** which is also where the connection to the inverse Ackermann function was formally established. Tarjan later won the Turing Award in 1986, in part for this and related work on data structure efficiency.",

    keyPoints = listOf(
        "**Find**: follow parent pointers to a group's root — its representative. Two elements are in the same group exactly when their roots match.",
        "**Union**: attach one root under the other, merging two groups with a single pointer change.",
        "**Union by rank/size** always attaches the smaller tree under the bigger one's root, preventing the long-chain degradation an unlucky union order would otherwise cause.",
        "**Path compression** rewrites every node on a `find` path to point directly at the root, flattening the structure as a free side effect of each lookup.",
        "**Together, both give amortised O(α(n))** per operation — the inverse Ackermann function, effectively constant for any input size that could exist in practice.",
        "**The standard tool for 'does this edge create a cycle' and 'how many connected components'** — anywhere the question is about grouping rather than pathfinding.",
    ),

    complexity = listOf(
        ComplexityRow("Find / union, with both optimisations", "O(α(n)) amortised", "O(n)", "α is the inverse Ackermann function — effectively constant for any n that can exist."),
        ComplexityRow("Find / union, naive (no optimisation)", "O(n) worst case", "O(n)", "An unlucky union order builds a long chain, degrading find to a full walk."),
    ),

    pitfalls = listOf(
        "Implementing `find` without path compression and `union` without union by rank/size — either optimisation alone helps, but skipping both lets an unlucky sequence of unions degrade to O(n) per operation.",
        "Forgetting that union-find only answers connectivity questions, not path or distance questions — it can tell you two nodes are connected, never how, or how far apart.",
        "Comparing two elements directly instead of comparing their `find` results — the whole point of the structure is that group membership is checked via the root, not by any other property.",
        "Re-initialising or rebuilding the structure per query instead of maintaining it incrementally as unions happen — that throws away exactly the amortised benefit the structure exists to provide.",
    ),

    steps = listOf(
        "Initialise every element as its own group, with itself as its own parent (and rank/size 1).",
        "To find an element's group, follow parent pointers to the root, compressing the path by repointing visited nodes directly at that root.",
        "To union two elements, find both roots; if they differ, attach the smaller-ranked (or smaller-sized) root under the other.",
        "Two elements are connected exactly when their `find` results match.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
class UnionFind(size: Int) {
    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)

    /** Path compression: repoint every visited node directly at the root. */
    fun find(x: Int): Int {
        if (parent[x] != x) parent[x] = find(parent[x])
        return parent[x]
    }

    /** Union by rank: attach the shorter tree under the taller one's root. */
    fun union(a: Int, b: Int): Boolean {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA == rootB) return false // already in the same group

        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA
            else -> { parent[rootB] = rootA; rank[rootA]++ }
        }
        return true
    }

    fun connected(a: Int, b: Int): Boolean = find(a) == find(b)
}
        """.trim(),

        Lang.GO to """
type UnionFind struct {
	parent []int
	rank   []int
}

func NewUnionFind(size int) *UnionFind {
	parent := make([]int, size)
	for i := range parent {
		parent[i] = i
	}
	return &UnionFind{parent: parent, rank: make([]int, size)}
}

// Find applies path compression: every visited node is repointed directly
// at the root.
func (u *UnionFind) Find(x int) int {
	if u.parent[x] != x {
		u.parent[x] = u.Find(u.parent[x])
	}
	return u.parent[x]
}

// Union attaches the shorter tree under the taller one's root.
func (u *UnionFind) Union(a, b int) bool {
	rootA, rootB := u.Find(a), u.Find(b)
	if rootA == rootB {
		return false // already in the same group
	}

	switch {
	case u.rank[rootA] < u.rank[rootB]:
		u.parent[rootA] = rootB
	case u.rank[rootA] > u.rank[rootB]:
		u.parent[rootB] = rootA
	default:
		u.parent[rootB] = rootA
		u.rank[rootA]++
	}
	return true
}

func (u *UnionFind) Connected(a, b int) bool {
	return u.Find(a) == u.Find(b)
}
        """.trim(),

        Lang.JAVASCRIPT to """
class UnionFind {
  #parent;
  #rank;

  constructor(size) {
    this.#parent = Array.from({ length: size }, (_, i) => i);
    this.#rank = new Array(size).fill(0);
  }

  /** Path compression: repoint every visited node directly at the root. */
  find(x) {
    if (this.#parent[x] !== x) this.#parent[x] = this.find(this.#parent[x]);
    return this.#parent[x];
  }

  /** Union by rank: attach the shorter tree under the taller one's root. */
  union(a, b) {
    const rootA = this.find(a);
    const rootB = this.find(b);
    if (rootA === rootB) return false; // already in the same group

    if (this.#rank[rootA] < this.#rank[rootB]) this.#parent[rootA] = rootB;
    else if (this.#rank[rootA] > this.#rank[rootB]) this.#parent[rootB] = rootA;
    else { this.#parent[rootB] = rootA; this.#rank[rootA]++; }
    return true;
  }

  connected(a, b) { return this.find(a) === this.find(b); }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 547,
            title = "Number of Provinces",
            difficulty = Difficulty.MEDIUM,
            idea = "Union every pair of directly-connected cities, then count distinct roots — the number of provinces is the number of distinct groups union-find ends up with, no separate traversal needed.",
            askedAt = "Amazon, Bloomberg",
        ),
        Question(
            id = 684,
            title = "Redundant Connection",
            difficulty = Difficulty.MEDIUM,
            idea = "Process edges in order and union each one — the first edge where union() reports the two endpoints already connected is exactly the redundant one, found in a single pass with no separate cycle-detection logic.",
            askedAt = "Amazon, Google",
        ),
        Question(
            id = 1319,
            title = "Number of Operations to Make Network Connected",
            difficulty = Difficulty.MEDIUM,
            idea = "Union every existing cable, then the answer is (number of connected components − 1) — each extra cable beyond what's needed to connect everything is exactly one redundant edge that can be repurposed.",
            askedAt = "Google, Meta",
        ),
    ),

    related = listOf("graph-representation", "kruskal", "dags"),
    references = Refs.basecs(),
)

val Kruskal = Topic(
    id = "kruskal",
    title = "Kruskal's Algorithm",
    tagline = "Sort every edge by weight, and greedily add whichever doesn't create a cycle.",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "Builds a minimum spanning tree by sorting all edges by weight and greedily adding each one, skipping any that would create a cycle — union-find makes the cycle check nearly O(1).",
        "O(E log E), dominated by the sort — the greedy add-if-no-cycle step itself is nearly linear thanks to union-find.",
        "A spanning tree connects every node with the fewest possible edges (V − 1); minimum means the total edge weight is as small as possible among all such trees.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A spanning tree of a connected graph is a subset of its edges that connects every node using the fewest possible edges — exactly V − 1 of them, with no cycles. Many different spanning trees usually exist for the same graph; a **minimum spanning tree (MST)** is one where the total weight of the chosen edges is as small as possible among all of them. Kruskal's algorithm builds one with a strikingly simple greedy rule: sort every edge by weight, then walk the sorted list adding each edge unless it would create a cycle.",
        "That greedy rule is not obviously correct, and the reason it works is worth stating precisely: at every point, the cheapest edge that doesn't create a cycle is guaranteed to belong to *some* minimum spanning tree. Adding it can never be a mistake, because if it weren't in the MST, swapping it in for some more expensive edge on the cycle it would otherwise create can only lower the total weight, never raise it. That argument — usually called the \"cycle property\" — is what justifies never reconsidering an edge once it's been skipped or added.",
        "\"Would this edge create a cycle?\" is exactly the question union-find answers efficiently: two endpoints already in the same union-find group means a path between them already exists in the edges chosen so far, so connecting them again would close a loop. Skip that edge. Otherwise, take it, and union the two groups. That's the entire algorithm — sort, then one pass with a union-find check per edge — and it's why Kruskal's algorithm is usually taught immediately after union-find rather than as an independent topic.",
        "The complexity is dominated entirely by the sort: O(E log E) for sorting the edges, with the union-find pass itself running in close to O(E) thanks to path compression and union by rank. That makes Kruskal's a good fit for **sparse** graphs specifically — **Prim's algorithm**, which grows a single tree outward from one node using a priority queue, tends to win on dense graphs instead, the same sparse-versus-dense trade-off that shows up between adjacency lists and matrices.",
    ),

    origin = "**Kruskal's algorithm was published by Joseph Kruskal in 1956**, in a paper titled 'On the Shortest Spanning Subtree of a Graph and the Traveling Salesman Problem' — notably, in the same paper that also discussed the traveling salesman problem. Kruskal developed it independently after learning that a colleague had proved, but not published, the same greedy cycle-avoidance idea; it appeared in the same journal issue as Prim's algorithm, which solves the identical problem via a different, tree-growing approach.",

    keyPoints = listOf(
        "**Sort every edge by weight, then greedily add each one unless it creates a cycle.** That's the entire algorithm.",
        "**Union-find answers 'would this create a cycle' in near-O(1)**: if the edge's two endpoints are already in the same group, a path between them already exists, and adding the edge would close a loop.",
        "**The greedy choice is provably safe** — the cheapest non-cycle-creating edge always belongs to some MST, which is why the algorithm never needs to reconsider a decision once made.",
        "**O(E log E)**, dominated by sorting the edges — the union-find pass itself is close to linear.",
        "**Kruskal's wins on sparse graphs; Prim's wins on dense ones** — the same trade-off that separates adjacency lists from adjacency matrices.",
        "A spanning tree always has exactly **V − 1 edges** for a connected graph with V nodes — more means a cycle exists, fewer means the graph isn't fully connected yet.",
    ),

    complexity = listOf(
        ComplexityRow("Sort edges", "O(E log E)", "O(E)", "Dominates the total runtime."),
        ComplexityRow("Union-find pass", "O(E α(V))", "O(V)", "Effectively O(E) in practice — α(V) is the near-constant inverse Ackermann function."),
    ),

    pitfalls = listOf(
        "Forgetting the cycle check entirely and just adding edges in sorted order — without union-find (or an equivalent check), the result isn't a tree at all, just the E cheapest edges.",
        "Assuming Kruskal's always beats Prim's — the sort-dominated O(E log E) loses to Prim's O(E log V) with a heap on dense graphs, where E approaches V².",
        "Applying it to a disconnected graph and expecting a single spanning tree — the algorithm correctly produces a minimum spanning *forest* instead, one tree per connected component.",
        "Reusing a union-find structure across multiple separate MST computations without resetting it — stale group memberships from a previous run silently corrupt the cycle check.",
    ),

    steps = listOf(
        "Sort every edge in the graph by weight, ascending.",
        "Initialise a union-find structure with every node in its own group.",
        "For each edge in sorted order: if its two endpoints are already in the same group, skip it — it would create a cycle.",
        "Otherwise, add the edge to the MST and union the two endpoints' groups.",
        "Stop once V − 1 edges have been added, or the sorted list is exhausted.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
data class WeightedEdge(val u: Int, val v: Int, val weight: Int)

/** Returns the MST's edges. Assumes the graph is connected. */
fun kruskal(vertexCount: Int, edges: List<WeightedEdge>): List<WeightedEdge> {
    val sorted = edges.sortedBy { it.weight }
    val uf = UnionFind(vertexCount)
    val mst = mutableListOf<WeightedEdge>()

    for (edge in sorted) {
        // union() returns false if u and v were already connected —
        // adding this edge would close a cycle, so skip it.
        if (uf.union(edge.u, edge.v)) {
            mst += edge
            if (mst.size == vertexCount - 1) break
        }
    }
    return mst
}
        """.trim(),

        Lang.GO to """
type WeightedEdge struct {
	U, V, Weight int
}

// Kruskal returns the MST's edges. Assumes the graph is connected.
func Kruskal(vertexCount int, edges []WeightedEdge) []WeightedEdge {
	sorted := append([]WeightedEdge(nil), edges...)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i].Weight < sorted[j].Weight })

	uf := NewUnionFind(vertexCount)
	var mst []WeightedEdge

	for _, edge := range sorted {
		// Union returns false if U and V were already connected — adding
		// this edge would close a cycle, so skip it.
		if uf.Union(edge.U, edge.V) {
			mst = append(mst, edge)
			if len(mst) == vertexCount-1 {
				break
			}
		}
	}
	return mst
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Returns the MST's edges as [u, v, weight] triples. Assumes a connected graph. */
function kruskal(vertexCount, edges) {
  const sorted = [...edges].sort((a, b) => a[2] - b[2]);
  const uf = new UnionFind(vertexCount);
  const mst = [];

  for (const edge of sorted) {
    const [u, v] = edge;
    // union() returns false if u and v were already connected — adding
    // this edge would close a cycle, so skip it.
    if (uf.union(u, v)) {
      mst.push(edge);
      if (mst.length === vertexCount - 1) break;
    }
  }
  return mst;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 1584,
            title = "Min Cost to Connect All Points",
            difficulty = Difficulty.MEDIUM,
            idea = "Build a complete graph of Manhattan distances between every pair of points, then run Kruskal's (or Prim's) directly. With n points there are O(n²) edges, which is exactly why Prim's O(E log V) sometimes edges out Kruskal's here on larger inputs.",
            askedAt = "Google, Amazon",
        ),
        Question(
            id = 684,
            title = "Redundant Connection",
            difficulty = Difficulty.MEDIUM,
            idea = "Viewed through the MST lens: the redundant edge is exactly the one Kruskal's algorithm would skip — the first edge, in input order, whose two endpoints are already connected by edges processed so far.",
            askedAt = "Amazon, Google",
        ),
    ),

    related = listOf("union-find", "graph-representation", "dijkstra"),
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

val BinaryTrees = Topic(
    id = "binary-trees",
    title = "Binary Trees",
    tagline = "Give a node two children instead of one, and a line becomes a hierarchy.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Add one invariant — left is smaller, right is bigger — and a binary tree becomes a binary **search** tree, halving the search space per comparison.",
        "That O(log n) bound only holds when the tree is balanced. Insert sorted data with no rebalancing and it degenerates into a straight line, O(n).",
        "In-order traversal of a BST visits keys in sorted order — the one traversal fact worth memorising cold.",
        "Deleting a node with two children needs its in-order successor promoted into its place, not a plain splice.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A linked list gave up contiguity for cheap insertion, but it is still a straight line — every node points to exactly one next. The next obvious question is what happens if a node can point to two: not a sequence any more, but a hierarchy. That is a tree, and it can represent shapes a line fundamentally cannot — file systems, decision paths, anything with a branching \"and/or\" structure.",
        "A binary tree just caps that branching at two children, conventionally called left and right. On its own that buys nothing but shape. The useful version adds one invariant: everything in a node's left subtree is smaller than it, everything in its right subtree is bigger. That is a **binary search tree**, and the invariant is what turns the shape into a search structure — comparing against the root eliminates one entire subtree, the same halving idea as binary search, except the sorted order is built into the shape instead of into contiguous memory.",
        "That halving is also where the O(log n) claim earns its asterisk. It only holds if the tree stays roughly balanced. Insert 1, 2, 3, 4, 5 in that order with no rebalancing and every node only ever gets a right child — the tree is a straight line, and every operation degrades to O(n), identical to a linked list. Self-balancing variants exist purely to prevent this by doing a little extra rotation work on every insert to keep the height near log n.",
        "Traversal order is the other thing worth being precise about, because the three common orders answer different questions rather than being interchangeable trivia. **In-order** (left, node, right) visits a BST's keys in sorted order — essentially the only reason to memorise it. **Pre-order** (node, left, right) visits a node before its children, which is how you would serialise a tree so the root is always available first when rebuilding it. **Post-order** (left, right, node) visits children before their parent, which is how you would safely delete or free a tree, since nothing is removed before what depends on it.",
        "Binary trees are also the honest justification for recursion in most curricula, because the structure's own definition is recursive: a binary tree is either empty, or a node with a left subtree and a right subtree that are themselves binary trees. Almost every operation follows that shape directly — handle the empty case, recurse left, recurse right — which is why tree code reads shorter than it has any right to.",
    ),

    origin = "**Binary search trees were described independently by several researchers around 1960** — among them P.F. Windley, Andrew Colin, and Thomas Hibbard, whose 1962 paper on the deletion algorithm is still the one most textbooks cite for the two-children removal trick. Donald Knuth's *The Art of Computer Programming* documents this multiple, near-simultaneous discovery and is where the systematic balanced-vs-unbalanced height analysis first appeared. The **self-balancing** answer followed almost immediately: Georgy Adelson-Velsky and Evgenii Landis published the AVL tree in 1962, the first structure to guarantee O(log n) height regardless of insertion order.",

    keyPoints = listOf(
        "A **binary search tree (BST)** adds one invariant to a bare binary tree: everything left of a node is smaller, everything right is bigger. That invariant is the whole reason search is possible at all.",
        "Search, insert and delete are all **O(height)**, and height is O(log n) only when the tree is balanced — a degenerate, line-shaped BST makes every operation O(n).",
        "**In-order traversal of a BST visits keys in sorted order.** Pre-order exists to serialise/rebuild; post-order exists to delete safely, children before parent.",
        "**Deleting a node with two children** needs its in-order successor — the smallest node in its right subtree — promoted into its place, then removed from where it used to sit.",
        "**Self-balancing trees** (AVL, red-black) exist purely to bound height at O(log n) by doing extra rotation work on every insert/delete. Plain BSTs make no such promise.",
        "A **complete binary tree** — every level full except possibly the last, filled left to right — is dense enough to store implicitly in a plain array with no pointers, which is exactly the shape a heap relies on.",
    ),

    complexity = listOf(
        ComplexityRow("Search (balanced BST)", "O(log n)", "O(1)", "Height-bounded; each comparison discards one whole subtree."),
        ComplexityRow("Search (unbalanced BST)", "O(n)", "O(1)", "Degenerates to a linked list when insertions arrive already sorted."),
        ComplexityRow("Insert / delete (balanced)", "O(log n)", "O(1)", "Same height bound as search."),
        ComplexityRow("Traversal (any order)", "O(n)", "O(h)", "Visits every node once; extra space is the recursion stack, h = height."),
        ComplexityRow("Storage", "—", "O(n)", "Two child pointers per node, plus the value."),
    ),

    pitfalls = listOf(
        "Deleting a two-child node by splicing it out directly — the invariant breaks unless the in-order successor (or predecessor) is promoted into its place first.",
        "Assuming O(log n) on data that arrives sorted or near-sorted — a plain BST builds a straight line under that input. Use a self-balancing tree when insertion order isn't random.",
        "Treating in-order traversal as proof of a valid BST — it just visits nodes in that pattern regardless of whether the invariant actually holds.",
        "Recursing on a subtree without a null/empty base case — the single most common binary-tree bug, since every child is either a subtree or absent.",
        "Comparing nodes by reference instead of by value after a rotation — an easy typo that silently breaks the ordering property without an obvious symptom.",
    ),

    steps = listOf(
        "To insert value v into a BST: start at the root.",
        "If the current spot is empty, v becomes that node.",
        "If v is smaller than the current node, recurse left; if larger, recurse right.",
        "Repeat until an empty spot is found — that is where the new node attaches.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
class TreeNode(val value: Int, var left: TreeNode? = null, var right: TreeNode? = null)

/** BST insert — recurse toward the empty slot the value belongs at. */
fun insert(root: TreeNode?, value: Int): TreeNode {
    if (root == null) return TreeNode(value)
    if (value < root.value) root.left = insert(root.left, value)
    else if (value > root.value) root.right = insert(root.right, value)
    return root
}

/** O(height): one comparison per level, discarding the other subtree entirely. */
fun search(root: TreeNode?, target: Int): Boolean = when {
    root == null -> false
    target == root.value -> true
    target < root.value -> search(root.left, target)
    else -> search(root.right, target)
}

/** In-order traversal of a BST visits every key in sorted order. */
fun inorder(root: TreeNode?, out: MutableList<Int> = mutableListOf()): List<Int> {
    if (root == null) return out
    inorder(root.left, out)
    out += root.value
    inorder(root.right, out)
    return out
}
        """.trim(),

        Lang.GO to """
type TreeNode struct {
	Value       int
	Left, Right *TreeNode
}

// Insert recurses toward the empty slot the value belongs at.
func Insert(root *TreeNode, value int) *TreeNode {
	if root == nil {
		return &TreeNode{Value: value}
	}
	if value < root.Value {
		root.Left = Insert(root.Left, value)
	} else if value > root.Value {
		root.Right = Insert(root.Right, value)
	}
	return root
}

// Search is O(height): one comparison per level, discarding the other subtree.
func Search(root *TreeNode, target int) bool {
	if root == nil {
		return false
	}
	if target == root.Value {
		return true
	}
	if target < root.Value {
		return Search(root.Left, target)
	}
	return Search(root.Right, target)
}

// Inorder visits every key of a BST in sorted order.
func Inorder(root *TreeNode, out *[]int) {
	if root == nil {
		return
	}
	Inorder(root.Left, out)
	*out = append(*out, root.Value)
	Inorder(root.Right, out)
}
        """.trim(),

        Lang.JAVASCRIPT to """
class TreeNode {
  constructor(value, left = null, right = null) {
    this.value = value;
    this.left = left;
    this.right = right;
  }
}

/** BST insert — recurse toward the empty slot the value belongs at. */
function insert(root, value) {
  if (root === null) return new TreeNode(value);
  if (value < root.value) root.left = insert(root.left, value);
  else if (value > root.value) root.right = insert(root.right, value);
  return root;
}

/** O(height): one comparison per level, discarding the other subtree entirely. */
function search(root, target) {
  if (root === null) return false;
  if (target === root.value) return true;
  return target < root.value ? search(root.left, target) : search(root.right, target);
}

/** In-order traversal of a BST visits every key in sorted order. */
function inorder(root, out = []) {
  if (root === null) return out;
  inorder(root.left, out);
  out.push(root.value);
  inorder(root.right, out);
  return out;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 98,
            title = "Validate Binary Search Tree",
            difficulty = Difficulty.MEDIUM,
            idea = "Comparing each node only to its immediate parent is the classic wrong answer — a node can be locally fine and still violate the invariant with a grandparent. Carry a valid (min, max) range down the recursion instead.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
        Question(
            id = 235,
            title = "Lowest Common Ancestor of a BST",
            difficulty = Difficulty.MEDIUM,
            idea = "Use the BST property instead of a generic tree LCA search: if both targets are smaller than the current node go left, if both are bigger go right, and the first node where they split is the answer — no full traversal needed.",
            askedAt = "Amazon, Facebook",
        ),
        Question(
            id = 230,
            title = "Kth Smallest Element in a BST",
            difficulty = Difficulty.MEDIUM,
            idea = "In-order traversal visits keys in sorted order, so the kth value visited is the answer — stop as soon as you reach it rather than collecting the whole traversal first.",
            askedAt = "Amazon, Google, Bloomberg",
        ),
    ),

    related = listOf("arrays", "linked-lists", "tries", "heaps"),
    references = Refs.basecs(),
)

val Tries = Topic(
    id = "tries",
    title = "Tries",
    tagline = "A tree shaped like the alphabet — every path from the root spells a prefix.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Hash tables answer 'is this key present?' in O(1) but can't answer 'what starts with this prefix?' without a full scan — a trie walks keys one character at a time so that question is free.",
        "O(k) lookup and insert, where k is the key's length — not the number of keys stored.",
        "Every node needs its own end-of-word flag; a stored path is not automatically a stored word.",
        "Long chains of single-child nodes waste space — a radix tree (Patricia trie) compresses them into one edge.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Hash tables give O(1) lookup for an exact key, but they can't answer \"which keys start with 'pre'?\" without scanning everything — hashing deliberately scrambles similar keys into unrelated buckets, so two keys sharing a prefix have nothing in common once hashed. A **trie** (from re*trie*val) inverts the approach entirely: instead of hashing the whole key at once, it walks the key one character at a time, and every step down the tree is shared by every other key with the same prefix.",
        "Concretely, each node represents one character position, and its children are keyed by whatever character can come next. Insert \"cat\" and \"car\" and they share the path for \"ca\", forking only at the third character. Because that sharing is structural, checking whether *any* stored word begins with a given prefix is just walking the prefix's path and confirming it exists — no scan of the stored words required.",
        "That is the entire value proposition: O(k) lookup and insert, where k is the length of the key, not the number of keys stored — plus prefix queries that a hash table cannot answer at all. The cost is space. A trie holding mostly-distinct strings can use more memory than the strings themselves, because every node carries an array or map of possible next characters, most of which go unused.",
        "Marking the end of a word matters and is a common source of bugs. Storing \"car\" does not make \"ca\" a word — it only makes \"ca\" a valid *path*. Each node therefore needs its own explicit end-of-word flag, separate from simply existing on a path, or a trie cannot distinguish \"this is a stored word\" from \"this is merely a prefix of one\".",
        "The naive trie wastes the most space on long chains of single-child nodes — storing \"hello\" alone allocates five nodes, each holding exactly one child. A **radix tree** (or Patricia trie) compresses those chains into a single edge labelled with the whole shared substring, trading a little insert complexity for a much smaller structure. That compression is the standard fix once memory, rather than lookup speed, becomes the bottleneck.",
    ),

    origin = "The trie was introduced by **René de la Briandais in 1959**, in a paper on fast file searching, though he did not give it a name. **Edward Fredkin coined the term 'trie' in 1960**, deriving it from re*trie*val — and, notoriously, insisted it still be pronounced \"tree\", a pronunciation that never really caught on since it is indistinguishable from the word for the broader structure a trie is built from. Most people now say \"try\" instead, if only to be understood.",

    keyPoints = listOf(
        "**O(k) lookup, insert and delete**, where k is the key's length — independent of how many other keys are stored, unlike a hash table's O(1) *average*, which still depends on load factor.",
        "**Shared prefixes are stored once.** That structural sharing is the entire mechanism behind autocomplete: walking a prefix's path and confirming it exists answers 'does anything start with this?'",
        "Each node needs an explicit **end-of-word marker**, distinct from simply being a valid path — otherwise a stored word is indistinguishable from a stored prefix of some other word.",
        "**Children storage is the memory/speed trade-off**: a fixed-size array (26 slots for lowercase English) is O(1) per step but wastes space on sparse or large alphabets; a hash map per node is denser but adds hashing overhead.",
        "A **radix tree / Patricia trie** compresses runs of single-child nodes into one edge — the standard fix once a plain trie's per-character node overhead starts to matter.",
        "Deletion needs care: clearing a node's end-of-word flag is only safe if that node is not also a prefix of another stored word, and nodes should only be pruned once they have no children left.",
    ),

    complexity = listOf(
        ComplexityRow("Insert", "O(k)", "O(k)", "k = key length; the worst case allocates one new node per character."),
        ComplexityRow("Search (exact)", "O(k)", "O(1)", "Walks the key's path — no dependency on how many keys are stored."),
        ComplexityRow("Prefix search", "O(p)", "O(1)", "p = prefix length, just confirming the path exists."),
        ComplexityRow("Storage", "—", "O(nodes × alphabet)", "Sparse alphabets waste space with array-backed children; compress with a radix tree or use hash-map children."),
    ),

    pitfalls = listOf(
        "Treating 'is a valid path' as 'is a stored word' — without an explicit end-of-word flag, a stored 'car' makes 'ca' look like a stored word too.",
        "Using a fixed-size children array sized for a small alphabet on Unicode input — 26 slots is fine for lowercase English, hopeless for arbitrary text; use a map instead.",
        "Deleting a node outright when removing a word, without first checking it isn't also a prefix of some other stored word.",
        "Reaching for a trie when prefix queries are never needed — a plain hash table is simpler and uses less memory for pure exact-match lookups.",
        "Assuming trie operations are O(1) the way a hash table's average case is — they are O(k), so a long key genuinely costs more than a short one.",
    ),

    steps = listOf(
        "To insert a word: start at the root.",
        "For each character, move to the existing child for that character, or create one if it doesn't exist yet.",
        "After the last character, mark the current node as end-of-word.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord = false
}

class Trie {
    private val root = TrieNode()

    /** O(k): one hop per character, creating nodes only where a path is new. */
    fun insert(word: String) {
        var node = root
        for (c in word) node = node.children.getOrPut(c) { TrieNode() }
        node.isWord = true
    }

    fun search(word: String): Boolean = nodeAt(word)?.isWord == true

    /** Existence of the path is enough — no scan of stored words required. */
    fun startsWith(prefix: String): Boolean = nodeAt(prefix) != null

    private fun nodeAt(key: String): TrieNode? {
        var node = root
        for (c in key) node = node.children[c] ?: return null
        return node
    }
}
        """.trim(),

        Lang.GO to """
type TrieNode struct {
	children map[byte]*TrieNode
	isWord   bool
}

func newTrieNode() *TrieNode {
	return &TrieNode{children: make(map[byte]*TrieNode)}
}

type Trie struct {
	root *TrieNode
}

func NewTrie() *Trie {
	return &Trie{root: newTrieNode()}
}

// Insert is O(k): one hop per character, creating nodes only where the
// path doesn't exist yet.
func (t *Trie) Insert(word string) {
	node := t.root
	for i := 0; i < len(word); i++ {
		c := word[i]
		next, ok := node.children[c]
		if !ok {
			next = newTrieNode()
			node.children[c] = next
		}
		node = next
	}
	node.isWord = true
}

func (t *Trie) Search(word string) bool {
	node := t.nodeAt(word)
	return node != nil && node.isWord
}

// StartsWith needs only that the path exists — no scan of stored words.
func (t *Trie) StartsWith(prefix string) bool {
	return t.nodeAt(prefix) != nil
}

func (t *Trie) nodeAt(key string) *TrieNode {
	node := t.root
	for i := 0; i < len(key); i++ {
		next, ok := node.children[key[i]]
		if !ok {
			return nil
		}
		node = next
	}
	return node
}
        """.trim(),

        Lang.JAVASCRIPT to """
class TrieNode {
  constructor() {
    this.children = new Map();
    this.isWord = false;
  }
}

class Trie {
  #root = new TrieNode();

  /** O(k): one hop per character, creating nodes only where a path is new. */
  insert(word) {
    let node = this.#root;
    for (const c of word) {
      if (!node.children.has(c)) node.children.set(c, new TrieNode());
      node = node.children.get(c);
    }
    node.isWord = true;
  }

  search(word) {
    return this.#nodeAt(word)?.isWord === true;
  }

  /** Existence of the path is enough — no scan of stored words required. */
  startsWith(prefix) {
    return this.#nodeAt(prefix) !== undefined;
  }

  #nodeAt(key) {
    let node = this.#root;
    for (const c of key) {
      node = node.children.get(c);
      if (!node) return undefined;
    }
    return node;
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 208,
            title = "Implement Trie (Prefix Tree)",
            difficulty = Difficulty.MEDIUM,
            idea = "The structure itself. The only trap is forgetting the explicit end-of-word flag and trying to infer 'is a word' from 'has no children', which breaks the moment one stored word is a prefix of another.",
            askedAt = "Amazon, Google, Microsoft",
        ),
        Question(
            id = 212,
            title = "Word Search II",
            difficulty = Difficulty.HARD,
            idea = "Build one trie from all target words, then DFS the board once, pruning any path whose prefix isn't in the trie. Searching the board separately per word is the trap — it revisits the same cells once per word instead of sharing the walk.",
            askedAt = "Google, Airbnb, Uber",
        ),
        Question(
            id = 211,
            title = "Design Add and Search Words Data Structure",
            difficulty = Difficulty.MEDIUM,
            idea = "A trie plus a wildcard: on a '.' character in the search, branch into every child at that position instead of just one, and recurse. Worst case degrades toward a full trie scan on a query of all dots — worth naming when asked about complexity.",
            askedAt = "Meta, Amazon",
        ),
    ),

    related = listOf("hash-tables", "binary-trees"),
    references = Refs.basecs(),
)

val AvlTrees = Topic(
    id = "avl-trees",
    title = "AVL Trees",
    tagline = "Rebalance after every insert, and height never drifts past O(log n).",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "The first self-balancing tree: after every insert or delete, rotate nodes back into balance so height never exceeds roughly 1.44 log n.",
        "The balance factor — height difference between a node's two subtrees — must stay within {-1, 0, 1} at every node, checked and fixed on the way back up from every insertion.",
        "Four rotation cases (left-left, right-right, left-right, right-left) cover every way a node can become unbalanced — only the path back to the root ever needs checking.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A plain BST's O(log n) promise silently depends on the tree staying roughly balanced, and nothing about a plain BST enforces that — insert sorted data and you get a straight line. AVL trees close that gap by adding one rule, checked after every insert and delete: for every node, the heights of its left and right subtrees may differ by at most 1. Break that rule anywhere and the tree repairs itself immediately, before the imbalance can compound.",
        "The repair mechanism is a **rotation** — a local restructuring that swaps a node with one of its children while preserving the BST ordering invariant, changing which subtree is \"taller\" without touching the sorted order at all. There are exactly four shapes an imbalance can take, each with a matching rotation: a straight left-heavy chain needs one right rotation, a straight right-heavy chain needs one left rotation, and the two \"zig-zag\" cases need two rotations each — one to straighten the zigzag before the matching single rotation applies.",
        "What makes this cheap rather than a constant rebalancing tax is that only the ancestors of the newly inserted or deleted node can possibly have become unbalanced — a change at a leaf cannot affect the height of a subtree it isn't part of. So after inserting, you walk back up the path you just descended, checking and fixing balance factors one node at a time, and can stop the moment a node is found already balanced, because balance below it means nothing above it changed either.",
        "The mathematical payoff for this bookkeeping is a hard guarantee: an AVL tree's height is always within a constant factor of log n — provably no worse than about 1.44 × log₂(n + 2). That is a stronger promise than a red-black tree's, which allows a slightly taller tree in exchange for cheaper, less frequent rebalancing — the classic trade-off between the two, and why AVL trees are typically preferred for read-heavy workloads and red-black trees for write-heavy ones.",
    ),

    origin = "**AVL trees were invented by Georgy Adelson-Velsky and Evgenii Landis**, two Soviet computer scientists, and published in their **1962 paper 'An algorithm for the organisation of information'** in *Doklady Akademii Nauk SSSR*. It was the first self-balancing binary search tree ever described, guaranteeing O(log n) height years before red-black trees or B-trees formalised alternative approaches to the same problem. The name is simply the authors' initials.",

    keyPoints = listOf(
        "**Balance factor** — height(left) − height(right) — must be in {-1, 0, 1} at every node. A node outside that range triggers a rotation before the insert or delete is considered finished.",
        "**Four rotation cases**: left-left and right-right need one rotation; left-right and right-left ('zig-zag' imbalances) need two.",
        "Only the **path from the changed node back to the root** can have become unbalanced — the rest of the tree never needs re-checking.",
        "**Height is guaranteed O(log n)** — provably at most ~1.44 log₂(n + 2) — a *stronger* balance guarantee than a red-black tree's.",
        "The trade-off against red-black trees: **AVL trees rebalance more aggressively**, which costs more on writes but keeps lookups slightly faster — the reverse of a red-black tree's priorities.",
    ),

    complexity = listOf(
        ComplexityRow("Search / insert / delete", "O(log n)", "O(1)", "Height is provably bounded — no degenerate case exists, unlike a plain BST."),
        ComplexityRow("Rotation", "O(1)", "O(1)", "A fixed, local restructuring — but insert/delete may trigger one at each level walked back up."),
    ),

    pitfalls = listOf(
        "Rebalancing only at the insertion point instead of walking back up and checking every ancestor — an imbalance can appear several levels above where the change actually happened.",
        "Misidentifying which of the four rotation cases applies — the zig-zag cases are frequently implemented as a single rotation, silently leaving the tree unbalanced.",
        "Reaching for an AVL tree when writes vastly outnumber reads — the stricter balance factor means more frequent rotations than a red-black tree tolerates.",
        "Forgetting to update stored height values on every node touched by a rotation, which corrupts every future rebalancing decision at that node.",
    ),

    steps = listOf(
        "Insert as you would into a plain BST.",
        "Walk back up the path to the root, updating each node's height.",
        "At each node, compute the balance factor. If it's outside {-1, 0, 1}, identify which of the four imbalance shapes it is.",
        "Apply the matching rotation — one for a straight imbalance, two for a zig-zag — and continue back up.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
class AvlNode(
    val value: Int,
    var left: AvlNode? = null,
    var right: AvlNode? = null,
    var height: Int = 1,
)

private fun height(node: AvlNode?) = node?.height ?: 0
private fun balanceFactor(node: AvlNode) = height(node.left) - height(node.right)
private fun updateHeight(node: AvlNode) {
    node.height = 1 + maxOf(height(node.left), height(node.right))
}

private fun rotateRight(y: AvlNode): AvlNode {
    val x = y.left!!
    y.left = x.right
    x.right = y
    updateHeight(y)
    updateHeight(x)
    return x
}

private fun rotateLeft(x: AvlNode): AvlNode {
    val y = x.right!!
    x.right = y.left
    y.left = x
    updateHeight(x)
    updateHeight(y)
    return y
}

fun insert(node: AvlNode?, value: Int): AvlNode {
    if (node == null) return AvlNode(value)

    if (value < node.value) node.left = insert(node.left, value)
    else if (value > node.value) node.right = insert(node.right, value)
    else return node // duplicates: no-op

    updateHeight(node)
    val balance = balanceFactor(node)

    return when {
        balance > 1 && value < node.left!!.value -> rotateRight(node)               // left-left
        balance < -1 && value > node.right!!.value -> rotateLeft(node)              // right-right
        balance > 1 -> { node.left = rotateLeft(node.left!!); rotateRight(node) }    // left-right
        balance < -1 -> { node.right = rotateRight(node.right!!); rotateLeft(node) } // right-left
        else -> node
    }
}
        """.trim(),

        Lang.GO to """
type AvlNode struct {
	Value       int
	Left, Right *AvlNode
	Height      int
}

func height(n *AvlNode) int {
	if n == nil {
		return 0
	}
	return n.Height
}

func updateHeight(n *AvlNode) {
	n.Height = 1 + max(height(n.Left), height(n.Right))
}

func balanceFactor(n *AvlNode) int {
	return height(n.Left) - height(n.Right)
}

func rotateRight(y *AvlNode) *AvlNode {
	x := y.Left
	y.Left = x.Right
	x.Right = y
	updateHeight(y)
	updateHeight(x)
	return x
}

func rotateLeft(x *AvlNode) *AvlNode {
	y := x.Right
	x.Right = y.Left
	y.Left = x
	updateHeight(x)
	updateHeight(y)
	return y
}

func Insert(node *AvlNode, value int) *AvlNode {
	if node == nil {
		return &AvlNode{Value: value, Height: 1}
	}
	if value < node.Value {
		node.Left = Insert(node.Left, value)
	} else if value > node.Value {
		node.Right = Insert(node.Right, value)
	} else {
		return node // duplicates: no-op
	}

	updateHeight(node)
	balance := balanceFactor(node)

	switch {
	case balance > 1 && value < node.Left.Value: // left-left
		return rotateRight(node)
	case balance < -1 && value > node.Right.Value: // right-right
		return rotateLeft(node)
	case balance > 1: // left-right
		node.Left = rotateLeft(node.Left)
		return rotateRight(node)
	case balance < -1: // right-left
		node.Right = rotateRight(node.Right)
		return rotateLeft(node)
	}
	return node
}
        """.trim(),

        Lang.JAVASCRIPT to """
class AvlNode {
  constructor(value, left = null, right = null, height = 1) {
    this.value = value;
    this.left = left;
    this.right = right;
    this.height = height;
  }
}

const height = (node) => (node ? node.height : 0);
const updateHeight = (node) => { node.height = 1 + Math.max(height(node.left), height(node.right)); };
const balanceFactor = (node) => height(node.left) - height(node.right);

function rotateRight(y) {
  const x = y.left;
  y.left = x.right;
  x.right = y;
  updateHeight(y);
  updateHeight(x);
  return x;
}

function rotateLeft(x) {
  const y = x.right;
  x.right = y.left;
  y.left = x;
  updateHeight(x);
  updateHeight(y);
  return y;
}

function insert(node, value) {
  if (node === null) return new AvlNode(value);

  if (value < node.value) node.left = insert(node.left, value);
  else if (value > node.value) node.right = insert(node.right, value);
  else return node; // duplicates: no-op

  updateHeight(node);
  const balance = balanceFactor(node);

  if (balance > 1 && value < node.left.value) return rotateRight(node);                 // left-left
  if (balance < -1 && value > node.right.value) return rotateLeft(node);                // right-right
  if (balance > 1) { node.left = rotateLeft(node.left); return rotateRight(node); }      // left-right
  if (balance < -1) { node.right = rotateRight(node.right); return rotateLeft(node); }   // right-left
  return node;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 110,
            title = "Balanced Binary Tree",
            difficulty = Difficulty.EASY,
            idea = "Checking the AVL invariant directly: at every node, the heights of its two subtrees must differ by at most 1. Compute height and check balance in the same bottom-up pass rather than two separate traversals, or it costs O(n²) instead of O(n).",
            askedAt = "Amazon, Bloomberg, Meta",
        ),
        Question(
            id = 108,
            title = "Convert Sorted Array to Binary Search Tree",
            difficulty = Difficulty.EASY,
            idea = "Always pick the middle element as the root, recursively, and the result is height-balanced for free — no rotations needed, because the input's sortedness lets you choose balance directly at construction time.",
            askedAt = "Amazon, Microsoft",
        ),
    ),

    related = listOf("binary-trees", "red-black-trees"),
    references = Refs.basecs(),
)

val RedBlackTrees = Topic(
    id = "red-black-trees",
    title = "Red-Black Trees",
    tagline = "Colour every node red or black, and four simple rules bound the height without strict balancing.",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "A looser balancing rule than AVL: colour every node red or black, enforce four colour invariants, and height is bounded at roughly 2 log n instead of AVL's tighter ~1.44 log n.",
        "Cheaper to maintain than an AVL tree — fewer rotations per insert on average — which is why it backs most language standard library ordered maps.",
        "The core invariant: every root-to-leaf path passes through the same number of black nodes, which is what keeps the tree from ever collapsing toward a line.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "AVL trees keep height tightly bounded by rebalancing on every insert, but that comes at a cost: every insertion potentially triggers a rotation, sometimes several. A red-black tree accepts a looser balance guarantee — height up to roughly 2 log n instead of AVL's ~1.44 log n — in exchange for needing far fewer rotations to maintain it. That trade is usually worth it, because the height difference barely matters in practice while the rotation savings add up across millions of writes.",
        "The mechanism is unusual for a data structure: every node gets a colour, red or black, and four rules about how those colours can appear are enough to bound the tree's height, with no explicit height or balance-factor bookkeeping at all. The rules: the root is black; every leaf (conceptually, the null pointers) is black; a red node never has a red child; and every path from a given node to any descendant leaf passes through the same number of black nodes.",
        "That last rule — equal **black-height** on every path — is where the height bound actually comes from. If the longest possible path (alternating red and black nodes) can be at most twice as long as the shortest possible path (all black), then no path can ever be more than twice as long as any other, and the tree can never collapse toward the line-shaped worst case a plain BST is vulnerable to.",
        "Fixing a colour violation after insertion needs at most a constant number of rotations, plus a possible **recolouring** cascade up toward the root — recolouring is cheap, just flipping colours with no restructuring, which is the specific thing that makes red-black trees less rotation-heavy than AVL trees on average. That cheapness is exactly why they sit behind ordered map implementations in most language standard libraries, where writes need to stay fast far more than reads need to be perfectly balanced.",
        "The practical takeaway for interviews is rarely \"implement a red-black tree from scratch\" — the rules are numerous enough that this is uncommon under time pressure — but rather recognising *why* one sits behind `TreeMap`, `std::map`, or the Linux kernel's process scheduler, and being able to state the trade-off against AVL trees precisely.",
    ),

    origin = "Red-black trees descend from **Rudolf Bayer's 1972 'symmetric binary B-trees'**, which encoded the same balance idea without using colour. **Leonidas Guibas and Robert Sedgewick renamed and reformulated the structure in their 1978 paper 'A Dichromatic Framework for Balanced Trees'**, introducing the red/black colouring that gives the structure its modern name and its now-standard four-rule formulation.",

    keyPoints = listOf(
        "**Four colour rules** bound height without explicit balance bookkeeping: root is black, leaves are black, no red node has a red child, and every root-to-leaf path has equal black-height.",
        "**Height is bounded at roughly 2 log n** — looser than an AVL tree's ~1.44 log n, but cheaper to maintain because fixes lean on cheap recolouring wherever possible.",
        "**Insert/delete fixups need at most a constant number of rotations**, unlike AVL's potential cascade — the main reason red-black trees are the more common choice in practice.",
        "Backs most **standard library ordered maps**: Java's `TreeMap`, C++'s `std::map`, and the Linux kernel's completely fair scheduler all use red-black trees specifically for this rotation-cheapness.",
        "**AVL trees win on read-heavy workloads** (tighter height bound, faster lookups); **red-black trees win on write-heavy workloads** (cheaper rebalancing) — the trade-off worth stating cleanly.",
    ),

    complexity = listOf(
        ComplexityRow("Search / insert / delete", "O(log n)", "O(1)", "Height is bounded at roughly 2 log n by the colour invariants."),
        ComplexityRow("Rebalancing per insert/delete", "O(log n) worst case", "O(1)", "Dominated by a recolouring cascade toward the root; actual rotations are O(1) amortised."),
    ),

    pitfalls = listOf(
        "Trying to memorise red-black rebalancing case-by-case for an interview — it's one of the least commonly asked 'implement this' questions precisely because the case analysis is long; knowing *why* and *when* to use one matters more than reciting the fixup algorithm.",
        "Assuming a red-black tree is always the better balanced tree — an AVL tree's tighter height bound wins on read-heavy workloads where lookups vastly outnumber writes.",
        "Forgetting that 'leaves are black' refers to the conceptual null children, not the deepest real nodes — a common source of off-by-one errors in black-height reasoning.",
        "Confusing red-black trees with B-trees because of the shared 'symmetric binary B-tree' ancestry — they solve the same balancing problem but serve very different contexts, in-memory maps versus disk-backed indexes.",
    ),

    steps = listOf(
        "Insert as you would into a plain BST, colouring the new node red.",
        "If the new node's parent is black, the four rules already hold — done.",
        "If the parent is red, that violates 'no red node has a red child' — resolve it with recolouring (if the uncle is red) or a rotation plus recolouring (if the uncle is black).",
        "Recolouring can cascade up toward the root; a rotation, once needed, resolves the violation in at most a constant number of steps.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
enum class Color { RED, BLACK }

class RbNode(
    val value: Int,
    var color: Color = Color.RED,
    var left: RbNode? = null,
    var right: RbNode? = null,
)

/**
 * Checks all four invariants at once. Full insert/delete fixup is long
 * enough that it's rarely asked for from scratch — this validator is the
 * part worth being able to write and reason about directly.
 */
fun isValidRedBlackTree(root: RbNode?): Boolean {
    if (root != null && root.color != Color.BLACK) return false // root is black
    return blackHeight(root) != -1
}

/** Returns the black-height if every path is consistent, or -1 if not. */
private fun blackHeight(node: RbNode?): Int {
    if (node == null) return 0 // null children are conceptually black leaves

    if (node.color == Color.RED) {
        if (node.left?.color == Color.RED || node.right?.color == Color.RED) {
            return -1 // a red node has a red child
        }
    }

    val leftHeight = blackHeight(node.left)
    val rightHeight = blackHeight(node.right)
    if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) {
        return -1 // unequal black-height on some path
    }

    return leftHeight + if (node.color == Color.BLACK) 1 else 0
}
        """.trim(),

        Lang.GO to """
type Color int

const (
	Red Color = iota
	Black
)

type RbNode struct {
	Value       int
	Color       Color
	Left, Right *RbNode
}

// IsValidRedBlackTree checks all four invariants at once. Full insert/delete
// fixup is long enough that it's rarely asked for from scratch — this
// validator is the part worth being able to write and reason about directly.
func IsValidRedBlackTree(root *RbNode) bool {
	if root != nil && root.Color != Black {
		return false // root is black
	}
	return blackHeight(root) != -1
}

// blackHeight returns the black-height if every path is consistent, or -1.
func blackHeight(node *RbNode) int {
	if node == nil {
		return 0 // null children are conceptually black leaves
	}

	if node.Color == Red {
		leftRed := node.Left != nil && node.Left.Color == Red
		rightRed := node.Right != nil && node.Right.Color == Red
		if leftRed || rightRed {
			return -1 // a red node has a red child
		}
	}

	left := blackHeight(node.Left)
	right := blackHeight(node.Right)
	if left == -1 || right == -1 || left != right {
		return -1 // unequal black-height on some path
	}

	if node.Color == Black {
		return left + 1
	}
	return left
}
        """.trim(),

        Lang.JAVASCRIPT to """
const Color = { RED: "red", BLACK: "black" };

class RbNode {
  constructor(value, color = Color.RED, left = null, right = null) {
    this.value = value;
    this.color = color;
    this.left = left;
    this.right = right;
  }
}

/**
 * Checks all four invariants at once. Full insert/delete fixup is long
 * enough that it's rarely asked for from scratch — this validator is the
 * part worth being able to write and reason about directly.
 */
function isValidRedBlackTree(root) {
  if (root !== null && root.color !== Color.BLACK) return false; // root is black
  return blackHeight(root) !== -1;
}

/** Returns the black-height if every path is consistent, or -1 if not. */
function blackHeight(node) {
  if (node === null) return 0; // null children are conceptually black leaves

  if (node.color === Color.RED) {
    if (node.left?.color === Color.RED || node.right?.color === Color.RED) {
      return -1; // a red node has a red child
    }
  }

  const left = blackHeight(node.left);
  const right = blackHeight(node.right);
  if (left === -1 || right === -1 || left !== right) {
    return -1; // unequal black-height on some path
  }

  return left + (node.color === Color.BLACK ? 1 : 0);
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 315,
            title = "Count of Smaller Numbers After Self",
            difficulty = Difficulty.HARD,
            idea = "An order-statistics BST — a self-balancing tree (conceptually a red-black tree) augmented with subtree sizes — answers 'how many inserted values are smaller than x' in O(log n) per insert, which is exactly what the running count needs. A Fenwick tree over compressed values is the more common accepted answer, but naming the balanced-tree approach shows you understand why augmentation works.",
            askedAt = "Google, Meta — a favourite for testing augmented balanced trees",
        ),
    ),

    related = listOf("binary-trees", "avl-trees"),
    references = Refs.basecs(),
)

val BTrees = Topic(
    id = "b-trees",
    title = "B-Trees",
    tagline = "A tree shaped for disk, where each node holds many keys instead of one.",
    level = Level.ADVANCED,

    quickSummary = listOf(
        "Wide, shallow trees — each node holds many keys and many children — designed around disk I/O, where reading one block costs the same whether it holds one key or a hundred.",
        "Height stays tiny even for huge datasets: a B-tree over a billion keys with a branching factor of a few hundred is only 3-4 levels deep.",
        "The structure behind almost every disk-backed database index — the node-size trade-off it makes only pays off once reads are expensive relative to comparisons, which is exactly disk's shape.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "A binary search tree's height grows with log₂ n, and every level down means one more pointer to chase — fine in memory, where chasing a pointer is nearly free, but disastrous on disk, where each level might mean a fresh disk seek costing milliseconds instead of nanoseconds. A B-tree's entire design answers that specific problem: instead of one key and two children per node, hold *many* keys and *many* children in each node, sized to fill exactly one disk block. Reading a node then costs one disk access regardless of how many keys it holds, so packing more keys per node directly reduces the number of disk accesses a search needs.",
        "Concretely, a B-tree of order m allows each node up to m children and m − 1 keys, keeping every node between half-full and full to bound how narrow the tree can get. Searching means comparing against the several keys in the current node to figure out which of its many children to descend into next — more comparisons per level than a binary tree, but far fewer levels overall, because the branching factor might be in the hundreds rather than two.",
        "That trade — more comparisons per node, drastically fewer nodes to visit — is exactly right when a \"visit\" (a disk read) costs vastly more than a comparison (a few CPU cycles, held in cache). A B-tree over a billion keys with a branching factor of a few hundred needs only 3-4 levels to reach any key, meaning 3-4 disk reads for any lookup — compare that to the roughly 30 levels a balanced binary tree would need, each potentially a separate seek.",
        "Insertion and deletion follow the same idea as a self-balancing binary tree — split an overfull node, merge or borrow from an underfull one — but operate on whole nodes of many keys at once rather than single elements, and rebalancing propagates upward exactly as rotations do in an AVL or red-black tree. The practical upshot is that essentially every disk-backed database index, and most filesystems' internal structures, uses a B-tree or one of its variants — B+ trees being the most common — specifically because the node-size trade-off it makes matches how disks actually work.",
    ),

    origin = "**B-trees were invented by Rudolf Bayer and Edward M. McCreight at Boeing Research Labs, published in a 1972 paper, 'Organization and Maintenance of Large Ordered Indices.'** They were solving exactly the problem of indexing large files efficiently on the disk drives of the era, where minimising the number of physical disk accesses mattered enormously. What the 'B' stands for has never been definitively confirmed by either author — candidates floated over the years include Boeing, balanced, and simply Bayer's own name — and both have stayed coy about it in interviews since.",

    keyPoints = listOf(
        "**Each node holds many keys and many children**, sized to fill one disk block — reading a node is one disk access regardless of how many keys it holds.",
        "**Height stays tiny even at huge scale**: a branching factor in the hundreds means a billion-key B-tree needs only 3-4 levels, versus roughly 30 for a balanced binary tree.",
        "**Nodes stay between half-full and full** by design — inserts that overflow a node split it, and deletes that underflow one merge or borrow from a sibling, propagating upward like AVL/red-black rebalancing.",
        "**The trade only pays off when reads are expensive relative to comparisons** — exactly disk access versus CPU comparison, and exactly why B-trees back disk-based indexes rather than in-memory ones (which use red-black trees or hash tables instead).",
        "**B+ trees**, the most common real-world variant, keep all actual data in the leaves and use internal nodes purely for navigation — better for range scans, which is why almost every database index you've used is technically a B+ tree.",
    ),

    complexity = listOf(
        ComplexityRow("Search / insert / delete", "O(log n)", "O(1)", "Base of the logarithm is the branching factor, not 2 — this is what keeps height tiny."),
        ComplexityRow("Disk accesses per operation", "O(log_m n)", "—", "m = branching factor, often in the hundreds — the number that actually matters for disk-backed structures."),
    ),

    pitfalls = listOf(
        "Using a B-tree in memory where a red-black tree or hash table would do — the whole design premise is amortising expensive disk reads across many keys per node; in memory, that trade-off doesn't apply.",
        "Confusing a B-tree with a B+ tree — B+ trees keep data only in leaves and are what most databases actually use; plain B-trees can store data in internal nodes too, which complicates range queries.",
        "Picking too small a branching factor — a B-tree with only a handful of keys per node barely improves on a binary tree's height, losing the entire point of the structure.",
        "Assuming 'B-tree' and 'binary tree' are related by more than a naming coincidence — a B-tree node routinely holds dozens or hundreds of keys, nothing like a binary tree's two children.",
    ),

    steps = listOf(
        "To search: compare the target against the keys in the current node to find which child range it falls into, then descend into that child.",
        "Repeat until reaching a leaf — the search either finds the key there or confirms it's absent.",
        "To insert: descend to the correct leaf and insert the key in sorted position within that node.",
        "If the node now exceeds its maximum key count, split it in two and push the middle key up into the parent — splits can cascade all the way to the root.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** A single B-tree node. Real implementations pick order (maxKeys) to fill one disk block. */
class BTreeNode(val maxKeys: Int) {
    val keys = mutableListOf<Int>()
    val children = mutableListOf<BTreeNode>()
    val isLeaf: Boolean get() = children.isEmpty()
}

/** O(log_m n): compares against this node's keys to pick which child to descend into. */
fun search(node: BTreeNode, target: Int): Boolean {
    var i = 0
    while (i < node.keys.size && target > node.keys[i]) i++
    if (i < node.keys.size && node.keys[i] == target) return true
    if (node.isLeaf) return false
    return search(node.children[i], target)
}

/**
 * Splits an overfull leaf node in two, returning the middle key that gets
 * pushed up to the parent — this is the operation that keeps height tiny
 * by fanning nodes back out instead of growing a single node without bound.
 */
fun splitLeaf(node: BTreeNode): Pair<Int, BTreeNode> {
    val midIndex = node.keys.size / 2
    val midKey = node.keys[midIndex]
    val right = BTreeNode(node.maxKeys)
    right.keys.addAll(node.keys.subList(midIndex + 1, node.keys.size))
    val keptKeys = node.keys.subList(0, midIndex).toList()
    node.keys.clear()
    node.keys.addAll(keptKeys)
    return midKey to right
}
        """.trim(),

        Lang.GO to """
// BTreeNode is a single B-tree node. Real implementations pick order
// (maxKeys) to fill one disk block.
type BTreeNode struct {
	MaxKeys  int
	Keys     []int
	Children []*BTreeNode
}

func (n *BTreeNode) IsLeaf() bool { return len(n.Children) == 0 }

// Search is O(log_m n): compares against this node's keys to pick which
// child to descend into.
func Search(node *BTreeNode, target int) bool {
	i := 0
	for i < len(node.Keys) && target > node.Keys[i] {
		i++
	}
	if i < len(node.Keys) && node.Keys[i] == target {
		return true
	}
	if node.IsLeaf() {
		return false
	}
	return Search(node.Children[i], target)
}

// SplitLeaf splits an overfull leaf node in two, returning the middle key
// that gets pushed up to the parent — the operation that keeps height tiny
// by fanning nodes back out instead of growing a single node without bound.
func SplitLeaf(node *BTreeNode) (int, *BTreeNode) {
	mid := len(node.Keys) / 2
	midKey := node.Keys[mid]
	right := &BTreeNode{MaxKeys: node.MaxKeys, Keys: append([]int(nil), node.Keys[mid+1:]...)}
	node.Keys = node.Keys[:mid]
	return midKey, right
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** A single B-tree node. Real implementations pick order (maxKeys) to fill one disk block. */
class BTreeNode {
  constructor(maxKeys) {
    this.maxKeys = maxKeys;
    this.keys = [];
    this.children = [];
  }
  get isLeaf() { return this.children.length === 0; }
}

/** O(log_m n): compares against this node's keys to pick which child to descend into. */
function search(node, target) {
  let i = 0;
  while (i < node.keys.length && target > node.keys[i]) i++;
  if (i < node.keys.length && node.keys[i] === target) return true;
  if (node.isLeaf) return false;
  return search(node.children[i], target);
}

/**
 * Splits an overfull leaf node in two, returning the middle key that gets
 * pushed up to the parent — the operation that keeps height tiny by
 * fanning nodes back out instead of growing a single node without bound.
 */
function splitLeaf(node) {
  const mid = Math.floor(node.keys.length / 2);
  const midKey = node.keys[mid];
  const right = new BTreeNode(node.maxKeys);
  right.keys = node.keys.slice(mid + 1);
  node.keys = node.keys.slice(0, mid);
  return [midKey, right];
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            title = "Why do databases use B-trees instead of binary search trees?",
            difficulty = Difficulty.MEDIUM,
            idea = "The real test isn't implementing one from scratch — it's explaining the trade-off precisely: minimising the number of disk seeks matters more than minimising comparisons, and a wide, shallow tree needs far fewer seeks than a narrow, deep one.",
            askedAt = "Common in database and systems-design interviews",
        ),
        Question(
            title = "Design an on-disk key-value store",
            difficulty = Difficulty.HARD,
            idea = "A B+ tree index over the data file is the standard answer: internal nodes purely for navigation, leaves holding (or pointing to) the actual records, sized so each node read is exactly one disk block.",
            askedAt = "Systems design interviews, especially at infrastructure-heavy companies",
        ),
    ),

    related = listOf("binary-trees", "avl-trees", "red-black-trees"),
    references = Refs.basecs(),
)

val Heaps = Topic(
    id = "heaps",
    title = "Heaps",
    tagline = "A tree that only promises the top is right — that weaker promise is what makes it fast.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "A heap only guarantees a parent is smaller (or larger) than its children — weaker than full sortedness, but enough to make the min or max O(log n) to insert and remove.",
        "Almost always a complete binary tree stored in a plain array — node i's children live at 2i+1 and 2i+2, no pointers needed.",
        "Peeking the min/max is O(1); searching for anything else is O(n) — a heap is not a search tree.",
        "Building a heap from an existing array is O(n), not O(n log n) — a common surprise worth being able to justify.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Sorting an entire collection just to repeatedly grab the minimum is wasteful — it pays O(n log n) up front for an order that is only ever needed at the top. A **heap** asks for less and gets more speed in return: it guarantees only that every parent is smaller (or, for a max-heap, larger) than its children, never that the whole collection is sorted. That weaker promise is still enough to make \"give me the smallest\" and \"give me the largest\" both O(log n), regardless of how many elements are inside.",
        "The heap-order invariant says nothing about how a node's two children compare to *each other* — only that both are ≥ (or ≤) their parent. That is the entire trick: it is a far cheaper property to maintain than full sortedness, yet it is still enough to guarantee the minimum sits at the root, because every path from the root only increases (or only decreases).",
        "Heaps are almost always **complete binary trees** — every level full except possibly the last, filled left to right — and that shape is dense enough to store with no pointers at all: node i's children live at indices `2i+1` and `2i+2` of a plain array. That is why \"heap\" so often means \"array kept in heap order\" rather than an actual pointer-based tree — smaller, faster, and simpler to implement than the tree it represents.",
        "Insert and extract-min both work by breaking the invariant at exactly one spot and repairing it along a single path. Inserting appends the new value at the end of the array and **bubbles it up**, swapping with its parent while it is smaller than that parent. Removing the root swaps in the very last element and **sinks it down**, swapping with the smaller child until the invariant holds again. Both are O(log n), because that is the height of a complete tree holding n nodes.",
        "The interview-relevant consequence is that a heap is the right structure whenever a problem needs the running min or max under a stream of insertions and removals, but never needs the full order — the kth largest element, merging sorted streams, or scheduling by priority. The moment a solution involves sorting something just to repeatedly look at one end of it, a heap is almost always the faster shape.",
    ),

    origin = "The heap and **heapsort were invented by J.W.J. Williams**, published as **Algorithm 232 in Communications of the ACM in 1964**. Robert Floyd improved the construction step later that same year, contributing the bottom-up **heapify** that runs in O(n) and is still the standard way to build one — a rare case where a data structure and the most efficient way to construct it were separate insights, published only months apart. The name is the plain English word for a disordered pile, chosen precisely because a heap's internal order is loose — the opposite of \"sorted\".",

    keyPoints = listOf(
        "The **heap-order invariant** — a min-heap's parent ≤ both children — is weaker than full sortedness, and that weakness is exactly what makes insert and extract both O(log n).",
        "Heaps are almost always **complete binary trees stored in a plain array**: node i's children sit at `2i+1` and `2i+2`, with no pointers needed.",
        "**Insert bubbles up**, swapping with the parent while smaller (min-heap); **extract-min sinks down** after swapping the root with the last element, always toward the smaller child.",
        "**Peeking the min/max is O(1)** — it's always the root — but reading any other element, or asking whether a value is present at all, is O(n): a heap has no search structure beyond the root ordering.",
        "**Heapify** (building a heap from an unordered array) is O(n), not O(n log n) — it looks like n inserts at O(log n) each, but most nodes sit near the bottom and sink only a short distance, so a tighter accounting gives a linear bound.",
        "A **priority queue** is the abstract interface; a **binary heap** is the usual concrete implementation of it, not a synonym for it.",
    ),

    complexity = listOf(
        ComplexityRow("Peek min/max", "O(1)", "O(1)", "Always the root."),
        ComplexityRow("Insert", "O(log n)", "O(1)", "Bubble up at most the height of the tree."),
        ComplexityRow("Extract min/max", "O(log n)", "O(1)", "Sink down after swapping in the last element."),
        ComplexityRow("Build heap from array", "O(n)", "O(1)", "Bottom-up heapify — tighter than n × O(log n) despite appearances."),
        ComplexityRow("Search for arbitrary value", "O(n)", "O(1)", "No structure beyond the root ordering — a heap is not a search tree."),
    ),

    pitfalls = listOf(
        "Expecting O(1) or even O(log n) search for an arbitrary value — a heap only orders the path to the root; anything else needs a full O(n) scan.",
        "Confusing 'heap' the data structure with the 'heap' memory region used for dynamic allocation — same word, entirely unrelated concepts.",
        "Building a heap by inserting n elements one at a time when all of them are already available — that costs O(n log n); heapify the whole array instead for O(n).",
        "Assuming a min-heap's second level is meaningfully ordered, e.g. that the second-smallest overall element must be one of the root's two children — it is guaranteed to be *a* descendant, not necessarily one at that exact level.",
        "Using a plain array with a linear scan for removal 'because it's simple' when a priority queue is needed — that is O(n) per extraction, which defeats the entire reason to reach for a heap.",
    ),

    steps = listOf(
        "To insert: append the new element at the end of the array.",
        "While it is smaller than its parent (min-heap), swap with the parent and repeat — this is 'bubbling up'.",
        "To extract the min: save the root's value, move the last element into the root position, and shrink the array by one.",
        "Sink the new root down: repeatedly swap with the smaller of its two children until it is no longer bigger than either — this is 'sinking down'.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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
        """.trim(),

        Lang.GO to """
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
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * Array-backed min-heap. Node i's children live at 2i+1 and 2i+2 — no
 * pointers needed, because the tree is always complete.
 */
class MinHeap {
  #items = [];

  peek() { return this.#items[0]; }

  insert(value) {
    this.#items.push(value);
    this.#bubbleUp(this.#items.length - 1);
  }

  extractMin() {
    const min = this.#items[0];
    const last = this.#items.pop();
    if (this.#items.length > 0) {
      this.#items[0] = last;
      this.#sinkDown(0);
    }
    return min;
  }

  #bubbleUp(i) {
    while (i > 0) {
      const parent = Math.floor((i - 1) / 2);
      if (this.#items[i] >= this.#items[parent]) break;
      [this.#items[i], this.#items[parent]] = [this.#items[parent], this.#items[i]];
      i = parent;
    }
  }

  #sinkDown(i) {
    while (true) {
      const left = 2 * i + 1;
      const right = 2 * i + 2;
      let smallest = i;
      if (left < this.#items.length && this.#items[left] < this.#items[smallest]) smallest = left;
      if (right < this.#items.length && this.#items[right] < this.#items[smallest]) smallest = right;
      if (smallest === i) break;
      [this.#items[i], this.#items[smallest]] = [this.#items[smallest], this.#items[i]];
      i = smallest;
    }
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 215,
            title = "Kth Largest Element in an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Keep a min-heap of size k rather than sorting everything: push each value, and pop whenever the heap exceeds k. The root ends up as the kth largest, in O(n log k) instead of O(n log n).",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
        Question(
            id = 347,
            title = "Top K Frequent Elements",
            difficulty = Difficulty.MEDIUM,
            idea = "Count frequencies first, then use a heap of size k over the counts instead of sorting every distinct value. Bucket sort by frequency is the O(n) alternative worth mentioning as a follow-up.",
            askedAt = "Amazon, Meta, Yahoo",
        ),
        Question(
            id = 295,
            title = "Find Median from Data Stream",
            difficulty = Difficulty.HARD,
            idea = "Two heaps: a max-heap for the lower half of the values seen so far, a min-heap for the upper half, kept within one element of each other in size. The median is then O(1) to read off the two roots, at O(log n) per insert.",
            askedAt = "Google, Amazon, Meta — a design-flavoured favourite",
        ),
    ),

    related = listOf("arrays", "binary-trees", "merge-sort", "heap-sort"),
    references = Refs.basecs(),
)

val Quicksort = Topic(
    id = "quicksort",
    title = "Quicksort",
    tagline = "Pick a pivot, partition around it, and let the recursion do the rest.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Partition around a pivot so everything smaller ends up left of it and everything bigger ends up right — then recurse on each side, no merge step needed.",
        "O(n log n) average, but a poor pivot choice degrades to O(n²) — pivot strategy is the entire engineering problem.",
        "Sorts in place with O(log n) auxiliary space, which is why it usually beats merge sort in practice despite matching average complexity.",
        "Not stable by default — equal elements can be reordered by the partition step.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Merge sort splits first and pays for the split later, in the merge. Quicksort inverts that order: do the hard work up front by picking a pivot and partitioning around it, so everything smaller than the pivot ends up to its left and everything bigger ends up to its right. Once that is done, the pivot is already in its final sorted position, and the two sides can be sorted independently — with no merge step required at all.",
        "That absence of a merge step is the whole appeal. Partitioning can be done in place, swapping elements past each other as you scan, so quicksort needs no auxiliary array the way merge sort does. That is the main reason it tends to win in practice: less memory traffic and better cache behaviour, despite an average complexity that matches merge sort exactly at O(n log n).",
        "The entire engineering problem is choosing the pivot well. A pivot that lands near the median splits the array roughly in half each time, giving log n levels of recursion. A pivot that lands near an extreme — always picking the first element on an already-sorted array, say — produces a split of size 1 and size n − 1, and the recursion degrades to O(n²), the same as never splitting at all. That is why \"always pick the first element\" is a textbook bad idea, and randomised or median-of-three pivot selection are the standard defences.",
        "That worst case is also why quicksort is not a safe default when an adversary controls the input — a solved problem for merge sort, since its bound holds unconditionally. Real-world quicksorts, including the ones behind most language standard libraries' sort for primitives, randomise the pivot specifically to make the worst case astronomically unlikely rather than merely possible.",
        "Quicksort is also not stable out of the box: two equal elements can cross each other during a partition and come out in the opposite order they went in. Where stability matters, merge sort or a stability-patched quicksort variant is the right tool instead.",
    ),

    origin = "**Quicksort was invented by Tony Hoare in 1959**, while he was a visiting student at Moscow State University working on a machine-translation project — he needed to sort Russian words quickly to look them up in a dictionary. He published it as **'Algorithm 64: Quicksort' in Communications of the ACM in 1961**. Hoare won the Turing Award in 1980, largely for work that grew out of reasoning carefully about exactly this kind of algorithm.",

    keyPoints = listOf(
        "**Partition first, recurse after** — the opposite order from merge sort. Once partitioning finishes, the pivot is already in its final sorted position.",
        "**O(n log n) average, O(n²) worst case.** The worst case happens when the pivot is consistently the smallest or largest element, which a fixed 'always pick the first element' strategy walks straight into on sorted or reverse-sorted input.",
        "**In-place, O(log n) auxiliary space** for the recursion stack — no scratch array, unlike merge sort. This is the main practical reason it tends to win despite matching average complexity.",
        "**Randomised or median-of-three pivot selection** turns the worst case from 'likely on common input shapes' into 'vanishingly unlikely regardless of input' — the standard real-world defence.",
        "**Not stable** by default — a partition step can reorder equal elements relative to each other.",
        "Below roughly 10–20 elements, insertion sort is faster; production quicksorts switch to it for small sub-arrays rather than recursing all the way down.",
    ),

    complexity = listOf(
        ComplexityRow("Best / average case", "O(n log n)", "O(log n)", "Balanced partitions; space is the recursion stack."),
        ComplexityRow("Worst case", "O(n²)", "O(n)", "Consistently unbalanced partitions — degenerate recursion depth of n."),
        ComplexityRow("Randomised pivot", "O(n log n) expected", "O(log n)", "The worst case still exists but is astronomically unlikely."),
    ),

    pitfalls = listOf(
        "Always picking the first (or last) element as the pivot — on already-sorted or reverse-sorted input this hits the O(n²) worst case every time.",
        "Forgetting that quicksort is not stable — relying on it to preserve the relative order of equal elements is a real bug, not a theoretical nitpick.",
        "Skipping the base case check and recursing on empty or single-element sub-arrays needlessly.",
        "Using it where a hard O(n log n) bound is required regardless of input, e.g. real-time systems — merge sort or heap sort give that guarantee, quicksort does not.",
        "Deep recursion on worst-case input overflowing the call stack — 'introsort' (falling back to heap sort past a recursion depth limit) is the standard production fix.",
    ),

    steps = listOf(
        "Pick a pivot — a random element or median-of-three, to avoid the worst case.",
        "Partition: scan the array, moving everything smaller than the pivot to its left and everything bigger to its right.",
        "The pivot is now in its final sorted position — recurse on the sub-array to its left.",
        "Recurse on the sub-array to its right.",
        "A sub-array of size 0 or 1 is already sorted — that is the base case.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
fun quicksort(nums: IntArray, lo: Int = 0, hi: Int = nums.lastIndex) {
    if (lo >= hi) return
    val p = partition(nums, lo, hi)
    quicksort(nums, lo, p - 1)
    quicksort(nums, p + 1, hi)
}

/** Lomuto partition, with a randomised pivot to avoid the O(n²) worst case. */
private fun partition(nums: IntArray, lo: Int, hi: Int): Int {
    val pivotIndex = (lo..hi).random()
    nums[pivotIndex] = nums[hi].also { nums[hi] = nums[pivotIndex] }
    val pivot = nums[hi]

    var boundary = lo
    for (i in lo until hi) {
        if (nums[i] < pivot) {
            nums[i] = nums[boundary].also { nums[boundary] = nums[i] }
            boundary++
        }
    }
    nums[boundary] = nums[hi].also { nums[hi] = nums[boundary] }
    return boundary
}
        """.trim(),

        Lang.GO to """
func Quicksort(nums []int, lo, hi int) {
	if lo >= hi {
		return
	}
	p := partition(nums, lo, hi)
	Quicksort(nums, lo, p-1)
	Quicksort(nums, p+1, hi)
}

// partition is Lomuto's scheme, with a randomised pivot to avoid the
// O(n^2) worst case.
func partition(nums []int, lo, hi int) int {
	pivotIndex := lo + rand.Intn(hi-lo+1)
	nums[pivotIndex], nums[hi] = nums[hi], nums[pivotIndex]
	pivot := nums[hi]

	boundary := lo
	for i := lo; i < hi; i++ {
		if nums[i] < pivot {
			nums[i], nums[boundary] = nums[boundary], nums[i]
			boundary++
		}
	}
	nums[boundary], nums[hi] = nums[hi], nums[boundary]
	return boundary
}
        """.trim(),

        Lang.JAVASCRIPT to """
function quicksort(nums, lo = 0, hi = nums.length - 1) {
  if (lo >= hi) return nums;
  const p = partition(nums, lo, hi);
  quicksort(nums, lo, p - 1);
  quicksort(nums, p + 1, hi);
  return nums;
}

/** Lomuto partition, with a randomised pivot to avoid the O(n^2) worst case. */
function partition(nums, lo, hi) {
  const pivotIndex = lo + Math.floor(Math.random() * (hi - lo + 1));
  [nums[pivotIndex], nums[hi]] = [nums[hi], nums[pivotIndex]];
  const pivot = nums[hi];

  let boundary = lo;
  for (let i = lo; i < hi; i++) {
    if (nums[i] < pivot) {
      [nums[i], nums[boundary]] = [nums[boundary], nums[i]];
      boundary++;
    }
  }
  [nums[boundary], nums[hi]] = [nums[hi], nums[boundary]];
  return boundary;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Implement quicksort directly and the naive first-element pivot times out on the test suite's adversarial cases — randomising the pivot choice is the fix, and being asked to explain why is the actual point of the question.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
        Question(
            id = 75,
            title = "Sort Colors",
            difficulty = Difficulty.MEDIUM,
            idea = "The Dutch national flag problem — a three-way partition, which is quicksort's partition step generalised from two buckets to three. One pass with low/mid/high pointers sorts 0s, 1s and 2s without a full comparison sort.",
            askedAt = "Amazon, Meta, Microsoft",
        ),
        Question(
            id = 215,
            title = "Kth Largest Element in an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Quickselect: run quicksort's partition step, but only recurse into the one side that must contain the kth element. Average O(n), because each partition throws away the other half's work entirely instead of sorting it.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
    ),

    related = listOf("merge-sort", "arrays"),
    references = Refs.basecs(),
)

val HeapSort = Topic(
    id = "heap-sort",
    title = "Heap Sort",
    tagline = "Turn an array into a heap, then pop the max off the end every time.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Build a max-heap from the array in O(n), then repeatedly swap the root — the max — to the end and shrink the heap: O(n log n), in place, no scratch array.",
        "The only common O(n log n) sort that needs O(1) extra space — merge sort needs O(n), quicksort's worst case needs O(n) of recursion stack.",
        "Not stable, and slower in practice than quicksort despite matching Big-O, because sinking jumps around the array rather than scanning it.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Heaps already give O(1) access to the maximum and O(log n) removal of it. Heap sort just uses that directly: build a max-heap out of the whole array, then repeatedly take the max off the top and place it at the end of the still-shrinking heap. Do that n times and the array ends up fully sorted, largest at the end, smallest at index 0.",
        "The trick that makes it in-place is that the removed maximum does not need anywhere new to go — the heap only occupies the front part of the array, so the space vacated by shrinking it by one is exactly where the extracted max belongs. Swap the root with the last live element, shrink the heap's boundary by one, and sink the new root down to restore the heap property; repeat.",
        "That is also exactly why heap sort needs no auxiliary array, unlike merge sort — everything happens by swapping elements within the one array you started with. The price for that space guarantee is speed in practice: each sink-down jumps around the array by index-doubling (`2i+1`, `2i+2`) rather than scanning sequentially, which is much harder on the CPU cache than quicksort's mostly-sequential partitioning. Same O(n log n) on paper, noticeably slower on real hardware.",
        "Heap sort's one unconditional guarantee is what makes it valuable despite that: no adversarial input pushes it to O(n²) the way quicksort's does, and it needs no scratch memory the way merge sort does. That is precisely why it is the fallback inside \"introsort\" hybrids — quicksort's usual approach, with a switch to heap sort if the recursion depth suggests the worst case has been hit.",
    ),

    origin = "Heap sort was **published by J.W.J. Williams in the same 1964 paper that introduced the heap itself** — Algorithm 232 in Communications of the ACM — making the data structure and the sorting algorithm built on it a single original contribution rather than two separate discoveries. Robert Floyd's O(n) heapify improvement, published later that same year, is what turned the construction step from the paper's original approach into the O(n) one taught today.",

    keyPoints = listOf(
        "**Build a max-heap in O(n)**, then repeatedly swap the root into the last live slot and sink the new root down — n extractions, each O(log n).",
        "**O(n log n) in every case** — best, average and worst — because there is no adversarial input the way there is for quicksort.",
        "**O(1) auxiliary space.** It is the only common O(n log n) sort that needs neither a scratch array (merge sort) nor risks O(n) recursion depth (quicksort's worst case).",
        "**Not stable** — swapping the max into place can reorder equal elements.",
        "**Poor cache locality** relative to quicksort, despite matching Big-O — sinking jumps by index-doubling rather than scanning sequentially.",
        "The classic fallback inside **introsort**: run quicksort, but switch to heap sort if the recursion goes deeper than expected, guaranteeing the O(n log n) bound never slips to O(n²).",
    ),

    complexity = listOf(
        ComplexityRow("Build heap", "O(n)", "O(1)", "Bottom-up heapify — see the Heaps topic for why this isn't O(n log n)."),
        ComplexityRow("Sort (all cases)", "O(n log n)", "O(1)", "n extractions, each an O(log n) sink-down — no adversarial input changes this."),
    ),

    pitfalls = listOf(
        "Forgetting to re-sink after swapping the max to the end — the new root is very likely out of place, and skipping the sink-down silently breaks the sort.",
        "Assuming heap sort beats quicksort in practice because they share Big-O — quicksort's better cache locality usually wins on real data despite the matching complexity.",
        "Building the heap with n individual inserts instead of bottom-up heapify — that costs O(n log n) for construction alone, throwing away the whole point of an O(n) build.",
        "Expecting stability — equal elements can and do get reordered by the swap-and-sink process.",
    ),

    steps = listOf(
        "Build a max-heap from the whole array using bottom-up heapify.",
        "Swap the root (the maximum) with the last element of the current heap region.",
        "Shrink the heap's boundary by one — the swapped-in element is now permanently sorted.",
        "Sink the new root down to restore the heap property within the smaller heap.",
        "Repeat until the heap region is a single element.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
fun heapSort(nums: IntArray) {
    // Bottom-up heapify: start at the last parent, work back to the root.
    for (i in nums.size / 2 - 1 downTo 0) sinkDown(nums, i, nums.size)

    for (end in nums.lastIndex downTo 1) {
        nums[0] = nums[end].also { nums[end] = nums[0] }
        sinkDown(nums, 0, end)
    }
}

/** Restores the max-heap property at [start], within the live region [0, size). */
private fun sinkDown(nums: IntArray, start: Int, size: Int) {
    var i = start
    while (true) {
        val left = 2 * i + 1
        val right = 2 * i + 2
        var largest = i
        if (left < size && nums[left] > nums[largest]) largest = left
        if (right < size && nums[right] > nums[largest]) largest = right
        if (largest == i) break
        nums[i] = nums[largest].also { nums[largest] = nums[i] }
        i = largest
    }
}
        """.trim(),

        Lang.GO to """
func HeapSort(nums []int) {
	// Bottom-up heapify: start at the last parent, work back to the root.
	for i := len(nums)/2 - 1; i >= 0; i-- {
		sinkDown(nums, i, len(nums))
	}

	for end := len(nums) - 1; end >= 1; end-- {
		nums[0], nums[end] = nums[end], nums[0]
		sinkDown(nums, 0, end)
	}
}

// sinkDown restores the max-heap property at start, within the live
// region [0, size).
func sinkDown(nums []int, start, size int) {
	i := start
	for {
		left, right := 2*i+1, 2*i+2
		largest := i
		if left < size && nums[left] > nums[largest] {
			largest = left
		}
		if right < size && nums[right] > nums[largest] {
			largest = right
		}
		if largest == i {
			break
		}
		nums[i], nums[largest] = nums[largest], nums[i]
		i = largest
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
function heapSort(nums) {
  // Bottom-up heapify: start at the last parent, work back to the root.
  for (let i = Math.floor(nums.length / 2) - 1; i >= 0; i--) {
    sinkDown(nums, i, nums.length);
  }

  for (let end = nums.length - 1; end >= 1; end--) {
    [nums[0], nums[end]] = [nums[end], nums[0]];
    sinkDown(nums, 0, end);
  }
  return nums;
}

/** Restores the max-heap property at start, within the live region [0, size). */
function sinkDown(nums, start, size) {
  let i = start;
  while (true) {
    const left = 2 * i + 1;
    const right = 2 * i + 2;
    let largest = i;
    if (left < size && nums[left] > nums[largest]) largest = left;
    if (right < size && nums[right] > nums[largest]) largest = right;
    if (largest === i) break;
    [nums[i], nums[largest]] = [nums[largest], nums[i]];
    i = largest;
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 912,
            title = "Sort an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "The one implementation that never degrades: no scratch array like merge sort, no adversarial input like quicksort. The trade-off worth naming is heap sort's cache-unfriendly access pattern, usually visibly slower in practice despite identical Big-O.",
            askedAt = "The standard \"implement a sort\" screen",
        ),
        Question(
            id = 215,
            title = "Kth Largest Element in an Array",
            difficulty = Difficulty.MEDIUM,
            idea = "Heapify the whole array in O(n), then extract the max only k times rather than sorting everything — O(n + k log n). Contrast this with the size-k min-heap approach: heapify-and-extract wins when k is close to n, the running min-heap wins when k is small.",
            askedAt = "Amazon, Meta, Bloomberg",
        ),
    ),

    related = listOf("heaps", "merge-sort"),
    references = Refs.basecs(),
)

val CountingSort = Topic(
    id = "counting-sort",
    title = "Counting Sort",
    tagline = "Skip comparisons entirely — count occurrences and place each value directly.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "No comparisons at all: count how many times each value occurs, then use those counts to place every element directly — O(n + k) for a value range of size k.",
        "Only works when values are small non-negative integers (or map cleanly to them) — a huge range defeats the whole idea, since the count array's cost is driven by the range, not by n.",
        "The stable variant, built on a running prefix sum over the counts, is the version that matters — radix sort is built directly on top of it.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Every comparison-based sort — merge sort, quicksort, heap sort — is bound by the same wall: Ω(n log n) comparisons, because there is no faster way to distinguish n! possible orderings. Counting sort escapes that wall entirely by not comparing elements to each other at all. If the values being sorted are integers in a known, small range, you can count how many times each value occurs and then read the counts back out in order — no comparisons required.",
        "Concretely: allocate a count array sized to the range of possible values, scan the input once incrementing the count for each value seen, then walk the count array in order, emitting each value as many times as it occurred. Two linear passes, total cost O(n + k), where k is the size of the value range — genuinely faster than O(n log n) when k is small relative to n.",
        "That \"when k is small\" is the entire catch, and it is why counting sort is not a general-purpose sort. If the values range from 0 to a billion, the count array itself costs O(billion) regardless of how few elements you are actually sorting — the algorithm's cost is driven by the *range* of possible values, not by n alone. Sorting ages (0–120) or exam scores (0–100) is exactly what this is for; sorting arbitrary 64-bit integers is not.",
        "The version worth knowing precisely is the **stable** one, built with a running prefix sum over the counts rather than the naive \"just re-emit\" approach. Converting counts to prefix sums gives, for each value, the index its first occurrence should land at in the output — and placing elements from the end of the input backwards through that index preserves their original relative order. That stability is not a nice-to-have; it is the exact property **radix sort** depends on, since radix sort is counting sort run once per digit, and each digit's pass has to preserve the order the previous one already established.",
    ),

    origin = "Counting sort was described by **Harold H. Seward in his 1954 master's thesis at MIT**, developed as the tool that makes each digit's pass of radix sort possible. It predates almost every other named sort in this curriculum, including quicksort and merge sort's popularisation, precisely because trading comparisons for direct counting is such a direct exploitation of a small, known value range.",

    keyPoints = listOf(
        "**No comparisons** — the value itself is used as (or maps to) an index, which is how it beats the Ω(n log n) comparison-sort lower bound.",
        "**O(n + k)**, where k is the size of the value range. Fast when k is small relative to n; the count array's size is driven by the range, not by how many elements you actually have.",
        "**The stable version uses a running prefix sum over the counts** to compute each value's starting position in the output, then places elements back-to-front to preserve their relative order.",
        "Only sorts **small non-negative integers**, or anything mappable to them (characters, bounded scores) — not arbitrary comparable objects.",
        "**Radix sort is counting sort applied once per digit** — the stability of each pass is what lets the next digit's pass build correctly on top of it.",
    ),

    complexity = listOf(
        ComplexityRow("Count + place", "O(n + k)", "O(n + k)", "n input elements, k possible distinct values — the count array plus (for the stable version) the output array."),
        ComplexityRow("vs. comparison sorts", "—", "—", "Beats the Ω(n log n) comparison lower bound only because it never compares elements — it exploits a bounded value range instead."),
    ),

    pitfalls = listOf(
        "Using it on a value range far larger than the input size — the count array's cost is driven by the range, not n, so this can be far slower and more memory-hungry than a comparison sort.",
        "Using the naive 'just re-emit counted values' version when stability matters — that version loses the original relative order of equal elements; the prefix-sum version preserves it.",
        "Forgetting to offset by the minimum value when the input includes negatives — counts need a non-negative index, so shift by `-min` first.",
        "Reaching for it on floating-point or arbitrary object data — it fundamentally requires values that map to small integer indices.",
    ),

    steps = listOf(
        "Find the range of values — minimum and maximum — to size the count array.",
        "Scan the input once, incrementing `count[value]` for each element.",
        "Convert counts to a running prefix sum, so `count[v]` becomes the number of elements ≤ v.",
        "Walk the input from the end backwards, placing each element at `count[value] - 1` in the output and decrementing that count — this is what preserves stability.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Stable counting sort. Assumes non-negative values; offset first if not. */
fun countingSort(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    val max = nums.max()

    val counts = IntArray(max + 1)
    for (value in nums) counts[value]++

    // Running prefix sum: count[v] becomes "how many elements are <= v".
    for (v in 1..max) counts[v] += counts[v - 1]

    val output = IntArray(nums.size)
    // Walking backwards through the input is what makes this stable —
    // equal values keep their original relative order.
    for (i in nums.indices.reversed()) {
        val value = nums[i]
        counts[value]--
        output[counts[value]] = value
    }
    return output
}
        """.trim(),

        Lang.GO to """
// CountingSort is stable. Assumes non-negative values; offset first if not.
func CountingSort(nums []int) []int {
	if len(nums) == 0 {
		return nums
	}
	max := nums[0]
	for _, v := range nums {
		if v > max {
			max = v
		}
	}

	counts := make([]int, max+1)
	for _, v := range nums {
		counts[v]++
	}

	// Running prefix sum: counts[v] becomes "how many elements are <= v".
	for v := 1; v <= max; v++ {
		counts[v] += counts[v-1]
	}

	output := make([]int, len(nums))
	// Walking backwards through the input is what makes this stable.
	for i := len(nums) - 1; i >= 0; i-- {
		v := nums[i]
		counts[v]--
		output[counts[v]] = v
	}
	return output
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Stable counting sort. Assumes non-negative values; offset first if not. */
function countingSort(nums) {
  if (nums.length === 0) return nums;
  const max = Math.max(...nums);

  const counts = new Array(max + 1).fill(0);
  for (const value of nums) counts[value]++;

  // Running prefix sum: counts[v] becomes "how many elements are <= v".
  for (let v = 1; v <= max; v++) counts[v] += counts[v - 1];

  const output = new Array(nums.length);
  // Walking backwards through the input is what makes this stable.
  for (let i = nums.length - 1; i >= 0; i--) {
    const value = nums[i];
    counts[value]--;
    output[counts[value]] = value;
  }
  return output;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 75,
            title = "Sort Colors",
            difficulty = Difficulty.MEDIUM,
            idea = "With only 3 possible values, this is counting sort with 3 buckets: count the 0s, 1s and 2s, then overwrite the array from the counts. The one-pass Dutch-flag partition is the fancier answer, but naming this as counting sort first shows you understand why it works.",
            askedAt = "Amazon, Meta, Microsoft",
        ),
        Question(
            id = 347,
            title = "Top K Frequent Elements",
            difficulty = Difficulty.MEDIUM,
            idea = "Bucket sort by frequency — index a bucket array by count (0 to n) and drop each value into its bucket — reads off the top k in O(n), beating both a full sort and a heap-based O(n log k) approach.",
            askedAt = "Amazon, Meta, Yahoo",
        ),
        Question(
            id = 164,
            title = "Maximum Gap",
            difficulty = Difficulty.HARD,
            idea = "The pigeonhole principle bounds the minimum possible maximum gap given n elements across a known range, which sizes buckets so bucket sort finds the answer in O(n) — no full comparison sort required.",
            askedAt = "Google, Meta — a favourite for testing non-comparison sorting",
        ),
    ),

    related = listOf("radix-sort", "arrays"),
    references = Refs.basecs(),
)

val RadixSort = Topic(
    id = "radix-sort",
    title = "Radix Sort",
    tagline = "Sort by one digit at a time, least significant first, and the whole number falls into order.",
    level = Level.INTERMEDIATE,

    quickSummary = listOf(
        "Sort by the least significant digit first, most significant last — running a stable counting sort once per digit position leaves the whole number correctly ordered.",
        "O(d × (n + k)) for d digits and a base-k counting sort per pass — linear in n when the digit count is bounded, beating O(n log n) comparison sorts.",
        "Every pass must be stable, or the ordering established by earlier, less significant digits gets destroyed by a later pass.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Counting sort is fast, but limited to a small range of values — sorting large numbers directly by their full value is out of the question. Radix sort's trick is not to sort by the full value at all: sort by one digit at a time, using counting sort as the tool for each pass, starting from the least significant digit and working up to the most significant.",
        "The order the digits are processed in is not incidental — it is the entire mechanism. After sorting by the ones digit, numbers ending in the same digit are grouped together, in whatever relative order they arrived in. Sort that result by the tens digit next, and because the pass is *stable*, ties on the tens digit fall back to the order the ones-digit pass already established. By the time the most significant digit's pass finishes, every digit position has been correctly resolved in the right priority — most significant wins overall, but only because every less-significant tie-break survived intact from the earlier passes.",
        "That stability requirement is not a minor implementation detail — it is why radix sort has to be built on the prefix-sum version of counting sort specifically, not the naive \"recount and re-emit\" one. A single unstable pass anywhere in the sequence would scramble every ordering decision the earlier passes made.",
        "The payoff is a sort that runs in O(d × (n + k)) for d digits and a base-k counting sort per digit — genuinely linear in n once the digit count is bounded, which beats every comparison sort's O(n log n) floor. The catch mirrors counting sort's: this only works cleanly on fixed-width keys with a small number of digits — 32-bit integers, fixed-length strings — not on arbitrary comparable values.",
    ),

    origin = "Radix sort predates electronic computing entirely. **Herman Hollerith's punch-card tabulating machines**, built for the **1890 US Census**, sorted cards mechanically one column — one digit — at a time, running each card through a sorter for the current digit before moving to the next. That is a physical, mechanical radix sort, decades before the term or a formal computer algorithm existed. **Harold Seward's 1954 MIT thesis**, alongside describing counting sort, formalised the digit-by-digit computer version still used today.",

    keyPoints = listOf(
        "**Least significant digit first.** Sorting most-significant-digit first would need to reopen and re-sort each group by the next digit — LSD-first with stable passes avoids that entirely.",
        "**Every pass must be stable**, using prefix-sum counting sort. An unstable pass destroys the ordering that earlier, less-significant passes already established.",
        "**O(d × (n + k))** for d digits and a base-k counting sort per digit — linear in n when d is bounded, which is how it beats the O(n log n) comparison-sort floor.",
        "Works cleanly on **fixed-width keys**: integers with a bounded digit count, fixed-length strings. Variable-length keys need padding or a most-significant-digit variant.",
        "**Base choice is a real trade-off.** Base 10 needs 10 counting buckets and more passes; base 256 (byte-at-a-time) needs fewer passes but a bigger count array per pass.",
    ),

    complexity = listOf(
        ComplexityRow("Sort", "O(d × (n + k))", "O(n + k)", "d = number of digits, k = base (bucket count per pass). Linear in n when d is bounded."),
        ComplexityRow("vs. comparison sorts", "—", "—", "Beats O(n log n) only when d is small relative to log n — very large or unbounded-length keys erase the advantage."),
    ),

    pitfalls = listOf(
        "Sorting most-significant-digit first without a plan for re-partitioning each group by the next digit — LSD-first with stable passes is simpler and is almost always what 'radix sort' means in practice.",
        "Using an unstable counting sort for any pass — it silently destroys the work of every earlier, less-significant pass.",
        "Applying it to keys with wildly varying digit counts without padding — shorter keys need to be treated as having leading zeros, or they sort incorrectly relative to longer ones.",
        "Choosing a base without thinking about the trade-off — a tiny base means many passes; a huge base means a large count array rebuilt on every pass.",
    ),

    steps = listOf(
        "Find the maximum number of digits among all values, to know how many passes are needed.",
        "For each digit position, starting from the least significant: run a stable counting sort keyed on just that digit.",
        "Use the result of each pass as the input to the next — ties are already correctly broken by every earlier, less-significant pass.",
        "After the most significant digit's pass, the array is fully sorted.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** LSD radix sort for non-negative integers, base 10. */
fun radixSort(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    var result = nums.copyOf()
    var placeValue = 1
    while (result.max() / placeValue > 0) {
        result = countingSortByDigit(result, placeValue)
        placeValue *= 10
    }
    return result
}

/** Stable counting sort keyed on the digit at [placeValue] (1, 10, 100, ...). */
private fun countingSortByDigit(nums: IntArray, placeValue: Int): IntArray {
    val counts = IntArray(10)
    for (value in nums) counts[(value / placeValue) % 10]++
    for (d in 1..9) counts[d] += counts[d - 1]

    val output = IntArray(nums.size)
    for (i in nums.indices.reversed()) {
        val digit = (nums[i] / placeValue) % 10
        counts[digit]--
        output[counts[digit]] = nums[i]
    }
    return output
}
        """.trim(),

        Lang.GO to """
// RadixSort is LSD radix sort for non-negative integers, base 10.
func RadixSort(nums []int) []int {
	if len(nums) == 0 {
		return nums
	}
	result := append([]int(nil), nums...)
	max := result[0]
	for _, v := range result {
		if v > max {
			max = v
		}
	}

	for placeValue := 1; max/placeValue > 0; placeValue *= 10 {
		result = countingSortByDigit(result, placeValue)
	}
	return result
}

// countingSortByDigit is a stable counting sort keyed on the digit at
// placeValue (1, 10, 100, ...).
func countingSortByDigit(nums []int, placeValue int) []int {
	var counts [10]int
	for _, v := range nums {
		counts[(v/placeValue)%10]++
	}
	for d := 1; d <= 9; d++ {
		counts[d] += counts[d-1]
	}

	output := make([]int, len(nums))
	for i := len(nums) - 1; i >= 0; i-- {
		digit := (nums[i] / placeValue) % 10
		counts[digit]--
		output[counts[digit]] = nums[i]
	}
	return output
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** LSD radix sort for non-negative integers, base 10. */
function radixSort(nums) {
  if (nums.length === 0) return nums;
  let result = [...nums];
  const max = Math.max(...result);

  for (let placeValue = 1; Math.floor(max / placeValue) > 0; placeValue *= 10) {
    result = countingSortByDigit(result, placeValue);
  }
  return result;
}

/** Stable counting sort keyed on the digit at placeValue (1, 10, 100, ...). */
function countingSortByDigit(nums, placeValue) {
  const counts = new Array(10).fill(0);
  for (const value of nums) counts[Math.floor(value / placeValue) % 10]++;
  for (let d = 1; d <= 9; d++) counts[d] += counts[d - 1];

  const output = new Array(nums.length);
  for (let i = nums.length - 1; i >= 0; i--) {
    const digit = Math.floor(nums[i] / placeValue) % 10;
    counts[digit]--;
    output[counts[digit]] = nums[i];
  }
  return output;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 164,
            title = "Maximum Gap",
            difficulty = Difficulty.HARD,
            idea = "Radix sort the integers in O(n) instead of comparison-sorting them in O(n log n), then a single linear scan finds the maximum gap. The insight worth stating out loud is that the sort itself, not the scan, is what the O(n) requirement is really testing.",
            askedAt = "Google, Meta — a favourite for testing non-comparison sorting",
        ),
        Question(
            id = 1,
            title = "Two Sum",
            difficulty = Difficulty.EASY,
            idea = "Not a radix-sort problem itself, but a useful contrast: if the values were bounded integers instead of arbitrary ones, a radix or counting sort followed by a two-pointer scan is a valid O(n)-ish alternative to the hash-table approach — worth mentioning to show you know when non-comparison sorting applies.",
            askedAt = "The single most asked interview question",
        ),
    ),

    related = listOf("counting-sort", "arrays"),
    references = Refs.basecs(),
)
