package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Span
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe: a longer token sequence with a [Span] marking
 * the trailing slice still inside the context window. Tokens to the left of
 * the span are tinted [Tone.BAD] — not "faded" or "summarised", just gone,
 * which is the one thing this picture needs to make visceral.
 */
fun contextWindowsScene(): Scene {
    val tokens = (1..14).map { "T$it" }
    val windowStart = 8 // last 7 of 14 tokens remain inside the window

    val frame = SeqFrame(
        values = tokens,
        caption =
        "The window only holds the last 7 of 14 tokens. Everything left of the " +
            "highlighted span isn't summarised or fuzzy — it simply isn't part of what " +
            "the model attends to on this call.",
        marks = (0 until windowStart - 1).associateWith { Tone.BAD },
        span = Span(windowStart - 1, tokens.lastIndex, "in context — 7 tokens", Tone.INFO),
        aux = listOf(AuxValue("window size", "7 tokens")),
    )
    return Scene.Cells(listOf(frame))
}
