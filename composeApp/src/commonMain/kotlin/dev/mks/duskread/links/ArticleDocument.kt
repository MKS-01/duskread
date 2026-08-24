package dev.mks.duskread.links

/**
 * The colours a rendered article borrows from the app.
 *
 * Passed in as CSS hex strings rather than read from `MaterialTheme` here so
 * this file stays a pure string builder with no Compose dependency — and so
 * the caller decides which scheme is in force, which matters because the app
 * swaps between Paper Black and Ink at runtime.
 */
data class ReaderPalette(
    val background: String,
    val ink: String,
    val muted: String,
    val accent: String,
    val rule: String,
    val panel: String,
    val mono: Boolean = false,
)

/**
 * Wraps an [Article] in a document this app styles.
 *
 * This is the whole reason extraction is worth doing. Injecting CSS into the
 * live page to hide its header and footer only ever hides *the parts you
 * named* — every site names them differently, and the cookie bar, the sticky
 * share rail and the newsletter interstitial are not among them. Rebuilding
 * the page from the extracted body means the chrome is gone by construction
 * rather than by selector, and the type, measure and colour are the app's.
 *
 * Deliberately no JavaScript and no remote CSS: the only things that load are
 * the images the body already refers to.
 */
fun articleDocument(article: Article, palette: ReaderPalette): String = """
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>${article.title.escapeText()}</title>
<style>
  :root { color-scheme: dark; }
  body {
    margin: 0;
    padding: 20px 18px 64px;
    background: ${palette.background};
    color: ${palette.ink};
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, system-ui, sans-serif;
    font-size: 17px;
    line-height: 1.65;
    /* A reading measure, centred, so the article does not run edge to edge on
       a tablet or the desktop window. */
    max-width: 40em;
    margin-inline: auto;
    overflow-wrap: break-word;
  }
  h1 { font-size: 1.55rem; line-height: 1.25; margin: 0 0 6px; font-weight: 600; }
  h2 { font-size: 1.2rem; line-height: 1.3; margin: 32px 0 8px; font-weight: 600; }
  h3, h4 { font-size: 1.02rem; margin: 24px 0 6px; font-weight: 600; }
  .source { color: ${palette.muted}; font-size: 0.8rem; letter-spacing: 0.04em; text-transform: uppercase; margin: 0 0 22px; }
  .lead { width: 100%; border-radius: 10px; margin: 0 0 24px; display: block; }
  p { margin: 0 0 18px; }
  a { color: ${palette.accent}; text-decoration: none; border-bottom: 1px solid ${palette.rule}; }
  /* Ink drains the hue out of the app's own chrome; a photo left in full
     colour underneath it would be the one thing on the page still shouting
     for attention. Matching it here is what makes the theme read as the
     absence of colour rather than "everything but the pictures". */
  img { max-width: 100%; height: auto; border-radius: 10px; display: block; margin: 22px auto;${if (palette.mono) " filter: grayscale(1);" else ""} }
  figure { margin: 22px 0; }
  figcaption { color: ${palette.muted}; font-size: 0.82rem; text-align: center; margin-top: 8px; }
  blockquote { margin: 22px 0; padding: 2px 0 2px 16px; border-left: 2px solid ${palette.accent}; color: ${palette.muted}; }
  /* Code is the one place a horizontal scrollbar is better than a reflow —
     wrapping a long line changes what the line says. */
  pre { background: ${palette.panel}; border-radius: 12px; padding: 14px; overflow-x: auto; font-size: 0.85rem; line-height: 1.5; }
  code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.88em; }
  pre code { font-size: 1em; }
  hr { border: 0; border-top: 1px solid ${palette.rule}; margin: 32px 0; }
  ul, ol { padding-left: 22px; margin: 0 0 18px; }
  li { margin-bottom: 8px; }
</style>
</head>
<body>
<h1>${article.title.escapeText()}</h1>
<p class="source">${hostOf(article.url).escapeText()}</p>
${article.leadImageTag()}
${article.bodyHtml}
</body>
</html>
""".trimIndent()

/**
 * The lead image is only worth its vertical space when the article does not
 * already open with one.
 *
 * The test is "is there any image up top", not "is it *this* image", because
 * the two are the same picture under different URLs far more often than not
 * and there is no reliable way to tell: a CDN writes the requested size into
 * the path or the query, so the card's copy and the body's copy of one hero
 * image share neither filename nor extension. Guessing wrong in this
 * direction costs a picture the reader still sees one line further down;
 * guessing wrong in the other shows it to them twice.
 */
private fun Article.leadImageTag(): String {
    val image = imageUrl ?: return ""
    if (BodyImagePattern.containsMatchIn(bodyHtml.take(LeadImageLookahead))) return ""

    return """<img class="lead" src="${image.escapeText()}" alt="">"""
}

private fun String.escapeText(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private val BodyImagePattern = Regex("""<img[^>]+src="([^"]+)"""", RegexOption.IGNORE_CASE)

/** Far enough in to clear a byline or a dateline, not so far as to catch a figure halfway down. */
private const val LeadImageLookahead = 1_500
