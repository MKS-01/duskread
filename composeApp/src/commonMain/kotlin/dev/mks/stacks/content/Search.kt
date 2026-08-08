package dev.mks.stacks.content

import dev.mks.stacks.model.Question
import dev.mks.stacks.model.Topic

/**
 * Search ranking.
 *
 * The old matcher was a filter: a topic either contained the query somewhere or
 * it did not, and survivors came back in curriculum order. That put "Linked
 * Lists" above "Bubble Sort" for the query *sort*, because one of its questions
 * is called *Merge Two Sorted Lists* — technically a match, and useless as an
 * answer. What a reader means by a two-word query is almost always a title.
 *
 * So a match now carries *where* it was found, and where decides the order:
 * title beats acronym beats tagline beats question, and within the title an
 * earlier, word-aligned hit beats one buried mid-word. Ties keep curriculum
 * order, which is why the sort below must stay stable — for a query like *sort*
 * every sorting algorithm scores identically, and basic-to-advanced is a better
 * tiebreak than anything a score could invent.
 */
enum class MatchField {
    TITLE,

    /** Initials only: *bfs* → Breadth-First Search. */
    ACRONYM,
    TAGLINE,

    /** The topic itself does not match; one of its practice questions does. */
    QUESTION,
}

/**
 * One result. [highlight] indexes the string the row actually shows — the
 * topic title, or the question title for a [MatchField.QUESTION] hit — so the
 * UI can mark the matched span without repeating the search.
 */
data class SearchHit(
    val topic: Topic,
    val field: MatchField,
    val question: Question? = null,
    val highlight: IntRange? = null,
    val score: Int = 0,
)

// Gaps are wide enough that no combination of weaker signals can outrank a
// stronger field. A title hit anywhere is worth more than a perfect tagline
// hit — this is a curriculum, and its titles are the names of the things in it.
private const val TitleExact = 100
private const val TitlePrefix = 92
private const val TitleWord = 84
private const val Acronym = 76
private const val TitleContains = 68
private const val TaglineWord = 50
private const val TaglineContains = 42
private const val QuestionWord = 30
private const val QuestionContains = 24

/** Ranked matches, best first. An empty query means everything, in course order. */
fun rankedSearch(query: String): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return AllTopics.map { SearchHit(it, MatchField.TITLE) }

    return AllTopics.mapNotNull { hitFor(it, q) }.sortedByDescending { it.score }
}

/** The topics alone, for callers that only need the list — the two-pane list pane. */
fun searchTopics(query: String): List<Topic> = rankedSearch(query).map { it.topic }

/**
 * The best single hit for one topic, or null. Fields are tried strongest
 * first and the first to match wins: a topic matching in both its title and a
 * question is a title hit, and showing it as anything else would be noise.
 */
private fun hitFor(topic: Topic, q: String): SearchHit? {
    val title = topic.title.lowercase()
    val inTitle = title.indexOf(q)
    if (inTitle >= 0) {
        return SearchHit(
            topic = topic,
            field = MatchField.TITLE,
            highlight = inTitle until inTitle + q.length,
            score = when {
                title == q -> TitleExact
                inTitle == 0 -> TitlePrefix
                isWordStart(title, inTitle) -> TitleWord
                else -> TitleContains
            },
        )
    }

    // Single letters are excluded: "s" is the initial of half the curriculum,
    // and matching them all would bury the topics whose titles actually start
    // with it.
    if (q.length >= 2 && initials(topic.title).startsWith(q)) {
        return SearchHit(topic, MatchField.ACRONYM, score = Acronym)
    }

    val tagline = topic.tagline.lowercase()
    val inTagline = tagline.indexOf(q)
    if (inTagline >= 0) {
        return SearchHit(
            topic = topic,
            field = MatchField.TAGLINE,
            score = if (isWordStart(tagline, inTagline)) TaglineWord else TaglineContains,
        )
    }

    val question = topic.questions.firstOrNull { it.title.lowercase().contains(q) }
    if (question != null) {
        val at = question.title.lowercase().indexOf(q)
        return SearchHit(
            topic = topic,
            field = MatchField.QUESTION,
            question = question,
            highlight = at until at + q.length,
            score = if (isWordStart(question.title.lowercase(), at)) QuestionWord else QuestionContains,
        )
    }

    return null
}

/**
 * A relaxed retry for queries that found nothing: search each word on its own
 * and keep the best hits. "quantum arrays" matches no topic whole, but
 * "arrays" matches one, and a near miss is a better dead end than none at all.
 *
 * Every word is tried, not just the longest — the word carrying the meaning is
 * as often the short one ("fast bfs") as the long one.
 */
fun closestMatches(query: String, limit: Int = 3): List<SearchHit> {
    val words = query.trim().split(' ').filter { it.length > 2 }
    if (words.size < 2) return emptyList()

    return words
        .flatMap { rankedSearch(it) }
        .groupBy { it.topic.id }
        .map { (_, hits) -> hits.maxBy { it.score } }
        .sortedByDescending { it.score }
        .take(limit)
}

/** True when [at] begins a word, so "search" scores higher in "Binary Search" than in "Researching". */
private fun isWordStart(text: String, at: Int): Boolean = at == 0 || !text[at - 1].isLetterOrDigit()

/** "Breadth-First Search" → "bfs". Hyphens split words, so DFS and BFS both work. */
private fun initials(title: String): String = buildString {
    title.forEachIndexed { index, char ->
        if (char.isLetterOrDigit() && isWordStart(title, index)) append(char.lowercaseChar())
    }
}
