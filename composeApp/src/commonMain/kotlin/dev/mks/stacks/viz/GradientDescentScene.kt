package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Pointer
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe of the loss bowl f(w) = (w - target)^2 — not a
 * step-by-step descent. The bars are the fixed shape of the curve; the two
 * pointers are the whole idea: start somewhere on the slope, walk toward
 * where it flattens.
 */
fun gradientDescentScene(
    target: Int = 7,
    range: Int = 14,
    start: Int = 1,
): Scene {
    fun loss(w: Int) = (w - target) * (w - target)
    val landscape = (0..range).map { loss(it).toString() }

    val frame = SeqFrame(
        values = landscape,
        caption =
        "Every bar is the loss f(w) = (w − $target)² at that w. Gradient descent " +
            "starts anywhere on the slope and repeatedly steps against the gradient — " +
            "downhill — until it settles at the bottom.",
        marks = mapOf(start to Tone.WARN, target to Tone.GOOD),
        pointers = listOf(
            Pointer("start", start, Tone.WARN),
            Pointer("minimum", target, Tone.GOOD),
        ),
        aux = listOf(AuxValue("update rule", "w := w − η · ∇f(w)")),
    )
    return Scene.Bars(listOf(frame))
}
