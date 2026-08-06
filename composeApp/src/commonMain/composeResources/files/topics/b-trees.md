---
id: b-trees
title: B-Trees
tagline: A tree shaped for disk, where each node holds many keys instead of one.
level: advanced
related: binary-trees, avl-trees, red-black-trees
---

## Note
- **Each node holds many keys and many children**, sized to fill one disk block — reading a node is one disk access regardless of how many keys it holds.
- **Height stays tiny even at huge scale**: a branching factor in the hundreds means a billion-key B-tree needs only 3-4 levels, versus roughly 30 for a balanced binary tree.
- **Nodes stay between half-full and full** by design — inserts that overflow a node split it, and deletes that underflow one merge or borrow from a sibling, propagating upward like AVL/red-black rebalancing.
- **The trade only pays off when reads are expensive relative to comparisons** — exactly disk access versus CPU comparison, and exactly why B-trees back disk-based indexes rather than in-memory ones.
- **B+ trees**, the most common real-world variant, keep all actual data in the leaves and use internal nodes purely for navigation — better for range scans, which is why almost every database index you've used is technically a B+ tree.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** A single B-tree node. Real implementations pick order (maxKeys) to fill one disk block. */
class BTreeNode(val maxKeys: Int) {
    val keys = mutableListOf<Int>()
    val children = mutableListOf<BTreeNode>()
    val isLeaf: Boolean get() = children.isEmpty()
}

/** O(log_m n): compares against this node's keys to pick which child to descend into. */
fun search(node: BTreeNode, target: Int): Boolean {
    var i = 0
    while (i < node.keys.size && target > node.keys[i]) i++
    if (i < node.keys.size && node.keys[i] == target) return true
    if (node.isLeaf) return false
    return search(node.children[i], target)
}
```

## Code: Go
```go
// BTreeNode is a single B-tree node. Real implementations pick order
// (maxKeys) to fill one disk block.
type BTreeNode struct {
	MaxKeys  int
	Keys     []int
	Children []*BTreeNode
}

func (n *BTreeNode) IsLeaf() bool { return len(n.Children) == 0 }

// Search is O(log_m n): compares against this node's keys to pick which
// child to descend into.
func Search(node *BTreeNode, target int) bool {
	i := 0
	for i < len(node.Keys) && target > node.Keys[i] {
		i++
	}
	if i < len(node.Keys) && node.Keys[i] == target {
		return true
	}
	if node.IsLeaf() {
		return false
	}
	return Search(node.Children[i], target)
}
```

## Questions
### Why do databases use B-trees instead of binary search trees?
difficulty: medium
askedAt: Common in database and systems-design interviews
The real test isn't implementing one from scratch — it's explaining the trade-off precisely: minimising the number of disk seeks matters more than minimising comparisons, and a wide, shallow tree needs far fewer seeks than a narrow, deep one.

### Design an on-disk key-value store
difficulty: hard
askedAt: Systems design interviews, especially at infrastructure-heavy companies
A B+ tree index over the data file is the standard answer: internal nodes purely for navigation, leaves holding (or pointing to) the actual records, sized so each node read is exactly one disk block.

## References
