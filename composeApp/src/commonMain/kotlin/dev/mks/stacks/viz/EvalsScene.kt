package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe: one suite, one pass rate — an eval run has no
 * meaningful intermediate steps to scrub through, just a row of cases and
 * whether each one passed.
 */
fun evalsScene(): Scene {
    val caseCount = 10
    val failing = setOf(2, 5, 8)
    val values = (1..caseCount).map { "t$it" }

    val frame = SeqFrame(
        values = values,
        caption = "Every box is one fixed test case run through the current model or " +
            "prompt. Green passed, red failed — the pass rate is the only number that " +
            "matters when comparing this run against the last one.",
        marks = values.indices.associateWith { i -> if (i in failing) Tone.BAD else Tone.GOOD },
        aux = listOf(AuxValue("pass rate", "${caseCount - failing.size}/$caseCount")),
    )

    return Scene.Cells(listOf(frame))
}
