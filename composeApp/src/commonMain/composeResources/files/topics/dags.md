---
id: dags
title: Directed Acyclic Graphs
tagline: No cycles means every dependency can be lined up in one valid order.
level: intermediate
related: bfs, dfs, graph-representation
---

## Quick Summary
- A DAG is a directed graph with no cycles — exactly the condition that guarantees a valid dependency ordering exists.
- Topological sort produces that ordering: Kahn's algorithm repeatedly removes nodes with no remaining incoming edges, in O(V + E).
- If a topological sort can't place every node, the graph has a cycle — this is the standard way to detect one.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Plenty of real problems are really "put these things in a valid order, respecting dependencies": course prerequisites, build steps, spreadsheet formula evaluation, package installation. Model each dependency as a directed edge — "A must happen before B" becomes an edge from A to B — and the question "is there a valid order at all?" becomes a graph question: does this directed graph contain a cycle?

If A depends on B and B depends on A, no order can satisfy both, and the graph has a cycle. A **directed acyclic graph (DAG)** is exactly a directed graph without that problem, and the reason DAGs matter so much is that acyclic is precisely the condition under which a valid dependency order — a **topological sort** — is guaranteed to exist at all.

**Kahn's algorithm** builds that order directly from the dependency structure: track how many unresolved incoming edges (the in-degree) each node has, start with every node whose in-degree is already zero — nothing blocks them — and repeatedly remove one, appending it to the order and decrementing the in-degree of everything it pointed to. Whenever that decrement drops a neighbour's in-degree to zero, it becomes newly available and joins the queue. If the graph is acyclic, every node eventually gets placed.

That "if acyclic" is also a free cycle detector: if the algorithm finishes and fewer than V nodes made it into the order, some group of nodes never reached in-degree zero, which can only happen if they depend on each other in a loop. This is precisely how build systems and compilers detect circular dependencies — running (or failing to complete) a topological sort *is* the cycle check, not a separate step.

Multiple valid topological orders usually exist for the same DAG — anything that respects every edge's direction counts — which is why "topological sort" describes a family of valid answers, not a single canonical one, in contrast to something like sorting numbers where there's exactly one correct output.

## Origin
**Topological sorting via in-degree tracking is known as Kahn's algorithm**, after **Arthur B. Kahn's 1962 paper 'Topological Sorting of Large Networks'** in Communications of the ACM, which addressed the practical problem of ordering large sets of interdependent tasks. The DFS-based alternative — post-order traversal, then reversed — is older in spirit, tracing to standard depth-first search techniques formalised around the same period.

## Key Points
- **A DAG is exactly the class of directed graphs where a valid dependency order exists** — cycles are the only thing that can make no valid order possible.
- **Kahn's algorithm**: track in-degree per node, start from every node already at in-degree zero, and repeatedly remove one, decrementing its neighbours' in-degrees — O(V + E).
- **A topological sort that places fewer than V nodes proves a cycle exists** — this doubles as the standard cycle-detection technique for directed graphs.
- **Multiple valid orderings usually exist.** Any order respecting every edge's direction is correct — there's no single canonical answer the way there is for sorting numbers.
- The DFS-based alternative (post-order, then reverse) works too, and is often simpler to reach for recursively — Kahn's is worth knowing by name because it's also the cycle-detection technique.

## Complexity
Topological sort (Kahn's) | O(V + E) | O(V) | Every node and edge is processed exactly once; extra space is the in-degree array and queue.
Cycle detection | O(V + E) | O(V) | A free byproduct of an incomplete topological sort — no separate pass needed.

## Pitfalls
- Forgetting that a topological order is not unique — comparing your output against a single 'expected' order rather than verifying it respects every edge is a common test-writing mistake.
- Assuming a topological sort always succeeds — on a graph with a cycle it cannot place every node, and that failure is itself the useful signal, not a bug to work around.
- Using DFS-based topological sort without handling disconnected components — every unvisited node needs its own DFS start, or part of the graph gets silently skipped.
- Confusing 'no valid order exists' (a cycle) with 'multiple valid orders exist' (completely normal, expected, and not an error).

## Steps
1. Compute the in-degree of every node — how many edges point into it.
2. Put every node with in-degree zero into a queue.
3. Repeatedly remove a node from the queue, append it to the result, and decrement the in-degree of every node it points to.
4. Whenever a neighbour's in-degree drops to zero, add it to the queue.
5. If the result contains all V nodes, it's a valid topological order; if not, the graph has a cycle.

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
