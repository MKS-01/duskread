package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.hashTableScene

val HashTables = Topic(
    id = "hash-tables",
    title = "Hash Tables",
    tagline = "Let the key compute its own address.",
    level = Level.INTERMEDIATE,
    scene = { hashTableScene() },

    quickSummary = listOf(
        "Let the key compute its own bucket instead of scanning or sorting — O(1) average for insert, lookup and delete.",
        "Collisions are a mathematical certainty, not a bug — chaining or open addressing decide how they're handled.",
        "Crossing a load factor of roughly 0.75 triggers a resize; amortises to O(1), but the worst case is O(n).",
        "No ordering guarantee — reach for a tree-backed map when iteration order matters.",
    ),
    readMore = Refs.BasecsHome,

    intuition = listOf(
        "Arrays gave us O(1) access, but only if you know the *index*. Usually you do not — you know a username, a URL, a word. The obvious approach is to scan until you find it, which is O(n), or to keep things sorted and binary search, which is O(log n). A hash table asks a better question: what if the key could tell us its own index?",
        "That is all a hash function is. Feed it a key, get back a number, take that number modulo the table size, and you have a bucket. No searching happened — the location was computed. Insert, lookup and delete all become O(1), which is genuinely remarkable given we started with arbitrary strings.",
        "The catch is unavoidable and it is worth being precise about why. There are infinitely many possible keys and only finitely many buckets, so some pair of distinct keys must land in the same bucket. This is the pigeonhole principle, and it means **collisions are not a bug to be engineered away — they are a mathematical certainty**. The entire design problem is what to do when one happens.",
        "The common answer is **chaining**: each bucket holds a small list, and colliding keys join it. Lookups then cost one hash plus a short walk. The alternative is **open addressing** — when a bucket is taken, probe onward for the next free slot. Chaining is simpler and degrades gracefully; open addressing is more cache-friendly because everything stays in one block, which is why high-performance implementations tend to prefer it.",
        "This is also where the O(1) claim earns its asterisk. It is an *average*, and it holds only while chains stay short. If every key collides, the table degenerates into one long list and every operation becomes O(n). Real implementations defend this by tracking the **load factor** — elements divided by buckets — and rehashing everything into a larger table when it crosses a threshold, typically around 0.75.",
        "Two consequences follow that interviews probe. First, hash tables have **no order**; if you need sorted iteration you want a tree-backed map instead. Second, the quality of your hash function is a performance decision, and for anything exposed to user input it is a **security** one too — attackers who can force collisions can turn your O(1) service into an O(n) one on purpose.",
    ),

    origin = "The idea came from **Hans Peter Luhn at IBM in an internal memo in January 1953**, while working on the problem of finding records fast without scanning them all. \"Hashing\" was borrowed from the kitchen sense of the word — to chop up and mix — because a good hash function scatters and recombines the bits of a key until the output looks nothing like the input. Luhn also invented the checksum algorithm that still validates every credit-card number you type. **Open addressing** followed shortly at IBM through Gene Amdahl and colleagues, and Donald Knuth's later analysis in *The Art of Computer Programming* turned the load-factor mathematics into the engineering rule that implementations still follow.",

    keyPoints = listOf(
        "Average O(1) for insert, lookup and delete. The **worst case is O(n)** when everything collides — the average is a statement about good hash distribution, not a guarantee.",
        "**Collisions are inevitable**, not a design flaw: infinitely many keys, finitely many buckets.",
        "**Chaining** puts a list in each bucket. **Open addressing** probes for the next free slot. Chaining is simpler; open addressing is faster in cache terms.",
        "The **load factor** (elements ÷ buckets) drives resizing. Crossing roughly 0.75 triggers a rehash into a bigger table, which is O(n) but rare enough to amortise away.",
        "Keys must be **immutable** while stored. Mutating a key changes its hash, and the entry becomes unreachable in a bucket it no longer belongs to.",
        "If two objects are equal they **must** hash equal. Overriding `equals` without `hashCode` is the classic Java/Kotlin bug — the object goes into the map and can never be found again.",
        "**No ordering guarantee.** Never rely on iteration order; use a tree map or a linked variant when order matters.",
    ),

    complexity = listOf(
        ComplexityRow("Insert / lookup / delete", "O(1) average", "O(1)", "Assumes a well-distributed hash and a controlled load factor."),
        ComplexityRow("Same, worst case", "O(n)", "O(1)", "All keys in one bucket — the table has become a linked list."),
        ComplexityRow("Resize / rehash", "O(n)", "O(n)", "Every key is rehashed into the new table. Amortises to O(1) per insert."),
        ComplexityRow("Iteration", "O(n + b)", "O(1)", "Elements plus buckets — a sparse table costs time to walk even when nearly empty."),
        ComplexityRow("Storage", "—", "O(n)", "Plus the empty buckets kept spare to hold the load factor down."),
    ),

    pitfalls = listOf(
        "Overriding `equals` but not `hashCode`. The object lands in one bucket and is looked up in another, so it is silently unfindable.",
        "Using a mutable object as a key and then mutating it. Same failure, harder to spot.",
        "Assuming iteration order is stable. It is not, and it can change between runs, versions, or after a resize.",
        "Trusting O(1) on adversarial input. If users control the keys, they can force collisions and degrade the table deliberately — a real denial-of-service vector.",
        "Writing a hash function that only samples part of the key, such as the first few characters. Any shared prefix then collides.",
        "Reaching for a hash table when the key space is small and dense. If your keys are integers 0..1000, a plain array is faster and simpler.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
/**
 * A hash table with separate chaining, written out to show the moving
 * parts. In production use the standard HashMap.
 */
class SimpleHashMap<K, V>(initialCapacity: Int = 16) {
    private data class Entry<K, V>(val key: K, var value: V)

    private var buckets = Array(initialCapacity) { mutableListOf<Entry<K, V>>() }
    private var size = 0

    /** Above this fill ratio, chains get long enough to hurt lookups. */
    private val loadFactorLimit = 0.75

    private fun bucketFor(key: K): MutableList<Entry<K, V>> {
        // Guard against negative hashes and Int.MIN_VALUE.
        val index = (key.hashCode().toLong() and 0x7fffffffL) % buckets.size
        return buckets[index.toInt()]
    }

    operator fun set(key: K, value: V) {
        val bucket = bucketFor(key)
        val existing = bucket.firstOrNull { it.key == key }

        if (existing != null) {
            existing.value = value   // same key: overwrite, do not append
            return
        }

        bucket += Entry(key, value)
        size++
        if (size.toDouble() / buckets.size > loadFactorLimit) resize()
    }

    operator fun get(key: K): V? = bucketFor(key).firstOrNull { it.key == key }?.value

    fun remove(key: K): Boolean {
        val bucket = bucketFor(key)
        val removed = bucket.removeAll { it.key == key }
        if (removed) size--
        return removed
    }

    /** O(n), but doubling makes it rare enough to amortise to O(1) per insert. */
    private fun resize() {
        val old = buckets
        buckets = Array(old.size * 2) { mutableListOf() }
        size = 0
        for (bucket in old) {
            for (entry in bucket) set(entry.key, entry.value)
        }
    }
}
        """.trim(),

        Lang.GO to """
// SimpleHashMap is a hash table with separate chaining, written out to
// show the moving parts. In production use the built-in map.
type entry struct {
	key   string
	value int
}

type SimpleHashMap struct {
	buckets [][]entry
	size    int
}

const loadFactorLimit = 0.75

func NewHashMap(capacity int) *SimpleHashMap {
	return &SimpleHashMap{buckets: make([][]entry, capacity)}
}

// FNV-1a: cheap, and it mixes every byte of the key.
func hash(key string) uint32 {
	var h uint32 = 2166136261
	for i := 0; i < len(key); i++ {
		h ^= uint32(key[i])
		h *= 16777619
	}
	return h
}

func (m *SimpleHashMap) index(key string) int {
	return int(hash(key) % uint32(len(m.buckets)))
}

func (m *SimpleHashMap) Set(key string, value int) {
	i := m.index(key)
	for j := range m.buckets[i] {
		if m.buckets[i][j].key == key {
			m.buckets[i][j].value = value // overwrite, do not append
			return
		}
	}

	m.buckets[i] = append(m.buckets[i], entry{key, value})
	m.size++
	if float64(m.size)/float64(len(m.buckets)) > loadFactorLimit {
		m.resize()
	}
}

func (m *SimpleHashMap) Get(key string) (int, bool) {
	for _, e := range m.buckets[m.index(key)] {
		if e.key == key {
			return e.value, true
		}
	}
	return 0, false
}

// O(n), but doubling makes it rare enough to amortise to O(1) per insert.
func (m *SimpleHashMap) resize() {
	old := m.buckets
	m.buckets = make([][]entry, len(old)*2)
	m.size = 0
	for _, bucket := range old {
		for _, e := range bucket {
			m.Set(e.key, e.value)
		}
	}
}
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * A hash table with separate chaining, written out to show the moving
 * parts. In production use Map.
 */
class SimpleHashMap {
  #buckets;
  #size = 0;
  static #LOAD_FACTOR_LIMIT = 0.75;

  constructor(capacity = 16) {
    this.#buckets = Array.from({ length: capacity }, () => []);
  }

  // FNV-1a: cheap, and it mixes every character of the key.
  #hash(key) {
    let h = 2166136261;
    const text = String(key);
    for (let i = 0; i < text.length; i++) {
      h ^= text.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return (h >>> 0) % this.#buckets.length;
  }

  set(key, value) {
    const bucket = this.#buckets[this.#hash(key)];
    const existing = bucket.find((e) => e.key === key);

    if (existing) {
      existing.value = value; // overwrite, do not append
      return this;
    }

    bucket.push({ key, value });
    this.#size++;
    if (this.#size / this.#buckets.length > SimpleHashMap.#LOAD_FACTOR_LIMIT) {
      this.#resize();
    }
    return this;
  }

  get(key) {
    return this.#buckets[this.#hash(key)].find((e) => e.key === key)?.value;
  }

  // O(n), but doubling makes it rare enough to amortise to O(1) per insert.
  #resize() {
    const old = this.#buckets;
    this.#buckets = Array.from({ length: old.length * 2 }, () => []);
    this.#size = 0;
    for (const bucket of old) {
      for (const { key, value } of bucket) this.set(key, value);
    }
  }
}
        """.trim(),
    ),

    questions = listOf(
        Question(
            id = 1,
            title = "Two Sum",
            difficulty = Difficulty.EASY,
            idea = "The canonical demonstration of what hash tables buy you: one pass, storing each value's index as you go and asking whether the complement has already been seen. Turns the obvious O(n²) double loop into O(n).",
            askedAt = "The single most asked interview question",
        ),
        Question(
            id = 49,
            title = "Group Anagrams",
            difficulty = Difficulty.MEDIUM,
            idea = "The insight is designing the key, not the lookup. Anagrams share a sorted-letter signature, so use that as the key. For a better answer, a 26-length character count avoids the O(k log k) sort per word.",
            askedAt = "Amazon, Meta, Uber",
        ),
        Question(
            id = 146,
            title = "LRU Cache",
            difficulty = Difficulty.MEDIUM,
            idea = "The problem that forces you to combine two structures: a hash map for O(1) lookup, plus a doubly linked list for O(1) eviction of the oldest entry. Neither alone can do both — this is why you learn them together.",
            askedAt = "Amazon, Meta, Microsoft — a design-flavoured favourite",
        ),
    ),

    related = listOf("arrays", "linked-lists"),
    references = Refs.basecs(),
)
