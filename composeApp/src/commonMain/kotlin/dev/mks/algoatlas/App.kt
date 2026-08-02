package dev.mks.algoatlas

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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.mks.algoatlas.content.AllTopics
import dev.mks.algoatlas.content.topicById
import dev.mks.algoatlas.data.rememberUserPrefs
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.ui.PlatformBackHandler
import dev.mks.algoatlas.ui.TopicListPane
import dev.mks.algoatlas.ui.TopicScreen
import dev.mks.algoatlas.ui.home.HomeScreen
import dev.mks.algoatlas.ui.onboarding.Onboarding
import dev.mks.algoatlas.ui.pomodoro.FocusScreen
import dev.mks.algoatlas.ui.theme.AlgoAtlasTheme
import dev.mks.algoatlas.ui.theme.AtlasIcons
import dev.mks.algoatlas.ui.theme.Layout
import dev.mks.algoatlas.ui.theme.Motion

/** Below this width there is no room for two panes, so we navigate instead. */
@Composable
fun App() {
    var dark by remember { mutableStateOf<Boolean?>(null) }
    val systemDark = isSystemInDarkTheme()
    val isDark = dark ?: systemDark

    val prefs = rememberUserPrefs()

    AlgoAtlasTheme(dark = isDark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                            isDark = isDark,
                            onToggleTheme = { dark = !isDark },
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
                            isDark = isDark,
                            onToggleTheme = { dark = !isDark },
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
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val topic = selectedId?.let { topicById(it) }
    PlatformBackHandler(enabled = topic != null, onBack = onBack)

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
                isDark = isDark,
                onToggleTheme = onToggleTheme,
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
    isDark: Boolean,
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
                    text = "Algo Atlas",
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
                        imageVector = AtlasIcons.Play,
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
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle theme",
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
