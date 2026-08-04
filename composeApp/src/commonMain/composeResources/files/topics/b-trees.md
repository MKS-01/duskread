---
id: b-trees
title: B-Trees
tagline: A tree shaped for disk, where each node holds many keys instead of one.
level: advanced
related: binary-trees, avl-trees, red-black-trees
---

## Quick Summary
- Wide, shallow trees — each node holds many keys and many children — designed around disk I/O, where reading one block costs the same whether it holds one key or a hundred.
- Height stays tiny even for huge datasets: a B-tree over a billion keys with a branching factor of a few hundred is only 3-4 levels deep.
- The structure behind almost every disk-backed database index — the node-size trade-off it makes only pays off once reads are expensive relative to comparisons, which is exactly disk's shape.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A binary search tree's height grows with log₂ n, and every level down means one more pointer to chase — fine in memory, where chasing a pointer is nearly free, but disastrous on disk, where each level might mean a fresh disk seek costing milliseconds instead of nanoseconds. A B-tree's entire design answers that specific problem: instead of one key and two children per node, hold *many* keys and *many* children in each node, sized to fill exactly one disk block. Reading a node then costs one disk access regardless of how many keys it holds, so packing more keys per node directly reduces the number of disk accesses a search needs.

Concretely, a B-tree of order m allows each node up to m children and m − 1 keys, keeping every node between half-full and full to bound how narrow the tree can get. Searching means comparing against the several keys in the current node to figure out which of its many children to descend into next — more comparisons per level than a binary tree, but far fewer levels overall, because the branching factor might be in the hundreds rather than two.

That trade — more comparisons per node, drastically fewer nodes to visit — is exactly right when a "visit" (a disk read) costs vastly more than a comparison (a few CPU cycles, held in cache). A B-tree over a billion keys with a branching factor of a few hundred needs only 3-4 levels to reach any key, meaning 3-4 disk reads for any lookup — compare that to the roughly 30 levels a balanced binary tree would need, each potentially a separate seek.

Insertion and deletion follow the same idea as a self-balancing binary tree — split an overfull node, merge or borrow from an underfull one — but operate on whole nodes of many keys at once rather than single elements, and rebalancing propagates upward exactly as rotations do in an AVL or red-black tree. The practical upshot is that essentially every disk-backed database index, and most filesystems' internal structures, uses a B-tree or one of its variants — B+ trees being the most common — specifically because the node-size trade-off it makes matches how disks actually work.

## Origin
**B-trees were invented by Rudolf Bayer and Edward M. McCreight at Boeing Research Labs, published in a 1972 paper, 'Organization and Maintenance of Large Ordered Indices.'** They were solving exactly the problem of indexing large files efficiently on the disk drives of the era, where minimising the number of physical disk accesses mattered enormously. What the 'B' stands for has never been definitively confirmed by either author — candidates floated over the years include Boeing, balanced, and simply Bayer's own name — and both have stayed coy about it in interviews since.

## Key Points
- **Each node holds many keys and many children**, sized to fill one disk block — reading a node is one disk access regardless of how many keys it holds.
- **Height stays tiny even at huge scale**: a branching factor in the hundreds means a billion-key B-tree needs only 3-4 levels, versus roughly 30 for a balanced binary tree.
- **Nodes stay between half-full and full** by design — inserts that overflow a node split it, and deletes that underflow one merge or borrow from a sibling, propagating upward like AVL/red-black rebalancing.
- **The trade only pays off when reads are expensive relative to comparisons** — exactly disk access versus CPU comparison, and exactly why B-trees back disk-based indexes rather than in-memory ones (which use red-black trees or hash tables instead).
- **B+ trees**, the most common real-world variant, keep all actual data in the leaves and use internal nodes purely for navigation — better for range scans, which is why almost every database index you've used is technically a B+ tree.

## Complexity
Search / insert / delete | O(log n) | O(1) | Base of the logarithm is the branching factor, not 2 — this is what keeps height tiny.
Disk accesses per operation | O(log_m n) | — | m = branching factor, often in the hundreds — the number that actually matters for disk-backed structures.

## Pitfalls
- Using a B-tree in memory where a red-black tree or hash table would do — the whole design premise is amortising expensive disk reads across many keys per node; in memory, that trade-off doesn't apply.
- Confusing a B-tree with a B+ tree — B+ trees keep data only in leaves and are what most databases actually use; plain B-trees can store data in internal nodes too, which complicates range queries.
- Picking too small a branching factor — a B-tree with only a handful of keys per node barely improves on a binary tree's height, losing the entire point of the structure.
- Assuming 'B-tree' and 'binary tree' are related by more than a naming coincidence — a B-tree node routinely holds dozens or hundreds of keys, nothing like a binary tree's two children.

## Steps
1. To search: compare the target against the keys in the current node to find which child range it falls into, then descend into that child.
2. Repeat until reaching a leaf — the search either finds the key there or confirms it's absent.
3. To insert: descend to the correct leaf and insert the key in sorted position within that node.
4. If the node now exceeds its maximum key count, split it in two and push the middle key up into the parent — splits can cascade all the way to the root.

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

/**
 * Splits an overfull leaf node in two, returning the middle key that gets
 * pushed up to the parent — this is the operation that keeps height tiny
 * by fanning nodes back out instead of growing a single node without bound.
 */
fun splitLeaf(node: BTreeNode): Pair<Int, BTreeNode> {
    val midIndex = node.keys.size / 2
    val midKey = node.keys[midIndex]
    val right = BTreeNode(node.maxKeys)
    right.keys.addAll(node.keys.subList(midIndex + 1, node.keys.size))
    val keptKeys = node.keys.subList(0, midIndex).toList()
    node.keys.clear()
    node.keys.addAll(keptKeys)
    return midKey to right
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

// SplitLeaf splits an overfull leaf node in two, returning the middle key
// that gets pushed up to the parent — the operation that keeps height tiny
// by fanning nodes back out instead of growing a single node without bound.
func SplitLeaf(node *BTreeNode) (int, *BTreeNode) {
	mid := len(node.Keys) / 2
	midKey := node.Keys[mid]
	right := &BTreeNode{MaxKeys: node.MaxKeys, Keys: append([]int(nil), node.Keys[mid+1:]...)}
	node.Keys = node.Keys[:mid]
	return midKey, right
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
