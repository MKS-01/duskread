package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.Chapter
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic

/**
 * The curriculum, ordered basic → advanced.
 *
 * Chapters are declared here even while thin, so the reading order is visible
 * and adding a topic is a one-line change.
 */
val Chapters: List<Chapter> = listOf(
    Chapter(
        id = "foundations",
        title = "Foundations",
        blurb = "How data is actually laid out, and what that costs you.",
        topics = listOf(Arrays, LinkedLists, HashTables),
    ),
    Chapter(
        id = "searching",
        title = "Searching",
        blurb = "Finding things fast, and the invariants that make it valid.",
        topics = listOf(BinarySearch),
    ),
    Chapter(
        id = "sorting",
        title = "Sorting",
        blurb = "Ordering data, and the trade-offs between the classic algorithms.",
        topics = listOf(MergeSort),
    ),
    Chapter(
        id = "graphs",
        title = "Graphs",
        blurb = "Traversal, connectivity, and shortest paths.",
        topics = listOf(Bfs),
    ),
)

val AllTopics: List<Topic> = Chapters.flatMap { it.topics }

fun topicById(id: String): Topic? = AllTopics.firstOrNull { it.id == id }

fun chapterOf(topic: Topic): Chapter? = Chapters.firstOrNull { chapter ->
    chapter.topics.any { it.id == topic.id }
}

/** A question paired with the topic that teaches it. */
data class PracticeItem(val question: Question, val topic: Topic)

/**
 * Every question across the curriculum, hardest last within each topic's
 * ordering preserved. This is what the Practice tab lists.
 */
val AllQuestions: List<PracticeItem> = AllTopics.flatMap { topic ->
    topic.questions.map { PracticeItem(it, topic) }
}

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
