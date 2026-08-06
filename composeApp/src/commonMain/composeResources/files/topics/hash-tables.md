---
id: hash-tables
title: Hash Tables
tagline: Let the key compute its own address.
level: intermediate
related: arrays, linked-lists, tries
---

## Note
- Let the key compute its own bucket instead of scanning or sorting — O(1) average for insert, lookup and delete; the **worst case is O(n)** when everything collides.
- **Collisions are inevitable**, not a design flaw: infinitely many keys, finitely many buckets.
- **Chaining** puts a list in each bucket; **open addressing** probes for the next free slot. Chaining is simpler, open addressing is faster in cache terms.
- The **load factor** (elements ÷ buckets) drives resizing. Crossing roughly 0.75 triggers a rehash into a bigger table — O(n), but rare enough to amortise away.
- Keys must be **immutable** while stored. Mutating a key changes its hash, and the entry becomes unreachable in a bucket it no longer belongs to.
- If two objects are equal they **must** hash equal — overriding `equals` without `hashCode` is the classic bug that makes an object unfindable once stored.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/** Hashes a key to a bucket index — the address is computed, not searched. */
fun bucketIndex(key: Any, bucketCount: Int): Int {
    val hash = key.hashCode().toLong() and 0x7fffffffL // guard against negative hashes
    return (hash % bucketCount).toInt()
}
```

## Code: Go
```go
// FNV-1a: cheap, and it mixes every byte of the key into the hash.
func hash(key string) uint32 {
	var h uint32 = 2166136261
	for i := 0; i < len(key); i++ {
		h ^= uint32(key[i])
		h *= 16777619
	}
	return h
}
```

## Questions
### Two Sum
id: 1
difficulty: easy
askedAt: The single most asked interview question
The canonical demonstration of what hash tables buy you: one pass, storing each value's index as you go and asking whether the complement has already been seen. Turns the obvious O(n²) double loop into O(n).

### Group Anagrams
id: 49
difficulty: medium
askedAt: Amazon, Meta, Uber
The insight is designing the key, not the lookup. Anagrams share a sorted-letter signature, so use that as the key. For a better answer, a 26-length character count avoids the O(k log k) sort per word.

### LRU Cache
id: 146
difficulty: medium
askedAt: Amazon, Meta, Microsoft — a design-flavoured favourite
The problem that forces you to combine two structures: a hash map for O(1) lookup, plus a doubly linked list for O(1) eviction of the oldest entry. Neither alone can do both — this is why you learn them together.

## References
