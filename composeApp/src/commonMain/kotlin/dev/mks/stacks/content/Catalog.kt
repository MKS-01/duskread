package dev.mks.stacks.content

import dev.mks.stacks.model.Chapter
import dev.mks.stacks.model.Question
import dev.mks.stacks.model.Topic
import stacks.composeapp.generated.resources.Res

/**
 * The curriculum, ordered basic → advanced.
 *
 * Chapter *structure* — which topic ids belong to which chapter, in what
 * order — stays a compile-checked Kotlin manifest, since it is navigation
 * metadata that rarely changes. Each topic's actual content is a bundled
 * Markdown file, loaded once at startup by [loadCatalog].
 */
private data class ChapterSpec(val id: String, val title: String, val blurb: String, val topicIds: List<String>)

private val ChapterManifest = listOf(
    ChapterSpec(
        id = "foundations",
        title = "Foundations",
        blurb = "How data is actually laid out, and what that costs you.",
        topicIds = listOf("arrays", "linked-lists", "stacks-queues", "hash-tables"),
    ),
    ChapterSpec(
        id = "searching",
        title = "Searching",
        blurb = "Finding things fast, and the invariants that make it valid.",
        topicIds = listOf("binary-search"),
    ),
    ChapterSpec(
        id = "patterns",
        title = "Patterns",
        blurb = "Recurring shapes that turn a brute-force scan into something faster.",
        topicIds = listOf("two-pointers", "sliding-window", "backtracking"),
    ),
    ChapterSpec(
        id = "sorting",
        title = "Sorting",
        blurb = "Ordering data, and the trade-offs between the classic algorithms.",
        topicIds = listOf(
            "bubble-sort",
            "selection-sort",
            "insertion-sort",
            "merge-sort",
            "quicksort",
            "heap-sort",
            "counting-sort",
            "radix-sort",
        ),
    ),
    ChapterSpec(
        id = "trees",
        title = "Trees",
        blurb = "Hierarchy instead of a line, and the shapes that buys you.",
        topicIds = listOf("binary-trees", "tries", "heaps", "avl-trees", "red-black-trees", "b-trees"),
    ),
    ChapterSpec(
        id = "graphs",
        title = "Graphs",
        blurb = "Traversal, connectivity, and shortest paths.",
        topicIds = listOf(
            "graph-representation",
            "bfs",
            "dfs",
            "dags",
            "dijkstra",
            "union-find",
            "kruskal",
        ),
    ),
    ChapterSpec(
        id = "dynamic-programming",
        title = "Dynamic Programming",
        blurb = "Solving a problem once, then never solving it again.",
        topicIds = listOf("coin-change"),
    ),
    ChapterSpec(
        id = "machine-learning",
        title = "Machine Learning",
        blurb = "How a model actually learns, one gradient step at a time.",
        topicIds = listOf("gradient-descent", "linear-regression"),
    ),
    ChapterSpec(
        id = "neural-networks",
        title = "Neural Networks",
        blurb = "Layers, gradients, and the mechanism modern models are built from.",
        topicIds = listOf("backpropagation", "attention", "transformer-architecture"),
    ),
    ChapterSpec(
        id = "llms",
        title = "LLMs",
        blurb = "What actually happens between a prompt and a generated token.",
        topicIds = listOf("tokenisation", "context-windows", "temperature-sampling"),
    ),
    ChapterSpec(
        id = "agentic-coding",
        title = "Agentic Coding",
        blurb = "Letting a model act, not just answer — and knowing if it worked.",
        topicIds = listOf("tool-use", "planning-loops", "evals"),
    ),
)

/** Populated once by [loadCatalog]; empty until then. */
var Chapters: List<Chapter> = emptyList()
    private set

/** Reads and parses every topic's bundled Markdown file, grouped per [ChapterManifest]. */
suspend fun loadCatalog() {
    Chapters = ChapterManifest.map { spec ->
        Chapter(
            id = spec.id,
            title = spec.title,
            blurb = spec.blurb,
            topics = spec.topicIds.map { id -> parseTopic(Res.readBytes("files/topics/$id.md").decodeToString()) },
        )
    }
}

val AllTopics: List<Topic>
    get() = Chapters.flatMap { it.topics }

/** Keyed lookup for opening a topic — every screen renders off this map, never a fresh scan. */
private val TopicsById: Map<String, Topic>
    get() = AllTopics.associateBy { it.id }

fun topicById(id: String): Topic? = TopicsById[id]

fun chapterOf(topic: Topic): Chapter? = Chapters.firstOrNull { chapter ->
    chapter.topics.any { it.id == topic.id }
}

/** A question paired with the topic that teaches it. */
data class PracticeItem(val question: Question, val topic: Topic)

/**
 * Every question across the curriculum, hardest last within each topic's
 * ordering preserved. This is what the Practice tab lists.
 */
val AllQuestions: List<PracticeItem>
    get() = AllTopics.flatMap { topic -> topic.questions.map { PracticeItem(it, topic) } }

/**
 * Case-insensitive match across titles, taglines and question names, so
 * searching for a LeetCode problem finds the topic that teaches it.
 */
fun searchTopics(query: String): List<Topic> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return AllTopics

    return AllTopics.filter { topic ->
        topic.title.lowercase().contains(q) ||
            topic.tagline.lowercase().contains(q) ||
            topic.questions.any { it.title.lowercase().contains(q) }
    }
}
