package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.MatrixFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.Tone

/**
 * A single static attention-weight grid for a four-token sentence — not a
 * step-by-step computation of it. The point is entirely in the grid: "it"
 * attends mostly to "animal", which is the one cell marked out.
 */
fun attentionScene(): Scene {
    val tokens = listOf("The", "animal", "was", "tired")

    // Row i = query token i's attention weights over every key token.
    // "it" (played here by "The") would attend to "animal" in the classic
    // coreference example; the row for "was" is kept close to uniform to
    // show what a token with a weak preference looks like by contrast.
    val weights = listOf(
        listOf("0.10", "0.62", "0.15", "0.13"),
        listOf("0.05", "0.70", "0.10", "0.15"),
        listOf("0.22", "0.28", "0.26", "0.24"),
        listOf("0.08", "0.58", "0.12", "0.22"),
    )

    val frame = MatrixFrame(
        caption =
        "Row = query token, column = key token; each cell is that pair's " +
            "attention weight after softmax. \"animal\" dominates the row for " +
            "\"tired\" — that's the model resolving what \"tired\" really refers to.",
        grid = weights,
        rowLabels = tokens,
        colLabels = tokens,
        marks = mapOf("3,1" to Tone.WARN),
        aux = listOf(
            AuxValue("weights", "softmax(QKᵀ / √d_k)"),
        ),
    )

    return Scene.Matrix(listOf(frame))
}
