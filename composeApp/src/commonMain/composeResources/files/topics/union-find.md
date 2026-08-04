---
id: union-find
title: Union-Find
tagline: Track which things are connected, without ever walking the whole group to check.
level: intermediate
related: graph-representation, kruskal, dags
---

## Quick Summary
- Answers 'are these two things in the same group?' and 'merge these two groups' in close to O(1) each, without ever traversing a group to check its members.
- Two optimisations — union by rank/size and path compression — turn a naive O(n) find into amortised O(α(n)), effectively constant for any input size that could exist.
- The standard tool for Kruskal's MST, detecting cycles in an undirected graph, and 'how many connected components' problems — anywhere the question is about grouping, not paths.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Some problems are purely about grouping: are these two nodes in the same connected component? If I merge these two groups, what does the result look like? A graph traversal (BFS/DFS) can answer "are A and B connected" by searching, but that's O(V + E) per query — wasteful if you're going to ask the question many times as the graph is built up incrementally. **Union-Find** (or **disjoint-set union**) is a structure purpose-built for exactly that repeated-query shape: track group membership directly, and answer both "which group is X in?" and "merge X's group with Y's group" in close to O(1) each.

The underlying idea is a forest, but a strange one: each element points to a parent, and following those parent pointers to the top gives a group's representative — the root of that particular tree. Two elements are in the same group exactly when following their parent chains lands on the same root. **Union** just makes one root point at the other, merging the two trees — and therefore the two groups — with a single pointer change.

Done naively, this degrades exactly like an unbalanced binary search tree: repeatedly unioning in an unlucky order can build one long chain, making `find` an O(n) walk instead of a fast one. Two independent fixes solve this. **Union by rank (or size)** always attaches the smaller tree under the bigger one's root during a union, keeping trees shallow instead of letting them grow into chains. **Path compression** goes further: every time `find` walks a chain to the root, it rewrites every node it passed through to point directly at that root, flattening future lookups for free as a side effect of doing the current one.

Combined, those two optimisations give an amortised time per operation of O(α(n)) — the inverse Ackermann function, which grows so slowly that it's smaller than 5 for any n you could ever actually construct in a real computer. That is, for every practical purpose, O(1) — a structure that started out looking like it might need O(log n) or worse ends up being essentially free per operation once both fixes are in place.

The classic application is **Kruskal's algorithm** for a minimum spanning tree: sort edges by weight, and greedily add each one unless its two endpoints are already in the same union-find group — which would mean adding it creates a cycle instead of connecting something new. Union-find turns "does this edge create a cycle" from an O(V + E) traversal question into a near-O(1) lookup, which is exactly why Kruskal's algorithm can afford to consider every edge individually.

## Origin
The tree-based union-find structure, with union by rank, is credited to **Bernard A. Galler and Michael J. Fischer's 1964 paper 'An Improved Equivalence Algorithm.'** **Path compression's amortised analysis — proving the combined O(α(n)) bound — is due to Robert Tarjan's 1975 paper 'Efficiency of a Good But Not Linear Set Union Algorithm,'** which is also where the connection to the inverse Ackermann function was formally established. Tarjan later won the Turing Award in 1986, in part for this and related work on data structure efficiency.

## Key Points
- **Find**: follow parent pointers to a group's root — its representative. Two elements are in the same group exactly when their roots match.
- **Union**: attach one root under the other, merging two groups with a single pointer change.
- **Union by rank/size** always attaches the smaller tree under the bigger one's root, preventing the long-chain degradation an unlucky union order would otherwise cause.
- **Path compression** rewrites every node on a `find` path to point directly at the root, flattening the structure as a free side effect of each lookup.
- **Together, both give amortised O(α(n))** per operation — the inverse Ackermann function, effectively constant for any input size that could exist in practice.
- **The standard tool for 'does this edge create a cycle' and 'how many connected components'** — anywhere the question is about grouping rather than pathfinding.

## Complexity
Find / union, with both optimisations | O(α(n)) amortised | O(n) | α is the inverse Ackermann function — effectively constant for any n that can exist.
Find / union, naive (no optimisation) | O(n) worst case | O(n) | An unlucky union order builds a long chain, degrading find to a full walk.

## Pitfalls
- Implementing `find` without path compression and `union` without union by rank/size — either optimisation alone helps, but skipping both lets an unlucky sequence of unions degrade to O(n) per operation.
- Forgetting that union-find only answers connectivity questions, not path or distance questions — it can tell you two nodes are connected, never how, or how far apart.
- Comparing two elements directly instead of comparing their `find` results — the whole point of the structure is that group membership is checked via the root, not by any other property.
- Re-initialising or rebuilding the structure per query instead of maintaining it incrementally as unions happen — that throws away exactly the amortised benefit the structure exists to provide.

## Steps
1. Initialise every element as its own group, with itself as its own parent (and rank/size 1).
2. To find an element's group, follow parent pointers to the root, compressing the path by repointing visited nodes directly at that root.
3. To union two elements, find both roots; if they differ, attach the smaller-ranked (or smaller-sized) root under the other.
4. Two elements are connected exactly when their `find` results match.

## Code: Kotlin
```kotlin
class UnionFind(size: Int) {
    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)

    /** Path compression: repoint every visited node directly at the root. */
    fun find(x: Int): Int {
        if (parent[x] != x) parent[x] = find(parent[x])
        return parent[x]
    }

    /** Union by rank: attach the shorter tree under the taller one's root. */
    fun union(a: Int, b: Int): Boolean {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA == rootB) return false // already in the same group

        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA
            else -> { parent[rootB] = rootA; rank[rootA]++ }
        }
        return true
    }

    fun connected(a: Int, b: Int): Boolean = find(a) == find(b)
}
```

## Code: Go
```go
type UnionFind struct {
	parent []int
	rank   []int
}

func NewUnionFind(size int) *UnionFind {
	parent := make([]int, size)
	for i := range parent {
		parent[i] = i
	}
	return &UnionFind{parent: parent, rank: make([]int, size)}
}

// Find applies path compression: every visited node is repointed directly
// at the root.
func (u *UnionFind) Find(x int) int {
	if u.parent[x] != x {
		u.parent[x] = u.Find(u.parent[x])
	}
	return u.parent[x]
}

// Union attaches the shorter tree under the taller one's root.
func (u *UnionFind) Union(a, b int) bool {
	rootA, rootB := u.Find(a), u.Find(b)
	if rootA == rootB {
		return false // already in the same group
	}

	switch {
	case u.rank[rootA] < u.rank[rootB]:
		u.parent[rootA] = rootB
	case u.rank[rootA] > u.rank[rootB]:
		u.parent[rootB] = rootA
	default:
		u.parent[rootB] = rootA
		u.rank[rootA]++
	}
	return true
}

func (u *UnionFind) Connected(a, b int) bool {
	return u.Find(a) == u.Find(b)
}
```

## Questions
### Number of Provinces
id: 547
difficulty: medium
askedAt: Amazon, Bloomberg
Union every pair of directly-connected cities, then count distinct roots — the number of provinces is the number of distinct groups union-find ends up with, no separate traversal needed.

### Redundant Connection
id: 684
difficulty: medium
askedAt: Amazon, Google
Process edges in order and union each one — the first edge where union() reports the two endpoints already connected is exactly the redundant one, found in a single pass with no separate cycle-detection logic.

### Number of Operations to Make Network Connected
id: 1319
difficulty: medium
askedAt: Google, Meta
Union every existing cable, then the answer is (number of connected components − 1) — each extra cable beyond what's needed to connect everything is exactly one redundant edge that can be repurposed.

## References
