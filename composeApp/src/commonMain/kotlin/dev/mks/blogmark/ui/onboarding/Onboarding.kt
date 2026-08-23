package dev.mks.blogmark.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mks.blogmark.reader.ReaderSourcePicker
import dev.mks.blogmark.reader.rememberReadRepository
import dev.mks.blogmark.ui.common.AppTextField
import dev.mks.blogmark.ui.theme.Motion
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.SectionLabel
import dev.mks.blogmark.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Three panels — Saved, Focus, Readback — then a name.
 *
 * One panel per pillar of the app, not one per feature: the two ways articles
 * get read here are real but different enough that either alone would be
 * half the story. The Readback panel does real work — connecting the folder
 * here, rather than only describing the feature, means someone who finishes
 * onboarding is actually set up rather than merely informed. Every screen is
 * skippable: the intro is a courtesy, not a gate, and the name is optional
 * because nothing here needs an identity to work.
 */
@Composable
fun Onboarding(onDone: (name: String?) -> Unit) {
    val pageCount = 4
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    fun finish() = onDone(name)

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (pager.currentPage < pageCount - 1) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.Pill))
                        .clickable { onDone(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> IntroPanel(SavedPanel)
                1 -> IntroPanel(FocusPanel)
                2 -> ReadbackPanel()
                else -> NamePanel(
                    name = name,
                    onNameChange = { name = it },
                    onSubmit = { finish() },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dots(count = pageCount, current = pager.currentPage)
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                label = if (pager.currentPage == pageCount - 1) "Get started" else "Next",
            ) {
                if (pager.currentPage == pageCount - 1) {
                    finish()
                } else {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
    }
}

private data class Panel(
    val label: String,
    val title: String,
    val body: String,
    val art: @Composable () -> Unit,
)

private val SavedPanel = Panel(
    label = "SAVE ANYTHING",
    title = "Keep the good reads",
    body = "Paste a link or share one in from any app — it saves instantly and stays " +
        "here, read or not, until you decide it's done.",
    art = { SavedArt() },
)

private val FocusPanel = Panel(
    label = "FOCUS",
    title = "A timer built for reading sessions",
    body = "Run a Pomodoro session while you read — start it from the dashboard, and " +
        "it keeps going even when you close the timer screen.",
    art = { TimerArt() },
)

@Composable
private fun IntroPanel(panel: Panel) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.height(150.dp), contentAlignment = Alignment.Center) { panel.art() }
        Spacer(Modifier.height(40.dp))
        Text(
            text = panel.label,
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = panel.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = panel.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}

/**
 * Not a generic [IntroPanel] because it does real work: connecting the
 * Readback folder here, rather than only describing the feature, means
 * someone who finishes onboarding is actually set up rather than just
 * informed. iOS, desktop and web fall back to [ReaderSourcePicker]'s own
 * platform message.
 */
@Composable
private fun ReadbackPanel() {
    val repository = rememberReadRepository()
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.height(150.dp), contentAlignment = Alignment.Center) { WaveformArt() }
        Spacer(Modifier.height(40.dp))
        Text(
            text = "READBACK",
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pick up where you left off",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Connect a synced readback audio library and your past reads show up " +
                "here, ready to play.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(20.dp))
        ReaderSourcePicker(repository)
    }
}

@Composable
private fun NamePanel(
    name: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ONE LAST THING",
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "What should I call you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Only used to say hello. It stays on this device — there is no account " +
                "and nothing leaves the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 400.dp),
        )
        Spacer(Modifier.height(26.dp))
        AppTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it) },
            placeholder = "Your name",
            textAlign = TextAlign.Center,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You can leave this blank.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 20.dp else 7.dp, tween(Motion.Chip), label = "dot")
            Box(
                Modifier
                    .height(7.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
    )
}

/* ----------------------------------------------------------------------------
 * Art. Built from the same primitives the visualiser uses, so the intro is
 * showing the real thing rather than a stock illustration of it.
 * ------------------------------------------------------------------------- */

/** Three rows of a saved-link list, reduced to a bookmark dot and a line of title. */
@Composable
private fun SavedArt() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0.9f, 0.55f, 0.75f).forEach { widthFraction ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .width(130.dp * widthFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun TimerArt() {
    Box(
        Modifier
            .size(88.dp)
            .clip(CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "25:00",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun WaveformArt() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(18.dp, 40.dp, 26.dp, 50.dp, 22.dp, 34.dp).forEach { height ->
            Box(
                Modifier
                    .width(8.dp)
                    .height(height)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
