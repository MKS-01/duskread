---
id: dijkstra
title: Dijkstra's Algorithm
tagline: Always expand the closest unfinished node next, and shortest paths fall out in order.
level: advanced
related: bfs, dags, graph-representation, heaps, kruskal
---

## Note
- **Always finish the closest unfinished node next** — the same core idea as BFS, generalised from hop count to total edge weight.
- **Relaxation**: when finishing a node, check whether reaching each neighbour through it beats that neighbour's current best-known distance, and update it if so.
- **Once a node is finished, its distance is final** — the correctness argument, and it depends entirely on non-negative weights.
- **Requires non-negative edge weights.** A negative edge can let an unfinished path beat an already-finished one, breaking the whole argument — use **Bellman-Ford** instead when negative weights are possible.
- **A min-heap of (distance, node) pairs** makes 'pick the smallest known distance' efficient: O((V + E) log V) instead of an O(V²) linear scan every round.
- **Dijkstra on unit-weight edges behaves identically to BFS** — 'closest by weight' and 'closest by hop count' collapse into the same statement.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
