package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.GraphFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.Tone
import dev.mks.stacks.model.VizEdge
import dev.mks.stacks.model.VizNode
import dev.mks.stacks.model.edgeKey

/**
 * A single static wireframe of a 2-2-1 network, not a training run. Forward
 * edges (INFO) carry activations left to right; the same edges, badged with a
 * gradient magnitude, are what backpropagation walks right to left — the
 * point of the diagram is that it is the *same graph*, read in reverse.
 */
fun backpropagationScene(): Scene {
    val nodes = listOf(
        VizNode("I1", "x₁", 0.05f, 0.25f),
        VizNode("I2", "x₂", 0.05f, 0.75f),
        VizNode("H1", "h₁", 0.45f, 0.20f),
        VizNode("H2", "h₂", 0.45f, 0.80f),
        VizNode("O", "ŷ", 0.90f, 0.50f),
    )

    val edges = listOf(
        VizEdge("I1", "H1"), VizEdge("I1", "H2"),
        VizEdge("I2", "H1"), VizEdge("I2", "H2"),
        VizEdge("H1", "O"), VizEdge("H2", "O"),
    )

    val frame = GraphFrame(
        caption =
        "Forward, each edge carries an activation toward ŷ. Backward, the " +
            "same edges carry ∂loss/∂weight — the chain rule applied layer by " +
            "layer — telling every weight how much it contributed to the error.",
        nodes = mapOf(
            "I1" to Tone.INFO, "I2" to Tone.INFO,
            "H1" to Tone.INFO, "H2" to Tone.INFO,
            "O" to Tone.WARN,
        ),
        edges = edges.associate { (from, to, _) -> edgeKey(from, to) to Tone.INFO },
        badges = mapOf(
            "H1" to "∂L=0.31", "H2" to "∂L=0.12",
            "I1" to "∂L=0.09", "I2" to "∂L=0.04",
            "O" to "loss",
        ),
        aux = listOf(
            AuxValue("rule", "∂L/∂w = ∂L/∂ŷ · ∂ŷ/∂h · ∂h/∂w"),
        ),
    )

    return Scene.Graph(nodes = nodes, edges = edges, frames = listOf(frame), directed = true)
}
