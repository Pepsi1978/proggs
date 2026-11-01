package de.frank.perfectmoment.data.repository

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.room.withTransaction
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.ZIELE_LEITSAETZE_QUELLE
import de.frank.perfectmoment.data.settings.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Direkt vom Ziele-Provider lesen: kein KI-Aufruf, keine Textbearbeitung, kein Netz nötig. */
class LeitsaetzeSync(
    private val context: Context,
    private val database: PerfectMomentDatabase,
    private val settings: SecureSettings,
) {
    private val mutex = Mutex()

    private suspend fun sessionSicherstellen(): Long = database.withTransaction {
        val dao = database.sessionDao()
        val vorhanden = dao.sessionsFromSource(ZIELE_LEITSAETZE_QUELLE)
        val id = vorhanden.firstOrNull()?.id ?: dao.insertSession(
            SessionEntity(
                topic = "Leitsätze", startedAt = System.currentTimeMillis(),
                durationMin = settings.sessionDurationMin, voiceName = "", providerId = settings.ttsProvider,
                pauseRep = settings.pauseRepSeconds, pauseNext = settings.pauseNextSeconds,
                reps = settings.repsPerQuestion, introContext = ZIELE_LEITSAETZE_QUELLE,
                summary = "Leitsätze", summaryManual = true, custom = true,
            ),
        )
        vorhanden.drop(1).forEach { dao.deleteSession(it.id) }
        id
    }

    suspend fun aktualisieren() = mutex.withLock {
        val sessionId = sessionSicherstellen()
        // Vor der Transaktion vollständig lesen. Fehler lassen den letzten guten Stand erhalten.
        val texte = withContext(Dispatchers.IO) {
            val signal = CancellationSignal()
            val handler = Handler(Looper.getMainLooper())
            val abbrechen = Runnable { signal.cancel() }
            handler.postDelayed(abbrechen, 5_000L)
            try {
                val cursor = context.contentResolver.query(URI, null, null, null, null, signal)
                    ?: error("Ziele ist nicht erreichbar. Bitte die aktuelle Ziele-App installieren.")
                cursor.use {
                    val symbol = it.getColumnIndexOrThrow("symbol")
                    val text = it.getColumnIndexOrThrow("leitsatz")
                    buildList {
                        while (it.moveToNext()) {
                            signal.throwIfCanceled()
                            add(it.getString(symbol) to it.getString(text))
                        }
                    }
                }
            } finally {
                handler.removeCallbacks(abbrechen)
            }
        }
        currentCoroutineContext().ensureActive()
        database.withTransaction {
            val dao = database.sessionDao()
            // Ein Restore kann während des Provider-Abrufs erfolgt sein: die Quelle erneut auflösen.
            val zielSessionId = dao.sessionFromSource(ZIELE_LEITSAETZE_QUELLE)?.id ?: sessionId
            val bisher = dao.getSession(zielSessionId)?.sorted()?.questions.orEmpty()
            if (bisher.map { it.emoji to it.text } != texte) {
                dao.deleteQuestions(zielSessionId)
                dao.insertQuestions(texte.mapIndexed { index, (symbol, text) ->
                    QuestionEntity(sessionId = zielSessionId, orderIndex = index, emoji = symbol, text = text)
                })
                dao.updateQuestionCount(zielSessionId, texte.size)
                dao.clearProgress(zielSessionId)
            }
            zielSessionId
        }
    }

    companion object {
        val URI: Uri = Uri.parse("content://de.frank.ziele.leitsaetze")
    }
}
