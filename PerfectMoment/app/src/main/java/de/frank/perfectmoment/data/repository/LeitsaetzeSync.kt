package de.frank.perfectmoment.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.ZIELE_LEITSAETZE_QUELLE
import de.frank.perfectmoment.data.settings.SecureSettings
import kotlinx.coroutines.Dispatchers
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

    suspend fun aktualisieren() = mutex.withLock {
        // Vor der Transaktion vollständig lesen. Fehler lassen den letzten guten Stand erhalten.
        val texte = withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(URI, null, null, null, null)
                ?: error("Ziele ist nicht erreichbar. Bitte die aktuelle Ziele-App installieren.")
            cursor.use {
                val symbol = it.getColumnIndexOrThrow("symbol")
                val text = it.getColumnIndexOrThrow("leitsatz")
                buildList {
                    while (it.moveToNext()) add(it.getString(symbol) to it.getString(text))
                }
            }
        }
        database.withTransaction {
            val dao = database.sessionDao()
            val vorhanden = dao.sessionFromSource(ZIELE_LEITSAETZE_QUELLE)
            val sessionId = vorhanden?.id ?: dao.insertSession(
                SessionEntity(
                    topic = "Leitsätze", startedAt = System.currentTimeMillis(),
                    durationMin = settings.sessionDurationMin, voiceName = "", providerId = settings.ttsProvider,
                    pauseRep = settings.pauseRepSeconds, pauseNext = settings.pauseNextSeconds,
                    reps = settings.repsPerQuestion, introContext = ZIELE_LEITSAETZE_QUELLE,
                    summary = "Leitsätze", summaryManual = true, custom = true,
                ),
            )
            val bisher = dao.getSession(sessionId)?.sorted()?.questions.orEmpty()
            if (bisher.map { it.emoji to it.text } != texte) {
                dao.deleteQuestions(sessionId)
                dao.insertQuestions(texte.mapIndexed { index, (symbol, text) ->
                    QuestionEntity(sessionId = sessionId, orderIndex = index, emoji = symbol, text = text)
                })
                dao.updateQuestionCount(sessionId, texte.size)
                dao.clearProgress(sessionId)
            }
        }
    }

    companion object {
        val URI: Uri = Uri.parse("content://de.frank.ziele.leitsaetze")
    }
}
