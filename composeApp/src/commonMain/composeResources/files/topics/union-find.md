---
id: union-find
title: Union-Find
tagline: Track which things are connected, without ever walking the whole group to check.
level: intermediate
related: graph-representation, kruskal, dags
---

## Note
- **Find**: follow parent pointers to a group's root — its representative. Two elements are in the same group exactly when their roots match.
- **Union**: attach one root under the other, merging two groups with a single pointer change.
- **Union by rank/size** always attaches the smaller tree under the bigger one's root, preventing the long-chain degradation an unlucky union order would otherwise cause.
- **Path compression** rewrites every node on a `find` path to point directly at the root, flattening the structure as a free side effect of each lookup.
- **Together, both give amortised O(α(n))** per operation — the inverse Ackermann function, effectively constant for any input size that could exist in practice.
- **The standard tool for 'does this edge create a cycle' and 'how many connected components'** — anywhere the question is about grouping rather than pathfinding.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
