package dev.mks.stacks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.mks.stacks.content.AllTopics
import dev.mks.stacks.content.loadCatalog
import dev.mks.stacks.content.topicById
import dev.mks.stacks.data.rememberUserPrefs
import dev.mks.stacks.model.Lang
import dev.mks.stacks.ui.PlatformBackHandler
import dev.mks.stacks.ui.TopicListPane
import dev.mks.stacks.ui.TopicScreen
import dev.mks.stacks.ui.home.HomeScreen
import dev.mks.stacks.ui.home.HomeTab
import dev.mks.stacks.ui.onboarding.Onboarding
import dev.mks.stacks.ui.pomodoro.FocusScreen
import dev.mks.stacks.ui.theme.Layout
import dev.mks.stacks.ui.theme.Motion
import dev.mks.stacks.ui.theme.StacksIcons
import dev.mks.stacks.ui.theme.StacksTheme

/** Below this width there is no room for two panes, so we navigate instead. */
@Composable
fun App() {
    // Both themes are dark; this picks the colourless one. Not persisted —
    // it is a mood switch for the current sitting, not a setting.
    var mono by remember { mutableStateOf(false) }

    val prefs = rememberUserPrefs()

    var catalogLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        loadCatalog()
        catalogLoaded = true
    }

    StacksTheme(mono = mono) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!catalogLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Surface
            }

            if (!prefs.introSeen) {
                Onboarding(
                    onDone = { name ->
                        prefs.updateName(name)
                        prefs.markIntroSeen()
                    },
                )
                return@Surface
            }

            var selectedId by remember { mutableStateOf<String?>(null) }
            var lang by remember { mutableStateOf(Lang.KOTLIN) }
            var focusMode by remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth >= Layout.TwoPaneBreakpoint) {
                        TwoPaneLayout(
                            selectedId = selectedId ?: AllTopics.first().id,
                            onSelect = { selectedId = it },
                            onOpenFocus = { focusMode = true },
                            lang = lang,
                            onLangChange = { lang = it },
                            mono = mono,
                            onToggleTheme = { mono = !mono },
                        )
                    } else {
                        PhoneLayout(
                            selectedId = selectedId,
                            greeting = prefs.name?.let { "Hello, $it" },
                            onSelect = { selectedId = it },
                            onBack = { selectedId = null },
                            onOpenFocus = { focusMode = true },
                            lang = lang,
                            onLangChange = { lang = it },
                            mono = mono,
                            onToggleTheme = { mono = !mono },
                        )
                    }
                }

                // The big-timer mode: a full-screen destination for whenever
                // the point is to actually stare at the clock, not glance at a
                // corner. Closing it never stops the session underneath.
                AnimatedVisibility(
                    visible = focusMode,
                    enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
                    exit = fadeOut(tween(Motion.PopFade)),
                ) {
                    FocusScreen(onClose = { focusMode = false })
                }
            }
        }
    }
}

@Composable
private fun PhoneLayout(
    selectedId: String?,
    greeting: String?,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onOpenFocus: () -> Unit,
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
) {
    val topic = selectedId?.let { topicById(it) }
    PlatformBackHandler(enabled = topic != null, onBack = onBack)

    // Hoisted here, not inside HomeScreen — HomeScreen leaves composition
    // while a topic is open (it's the null branch of the AnimatedContent
    // below), so state remembered inside it would reset to HOME on every
    // return from a topic. This call site survives that swap.
    var homeTab by remember { mutableStateOf(HomeTab.HOME) }

    AnimatedContent(
        targetState = topic?.id,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally(tween(Motion.PushIn)) { it / 3 } + fadeIn(tween(Motion.PopIn)) togetherWith
                    fadeOut(tween(Motion.PushOut))
            } else {
                fadeIn(tween(Motion.PopIn)) togetherWith
                    slideOutHorizontally(tween(Motion.PopOut)) { it / 3 } + fadeOut(tween(Motion.PopFade))
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "screen",
    ) { id ->
        val current = id?.let { topicById(it) }

        if (current == null) {
            HomeScreen(
                onOpenTopic = onSelect,
                onOpenFocus = onOpenFocus,
                greeting = greeting,
                mono = mono,
                onToggleTheme = onToggleTheme,
                tab = homeTab,
                onTabChange = { homeTab = it },
            )
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                // A minimal back affordance rather than a full app bar — the
                // title is already the first thing in the article.
                Row(
                    Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to topics",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TopicScreen(
                    topic = current,
                    lang = lang,
                    onLangChange = onLangChange,
                    onOpenTopic = onSelect,
                )
            }
        }
    }
}

@Composable
private fun TwoPaneLayout(
    selectedId: String,
    onSelect: (String) -> Unit,
    onOpenFocus: () -> Unit,
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
) {
    val topic = topicById(selectedId) ?: AllTopics.first()
    var query by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.widthIn(max = Layout.ListPaneWidth).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, top = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stacks",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onOpenFocus),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = StacksIcons.Play,
                        contentDescription = "Open focus timer",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = StacksIcons.Contrast,
                        contentDescription = if (mono) "Switch to the colour theme" else "Switch to the monochrome theme",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TopicListPane(
                query = query,
                onQueryChange = { query = it },
                selectedId = topic.id,
                onSelect = onSelect,
            )
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(Modifier.weight(1f).fillMaxSize()) {
            TopicScreen(
                topic = topic,
                lang = lang,
                onLangChange = onLangChange,
                onOpenTopic = onSelect,
                modifier = Modifier.widthIn(max = Layout.ReadingMaxWidth),
            )
        }
    }
}
