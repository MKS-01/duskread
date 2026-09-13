package dev.mks.duskread.links

/**
 * The form of a URL used to decide whether two links are the same article.
 */
fun canonicalUrl(raw: String): String {
    val url = normaliseUrl(raw).trim()

    // Everything after the scheme, with any fragment discarded — `#section` addresses a
    // place within one page, never a different page.
    val body = url.substringAfter("://", url).substringBefore('#')

    val authority = body.substringBefore('/').substringBefore('?')
    val rest = body.removePrefix(authority)
    val path = rest.substringBefore('?')
    val query = rest.substringAfter('?', "")

    val host = authority.lowercase()
        .removePrefix("www.")
        // A default port is the same address written longer. Any other port is part of
        // the address and stays.
        .removeSuffix(":80")
        .removeSuffix(":443")

    // A trailing slash is a formatting habit, not a different resource — but the root is
    // nothing but its slash, so it keeps it.
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
 * Parameters that identify how someone arrived, never what they arrived at. `utm_*` is
 * handled by prefix.
 */
private val TrackingKeys = setOf(
    "ref", "referrer", "source", "src",
    "fbclid", "gclid", "dclid", "msclkid", "twclid", "igshid", "si",
    "mc_cid", "mc_eid",
    "ck_subscriber_id", "triedredirect", "isfreemail", "_bhlid",
    // Met on a real AWS link pasted into the app: an ad click id and three campaign
    // fields, ninety percent of the address by length.
    "ef_id", "trk", "sc_channel", "s_kwcid",
    "utm", "at_medium", "at_campaign",
    // Substack's referral code, on every link it shares.
    "r",
)

// Deliberately absent: `post_id`, `publication_id`, `p`, `id`, `v`.
