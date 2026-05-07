package de.frank.entropyreducer.presentation.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Share-Sheet-Empfaenger (Spec §17).
 * Transparent, ohne UI — uebernimmt den Text aus dem Send-Intent, schreibt
 * sofort einen EntropyEntry mit `source = SHARE_SHEET` und schliesst sich.
 *
 * Toast bestaetigt visuell — keine Activity-Navigation, kein Bildschirm-Flicker.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject lateinit var processEntry: ProcessEntryUseCase

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = extractText(intent)
        if (text.isNullOrBlank()) {
            Toast.makeText(this, "Kein Text zum Importieren gefunden.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(this, "Eintrag gespeichert — wird verarbeitet.", Toast.LENGTH_SHORT).show()
        scope.launch {
            // Best-Effort — Fehler loggen wir nicht direkt, ProcessEntryUseCase
            // schreibt schon ein Roh-Entry und ruft KI im Hintergrund auf.
            runCatching { processEntry(rawTranscript = text, source = EntrySource.SHARE_SHEET) }
        }
        finish()
    }

    private fun extractText(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!raw.isNullOrBlank()) return raw
        // Fallback: einige Apps senden einen Subject statt Text
        return intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()
    }
}
