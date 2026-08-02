package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.dfsScene

val Dfs = Topic(
    id = "dfs",
    title = "Depth-First Search",
    tagline = "Commit to a path, and only back up when it runs out.",
    level = Level.INTERMEDIATE,
    scene = { dfsScene() },

    quickSummary = listOf(
        "Same traversal as BFS with a stack instead of a queue — commit to a path, back up only when it runs out.",
        "Recursion *is* the stack — that's why the recursive version is only a few lines long.",
        "Finishing a branch before moving on is what powers cycle detection, topological sort and connected components.",
        "Finds *a* path, never reliably the shortest one — that's still BFS's job.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Breadth-first search explores cautiously, finishing everything one step away before looking two steps away. Depth-first search does the opposite: it picks a direction and *commits*, following it as far as it goes, and only when that path is exhausted does it retreat to the last junction and try the next option. It is the strategy of someone walking a maze with one hand on the wall.",
        "The only structural difference from BFS is the container holding the frontier. BFS uses a queue, so the oldest discovery comes back first. DFS uses a **stack**, so the newest one does. Swap those two and the traversal order changes completely with no other edit — which is the clearest illustration of why data structures matter that this curriculum has.",
        "Most of the time you never declare that stack, because recursion already provides one. Calling `dfs(neighbour)` pushes a frame; returning pops it. That makes DFS extraordinarily short to write — often four or five lines — and it is why the recursive version is the one people reach for. It is also why DFS blows up on deep graphs in a way BFS does not: the stack is real, and it is finite.",
        "What DFS is *good* at is a specific class of questions. Because it fully finishes a branch before moving on, it knows when a subtree is complete — and that is exactly what you need for **cycle detection**, **topological ordering**, **connected components**, and anything where an answer for a node depends on answers for everything below it. BFS cannot easily tell you that, because it never finishes a branch before starting others.",
        "The single most important detail is the **visited set**. Without one, any cycle turns DFS into an infinite loop, and even in an acyclic graph you will revisit shared nodes exponentially often. Mark a node when you first reach it, not when you finish it, or two paths arriving at the same node will both descend into it.",
        "One thing DFS does **not** give you is shortest paths. It finds *a* path readily, but the first one it stumbles onto is whichever direction it happened to try first — which can be arbitrarily worse than the best. If the question says shortest and the edges are unweighted, you want BFS.",
    ),

    origin = "The method is older than computing. **Charles Pierre Trémaux**, a French engineer, described a systematic way to walk a maze in the 19th century — mark each passage as you enter it, never take a marked passage twice, and retreat when you run out of new ones. It was written up by **Édouard Lucas in *Récréations Mathématiques* (1882)**, and it is depth-first search with a visited set, several decades before there was a machine to run it on. Its modern importance came from **John Hopcroft and Robert Tarjan in the early 1970s**, who showed that DFS was not merely one traversal among several but the engine behind efficient algorithms for biconnectivity, strongly connected components and planarity testing. The two shared the **1986 Turing Award**, with this work among the cited contributions.",

    keyPoints = listOf(
        "DFS and BFS are **the same algorithm** with a different container: a stack rather than a queue.",
        "Recursion *is* the stack. The explicit-stack version exists mainly to survive graphs deeper than the call stack.",
        "**O(V + E)** time — every vertex and every edge is considered once. Space is O(V) for the visited set plus the stack.",
        "Mark visited **on discovery**, not on completion, or shared nodes get explored more than once.",
        "It finds **a** path, never reliably the shortest. Unweighted shortest path is BFS's job.",
        "Finishing a branch before moving on is what enables **topological sort**, **cycle detection** and **components**.",
        "Cycle detection on a *directed* graph needs three states, not two: unvisited, in-progress, done. Meeting an in-progress node is a cycle; meeting a done one is not.",
    ),

    complexity = listOf(
        ComplexityRow("Traversal", "O(V + E)", "O(V)", "Each vertex is visited once and each edge examined once, from each end for an undirected graph."),
        ComplexityRow("Recursive space", "—", "O(V)", "Worst case the call stack holds every vertex — a path graph, or a long chain."),
        ComplexityRow("Cycle detection", "O(V + E)", "O(V)", "Same traversal; the three-state colouring is what reads the answer off it."),
        ComplexityRow("Topological sort", "O(V + E)", "O(V)", "Push each vertex when it *finishes*, then reverse the resulting order."),
        ComplexityRow("Path finding", "O(V + E)", "O(V)", "Finds a path quickly. Says nothing about it being short."),
    ),

    pitfalls = listOf(
        "Leaving out the visited set. On any graph with a cycle this never terminates; on a DAG with shared nodes it silently does exponential work.",
        "Marking visited when a node *finishes* rather than when it is discovered. Two paths reaching the same node then both descend into it.",
        "Using DFS for shortest path. It returns the first path it happens to find, which can be arbitrarily longer than the best one.",
        "Recursing on a graph deep enough to overflow the call stack. A linked-list-shaped graph of 100,000 nodes will do it — rewrite with an explicit stack.",
        "Using two states for cycle detection in a *directed* graph. A node you have already fully finished is not a cycle; only one still on the current path is.",
        "Pushing a node's neighbours and expecting the visit order to match the recursive version. A stack reverses them, so the iterative form explores the last neighbour first unless you push in reverse.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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

/** Iterative: identical, but survives graphs deeper than the call stack. */
fun dfsIterative(graph: Map<String, List<String>>, start: String): List<String> {
    val visited = mutableSetOf<String>()
    val order = mutableListOf<String>()
    val stack = ArrayDeque<String>()
    stack.addLast(start)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (!visited.add(node)) continue
        order += node

        // Reversed, so the first neighbour is on top and the visit order
        // matches the recursive version.
        for (neighbour in graph[node].orEmpty().reversed()) {
            if (neighbour !in visited) stack.addLast(neighbour)
        }
    }
    return order
}

/**
 * Cycle detection needs three states, not two: a node still on the current
 * path is a cycle, a node already finished is not.
 */
fun hasCycle(graph: Map<String, List<String>>): Boolean {
    val inProgress = mutableSetOf<String>()
    val done = mutableSetOf<String>()

    fun visit(node: String): Boolean {
        if (node in inProgress) return true   // back edge — a real cycle
        if (node in done) return false        // already settled, not a cycle

        inProgress += node
        val found = graph[node].orEmpty().any { visit(it) }
        inProgress -= node
        done += node
        return found
    }

    return graph.keys.any { it !in done && visit(it) }
}
        """.trim(),

        Lang.GO to """
// Recursive: the call stack is doing the work, which is why this is so short.
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

// Iterative: identical, but survives graphs deeper than the call stack.
func DFSIterative(graph map[string][]string, start string) []string {
	visited := map[string]bool{}
	order := []string{}
	stack := []string{start}

	for len(stack) > 0 {
		node := stack[len(stack)-1]
		stack = stack[:len(stack)-1]

		if visited[node] {
			continue
		}
		visited[node] = true
		order = append(order, node)

		// Reversed, so the first neighbour ends up on top.
		neighbours := graph[node]
		for i := len(neighbours) - 1; i >= 0; i-- {
			if !visited[neighbours[i]] {
				stack = append(stack, neighbours[i])
			}
		}
	}
	return order
}

// Cycle detection needs three states: unvisited, in progress, done.
func HasCycle(graph map[string][]string) bool {
	const (
		unvisited = iota
		inProgress
		done
	)
	state := map[string]int{}

	var visit func(string) bool
	visit = func(node string) bool {
		if state[node] == inProgress {
			return true // back edge — a real cycle
		}
		if state[node] == done {
			return false // already settled
		}

		state[node] = inProgress
		for _, neighbour := range graph[node] {
			if visit(neighbour) {
				return true
			}
		}
		state[node] = done
		return false
	}

	for node := range graph {
		if state[node] == unvisited && visit(node) {
			return true
		}
	}
	return false
}
        """.trim(),

        Lang.JAVASCRIPT to """
/** Recursive: the call stack is doing the work, which is why this is so short. */
function dfs(graph, start, visited = new Set()) {
  // Marked on discovery. Marking on completion lets two paths into the same
  // node before either finishes.
  if (visited.has(start)) return [];
  visited.add(start);

  const order = [start];
  for (const neighbour of graph[start] ?? []) {
    order.push(...dfs(graph, neighbour, visited));
  }
  return order;
}

/** Iterative: identical, but survives graphs deeper than the call stack. */
function dfsIterative(graph, start) {
  const visited = new Set();
  const order = [];
  const stack = [start];

  while (stack.length > 0) {
    const node = stack.pop();
    if (visited.has(node)) continue;

    visited.add(node);
    order.push(node);

    // Reversed, so the first neighbour is on top and the order matches
    // the recursive version.
    for (const neighbour of [...(graph[node] ?? [])].reverse()) {
      if (!visited.has(neighbour)) stack.push(neighbour);
    }
  }
  return order;
}

/**
 * Cycle detection needs three states, not two: a node still on the current
 * path is a cycle, a node already finished is not.
 */
function hasCycle(graph) {
  const inProgress = new Set();
  const done = new Set();

  const visit = (node) => {
    if (inProgress.has(node)) return true; // back edge — a real cycle
    if (done.has(node)) return false;      // already settled

    inProgress.add(node);
    const found = (graph[node] ?? []).some(visit);
    inProgress.delete(node);
    done.add(node);
    return found;
  };

  return Object.keys(graph).some((node) => !done.has(node) && visit(node));
}
        """.trim(),
    ),

    steps = listOf(
        "**Push the start** onto the stack — or make the first call, which does the same thing.",
        "**Pop a node.** If it has already been visited, discard it and pop again; duplicates reach the stack legitimately.",
        "**Mark it visited immediately**, before touching its neighbours.",
        "**Push every unvisited neighbour.** Because a stack is last in, first out, the most recently pushed is explored next — that is the depth.",
        "**Repeat until the stack empties.** Popping past a dead end is exactly the backtracking; nothing extra is needed to make it happen.",
    ),

    questions = listOf(
        Question(
            id = 200,
            title = "Number of Islands",
            difficulty = Difficulty.MEDIUM,
            idea = "The grid is a graph in disguise — each cell has up to four neighbours. Scan for an unvisited piece of land, run one DFS to drown the entire island, and increment a counter. The insight is that the number of DFS *launches* is the answer, not anything the traversal itself returns.",
            askedAt = "Amazon, Meta, Google — the most asked graph question there is",
        ),
        Question(
            id = 207,
            title = "Course Schedule",
            difficulty = Difficulty.MEDIUM,
            idea = "\"Can all courses be finished\" is asking whether the prerequisite graph has a cycle. The trap is using a plain visited set: a node you have already fully explored is not a cycle. You need three states, and only a node still on the current path counts as one.",
            askedAt = "Amazon, Meta, Uber",
        ),
        Question(
            id = 133,
            title = "Clone Graph",
            difficulty = Difficulty.MEDIUM,
            idea = "The visited set has to become a map from original node to its copy. That map is doing double duty — it prevents infinite recursion on cycles *and* it is how you rewire each clone's neighbours to point at clones rather than originals. Miss the second job and you get a copy that still references the input.",
            askedAt = "Meta, Bloomberg",
        ),
    ),

    related = listOf("bfs", "stacks-queues", "coin-change"),
    references = Refs.basecs(),
)
