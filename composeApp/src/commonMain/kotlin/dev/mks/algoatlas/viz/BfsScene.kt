package dev.mks.algoatlas.viz

import dev.mks.algoatlas.model.AuxValue
import dev.mks.algoatlas.model.GraphFrame
import dev.mks.algoatlas.model.Scene
import dev.mks.algoatlas.model.Tone
import dev.mks.algoatlas.model.VizEdge
import dev.mks.algoatlas.model.VizNode
import dev.mks.algoatlas.model.edgeKey

/**
 * Breadth-first search from a single source.
 *
 * The visual argument the frames need to make: the queue always holds nodes of
 * at most two adjacent levels, so nodes come out in non-decreasing distance
 * order — which is exactly why BFS gives shortest paths on unweighted graphs.
 */
fun bfsScene(): Scene {
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

    val source = "A"
    val frames = mutableListOf<GraphFrame>()

    val state = mutableMapOf<String, Tone>()
    val edgeState = mutableMapOf<String, Tone>()
    val dist = mutableMapOf(source to 0)
    val queue = ArrayDeque(listOf(source))
    val visited = mutableSetOf(source)

    /** Undirected edges are stored under both orientations so lookup is trivial. */
    fun markEdge(a: String, b: String, tone: Tone) {
        edgeState[edgeKey(a, b)] = tone
        edgeState[edgeKey(b, a)] = tone
    }

    fun badges() = dist.mapValues { (_, d) -> "d=$d" }

    fun aux() = listOf(
        AuxValue("queue", if (queue.isEmpty()) "empty" else queue.joinToString(" ")),
        AuxValue("visited", "${visited.size}/${nodes.size}"),
    )

    fun emit(caption: String) {
        frames += GraphFrame(
            caption = caption,
            nodes = state.toMap(),
            edges = edgeState.toMap(),
            badges = badges(),
            aux = aux(),
        )
    }

    state[source] = Tone.INFO
    emit("Start at $source with distance 0 and put it on the queue. Everything else is unseen.")

    val order = mutableListOf<String>()

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        order += current
        state[current] = Tone.ACTIVE
        emit("Dequeue $current (distance ${dist[current]}). Examine every neighbour before going any deeper.")

        var discovered = 0
        for (next in adjacency.getValue(current)) {
            if (next in visited) {
                markEdge(current, next, Tone.BAD)
                emit("$next is already visited, so the edge $current–$next is not a shortest path. Skip it.")
                continue
            }
            visited += next
            discovered++
            dist[next] = dist.getValue(current) + 1
            queue += next
            state[next] = Tone.INFO
            markEdge(current, next, Tone.GOOD)
            emit("$next is new — record distance ${dist[next]}, keep the tree edge $current–$next, and enqueue it.")
        }

        state[current] = Tone.GOOD
        emit(
            if (discovered == 0) "$current has no unvisited neighbours left. It is finished."
            else "$current is finished — it added $discovered node(s) to the queue.",
        )
    }

    frames += GraphFrame(
        caption = "Queue is empty, so every reachable node is settled. Visit order: ${order.joinToString(" → ")}. " +
            "Each d value is the true shortest-path length from $source.",
        nodes = nodes.associate { it.id to Tone.GOOD },
        edges = edgeState.toMap(),
        badges = badges(),
        aux = listOf(AuxValue("queue", "empty"), AuxValue("visited", "${visited.size}/${nodes.size}")),
    )

    return Scene.Graph(nodes = nodes, edges = edges, frames = frames)
}
