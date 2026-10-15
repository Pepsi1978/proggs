package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.CalendarRepository
import de.frank.entropyreducer.data.repository.ZeppBodyRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.data.remote.oauth.WhoopReauthRequiredException
import de.frank.entropyreducer.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Zentraler Daten-Sync. Trennt bewusst ZWEI Sync-Arten mit unterschiedlichem Takt
 * (Frank-Wunsch 2026-06-19, Sync-Synchronitaets-Umbau Etappe 1.1):
 *
 * 1. **Drive-Backup (Restore + Upload)** — klein (JSON), traegt die 1:1-Multi-Device-
 *    Synchronitaet (Aufgaben, Gewohnheiten, Texte ...). Laeuft bei JEDEM App-Start und
 *    JEDEM Foreground-Wechsel SOFORT (kein Throttle). Siehe [syncDriveNow].
 *
 * 2. **Fitness-APIs (Whoop/Oura/Health Connect/Kalender)** — seit 2026-10-13 (Frank-Wunsch:
 *    "Werte sollen immer aktuell sein") laufend: alle 30 Minuten im Hintergrund, beim Oeffnen der
 *    App (wenn aelter als 15 Minuten), bei jedem Oeffnen des Biomarker-Reiters und auf Anstoss von
 *    Jarvis ([syncApisNow]). Davor (2026-06-01 bis 2026-10-13) nur alle 8 h oder im Biomarker-Reiter.
 *    Seit 2026-10-15 entscheidet fuer Whoop allein [WhoopAbrufPlan]: abgerufen wird, wenn ein neuer
 *    Schlaf zu erwarten ist oder der neueste noch nicht fertig berechnet ist — nicht nach der Uhr. Die
 *    anderen Quellen gleichen unabhaengig davon weiter ab.
 *
 * Vorgeschichte: Frueher lief BEIDES gemeinsam und nur alle 8 h (`syncIfStale`/`syncAllSources`).
 * Dadurch konnte eine Aenderung auf Geraet A nicht "3 Minuten spaeter" auf Geraet B ankommen —
 * der Restore lief schlicht nicht. Der 8-h-Throttle bleibt jetzt NUR fuer die teuren APIs.
 *
 * Zwei getrennte Mutexe: Ein laufender (langsamer) API-Sync darf den schnellen Drive-Sync
 * NICHT blockieren und umgekehrt — ein Biomarker-Tab-Klick soll sofort die APIs ziehen koennen,
 * auch wenn gerade ein Drive-Sync laeuft.
 *
 * Hinweis (bugs/android/kotlin.md §2.1): die `runCatching`-Bloecke um die einzelnen Sync-Calls
 * sind das bewusst erhaltene Bestandsverhalten (frueher `syncAllSources`). Die Haupt-Aufrufer
 * laufen im app-weiten Scope (nie gecancelt); die fachlichen Repos behandeln Cancellation selbst.
 */
@Singleton
class ForegroundSyncManager
@Inject
constructor(
    private val secrets: EncryptedSecretsStore,
    private val syncEntries: SyncEntriesUseCase,
    private val coordinator: SyncCoordinator,
    private val oauth: OAuthService,
    private val whoop: WhoopRepository,
    private val oura: OuraRepository,
    private val amazfit: AmazfitRepository,
    private val calendar: CalendarRepository,
    private val zeppBodyRepository: ZeppBodyRepository,
    private val appSettings: AppSettings,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val driveMutex = Mutex()
    private val apiMutex = Mutex()

    @Volatile private var letzterBericht: ApiSyncBericht? = null
    @Volatile private var letzterAnlass: WhoopAbrufPlan.Anlass? = null

    /**
     * Vollabgleich beim App-Start / Foreground-Wechsel: Drive IMMER (sofort), Fitness-APIs nur
     * wenn der 8-h-Timer abgelaufen ist. Reihenfolge: erst Drive (Restore holt fremde
     * Geraete-Aenderungen + sichert den eigenen Stand), dann ggf. die APIs.
     */
    suspend fun syncForeground(minIntervalMs: Long = FOREGROUND_SYNC_MIN_INTERVAL_MS) {
        syncDriveNow()
        syncApisIfStale(minIntervalMs)
    }

    /**
     * Drive-Restore + Backup. Laeuft bei JEDEM Start/Foreground sofort (kein Throttle).
     * Restore VOR Upload (Frank-Wunsch 2026-06-02): zuerst fremde Geraete-Aenderungen holen,
     * dann den eigenen Stand hochladen. Loop-Vorlagen erzeugen keine Aufgaben mehr automatisch.
     * Bei parallelem Aufruf (Lock belegt) kehrt die Methode sofort zurueck.
     */
    suspend fun syncDriveNow() {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return
        if (!driveMutex.tryLock()) {
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync laeuft bereits -> uebersprungen")
            return
        }
        try {
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync START (Restore -> Heilung -> Upload)")
            val restoreResult = syncEntries.restoreFromDrive()
            restoreResult.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                Diag.w(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Restore fehlgeschlagen; Upload zum Schutz des Remote-Stands abgebrochen", error)
                return
            }
            val uploadResult = coordinator.syncNowAndWait()
            uploadResult.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                Diag.w(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Upload fehlgeschlagen", error)
                return
            }
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync FERTIG")
        } finally {
            driveMutex.unlock()
        }
    }

    /**
     * Fitness-API-Sync nur, wenn seit dem letzten Abgleich mindestens [minIntervalMs] vergangen
     * sind (Standard 15 Minuten). Wird beim Start/Foreground aufgerufen. Wenn noch nie gesynced
     * wurde (Timer 0), wird synchronisiert.
     */
    suspend fun syncApisIfStale(minIntervalMs: Long) {
        val lastMs = appSettings.lastRefreshFooterAtMsFlow.first()
        val now = System.currentTimeMillis()
        if (lastMs > 0L && now - lastMs < minIntervalMs) {
            Diag.i(
                DiagnosticArea.APP,
                "ForegroundSync",
                "API-Sync uebersprungen (noch frisch: ${(now - lastMs) / 60000} min < ${minIntervalMs / 60000} min)",
            )
            return
        }
        syncApisNow("App geöffnet", WhoopAbrufPlan.Anlass.VORDERGRUND)
    }

    /**
     * Voller Fitness-API-Abgleich (Whoop/Oura/Waage/Health Connect/Kalender). Aufrufer: App-Start und
     * Foreground (wenn aelter als 15 Minuten), Biomarker-Reiter, Aktualisieren-Knopf, der
     * Hintergrund-Abgleich alle 30 Minuten ([de.frank.entropyreducer.workers.BiomarkerSyncWorker])
     * und Jarvis ueber die Bruecke.
     *
     * Umbau 2026-10-13 (Direktive 3, Whoop seit 09.10. ohne Daten):
     * - JEDE Quelle wird einzeln bewertet. Frueher brach der erste Fehler (Whoop-Anmeldung, "Zepp
     *   antwortet nicht") die Auswertung ab: Fusszeile und Timer blieben tagelang stehen, obwohl
     *   Oura und Health Connect liefen — und niemand erfuhr, WELCHE Quelle hing.
     * - Das Ergebnis je Quelle wird gespeichert ([ApiSyncBericht]) und geht an die Fusszeile und an
     *   Jarvis.
     * - Laeuft schon ein Abgleich, WARTET der Aufrufer auf dessen Ergebnis, statt mit "uebersprungen"
     *   so zu tun, als waere abgeglichen worden.
     * - Jede Quelle hat ein Zeitlimit, damit eine haengende Quelle die Sperre nicht ewig haelt.
     */
    suspend fun syncApisNow(
        reason: String = "Biomarker-Tab",
        anlass: WhoopAbrufPlan.Anlass = WhoopAbrufPlan.Anlass.AUTOMATIK,
    ): ApiSyncBericht {
        val angefragtMs = System.currentTimeMillis()
        return apiMutex.withLock {
            // Ein gerade beendeter Lauf zaehlt nur, wenn er Whoop mindestens so gruendlich behandelt hat (ein
            // Lauf ohne Whoop ersetzt keinen, in dem Whoop faellig sein koennte).
            val frisch = letzterBericht?.takeIf {
                it.versuchMs >= angefragtMs && anlass != WhoopAbrufPlan.Anlass.AUSDRUECKLICH &&
                    rang(letzterAnlass) >= rang(anlass)
            }
            if (frisch != null) {
                Diag.i(DiagnosticArea.APP, "ForegroundSync", "API-Sync ($reason): Ergebnis des gerade beendeten Laufs übernommen")
                frisch
            } else {
                fuehreApiSyncAus(reason, anlass)
            }
        }
    }

    /**
     * Startet den Abgleich im app-weiten Scope. Fuer Aufrufer mit eigenem Zeitlimit (Jarvis-Bruecke):
     * laeuft das Limit ab, arbeitet der Abgleich trotzdem zu Ende, statt mitten im Schreiben
     * abgebrochen zu werden.
     */
    fun starteApiSync(reason: String, anlass: WhoopAbrufPlan.Anlass): Deferred<ApiSyncBericht> =
        applicationScope.async { syncApisNow(reason, anlass) }

    /**
     * Whoop-Stand fuer Jarvis und die Anzeige, ohne Netz: wann zuletzt abgerufen, bis wann der neueste
     * Schlaf reicht, ob er fertig berechnet ist und wann die App fruehestens wieder bei Whoop nachfragt.
     */
    suspend fun whoopUebersicht(): WhoopUebersicht {
        val stand = appSettings.whoopAbrufStandFlow.first()
        val jetzt = System.currentTimeMillis()
        val e = WhoopAbrufPlan.entscheide(
            stand, WhoopAbrufPlan.Anlass.AUTOMATIK, jetzt, java.time.LocalDate.now().toString(),
            ouraSchlafEndeMs(), oauth.whoopNeedsReauth(),
        )
        return WhoopUebersicht(
            letzterErfolgMs = maxOf(stand.letzterErfolgMs, appSettings.lastWhoopSyncMsFlow.first()),
            schlafEndeMs = stand.schlafEndeMs,
            schlafVollstaendig = stand.schlafVollstaendig,
            naechsterAbrufFruehestensMs = if (e.abrufen) jetzt else e.fruehestensMs,
            grund = e.grund,
        )
    }

    /**
     * Ende des neuesten Oura-Hauptschlafs (heute oder gestern), 0 = keiner. Nur ein Hinweis fuer die
     * Whoop-Steuerung: Kennt Oura schon einen neueren Schlaf, lohnt es sich, Whoop frueher zu fragen.
     */
    private suspend fun ouraSchlafEndeMs(): Long = runCatching {
        val heute = java.time.LocalDate.now()
        listOf(heute, heute.minusDays(1))
            .flatMap { oura.observeSleepDetailsForDay(it.toString()).first() }
            .filter { (it.totalSleepSeconds ?: 0) >= 3 * 3600 }
            .mapNotNull { s -> s.bedtimeEnd?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } }
            .maxOrNull() ?: 0L
    }.getOrDefault(0L)

    /**
     * Ergebnis des letzten Abgleichs (auch aus einem frueheren Prozess), null = noch nie gelaufen.
     * Ein Whoop-Abruf, der danach ausserhalb des Abgleichs glueckte (Abruf direkt nach neuer Anmeldung,
     * "Alles neu laden"), ersetzt einen alten Whoop-Fehler sofort (2026-10-15): sonst meldete Jarvis
     * "Anmeldung abgelaufen, letzter Erfolg 11:24" neben "zuletzt abgerufen 16:22".
     */
    suspend fun letzterStand(): ApiSyncBericht? {
        val gespeichert = letzterBericht
            ?: ApiSyncBericht.ausJson(appSettings.biomarkerSyncStatusFlow.first())?.also { letzterBericht = it }
            ?: return null
        return gespeichert.mitSpaeteremWhoopErfolg(
            letzterOkMs = appSettings.lastWhoopSyncMsFlow.first(),
            anmeldungAbgelaufen = oauth.whoopNeedsReauth(),
            fehlversuche = appSettings.whoopAbrufStandFlow.first().fehlversuche,
        )
    }

    private suspend fun fuehreApiSyncAus(reason: String, anlass: WhoopAbrufPlan.Anlass): ApiSyncBericht {
        Diag.i(DiagnosticArea.APP, "ForegroundSync", "API-Sync START ($reason)")
        val vorher = letzterStand()
        val whoopVerbunden = oauth.loadWhoopAuthState().isAuthorized
        // Frank-Wunsch 2026-10-15: Whoop nur, wenn es etwas Neues geben kann (neuer Schlaf moeglich, neuester
        // Schlaf noch nicht fertig) — jeder Abruf kostet einen Token-Refresh, und jeder Refresh, dessen Antwort
        // verloren geht, kostet die Anmeldung. Die Regeln stehen in WhoopAbrufPlan; die anderen Quellen laufen
        // unabhaengig davon weiter.
        val letzterWhoopOkMs = appSettings.lastWhoopSyncMsFlow.first()
        val whoopReauth = oauth.whoopNeedsReauth()
        val whoopAbrufStand = appSettings.whoopAbrufStandFlow.first()
        val whoopEntscheidung =
            if (whoopVerbunden) {
                WhoopAbrufPlan.entscheide(
                    stand = whoopAbrufStand,
                    anlass = anlass,
                    jetztMs = System.currentTimeMillis(),
                    heute = java.time.LocalDate.now().toString(),
                    hinweisSchlafEndeMs = ouraSchlafEndeMs(),
                    anmeldungAbgelaufen = whoopReauth,
                )
            } else {
                null
            }
        val whoopAbfragen = whoopEntscheidung?.abrufen == true
        if (whoopAbfragen && anlass == WhoopAbrufPlan.Anlass.AUTOMATIK) {
            runCatchingCancellable {
                appSettings.aendereWhoopAbrufStand { WhoopAbrufPlan.nachAutomatischemAbruf(it, java.time.LocalDate.now().toString()) }
            }
        }
        Diag.i(
            DiagnosticArea.WHOOP,
            "ForegroundSync",
            if (whoopEntscheidung == null) "Whoop nicht verbunden"
            else "Whoop ${if (whoopAbfragen) "abfragen" else "uebersprungen"} ($anlass): ${whoopEntscheidung.grund}",
        )
        val ouraVerbunden = oura.isTokenConfigured()
        val kalenderVerbunden = secrets.calendarAccountEmail != null
        val ergebnisse = coroutineScope {
            val bodyDeferred = async { quelle { runCatchingCancellable { zeppBodyRepository.syncToCache() } } }
            val whoopDeferred = async { if (whoopAbfragen) quelle { whoop.syncLastDays(365) } else null }
            val ouraDeferred =
                async { if (ouraVerbunden) quelle { oura.syncLastDays(365).map { it.values.sum() } } else null }
            val trainingDeferred =
                async {
                    // Frank-Wunsch 2026-07-03: Trainings kommen jetzt aus Health Connect (Strava raus).
                    // Frank-Wunsch 2026-09-30: einmal ALLE Trainings (gesamter Verlauf) auf die
                    // Polar-Werte umstellen, danach wieder nur die letzten 30 Tage.
                    quelle {
                        runCatchingCancellable {
                            if (appSettings.isPolarFullResyncV1Done()) {
                                amazfit.mergeFromHealthConnect(days = 30)
                            } else {
                                amazfit.mergeFromHealthConnect(days = AmazfitRepository.FULL_HISTORY_DAYS)
                                    .also { appSettings.setPolarFullResyncV1Done(true) }
                            }
                        }
                    }
                }
            val calendarDeferred =
                async { if (kalenderVerbunden) quelle { calendar.syncDefaultWindow().map { 0 } } else null }
            listOf(
                whoopDeferred.await(),
                ouraDeferred.await(),
                bodyDeferred.await(),
                trainingDeferred.await(),
                calendarDeferred.await(),
            )
        }
        val jetzt = System.currentTimeMillis()

        fun stand(id: String, name: String, ergebnis: Result<Int>?, bisherOkMs: Long): QuellenStand {
            val fehler = ergebnis?.exceptionOrNull()
            val alt = vorher?.quellen?.firstOrNull { it.id == id }
            return QuellenStand(
                id = id,
                name = name,
                verbunden = ergebnis != null,
                ok = ergebnis?.isSuccess == true,
                anzahl = ergebnis?.getOrNull() ?: 0,
                fehler = fehler?.let(::fehlerText),
                zuletztOkMs = if (ergebnis?.isSuccess == true) jetzt else maxOf(alt?.zuletztOkMs ?: 0L, bisherOkMs),
                eingriffNoetig = fehler is WhoopReauthRequiredException || (fehler as? HttpException)?.code() == 401,
            )
        }
        // Whoop diesmal bewusst nicht abgefragt: den letzten Stand weiterfuehren, sonst stuende in der
        // Fusszeile und bei Jarvis "nicht verbunden" oder eine Stoerung. Einen alten Fehler aber nur,
        // solange seitdem kein Whoop-Abruf geglueckt ist (etwa der Einzel-Abruf nach neuer Anmeldung).
        // Eine als abgelaufen markierte Anmeldung bleibt immer sichtbar.
        val whoopStand =
            if (whoopVerbunden && !whoopAbfragen) {
                vorher?.quellen
                    ?.firstOrNull { it.id == QUELLE_WHOOP && it.verbunden }
                    ?.takeIf { it.ok || it.zuletztOkMs >= letzterWhoopOkMs }
                    ?.takeIf { !whoopReauth || it.eingriffNoetig }
                    // Ein gescheiterter Versuch seit dem letzten Erfolg darf nicht hinter einem alten "ok" verschwinden.
                    ?.takeIf { !it.ok || whoopAbrufStand.fehlversuche == 0 }
                    ?.let { it.copy(zuletztOkMs = maxOf(it.zuletztOkMs, letzterWhoopOkMs)) }
                    ?: QuellenStand(
                        id = QUELLE_WHOOP, name = "Whoop", verbunden = true,
                        ok = !whoopReauth && whoopAbrufStand.fehlversuche == 0, anzahl = 0,
                        fehler = when {
                            whoopReauth -> fehlerText(WhoopReauthRequiredException())
                            whoopAbrufStand.fehlversuche > 0 ->
                                "letzter Abruf gescheitert (${whoopAbrufStand.fehlversuche}× in Folge), nächster Versuch automatisch"
                            else -> null
                        },
                        zuletztOkMs = letzterWhoopOkMs, eingriffNoetig = whoopReauth,
                    )
            } else {
                stand(QUELLE_WHOOP, "Whoop", ergebnisse[0], letzterWhoopOkMs)
            }
        val quellen =
            listOf(
                whoopStand,
                stand(QUELLE_OURA, "Oura", ergebnisse[1], secrets.ouraLastSyncEpochMs),
                stand(QUELLE_WAAGE, "Waage (Zepp)", ergebnisse[2], 0L),
                stand(QUELLE_TRAINING, "Trainings (Health Connect)", ergebnisse[3], 0L),
                stand(QUELLE_KALENDER, "Kalender", ergebnisse[4], 0L),
            )
        val bericht = ApiSyncBericht(versuchMs = jetzt, anlass = reason, quellen = quellen)
        letzterBericht = bericht
        letzterAnlass = anlass
        runCatchingCancellable { appSettings.setBiomarkerSyncStatus(bericht.alsJson().toString()) }
            .onFailure { Diag.w(DiagnosticArea.APP, "ForegroundSync", "Abgleich-Stand nicht gespeichert", it) }

        // Fusszeile IMMER schreiben, mit dem Zustand jeder Quelle. Der Timer fuer "noch frisch" wird nur
        // dann NICHT neu gesetzt, wenn eine Quelle voruebergehend scheiterte (kein Netz): dann soll das
        // naechste Oeffnen sofort neu versuchen. Eine abgelaufene Anmeldung oder eine stumme Waage
        // heilt kein Sofort-Neuversuch — sie duerfen die anderen Quellen nicht dauernd neu laden lassen.
        fun uhr(ms: Long): String {
            val z = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
            return "%02d.%02d. %02d:%02d".format(z.dayOfMonth, z.monthValue, z.hour, z.minute)
        }
        fun teil(id: String, kurz: String): String {
            val q = quellen.first { it.id == id }
            return when {
                !q.verbunden -> "$kurz nicht verbunden"
                // Ehrliche Zeit: Whoop wurde diesmal nicht gefragt, also zaehlt der letzte echte Abruf.
                id == QUELLE_WHOOP && !whoopAbfragen && q.ok -> "$kurz Stand ${if (q.zuletztOkMs > 0L) uhr(q.zuletztOkMs) else "unbekannt"}"
                q.ok -> "$kurz ${q.anzahl}"
                q.eingriffNoetig -> "$kurz ⚠ neu anmelden"
                id == QUELLE_WAAGE -> "$kurz ⚠ nicht erreichbar"
                else -> "$kurz ⚠ Fehler"
            }
        }
        val nowZ = java.time.ZonedDateTime.now()
        val footerTs = "%02d.%02d. %02d:%02d".format(nowZ.dayOfMonth, nowZ.monthValue, nowZ.hour, nowZ.minute)
        val footer =
            "${if (bericht.probleme.isEmpty()) "✓" else "⚠"} $footerTs · ${teil(QUELLE_WHOOP, "Whoop")} · " +
                "${teil(QUELLE_TRAINING, "Training")} · ${teil(QUELLE_OURA, "Oura")} · ${teil(QUELLE_WAAGE, "Zepp")}"
        // Whoop zaehlt nur, wenn es diesmal wirklich gefragt wurde: Eine Pause nach Fehlern oder eine 429-Sperre
        // heilt kein Sofort-Neuversuch der anderen Quellen.
        val voruebergehend = quellen.any {
            it.verbunden && !it.ok && !it.eingriffNoetig && it.id != QUELLE_WAAGE && !(it.id == QUELLE_WHOOP && !whoopAbfragen)
        }
        appSettings.setLastRefreshFooter(
            footer,
            if (voruebergehend) appSettings.lastRefreshFooterAtMsFlow.first() else jetzt,
        )
        if (bericht.probleme.isEmpty()) {
            Diag.i(DiagnosticArea.APP, "ForegroundSync", "API-Sync FERTIG ($reason): $footer")
        } else {
            Diag.w(DiagnosticArea.APP, "ForegroundSync", "API-Sync FERTIG mit Problemen ($reason): ${bericht.probleme.joinToString(" | ")}")
        }
        return bericht
    }

    /** Eine Quelle mit Zeitlimit: eine haengende Quelle darf den Abgleich (und die Sperre) nicht festhalten. */
    private suspend fun quelle(block: suspend () -> Result<Int>): Result<Int> =
        withTimeoutOrNull(QUELLEN_ZEITLIMIT_MS) { block() }
            ?: Result.failure(java.io.IOException("Zeitüberschreitung nach ${QUELLEN_ZEITLIMIT_MS / 1000} Sekunden"))

    private fun rang(a: WhoopAbrufPlan.Anlass?): Int = when (a) {
        WhoopAbrufPlan.Anlass.OHNE_WHOOP, null -> 0
        WhoopAbrufPlan.Anlass.AUTOMATIK -> 1
        WhoopAbrufPlan.Anlass.VORDERGRUND -> 2
        WhoopAbrufPlan.Anlass.AUSDRUECKLICH -> 3
    }

    private fun fehlerText(fehler: Throwable): String =
        when {
            fehler is WhoopReauthRequiredException -> "Anmeldung abgelaufen, neue Anmeldung in Entropie Reductor nötig (Einstellungen → API-Schlüssel)"
            fehler is HttpException && fehler.code() == 401 -> "Zugang abgelehnt (401), Token in Entropie Reductor prüfen"
            fehler is HttpException && fehler.code() == 429 -> "Abruflimit erreicht (429)"
            fehler is de.frank.entropyreducer.data.repository.WhoopAbruflimitException -> fehler.message ?: "Abruflimit erreicht (429)"
            fehler is HttpException -> "Serverfehler ${fehler.code()}"
            fehler is java.net.UnknownHostException -> "keine Internetverbindung"
            else -> fehler.message ?: fehler.javaClass.simpleName
        }

    companion object {
        /**
         * Mindestabstand fuer den automatischen Abgleich beim Oeffnen der App: 15 Minuten.
         * Frank-Wunsch 2026-10-13 ("Werte sollen immer aktuell sein") loest den 8-h-Abstand vom
         * 2026-06-01 ab. Der Biomarker-Reiter gleicht weiterhin bei jedem Oeffnen ab.
         */
        const val FOREGROUND_SYNC_MIN_INTERVAL_MS = 15L * 60L * 1000L

        private const val QUELLEN_ZEITLIMIT_MS = 120_000L

        const val QUELLE_WHOOP = "whoop"
        const val QUELLE_OURA = "oura"
        const val QUELLE_WAAGE = "waage"
        const val QUELLE_TRAINING = "training"
        const val QUELLE_KALENDER = "kalender"
    }

    private suspend inline fun <T> runCatchingCancellable(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
}

/** Whoop-Stand ohne Netz, fuer Jarvis und Anzeige (siehe [ForegroundSyncManager.whoopUebersicht]). */
data class WhoopUebersicht(
    val letzterErfolgMs: Long,
    val schlafEndeMs: Long,
    val schlafVollstaendig: Boolean,
    val naechsterAbrufFruehestensMs: Long,
    val grund: String,
)

/** Zustand einer Biomarker-Quelle nach dem letzten Abgleich. */
data class QuellenStand(
    val id: String,
    val name: String,
    /** false = in der App gar nicht eingerichtet (kein Fehler). */
    val verbunden: Boolean,
    val ok: Boolean,
    val anzahl: Int,
    val fehler: String?,
    /** Letzter geglückter Abruf dieser Quelle (ms), 0 = unbekannt. */
    val zuletztOkMs: Long,
    /** true = heilt nicht von selbst, Frank muss etwas tun (neu anmelden, Token prüfen). */
    val eingriffNoetig: Boolean,
)

/** Ergebnis eines Abgleichs über alle Quellen. Wird gespeichert und geht an Fußzeile und Jarvis. */
data class ApiSyncBericht(val versuchMs: Long, val anlass: String, val quellen: List<QuellenStand>) {

    /** Lesbare Probleme, eine Zeile je betroffener Quelle. Leer = alles in Ordnung. */
    val probleme: List<String>
        get() = quellen.filter { it.verbunden && !it.ok }.map { q ->
            val seit = if (q.zuletztOkMs > 0L) {
                val tag = java.time.Instant.ofEpochMilli(q.zuletztOkMs).atZone(java.time.ZoneId.systemDefault())
                ", letzter geglückter Abruf am %02d.%02d. um %02d:%02d Uhr".format(tag.dayOfMonth, tag.monthValue, tag.hour, tag.minute)
            } else ""
            "${q.name}: ${q.fehler ?: "Fehler"}$seit"
        }

    /**
     * Whoop glückte nach diesem Bericht ([letzterOkMs] = letzter erfolgreicher Whoop-Abruf): einen alten Whoop-Fehler
     * nicht weiter melden — aber nur, wenn die Anmeldung nicht als abgelaufen markiert ist und seitdem kein Versuch
     * scheiterte. Ein geglückter Stand bekommt die neuere Erfolgszeit. Ohne Netz.
     */
    fun mitSpaeteremWhoopErfolg(letzterOkMs: Long, anmeldungAbgelaufen: Boolean, fehlversuche: Int): ApiSyncBericht {
        val whoop = quellen.firstOrNull { it.id == ForegroundSyncManager.QUELLE_WHOOP && it.verbunden } ?: return this
        if (letzterOkMs <= whoop.zuletztOkMs) return this
        val neu = when {
            whoop.ok -> whoop.copy(zuletztOkMs = letzterOkMs)
            letzterOkMs > versuchMs && !anmeldungAbgelaufen && fehlversuche == 0 ->
                whoop.copy(ok = true, fehler = null, eingriffNoetig = false, zuletztOkMs = letzterOkMs)
            else -> return this
        }
        return copy(quellen = quellen.map { if (it === whoop) neu else it })
    }

    fun alsJson(): JSONObject = JSONObject()
        .put("versuch_ms", versuchMs)
        .put("anlass", anlass)
        .put("quellen", JSONArray(quellen.map { q ->
            JSONObject().put("id", q.id).put("name", q.name).put("verbunden", q.verbunden).put("ok", q.ok)
                .put("anzahl", q.anzahl).put("fehler", q.fehler ?: JSONObject.NULL)
                .put("zuletzt_ok_ms", q.zuletztOkMs).put("eingriff_noetig", q.eingriffNoetig)
        }))

    companion object {
        fun ausJson(text: String): ApiSyncBericht? = runCatching {
            if (text.isBlank()) return null
            val json = JSONObject(text)
            val liste = json.getJSONArray("quellen")
            ApiSyncBericht(
                versuchMs = json.getLong("versuch_ms"),
                anlass = json.optString("anlass"),
                quellen = (0 until liste.length()).map { i ->
                    val q = liste.getJSONObject(i)
                    QuellenStand(
                        id = q.getString("id"), name = q.getString("name"), verbunden = q.optBoolean("verbunden"),
                        ok = q.optBoolean("ok"), anzahl = q.optInt("anzahl"),
                        fehler = if (q.isNull("fehler")) null else q.optString("fehler"),
                        zuletztOkMs = q.optLong("zuletzt_ok_ms"), eingriffNoetig = q.optBoolean("eingriff_noetig"),
                    )
                },
            )
        }.getOrNull()
    }
}
