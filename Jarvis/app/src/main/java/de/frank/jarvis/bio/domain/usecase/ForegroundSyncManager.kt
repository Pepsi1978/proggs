package de.frank.jarvis.bio.domain.usecase

import de.frank.jarvis.bio.data.diagnostics.Diag
import de.frank.jarvis.bio.data.diagnostics.DiagnosticArea
import de.frank.jarvis.bio.data.local.AppDatabase
import de.frank.jarvis.bio.data.remote.oauth.OAuthService
import de.frank.jarvis.bio.data.remote.oauth.WhoopReauthRequiredException
import de.frank.jarvis.bio.data.repository.AmazfitRepository
import de.frank.jarvis.bio.data.repository.OuraRepository
import de.frank.jarvis.bio.data.repository.WhoopRepository
import de.frank.jarvis.bio.data.repository.ZeppBodyRepository
import de.frank.jarvis.bio.data.settings.AppSettings
import de.frank.jarvis.bio.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Der Biomarker-Abgleich von Jarvis (aus Entropie Reductor übernommen, dort ebenso benannt). Holt Whoop, Oura, Waage (Zepp)
 * und Trainings (Health Connect) gleichzeitig, jede Quelle mit eigenem Zeitlimit, und hält das Ergebnis je Quelle fest
 * ([ApiSyncBericht]). Wer dazukommt, während ein Abgleich läuft, wartet auf dessen Ergebnis.
 *
 * In Jarvis gibt es bewusst KEINEN Hintergrund-Abgleich (Franks Vorgabe vom 11.11.2026): Ausgelöst wird nur von
 * [de.frank.jarvis.bio.BioAbruf] (Biomarker-Bereich öffnen, ausdrücklicher Wunsch auch per Sprache, vor der Tagesauswertung).
 * Ob Whoop gefragt wird, entscheidet weiter allein [WhoopAbrufPlan]: Whoop tauscht bei jedem Token-Refresh den Refresh-Token,
 * jeder unnötige Abruf ist ein Risiko für die Anmeldung.
 *
 * Neu gegenüber Entropie Reductor: Je Quelle wird festgehalten, ob der Abruf NEUE Messwerte brachte (Vergleich eines
 * Fingerabdrucks der jüngsten Daten vor und nach dem Abruf) und wann die neueste Messung war. Pausierte Quellen werden nicht
 * gefragt und melden keine Störung.
 */
class ForegroundSyncManager(
    private val secrets: EncryptedSecretsStore,
    private val oauth: OAuthService,
    private val whoop: WhoopRepository,
    private val oura: OuraRepository,
    private val amazfit: AmazfitRepository,
    private val zeppBodyRepository: ZeppBodyRepository,
    private val appSettings: AppSettings,
    private val applicationScope: CoroutineScope,
    private val datenbank: () -> AppDatabase,
    /** Ist die Quelle in Jarvis' Einstellungen eingeschaltet? (whoop, oura, waage, training) */
    private val aktiv: (String) -> Boolean = { true },
) {
    private val apiMutex = Mutex()

    @Volatile private var letzterBericht: ApiSyncBericht? = null
    @Volatile private var letzterAnlass: WhoopAbrufPlan.Anlass? = null

    /** Läuft gerade ein Abgleich? Für die Anzeige. */
    val laeuft: Boolean get() = apiMutex.isLocked

    /**
     * Voller Abgleich. Läuft schon einer, wartet der Aufrufer und übernimmt dessen Ergebnis, sofern der Whoop mindestens so
     * gründlich behandelt hat (ein ausdrücklicher Wunsch fragt immer selbst).
     */
    suspend fun syncApisNow(
        reason: String = "Biomarker-Bereich",
        anlass: WhoopAbrufPlan.Anlass = WhoopAbrufPlan.Anlass.VORDERGRUND,
    ): ApiSyncBericht {
        val angefragtMs = System.currentTimeMillis()
        return apiMutex.withLock {
            val frisch = letzterBericht?.takeIf {
                it.versuchMs >= angefragtMs && anlass != WhoopAbrufPlan.Anlass.AUSDRUECKLICH && rang(letzterAnlass) >= rang(anlass)
            }
            if (frisch != null) {
                Diag.i(DiagnosticArea.APP, "BioAbgleich", "Abgleich ($reason): Ergebnis des gerade beendeten Laufs übernommen")
                frisch
            } else {
                fuehreApiSyncAus(reason, anlass)
            }
        }
    }

    /** Im app-weiten Bereich: Läuft die Wartezeit des Aufrufers ab, arbeitet der Abgleich trotzdem zu Ende. */
    fun starteApiSync(reason: String, anlass: WhoopAbrufPlan.Anlass): Deferred<ApiSyncBericht> =
        applicationScope.async { syncApisNow(reason, anlass) }

    /** Whoop-Stand ohne Netz: wann zuletzt abgerufen, bis wann der neueste Schlaf reicht, ob er fertig berechnet ist. */
    suspend fun whoopUebersicht(): WhoopUebersicht {
        val stand = appSettings.whoopAbrufStandFlow.first()
        val jetzt = System.currentTimeMillis()
        val e = WhoopAbrufPlan.entscheide(
            stand, WhoopAbrufPlan.Anlass.VORDERGRUND, jetzt, java.time.LocalDate.now().toString(),
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

    private suspend fun ouraSchlafEndeMs(): Long = runCatching {
        val heute = java.time.LocalDate.now()
        listOf(heute, heute.minusDays(1))
            .flatMap { oura.observeSleepDetailsForDay(it.toString()).first() }
            .filter { (it.totalSleepSeconds ?: 0) >= 3 * 3600 }
            .mapNotNull { s -> s.bedtimeEnd?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } }
            .maxOrNull() ?: 0L
    }.getOrDefault(0L)

    /** Ergebnis des letzten Abgleichs (auch aus einem früheren Prozess), null = noch nie gelaufen. */
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
        Diag.i(DiagnosticArea.APP, "BioAbgleich", "Abgleich START ($reason)")
        val vorher = letzterStand()
        val fingerVorher = fingerabdruecke()
        val whoopVerbunden = aktiv(QUELLE_WHOOP) && oauth.loadWhoopAuthState().isAuthorized
        val letzterWhoopOkMs = appSettings.lastWhoopSyncMsFlow.first()
        val whoopReauth = oauth.whoopNeedsReauth()
        val whoopAbrufStand = appSettings.whoopAbrufStandFlow.first()
        val whoopEntscheidung = if (whoopVerbunden) {
            WhoopAbrufPlan.entscheide(
                stand = whoopAbrufStand, anlass = anlass, jetztMs = System.currentTimeMillis(),
                heute = java.time.LocalDate.now().toString(), hinweisSchlafEndeMs = ouraSchlafEndeMs(), anmeldungAbgelaufen = whoopReauth,
            )
        } else null
        val whoopAbfragen = whoopEntscheidung?.abrufen == true
        Diag.i(
            DiagnosticArea.WHOOP, "BioAbgleich",
            if (whoopEntscheidung == null) "Whoop nicht verbunden oder pausiert"
            else "Whoop ${if (whoopAbfragen) "abfragen" else "übersprungen"} ($anlass): ${whoopEntscheidung.grund}",
        )
        val ouraVerbunden = aktiv(QUELLE_OURA) && oura.isTokenConfigured()
        val waageAktiv = aktiv(QUELLE_WAAGE)
        val trainingAktiv = aktiv(QUELLE_TRAINING)
        val ergebnisse = coroutineScope {
            val body = async { if (waageAktiv) quelle { runCatchingCancellable { zeppBodyRepository.syncToCache() } } else null }
            val wh = async { if (whoopAbfragen) quelle { whoop.syncLastDays(365) } else null }
            val ou = async { if (ouraVerbunden) quelle { oura.syncLastDays(365).map { it.values.sum() } } else null }
            val tr = async {
                if (!trainingAktiv) null else quelle {
                    runCatchingCancellable {
                        if (appSettings.isPolarFullResyncV1Done()) amazfit.mergeFromHealthConnect(days = 30)
                        else amazfit.mergeFromHealthConnect(days = AmazfitRepository.FULL_HISTORY_DAYS).also { appSettings.setPolarFullResyncV1Done(true) }
                    }
                }
            }
            listOf(wh.await(), ou.await(), body.await(), tr.await())
        }
        val jetzt = System.currentTimeMillis()
        val fingerNachher = fingerabdruecke()

        fun stand(id: String, name: String, ergebnis: Result<Int>?, bisherOkMs: Long, pausiert: Boolean = !aktiv(id)): QuellenStand {
            val fehler = ergebnis?.exceptionOrNull()
            val alt = vorher?.quellen?.firstOrNull { it.id == id }
            val neu = ergebnis?.isSuccess == true && fingerNachher[id]?.first != fingerVorher[id]?.first
            return QuellenStand(
                id = id, name = name, verbunden = ergebnis != null, ok = ergebnis?.isSuccess == true,
                anzahl = ergebnis?.getOrNull() ?: 0, fehler = fehler?.let(::fehlerText),
                zuletztOkMs = if (ergebnis?.isSuccess == true) jetzt else maxOf(alt?.zuletztOkMs ?: 0L, bisherOkMs),
                eingriffNoetig = fehler is WhoopReauthRequiredException || (fehler as? HttpException)?.code() == 401,
                pausiert = pausiert,
                gefragt = ergebnis != null,
                neueWerte = neu,
                neuesteMessungMs = fingerNachher[id]?.second ?: alt?.neuesteMessungMs ?: 0L,
                zuletztNeuMs = if (neu) jetzt else alt?.zuletztNeuMs ?: 0L,
            )
        }
        // Whoop diesmal bewusst nicht gefragt: den letzten Stand weiterführen (ehrlich: „nicht gefragt“, mit Grund und Datum).
        val whoopStand = if (whoopVerbunden && !whoopAbfragen) {
            val alt = vorher?.quellen?.firstOrNull { it.id == QUELLE_WHOOP && it.verbunden }
            QuellenStand(
                id = QUELLE_WHOOP, name = "Whoop", verbunden = true,
                ok = !whoopReauth && whoopAbrufStand.fehlversuche == 0,
                anzahl = 0,
                fehler = when {
                    whoopReauth -> fehlerText(WhoopReauthRequiredException())
                    whoopAbrufStand.fehlversuche > 0 -> "letzter Abruf gescheitert (${whoopAbrufStand.fehlversuche}× in Folge), Pause bis zum nächsten erlaubten Versuch"
                    else -> null
                },
                zuletztOkMs = maxOf(alt?.zuletztOkMs ?: 0L, letzterWhoopOkMs), eingriffNoetig = whoopReauth,
                gefragt = false, nichtGefragtGrund = whoopEntscheidung?.grund,
                neuesteMessungMs = fingerNachher[QUELLE_WHOOP]?.second ?: alt?.neuesteMessungMs ?: 0L,
                zuletztNeuMs = alt?.zuletztNeuMs ?: 0L,
            )
        } else {
            stand(QUELLE_WHOOP, "Whoop", ergebnisse[0], letzterWhoopOkMs, pausiert = !aktiv(QUELLE_WHOOP))
        }
        val quellen = listOf(
            whoopStand,
            stand(QUELLE_OURA, "Oura", ergebnisse[1], secrets.ouraLastSyncEpochMs),
            stand(QUELLE_WAAGE, "Waage (Zepp)", ergebnisse[2], 0L),
            stand(QUELLE_TRAINING, "Trainings (Health Connect)", ergebnisse[3], 0L),
        )
        val bericht = ApiSyncBericht(versuchMs = jetzt, anlass = reason, quellen = quellen)
        letzterBericht = bericht
        letzterAnlass = anlass
        runCatchingCancellable { appSettings.setBiomarkerSyncStatus(bericht.alsJson().toString()) }
            .onFailure { Diag.w(DiagnosticArea.APP, "BioAbgleich", "Abgleich-Stand nicht gespeichert", it) }
        runCatchingCancellable { appSettings.setLastRefreshFooter(bericht.kurzfassung(), jetzt) }
        if (bericht.probleme.isEmpty()) Diag.i(DiagnosticArea.APP, "BioAbgleich", "Abgleich FERTIG ($reason): ${bericht.kurzfassung()}")
        else Diag.w(DiagnosticArea.APP, "BioAbgleich", "Abgleich FERTIG mit Problemen ($reason): ${bericht.probleme.joinToString(" | ")}")
        return bericht
    }

    /**
     * Je Quelle ein Fingerabdruck der jüngsten Daten (ändert sich nur, wenn neue oder geänderte Messwerte ankamen) und der
     * Zeitpunkt der neuesten Messung. Abrufzeiten fließen bewusst nicht ein: Ein Abruf ohne neue Messung bleibt gleich.
     */
    private fun fingerabdruecke(): Map<String, Pair<String, Long>> = runCatching {
        val db = datenbank().openHelper.readableDatabase
        val grenze = System.currentTimeMillis() - 10L * 86_400_000L
        val grenzTag = java.time.LocalDate.now().minusDays(10).toString()
        fun zeile(sql: String): List<String> = db.query(sql).use { c -> if (c.moveToFirst()) (0 until c.columnCount).map { c.getString(it) ?: "" } else emptyList() }
        fun ms(text: String?): Long = text?.toDoubleOrNull()?.toLong() ?: 0L
        // Nur abgeschlossene Messungen (Schlaf, Erholung, HRV, Ruhepuls, Readiness, Schlafscore): Belastung, Energie, Schritte und
        // der Aktivitätsscore wachsen über den Tag und würden sonst jeden Abruf als „neu“ melden.
        val wh = zeile("SELECT COUNT(*), MAX(capturedAt), TOTAL(recoveryScore), TOTAL(hrvMs), TOTAL(restingHeartRate), TOTAL(sleepTotalMinutes), TOTAL(sleepDeepMinutes) FROM biomarker_snapshots WHERE capturedAt >= $grenze")
        val whNeu = zeile("SELECT MAX(capturedAt) FROM biomarker_snapshots").firstOrNull()
        val ou = zeile("SELECT (SELECT COUNT(*) || ':' || TOTAL(score) FROM oura_daily_readiness WHERE day >= '$grenzTag'), (SELECT COUNT(*) || ':' || TOTAL(score) FROM oura_daily_sleep WHERE day >= '$grenzTag'), (SELECT COUNT(*) FROM oura_daily_resilience WHERE day >= '$grenzTag'), (SELECT COUNT(*) FROM oura_sleep_detail WHERE day >= '$grenzTag')")
        val ouNeu = zeile("SELECT MAX(day) FROM oura_daily_readiness").firstOrNull()
        val wa = zeile("SELECT COUNT(*), MAX(timestampMs), TOTAL(value) FROM hc_value_cache WHERE metric LIKE 'zepp_%'")
        val tr = zeile("SELECT COUNT(*), MAX(startMs), TOTAL(COALESCE(distanceMeters, 0)) FROM amazfit_workouts")
        val zone = java.time.ZoneId.systemDefault()
        mapOf(
            QUELLE_WHOOP to (wh.joinToString("|") to ms(whNeu)),
            QUELLE_OURA to (ou.joinToString("|") to (ouNeu?.let { runCatching { java.time.LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() } ?: 0L)),
            QUELLE_WAAGE to (wa.joinToString("|") to ms(wa.getOrNull(1))),
            QUELLE_TRAINING to (tr.joinToString("|") to ms(tr.getOrNull(1))),
        )
    }.getOrElse { Diag.w(DiagnosticArea.APP, "BioAbgleich", "Fingerabdruck nicht lesbar", it); emptyMap() }

    private suspend fun quelle(block: suspend () -> Result<Int>): Result<Int> =
        withTimeoutOrNull(QUELLEN_ZEITLIMIT_MS) { block() }
            ?: Result.failure(java.io.IOException("Zeitüberschreitung nach ${QUELLEN_ZEITLIMIT_MS / 1000} Sekunden"))

    private fun rang(a: WhoopAbrufPlan.Anlass?): Int = when (a) {
        WhoopAbrufPlan.Anlass.OHNE_WHOOP, null -> 0
        WhoopAbrufPlan.Anlass.AUTOMATIK -> 1
        WhoopAbrufPlan.Anlass.VORDERGRUND -> 2
        WhoopAbrufPlan.Anlass.AUSDRUECKLICH -> 3
    }

    private fun fehlerText(fehler: Throwable): String = when {
        fehler is WhoopReauthRequiredException -> "Anmeldung abgelaufen, neue Anmeldung in Jarvis nötig (Biomarker → Einstellungen → Whoop)"
        fehler is HttpException && fehler.code() == 401 -> "Zugang abgelehnt (401), Zugang in Jarvis unter Biomarker → Einstellungen prüfen"
        fehler is HttpException && fehler.code() == 429 -> "Abruflimit erreicht (429)"
        fehler is de.frank.jarvis.bio.data.repository.WhoopAbruflimitException -> fehler.message ?: "Abruflimit erreicht (429)"
        fehler is HttpException -> "Serverfehler ${fehler.code()}"
        fehler is java.net.UnknownHostException -> "keine Internetverbindung"
        else -> fehler.message ?: fehler.javaClass.simpleName
    }

    companion object {
        /** Zeitlimit je Quelle. Eine hängende Quelle darf den Abgleich (und die Tagesauswertung) nicht festhalten. */
        const val QUELLEN_ZEITLIMIT_MS = 75_000L

        const val QUELLE_WHOOP = "whoop"
        const val QUELLE_OURA = "oura"
        const val QUELLE_WAAGE = "waage"
        const val QUELLE_TRAINING = "training"
    }

    private suspend inline fun <T> runCatchingCancellable(crossinline block: suspend () -> T): Result<T> =
        try { Result.success(block()) } catch (c: CancellationException) { throw c } catch (e: Exception) { Result.failure(e) }
}

/** Whoop-Stand ohne Netz, für Jarvis und Anzeige (siehe [ForegroundSyncManager.whoopUebersicht]). */
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
    /** false = gar nicht eingerichtet oder pausiert (kein Fehler). */
    val verbunden: Boolean,
    val ok: Boolean,
    val anzahl: Int,
    val fehler: String?,
    /** Letzter geglückter Abruf dieser Quelle (ms), 0 = unbekannt. */
    val zuletztOkMs: Long,
    /** true = heilt nicht von selbst, Frank muss etwas tun (neu anmelden, Token prüfen). */
    val eingriffNoetig: Boolean,
    /** In Jarvis' Einstellungen pausiert: nicht gefragt, keine Störung. */
    val pausiert: Boolean = false,
    /** Diesmal wirklich abgefragt (Whoop kann bewusst ausgelassen werden). */
    val gefragt: Boolean = true,
    val nichtGefragtGrund: String? = null,
    /** Hat dieser Abruf neue oder geänderte Messwerte gebracht? */
    val neueWerte: Boolean = false,
    /** Zeitpunkt der neuesten gespeicherten Messung dieser Quelle (ms), 0 = keine. */
    val neuesteMessungMs: Long = 0L,
    /** Wann zuletzt ein Abruf neue Werte brachte (ms), 0 = unbekannt. */
    val zuletztNeuMs: Long = 0L,
)

/** Ergebnis eines Abgleichs über alle Quellen. Wird gespeichert und geht an die Anzeige und an Jarvis' Werkzeuge. */
data class ApiSyncBericht(val versuchMs: Long, val anlass: String, val quellen: List<QuellenStand>) {

    /** Lesbare Probleme, eine Zeile je gestörter (nicht pausierter) Quelle. Leer = alles in Ordnung. */
    val probleme: List<String>
        get() = quellen.filter { it.verbunden && !it.ok && !it.pausiert }.map { q ->
            "${q.name}: ${q.fehler ?: "Fehler"}" + (if (q.zuletztOkMs > 0L) ", letzter geglückter Abruf am ${uhr(q.zuletztOkMs)} Uhr" else "")
        }

    /** Eine Zeile für Fußzeile und Logbuch. */
    fun kurzfassung(): String = quellen.joinToString(" · ") { q ->
        q.name.substringBefore(" (") + " " + when {
            q.pausiert -> "pausiert"
            !q.verbunden -> "nicht verbunden"
            !q.gefragt -> "nicht gefragt"
            q.ok && q.neueWerte -> "neu"
            q.ok -> "ohne neue Messung"
            q.eingriffNoetig -> "⚠ neu anmelden"
            else -> "⚠ " + (q.fehler ?: "Fehler")
        }
    }

    fun mitSpaeteremWhoopErfolg(letzterOkMs: Long, anmeldungAbgelaufen: Boolean, fehlversuche: Int): ApiSyncBericht {
        val whoop = quellen.firstOrNull { it.id == ForegroundSyncManager.QUELLE_WHOOP && it.verbunden } ?: return this
        if (letzterOkMs <= whoop.zuletztOkMs) return this
        val neu = when {
            whoop.ok -> whoop.copy(zuletztOkMs = letzterOkMs)
            letzterOkMs > versuchMs && !anmeldungAbgelaufen && fehlversuche == 0 -> whoop.copy(ok = true, fehler = null, eingriffNoetig = false, zuletztOkMs = letzterOkMs)
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
                .put("pausiert", q.pausiert).put("gefragt", q.gefragt).put("nicht_gefragt_grund", q.nichtGefragtGrund ?: JSONObject.NULL)
                .put("neue_werte", q.neueWerte).put("neueste_messung_ms", q.neuesteMessungMs).put("zuletzt_neu_ms", q.zuletztNeuMs)
        }))

    companion object {
        fun uhr(ms: Long): String {
            val z = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
            return "%02d.%02d. um %02d:%02d".format(z.dayOfMonth, z.monthValue, z.hour, z.minute)
        }

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
                        pausiert = q.optBoolean("pausiert"), gefragt = q.optBoolean("gefragt", true),
                        nichtGefragtGrund = if (q.isNull("nicht_gefragt_grund")) null else q.optString("nicht_gefragt_grund").ifEmpty { null },
                        neueWerte = q.optBoolean("neue_werte"), neuesteMessungMs = q.optLong("neueste_messung_ms"), zuletztNeuMs = q.optLong("zuletzt_neu_ms"),
                    )
                }.filter { it.id != "kalender" },
            )
        }.getOrNull()
    }
}
