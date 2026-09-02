package dev.mks.duskread.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.notion.NotionClient
import dev.mks.duskread.notion.NotionPage
import dev.mks.duskread.notion.NotionPrefs
import dev.mks.duskread.notion.NotionResult
import dev.mks.duskread.notion.PastedTokenAuth
import dev.mks.duskread.notion.Provisioning
import dev.mks.duskread.notion.provision
import dev.mks.duskread.ui.PlatformBackHandler
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.PrimaryButton
import dev.mks.duskread.ui.rememberExternalUrlOpener
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke
import kotlinx.coroutines.launch

/**
 * Connecting Notion, as four steps that check themselves.
 *
 * This replaces two masked ID fields and a page of external documentation. The
 * old Settings section could only ever report the *last* thing that failed —
 * "Database not found" covers a mistyped ID, a database shared with the wrong
 * integration, and a token that reaches nothing at all, and those have nothing
 * in common except the sentence. Splitting the setup into steps that each know
 * whether they are done means the screen can point at the one that isn't.
 *
 * Step ③ is the reason this is a sheet rather than a tooltip. Notion's API
 * cannot create anything at the workspace root — see `NotionProvision.kt` — so
 * a page has to be shared with the token by hand, in a menu three levels deep
 * that nobody finds by guessing. It gets the literal menu path, a link, and a
 * button that re-checks rather than a paragraph asking the reader to trust
 * that they did it right.
 *
 * **It says "optional" first, and means it.** Nothing reaches this sheet
 * except a deliberate tap on Settings ▸ Notion ▸ Set up — onboarding does not
 * mention Notion and no screen refuses to render without it — so the copy is
 * written for someone who has never heard of a Notion access token and may
 * well close this again. That is also what a store reviewer does: opens the
 * app, uses it, never signs into anything.
 *
 * The wording follows Notion's own, deliberately: **personal access token**,
 * created with **New token**, starting `ntn_`. Someone matching two screens
 * cannot afford a third synonym, which is what this sheet had — a title
 * saying "token", a body saying "secret" and a field saying "personal access
 * token", for one string. The `ntn_` shape is the half that survives Notion
 * relabelling the button again.
 *
 * The portal link opens in the reader's *own* browser, not the in-app one —
 * see `rememberExternalUrlOpener`. Signing in and copying a secret inside a
 * WebView this app owns is the wrong place to ask for a password.
 *
 * The steps are not navigable. A reader cannot be on step ④ with step ② unmet,
 * so the sheet computes where it is from what is true rather than from where
 * anyone tapped.
 */
@Composable
fun NotionSetupSheet(
    prefs: NotionPrefs,
    auth: PastedTokenAuth,
    api: NotionClient,
    hasToken: Boolean,
    onTokenSaved: () -> Unit,
    onConnected: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    val scope = rememberCoroutineScope()
    val openUrl = rememberExternalUrlOpener()

    var tokenSaved by remember { mutableStateOf(hasToken) }
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var choices by remember { mutableStateOf<List<NotionPage>>(emptyList()) }
    var done by remember { mutableStateOf(false) }

    /**
     * One attempt at the whole of steps ③ and ④.
     *
     * [parentPageId] is set only when the reader has just tapped a page in the
     * picker; every other call lets `provision` work it out.
     */
    fun run(parentPageId: String? = null) {
        if (busy) return
        busy = true
        note = null

        scope.launch {
            when (val result = provision(api, prefs, parentPageId)) {
                is NotionResult.Failure -> note = result.message

                is NotionResult.Ok -> when (val state = result.value) {
                    is Provisioning.Ready -> {
                        choices = emptyList()
                        done = true
                        onConnected()
                    }

                    is Provisioning.NeedsParent -> {
                        choices = state.pages
                        note = "Pick where DuskRead should keep its databases."
                    }

                    Provisioning.NoPagesShared -> {
                        choices = emptyList()
                        note = "That token cannot see any pages yet."
                    }
                }
            }
            busy = false
        }
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = DuskReadIcons.Close,
                        contentDescription = "Close setup",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Connect Notion",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    // Same order and the same union as SettingsScreen, and for
                    // the same reason: the token field is low enough down that
                    // an open keyboard would otherwise cover it.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Optional — everything in DuskRead works without it. Your saved " +
                        "links and followed blogs live on this phone either way.\n\n" +
                        "If you use Notion, connecting it keeps a copy of both there, in two " +
                        "databases DuskRead creates for you: somewhere to read, sort and add " +
                        "to them on a laptop. Close this any time; nothing is half-done.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                Step(number = 1, title = "Create a token", done = tokenSaved) {
                    Text(
                        text = "Notion calls this a personal access token — a key that lets " +
                            "one app in, and nothing else. Choose New token, give it any " +
                            "name, and copy the value it shows you: a long line starting ntn_.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    StepAction("Open Notion in your browser") { openUrl(IntegrationsUrl) }
                }

                Spacer(Modifier.height(18.dp))

                Step(number = 2, title = "Paste it here", done = tokenSaved) {
                    Text(
                        text = "Notion shows the token once, so paste it before you leave the " +
                            "page. It stays on this device, and DuskRead sends it nowhere but " +
                            "Notion.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = if (tokenSaved && token.isEmpty()) MaskedToken else token,
                        onValueChange = { token = it },
                        placeholder = "Personal access token",
                        fontSize = 13.5.sp,
                        mono = true,
                        trailing = {
                            AnimatedVisibility(token.isNotBlank()) {
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            auth.save(token)
                                            token = ""
                                            tokenSaved = true
                                            onTokenSaved()
                                            // Straight on to the next step: the
                                            // token is only ever saved in order
                                            // to be used, and a reader who has
                                            // just pasted one should not have to
                                            // find a second button to find out
                                            // whether it worked.
                                            run()
                                        }
                                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                )
                            }
                        },
                    )
                }

                Spacer(Modifier.height(18.dp))

                Step(number = 3, title = "Share one page", done = done) {
                    Text(
                        text = "A new token can see nothing until you hand it something. Open " +
                            "any Notion page — a new empty one is fine — then " +
                            "··· › Connections, and pick the one you just made. DuskRead " +
                            "builds its two databases inside that page.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // The picker, only when the choice is real. One accessible
                    // page is chosen without asking; see `provision`.
                    choices.forEach { page ->
                        Spacer(Modifier.height(8.dp))
                        PageChoice(page = page, enabled = !busy) { run(page.id) }
                    }

                    Spacer(Modifier.height(10.dp))
                    StepAction(if (busy) "Checking…" else "Check again") { if (tokenSaved) run() }
                }

                Spacer(Modifier.height(18.dp))

                Step(number = 4, title = "Ready", done = done) {
                    Text(
                        text = if (done) {
                            "Both databases are connected — look for DuskRead Sources and " +
                                "DuskRead Reading List in Notion. Follow a blog here and it " +
                                "appears there; add a row there and it appears here."
                        } else {
                            "Nothing to do — DuskRead creates both databases as soon as the " +
                                "steps above are done."
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                note?.let {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(28.dp))

                if (done) {
                    PrimaryButton(label = "Done", onClick = onClose)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * One step: a number, a title, and whatever it takes to satisfy it.
 *
 * Only the step being worked on takes the accent — the one-accent rule. A
 * finished step goes quiet and keeps a tick, because a column of four accented
 * ticks would put four competing marks on one surface and say nothing about
 * where the reader actually is.
 */
@Composable
private fun Step(
    number: Int,
    title: String,
    done: Boolean,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (done) scheme.onSurfaceVariant else scheme.primary

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(Radius.Chip))
                    .border(Stroke.Hairline, tint, RoundedCornerShape(Radius.Chip)),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        imageVector = DuskReadIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = tint,
                    )
                } else {
                    Text(
                        text = number.toString(),
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        color = tint,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
        }

        // Indented to the width of the numeral and its gap, so the steps read
        // as a numbered list rather than four unrelated blocks.
        Column(Modifier.padding(start = 32.dp, top = 8.dp)) { content() }
    }
}

/** A step's own affordance, weighted below [PrimaryButton] — a step is not the screen's call to action. */
@Composable
private fun StepAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .border(
                Stroke.Hairline,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Radius.Chip),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/** One candidate home for the databases. */
@Composable
private fun PageChoice(page: NotionPage, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Text(
        text = page.title,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 13.sp,
        color = scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Inline))
            .background(scheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Enough to show a token is held without showing the token. */
private const val MaskedToken = "ntn_••••••••••••••••"

private const val IntegrationsUrl = "https://www.notion.so/profile/integrations"
