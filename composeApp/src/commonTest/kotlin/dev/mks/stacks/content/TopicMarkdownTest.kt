package dev.mks.stacks.content

import dev.mks.stacks.model.ComplexityRow
import dev.mks.stacks.model.Difficulty
import dev.mks.stacks.model.Lang
import dev.mks.stacks.model.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trips the migrated Binary Search topic — the first topic converted
 * from a compiled `Topic` literal to this format — against every field the
 * old literal held, so a change to the parser can't silently drop content.
 */
class TopicMarkdownTest {
    private val raw = """
---
id: binary-search
title: Binary Search
tagline: Halve the search space on every comparison.
level: basic
scene: binarySearchScene
related: arrays, merge-sort
---

## Quick Summary
- Halve the search space on every comparison — O(log n) instead of O(n), but only on sorted data.
- Reframe it as finding the boundary of a monotonic predicate, not just a value in an array — that unlocks 'search the answer' problems.
- Write `lo + (hi - lo) / 2`, never `(lo + hi) / 2` — the latter can silently overflow.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Binary search is the pay-off for keeping data sorted. One comparison against the middle element tells you which half the answer cannot be in, so you throw that half away and repeat. Ten elements takes four comparisons; a billion takes thirty.

The mental model that generalises best is not "find a value in an array" — it is "find the boundary in a monotonic predicate". Imagine mapping every index to true or false, where the sequence looks like false, false, false, true, true. Binary search finds the first true. Once you can phrase a problem that way, it does not matter whether you are searching an array, a range of answers, or a rotated list.

That reframing is what turns binary search from a library call into an interview weapon. "Koko eating bananas" has no sorted array anywhere in the statement, but "can she finish at speed k?" is monotonic in k — false for small speeds, true for large ones — so you binary search the answer itself.

## Key Points
- The input must be **sorted** with respect to whatever you are comparing — otherwise halving is unjustified.
- Write `mid = lo + (hi - lo) / 2`, not `(lo + hi) / 2`. The latter overflows once `lo + hi` exceeds `Int.MAX_VALUE`, a bug that sat in the JDK for nine years.
- Prefer the **lower-bound** form (half-open range, `lo < hi`, no equality check) as your default. It returns the insertion point, handles duplicates, and has no special case for "not found".
- Every loop iteration must strictly shrink the range, or you spin forever. Check that each branch either raises `lo` or lowers `hi`.
- Binary search on the *answer* applies whenever a predicate is monotonic, even with no array in sight.

## Complexity
Search | O(log n) | O(1) | Iterative form. The range halves each step, so at most comparisons.
Search (recursive) | O(log n) | O(log n) | Call stack depth. Prefer the iterative form unless the recursion reads better.
Sorting first | O(n log n) | O(1)–O(n) | If you sort only to search once, a linear scan at O(n) is cheaper.

## Pitfalls
- Using `(lo + hi) / 2` on large ranges — silent integer overflow, negative index, crash.
- Mixing an inclusive `hi = n - 1` with a half-open loop condition `lo < hi`, or vice versa. Pick one convention and keep it consistent.
- Writing `lo = mid` instead of `lo = mid + 1`, which makes no progress when `hi == lo + 1` and hangs the loop.
- Assuming a returned index is unique when the array holds duplicates — plain binary search may return any matching index.
- Applying it to data that is only *nearly* sorted. Any inversion breaks the halving argument.

## Steps
1. Set `lo = 0` and `hi = n - 1`, making the whole array live.
2. While `lo <= hi`, compute `mid` without overflowing.
3. If `nums[mid] == target`, you are done — return `mid`.
4. If `nums[mid] < target`, the target must be to the right, so set `lo = mid + 1`.
5. Otherwise the target is to the left, so set `hi = mid - 1`.
6. If the loop exits, the range is empty and the target is absent. `lo` now holds the index where it would be inserted.

## Code: Kotlin
```kotlin
fun binarySearch(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.lastIndex
    return lo + hi
}
```

## Code: Go
```go
func BinarySearch(nums []int, target int) int {
	lo, hi := 0, len(nums)-1
	return lo + hi
}
```

## Questions
### Binary Search
id: 704
difficulty: easy
askedAt: Warm-up at almost every company
The template itself. Worth writing from memory until the boundary conditions stop needing thought.

### Search in Rotated Sorted Array
id: 33
difficulty: medium
askedAt: Amazon, Meta, Microsoft
A rotated array still has one sorted half at every step.

## References
    """.trimIndent()

    private val topic = parseTopic(raw)

    @Test
    fun frontMatter() {
        assertEquals("binary-search", topic.id)
        assertEquals("Binary Search", topic.title)
        assertEquals("Halve the search space on every comparison.", topic.tagline)
        assertEquals(Level.BASIC, topic.level)
        assertEquals(listOf("arrays", "merge-sort"), topic.related)
        assertTrue(topic.scene != null, "scene: binarySearchScene should resolve through SceneRegistry")
    }

    @Test
    fun quickSummaryAndKeyPoints() {
        assertEquals(3, topic.quickSummary.size)
        assertTrue(topic.quickSummary[0].startsWith("Halve the search space"))
        assertEquals(5, topic.keyPoints.size)
        assertTrue(topic.keyPoints[2].contains("\"not found\""), "escaped quotes must round-trip")
    }

    @Test
    fun readMore() {
        val readMore = requireNotNull(topic.readMore)
        assertEquals("basecs — computer science fundamentals, explained properly", readMore.label)
        assertEquals("https://medium.com/basecs", readMore.url)
        assertEquals("Vaidehi Joshi · Medium", readMore.source)
    }

    @Test
    fun intuitionIsThreeParagraphs() {
        assertEquals(3, topic.intuition.size)
        assertTrue(topic.intuition[2].contains("\"can she finish at speed k?\""))
    }

    @Test
    fun originIsAbsentForThisTopic() {
        assertNull(topic.origin)
    }

    @Test
    fun complexityRows() {
        assertEquals(3, topic.complexity.size)
        assertEquals(ComplexityRow("Sorting first", "O(n log n)", "O(1)–O(n)", "If you sort only to search once, a linear scan at O(n) is cheaper."), topic.complexity[2])
    }

    @Test
    fun stepsAreOrdered() {
        assertEquals(6, topic.steps.size)
        assertTrue(topic.steps[0].startsWith("Set"))
        assertTrue(topic.steps[5].startsWith("If the loop exits"))
    }

    @Test
    fun codeHasKotlinAndGoOnly() {
        assertEquals(setOf(Lang.KOTLIN, Lang.GO), topic.code.keys)
        assertTrue(topic.code.getValue(Lang.KOTLIN).contains("fun binarySearch"))
        assertTrue(topic.code.getValue(Lang.GO).contains("func BinarySearch"))
    }

    @Test
    fun questionsParseNamedFields() {
        assertEquals(2, topic.questions.size)
        val first = topic.questions[0]
        assertEquals(704, first.id)
        assertEquals("Binary Search", first.title)
        assertEquals(Difficulty.EASY, first.difficulty)
        assertEquals("Warm-up at almost every company", first.askedAt)
    }

    @Test
    fun referencesAlwaysCarryTheTwoBasecsLinks() {
        assertEquals(2, topic.references.size)
        assertTrue(topic.references.any { it.url == "https://medium.com/basecs" })
        assertTrue(topic.references.any { it.url == "https://github.com/vaidehijoshi/basecs-series" })
    }
}
