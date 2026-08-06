---
id: dfs
title: Depth-First Search
tagline: Commit to a path, and only back up when it runs out.
level: intermediate
related: bfs, stacks-queues, coin-change, dags
---

## Note
- DFS and BFS are **the same algorithm** with a different container: a stack rather than a queue.
- Recursion *is* the stack. The explicit-stack version exists mainly to survive graphs deeper than the call stack.
- **O(V + E)** time — every vertex and every edge is considered once. Space is O(V) for the visited set plus the stack.
- Mark visited **on discovery**, not on completion, or shared nodes get explored more than once.
- It finds **a** path, never reliably the shortest. Unweighted shortest path is BFS's job.
- Cycle detection on a *directed* graph needs three states, not two: unvisited, in-progress, done. Meeting an in-progress node is a cycle; meeting a done one is not.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Recursive: the call stack is doing the work, which is why this is so short. */
fun dfs(
    graph: Map<String, List<String>>,
    start: String,
    visited: MutableSet<String> = mutableSetOf(),
): List<String> {
    // Marked on discovery. Marking on completion lets two paths into the
    // same node before either finishes.
    if (!visited.add(start)) return emptyList()

    val order = mutableListOf(start)
    for (neighbour in graph[start].orEmpty()) {
        order += dfs(graph, neighbour, visited)
    }
    return order
}
```

## Code: Go
```go
// DFS is recursive: the call stack is doing the work, which is why this is
// so short.
func DFS(graph map[string][]string, start string, visited map[string]bool) []string {
	// Marked on discovery. Marking on completion lets two paths into the
	// same node before either finishes.
	if visited[start] {
		return nil
	}
	visited[start] = true

	order := []string{start}
	for _, neighbour := range graph[start] {
		order = append(order, DFS(graph, neighbour, visited)...)
	}
	return order
}
```

## Questions
### Number of Islands
id: 200
difficulty: medium
askedAt: Amazon, Meta, Google — the most asked graph question there is
The grid is a graph in disguise — each cell has up to four neighbours. Scan for an unvisited piece of land, run one DFS to drown the entire island, and increment a counter. The insight is that the number of DFS *launches* is the answer, not anything the traversal itself returns.

### Course Schedule
id: 207
difficulty: medium
askedAt: Amazon, Meta, Uber
"Can all courses be finished" is asking whether the prerequisite graph has a cycle. The trap is using a plain visited set: a node you have already fully explored is not a cycle. You need three states, and only a node still on the current path counts as one.

### Clone Graph
id: 133
difficulty: medium
askedAt: Meta, Bloomberg
The visited set has to become a map from original node to its copy. That map is doing double duty — it prevents infinite recursion on cycles *and* it is how you rewire each clone's neighbours to point at clones rather than originals. Miss the second job and you get a copy that still references the input.

## References
