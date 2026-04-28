package com.bestjournal.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R

/**
 * EU AI Act Art. 50 (Pflicht ab 02.08.2026) verlangt eine fuer Menschen erkennbare
 * Kennzeichnung von KI-generiertem oder KI-verbessertem Text.
 *
 * Architektur:
 * - DB-Text enthaelt den Suffix (rechtliche Pflicht — bleibt beim Export/Teilen erhalten)
 * - UI-Anzeige entfernt den Suffix vor TextField-Rendering und zeigt stattdessen
 *   den dezenten [AiImprovedFooter] (kleiner, halbtransparent, eigene Zeile)
 * - Beim Save-Flow wird der Suffix wieder angehaengt (siehe [reattachIfMissing])
 */
object AiImprovedSuffixHelper {

    /** Liefert den App-Marker-String (locale-stabil ueber R.string.ai_improved_suffix_marker). */
    fun marker(context: Context): String =
        context.getString(R.string.ai_improved_suffix_marker)

    /** Liefert den App-Suffix-String (mit fuehrenden Newlines). */
    fun suffix(context: Context): String =
        context.getString(R.string.ai_improved_suffix)

    /** Liefert true wenn der Text den Marker enthaelt — egal an welcher Stelle. */
    fun containsMarker(context: Context, text: String): Boolean =
        text.contains(marker(context))

    /**
     * Liefert den Text OHNE den Suffix-Block am Ende. Wenn kein Marker drin steht,
     * wird der Text unveraendert zurueckgegeben.
     */
    fun strip(context: Context, text: String): String {
        val m = marker(context)
        val idx = text.lastIndexOf(m)
        return if (idx >= 0) text.substring(0, idx).trimEnd() else text
    }

    /**
     * Haengt den Suffix wieder an, falls er fehlt. Rechtssicher fuer Save-Pfade
     * nach Editing — selbst wenn der Nutzer den Suffix versehentlich geloescht hat.
     */
    fun reattachIfMissing(context: Context, text: String): String =
        if (containsMarker(context, text)) text else text.trimEnd() + suffix(context)
}

/**
 * Dezenter KI-Hinweis unterhalb eines KI-verbesserten Inhalts.
 * Rendert den Marker in kleiner Schriftgroesse mit reduziertem Alpha (~55%) — gut
 * sichtbar genug fuer EU AI Act Art. 50, stoert aber nicht beim Lesen des Inhalts.
 */
@Composable
fun AiImprovedFooter(modifier: Modifier = Modifier) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.ai_improved_suffix_marker),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        textAlign = TextAlign.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    )
}

/** Composable-Wrapper fuer [AiImprovedSuffixHelper.strip] — bequem in der UI. */
@Composable
fun rememberStrippedText(text: String): String {
    val context = LocalContext.current
    return AiImprovedSuffixHelper.strip(context, text)
}

/** Composable-Wrapper fuer [AiImprovedSuffixHelper.containsMarker]. */
@Composable
fun rememberContainsAiSuffix(text: String): Boolean {
    val context = LocalContext.current
    return AiImprovedSuffixHelper.containsMarker(context, text)
}
