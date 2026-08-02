package dev.mks.stacks.model

/**
 * Content model.
 *
 * Everything the UI shows is plain data. A [Scene] is a pre-computed list of
 * immutable frames — the UI never runs an algorithm, it only plays frames back.
 * That keeps the visualisations deterministic, testable, and trivially
 * scrubbable in both directions.
 */

enum class Lang(val label: String) {
    KOTLIN("Kotlin"),
    GO("Go"),
    JAVASCRIPT("JavaScript"),
}

enum class Level(val label: String) {
    BASIC("Basic"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
}

data class ComplexityRow(
    val label: String,
    val time: String,
    val space: String,
    val note: String? = null,
)

data class Question(
    val title: String,
    val difficulty: Difficulty,
    /** The insight that unlocks the problem — deliberately not a full solution. */
    val idea: String,
    val id: Int? = null,
    val askedAt: String? = null,
) {
    val url: String?
        get() = id?.let { "https://leetcode.com/problems/$slug/" }

    private val slug: String
        get() = title.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString("-")
}

/** An outside source worth reading after the note — credited, not absorbed. */
data class Reference(
    val label: String,
    val url: String,
    /** Publication or author, shown as a subtitle. */
    val source: String? = null,
)

/* ------------------------------------------------------------------ *
 * Visualisation primitives
 * ------------------------------------------------------------------ */

/** Semantic colour roles. The theme maps these to concrete colours. */
enum class Tone { IDLE, ACTIVE, GOOD, BAD, INFO, WARN }

data class AuxValue(val label: String, val value: String)

data class Pointer(
    val label: String,
    val index: Int,
    val tone: Tone = Tone.ACTIVE,
    /** Render under the sequence instead of above it. */
    val below: Boolean = false,
)

/** An inclusive highlighted range — a sliding window, a live search range. */
data class Span(
    val from: Int,
    val to: Int,
    val label: String? = null,
    val tone: Tone = Tone.INFO,
)

sealed interface Frame {
    val caption: String
    val aux: List<AuxValue>
}

/** One snapshot of a linear structure: array, histogram, or linked chain. */
data class SeqFrame(
    val values: List<String>,
    override val caption: String,
    val marks: Map<Int, Tone> = emptyMap(),
    val pointers: List<Pointer> = emptyList(),
    val span: Span? = null,
    /** Replaces the default index printed beneath each cell. */
    val subLabels: Map<Int, String> = emptyMap(),
    override val aux: List<AuxValue> = emptyList(),
) : Frame

data class VizNode(
    val id: String,
    val label: String,
    /** Normalised layout position, both axes in 0f..1f. */
    val x: Float,
    val y: Float,
)

data class VizEdge(val from: String, val to: String, val weight: Int? = null)

data class GraphFrame(
    override val caption: String,
    val nodes: Map<String, Tone> = emptyMap(),
    /** Keyed by [edgeKey]. Undirected edges are matched in either orientation. */
    val edges: Map<String, Tone> = emptyMap(),
    /** Badge text drawn beside a node — distances, discovery order, ranks. */
    val badges: Map<String, String> = emptyMap(),
    override val aux: List<AuxValue> = emptyList(),
) : Frame

data class MatrixFrame(
    override val caption: String,
    val grid: List<List<String?>>,
    /** Keyed by "row,col". */
    val marks: Map<String, Tone> = emptyMap(),
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
    override val aux: List<AuxValue> = emptyList(),
) : Frame

fun edgeKey(from: String, to: String) = "$from|$to"

sealed interface Scene {
    val frames: List<Frame>

    /** Boxed values in a row — arrays, strings, windows, pointers. */
    data class Cells(override val frames: List<SeqFrame>) : Scene

    /** Histogram — the right form when relative magnitude is the point. */
    data class Bars(override val frames: List<SeqFrame>) : Scene

    /** Boxes joined by arrows, terminated with null. */
    data class Chain(override val frames: List<SeqFrame>) : Scene

    data class Graph(
        val nodes: List<VizNode>,
        val edges: List<VizEdge>,
        override val frames: List<GraphFrame>,
        val directed: Boolean = false,
        /** Draw as a tree: no arrowheads, edges still parent → child. */
        val tree: Boolean = false,
    ) : Scene

    data class Matrix(override val frames: List<MatrixFrame>) : Scene
}

/* ------------------------------------------------------------------ *
 * Topics
 * ------------------------------------------------------------------ */

data class Topic(
    val id: String,
    val title: String,
    val tagline: String,
    val level: Level,
    val intuition: List<String>,
    val keyPoints: List<String>,
    /**
     * 2-4 bullets shown before the full [intuition] prose — the topic opens
     * condensed on this plus [keyPoints], expanding to everything else on
     * request. Empty means there is nothing shorter than the full notes yet.
     */
    val quickSummary: List<String> = emptyList(),
    /** A deeper piece worth the extra time — shown alongside the condensed view. */
    val readMore: Reference? = null,
    /**
     * Where the idea (and usually the name) came from — who invented it, when,
     * and what problem they were staring at. Origin stories are what make a
     * structure stick in memory long after the implementation has faded.
     */
    val origin: String? = null,
    val complexity: List<ComplexityRow>,
    val code: Map<Lang, String>,
    val questions: List<Question>,
    val steps: List<String> = emptyList(),
    val pitfalls: List<String> = emptyList(),
    val related: List<String> = emptyList(),
    val references: List<Reference> = emptyList(),
    /** Built on demand so we only compute frames for the topic on screen. */
    val scene: (() -> Scene)? = null,
)

data class Chapter(
    val id: String,
    val title: String,
    val blurb: String,
    val topics: List<Topic>,
)
