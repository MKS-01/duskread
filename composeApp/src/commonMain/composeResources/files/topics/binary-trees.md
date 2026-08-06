---
id: binary-trees
title: Binary Trees
tagline: Give a node two children instead of one, and a line becomes a hierarchy.
level: intermediate
related: arrays, linked-lists, tries, heaps
---

## Note
- Add one invariant — left is smaller, right is bigger — and a binary tree becomes a binary **search** tree, halving the search space per comparison.
- That O(log n) bound only holds when the tree is balanced. Insert sorted data with no rebalancing and it degenerates into a straight line, O(n).
- **In-order traversal of a BST visits keys in sorted order** — the one traversal fact worth memorising cold.
- **Deleting a node with two children** needs its in-order successor — the smallest node in its right subtree — promoted into its place, not a plain splice.
- **Self-balancing trees** (AVL, red-black) exist purely to bound height at O(log n) by doing extra rotation work on every insert/delete. Plain BSTs make no such promise.
- A **complete binary tree** — every level full except possibly the last, filled left to right — is dense enough to store implicitly in a plain array, exactly the shape a heap relies on.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
