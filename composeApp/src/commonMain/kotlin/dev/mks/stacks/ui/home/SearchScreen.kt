package dev.mks.stacks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.content.MatchField
import dev.mks.stacks.content.SearchHit
import dev.mks.stacks.content.closestMatches
import dev.mks.stacks.content.rankedSearch
import dev.mks.stacks.data.rememberRecentSearches
import dev.mks.stacks.ui.rememberUrlOpener
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.theme.StacksIcons

/**
 * Search, as a screen of its own rather than a sheet over the list.
 *
 * The field floats at the bottom beside the thumb, and the keyboard pushes it
 * up rather than covering it. Everything above the field is a single scrolling
 * column, so the same layout works whether the keyboard is up or not — the
 * previous version centred its empty state in the free space, which the
 * keyboard then ate.
 *
 * Results are ranked rather than filtered (see [rankedSearch]) and the matched
 * span is marked in the accent, so a list of near-identical sorting algorithms
 * still shows *why* each row is there.
 */
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenReader: () -> Unit,
    onDismiss: () -> Unit,
) {
    val trimmed = query.trim()
    val results = remember(trimmed) { if (trimmed.isEmpty()) emptyList() else rankedSearch(trimmed) }
    val recents = rememberRecentSearches()

    // A query is worth remembering once it has led somewhere — opening a topic,
    // or submitting the field. Recording every keystroke would fill the list
    // with the prefixes of one word.
    fun open(id: String) {
        recents.record(trimmed)
        onSelect(id)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                trimmed.isEmpty() -> BrowsePane(
                    recents = recents.entries,
                    onPick = onQueryChange,
                    onForget = recents::forget,
                    onClear = recents::clear,
                    onOpenFocus = onOpenFocus,
                    onOpenReader = onOpenReader,
                )

                results.isEmpty() -> NoResults(trimmed, ::open)
                else -> ResultsList(results, ::open)
            }
        }

        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = { recents.record(trimmed) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // Everything sits at the bottom, floating clear of the edges — the same
    // argument as the navigation bar on Home. The keyboard pushes the whole
    // row up rather than covering it.
    // One floating pill, no loose arrow beside it — an unenclosed icon next to
    // an enclosed field reads as two unrelated controls. The cross inside
    // clears the text, or closes the screen when there is none; the system
    // back gesture does the same.
    Row(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                StacksIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))

            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Topics, problems, ideas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                )
            }

            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { if (query.isEmpty()) onDismiss() else onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    StacksIcons.Close,
                    contentDescription = if (query.isEmpty()) "Close search" else "Clear",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Nothing typed: the three-pillar carousel, then whatever was searched for
 * before.
 *
 * The carousel stays because Focus and Reader are destinations rather than
 * text — no typed query could ever surface them. Recents sit under it because
 * they are the faster path for anyone who came back for a second look, and
 * both scroll together so the keyboard can take half the screen without
 * hiding either.
 */
@Composable
private fun BrowsePane(
    recents: List<String>,
    onPick: (String) -> Unit,
    onForget: (String) -> Unit,
    onClear: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenReader: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
        item("carousel") {
            PillarCarousel(
                onSearchAlgo = onPick,
                onOpenFocus = onOpenFocus,
                onOpenReader = onOpenReader,
                modifier = Modifier.padding(top = 12.dp, bottom = 26.dp),
            )
        }

        if (recents.isNotEmpty()) {
            item("recent-head") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RECENT",
                        style = SectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Clear",
                        style = SectionLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.Inline))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            recents.forEach { entry ->
                item(entry) { RecentRow(entry, onPick = { onPick(entry) }, onForget = { onForget(entry) }) }
            }
        }
    }
}

@Composable
private fun ResultsList(results: List<SearchHit>, onSelect: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
        item("count") {
            Text(
                text = if (results.size == 1) "1 RESULT" else "${results.size} RESULTS",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp),
            )
        }
        results.forEach { hit ->
            item(hit.topic.id) { ResultRow(hit) { onSelect(hit.topic.id) } }
        }
    }
}

/**
 * A dead end is where a reference loses people, so this never stops at
 * "nothing found": it retries the query one word at a time, and failing that
 * offers a way out to the web for the thing we do not have.
 */
@Composable
private fun NoResults(query: String, onSelect: (String) -> Unit) {
    val open = rememberUrlOpener()
    val encoded = remember(query) { query.replace(" ", "+") }
    val closest = remember(query) { closestMatches(query) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
        item("head") {
            Text(
                text = "Nothing on “$query” yet",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 16.dp),
            )
        }

        if (closest.isNotEmpty()) {
            item("closest-head") {
                Text(
                    text = "CLOSEST",
                    style = SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
            closest.forEach { hit ->
                item(hit.topic.id) { ResultRow(hit) { onSelect(hit.topic.id) } }
            }
            item("closest-gap") { Spacer(Modifier.height(18.dp)) }
        }

        item("web") {
            WebOut("Search the web", "https://duckduckgo.com/?q=$encoded", open)
            WebOut("Wikipedia", "https://en.wikipedia.org/w/index.php?search=$encoded", open)
        }
    }
}

/* ----------------------------------------------------------------------------
 * Pieces
 * ------------------------------------------------------------------------- */

private val ListPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp)

@Composable
private fun ResultRow(hit: SearchHit, onClick: () -> Unit) {
    val topic = hit.topic
    val title = markMatch(topic.title, hit.highlight.takeIf { hit.field == MatchField.TITLE })

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Inline))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 26.dp)
                .clip(RoundedCornerShape(Radius.Marker))
                .background(LocalVizPalette.current.of(topic.level)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Say which question matched when the hit came from the question
            // list rather than the title — otherwise the row looks arbitrary,
            // which is exactly how the unranked version failed.
            if (hit.field == MatchField.QUESTION && hit.question != null) {
                Text(
                    text = markMatch(hit.question.title, hit.highlight),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    fontFamily = Mono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = topic.tagline,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentRow(query: String, onPick: () -> Unit, onForget: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Inline))
            .clickable(onClick = onPick)
            .padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            StacksIcons.Clock,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onForget),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                StacksIcons.Close,
                contentDescription = "Forget “$query”",
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WebOut(label: String, url: String, open: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Inline))
            .clickable { open(url) }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            StacksIcons.External,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Tints the matched span in the accent. Bold alone would not survive a row of
 * near-identical titles — the point is to answer "why is *this* here?" at a
 * glance, and colour is the fastest answer the palette has.
 *
 * A match covering the whole string is left plain: there is no contrast to
 * draw, and an entirely terracotta row reads as a link rather than a result.
 */
@Composable
private fun markMatch(text: String, range: IntRange?): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return remember(text, range, accent) {
        buildAnnotatedString {
            append(text)
            val marks = range != null &&
                range.first >= 0 &&
                range.last < text.length &&
                !(range.first == 0 && range.last == text.lastIndex)
            if (marks) {
                addStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold), range!!.first, range.last + 1)
            }
        }
    }
}
