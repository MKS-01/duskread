package dev.mks.duskread.ui.pomodoro

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.pomodoro.PickableMinutes
import dev.mks.duskread.pomodoro.PomodoroState
import dev.mks.duskread.pomodoro.clockLabel
import dev.mks.duskread.pomodoro.rememberPomodoroController
import dev.mks.duskread.ui.PlatformBackHandler
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.WaveformMeter
import dev.mks.duskread.ui.theme.CodeStyle
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * The big-timer mode: a session used as its own destination rather than a
 * corner chip, for whenever the point is to actually stare at the clock. The
 * chip and this screen read the same [dev.mks.duskread.pomodoro.PomodoroController],
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
    var customLength by remember { mutableStateOf(false) }

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
                    imageVector = DuskReadIcons.Close,
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
                EyebrowHeader(
                    text = if (state.idle) "FOCUS" else "FOCUS · ${state.totalSeconds / 60} MIN",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(10.dp))

                if (state.idle) {
                    Text(
                        text = "Pick a length",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                } else {
                    Text(
                        text = state.clockLabel,
                        style = CodeStyle,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                if (!state.idle) {
                    Spacer(Modifier.height(20.dp))
                    // Elapsed is filled, remaining is not — the same construction
                    // as the readback progress, so one visual language covers
                    // audio and time alike.
                    WaveformMeter(
                        progress = state.elapsedFraction,
                        modifier = Modifier.height(26.dp),
                        barCount = 20,
                        barGap = 2.dp,
                    )
                }

                Spacer(Modifier.height(28.dp))

                if (state.idle) {
                    if (customLength) {
                        CustomLengthEntry(
                            onStart = { minutes -> controller.start(minutes) },
                            onCancel = { customLength = false },
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PickableMinutes.forEach { minutes ->
                                FocusOption(text = "$minutes min", onClick = { controller.start(minutes) })
                            }
                            FocusOption(text = "Custom", onClick = { customLength = true })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusOption(
                            text = if (state.running) "Pause" else "Resume",
                            primary = true,
                            onClick = { if (state.running) controller.pause() else controller.resume() },
                        )
                        FocusOption(
                            text = "Reset",
                            onClick = {
                                controller.reset()
                                customLength = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * What "Custom" on the length picker opens into: a bare minute count rather
 * than a full duration picker, since every other length in this app is
 * already just a number of minutes. Digits only, capped at 3 characters —
 * plenty for anything a focus session would reasonably run to.
 */
@Composable
private fun CustomLengthEntry(onStart: (Int) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AppTextField(
            value = text,
            onValueChange = { new -> if (new.length <= 3 && new.all(Char::isDigit)) text = new },
            placeholder = "MIN",
            modifier = Modifier.width(96.dp),
            mono = true,
            textAlign = TextAlign.Center,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { minutes?.takeIf { it > 0 }?.let(onStart) }),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusOption(text = "Start", primary = true, onClick = { minutes?.takeIf { it > 0 }?.let(onStart) })
            FocusOption(text = "Cancel", onClick = onCancel)
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

/**
 * A bordered pill, never filled — the same `.pill`/`.pill.sel` shape as the
 * sort chips on Readback and the length picker above this one. The "active"
 * option (Pause while running, one of the length choices once tapped) gets
 * the brighter border and text; nothing here is a filled button.
 */
@Composable
private fun FocusOption(text: String, onClick: () -> Unit, primary: Boolean = false) {
    val tone = if (primary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Inline))
            .border(Stroke.Hairline, tone, RoundedCornerShape(Radius.Inline))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
