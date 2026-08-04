package dev.mks.stacks.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import dev.mks.stacks.model.Lang
import dev.mks.stacks.ui.theme.CodePalette

/**
 * A small hand-rolled tokeniser for Kotlin and Go.
 *
 * This is deliberately not a full grammar. It is a single left-to-right scan
 * that recognises comments, strings, numbers, keywords, type-ish names and call
 * sites — which is everything you need to read a self-contained 40-line teaching
 * snippet, and it costs nothing on any platform. Anything it cannot classify
 * falls through as plain text rather than being mangled.
 */
fun highlight(code: String, lang: Lang, palette: CodePalette): AnnotatedString {
    val keywords = keywordsFor(lang)

    return buildAnnotatedString {
        var i = 0

        fun push(text: String, color: androidx.compose.ui.graphics.Color, italic: Boolean = false) {
            withStyle(
                SpanStyle(
                    color = color,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                ),
            ) { append(text) }
        }

        while (i < code.length) {
            val c = code[i]

            // Line comment
            if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
                val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                push(code.substring(i, end), palette.comment, italic = true)
                i = end
                continue
            }

            // Block comment
            if (c == '/' && i + 1 < code.length && code[i + 1] == '*') {
                val close = code.indexOf("*/", i + 2)
                val end = if (close == -1) code.length else close + 2
                push(code.substring(i, end), palette.comment, italic = true)
                i = end
                continue
            }

            // Strings. Go raw literals use backticks and contain no escapes.
            if (c == '"' || c == '\'' || c == '`') {
                val end = scanString(code, i, c)
                push(code.substring(i, end), palette.string)
                i = end
                continue
            }

            // Numbers, including 0x… and decimals
            if (c.isDigit()) {
                var j = i
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                push(code.substring(i, j), palette.number)
                i = j
                continue
            }

            // Identifiers, keywords, annotations
            if (c.isLetter() || c == '_' || c == '@') {
                var j = i
                if (code[j] == '@') j++
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)

                // Is the next non-space character an opening paren?
                var k = j
                while (k < code.length && code[k] == ' ') k++
                val isCall = k < code.length && code[k] == '('

                val color = when {
                    word in keywords -> palette.keyword
                    word.startsWith("@") -> palette.type
                    isCall -> palette.function
                    word.first().isUpperCase() -> palette.type
                    else -> palette.plain
                }
                push(word, color)
                i = j
                continue
            }

            // Punctuation and operators
            if (!c.isWhitespace() && !c.isLetterOrDigit()) {
                push(c.toString(), palette.punctuation)
                i++
                continue
            }

            push(c.toString(), palette.plain)
            i++
        }
    }
}

/** Returns the index just past the closing quote. */
private fun scanString(code: String, start: Int, quote: Char): Int {
    var j = start + 1
    while (j < code.length) {
        val ch = code[j]
        if (ch == '\\' && quote != '`') {
            j += 2
            continue
        }
        if (ch == quote) return j + 1
        // An unterminated single-quoted literal should not swallow the file.
        if (ch == '\n' && quote != '`') return j
        j++
    }
    return code.length
}

private fun keywordsFor(lang: Lang): Set<String> = when (lang) {
    Lang.KOTLIN -> KotlinKeywords
    Lang.GO -> GoKeywords
}

private val KotlinKeywords = setOf(
    "fun", "val", "var", "if", "else", "when", "for", "while", "do", "return",
    "in", "is", "as", "class", "object", "interface", "typealias", "enum",
    "data", "sealed", "annotation", "companion", "init", "constructor",
    "private", "internal", "public", "protected", "override", "open",
    "abstract", "final", "const", "lateinit", "vararg", "inline", "noinline",
    "crossinline", "reified", "suspend", "operator", "infix", "tailrec",
    "external", "expect", "actual", "import", "package", "null", "true",
    "false", "this", "super", "break", "continue", "throw", "try", "catch",
    "finally", "by", "out", "where", "get", "set", "field",
)

private val GoKeywords = setOf(
    "func", "var", "const", "type", "struct", "interface", "map", "chan",
    "go", "defer", "select", "if", "else", "for", "range", "switch", "case",
    "default", "fallthrough", "return", "break", "continue", "goto",
    "package", "import", "nil", "true", "false", "make", "len", "cap",
    "append", "copy", "new", "delete", "panic", "recover", "int", "int64",
    "string", "bool", "byte", "rune", "float64", "error",
)
