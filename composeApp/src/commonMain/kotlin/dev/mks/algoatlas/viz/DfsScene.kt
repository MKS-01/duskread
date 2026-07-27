package dev.mks.algoatlas.viz

import dev.mks.algoatlas.model.AuxValue
import dev.mks.algoatlas.model.GraphFrame
import dev.mks.algoatlas.model.Scene
import dev.mks.algoatlas.model.Tone
import dev.mks.algoatlas.model.VizEdge
import dev.mks.algoatlas.model.VizNode
import dev.mks.algoatlas.model.edgeKey

/**
 * Depth-first search on the same graph [bfsScene] uses.
 *
 * Running both over identical input is the point: the only difference is the
 * container, and the resulting traversal could hardly look more different.
 */
fun dfsScene(): Scene {
    val nodes = listOf(
        VizNode("A", "A", 0.07f, 0.50f),
        VizNode("B", "B", 0.30f, 0.16f),
        VizNode("C", "C", 0.30f, 0.84f),
        VizNode("D", "D", 0.52f, 0.50f),
        VizNode("E", "E", 0.74f, 0.16f),
        VizNode("F", "F", 0.74f, 0.84f),
        VizNode("G", "G", 0.95f, 0.50f),
    )

    val edges = listOf(
        VizEdge("A", "B"), VizEdge("A", "C"),
        VizEdge("B", "D"), VizEdge("C", "D"),
        VizEdge("D", "E"), VizEdge("D", "F"),
        VizEdge("E", "G"), VizEdge("F", "G"),
    )

    val adjacency = buildMap<String, MutableList<String>> {
        nodes.forEach { put(it.id, mutableListOf()) }
        edges.forEach { (from, to, _) ->
            getValue(from) += to
            getValue(to) += from
        }
    }

    val frames = mutableListOf<GraphFrame>()
    val state = mutableMapOf<String, Tone>()
    val edgeState = mutableMapOf<String, Tone>()
    val visited = mutableSetOf<String>()
    val stack = ArrayDeque<String>()
    val order = mutableListOf<String>()
    val depth = mutableMapOf<String, Int>()

    fun markEdge(a: String, b: String, tone: Tone) {
        edgeState[edgeKey(a, b)] = tone
        edgeState[edgeKey(b, a)] = tone
    }

    fun emit(caption: String) {
        frames += GraphFrame(
            caption = caption,
            nodes = state.toMap(),
            edges = edgeState.toMap(),
            badges = depth.mapValues { (_, d) -> "depth $d" },
            aux = listOf(
                AuxValue("stack", if (stack.isEmpty()) "empty" else stack.joinToString(" ")),
                AuxValue("visited", "${visited.size}/${nodes.size}"),
            ),
        )
    }

    // Written iteratively so the stack is visible. The recursive form is the
    // same algorithm with the call stack doing this job implicitly.
    fun explore(node: String, parent: String?, level: Int) {
        visited += node
        depth[node] = level
        stack.addLast(node)
        state[node] = Tone.ACTIVE
        if (parent != null) markEdge(parent, node, Tone.GOOD)

        emit(
            if (parent == null) "Start at $node. DFS commits to one path and follows it as far as it goes."
            else "Step down to $node at depth $level. Rather than look at its siblings, we immediately go deeper.",
        )

        for (next in adjacency.getValue(node)) {
            if (next in visited) {
                if (next != parent) {
                    markEdge(node, next, Tone.BAD)
                    emit("$next has already been seen, so $node–$next closes a cycle. Skip it, or DFS would loop forever.")
                }
                continue
            }
            explore(next, node, level + 1)
        }

        state[node] = Tone.GOOD
        order += node
        stack.removeLast()
        emit("$node has no unexplored neighbours left, so it finishes and we backtrack. Note it finishes *after* its children.")
    }

    explore("A", null, 0)

    frames += GraphFrame(
        caption = "Every node is settled. Nodes finished in the order ${order.joinToString(" → ")} — reverse that and you have a topological sort.",
        nodes = nodes.associate { it.id to Tone.GOOD },
        edges = edgeState.toMap(),
        badges = depth.mapValues { (_, d) -> "depth $d" },
        aux = listOf(AuxValue("stack", "empty"), AuxValue("visited", "${nodes.size}/${nodes.size}")),
    )

    return Scene.Graph(nodes = nodes, edges = edges, frames = frames)
}
