package dev.mks.algoatlas.viz

import dev.mks.algoatlas.model.AuxValue
import dev.mks.algoatlas.model.Scene
import dev.mks.algoatlas.model.SeqFrame
import dev.mks.algoatlas.model.Tone

/**
 * Merge sort as a histogram.
 *
 * The recursion is recorded in the order it actually happens: split all the way
 * down the left spine first, then merge back up. Bars animate their height as
 * values are written back, which is what makes a merge legible.
 */
fun mergeSortScene(input: List<Int> = listOf(38, 27, 43, 10, 9, 82, 3, 55)): Scene {
    val arr = input.toMutableList()
    val frames = mutableListOf<SeqFrame>()
    var writes = 0
    var comparisons = 0

    fun snapshot(
        caption: String,
        marks: Map<Int, Tone> = emptyMap(),
    ) {
        frames += SeqFrame(
            values = arr.map { it.toString() },
            caption = caption,
            marks = marks,
            aux = listOf(
                AuxValue("comparisons", "$comparisons"),
                AuxValue("writes", "$writes"),
            ),
        )
    }

    fun range(from: Int, to: Int, tone: Tone) = (from..to).associateWith { tone }

    snapshot("Merge sort is divide and conquer: split until every piece has one element, then merge sorted pieces back together.")

    fun merge(lo: Int, mid: Int, hi: Int) {
        val left = arr.subList(lo, mid + 1).toList()
        val right = arr.subList(mid + 1, hi + 1).toList()

        snapshot(
            "Both halves are sorted now. Merge [$lo..$mid] with [${mid + 1}..$hi].",
            range(lo, mid, Tone.ACTIVE) + range(mid + 1, hi, Tone.WARN),
        )

        var i = 0
        var j = 0
        var k = lo
        while (i < left.size || j < right.size) {
            val takeLeft = when {
                i >= left.size -> false
                j >= right.size -> true
                else -> {
                    comparisons++
                    left[i] <= right[j]
                }
            }

            val from: String
            if (takeLeft) {
                arr[k] = left[i]
                from = "left"
                i++
            } else {
                arr[k] = right[j]
                from = "right"
                j++
            }
            writes++

            snapshot(
                "Smallest unused element is ${arr[k]} from the $from run — write it to index $k.",
                range(lo, k, Tone.GOOD) +
                    range(k + 1, mid, Tone.ACTIVE) +
                    range(maxOf(mid + 1, k + 1), hi, Tone.WARN) +
                    (k to Tone.INFO),
            )
            k++
        }

        snapshot(
            "[$lo..$hi] is now a single sorted run of ${hi - lo + 1} elements.",
            range(lo, hi, Tone.GOOD),
        )
    }

    fun sort(lo: Int, hi: Int) {
        if (lo >= hi) return
        val mid = lo + (hi - lo) / 2

        snapshot(
            "Split [$lo..$hi] at $mid into two halves and sort each one.",
            range(lo, mid, Tone.ACTIVE) + range(mid + 1, hi, Tone.WARN),
        )

        sort(lo, mid)
        sort(mid + 1, hi)
        merge(lo, mid, hi)
    }

    sort(0, arr.lastIndex)

    snapshot(
        "Sorted. Every element was compared O(log n) times — once per level of the recursion.",
        range(0, arr.lastIndex, Tone.GOOD),
    )

    return Scene.Bars(frames)
}
