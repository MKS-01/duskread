---
id: dijkstra
title: Dijkstra's Algorithm
tagline: Always expand the closest unfinished node next, and shortest paths fall out in order.
level: advanced
related: bfs, dags, graph-representation, heaps, kruskal
---

## Quick Summary
- BFS finds shortest paths by hop count; Dijkstra finds shortest paths by total weight, always expanding whichever unfinished node is currently closest.
- A min-heap of (distance, node) pairs makes 'always expand the closest' cheap — O((V + E) log V) instead of the naive O(V²).
- Only correct with non-negative weights — a negative edge can make a 'finished' node's distance wrong after the fact, which is exactly what Bellman-Ford exists to handle instead.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
BFS finds the shortest path in an unweighted graph by expanding outward in rings, one hop at a time — the first time you reach a node is guaranteed to be via the fewest hops. Dijkstra's algorithm generalises that idea to weighted graphs, where "fewest hops" isn't the same as "least total distance." It keeps the same core idea — always finish with the node currently believed closest before moving on — but replaces "closest by hop count" with "closest by total edge weight so far."

Concretely: track a running best-known distance to every node (infinity until discovered otherwise), and repeatedly pick the unfinished node with the smallest known distance, mark it finished, and use its edges to try to improve its neighbours' distances — a step called **relaxation**. Because the algorithm always finishes the closest remaining node next, once a node is finished its distance can never be improved again: anything that could beat it would have to arrive via a node that isn't finished yet, which by definition is not closer.

That correctness argument is also exactly where the requirement for **non-negative weights** comes from. It depends on "nothing unfinished can be closer than what we just finished" — and a negative edge can violate that outright, letting a path through an unfinished, seemingly-farther node beat a path through one already marked finished and closed off. **Bellman-Ford** exists specifically to handle graphs where that assumption doesn't hold, at the cost of being slower.

Doing "pick the smallest known distance" efficiently is precisely a job for a **min-heap**: push every relaxation as a candidate `(distance, node)` pair, and always pop the smallest. Because a node can be pushed multiple times before it's finished (each relaxation is a fresh candidate), the standard implementation just skips a popped entry if that node has already been finished with a smaller distance — cheaper than removing stale entries from the heap outright. That heap-backed version runs in O((V + E) log V), a direct improvement over the naive O(V²) approach of scanning every unfinished node each round.

Dijkstra's algorithm and BFS being close relatives is not a coincidence worth glossing over: run Dijkstra on a graph where every edge weight is 1, and it behaves identically to BFS, because "closest by weight" and "closest by hop count" become the same statement. That's a genuinely useful way to remember why the two exist and how they relate, rather than as two unrelated algorithms to memorise.

## Origin
**Dijkstra's algorithm was conceived by Edsger W. Dijkstra in 1956**, reportedly in about twenty minutes while he was having coffee with his fiancée in Amsterdam, thinking about the shortest route between two Dutch cities as a demonstration for a new computer. He published it in **1959 as 'A note on two problems in connexion with graphs'** — a two-page paper that also introduced an algorithm for minimum spanning trees. Dijkstra later won the Turing Award in 1972 for foundational contributions to programming as a discipline.

## Key Points
- **Always finish the closest unfinished node next** — the same core idea as BFS, generalised from hop count to total edge weight.
- **Relaxation**: when finishing a node, check whether reaching each neighbour through it beats that neighbour's current best-known distance, and update it if so.
- **Once a node is finished, its distance is final** — the correctness argument, and it depends entirely on non-negative weights.
- **Requires non-negative edge weights.** A negative edge can let an unfinished path beat an already-finished one, breaking the whole argument — use **Bellman-Ford** instead when negative weights are possible.
- **A min-heap of (distance, node) pairs** makes 'pick the smallest known distance' efficient: O((V + E) log V) instead of an O(V²) linear scan every round.
- **Dijkstra on unit-weight edges behaves identically to BFS** — 'closest by weight' and 'closest by hop count' collapse into the same statement.

## Complexity
Heap-backed | O((V + E) log V) | O(V) | Standard implementation: a min-heap of (distance, node) candidates, lazily skipping stale entries.
Naive (no heap) | O(V²) | O(V) | Scans every unfinished node each round to find the minimum — fine for dense graphs, wasteful for sparse ones.

## Pitfalls
- Running it on a graph with negative edge weights — the correctness argument breaks silently, producing a wrong answer with no error raised. Use Bellman-Ford instead.
- Removing stale entries from the heap instead of just checking-and-skipping on pop — it's simpler and no more expensive to let stale entries sit in the heap and discard them lazily.
- Re-relaxing a node's neighbours after it's already finished — once finished, a node's distance is final, and revisiting it wastes work without changing correctness.
- Reaching for Dijkstra when the graph is unweighted — plain BFS is simpler and does the identical job for that special case.
- Forgetting to track the actual path, not just the distance, when the problem asks for the path itself — that needs a parent pointer updated alongside every relaxation.

## Steps
1. Set every node's distance to infinity except the source, which is zero.
2. Push the source onto a min-heap as (0, source).
3. Repeatedly pop the smallest (distance, node) pair. If that node is already finished with a smaller distance, skip it.
4. Otherwise, mark it finished, and relax every edge out of it: if going through this node beats a neighbour's current best distance, update it and push the improved (distance, neighbour) pair.
5. Stop when the heap is empty, or as soon as the target node is popped, if only one destination matters.

## Code: Kotlin
```kotlin
data class Edge(val to: Int, val weight: Int)

/** Returns the shortest distance from source to every node, or Int.MAX_VALUE if unreachable. */
fun dijkstra(vertexCount: Int, adj: List<List<Edge>>, source: Int): IntArray {
    val distances = IntArray(vertexCount) { Int.MAX_VALUE }
    distances[source] = 0

    val heap = java.util.PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
    heap.add(0 to source) // (distance, node)
    val finished = BooleanArray(vertexCount)

    while (heap.isNotEmpty()) {
        val (dist, node) = heap.poll()
        if (finished[node]) continue // a stale, already-beaten entry
        finished[node] = true

        for (edge in adj[node]) {
            val candidate = dist + edge.weight
            if (candidate < distances[edge.to]) {
                distances[edge.to] = candidate
                heap.add(candidate to edge.to)
            }
        }
    }
    return distances
}
```

## Code: Go
```go
type Edge struct {
	To     int
	Weight int
}

type heapItem struct{ dist, node int }
type minHeap []heapItem

func (h minHeap) Len() int           { return len(h) }
func (h minHeap) Less(i, j int) bool { return h[i].dist < h[j].dist }
func (h minHeap) Swap(i, j int)      { h[i], h[j] = h[j], h[i] }
func (h *minHeap) Push(x any)        { *h = append(*h, x.(heapItem)) }
func (h *minHeap) Pop() any {
	old := *h
	item := old[len(old)-1]
	*h = old[:len(old)-1]
	return item
}

// Dijkstra returns the shortest distance from source to every node, using
// math.MaxInt to mean "unreachable".
func Dijkstra(vertexCount int, adj [][]Edge, source int) []int {
	distances := make([]int, vertexCount)
	for i := range distances {
		distances[i] = math.MaxInt
	}
	distances[source] = 0

	h := &minHeap{{0, source}}
	finished := make([]bool, vertexCount)

	for h.Len() > 0 {
		item := heap.Pop(h).(heapItem)
		if finished[item.node] {
			continue // a stale, already-beaten entry
		}
		finished[item.node] = true

		for _, edge := range adj[item.node] {
			candidate := item.dist + edge.Weight
			if candidate < distances[edge.To] {
				distances[edge.To] = candidate
				heap.Push(h, heapItem{candidate, edge.To})
			}
		}
	}
	return distances
}
```

## Questions
### Network Delay Time
id: 743
difficulty: medium
askedAt: Google, Amazon
The direct application: shortest weighted path from one source to every node, then the answer is the maximum of those distances — the time for the signal to reach everyone.

### Cheapest Flights Within K Stops
id: 787
difficulty: medium
askedAt: Amazon, Meta — a favourite for testing the limits of Dijkstra
Plain Dijkstra's 'once finished, never revisit' breaks once a hop limit is added, because a longer-but-fewer-hops path can be the only valid one. Bellman-Ford-style relaxation, bounded to k+1 rounds, handles the constraint more naturally than patching Dijkstra.

### Path with Maximum Probability
id: 1514
difficulty: medium
askedAt: Google
The same algorithm with the comparison flipped: a max-heap instead of a min-heap, and multiplying edge probabilities instead of summing weights. Shows Dijkstra generalises to any relaxation rule that only ever improves monotonically, not just addition.

## References
