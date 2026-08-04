package dev.mks.stacks.content

import dev.mks.stacks.model.ComplexityRow
import dev.mks.stacks.model.Difficulty
import dev.mks.stacks.model.Lang
import dev.mks.stacks.model.Level
import dev.mks.stacks.model.Question
import dev.mks.stacks.model.Reference
import dev.mks.stacks.model.Topic

/**
 * Reader for the topic content format: flat `key: value` front matter
 * followed by `##`-headed body sections, each with its own tiny per-section
 * syntax (bullets, numbered steps, `label | value | value` rows, fenced code).
 *
 * Deliberately not YAML or a Markdown-spec parser — the format is small and
 * fully ours (written by hand or by `/add-topic`), so a purpose-built reader
 * is less total complexity than configuring and trusting a generic
 * multiplatform library for a shape nothing else uses.
 */
fun parseTopic(raw: String): Topic {
    val (frontMatter, body) = splitFrontMatter(raw)
    val fields = parseFrontMatter(frontMatter)
    val sections = parseSections(body)

    val sceneKey = fields["scene"]

    return Topic(
        id = fields.getValue("id"),
        title = fields.getValue("title"),
        tagline = fields.getValue("tagline"),
        level = Level.valueOf(fields.getValue("level").uppercase()),
        quickSummary = sections["Quick Summary"]?.let(::parseBullets).orEmpty(),
        readMore = sections["Read More"]?.let(::firstNonBlankLine)?.let(::parseReferenceRow),
        intuition = sections["Intuition"]?.let(::parseParagraphs).orEmpty(),
        origin = sections["Origin"]?.trim()?.takeIf { it.isNotEmpty() },
        keyPoints = sections["Key Points"]?.let(::parseBullets).orEmpty(),
        complexity = sections["Complexity"]?.let(::parseComplexityRows).orEmpty(),
        code = parseCode(sections),
        questions = sections["Questions"]?.let(::parseQuestions).orEmpty(),
        steps = sections["Steps"]?.let(::parseNumbered).orEmpty(),
        pitfalls = sections["Pitfalls"]?.let(::parseBullets).orEmpty(),
        related = fields["related"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        references = Refs.basecs(*parseReferenceRows(sections["References"]).toTypedArray()),
        scene = sceneKey?.let { key -> SceneRegistry[key] },
    )
}

private fun splitFrontMatter(raw: String): Pair<String, String> {
    val trimmed = raw.trimStart('\n')
    require(trimmed.startsWith("---")) { "Topic file must open with a --- front matter block" }
    val afterOpen = trimmed.substring(3).trimStart('\n')
    val closeIndex = afterOpen.indexOf("\n---")
    require(closeIndex != -1) { "Topic file front matter is never closed with ---" }
    val frontMatter = afterOpen.substring(0, closeIndex)
    val body = afterOpen.substring(closeIndex + 4).trimStart('\n')
    return frontMatter to body
}

private fun parseFrontMatter(block: String): Map<String, String> = block.lines()
    .filter { it.isNotBlank() }
    .associate { line ->
        val colon = line.indexOf(':')
        require(colon != -1) { "Front matter line has no ':': $line" }
        line.substring(0, colon).trim() to line.substring(colon + 1).trim()
    }

/** Splits the body on `## Heading` lines into heading name -> section body. */
private fun parseSections(body: String): Map<String, String> {
    val sections = mutableMapOf<String, String>()
    var currentHeading: String? = null
    val currentBody = StringBuilder()

    fun flush() {
        val heading = currentHeading ?: return
        sections[heading] = currentBody.toString().trim('\n')
        currentBody.clear()
    }

    for (line in body.lines()) {
        if (line.startsWith("## ")) {
            flush()
            currentHeading = line.removePrefix("## ").trim()
        } else if (currentHeading != null) {
            currentBody.append(line).append('\n')
        }
    }
    flush()

    return sections
}

private fun parseBullets(section: String): List<String> = section.lines()
    .filter { it.isNotBlank() }
    .map { it.trim().removePrefix("- ").trim() }

private fun parseNumbered(section: String): List<String> = section.lines()
    .filter { it.isNotBlank() }
    .map { it.trim().replace(Regex("""^\d+\.\s*"""), "") }

/** Paragraphs are separated by a blank line; each paragraph is one physical line. */
private fun parseParagraphs(section: String): List<String> = section.split("\n\n")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

private fun firstNonBlankLine(section: String): String? = section.lines().firstOrNull { it.isNotBlank() }?.trim()

/** `label | url | source` — source is optional and blank collapses to null. */
private fun parseReferenceRow(line: String): Reference {
    val parts = line.split("|").map { it.trim() }
    val source = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
    return Reference(label = parts[0], url = parts[1], source = source)
}

private fun parseReferenceRows(section: String?): List<Reference> = section?.lines()
    ?.filter { it.isNotBlank() }
    ?.map(::parseReferenceRow)
    .orEmpty()

/** `label | time | space | note` — note is optional and blank collapses to null. */
private fun parseComplexityRows(section: String): List<ComplexityRow> = section.lines()
    .filter { it.isNotBlank() }
    .map { line ->
        val parts = line.split("|").map { it.trim() }
        ComplexityRow(
            label = parts[0],
            time = parts[1],
            space = parts[2],
            note = parts.getOrNull(3)?.takeIf { it.isNotEmpty() },
        )
    }

private fun parseCode(sections: Map<String, String>): Map<Lang, String> = buildMap {
    sections["Code: Kotlin"]?.let { put(Lang.KOTLIN, extractFence(it)) }
    sections["Code: Go"]?.let { put(Lang.GO, extractFence(it)) }
}

/** Strips the ```lang / ``` fence wrapping a code section's body. */
private fun extractFence(section: String): String {
    val lines = section.trim('\n').lines()
    val open = lines.indexOfFirst { it.trimStart().startsWith("```") }
    val close = lines.indexOfLast { it.trim() == "```" }
    require(open != -1 && close > open) { "Code section is missing its ``` fence" }
    return lines.subList(open + 1, close).joinToString("\n").trim('\n')
}

/** Repeated `### Title` blocks: `key: value` lines, then free-text idea. */
private fun parseQuestions(section: String): List<Question> {
    val blocks = mutableListOf<MutableList<String>>()
    for (line in section.lines()) {
        if (line.startsWith("### ")) {
            blocks.add(mutableListOf(line))
        } else if (blocks.isNotEmpty()) {
            blocks.last().add(line)
        }
    }

    return blocks.map { blockLines ->
        val title = blockLines.first().removePrefix("### ").trim()
        var id: Int? = null
        var difficulty: Difficulty? = null
        var askedAt: String? = null
        val ideaLines = mutableListOf<String>()

        for (line in blockLines.drop(1)) {
            if (line.isBlank()) continue
            val colon = line.indexOf(':')
            val key = if (colon != -1) line.substring(0, colon).trim() else null
            when (key) {
                "id" -> id = line.substring(colon + 1).trim().toInt()
                "difficulty" -> difficulty = Difficulty.valueOf(line.substring(colon + 1).trim().uppercase())
                "askedAt" -> askedAt = line.substring(colon + 1).trim()
                else -> ideaLines.add(line.trim())
            }
        }

        Question(
            title = title,
            difficulty = requireNotNull(difficulty) { "Question '$title' is missing difficulty" },
            idea = ideaLines.joinToString(" ").trim(),
            id = id,
            askedAt = askedAt,
        )
    }
}
