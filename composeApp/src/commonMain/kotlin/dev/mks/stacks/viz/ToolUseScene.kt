package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.GraphFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.Tone
import dev.mks.stacks.model.VizEdge
import dev.mks.stacks.model.VizNode
import dev.mks.stacks.model.edgeKey

/**
 * A single static wireframe, not a run — tool use has no steps to play back,
 * only a shape: one agent, a menu of tools it can call, and a call/result
 * pair drawn as two directed edges so the round trip reads at a glance.
 */
fun toolUseScene(): Scene {
    val nodes = listOf(
        VizNode("agent", "Agent", 0.50f, 0.50f),
        VizNode("search", "search", 0.15f, 0.15f),
        VizNode("run_tests", "run_tests", 0.85f, 0.15f),
        VizNode("read_file", "read_file", 0.50f, 0.90f),
    )

    val edges = listOf(
        VizEdge("agent", "search"), VizEdge("search", "agent"),
        VizEdge("agent", "run_tests"), VizEdge("run_tests", "agent"),
        VizEdge("agent", "read_file"), VizEdge("read_file", "agent"),
    )

    val frame = GraphFrame(
        caption = "The agent picks a tool and calls it with arguments; the tool runs " +
            "outside the model and its result comes back as text, appended to what the " +
            "model reads next.",
        nodes = mapOf(
            "agent" to Tone.ACTIVE,
            "search" to Tone.INFO,
            "run_tests" to Tone.IDLE,
            "read_file" to Tone.IDLE,
        ),
        edges = mapOf(
            edgeKey("agent", "search") to Tone.ACTIVE,
            edgeKey("search", "agent") to Tone.GOOD,
        ),
        badges = mapOf("search" to "\"capital of France\" → \"Paris\""),
        aux = listOf(AuxValue("active call", "search")),
    )

    return Scene.Graph(nodes = nodes, edges = edges, frames = listOf(frame), directed = true)
}
