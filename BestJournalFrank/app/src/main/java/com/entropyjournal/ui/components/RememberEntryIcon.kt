package com.entropyjournal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.entropyjournal.data.prefs.EntryIconCache
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.domain.usecase.ClassifyEntryIconUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IconClassifierEntryPoint {
    fun classifier(): ClassifyEntryIconUseCase
    fun cache(): EntryIconCache
}

/**
 * Composable-Hook der fuer einen Tagebucheintrag den passenden Icon-Schluessel liefert.
 *
 * Logik:
 *  1. Sofort: Stichwort-Mapping (instant, deterministisch)
 *  2. Cache-Lookup: wenn KI-Klassifikation fuer diesen Eintrag schon gemacht wurde,
 *     den gecachten Wert nutzen (ueberschreibt den Fallback)
 *  3. Async: wenn Cache leer und Internet+Key da, Gemini-Call → Cache fuellen → Icon
 *     wird automatisch via State-Update aktualisiert
 *
 * @param entry Tagebucheintrag
 * @return Aktueller iconKey aus [LucideIconPool]
 */
@Composable
fun rememberEntryIconKey(entry: JournalEntry): String {
    val context = LocalContext.current
    val accessors = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            IconClassifierEntryPoint::class.java,
        )
    }
    val cache = accessors.cache()
    val classifier = accessors.classifier()

    val combinedText = remember(entry.title, entry.displayText) {
        buildString {
            entry.title?.let { append(it).append(' ') }
            append(entry.displayText)
        }
    }
    val fallbackKey = remember(combinedText) { EntryIconResolver.resolveKey(combinedText) }

    var iconKey by remember(entry.id) {
        mutableStateOf(cache.get(entry.id) ?: fallbackKey)
    }

    LaunchedEffect(entry.id, combinedText.hashCode()) {
        // Wenn schon gecacht, nichts tun
        if (cache.get(entry.id) != null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            classifier.classify(entry.title, entry.displayText).onSuccess { newKey ->
                if (newKey != null) {
                    cache.put(entry.id, newKey)
                    withContext(Dispatchers.Main) {
                        iconKey = newKey
                    }
                }
            }
        }
    }

    return iconKey
}
