---
id: kruskal
title: Kruskal's Algorithm
tagline: Sort every edge by weight, and greedily add whichever doesn't create a cycle.
level: advanced
related: union-find, graph-representation, dijkstra
---

## Quick Summary
- Builds a minimum spanning tree by sorting all edges by weight and greedily adding each one, skipping any that would create a cycle — union-find makes the cycle check nearly O(1).
- O(E log E), dominated by the sort — the greedy add-if-no-cycle step itself is nearly linear thanks to union-find.
- A spanning tree connects every node with the fewest possible edges (V − 1); minimum means the total edge weight is as small as possible among all such trees.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A spanning tree of a connected graph is a subset of its edges that connects every node using the fewest possible edges — exactly V − 1 of them, with no cycles. Many different spanning trees usually exist for the same graph; a **minimum spanning tree (MST)** is one where the total weight of the chosen edges is as small as possible among all of them. Kruskal's algorithm builds one with a strikingly simple greedy rule: sort every edge by weight, then walk the sorted list adding each edge unless it would create a cycle.

That greedy rule is not obviously correct, and the reason it works is worth stating precisely: at every point, the cheapest edge that doesn't create a cycle is guaranteed to belong to *some* minimum spanning tree. Adding it can never be a mistake, because if it weren't in the MST, swapping it in for some more expensive edge on the cycle it would otherwise create can only lower the total weight, never raise it. That argument — usually called the "cycle property" — is what justifies never reconsidering an edge once it's been skipped or added.

"Would this edge create a cycle?" is exactly the question union-find answers efficiently: two endpoints already in the same union-find group means a path between them already exists in the edges chosen so far, so connecting them again would close a loop. Skip that edge. Otherwise, take it, and union the two groups. That's the entire algorithm — sort, then one pass with a union-find check per edge — and it's why Kruskal's algorithm is usually taught immediately after union-find rather than as an independent topic.

The complexity is dominated entirely by the sort: O(E log E) for sorting the edges, with the union-find pass itself running in close to O(E) thanks to path compression and union by rank. That makes Kruskal's a good fit for **sparse** graphs specifically — **Prim's algorithm**, which grows a single tree outward from one node using a priority queue, tends to win on dense graphs instead, the same sparse-versus-dense trade-off that shows up between adjacency lists and matrices.

## Origin
**Kruskal's algorithm was published by Joseph Kruskal in 1956**, in a paper titled 'On the Shortest Spanning Subtree of a Graph and the Traveling Salesman Problem' — notably, in the same paper that also discussed the traveling salesman problem. Kruskal developed it independently after learning that a colleague had proved, but not published, the same greedy cycle-avoidance idea; it appeared in the same journal issue as Prim's algorithm, which solves the identical problem via a different, tree-growing approach.

## Key Points
- **Sort every edge by weight, then greedily add each one unless it creates a cycle.** That's the entire algorithm.
- **Union-find answers 'would this create a cycle' in near-O(1)**: if the edge's two endpoints are already in the same group, a path between them already exists, and adding the edge would close a loop.
- **The greedy choice is provably safe** — the cheapest non-cycle-creating edge always belongs to some MST, which is why the algorithm never needs to reconsider a decision once made.
- **O(E log E)**, dominated by sorting the edges — the union-find pass itself is close to linear.
- **Kruskal's wins on sparse graphs; Prim's wins on dense ones** — the same trade-off that separates adjacency lists from adjacency matrices.
- A spanning tree always has exactly **V − 1 edges** for a connected graph with V nodes — more means a cycle exists, fewer means the graph isn't fully connected yet.

## Complexity
Sort edges | O(E log E) | O(E) | Dominates the total runtime.
Union-find pass | O(E α(V)) | O(V) | Effectively O(E) in practice — α(V) is the near-constant inverse Ackermann function.

## Pitfalls
- Forgetting the cycle check entirely and just adding edges in sorted order — without union-find (or an equivalent check), the result isn't a tree at all, just the E cheapest edges.
- Assuming Kruskal's always beats Prim's — the sort-dominated O(E log E) loses to Prim's O(E log V) with a heap on dense graphs, where E approaches V².
- Applying it to a disconnected graph and expecting a single spanning tree — the algorithm correctly produces a minimum spanning *forest* instead, one tree per connected component.
- Reusing a union-find structure across multiple separate MST computations without resetting it — stale group memberships from a previous run silently corrupt the cycle check.

## Steps
1. Sort every edge in the graph by weight, ascending.
2. Initialise a union-find structure with every node in its own group.
3. For each edge in sorted order: if its two endpoints are already in the same group, skip it — it would create a cycle.
4. Otherwise, add the edge to the MST and union the two endpoints' groups.
5. Stop once V − 1 edges have been added, or the sorted list is exhausted.

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
