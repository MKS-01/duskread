package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.GraphFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.Tone
import dev.mks.stacks.model.VizEdge
import dev.mks.stacks.model.VizNode
import dev.mks.stacks.model.edgeKey

/**
 * A single static wireframe of the loop's shape, not a run through it — the
 * point a still frame can make is the cycle itself: three nodes arranged in
 * a triangle with edges that curl back to where they started.
 */
fun planningLoopsScene(): Scene {
    val nodes = listOf(
        VizNode("plan", "Plan", 0.50f, 0.10f),
        VizNode("act", "Act", 0.88f, 0.80f),
        VizNode("observe", "Observe", 0.12f, 0.80f),
    )

    val edges = listOf(
        VizEdge("plan", "act"),
        VizEdge("act", "observe"),
        VizEdge("observe", "plan"),
    )

    val frame = GraphFrame(
        caption = "Plan chooses the next action, Act performs it, Observe reads what " +
            "happened — and that observation is what the next Plan step reasons over, " +
            "not a fresh guess.",
        nodes = mapOf(
            "plan" to Tone.GOOD,
            "act" to Tone.ACTIVE,
            "observe" to Tone.IDLE,
        ),
        edges = mapOf(
            edgeKey("plan", "act") to Tone.GOOD,
            edgeKey("act", "observe") to Tone.ACTIVE,
        ),
        badges = mapOf("act" to "run_tests()"),
        aux = listOf(AuxValue("cycle", "plan → act → observe → replan")),
    )

    return Scene.Graph(nodes = nodes, edges = edges, frames = listOf(frame), directed = true)
}
