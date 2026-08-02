package dev.mks.stacks.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.model.Lang
import dev.mks.stacks.ui.theme.CodeStyle
import dev.mks.stacks.ui.theme.LocalCodePalette

/**
 * Tabbed source listing. The selected language is hoisted by the caller so it
 * persists as you move between topics — pick Kotlin once, stay in Kotlin.
 */
@Composable
fun CodeBlock(
    code: Map<Lang, String>,
    selected: Lang,
    onSelect: (Lang) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCodePalette.current
    val available = Lang.entries.filter { code.containsKey(it) }
    if (available.isEmpty()) return

    val active = if (selected in available) selected else available.first()
    val source = code.getValue(active)
    val highlighted = remember(source, active, palette) { highlight(source, active, palette) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            available.forEach { lang ->
                LangTab(lang, lang == active) { onSelect(lang) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            Modifier
                .fillMaxWidth()
                .background(palette.background)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(text = highlighted, style = CodeStyle)
        }
    }
}

@Composable
private fun LangTab(lang: Lang, active: Boolean, onClick: () -> Unit) {
    Text(
        text = lang.label,
        fontSize = 12.5.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
