package dev.mks.algoatlas.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.content.AllTopics
import dev.mks.algoatlas.content.Chapters
import dev.mks.algoatlas.content.searchTopics
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.ui.rememberUrlOpener
import dev.mks.algoatlas.ui.theme.AtlasIcons
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Mono
import dev.mks.algoatlas.ui.theme.Radius
import dev.mks.algoatlas.ui.theme.SectionLabel
import dev.mks.algoatlas.ui.theme.Space

/**
 * Search, as a screen of its own rather than a sheet over the list.
 *
 * The field floats at the bottom beside the back button, where the thumb
 * already is, and the keyboard pushes the pair up rather than covering them.
 *
 * Before anything is typed the middle of the screen runs a slow carousel of
 * algorithm families — drawn moving, not described. Tapping one searches it,
 * so the animation is the suggestion rather than decoration around it.
 */
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val trimmed = query.trim()
    val results = remember(trimmed) { searchTopics(trimmed) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                trimmed.isEmpty() -> BrowseList(onSelect, onQueryChange)
                results.isEmpty() -> NoResults(trimmed, onSelect)
                else -> ResultsList(results, trimmed, onSelect)
            }
        }

        SearchBar(query, onQueryChange, onDismiss)
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
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
                AtlasIcons.Search,
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
                    AtlasIcons.Close,
                    contentDescription = if (query.isEmpty()) "Close search" else "Clear",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Nothing typed: the algorithm families, each drawn running.
 *
 * A list of topic names would say the same thing the Learn tab already says.
 * Showing the families in motion is the one thing this app can do that a list
 * of links cannot, and tapping one searches it — so the animation is the
 * suggestion rather than decoration around it.
 */
@Composable
private fun BrowseList(onSelect: (String) -> Unit, onQueryChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AlgoTypeCarousel(
            onPick = onQueryChange,
            modifier = Modifier.padding(bottom = 40.dp),
        )
    }
}

@Composable
private fun ResultsList(results: List<Topic>, query: String, onSelect: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ListPadding,
    ) {
        results.forEach { topic ->
            item(topic.id) { TopicRow(topic, query) { onSelect(topic.id) } }
        }
    }
}

/**
 * A dead end is where a reference loses people, so this never stops at
 * "nothing found": it offers a way out to the web for the thing we do not
 * have, then the topics we do.
 */
@Composable
private fun NoResults(query: String, onSelect: (String) -> Unit) {
    val open = rememberUrlOpener()
    val encoded = remember(query) { query.replace(" ", "+") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ListPadding,
    ) {
        item("head") {
            Text(
                text = "Nothing on “$query” yet",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 16.dp),
            )
            WebOut("Search the web", "https://duckduckgo.com/?q=$encoded", open)
            WebOut("Wikipedia", "https://en.wikipedia.org/w/index.php?search=$encoded", open)
            Spacer(Modifier.height(18.dp))
        }
        // Two, not four. Enough to offer a way onward without turning a dead
        // end into a second menu.
        AllTopics.take(2).forEach { topic ->
            item(topic.id) { TopicRow(topic, null) { onSelect(topic.id) } }
        }
    }
}

/* ----------------------------------------------------------------------------
 * Pieces
 * ------------------------------------------------------------------------- */

private val ListPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp)

/** Short queries, each known to return something. */
private val Shortcuts = listOf("arrays", "binary search", "hash", "sort", "graph", "linked")

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = SectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun TopicRow(topic: Topic, query: String?, onClick: () -> Unit) {
    // Show which question matched, when the hit came from the question list
    // rather than the title — otherwise the result looks arbitrary.
    val matched = query?.let { q ->
        topic.questions.firstOrNull { it.title.contains(q, ignoreCase = true) }
            ?.takeUnless { topic.title.contains(q, ignoreCase = true) }
    }

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
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = matched?.let { "matches ${it.title}" } ?: topic.tagline,
                fontSize = 11.5.sp,
                maxLines = 1,
                fontFamily = if (matched != null) Mono else null,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            AtlasIcons.External,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
