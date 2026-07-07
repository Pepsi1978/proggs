package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/** Pille mit Kategorie-Farbe als Hintergrund-Tint, Text in voller Akzentfarbe. */
@Composable
fun EntropyCategoryPill(category: EntropyCategory, modifier: Modifier = Modifier) {
    val color = category.color()
    Text(
        text = category.label(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
