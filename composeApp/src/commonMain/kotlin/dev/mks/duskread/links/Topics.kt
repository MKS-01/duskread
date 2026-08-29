package dev.mks.duskread.links

/**
 * The subjects this reader actually uses.
 *
 * Derived from what is already on the device rather than fetched from Notion's
 * `Topic` options, for three reasons: every followed feed carries one, so the
 * curated set arrives with the source sync anyway; it costs no request, so the
 * picker opens instantly and works on a plane; and a topic invented on the
 * phone appears in the list the moment it is used, without waiting for a round
 * trip to learn about itself.
 *
 * The cost is that a topic sitting unused in Notion's dropdown does not appear
 * here. That is the right trade — typing it is how it gets used, and using it
 * is what puts it in this list.
 */
fun knownTopics(feeds: List<Feed>, links: List<SavedLink>): List<String> = (feeds.mapNotNull { it.topic } + links.mapNotNull { it.topic })
    .map { it.trim() }
    .filter { it.isNotBlank() }
    // Case-insensitively distinct, keeping the first spelling seen: Notion
    // treats "Security" and "security" as two options, and offering both
    // in one strip is how a vocabulary starts splitting in half.
    .distinctBy { it.lowercase() }
    .sorted()
