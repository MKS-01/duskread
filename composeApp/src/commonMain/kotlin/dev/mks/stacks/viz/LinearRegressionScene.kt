package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * A single static wireframe: the actual y-values of a small noisy dataset,
 * next to the values a fitted line predicts at the same x's. The two rows
 * side by side are the whole idea of "least squares" — minimise the total
 * gap between them — without animating the fit that produces it.
 */
fun linearRegressionScene(
    xs: List<Int> = listOf(1, 2, 3, 4, 5),
    ys: List<Int> = listOf(3, 6, 9, 12, 15),
    slope: Double = 2.99,
): Scene {
    val predicted = xs.map { (it * slope).toInt() }
    val marks = xs.indices.associateWith { i ->
        if (ys[i] == predicted[i]) Tone.GOOD else Tone.WARN
    }

    val frame = SeqFrame(
        values = ys.map { it.toString() },
        caption =
        "Actual y at each x. The fitted line y = $slope·x predicts $predicted at the " +
            "same x's — least squares picks the slope that makes the total squared gap " +
            "between actual and predicted as small as possible.",
        marks = marks,
        subLabels = xs.indices.associateWith { "x=${xs[it]}" },
        aux = listOf(AuxValue("fitted slope w", slope.toString())),
    )
    return Scene.Bars(listOf(frame))
}
