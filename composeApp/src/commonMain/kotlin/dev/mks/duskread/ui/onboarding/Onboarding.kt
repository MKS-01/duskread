package dev.mks.duskread.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.PrimaryButton
import dev.mks.duskread.ui.theme.SectionLabel

/**
 * One screen: what the app is, and an optional name.
 *
 * It used to be a four-panel pager — Saved, Focus, Readback, then the name.
 * Three of those panels described tabs that are one tap away and explain
 * themselves on arrival, so the deck's only real effect was to stand between
 * someone and the app they had just installed. The fourth asked for a Storage
 * Access Framework grant to a synced readback folder, which nobody installing
 * this fresh has; that tab is hidden now (see `UserPrefs.readbackEnabled`) and
 * the panel went with it.
 *
 * What survives is the part that could not be discovered in place: the name,
 * because nothing else in the app would think to ask, and one sentence saying
 * what the four pillars are so the tab bar is not a guess.
 *
 * Still not a gate — the button reads "Get started" whether or not the field
 * has anything in it. What changed is what happens to a blank one: it used
 * to be stored as absent, which was the right call when a bare "Hello,
 * there" was the only alternative, but it meant Settings' own name field —
 * see `NameField` — opened on a reader who had never typed anything with no
 * way to tell whether that was a choice or an oversight. A short, friendly,
 * randomly generated name closes that gap without turning the field back
 * into something that has to be filled in. It reads and edits exactly like
 * a typed one; nothing downstream needs to know the difference.
 *
 * Only onboarding does this. Clearing the name back to blank afterwards, in
 * Settings, is a deliberate act and is still honoured as absent — see
 * `UserPrefs.updateName`.
 */
@Composable
fun Onboarding(onDone: (name: String?) -> Unit) {
    var name by remember { mutableStateOf("") }

    fun finish() = onDone(name.trim().ifBlank(::randomReaderName))

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "DuskRead",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Save a link, follow a blog, hear it read back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 400.dp),
        )

        Spacer(Modifier.height(44.dp))

        Text(
            text = "ONE LAST THING",
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "What should I call you?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Only used to say hello. It stays on this device — there is no account " +
                "and nothing leaves the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 380.dp),
        )
        Spacer(Modifier.height(20.dp))
        AppTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            placeholder = "Your name",
            textAlign = TextAlign.Center,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finish() }),
            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
        )

        Spacer(Modifier.height(28.dp))

        PrimaryButton(label = "Get started", onClick = { finish() })
    }
}

/**
 * "Reader" plus four characters, so a name nobody typed still reads as one
 * rather than as an error code.
 *
 * The character set skips the pairs a phone font blurs together at this size
 * — `0`/`O`, `1`/`I`/`l` — because the one place this string is ever seen
 * again is Settings' own name field, where it has to be legible enough to
 * recognise as "the thing I never bothered to change" rather than copied
 * out and typed somewhere.
 */
private fun randomReaderName(): String {
    val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    return "Reader" + (1..4).map { chars.random() }.joinToString("")
}
