---
id: tries
title: Tries
tagline: A tree shaped like the alphabet — every path from the root spells a prefix.
level: intermediate
related: hash-tables, binary-trees
---

## Quick Summary
- Hash tables answer 'is this key present?' in O(1) but can't answer 'what starts with this prefix?' without a full scan — a trie walks keys one character at a time so that question is free.
- O(k) lookup and insert, where k is the key's length — not the number of keys stored.
- Every node needs its own end-of-word flag; a stored path is not automatically a stored word.
- Long chains of single-child nodes waste space — a radix tree (Patricia trie) compresses them into one edge.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Intuition
Hash tables give O(1) lookup for an exact key, but they can't answer "which keys start with 'pre'?" without scanning everything — hashing deliberately scrambles similar keys into unrelated buckets, so two keys sharing a prefix have nothing in common once hashed. A **trie** (from re*trie*val) inverts the approach entirely: instead of hashing the whole key at once, it walks the key one character at a time, and every step down the tree is shared by every other key with the same prefix.

Concretely, each node represents one character position, and its children are keyed by whatever character can come next. Insert "cat" and "car" and they share the path for "ca", forking only at the third character. Because that sharing is structural, checking whether *any* stored word begins with a given prefix is just walking the prefix's path and confirming it exists — no scan of the stored words required.

That is the entire value proposition: O(k) lookup and insert, where k is the length of the key, not the number of keys stored — plus prefix queries that a hash table cannot answer at all. The cost is space. A trie holding mostly-distinct strings can use more memory than the strings themselves, because every node carries an array or map of possible next characters, most of which go unused.

Marking the end of a word matters and is a common source of bugs. Storing "car" does not make "ca" a word — it only makes "ca" a valid *path*. Each node therefore needs its own explicit end-of-word flag, separate from simply existing on a path, or a trie cannot distinguish "this is a stored word" from "this is merely a prefix of one".

The naive trie wastes the most space on long chains of single-child nodes — storing "hello" alone allocates five nodes, each holding exactly one child. A **radix tree** (or Patricia trie) compresses those chains into a single edge labelled with the whole shared substring, trading a little insert complexity for a much smaller structure. That compression is the standard fix once memory, rather than lookup speed, becomes the bottleneck.

## Origin
The trie was introduced by **René de la Briandais in 1959**, in a paper on fast file searching, though he did not give it a name. **Edward Fredkin coined the term 'trie' in 1960**, deriving it from re*trie*val — and, notoriously, insisted it still be pronounced "tree", a pronunciation that never really caught on since it is indistinguishable from the word for the broader structure a trie is built from. Most people now say "try" instead, if only to be understood.

## Key Points
- **O(k) lookup, insert and delete**, where k is the key's length — independent of how many other keys are stored, unlike a hash table's O(1) *average*, which still depends on load factor.
- **Shared prefixes are stored once.** That structural sharing is the entire mechanism behind autocomplete: walking a prefix's path and confirming it exists answers 'does anything start with this?'
- Each node needs an explicit **end-of-word marker**, distinct from simply being a valid path — otherwise a stored word is indistinguishable from a stored prefix of some other word.
- **Children storage is the memory/speed trade-off**: a fixed-size array (26 slots for lowercase English) is O(1) per step but wastes space on sparse or large alphabets; a hash map per node is denser but adds hashing overhead.
- A **radix tree / Patricia trie** compresses runs of single-child nodes into one edge — the standard fix once a plain trie's per-character node overhead starts to matter.
- Deletion needs care: clearing a node's end-of-word flag is only safe if that node is not also a prefix of another stored word, and nodes should only be pruned once they have no children left.

## Complexity
Insert | O(k) | O(k) | k = key length; the worst case allocates one new node per character.
Search (exact) | O(k) | O(1) | Walks the key's path — no dependency on how many keys are stored.
Prefix search | O(p) | O(1) | p = prefix length, just confirming the path exists.
Storage | — | O(nodes × alphabet) | Sparse alphabets waste space with array-backed children; compress with a radix tree or use hash-map children.

## Pitfalls
- Treating 'is a valid path' as 'is a stored word' — without an explicit end-of-word flag, a stored 'car' makes 'ca' look like a stored word too.
- Using a fixed-size children array sized for a small alphabet on Unicode input — 26 slots is fine for lowercase English, hopeless for arbitrary text; use a map instead.
- Deleting a node outright when removing a word, without first checking it isn't also a prefix of some other stored word.
- Reaching for a trie when prefix queries are never needed — a plain hash table is simpler and uses less memory for pure exact-match lookups.
- Assuming trie operations are O(1) the way a hash table's average case is — they are O(k), so a long key genuinely costs more than a short one.

## Steps
1. To insert a word: start at the root.
2. For each character, move to the existing child for that character, or create one if it doesn't exist yet.
3. After the last character, mark the current node as end-of-word.

## Code: Kotlin
```kotlin
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord = false
}

class Trie {
    private val root = TrieNode()

    /** O(k): one hop per character, creating nodes only where a path is new. */
    fun insert(word: String) {
        var node = root
        for (c in word) node = node.children.getOrPut(c) { TrieNode() }
        node.isWord = true
    }

    fun search(word: String): Boolean = nodeAt(word)?.isWord == true

    /** Existence of the path is enough — no scan of stored words required. */
    fun startsWith(prefix: String): Boolean = nodeAt(prefix) != null

    private fun nodeAt(key: String): TrieNode? {
        var node = root
        for (c in key) node = node.children[c] ?: return null
        return node
    }
}
```

## Code: Go
```go
type TrieNode struct {
	children map[byte]*TrieNode
	isWord   bool
}

func newTrieNode() *TrieNode {
	return &TrieNode{children: make(map[byte]*TrieNode)}
}

type Trie struct {
	root *TrieNode
}

func NewTrie() *Trie {
	return &Trie{root: newTrieNode()}
}

// Insert is O(k): one hop per character, creating nodes only where the
// path doesn't exist yet.
func (t *Trie) Insert(word string) {
	node := t.root
	for i := 0; i < len(word); i++ {
		c := word[i]
		next, ok := node.children[c]
		if !ok {
			next = newTrieNode()
			node.children[c] = next
		}
		node = next
	}
	node.isWord = true
}

func (t *Trie) Search(word string) bool {
	node := t.nodeAt(word)
	return node != nil && node.isWord
}

// StartsWith needs only that the path exists — no scan of stored words.
func (t *Trie) StartsWith(prefix string) bool {
	return t.nodeAt(prefix) != nil
}

func (t *Trie) nodeAt(key string) *TrieNode {
	node := t.root
	for i := 0; i < len(key); i++ {
		next, ok := node.children[key[i]]
		if !ok {
			return nil
		}
		node = next
	}
	return node
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
