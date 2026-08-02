package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Pointer
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.model.Tone

/**
 * Why an array read is O(1): the address is arithmetic, not a search.
 * Then why an insert in the middle is not.
 */
fun arrayScene(): Scene {
    val frames = mutableListOf<SeqFrame>()
    val values = mutableListOf("A", "B", "C", "D", "E")

    fun addresses() = values.indices.associateWith { "${1000 + it * 4}" }

    frames += SeqFrame(
        values = values.toList(),
        caption = "An array is one unbroken block of memory. Here five elements sit at addresses 1000 to 1016, four bytes apart.",
        subLabels = addresses(),
        aux = listOf(AuxValue("base", "1000"), AuxValue("stride", "4 bytes")),
    )

    frames += SeqFrame(
        values = values.toList(),
        caption = "To read index 3 the machine computes base + 3 × 4 = 1012 and jumps straight there. No scanning — that is why reads are O(1).",
        marks = mapOf(3 to Tone.GOOD),
        pointers = listOf(Pointer("index 3", 3, Tone.GOOD)),
        subLabels = addresses(),
        aux = listOf(AuxValue("address", "1000 + 3×4 = 1012")),
    )

    frames += SeqFrame(
        values = values.toList(),
        caption = "Now insert a new value at index 1. The slot is occupied, and the block cannot bend — something has to move.",
        marks = mapOf(1 to Tone.ACTIVE),
        pointers = listOf(Pointer("insert here", 1)),
        subLabels = addresses(),
    )

    // Grow by one slot, then walk backwards moving each element right. Doing it
    // front-to-back would overwrite values before they were copied.
    val work = (values + "·").toMutableList()
    for (i in values.lastIndex downTo 1) {
        work[i + 1] = work[i]
        work[i] = "·"
        frames += SeqFrame(
            values = work.toList(),
            caption = "Shift ${work[i + 1]} one slot to the right to open up space.",
            marks = mapOf((i + 1) to Tone.WARN, i to Tone.ACTIVE),
            aux = listOf(AuxValue("shifts", "${values.size - i}")),
        )
    }

    work[1] = "X"
    frames += SeqFrame(
        values = work.toList(),
        caption = "Only now can X be written. Inserting near the front costs O(n) moves — the price arrays pay for O(1) reads.",
        marks = mapOf(1 to Tone.GOOD),
        pointers = listOf(Pointer("X", 1, Tone.GOOD)),
        aux = listOf(AuxValue("shifts", "4"), AuxValue("cost", "O(n)")),
    )

    return Scene.Cells(frames)
}

/**
 * A linked list insert, which is the exact mirror image of the array insert
 * above: finding the spot is slow, but the write itself is free.
 */
fun linkedListScene(): Scene {
    val frames = mutableListOf<SeqFrame>()
    val nodes = mutableListOf("7", "12", "19", "26")

    frames += SeqFrame(
        values = nodes.toList(),
        caption = "A linked list scatters its nodes anywhere in memory. Each one stores a value and the address of the next.",
        aux = listOf(AuxValue("head", "7"), AuxValue("size", "4")),
    )

    frames += SeqFrame(
        values = nodes.toList(),
        caption = "There is no arithmetic shortcut to index 2 — you only ever hold the head, so you have to walk.",
        marks = mapOf(0 to Tone.ACTIVE),
        pointers = listOf(Pointer("current", 0)),
        aux = listOf(AuxValue("steps", "0")),
    )

    for (i in 1..2) {
        frames += SeqFrame(
            values = nodes.toList(),
            caption = "Follow the next pointer to node ${nodes[i]}.",
            marks = (0 until i).associateWith { Tone.BAD } + (i to Tone.ACTIVE),
            pointers = listOf(Pointer("current", i)),
            aux = listOf(AuxValue("steps", "$i")),
        )
    }

    frames += SeqFrame(
        values = nodes.toList(),
        caption = "Found the insertion point after ${nodes[2]}. Walking here cost O(n) — the traversal is the expensive half.",
        marks = mapOf(2 to Tone.GOOD),
        pointers = listOf(Pointer("current", 2, Tone.GOOD)),
        aux = listOf(AuxValue("steps", "2")),
    )

    nodes.add(3, "21")
    frames += SeqFrame(
        values = nodes.toList(),
        caption = "The insert itself is two pointer writes: 21 points at 26, and 19 points at 21. Nothing shifts. That part is O(1).",
        marks = mapOf(3 to Tone.GOOD, 2 to Tone.INFO),
        pointers = listOf(Pointer("new", 3, Tone.GOOD)),
        aux = listOf(AuxValue("shifts", "0"), AuxValue("pointer writes", "2")),
    )

    return Scene.Chain(frames)
}

/** A stack and a queue fed the same input, to show the discipline is the only difference. */
fun stackQueueScene(): Scene {
    val frames = mutableListOf<SeqFrame>()
    val stack = mutableListOf<String>()

    frames += SeqFrame(
        values = listOf("∅"),
        caption = "A stack is last in, first out. Think of a stack of plates: you can only touch the top one.",
        marks = mapOf(0 to Tone.IDLE),
        aux = listOf(AuxValue("size", "0")),
    )

    for (value in listOf("A", "B", "C")) {
        stack += value
        frames += SeqFrame(
            values = stack.toList(),
            caption = "push($value) — the new element goes on top and becomes the only one reachable.",
            marks = mapOf(stack.lastIndex to Tone.GOOD),
            pointers = listOf(Pointer("top", stack.lastIndex, Tone.GOOD)),
            aux = listOf(AuxValue("size", "${stack.size}")),
        )
    }

    while (stack.isNotEmpty()) {
        val popped = stack.removeLast()
        frames += SeqFrame(
            values = stack.toList().ifEmpty { listOf("∅") },
            caption = "pop() returns $popped — the most recent arrival leaves first. That is what LIFO means.",
            marks = if (stack.isEmpty()) mapOf(0 to Tone.IDLE) else mapOf(stack.lastIndex to Tone.GOOD),
            pointers = if (stack.isEmpty()) emptyList() else listOf(Pointer("top", stack.lastIndex, Tone.GOOD)),
            aux = listOf(AuxValue("popped", popped), AuxValue("size", "${stack.size}")),
        )
    }

    val queue = mutableListOf("A", "B", "C")
    frames += SeqFrame(
        values = queue.toList(),
        caption = "A queue keeps the same three arrivals but serves them first in, first out — like a line at a counter.",
        pointers = listOf(Pointer("front", 0, Tone.GOOD), Pointer("back", 2, Tone.WARN)),
        aux = listOf(AuxValue("size", "3")),
    )

    while (queue.isNotEmpty()) {
        val removed = queue.removeFirst()
        frames += SeqFrame(
            values = queue.toList().ifEmpty { listOf("∅") },
            caption = "dequeue() returns $removed — the oldest arrival leaves first. Same data, opposite order to the stack.",
            marks = if (queue.isEmpty()) mapOf(0 to Tone.IDLE) else mapOf(0 to Tone.GOOD),
            pointers = if (queue.isEmpty()) emptyList() else listOf(Pointer("front", 0, Tone.GOOD)),
            aux = listOf(AuxValue("served", removed), AuxValue("size", "${queue.size}")),
        )
    }

    return Scene.Cells(frames)
}

/** Hashing, bucket placement, and what a collision actually looks like. */
fun hashTableScene(): Scene {
    val frames = mutableListOf<SeqFrame>()
    val buckets = MutableList(7) { "" }

    // Small deterministic hash so the collision is reproducible in the notes.
    fun hash(key: String): Int = key.sumOf { it.code } % 7

    fun render(caption: String, marks: Map<Int, Tone>, aux: List<AuxValue>) {
        frames += SeqFrame(
            values = buckets.map { it.ifEmpty { "·" } },
            caption = caption,
            marks = marks,
            subLabels = buckets.indices.associateWith { "$it" },
            aux = aux,
        )
    }

    render(
        "A hash table is an array of buckets. The trick is computing which bucket a key belongs in, rather than searching for it.",
        emptyMap(),
        listOf(AuxValue("buckets", "7"), AuxValue("stored", "0")),
    )

    var stored = 0
    for (key in listOf("cat", "dog", "bird")) {
        val index = hash(key)
        render(
            "hash(\"$key\") = ${key.sumOf { it.code }} mod 7 = $index. The key itself tells us where to look — no scanning.",
            mapOf(index to Tone.ACTIVE),
            listOf(AuxValue("key", key), AuxValue("bucket", "$index")),
        )
        buckets[index] = key
        stored++
        render(
            "Store \"$key\" in bucket $index. Lookup later will recompute the same hash and land in one step: O(1).",
            mapOf(index to Tone.GOOD),
            listOf(AuxValue("stored", "$stored")),
        )
    }

    // Force a collision to make the failure mode concrete.
    val collidingKey = "act" // same letters as "cat", so the same sum
    val collisionIndex = hash(collidingKey)
    render(
        "Now insert \"$collidingKey\". Its letters sum to the same value as \"cat\", so it hashes to bucket $collisionIndex — which is taken.",
        mapOf(collisionIndex to Tone.BAD),
        listOf(AuxValue("key", collidingKey), AuxValue("bucket", "$collisionIndex")),
    )

    buckets[collisionIndex] = "cat→act"
    render(
        "That is a collision. Chaining resolves it by turning the bucket into a small list. Lookups there now cost O(k), not O(1).",
        mapOf(collisionIndex to Tone.WARN),
        listOf(AuxValue("collisions", "1"), AuxValue("load factor", "4/7")),
    )

    render(
        "A good hash function spreads keys evenly, keeping chains short. A bad one degrades every operation toward O(n).",
        mapOf(collisionIndex to Tone.WARN),
        listOf(AuxValue("avg lookup", "O(1)"), AuxValue("worst", "O(n)")),
    )

    return Scene.Cells(frames)
}
