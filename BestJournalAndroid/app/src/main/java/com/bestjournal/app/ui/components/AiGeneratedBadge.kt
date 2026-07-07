package com.bestjournal.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.R

/**
 * Inline variant — no background pill, no click, same typography as labelSmall.
 * Designed to sit next to a timestamp on the same baseline. Use wherever the user
 * sees "Letzte Aktualisierung am ..." so the AI provenance is always visible
 * whenever fresh AI content is shown.
 */
@Composable
fun AiGeneratedBadgeInline(modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.ai_generated_badge),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

/**
 * In-App-Kennzeichnung fuer KI-generierten Inhalt (H4 — EU AI Act Art. 50, Pflicht ab 02.08.2026).
 *
 * Platziert man oberhalb oder neben jedem KI-Output:
 *  - Dashboard-Zusammenfassung
 *  - Wochen-/Monats-/Jahresrueckblick
 *  - Textverbesserung (Improve)
 *
 * Bei Klick auf den Badge wird ein kleiner Tooltip/Hinweis ein-/ausgeblendet der erklaert
 * dass der Inhalt von Gemini erstellt wurde und Fehler enthalten kann.
 *
 * Rechtsgrundlage: Art. 50 Abs. 2 + Abs. 4 EU AI Act — KI-generierte Inhalte muessen
 * als solche erkennbar sein, wenn sie Nutzern angezeigt werden.
 */
@Composable
fun AiGeneratedBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    // Color-of-choice: Primary-Farbschema + light Tint. Keine Warnung, nur freundliche Info.
    val badgeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val badgeFg = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(if (compact) 12.dp else 14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.ai_generated_badge),
                color = badgeFg,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 10.sp else 11.sp,
            )
        }

        if (expanded) {
            Spacer(Modifier.size(6.dp))
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(R.string.ai_generated_tooltip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compact) 10.sp else 11.sp,
                    lineHeight = if (compact) 14.sp else 15.sp,
                )
            }
        }
    }
}
