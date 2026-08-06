---
id: avl-trees
title: AVL Trees
tagline: Rebalance after every insert, and height never drifts past O(log n).
level: advanced
related: binary-trees, red-black-trees
---

## Note
- The first self-balancing tree: after every insert or delete, rotate nodes back into balance so height never exceeds roughly 1.44 log n.
- **Balance factor** — height(left) − height(right) — must stay within {-1, 0, 1} at every node, checked and fixed on the way back up from every insertion.
- **Four rotation cases** (left-left, right-right, left-right, right-left) cover every way a node can become unbalanced.
- Only the **path from the changed node back to the root** can have become unbalanced — the rest of the tree never needs re-checking.
- The trade-off against red-black trees: **AVL trees rebalance more aggressively**, costing more on writes but keeping lookups slightly faster.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
class AvlNode(val value: Int, var left: AvlNode? = null, var right: AvlNode? = null, var height: Int = 1)

private fun h(n: AvlNode?) = n?.height ?: 0

private fun rotateRight(y: AvlNode): AvlNode {
    val x = y.left!!
    y.left = x.right; x.right = y
    y.height = 1 + maxOf(h(y.left), h(y.right))
    x.height = 1 + maxOf(h(x.left), h(x.right))
    return x
}

private fun rotateLeft(x: AvlNode): AvlNode {
    val y = x.right!!
    x.right = y.left; y.left = x
    x.height = 1 + maxOf(h(x.left), h(x.right))
    y.height = 1 + maxOf(h(y.left), h(y.right))
    return y
}

/** Standard BST insert, then rebalance on the way back up via rotation. */
fun insert(node: AvlNode?, value: Int): AvlNode {
    if (node == null) return AvlNode(value)
    if (value < node.value) node.left = insert(node.left, value)
    else if (value > node.value) node.right = insert(node.right, value)
    else return node // duplicates: no-op

    node.height = 1 + maxOf(h(node.left), h(node.right))
    val balance = h(node.left) - h(node.right)

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

func rotateRight(y *AvlNode) *AvlNode {
	x := y.Left
	y.Left = x.Right
	x.Right = y
	y.Height = 1 + max(height(y.Left), height(y.Right))
	x.Height = 1 + max(height(x.Left), height(x.Right))
	return x
}

func rotateLeft(x *AvlNode) *AvlNode {
	y := x.Right
	x.Right = y.Left
	y.Left = x
	x.Height = 1 + max(height(x.Left), height(x.Right))
	y.Height = 1 + max(height(y.Left), height(y.Right))
	return y
}

// Insert does a standard BST insert, then rebalances on the way back up.
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

	node.Height = 1 + max(height(node.Left), height(node.Right))
	balance := height(node.Left) - height(node.Right)

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
