package dev.mks.algoatlas.viz

import dev.mks.algoatlas.model.AuxValue
import dev.mks.algoatlas.model.Pointer
import dev.mks.algoatlas.model.Scene
import dev.mks.algoatlas.model.SeqFrame
import dev.mks.algoatlas.model.Span
import dev.mks.algoatlas.model.Tone

/**
 * Binary search, recorded frame by frame.
 *
 * Discarded halves stay on screen tinted as [Tone.BAD] rather than vanishing —
 * seeing the live range collapse by half is the whole point of the picture.
 */
fun binarySearchScene(
    values: List<Int> = listOf(2, 5, 8, 12, 16, 23, 38, 56, 72, 91),
    target: Int = 23,
): Scene {
    val labels = values.map { it.toString() }
    val frames = mutableListOf<SeqFrame>()
    val dead = mutableMapOf<Int, Tone>()

    var lo = 0
    var hi = values.lastIndex
    var steps = 0

    fun aux() = listOf(
        AuxValue("target", "$target"),
        AuxValue("lo", "$lo"),
        AuxValue("hi", "$hi"),
        AuxValue("comparisons", "$steps"),
    )

    frames += SeqFrame(
        values = labels,
        caption = "Looking for $target. The array is sorted, so the entire range lo..hi is still a candidate.",
        marks = dead.toMap(),
        pointers = listOf(Pointer("lo", lo), Pointer("hi", hi)),
        span = Span(lo, hi, "search space — ${hi - lo + 1} elements"),
        aux = aux(),
    )

    var found = -1
    while (lo <= hi) {
        // `lo + (hi - lo) / 2` rather than `(lo + hi) / 2` — see the pitfalls note.
        val mid = lo + (hi - lo) / 2
        steps++

        frames += SeqFrame(
            values = labels,
            caption = "mid = lo + (hi - lo) / 2 = $mid. Compare ${values[mid]} against $target.",
            marks = dead + (mid to Tone.ACTIVE),
            pointers = listOf(Pointer("lo", lo), Pointer("hi", hi), Pointer("mid", mid, Tone.ACTIVE, below = true)),
            span = Span(lo, hi, "search space — ${hi - lo + 1} elements"),
            aux = aux(),
        )

        when {
            values[mid] == target -> {
                found = mid
                frames += SeqFrame(
                    values = labels,
                    caption = "${values[mid]} == $target. Found it at index $mid after $steps comparisons.",
                    marks = dead + (mid to Tone.GOOD),
                    pointers = listOf(Pointer("found", mid, Tone.GOOD, below = true)),
                    aux = aux(),
                )
                break
            }

            values[mid] < target -> {
                for (i in lo..mid) dead[i] = Tone.BAD
                lo = mid + 1
                frames += SeqFrame(
                    values = labels,
                    caption = "${values[mid]} < $target, so every element at or left of mid is too small. " +
                        "Discard them and set lo = mid + 1.",
                    marks = dead.toMap(),
                    pointers = if (lo <= hi) listOf(Pointer("lo", lo), Pointer("hi", hi)) else emptyList(),
                    span = if (lo <= hi) Span(lo, hi, "search space — ${hi - lo + 1} elements") else null,
                    aux = aux(),
                )
            }

            else -> {
                for (i in mid..hi) dead[i] = Tone.BAD
                hi = mid - 1
                frames += SeqFrame(
                    values = labels,
                    caption = "${values[mid]} > $target, so mid and everything right of it is too large. " +
                        "Discard them and set hi = mid - 1.",
                    marks = dead.toMap(),
                    pointers = if (lo <= hi) listOf(Pointer("lo", lo), Pointer("hi", hi)) else emptyList(),
                    span = if (lo <= hi) Span(lo, hi, "search space — ${hi - lo + 1} elements") else null,
                    aux = aux(),
                )
            }
        }
    }

    if (found < 0) {
        frames += SeqFrame(
            values = labels,
            caption = "lo passed hi, so the range is empty — $target is not in the array. " +
                "lo is now the index where it would be inserted.",
            marks = dead.toMap(),
            aux = aux(),
        )
    }

    return Scene.Cells(frames)
}
