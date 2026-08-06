package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe: a short sentence already split into the
 * subword tokens a BPE tokeniser would produce, id beneath each one. The
 * point is the *shape* of the split — a rare word ("tokenisation") breaks
 * into two pieces while common short words stay whole — not a step-by-step
 * merge process.
 */
fun tokenisationScene(): Scene {
    val tokens = listOf("Token", "isation", " splits", " rare", " words", ".")
    val ids = listOf("5709", "3411", "27414", "4055", "2456", "13")

    val frame = SeqFrame(
        values = tokens,
        caption =
        "\"Token\" and \"isation\" are one word split into two tokens — rare in the " +
            "training corpus, so it never earned a single-token slot. The short common " +
            "words each got their own token instead.",
        marks = mapOf(0 to Tone.WARN, 1 to Tone.WARN),
        subLabels = ids.indices.associateWith { ids[it] },
        aux = listOf(AuxValue("vocabulary size", "~50,000")),
    )
    return Scene.Cells(listOf(frame))
}
