package dev.mks.algoatlas.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.mks.algoatlas.model.Tone
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Motion
import dev.mks.algoatlas.ui.theme.Radius
import dev.mks.algoatlas.ui.theme.SectionLabel
import dev.mks.algoatlas.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Three panels explaining what this app is, then a name.
 *
 * The panels lead with the thing that is actually different — you watch the
 * algorithm run — rather than a welcome message, because a reader deciding
 * whether to keep the app needs to see the point in about four seconds. Every
 * screen is skippable: the intro is a courtesy, not a gate, and the name is
 * optional because nothing here needs an identity to work.
 */
@Composable
fun Onboarding(onDone: (name: String?) -> Unit) {
    val pageCount = Panels.size + 1
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
            if (page < Panels.size) {
                IntroPanel(Panels[page])
            } else {
                NamePanel(
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
                label = if (pager.currentPage == pageCount - 1) {
                    if (name.isBlank()) "Start reading" else "Start reading"
                } else {
                    "Next"
                },
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

private val Panels = listOf(
    Panel(
        label = "STEP THROUGH IT",
        title = "Watch the algorithm run",
        body = "Every topic has a visualisation you drive yourself — play it, pause it, " +
            "scrub back a step. The captions explain why each move happens, not what just moved.",
        art = { CellsArt() },
    ),
    Panel(
        label = "THREE LANGUAGES",
        title = "In the language you actually use",
        body = "Kotlin, Go and JavaScript side by side, so you can read the one you know " +
            "and compare it against the one you are learning.",
        art = { LangArt() },
    ),
    Panel(
        label = "WHAT GETS ASKED",
        title = "And the questions behind it",
        body = "Two or three real interview questions per topic, each with the insight that " +
            "unlocks it — and the trap, where there is one.",
        art = { QuestionArt() },
    ),
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
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it) },
            singleLine = true,
            placeholder = { Text("Your name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(Radius.Panel),
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

@Composable
private fun CellsArt() {
    val palette = LocalVizPalette.current
    val tones = listOf(Tone.BAD, Tone.BAD, Tone.ACTIVE, Tone.IDLE, Tone.IDLE, Tone.GOOD)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
        tones.forEach { tone ->
            val alpha by animateFloatAsState(1f, tween(Motion.Tone), label = "cell")
            Box(
                Modifier
                    .size(width = 34.dp, height = 42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.bg(tone).copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun LangArt() {
    val labels = listOf("Kotlin", "Go", "JavaScript")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
            labels.forEachIndexed { index, label ->
                val selected = index == 0
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.Pill))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(Radius.Panel))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0.9f, 0.65f, 0.8f, 0.45f).forEach { fraction ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(160.dp * fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun QuestionArt() {
    val palette = LocalVizPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Easy" to palette.good, "Medium" to palette.active, "Hard" to palette.bad)
            .forEach { (label, tint) ->
                Row(
                    Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(Radius.Inline))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(Radius.Inline),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .height(6.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
    }
}
