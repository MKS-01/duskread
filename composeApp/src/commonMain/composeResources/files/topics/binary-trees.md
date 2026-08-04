---
id: binary-trees
title: Binary Trees
tagline: Give a node two children instead of one, and a line becomes a hierarchy.
level: intermediate
related: arrays, linked-lists, tries, heaps
---

## Quick Summary
- Add one invariant — left is smaller, right is bigger — and a binary tree becomes a binary **search** tree, halving the search space per comparison.
- That O(log n) bound only holds when the tree is balanced. Insert sorted data with no rebalancing and it degenerates into a straight line, O(n).
- In-order traversal of a BST visits keys in sorted order — the one traversal fact worth memorising cold.
- Deleting a node with two children needs its in-order successor promoted into its place, not a plain splice.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A linked list gave up contiguity for cheap insertion, but it is still a straight line — every node points to exactly one next. The next obvious question is what happens if a node can point to two: not a sequence any more, but a hierarchy. That is a tree, and it can represent shapes a line fundamentally cannot — file systems, decision paths, anything with a branching "and/or" structure.

A binary tree just caps that branching at two children, conventionally called left and right. On its own that buys nothing but shape. The useful version adds one invariant: everything in a node's left subtree is smaller than it, everything in its right subtree is bigger. That is a **binary search tree**, and the invariant is what turns the shape into a search structure — comparing against the root eliminates one entire subtree, the same halving idea as binary search, except the sorted order is built into the shape instead of into contiguous memory.

That halving is also where the O(log n) claim earns its asterisk. It only holds if the tree stays roughly balanced. Insert 1, 2, 3, 4, 5 in that order with no rebalancing and every node only ever gets a right child — the tree is a straight line, and every operation degrades to O(n), identical to a linked list. Self-balancing variants exist purely to prevent this by doing a little extra rotation work on every insert to keep the height near log n.

Traversal order is the other thing worth being precise about, because the three common orders answer different questions rather than being interchangeable trivia. **In-order** (left, node, right) visits a BST's keys in sorted order — essentially the only reason to memorise it. **Pre-order** (node, left, right) visits a node before its children, which is how you would serialise a tree so the root is always available first when rebuilding it. **Post-order** (left, right, node) visits children before their parent, which is how you would safely delete or free a tree, since nothing is removed before what depends on it.

Binary trees are also the honest justification for recursion in most curricula, because the structure's own definition is recursive: a binary tree is either empty, or a node with a left subtree and a right subtree that are themselves binary trees. Almost every operation follows that shape directly — handle the empty case, recurse left, recurse right — which is why tree code reads shorter than it has any right to.

## Origin
**Binary search trees were described independently by several researchers around 1960** — among them P.F. Windley, Andrew Colin, and Thomas Hibbard, whose 1962 paper on the deletion algorithm is still the one most textbooks cite for the two-children removal trick. Donald Knuth's *The Art of Computer Programming* documents this multiple, near-simultaneous discovery and is where the systematic balanced-vs-unbalanced height analysis first appeared. The **self-balancing** answer followed almost immediately: Georgy Adelson-Velsky and Evgenii Landis published the AVL tree in 1962, the first structure to guarantee O(log n) height regardless of insertion order.

## Key Points
- A **binary search tree (BST)** adds one invariant to a bare binary tree: everything left of a node is smaller, everything right is bigger. That invariant is the whole reason search is possible at all.
- Search, insert and delete are all **O(height)**, and height is O(log n) only when the tree is balanced — a degenerate, line-shaped BST makes every operation O(n).
- **In-order traversal of a BST visits keys in sorted order.** Pre-order exists to serialise/rebuild; post-order exists to delete safely, children before parent.
- **Deleting a node with two children** needs its in-order successor — the smallest node in its right subtree — promoted into its place, then removed from where it used to sit.
- **Self-balancing trees** (AVL, red-black) exist purely to bound height at O(log n) by doing extra rotation work on every insert/delete. Plain BSTs make no such promise.
- A **complete binary tree** — every level full except possibly the last, filled left to right — is dense enough to store implicitly in a plain array with no pointers, which is exactly the shape a heap relies on.

## Complexity
Search (balanced BST) | O(log n) | O(1) | Height-bounded; each comparison discards one whole subtree.
Search (unbalanced BST) | O(n) | O(1) | Degenerates to a linked list when insertions arrive already sorted.
Insert / delete (balanced) | O(log n) | O(1) | Same height bound as search.
Traversal (any order) | O(n) | O(h) | Visits every node once; extra space is the recursion stack, h = height.
Storage | — | O(n) | Two child pointers per node, plus the value.

## Pitfalls
- Deleting a two-child node by splicing it out directly — the invariant breaks unless the in-order successor (or predecessor) is promoted into its place first.
- Assuming O(log n) on data that arrives sorted or near-sorted — a plain BST builds a straight line under that input. Use a self-balancing tree when insertion order isn't random.
- Treating in-order traversal as proof of a valid BST — it just visits nodes in that pattern regardless of whether the invariant actually holds.
- Recursing on a subtree without a null/empty base case — the single most common binary-tree bug, since every child is either a subtree or absent.
- Comparing nodes by reference instead of by value after a rotation — an easy typo that silently breaks the ordering property without an obvious symptom.

## Steps
1. To insert value v into a BST: start at the root.
2. If the current spot is empty, v becomes that node.
3. If v is smaller than the current node, recurse left; if larger, recurse right.
4. Repeat until an empty spot is found — that is where the new node attaches.

## Code: Kotlin
```kotlin
class TreeNode(val value: Int, var left: TreeNode? = null, var right: TreeNode? = null)

/** BST insert — recurse toward the empty slot the value belongs at. */
fun insert(root: TreeNode?, value: Int): TreeNode {
    if (root == null) return TreeNode(value)
    if (value < root.value) root.left = insert(root.left, value)
    else if (value > root.value) root.right = insert(root.right, value)
    return root
}

/** O(height): one comparison per level, discarding the other subtree entirely. */
fun search(root: TreeNode?, target: Int): Boolean = when {
    root == null -> false
    target == root.value -> true
    target < root.value -> search(root.left, target)
    else -> search(root.right, target)
}

/** In-order traversal of a BST visits every key in sorted order. */
fun inorder(root: TreeNode?, out: MutableList<Int> = mutableListOf()): List<Int> {
    if (root == null) return out
    inorder(root.left, out)
    out += root.value
    inorder(root.right, out)
    return out
}
```

## Code: Go
```go
type TreeNode struct {
	Value       int
	Left, Right *TreeNode
}

// Insert recurses toward the empty slot the value belongs at.
func Insert(root *TreeNode, value int) *TreeNode {
	if root == nil {
		return &TreeNode{Value: value}
	}
	if value < root.Value {
		root.Left = Insert(root.Left, value)
	} else if value > root.Value {
		root.Right = Insert(root.Right, value)
	}
	return root
}

// Search is O(height): one comparison per level, discarding the other subtree.
func Search(root *TreeNode, target int) bool {
	if root == nil {
		return false
	}
	if target == root.Value {
		return true
	}
	if target < root.Value {
		return Search(root.Left, target)
	}
	return Search(root.Right, target)
}

// Inorder visits every key of a BST in sorted order.
func Inorder(root *TreeNode, out *[]int) {
	if root == nil {
		return
	}
	Inorder(root.Left, out)
	*out = append(*out, root.Value)
	Inorder(root.Right, out)
}
```

## Questions
### Validate Binary Search Tree
id: 98
difficulty: medium
askedAt: Amazon, Meta, Bloomberg
Comparing each node only to its immediate parent is the classic wrong answer — a node can be locally fine and still violate the invariant with a grandparent. Carry a valid (min, max) range down the recursion instead.

### Lowest Common Ancestor of a BST
id: 235
difficulty: medium
askedAt: Amazon, Facebook
Use the BST property instead of a generic tree LCA search: if both targets are smaller than the current node go left, if both are bigger go right, and the first node where they split is the answer — no full traversal needed.

### Kth Smallest Element in a BST
id: 230
difficulty: medium
askedAt: Amazon, Google, Bloomberg
In-order traversal visits keys in sorted order, so the kth value visited is the answer — stop as soon as you reach it rather than collecting the whole traversal first.

## References
