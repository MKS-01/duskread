---
id: bfs
title: Breadth-First Search
tagline: Explore in rings of increasing distance — the shortest-path tool for unweighted graphs.
level: intermediate
related: dfs, stacks-queues, hash-tables, linked-lists
---

## Note
- Uses a **FIFO queue**. Swapping it for a stack gives you DFS, and that one-line change is worth internalising.
- Mark visited **on enqueue**, not dequeue — marking on dequeue admits duplicates into the queue and breaks the complexity bound.
- Gives **shortest paths only on unweighted graphs** (or where all edges share one weight). Weighted graphs need Dijkstra.
- **Multi-source BFS**: seed the queue with every source at distance 0 and the algorithm computes, for each node, the distance to its *nearest* source — in one pass, not one pass per source.
- Processing the queue one **level at a time** (snapshot `queue.size` before the inner loop) gives you level-order traversal.
- To recover the actual path, store a `parent` pointer as you discover each node, then walk it back from the target.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Shortest distance in edges from [source] to every reachable node. */
fun bfs(graph: Map<Int, List<Int>>, source: Int): Map<Int, Int> {
    val dist = mutableMapOf(source to 0)
    val queue = ArrayDeque<Int>()
    queue.addLast(source)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node].orEmpty()) {
            if (next in dist) continue // visited — mark on enqueue, not dequeue
            dist[next] = dist.getValue(node) + 1
            queue.addLast(next)
        }
    }
    return dist
}
```

## Code: Go
```go
// BFS returns the shortest distance in edges from source to every
// reachable node.
func BFS(graph map[int][]int, source int) map[int]int {
	dist := map[int]int{source: 0}
	queue := []int{source}

	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]

		for _, next := range graph[node] {
			if _, seen := dist[next]; seen {
				continue // mark on enqueue, not dequeue
			}
			dist[next] = dist[node] + 1
			queue = append(queue, next)
		}
	}
	return dist
}
```

## Questions
### Binary Tree Level Order Traversal
id: 102
difficulty: medium
askedAt: Amazon, Microsoft, LinkedIn
BFS with an explicit level boundary. Snapshot the queue size before the inner loop — that count is exactly one level, because everything added during the loop belongs to the next one.

### Rotting Oranges
id: 994
difficulty: medium
askedAt: Amazon — a favourite
Multi-source BFS. Seed the queue with every rotten orange at time 0 and expand in levels; the answer is the number of levels. Running a separate BFS per source is the trap. Remember to check for fresh oranges left over at the end.

### Word Ladder
id: 127
difficulty: hard
askedAt: Google, Amazon, Meta
The graph is implicit: words are nodes and an edge means "differs by one letter". Do not build the adjacency list by comparing all pairs — generate neighbours by replacing each position with every letter and testing membership in the word set. Bidirectional BFS from both ends is the follow-up they want.

## References
