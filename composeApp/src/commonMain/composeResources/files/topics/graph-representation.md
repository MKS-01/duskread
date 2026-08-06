---
id: graph-representation
title: Representing Graphs
tagline: Adjacency list or adjacency matrix — the choice decides what's cheap and what's expensive.
level: intermediate
related: bfs, dfs, dijkstra, dags
---

## Note
- **Adjacency list**: O(V + E) space, O(1) amortised to list a node's neighbours, O(degree) to check a specific edge. The default for sparse graphs — which is most real graphs.
- **Adjacency matrix**: O(V²) space, O(1) to check a specific edge, O(V) to list a node's neighbours (scan the row). Wins when the graph is dense or edge-existence checks dominate.
- **Traversal cost inherits the representation's cost**: BFS/DFS are O(V + E) on a list, O(V²) on a matrix — the same algorithm, different asymptotic bound.
- **Weighted graphs**: a matrix cell holds the weight instead of a boolean; a list entry pairs each neighbour with its weight.
- **Directed graphs**: a matrix loses symmetry across the diagonal; a list simply records only the direction each edge points.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Adjacency list: the default for sparse graphs. O(V + E) space. */
fun buildAdjacencyList(vertexCount: Int, edges: List<Pair<Int, Int>>): List<MutableList<Int>> {
    val adj = List(vertexCount) { mutableListOf<Int>() }
    for ((u, v) in edges) {
        adj[u].add(v)
        adj[v].add(u) // omit this line for a directed graph
    }
    return adj
}
```

## Code: Go
```go
// BuildAdjacencyList is the default for sparse graphs. O(V + E) space.
func BuildAdjacencyList(vertexCount int, edges [][2]int) [][]int {
	adj := make([][]int, vertexCount)
	for _, e := range edges {
		u, v := e[0], e[1]
		adj[u] = append(adj[u], v)
		adj[v] = append(adj[v], u) // omit this line for a directed graph
	}
	return adj
}
```

## Questions
### Find the Town Judge
id: 997
difficulty: easy
askedAt: Amazon, Facebook
No need to build a full graph structure — track in-degree and out-degree per person with two count arrays. The judge is the one person with in-degree n-1 and out-degree 0, found in one pass over the edges.

### Number of Provinces
id: 547
difficulty: medium
askedAt: Amazon, Bloomberg
The input is literally an adjacency matrix — a direct test of reading that representation. DFS or union-find over it counts connected components; the only real trap is treating the matrix as an edge list instead of indexing into it.

### Clone Graph
id: 133
difficulty: medium
askedAt: Amazon, Meta, Microsoft
Building a new adjacency-list representation while traversing the old one — a hash map from original node to clone is what prevents infinite loops on cycles and ensures each node is cloned exactly once.

## References
