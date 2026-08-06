---
id: kruskal
title: Kruskal's Algorithm
tagline: Sort every edge by weight, and greedily add whichever doesn't create a cycle.
level: advanced
related: union-find, graph-representation, dijkstra
---

## Note
- **Sort every edge by weight, then greedily add each one unless it creates a cycle.** That's the entire algorithm.
- **Union-find answers 'would this create a cycle' in near-O(1)**: if the edge's two endpoints are already in the same group, a path between them already exists, and adding the edge would close a loop.
- **The greedy choice is provably safe** — the cheapest non-cycle-creating edge always belongs to some MST, which is why the algorithm never needs to reconsider a decision once made.
- **O(E log E)**, dominated by sorting the edges — the union-find pass itself is close to linear.
- **Kruskal's wins on sparse graphs; Prim's wins on dense ones** — the same trade-off that separates adjacency lists from adjacency matrices.
- A spanning tree always has exactly **V − 1 edges** for a connected graph with V nodes — more means a cycle exists, fewer means the graph isn't fully connected yet.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
data class WeightedEdge(val u: Int, val v: Int, val weight: Int)

/** Returns the MST's edges. Assumes the graph is connected. */
fun kruskal(vertexCount: Int, edges: List<WeightedEdge>): List<WeightedEdge> {
    val sorted = edges.sortedBy { it.weight }
    val uf = UnionFind(vertexCount)
    val mst = mutableListOf<WeightedEdge>()

    for (edge in sorted) {
        // union() returns false if u and v were already connected —
        // adding this edge would close a cycle, so skip it.
        if (uf.union(edge.u, edge.v)) {
            mst += edge
            if (mst.size == vertexCount - 1) break
        }
    }
    return mst
}
```

## Code: Go
```go
type WeightedEdge struct {
	U, V, Weight int
}

// Kruskal returns the MST's edges. Assumes the graph is connected.
func Kruskal(vertexCount int, edges []WeightedEdge) []WeightedEdge {
	sorted := append([]WeightedEdge(nil), edges...)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i].Weight < sorted[j].Weight })

	uf := NewUnionFind(vertexCount)
	var mst []WeightedEdge

	for _, edge := range sorted {
		// Union returns false if U and V were already connected — adding
		// this edge would close a cycle, so skip it.
		if uf.Union(edge.U, edge.V) {
			mst = append(mst, edge)
			if len(mst) == vertexCount-1 {
				break
			}
		}
	}
	return mst
}
```

## Questions
### Min Cost to Connect All Points
id: 1584
difficulty: medium
askedAt: Google, Amazon
Build a complete graph of Manhattan distances between every pair of points, then run Kruskal's (or Prim's) directly. With n points there are O(n²) edges, which is exactly why Prim's O(E log V) sometimes edges out Kruskal's here on larger inputs.

### Redundant Connection
id: 684
difficulty: medium
askedAt: Amazon, Google
Viewed through the MST lens: the redundant edge is exactly the one Kruskal's algorithm would skip — the first edge, in input order, whose two endpoints are already connected by edges processed so far.

## References
