---
id: red-black-trees
title: Red-Black Trees
tagline: Colour every node red or black, and four simple rules bound the height without strict balancing.
level: advanced
related: binary-trees, avl-trees
---

## Quick Summary
- A looser balancing rule than AVL: colour every node red or black, enforce four colour invariants, and height is bounded at roughly 2 log n instead of AVL's tighter ~1.44 log n.
- Cheaper to maintain than an AVL tree — fewer rotations per insert on average — which is why it backs most language standard library ordered maps.
- The core invariant: every root-to-leaf path passes through the same number of black nodes, which is what keeps the tree from ever collapsing toward a line.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
AVL trees keep height tightly bounded by rebalancing on every insert, but that comes at a cost: every insertion potentially triggers a rotation, sometimes several. A red-black tree accepts a looser balance guarantee — height up to roughly 2 log n instead of AVL's ~1.44 log n — in exchange for needing far fewer rotations to maintain it. That trade is usually worth it, because the height difference barely matters in practice while the rotation savings add up across millions of writes.

The mechanism is unusual for a data structure: every node gets a colour, red or black, and four rules about how those colours can appear are enough to bound the tree's height, with no explicit height or balance-factor bookkeeping at all. The rules: the root is black; every leaf (conceptually, the null pointers) is black; a red node never has a red child; and every path from a given node to any descendant leaf passes through the same number of black nodes.

That last rule — equal **black-height** on every path — is where the height bound actually comes from. If the longest possible path (alternating red and black nodes) can be at most twice as long as the shortest possible path (all black), then no path can ever be more than twice as long as any other, and the tree can never collapse toward the line-shaped worst case a plain BST is vulnerable to.

Fixing a colour violation after insertion needs at most a constant number of rotations, plus a possible **recolouring** cascade up toward the root — recolouring is cheap, just flipping colours with no restructuring, which is the specific thing that makes red-black trees less rotation-heavy than AVL trees on average. That cheapness is exactly why they sit behind ordered map implementations in most language standard libraries, where writes need to stay fast far more than reads need to be perfectly balanced.

The practical takeaway for interviews is rarely "implement a red-black tree from scratch" — the rules are numerous enough that this is uncommon under time pressure — but rather recognising *why* one sits behind `TreeMap`, `std::map`, or the Linux kernel's process scheduler, and being able to state the trade-off against AVL trees precisely.

## Origin
Red-black trees descend from **Rudolf Bayer's 1972 'symmetric binary B-trees'**, which encoded the same balance idea without using colour. **Leonidas Guibas and Robert Sedgewick renamed and reformulated the structure in their 1978 paper 'A Dichromatic Framework for Balanced Trees'**, introducing the red/black colouring that gives the structure its modern name and its now-standard four-rule formulation.

## Key Points
- **Four colour rules** bound height without explicit balance bookkeeping: root is black, leaves are black, no red node has a red child, and every root-to-leaf path has equal black-height.
- **Height is bounded at roughly 2 log n** — looser than an AVL tree's ~1.44 log n, but cheaper to maintain because fixes lean on cheap recolouring wherever possible.
- **Insert/delete fixups need at most a constant number of rotations**, unlike AVL's potential cascade — the main reason red-black trees are the more common choice in practice.
- Backs most **standard library ordered maps**: Java's `TreeMap`, C++'s `std::map`, and the Linux kernel's completely fair scheduler all use red-black trees specifically for this rotation-cheapness.
- **AVL trees win on read-heavy workloads** (tighter height bound, faster lookups); **red-black trees win on write-heavy workloads** (cheaper rebalancing) — the trade-off worth stating cleanly.

## Complexity
Search / insert / delete | O(log n) | O(1) | Height is bounded at roughly 2 log n by the colour invariants.
Rebalancing per insert/delete | O(log n) worst case | O(1) | Dominated by a recolouring cascade toward the root; actual rotations are O(1) amortised.

## Pitfalls
- Trying to memorise red-black rebalancing case-by-case for an interview — it's one of the least commonly asked 'implement this' questions precisely because the case analysis is long; knowing *why* and *when* to use one matters more than reciting the fixup algorithm.
- Assuming a red-black tree is always the better balanced tree — an AVL tree's tighter height bound wins on read-heavy workloads where lookups vastly outnumber writes.
- Forgetting that 'leaves are black' refers to the conceptual null children, not the deepest real nodes — a common source of off-by-one errors in black-height reasoning.
- Confusing red-black trees with B-trees because of the shared 'symmetric binary B-tree' ancestry — they solve the same balancing problem but serve very different contexts, in-memory maps versus disk-backed indexes.

## Steps
1. Insert as you would into a plain BST, colouring the new node red.
2. If the new node's parent is black, the four rules already hold — done.
3. If the parent is red, that violates 'no red node has a red child' — resolve it with recolouring (if the uncle is red) or a rotation plus recolouring (if the uncle is black).
4. Recolouring can cascade up toward the root; a rotation, once needed, resolves the violation in at most a constant number of steps.

## Code: Kotlin
```kotlin
enum class Color { RED, BLACK }

class RbNode(
    val value: Int,
    var color: Color = Color.RED,
    var left: RbNode? = null,
    var right: RbNode? = null,
)

/**
 * Checks all four invariants at once. Full insert/delete fixup is long
 * enough that it's rarely asked for from scratch — this validator is the
 * part worth being able to write and reason about directly.
 */
fun isValidRedBlackTree(root: RbNode?): Boolean {
    if (root != null && root.color != Color.BLACK) return false // root is black
    return blackHeight(root) != -1
}

/** Returns the black-height if every path is consistent, or -1 if not. */
private fun blackHeight(node: RbNode?): Int {
    if (node == null) return 0 // null children are conceptually black leaves

    if (node.color == Color.RED) {
        if (node.left?.color == Color.RED || node.right?.color == Color.RED) {
            return -1 // a red node has a red child
        }
    }

    val leftHeight = blackHeight(node.left)
    val rightHeight = blackHeight(node.right)
    if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) {
        return -1 // unequal black-height on some path
    }

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

// IsValidRedBlackTree checks all four invariants at once. Full insert/delete
// fixup is long enough that it's rarely asked for from scratch — this
// validator is the part worth being able to write and reason about directly.
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
