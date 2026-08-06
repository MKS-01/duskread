---
id: dags
title: Directed Acyclic Graphs
tagline: No cycles means every dependency can be lined up in one valid order.
level: intermediate
related: bfs, dfs, graph-representation
---

## Note
- **A DAG is exactly the class of directed graphs where a valid dependency order exists** — cycles are the only thing that can make no valid order possible.
- **Kahn's algorithm**: track in-degree per node, start from every node already at in-degree zero, and repeatedly remove one, decrementing its neighbours' in-degrees — O(V + E).
- **A topological sort that places fewer than V nodes proves a cycle exists** — this doubles as the standard cycle-detection technique for directed graphs.
- **Multiple valid orderings usually exist.** Any order respecting every edge's direction is correct — there's no single canonical answer the way there is for sorting numbers.
- The DFS-based alternative (post-order, then reverse) works too, and is often simpler to reach for recursively — Kahn's is worth knowing by name because it's also the cycle-detection technique.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Kahn's algorithm. Returns null if the graph has a cycle. */
fun topologicalSort(vertexCount: Int, edges: List<Pair<Int, Int>>): List<Int>? {
    val adj = List(vertexCount) { mutableListOf<Int>() }
    val inDegree = IntArray(vertexCount)
    for ((from, to) in edges) {
        adj[from].add(to)
        inDegree[to]++
    }

    val queue = ArrayDeque((0 until vertexCount).filter { inDegree[it] == 0 })
    val order = mutableListOf<Int>()
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        order += node
        for (next in adj[node]) {
            inDegree[next]--
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }
    // Fewer nodes than V made it in: some group never reached in-degree
    // zero, which only happens if they depend on each other in a cycle.
    return order.takeIf { it.size == vertexCount }
}
```

## Code: Go
```go
// TopologicalSort runs Kahn's algorithm. Returns nil if the graph has a cycle.
func TopologicalSort(vertexCount int, edges [][2]int) []int {
	adj := make([][]int, vertexCount)
	inDegree := make([]int, vertexCount)
	for _, e := range edges {
		from, to := e[0], e[1]
		adj[from] = append(adj[from], to)
		inDegree[to]++
	}

	queue := []int{}
	for i := 0; i < vertexCount; i++ {
		if inDegree[i] == 0 {
			queue = append(queue, i)
		}
	}

	order := []int{}
	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]
		order = append(order, node)
		for _, next := range adj[node] {
			inDegree[next]--
			if inDegree[next] == 0 {
				queue = append(queue, next)
			}
		}
	}

	// Fewer nodes than vertexCount made it in: some group never reached
	// in-degree zero, which only happens if they depend on each other in
	// a cycle.
	if len(order) != vertexCount {
		return nil
	}
	return order
}
```

## Questions
### Course Schedule
id: 207
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Direct cycle detection: model prerequisites as directed edges and run Kahn's algorithm. If it can't place every course, a prerequisite cycle exists and no valid schedule is possible.

### Course Schedule II
id: 210
difficulty: medium
askedAt: Amazon, Meta
Same graph, but return the actual order rather than just yes/no — Kahn's algorithm already produces it as a side effect of the cycle check, no extra work needed.

### Alien Dictionary
id: 269
difficulty: hard
askedAt: Meta, Airbnb, Google — a classic (if often locked) topological-sort question
The graph isn't given — it has to be inferred: compare adjacent words to extract letter-ordering constraints, build edges between letters, then topologically sort the alphabet itself. The real difficulty is the graph construction, not the sort.

## References
