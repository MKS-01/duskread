package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe: the probability distribution softmax produces
 * over a handful of candidate next tokens at a fixed temperature, with the
 * token sampling actually chose marked out. Not a step-by-step decode.
 */
fun temperatureSamplingScene(): Scene {
    val candidates = listOf("cat", "dog", "mouse", "bird", "fish")
    val probs = listOf("0.42", "0.27", "0.15", "0.10", "0.06")
    val sampled = 1 // "dog" — plausible, not the top candidate

    val frame = SeqFrame(
        values = probs,
        caption =
        "Softmax turns the model's raw scores into a probability over candidate next " +
            "tokens. At this temperature \"cat\" is still favourite, but sampling drew " +
            "\"dog\" — a real possibility, not an error.",
        marks = mapOf(sampled to Tone.GOOD),
        subLabels = candidates.indices.associateWith { candidates[it] },
        aux = listOf(AuxValue("temperature", "0.7")),
    )
    return Scene.Bars(listOf(frame))
}
