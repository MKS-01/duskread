package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.mergeSortScene

val MergeSort = Topic(
    id = "merge-sort",
    title = "Merge Sort",
    tagline = "Divide until trivial, then merge sorted runs back together.",
    level = Level.INTERMEDIATE,
    scene = { mergeSortScene() },

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
