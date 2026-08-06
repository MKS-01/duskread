---
id: red-black-trees
title: Red-Black Trees
tagline: Colour every node red or black, and four simple rules bound the height without strict balancing.
level: advanced
related: binary-trees, avl-trees
---

## Note
- **Four colour rules** bound height without explicit balance bookkeeping: root is black, leaves are black, no red node has a red child, and every root-to-leaf path has equal black-height.
- **Height is bounded at roughly 2 log n** — looser than an AVL tree's ~1.44 log n, but cheaper to maintain because fixes lean on cheap recolouring wherever possible.
- **Insert/delete fixups need at most a constant number of rotations**, unlike AVL's potential cascade — the main reason red-black trees are the more common choice in practice.
- Backs most **standard library ordered maps**: Java's `TreeMap`, C++'s `std::map`, and the Linux kernel's completely fair scheduler all use red-black trees specifically for this rotation-cheapness.
- **AVL trees win on read-heavy workloads** (tighter height bound, faster lookups); **red-black trees win on write-heavy workloads** (cheaper rebalancing) — the trade-off worth stating cleanly.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
enum class Color { RED, BLACK }

class RbNode(
    val value: Int,
    var color: Color = Color.RED,
    var left: RbNode? = null,
    var right: RbNode? = null,
)

/** Checks all four invariants at once — the part worth writing from scratch. */
fun isValidRedBlackTree(root: RbNode?): Boolean {
    if (root != null && root.color != Color.BLACK) return false // root is black
    return blackHeight(root) != -1
}

/** Returns the black-height if every path is consistent, or -1 if not. */
private fun blackHeight(node: RbNode?): Int {
    if (node == null) return 0 // null children are conceptually black leaves
    if (node.color == Color.RED) {
        if (node.left?.color == Color.RED || node.right?.color == Color.RED) return -1
    }
    val leftHeight = blackHeight(node.left)
    val rightHeight = blackHeight(node.right)
    if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) return -1
    return leftHeight + if (node.color == Color.BLACK) 1 else 0
}
```

## Code: Go
```go
type Color int

const (
	Red Color = iota
	Black
)

type RbNode struct {
	Value       int
	Color       Color
	Left, Right *RbNode
}

// IsValidRedBlackTree checks all four invariants at once — the part worth
// writing from scratch.
func IsValidRedBlackTree(root *RbNode) bool {
	if root != nil && root.Color != Black {
		return false // root is black
	}
	return blackHeight(root) != -1
}

// blackHeight returns the black-height if every path is consistent, or -1.
func blackHeight(node *RbNode) int {
	if node == nil {
		return 0 // null children are conceptually black leaves
	}
	if node.Color == Red {
		leftRed := node.Left != nil && node.Left.Color == Red
		rightRed := node.Right != nil && node.Right.Color == Red
		if leftRed || rightRed {
			return -1 // a red node has a red child
		}
	}
	left := blackHeight(node.Left)
	right := blackHeight(node.Right)
	if left == -1 || right == -1 || left != right {
		return -1 // unequal black-height on some path
	}
	if node.Color == Black {
		return left + 1
	}
	return left
}
```

## Questions
### Count of Smaller Numbers After Self
id: 315
difficulty: hard
askedAt: Google, Meta — a favourite for testing augmented balanced trees
An order-statistics BST — a self-balancing tree (conceptually a red-black tree) augmented with subtree sizes — answers 'how many inserted values are smaller than x' in O(log n) per insert, which is exactly what the running count needs. A Fenwick tree over compressed values is the more common accepted answer, but naming the balanced-tree approach shows you understand why augmentation works.

## References
