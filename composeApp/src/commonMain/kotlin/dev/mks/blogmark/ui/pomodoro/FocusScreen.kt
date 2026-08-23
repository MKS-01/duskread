package dev.mks.blogmark.ui.pomodoro

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.pomodoro.PickableMinutes
import dev.mks.blogmark.pomodoro.PomodoroState
import dev.mks.blogmark.pomodoro.clockLabel
import dev.mks.blogmark.pomodoro.rememberPomodoroController
import dev.mks.blogmark.ui.PlatformBackHandler
import dev.mks.blogmark.ui.common.WaveformMeter
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.CodeStyle
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.SectionLabel
import dev.mks.blogmark.ui.theme.Stroke

/**
 * The big-timer mode: a session used as its own destination rather than a
 * corner chip, for whenever the point is to actually stare at the clock. The
 * chip and this screen read the same [dev.mks.blogmark.pomodoro.PomodoroController],
 * so closing this never stops a running session — it only stops looking at it.
 *
 * Bottom-anchored rather than centred: the close button aside, everything a
 * thumb can reach lives in the lower third, same as the floating bar it sits
 * above. Centring the clock looked considered on a design file and useless on
 * a phone held one-handed.
 */
@Composable
fun FocusScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val controller = rememberPomodoroController()
    val state by controller.state.collectAsState()

    PlatformBackHandler(enabled = true, onBack = onClose)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BlogmarkIcons.Close,
                    contentDescription = "Close focus mode",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 56.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.idle) "Focus" else "Focus · ${state.totalSeconds / 60} min",
                    style = SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (state.idle) "Pick a length" else state.clockLabel,
                    style = CodeStyle,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                if (!state.idle) {
                    Spacer(Modifier.height(20.dp))
                    // Elapsed is filled, remaining is not — the same construction
                    // as the readback progress, so one visual language covers
                    // audio and time alike.
                    WaveformMeter(
                        progress = state.elapsedFraction,
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                    )
                }

                Spacer(Modifier.height(28.dp))

                if (state.idle) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PickableMinutes.forEach { minutes ->
                            FocusOption(text = "$minutes min", onClick = { controller.start(minutes) })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusOption(
                            text = if (state.running) "Pause" else "Resume",
                            primary = true,
                            onClick = { if (state.running) controller.pause() else controller.resume() },
                        )
                        FocusOption(text = "Reset", onClick = { controller.reset() })
                    }
                }
            }
        }
    }
}

/** How much of the chosen session has elapsed, for the meter — 0 while idle. */
private val PomodoroState.elapsedFraction: Float
    get() = if (idle) {
        0f
    } else {
        (1f - remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

@Composable
private fun FocusOption(text: String, onClick: () -> Unit, primary: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                Stroke.Hairline,
                if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Radius.Pill),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
