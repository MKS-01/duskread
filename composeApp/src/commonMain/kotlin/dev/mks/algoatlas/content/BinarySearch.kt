package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.binarySearchScene

val BinarySearch = Topic(
    id = "binary-search",
    title = "Binary Search",
    tagline = "Halve the search space on every comparison.",
    level = Level.BASIC,
    scene = { binarySearchScene() },

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
