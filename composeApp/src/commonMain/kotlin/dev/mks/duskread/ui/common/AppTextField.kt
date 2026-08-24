package dev.mks.duskread.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius

/**
 * The one text field shape in the app: a hairline-bordered box with square
 * (well — 10dp, the same as every other bordered control) corners rather
 * than the fully circular pill each input used to draw for itself. A pill
 * is the shape of a button, and every one of these fields sat next to actual
 * pill buttons (Save, Follow, Add links) that needed to look different from
 * the thing they acted on — this is what makes that difference legible.
 *
 * One component rather than four near-identical copies (Saved's paste field,
 * the feed-follow field, the settings name field, the import box) means a
 * future change to how a field looks only has one place to happen.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp? = null,
    mono: Boolean = false,
    fontSize: TextUnit = 13.5.sp,
    textAlign: TextAlign = TextAlign.Start,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val lineHeight = if (singleLine) fontSize else (fontSize.value * 1.35f).sp
    val family = if (mono) Mono else FontFamily.Default

    Row(
        modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Inline))
            .padding(horizontal = 14.dp, vertical = if (singleLine) 10.dp else 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Box(Modifier.weight(1f).let { if (minHeight != null) it.heightIn(min = minHeight) else it }) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = family,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    textAlign = textAlign,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
        }
        trailing?.let { it() }
    }
}
