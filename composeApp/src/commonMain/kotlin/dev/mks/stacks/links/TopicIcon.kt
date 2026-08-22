package dev.mks.stacks.links

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Cloud
import compose.icons.feathericons.Code
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.GitBranch
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Music
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Rss
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Terminal
import compose.icons.feathericons.Zap

/**
 * A badge for a followed blog, picked from what its host says about it.
 *
 * A handful of well-known publishers get an icon that actually means
 * something — an Android blog gets the phone glyph, not a coin flip. Anything
 * else falls back to a small rotation, chosen by the host's own hash so the
 * same blog always lands on the same icon rather than reshuffling every sync.
 */
fun topicIcon(host: String): ImageVector {
    val lower = host.lowercase()
    fun matches(vararg words: String) = words.any { it in lower }

    return when {
        matches("android", "google") -> FeatherIcons.Smartphone
        matches("spotify", "music") -> FeatherIcons.Music
        matches("anthropic", "claude", "openai") -> FeatherIcons.Cpu
        matches("react", "callstack", "swmansion") -> FeatherIcons.Code
        matches("github") -> FeatherIcons.GitBranch
        matches("aws", "cloud", "azure") -> FeatherIcons.Cloud
        else -> FallbackIcons[lower.hashCode().mod(FallbackIcons.size)]
    }
}

private val FallbackIcons = listOf(
    FeatherIcons.Rss,
    FeatherIcons.Globe,
    FeatherIcons.BookOpen,
    FeatherIcons.Zap,
    FeatherIcons.Layers,
    FeatherIcons.Coffee,
    FeatherIcons.Terminal,
    FeatherIcons.Radio,
)
