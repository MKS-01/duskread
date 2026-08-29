package dev.mks.duskread.links

/**
 * The form of a URL used to decide whether two links are the same article.
 *
 * Everything in this app that asks "do I already have this" used to compare
 * the address as typed, lowercased. That is too strict in exactly the cases
 * this app creates: the same post arrives from its blog's feed, again in a
 * newsletter carrying `?utm_source=`, and a third time shared from a browser
 * with a trailing slash — three addresses, one article, three rows in Notion.
 *
 * **This is a comparison key, never a destination.** The address a link was
 * saved with is what gets opened and what goes to Notion; a canonical form is
 * only ever a map key. Stripping a parameter to compare is safe, and stripping
 * it to *navigate* is a guess about someone else's server.
 *
 * Deliberately conservative. Query parameters are dropped from a known list of
 * tracking keys rather than kept from a list of meaningful ones, because the
 * meaningful ones are unknowable — `?p=` identifies a WordPress post and `?v=`
 * a YouTube video, and a blocklist that guessed wrong would silently merge two
 * different articles into one. Losing a duplicate is worse than keeping one.
 */
fun canonicalUrl(raw: String): String {
    val url = normaliseUrl(raw).trim()

    // Everything after the scheme, with any fragment discarded — `#section`
    // addresses a place within one page, never a different page.
    val body = url.substringAfter("://", url).substringBefore('#')

    val authority = body.substringBefore('/').substringBefore('?')
    val rest = body.removePrefix(authority)
    val path = rest.substringBefore('?')
    val query = rest.substringAfter('?', "")

    val host = authority.lowercase()
        .removePrefix("www.")
        // A default port is the same address written longer. Any other port
        // is part of the address and stays.
        .removeSuffix(":80")
        .removeSuffix(":443")

    // A trailing slash is a formatting habit, not a different resource — but
    // the root is nothing but its slash, so it keeps it.
    val cleanPath = path.trimEnd('/').ifEmpty { "" }

    val keptQuery = query
        .split('&')
        .filter { it.isNotBlank() }
        .filterNot { param ->
            val key = param.substringBefore('=').lowercase()
            key.startsWith("utm_") || key in TrackingKeys
        }
        // Sorted, so the same parameters in a different order are one key.
        .sorted()
        .joinToString("&")

    return buildString {
        append(host)
        append(cleanPath)
        if (keptQuery.isNotEmpty()) {
            append('?')
            append(keptQuery)
        }
    }
}

/** Whether two addresses point at the same article. */
fun sameArticle(a: String, b: String): Boolean = canonicalUrl(a) == canonicalUrl(b)

/**
 * Parameters that identify how someone arrived, never what they arrived at.
 *
 * `utm_*` is handled by prefix. The rest are the ones this app actually meets:
 * Substack and Medium both append their own, mail clients add theirs, and the
 * ad platforms add a click id to everything they touch.
 */
private val TrackingKeys = setOf(
    "ref", "referrer", "source", "src",
    "fbclid", "gclid", "dclid", "msclkid", "twclid", "igshid", "si",
    "mc_cid", "mc_eid",
    "ck_subscriber_id", "triedredirect", "isfreemail", "_bhlid",
    // Met on a real AWS link pasted into the app: an ad click id and three
    // campaign fields, ninety percent of the address by length.
    "ef_id", "trk", "sc_channel", "s_kwcid",
    "utm", "at_medium", "at_campaign",
    // Substack's referral code, on every link it shares. A single letter is
    // exactly the kind of key this list should be wary of — but five of the
    // followed publications are Substacks, so meeting it is the common case
    // and treating it as meaningful would duplicate most newsletter links.
    "r",
)

// Deliberately absent: `post_id`, `publication_id`, `p`, `id`, `v`. Substack
// does append the first two as tracking, but elsewhere a key like that is the
// article itself, and this list cannot tell the two apart. Per the rule above,
// an extra duplicate is the cheaper mistake.
