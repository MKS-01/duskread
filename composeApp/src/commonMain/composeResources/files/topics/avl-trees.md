---
id: avl-trees
title: AVL Trees
tagline: Rebalance after every insert, and height never drifts past O(log n).
level: advanced
related: binary-trees, red-black-trees
---

## Quick Summary
- The first self-balancing tree: after every insert or delete, rotate nodes back into balance so height never exceeds roughly 1.44 log n.
- The balance factor — height difference between a node's two subtrees — must stay within {-1, 0, 1} at every node, checked and fixed on the way back up from every insertion.
- Four rotation cases (left-left, right-right, left-right, right-left) cover every way a node can become unbalanced — only the path back to the root ever needs checking.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
A plain BST's O(log n) promise silently depends on the tree staying roughly balanced, and nothing about a plain BST enforces that — insert sorted data and you get a straight line. AVL trees close that gap by adding one rule, checked after every insert and delete: for every node, the heights of its left and right subtrees may differ by at most 1. Break that rule anywhere and the tree repairs itself immediately, before the imbalance can compound.

The repair mechanism is a **rotation** — a local restructuring that swaps a node with one of its children while preserving the BST ordering invariant, changing which subtree is "taller" without touching the sorted order at all. There are exactly four shapes an imbalance can take, each with a matching rotation: a straight left-heavy chain needs one right rotation, a straight right-heavy chain needs one left rotation, and the two "zig-zag" cases need two rotations each — one to straighten the zigzag before the matching single rotation applies.

What makes this cheap rather than a constant rebalancing tax is that only the ancestors of the newly inserted or deleted node can possibly have become unbalanced — a change at a leaf cannot affect the height of a subtree it isn't part of. So after inserting, you walk back up the path you just descended, checking and fixing balance factors one node at a time, and can stop the moment a node is found already balanced, because balance below it means nothing above it changed either.

The mathematical payoff for this bookkeeping is a hard guarantee: an AVL tree's height is always within a constant factor of log n — provably no worse than about 1.44 × log₂(n + 2). That is a stronger promise than a red-black tree's, which allows a slightly taller tree in exchange for cheaper, less frequent rebalancing — the classic trade-off between the two, and why AVL trees are typically preferred for read-heavy workloads and red-black trees for write-heavy ones.

## Origin
**AVL trees were invented by Georgy Adelson-Velsky and Evgenii Landis**, two Soviet computer scientists, and published in their **1962 paper 'An algorithm for the organisation of information'** in *Doklady Akademii Nauk SSSR*. It was the first self-balancing binary search tree ever described, guaranteeing O(log n) height years before red-black trees or B-trees formalised alternative approaches to the same problem. The name is simply the authors' initials.

## Key Points
- **Balance factor** — height(left) − height(right) — must be in {-1, 0, 1} at every node. A node outside that range triggers a rotation before the insert or delete is considered finished.
- **Four rotation cases**: left-left and right-right need one rotation; left-right and right-left ('zig-zag' imbalances) need two.
- Only the **path from the changed node back to the root** can have become unbalanced — the rest of the tree never needs re-checking.
- **Height is guaranteed O(log n)** — provably at most ~1.44 log₂(n + 2) — a *stronger* balance guarantee than a red-black tree's.
- The trade-off against red-black trees: **AVL trees rebalance more aggressively**, which costs more on writes but keeps lookups slightly faster — the reverse of a red-black tree's priorities.

## Complexity
Search / insert / delete | O(log n) | O(1) | Height is provably bounded — no degenerate case exists, unlike a plain BST.
Rotation | O(1) | O(1) | A fixed, local restructuring — but insert/delete may trigger one at each level walked back up.

## Pitfalls
- Rebalancing only at the insertion point instead of walking back up and checking every ancestor — an imbalance can appear several levels above where the change actually happened.
- Misidentifying which of the four rotation cases applies — the zig-zag cases are frequently implemented as a single rotation, silently leaving the tree unbalanced.
- Reaching for an AVL tree when writes vastly outnumber reads — the stricter balance factor means more frequent rotations than a red-black tree tolerates.
- Forgetting to update stored height values on every node touched by a rotation, which corrupts every future rebalancing decision at that node.

## Steps
1. Insert as you would into a plain BST.
2. Walk back up the path to the root, updating each node's height.
3. At each node, compute the balance factor. If it's outside {-1, 0, 1}, identify which of the four imbalance shapes it is.
4. Apply the matching rotation — one for a straight imbalance, two for a zig-zag — and continue back up.

## Code: Kotlin
```kotlin
class AvlNode(
    val value: Int,
    var left: AvlNode? = null,
    var right: AvlNode? = null,
    var height: Int = 1,
)

private fun height(node: AvlNode?) = node?.height ?: 0
private fun balanceFactor(node: AvlNode) = height(node.left) - height(node.right)
private fun updateHeight(node: AvlNode) {
    node.height = 1 + maxOf(height(node.left), height(node.right))
}

private fun rotateRight(y: AvlNode): AvlNode {
    val x = y.left!!
    y.left = x.right
    x.right = y
    updateHeight(y)
    updateHeight(x)
    return x
}

private fun rotateLeft(x: AvlNode): AvlNode {
    val y = x.right!!
    x.right = y.left
    y.left = x
    updateHeight(x)
    updateHeight(y)
    return y
}

fun insert(node: AvlNode?, value: Int): AvlNode {
    if (node == null) return AvlNode(value)

    if (value < node.value) node.left = insert(node.left, value)
    else if (value > node.value) node.right = insert(node.right, value)
    else return node // duplicates: no-op

    updateHeight(node)
    val balance = balanceFactor(node)

    return when {
        balance > 1 && value < node.left!!.value -> rotateRight(node)               // left-left
        balance < -1 && value > node.right!!.value -> rotateLeft(node)              // right-right
        balance > 1 -> { node.left = rotateLeft(node.left!!); rotateRight(node) }    // left-right
        balance < -1 -> { node.right = rotateRight(node.right!!); rotateLeft(node) } // right-left
        else -> node
    }
}
```

## Code: Go
```go
type AvlNode struct {
	Value       int
	Left, Right *AvlNode
	Height      int
}

func height(n *AvlNode) int {
	if n == nil {
		return 0
	}
	return n.Height
}

func updateHeight(n *AvlNode) {
	n.Height = 1 + max(height(n.Left), height(n.Right))
}

func balanceFactor(n *AvlNode) int {
	return height(n.Left) - height(n.Right)
}

func rotateRight(y *AvlNode) *AvlNode {
	x := y.Left
	y.Left = x.Right
	x.Right = y
	updateHeight(y)
	updateHeight(x)
	return x
}

func rotateLeft(x *AvlNode) *AvlNode {
	y := x.Right
	x.Right = y.Left
	y.Left = x
	updateHeight(x)
	updateHeight(y)
	return y
}

func Insert(node *AvlNode, value int) *AvlNode {
	if node == nil {
		return &AvlNode{Value: value, Height: 1}
	}
	if value < node.Value {
		node.Left = Insert(node.Left, value)
	} else if value > node.Value {
		node.Right = Insert(node.Right, value)
	} else {
		return node // duplicates: no-op
	}

	updateHeight(node)
	balance := balanceFactor(node)

	switch {
	case balance > 1 && value < node.Left.Value: // left-left
		return rotateRight(node)
	case balance < -1 && value > node.Right.Value: // right-right
		return rotateLeft(node)
	case balance > 1: // left-right
		node.Left = rotateLeft(node.Left)
		return rotateRight(node)
	case balance < -1: // right-left
		node.Right = rotateRight(node.Right)
		return rotateLeft(node)
	}
	return node
}
```

## Questions
### Balanced Binary Tree
id: 110
difficulty: easy
askedAt: Amazon, Bloomberg, Meta
Checking the AVL invariant directly: at every node, the heights of its two subtrees must differ by at most 1. Compute height and check balance in the same bottom-up pass rather than two separate traversals, or it costs O(n²) instead of O(n).

### Convert Sorted Array to Binary Search Tree
id: 108
difficulty: easy
askedAt: Amazon, Microsoft
Always pick the middle element as the root, recursively, and the result is height-balanced for free — no rotations needed, because the input's sortedness lets you choose balance directly at construction time.

## References
