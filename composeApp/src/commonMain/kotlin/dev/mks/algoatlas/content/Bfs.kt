package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.bfsScene

val Bfs = Topic(
    id = "bfs",
    title = "Breadth-First Search",
    tagline = "Explore in rings of increasing distance — the shortest-path tool for unweighted graphs.",
    level = Level.INTERMEDIATE,
    scene = { bfsScene() },

    quickSummary = listOf(
        "Explores in rings of increasing distance using a FIFO queue — the first time you reach a node, it's via the fewest edges.",
        "Mark visited **on enqueue**, not dequeue, or the same node re-enters the queue and blows up the runtime.",
        "Only gives shortest paths on unweighted graphs; weighted graphs need Dijkstra instead.",
        "Multi-source BFS seeds every source at distance 0, finding nearest-source distance for every node in one pass.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "BFS spreads outward from a source like ripples on water. It finishes every node at distance 1 before touching anything at distance 2, and so on. The queue is what enforces that discipline: first in, first out means nodes leave the queue in the same order they were discovered, which is non-decreasing distance order.",
        "That ordering is the entire correctness argument for shortest paths. The first time you reach a node, you have reached it by the fewest possible edges — no later route can be shorter, because any later route was discovered from a node at least as far away. This is why you mark a node visited **when you enqueue it, not when you dequeue it**; delaying the mark lets the same node enter the queue several times and quietly turns O(V + E) into something much worse.",
        "It only gives shortest paths when every edge costs the same. The moment edges carry different weights, the ripple metaphor breaks — a two-hop cheap path can beat a one-hop expensive one — and you need Dijkstra, which is BFS with a priority queue instead of a plain one.",
        "Most grid problems are BFS problems in disguise. A grid is just a graph where each cell has up to four neighbours, and you never need to build an adjacency list — you compute neighbours on the fly from the coordinates.",
    ),

    keyPoints = listOf(
        "Uses a **FIFO queue**. Swapping it for a stack gives you DFS, and that one-line change is worth internalising.",
        "Mark visited **on enqueue**. Marking on dequeue admits duplicates into the queue and breaks the complexity bound.",
        "Gives **shortest paths only on unweighted graphs** (or where all edges share one weight). Weighted graphs need Dijkstra.",
        "**Multi-source BFS**: seed the queue with every source at distance 0 and the algorithm computes, for each node, the distance to its *nearest* source — in one pass, not one pass per source.",
        "Processing the queue one **level at a time** (snapshot `queue.size` before the inner loop) gives you level-order traversal and lets you count how many rings you have expanded.",
        "To recover the actual path, store a `parent` pointer as you discover each node, then walk it back from the target.",
    ),

    steps = listOf(
        "Put the source in the queue, set its distance to 0, and mark it visited.",
        "While the queue is not empty, remove the node at the front.",
        "For each neighbour of that node, skip it if it is already visited.",
        "Otherwise mark it visited, set its distance to the current node's distance plus one, record its parent, and push it to the back of the queue.",
        "When the queue empties, every reachable node has been assigned its true shortest distance from the source.",
    ),

    complexity = listOf(
        ComplexityRow("Traversal", "O(V + E)", "O(V)", "Each node is enqueued once and each edge inspected once (twice if undirected)."),
        ComplexityRow("Grid of R × C", "O(R · C)", "O(R · C)", "Every cell is a node with at most four edges, so E is proportional to V."),
        ComplexityRow("Worst-case queue", "—", "O(V)", "A star graph puts every neighbour in the queue at once."),
    ),

    pitfalls = listOf(
        "Marking visited on dequeue rather than enqueue — the classic bug. Nodes get queued repeatedly and the run time blows up.",
        "Using a list and calling `removeAt(0)` as the dequeue. That is O(n) per operation and turns the whole traversal quadratic. Use `ArrayDeque`.",
        "Reaching for BFS on a weighted graph. It returns fewest *edges*, not lowest *cost*.",
        "Forgetting bounds checks on grid neighbours, or revisiting the cell you came from.",
        "Losing the level boundary when you need it — capture `queue.size` before the inner loop, because the queue grows while you iterate.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/** Shortest distance in edges from [source] to every reachable node. */
fun bfs(graph: Map<Int, List<Int>>, source: Int): Map<Int, Int> {
    val dist = mutableMapOf(source to 0)
    val parent = mutableMapOf<Int, Int>()
    val queue = ArrayDeque<Int>()
    queue.addLast(source)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()

        for (next in graph[node].orEmpty()) {
            if (next in dist) continue   // visited — mark on enqueue, not dequeue

            dist[next] = dist.getValue(node) + 1
            parent[next] = node
            queue.addLast(next)
        }
    }
    return dist
}

/**
 * Multi-source BFS on a grid: distance from every cell to the nearest source.
 * Seeding all sources at distance 0 costs one pass, not one pass per source.
 */
fun nearestSource(grid: Array<IntArray>, sources: List<Pair<Int, Int>>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val dist = Array(rows) { IntArray(cols) { -1 } }
    val queue = ArrayDeque<Pair<Int, Int>>()

    for ((r, c) in sources) {
        dist[r][c] = 0
        queue.addLast(r to c)
    }

    val moves = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()

        for ((dr, dc) in moves) {
            val nr = r + dr
            val nc = c + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            if (dist[nr][nc] != -1) continue

            dist[nr][nc] = dist[r][c] + 1
            queue.addLast(nr to nc)
        }
    }
    return dist
}
        """.trim(),

        Lang.GO to """
// BFS returns the shortest distance in edges from source to every
// reachable node.
func BFS(graph map[int][]int, source int) map[int]int {
	dist := map[int]int{source: 0}
	parent := map[int]int{}
	queue := []int{source}

	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]

		for _, next := range graph[node] {
			if _, seen := dist[next]; seen {
				continue // mark on enqueue, not dequeue
			}

			dist[next] = dist[node] + 1
			parent[next] = node
			queue = append(queue, next)
		}
	}
	return dist
}

// LevelOrder walks the graph one ring at a time, returning the nodes
// grouped by their distance from the source.
func LevelOrder(graph map[int][]int, source int) [][]int {
	visited := map[int]bool{source: true}
	queue := []int{source}
	levels := [][]int{}

	for len(queue) > 0 {
		size := len(queue) // snapshot: the queue grows while we iterate
		level := make([]int, 0, size)

		for i := 0; i < size; i++ {
			node := queue[0]
			queue = queue[1:]
			level = append(level, node)

			for _, next := range graph[node] {
				if visited[next] {
					continue
				}
				visited[next] = true
				queue = append(queue, next)
			}
		}
		levels = append(levels, level)
	}
	return levels
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Shortest distance in edges from source to every reachable node. */
function bfs(graph, source) {
  const dist = new Map([[source, 0]]);
  const parent = new Map();
  const queue = [source];
  let head = 0; // index cursor — shift() would be O(n) per call

  while (head < queue.length) {
    const node = queue[head++];

    for (const next of graph.get(node) ?? []) {
      if (dist.has(next)) continue; // mark on enqueue, not dequeue

      dist.set(next, dist.get(node) + 1);
      parent.set(next, node);
      queue.push(next);
    }
  }
  return dist;
}

/**
 * Walks the graph one ring at a time, returning nodes grouped by their
 * distance from the source.
 */
function levelOrder(graph, source) {
  const visited = new Set([source]);
  let frontier = [source];
  const levels = [];

  while (frontier.length > 0) {
    levels.push(frontier);
    const next = [];

    for (const node of frontier) {
      for (const neighbour of graph.get(node) ?? []) {
        if (visited.has(neighbour)) continue;
        visited.add(neighbour);
        next.push(neighbour);
      }
    }
    frontier = next;
  }
  return levels;
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 102,
            title = "Binary Tree Level Order Traversal",
            difficulty = Difficulty.MEDIUM,
            idea = "BFS with an explicit level boundary. Snapshot the queue size before the inner loop — that count is exactly one level, because everything added during the loop belongs to the next one.",
            askedAt = "Amazon, Microsoft, LinkedIn",
        ),
        Question(
            id = 994,
            title = "Rotting Oranges",
            difficulty = Difficulty.MEDIUM,
            idea = "Multi-source BFS. Seed the queue with every rotten orange at time 0 and expand in levels; the answer is the number of levels. Running a separate BFS per source is the trap. Remember to check for fresh oranges left over at the end.",
            askedAt = "Amazon — a favourite",
        ),
        Question(
            id = 127,
            title = "Word Ladder",
            difficulty = Difficulty.HARD,
            idea = "The graph is implicit: words are nodes and an edge means \"differs by one letter\". Do not build the adjacency list by comparing all pairs — generate neighbours by replacing each position with every letter and testing membership in the word set. Bidirectional BFS from both ends is the follow-up they want.",
            askedAt = "Google, Amazon, Meta",
        ),
    ),

    related = listOf("dfs", "stacks-queues", "hash-tables", "linked-lists"),
    references = Refs.basecs(),
)
