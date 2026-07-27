package dev.mks.algoatlas.ui.code

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.ui.theme.LocalCodePalette
import dev.mks.algoatlas.ui.theme.Mono

/**
 * Minimal inline markup for note text: `**bold**`, `*italic*` and `` `code` ``.
 *
 * Deliberately not a Markdown parser — notes are authored as plain prose and
 * these three are the only emphases worth having in body copy.
 */
@Composable
@ReadOnlyComposable
fun markup(text: String): AnnotatedString {
    val codeColor = LocalCodePalette.current.keyword
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh

    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val close = text.indexOf("**", i + 2)
                    if (close == -1) {
                        append(text.substring(i))
                        i = text.length
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, close))
                        }
                        i = close + 2
                    }
                }

                text[i] == '*' -> {
                    val close = text.indexOf('*', i + 1)
                    if (close == -1) {
                        append(text.substring(i))
                        i = text.length
                    } else {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, close))
                        }
                        i = close + 1
                    }
                }

                text[i] == '`' -> {
                    val close = text.indexOf('`', i + 1)
                    if (close == -1) {
                        append(text.substring(i))
                        i = text.length
                    } else {
                        withStyle(
                            SpanStyle(
                                fontFamily = Mono,
                                fontSize = 13.sp,
                                color = codeColor,
                                background = codeBackground,
                            ),
                        ) {
                            append(text.substring(i + 1, close))
                        }
                        i = close + 1
                    }
                }

                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
