package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.arrayScene

val Arrays = Topic(
    id = "arrays",
    title = "Arrays",
    tagline = "One unbroken block of memory — and everything that follows from it.",
    level = Level.BASIC,
    scene = { arrayScene() },

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
