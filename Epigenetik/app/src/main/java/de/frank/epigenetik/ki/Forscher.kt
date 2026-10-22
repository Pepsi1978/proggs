package de.frank.epigenetik.ki

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.frank.epigenetik.MainActivity
import de.frank.epigenetik.R
import de.frank.epigenetik.auth.AuthErrorKind
import de.frank.epigenetik.auth.CodexAuthException
import de.frank.epigenetik.auth.CodexAuthManager
import de.frank.epigenetik.data.Einstellungen
import de.frank.epigenetik.data.Repository
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Status
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.Zielkatalog
import de.frank.epigenetik.data.normalisiere
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Live-Stand einer laufenden Recherche (nur im Speicher, für Balken und Zeichenzähler). */
data class LiveStand(val start: Long, val zeichen: Int = 0, val schritt: String = "Recherchiert im Internet …")

object Live {
    private val _staende = MutableStateFlow<Map<Long, LiveStand>>(emptyMap())
    val staende: StateFlow<Map<Long, LiveStand>> = _staende.asStateFlow()
    fun start(id: Long) = _staende.update { it + (id to LiveStand(System.currentTimeMillis())) }
    fun zeichen(id: Long, n: Int) = _staende.update { m ->
        val s = m[id] ?: return@update m
        m + (id to s.copy(zeichen = s.zeichen + n, schritt = if (s.zeichen + n > 0) "Schreibt die Auswertung …" else s.schritt))
    }
    fun ende(id: Long) = _staende.update { it - id }
}

/**
 * Führt eine Recherche im Hintergrund aus. WorkManager statt eigenem Dienst, damit auch Aufträge von Jarvis
 * starten dürfen, während die App nicht offen ist (Android verbietet dann den Start eines Vordergrund-Dienstes).
 */
class Forscher(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val id = inputData.getLong(ID, -1)
        val name = Repository.get(applicationContext).dao.hole(id)?.name ?: "Recherche"
        val typ = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        return ForegroundInfo(LAUFEND_BASIS + id.toInt(), meldung(applicationContext, "Recherchiert „$name“ …", null, laufend = true), typ)
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(ID, -1)
        val repo = Repository.get(applicationContext)
        val r = repo.dao.hole(id) ?: return Result.success()
        if (r.fertig && r.json.isNotBlank()) return Result.success()
        // Hält den Prozess bei langen Recherchen am Leben; aus dem Hintergrund (Jarvis) kann Android das verweigern – dann
        // läuft die Arbeit trotzdem als Expedited-Job weiter.
        runCatching { setForeground(getForegroundInfo()) }
        val e = Einstellungen.get(applicationContext)
        val auth = CodexAuthManager(applicationContext)
        if (!auth.kannDenken(e.modell)) {
            repo.dao.setzeStatus(id, Status.FEHLER.name, if (e.modell.istAnthropic) "KI nicht bereit: In den Einstellungen den Anthropic-API-Schlüssel eintragen." else "KI nicht verbunden: In den Einstellungen mit ChatGPT verbinden.")
            return Result.failure()
        }
        return grenze(e.parallel).withPermit {
            repo.dao.setzeStatus(id, Status.LAEUFT.name)
            Live.start(id)
            try {
                val json = EpigenetikKi(auth, e).recherchiere(r, kontext(repo, r)) { Live.zeichen(id, it.length) }
                repo.ergebnisSpeichern(id, json)
                val fertig = repo.dao.hole(id)
                melden(applicationContext, id, "${fertig?.emoji ?: ""} ${fertig?.name ?: r.name} ist fertig".trim(), fertig?.kurz.orEmpty())
                Result.success()
            } catch (c: CancellationException) {
                repo.dao.setzeStatus(id, Status.FEHLER.name, "Abgebrochen.")
                throw c
            } catch (t: Throwable) {
                KiLog.fehler("Recherche $id „${r.name}“ gescheitert (Versuch ${runAttemptCount + 1})", t)
                val netz = t is java.io.IOException || (t is CodexAuthException && t.kind == AuthErrorKind.NETWORK)
                if (netz && runAttemptCount < 2) {
                    repo.dao.setzeStatus(id, Status.WARTET.name, "Netzwerkproblem – neuer Versuch folgt.")
                    Result.retry()
                } else {
                    val text = when {
                        t is CodexAuthException && t.kind == AuthErrorKind.REAUTH -> "Die ChatGPT-Anmeldung ist abgelaufen. Bitte in den Einstellungen neu verbinden."
                        t is CodexAuthException && t.kind == AuthErrorKind.QUOTA -> "Das ChatGPT-Kontingent ist gerade aufgebraucht. Später erneut versuchen."
                        else -> t.message ?: "Die Recherche ist fehlgeschlagen."
                    }
                    repo.dao.setzeStatus(id, Status.FEHLER.name, text)
                    melden(applicationContext, id, "Recherche „${r.name}“ gescheitert", text)
                    Result.failure()
                }
            } finally {
                Live.ende(id)
            }
        }
    }

    /** Sammelt, was die KI aus der Bibliothek wissen sollte (Wirkungen und Ziele bauen auf vorhandenen Recherchen auf). */
    private suspend fun kontext(repo: Repository, r: de.frank.epigenetik.data.Recherche): Kontext {
        val alle = repo.dao.alleJetzt()
        val mittel = alle.filter { it.typEnum == Typ.MITTEL && it.fertig }
        val stack = mittel.filter { it.imStack }.map { m ->
            m to repo.dao.wirkungenVon(m.id).joinToString { "${it.schalter} ${it.richtung} ${it.staerke}" }
        }
        val ziele = alle.filter { it.typEnum == Typ.ZIEL }.map { it.name }
        val schalterIds = when (r.typEnum) {
            Typ.WIRKUNG -> {
                val k = normalisiere(r.name)
                Schalterkatalog.alle.filter { s ->
                    normalisiere(s.id) in k.split(' ') || k.contains(normalisiere(s.name)) ||
                        (k.contains("sirtuin") && s.id.startsWith("SIRT")) || (k.contains("nad") && s.id == "NAD") ||
                        (k.contains("methyl") && s.gruppe.name == "DNA_METHYLIERUNG") || (k.contains("autophag") && s.id == "AUTOPHAGIE") ||
                        (k.contains("entzuend") && s.gruppe.name == "ENTZUENDUNG") || (k.contains("telomer") && s.id == "TERT")
                }.map { it.id }
            }
            Typ.ZIEL -> Zielkatalog.alle.firstOrNull { normalisiere(it.name) == r.schluessel }?.schalter.orEmpty()
            else -> emptyList()
        }
        val treffer = schalterIds.flatMap { repo.dao.mittelFuer(it) }
        return Kontext(bibliothek = mittel, treffer = treffer, stack = stack, ziele = ziele)
    }

    companion object {
        private const val ID = "id"
        private const val KANAL = "recherchen"
        private const val LAUFEND_BASIS = 10_000
        private const val FERTIG_BASIS = 20_000
        @Volatile private var semaphore: Semaphore? = null

        private fun grenze(n: Int): Semaphore = semaphore ?: synchronized(this) { semaphore ?: Semaphore(n).also { semaphore = it } }

        fun name(id: Long) = "recherche-$id"

        fun einreihen(context: Context, id: Long) {
            val anfrage = OneTimeWorkRequestBuilder<Forscher>()
                .setInputData(workDataOf(ID to id))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("recherche")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(name(id), ExistingWorkPolicy.KEEP, anfrage)
        }

        fun abbrechen(context: Context, id: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(name(id))
        }

        fun kanalAnlegen(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(KANAL, "Recherchen", NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null) })
        }

        private fun meldung(context: Context, titel: String, text: String?, laufend: Boolean, id: Long = -1): Notification {
            val oeffnen = PendingIntent.getActivity(
                context, id.toInt().coerceAtLeast(0),
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_ID, id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val b = NotificationCompat.Builder(context, KANAL)
                .setSmallIcon(R.drawable.ic_stat_epigenetik)
                .setContentTitle(titel)
                .setContentIntent(oeffnen)
                .setOnlyAlertOnce(true)
            if (laufend) b.setProgress(0, 0, true).setOngoing(true).setSilent(true)
            else b.setAutoCancel(true).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            return b.build()
        }

        private fun melden(context: Context, id: Long, titel: String, text: String) {
            runCatching {
                context.getSystemService(NotificationManager::class.java).notify(FERTIG_BASIS + id.toInt(), meldung(context, titel, text, laufend = false, id = id))
            }
        }
    }
}
