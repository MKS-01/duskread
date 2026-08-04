---
id: graph-representation
title: Representing Graphs
tagline: Adjacency list or adjacency matrix — the choice decides what's cheap and what's expensive.
level: intermediate
related: bfs, dfs, dijkstra, dags
---

## Quick Summary
- Adjacency list and adjacency matrix trade off exactly opposite things: a list is compact and fast to iterate neighbours, a matrix is fast to check 'is there an edge here?' at the cost of O(V²) space.
- Most real graphs are sparse — E is much smaller than V² — which is why adjacency lists are the default; matrices earn their keep on dense graphs or when edge-existence checks dominate.
- The representation choice changes the complexity of every traversal built on it — BFS and DFS are O(V + E) on a list but O(V²) on a matrix.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A graph is just a set of nodes and the connections between them, but "just" hides a real decision: how do you actually store which nodes connect to which? The two standard answers pull in opposite directions. An **adjacency matrix** is a V×V grid where cell (i, j) says whether an edge exists between i and j — checking for a specific edge is O(1), but the grid costs O(V²) space no matter how few edges actually exist. An **adjacency list** instead gives each node its own list of neighbours — checking for a specific edge means scanning that list, but the total space is O(V + E), proportional to what's actually there.

That distinction matters enormously in practice because most real graphs are **sparse**: a social network with millions of users has nowhere near a million² friendships. Storing a million-node graph as a matrix would need a trillion cells, the overwhelming majority holding "no edge" — a spectacular waste next to an adjacency list's proportional cost. Dense graphs, where E approaches V², are the exception where a matrix's O(1) edge lookups start to pay for themselves.

The representation choice isn't cosmetic — it changes the complexity of every algorithm built on top of it. Visiting every neighbour of every node during a BFS or DFS costs O(V + E) total on an adjacency list, because each node's neighbour list is walked exactly once across the whole traversal. The same traversal on a matrix costs O(V²), because finding a node's neighbours means scanning an entire row of size V, edge or no edge. Same traversal, same graph, different asymptotic cost — purely from the underlying representation.

Weighted graphs extend either representation the same way: a matrix cell holds the edge's weight instead of a boolean, and a list entry pairs each neighbour with its weight. Directed graphs are handled identically by both — a matrix simply loses its symmetry across the diagonal, and a list only records the direction each edge actually points.

## Origin
Representing a network as a grid of connections traces to **Dénes König's 1936 book *Theorie der endlichen und unendlichen Graphen***, the first systematic textbook treatment of graph theory, which formalised the matrix view mathematically. **Adjacency lists have no comparable single-inventor origin** — they are simply the natural computational answer once programmers needed to avoid paying O(V²) space for graphs that were mostly empty, and appear informally throughout early graph-algorithm literature from the 1950s and 60s without a clean point of origin.

## Key Points
- **Adjacency list**: O(V + E) space, O(1) amortised to list a node's neighbours, O(degree) to check a specific edge. The default for sparse graphs — which is most real graphs.
- **Adjacency matrix**: O(V²) space, O(1) to check a specific edge, O(V) to list a node's neighbours (scan the row). Wins when the graph is dense or edge-existence checks dominate.
- **Traversal cost inherits the representation's cost**: BFS/DFS are O(V + E) on a list, O(V²) on a matrix — the same algorithm, different asymptotic bound.
- **Weighted graphs**: a matrix cell holds the weight instead of a boolean; a list entry pairs each neighbour with its weight.
- **Directed graphs**: a matrix loses symmetry across the diagonal; a list simply records only the direction each edge points.

## Complexity
Adjacency list — space | — | O(V + E) | Proportional to what actually exists — the default for sparse graphs.
Adjacency matrix — space | — | O(V²) | Fixed cost regardless of how many edges actually exist.
List a node's neighbours | O(degree) | — | List: direct. Matrix: O(V), scanning the full row regardless of degree.
Check a specific edge | O(degree) | — | List: scan the node's neighbours. Matrix: O(1), direct lookup.

## Pitfalls
- Defaulting to an adjacency matrix out of habit on a sparse graph — the O(V²) space cost is real and often dwarfs what an adjacency list would need.
- Forgetting that a matrix-backed traversal is O(V²), not O(V + E) — the two representations aren't interchangeable without changing the complexity of everything built on top.
- Using an adjacency list but repeatedly checking 'is there an edge from A to B' in a hot loop — that's an O(degree) scan every time; a hash set of neighbours per node fixes it at the cost of extra memory.
- Mismanaging directed vs. undirected edges — an undirected edge needs to be recorded (or checked) in both directions, easy to forget in a hand-rolled adjacency list.

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

/** Adjacency matrix: O(V²) space, O(1) edge lookups regardless of density. */
fun buildAdjacencyMatrix(vertexCount: Int, edges: List<Pair<Int, Int>>): Array<BooleanArray> {
    val matrix = Array(vertexCount) { BooleanArray(vertexCount) }
    for ((u, v) in edges) {
        matrix[u][v] = true
        matrix[v][u] = true // omit this line for a directed graph
    }
    return matrix
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

// BuildAdjacencyMatrix costs O(V^2) space but gives O(1) edge lookups
// regardless of density.
func BuildAdjacencyMatrix(vertexCount int, edges [][2]int) [][]bool {
	matrix := make([][]bool, vertexCount)
	for i := range matrix {
		matrix[i] = make([]bool, vertexCount)
	}
	for _, e := range edges {
		u, v := e[0], e[1]
		matrix[u][v] = true
		matrix[v][u] = true // omit this line for a directed graph
	}
	return matrix
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
