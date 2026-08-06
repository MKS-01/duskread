---
id: tries
title: Tries
tagline: A tree shaped like the alphabet — every path from the root spells a prefix.
level: intermediate
related: hash-tables, binary-trees
---

## Note
- Hash tables answer 'is this key present?' in O(1) but can't answer 'what starts with this prefix?' without a full scan — a trie walks keys one character at a time so that question is free.
- **O(k) lookup and insert**, where k is the key's length — independent of how many other keys are stored.
- Every node needs its own explicit **end-of-word flag** — a stored path is not automatically a stored word.
- **Children storage is the memory/speed trade-off**: a fixed-size array is O(1) per step but wastes space on sparse or large alphabets; a hash map per node is denser but adds hashing overhead.
- Long chains of single-child nodes waste space — a **radix tree (Patricia trie)** compresses them into one edge.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord = false
}

/** O(k): one hop per character, creating nodes only where a path is new. */
fun insert(root: TrieNode, word: String) {
    var node = root
    for (c in word) node = node.children.getOrPut(c) { TrieNode() }
    node.isWord = true
}
```

## Code: Go
```go
type TrieNode struct {
	children map[byte]*TrieNode
	isWord   bool
}

// Insert is O(k): one hop per character, creating nodes only where the
// path doesn't exist yet.
func Insert(root *TrieNode, word string) {
	node := root
	for i := 0; i < len(word); i++ {
		c := word[i]
		next, ok := node.children[c]
		if !ok {
			next = &TrieNode{children: make(map[byte]*TrieNode)}
			node.children[c] = next
		}
		node = next
	}
	node.isWord = true
}
```

## Questions
### Implement Trie (Prefix Tree)
id: 208
difficulty: medium
askedAt: Amazon, Google, Microsoft
The structure itself. The only trap is forgetting the explicit end-of-word flag and trying to infer 'is a word' from 'has no children', which breaks the moment one stored word is a prefix of another.

### Word Search II
id: 212
difficulty: hard
askedAt: Google, Airbnb, Uber
Build one trie from all target words, then DFS the board once, pruning any path whose prefix isn't in the trie. Searching the board separately per word is the trap — it revisits the same cells once per word instead of sharing the walk.

### Design Add and Search Words Data Structure
id: 211
difficulty: medium
askedAt: Meta, Amazon
A trie plus a wildcard: on a '.' character in the search, branch into every child at that position instead of just one, and recurse. Worst case degrades toward a full trie scan on a query of all dots — worth naming when asked about complexity.

## References
