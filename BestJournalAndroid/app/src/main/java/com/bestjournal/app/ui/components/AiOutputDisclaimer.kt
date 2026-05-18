package com.bestjournal.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R

/**
 * Wiederverwendbare Disclaimer-Zeile, die unter jeder KI-generierten Ausgabe
 * (Dashboard-Analyse, Retrospektive, Text-Verbesserung, Summary) angezeigt wird.
 *
 * HWG §3-Mitigation: erinnert den Nutzer dass KI-Ausgaben keine professionelle
 * Beratung ersetzen. Kombiniert mit den existierenden AiGeneratedBadge-
 * Komponenten ist das die DSGVO-/HWG-/EU-AI-Act-konforme Doppellinie:
 * 1. "KI-generiert" Badge (Art. 50 AI Act)
 * 2. Therapie-Disclaimer (HWG §3)
 */
@Composable
fun AiOutputDisclaimer(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.ai_output_health_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
