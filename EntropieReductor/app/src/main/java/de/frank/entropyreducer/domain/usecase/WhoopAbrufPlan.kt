package de.frank.entropyreducer.domain.usecase

import org.json.JSONObject

/**
 * Zentrale Entscheidung, ob Entropie Reductor jetzt bei Whoop nachfragt (Frank-Wunsch 2026-10-15).
 *
 * Hintergrund (belegt im Diagnose-Protokoll, 09.10. 14:00 und 15.10. 13:37): Whoop tauscht bei JEDEM
 * Token-Refresh den Refresh-Token aus. Reißt die Verbindung ab, während die Antwort unterwegs ist
 * ("Software caused connection abort"), ist der alte Token verbraucht und der neue nie angekommen —
 * danach hilft nur eine neue Anmeldung. Der Zugangs-Token gilt eine Stunde, also kostet fast jeder
 * Abruf einen Refresh. Weniger Abrufe = weniger Gelegenheiten, die Anmeldung zu verlieren.
 *
 * Deshalb fragt die App nicht nach der Uhr, sondern nach dem Schlaf:
 *  - Ist der neueste Hauptschlaf samt Erholung von Whoop fertig berechnet, gibt es bis zum nächsten
 *    Schlaf nichts Neues, was zählt. Erst ab [NEUER_SCHLAF_FRUEHESTENS_MS] nach dessen Ende wird wieder
 *    nachgesehen — langsam ([LANGSAM_MS]), schneller nur mit einem Hinweis (Oura meldet einen neueren
 *    Schlaf) oder wenn Frank die App selbst öffnet.
 *  - Ist der neueste Schlaf noch nicht fertig berechnet, wird begrenzt nachgeladen
 *    ([OFFEN_ABSTAENDE_MS]), danach gilt wieder der langsame Takt.
 *  - Belastung und Energie (Tageswerte) laufen mit; eigene Abrufe nur, wenn Frank die App offen hat
 *    oder ausdrücklich frische Daten will.
 *  - Nach Fehlern wächst der Abstand ([backoff]); ein 429 mit Retry-After sperrt bis dahin.
 *  - Ein Tagesbudget ([TAGESBUDGET_AUTOMATIK]) fängt alles ab, was die Regeln oben übersehen.
 *
 * Schichtdienst: Es gibt bewusst keine Uhrzeit-Regel. Maßgeblich ist allein das Ende des letzten von
 * Whoop gemeldeten Schlafs, egal ob er nachts oder tagsüber war.
 *
 * Reine Logik ohne Android, damit sie in Unit-Tests geprüft werden kann.
 */
object WhoopAbrufPlan {

    /** Wer fragt. Bestimmt die Mindestabstände. */
    enum class Anlass {
        /** Hintergrund-Abgleich ohne Zutun von Frank. */
        AUTOMATIK,

        /** Frank hat die App bzw. den Biomarker-Reiter geöffnet: Netz meist stabil, er schaut gerade hin. */
        VORDERGRUND,

        /** Frank will ausdrücklich frische Daten (Aktualisieren-Knopf, „Jarvis, hol frische Werte“). */
        AUSDRUECKLICH,

        /** Abgleich der anderen Quellen, Whoop bleibt unberührt (etwa ein Anstoß von Jarvis ohne ausdrücklichen Wunsch). */
        OHNE_WHOOP,
    }

    /** Was über den letzten Whoop-Abruf gespeichert ist. Zeiten in Epoch-Millisekunden, 0 = unbekannt. */
    data class Stand(
        val letzterVersuchMs: Long = 0L,
        val letzterErfolgMs: Long = 0L,
        /** Ende des neuesten Hauptschlafs, den Whoop gemeldet hat (fertig oder nicht). */
        val schlafEndeMs: Long = 0L,
        /** true = dieser Schlaf und seine Erholung sind fertig berechnet (oder endgültig nicht berechenbar). */
        val schlafVollstaendig: Boolean = false,
        /** Abrufe, seit der neueste Schlaf als noch nicht fertig gesehen wurde. */
        val offeneVersuche: Int = 0,
        /** Fehlgeschlagene Abrufe in Folge (Netz, Server, Abruflimit). 0 nach einem Erfolg. */
        val fehlversuche: Int = 0,
        /** Whoop hat mit 429 und Retry-After geantwortet: vorher nicht wieder fragen. */
        val gesperrtBisMs: Long = 0L,
        /** Tag (yyyy-MM-dd, lokal) und Zahl der Abrufe an diesem Tag, für das Tagesbudget. */
        val budgetTag: String = "",
        val budgetAnzahl: Int = 0,
    ) {
        fun alsJson(): String = JSONObject()
            .put("letzter_versuch_ms", letzterVersuchMs)
            .put("letzter_erfolg_ms", letzterErfolgMs)
            .put("schlaf_ende_ms", schlafEndeMs)
            .put("schlaf_vollstaendig", schlafVollstaendig)
            .put("offene_versuche", offeneVersuche)
            .put("fehlversuche", fehlversuche)
            .put("gesperrt_bis_ms", gesperrtBisMs)
            .put("budget_tag", budgetTag)
            .put("budget_anzahl", budgetAnzahl)
            .toString()

        companion object {
            fun ausJson(text: String?): Stand = runCatching {
                if (text.isNullOrBlank()) return Stand()
                val j = JSONObject(text)
                Stand(
                    letzterVersuchMs = j.optLong("letzter_versuch_ms"),
                    letzterErfolgMs = j.optLong("letzter_erfolg_ms"),
                    schlafEndeMs = j.optLong("schlaf_ende_ms"),
                    schlafVollstaendig = j.optBoolean("schlaf_vollstaendig"),
                    offeneVersuche = j.optInt("offene_versuche"),
                    fehlversuche = j.optInt("fehlversuche"),
                    gesperrtBisMs = j.optLong("gesperrt_bis_ms"),
                    budgetTag = j.optString("budget_tag"),
                    budgetAnzahl = j.optInt("budget_anzahl"),
                )
            }.getOrDefault(Stand())
        }
    }

    /** Ergebnis der Prüfung. [fruehestensMs] = ab wann sich ein neuer Versuch lohnen könnte (0 = sofort). */
    data class Entscheidung(val abrufen: Boolean, val grund: String, val fruehestensMs: Long = 0L)

    /**
     * @param hinweisSchlafEndeMs Ende des neuesten Schlafs aus einer anderen Quelle (Oura), 0 = keiner.
     *   Nur ein Hinweis, nie eine Bedingung: liefert Oura tagelang nichts, greift der langsame Takt.
     * @param heute lokaler Tag yyyy-MM-dd für das Tagesbudget.
     */
    fun entscheide(
        stand: Stand,
        anlass: Anlass,
        jetztMs: Long,
        heute: String,
        hinweisSchlafEndeMs: Long = 0L,
        anmeldungAbgelaufen: Boolean = false,
    ): Entscheidung {
        if (anlass == Anlass.OHNE_WHOOP) return Entscheidung(false, "Whoop bei diesem Anlass ausgenommen")
        if (anmeldungAbgelaufen && anlass != Anlass.AUSDRUECKLICH) {
            // Alle 6 Stunden im Hintergrund pruefen, ob Whoop die Anmeldung doch wieder annimmt (heilt einen
            // einmaligen Fehler auf Whoop-Seite). Eine abgelehnte Anmeldung tauscht keinen Token, das kostet nichts.
            val seit = jetztMs - stand.letzterVersuchMs
            if (anlass == Anlass.AUTOMATIK && (stand.letzterVersuchMs == 0L || seit !in 0 until REAUTH_PRUEFEN_MS)) {
                return Entscheidung(true, "prüfen, ob Whoop die abgelaufene Anmeldung wieder annimmt")
            }
            return Entscheidung(false, "Anmeldung abgelaufen, neue Anmeldung nötig", stand.letzterVersuchMs + REAUTH_PRUEFEN_MS)
        }
        if (stand.gesperrtBisMs > jetztMs) {
            return Entscheidung(false, "Whoop-Abruflimit, Sperre bis ${stand.gesperrtBisMs}", stand.gesperrtBisMs)
        }
        val seitVersuch = jetztMs - stand.letzterVersuchMs
        val mindest = when (anlass) {
            Anlass.AUTOMATIK, Anlass.OHNE_WHOOP -> MINDESTABSTAND_AUTOMATIK_MS
            Anlass.VORDERGRUND -> MINDESTABSTAND_VORDERGRUND_MS
            Anlass.AUSDRUECKLICH -> MINDESTABSTAND_AUSDRUECKLICH_MS
        }
        // Eine Uhr, die zurückgestellt wurde (seitVersuch < 0), sperrt nicht dauerhaft.
        if (stand.letzterVersuchMs > 0L && seitVersuch in 0 until mindest) {
            return Entscheidung(false, "letzter Versuch erst vor ${seitVersuch / 60_000} min", stand.letzterVersuchMs + mindest)
        }
        if (anlass == Anlass.AUSDRUECKLICH) return Entscheidung(true, "ausdrücklich angefordert")

        val budget = if (stand.budgetTag == heute) stand.budgetAnzahl else 0
        if (anlass == Anlass.AUTOMATIK && budget >= TAGESBUDGET_AUTOMATIK) {
            return Entscheidung(false, "Tagesbudget von $TAGESBUDGET_AUTOMATIK automatischen Abrufen erreicht")
        }
        if (stand.fehlversuche > 0) {
            val warte = backoff(stand.fehlversuche)
            if (seitVersuch in 0 until warte) {
                return Entscheidung(false, "nach ${stand.fehlversuche} Fehlversuch(en) Pause", stand.letzterVersuchMs + warte)
            }
            return Entscheidung(true, "erneuter Versuch nach Fehler")
        }
        if (stand.letzterErfolgMs == 0L || stand.schlafEndeMs == 0L) {
            return Entscheidung(true, "Schlafstand noch unbekannt")
        }

        // Neuester Schlaf noch nicht fertig berechnet: begrenzt nachladen.
        if (!stand.schlafVollstaendig && stand.offeneVersuche < OFFEN_ABSTAENDE_MS.size) {
            val warte = OFFEN_ABSTAENDE_MS[stand.offeneVersuche]
            return if (seitVersuch >= warte) {
                Entscheidung(true, "Schlaf noch nicht fertig berechnet (Versuch ${stand.offeneVersuche + 1})")
            } else {
                Entscheidung(false, "Schlaf noch nicht fertig, nächster Versuch später", stand.letzterVersuchMs + warte)
            }
        }

        // Schlaf fertig (oder Nachladen ausgeschöpft): bis zum nächsten möglichen Schlaf nichts Neues.
        val seitSchlafEnde = jetztMs - stand.schlafEndeMs
        val seitErfolg = jetztMs - stand.letzterErfolgMs
        val tageswerteAlter = if (anlass == Anlass.VORDERGRUND) TAGESWERTE_VORDERGRUND_MS else Long.MAX_VALUE
        if (seitSchlafEnde < NEUER_SCHLAF_FRUEHESTENS_MS) {
            if (seitErfolg >= tageswerteAlter) return Entscheidung(true, "Tageswerte (Belastung, Energie) auffrischen")
            return Entscheidung(
                false,
                "Schlaf vollständig, vor ${seitSchlafEnde / 3_600_000} h zu Ende; kein neuer Schlaf zu erwarten",
                stand.schlafEndeMs + NEUER_SCHLAF_FRUEHESTENS_MS,
            )
        }
        val hinweis = hinweisSchlafEndeMs > stand.schlafEndeMs + HINWEIS_TOLERANZ_MS
        val takt = when {
            hinweis -> HINWEIS_MS
            anlass == Anlass.VORDERGRUND -> NEUER_SCHLAF_VORDERGRUND_MS
            else -> LANGSAM_MS
        }
        if (seitVersuch >= takt) {
            return Entscheidung(true, if (hinweis) "Oura meldet einen neueren Schlaf" else "nach neuem Schlaf sehen")
        }
        if (seitErfolg >= tageswerteAlter) return Entscheidung(true, "Tageswerte (Belastung, Energie) auffrischen")
        return Entscheidung(false, "auf neuen Schlaf warten", stand.letzterVersuchMs + takt)
    }

    /** Pause nach [fehlversuche] Fehlern in Folge: 15 min, 30 min, 1 h, 2 h, danach 4 h. */
    fun backoff(fehlversuche: Int): Long =
        (BACKOFF_START_MS shl (fehlversuche - 1).coerceIn(0, 4)).coerceAtMost(BACKOFF_MAX_MS)

    /** Stand nach einem Versuch, vor dem Ergebnis. */
    fun nachVersuch(stand: Stand, jetztMs: Long): Stand = stand.copy(letzterVersuchMs = jetztMs)

    /** Zählt einen automatischen Abruf (Hintergrund) für das Tagesbudget; Frank selbst wird nie begrenzt. */
    fun nachAutomatischemAbruf(stand: Stand, heute: String): Stand =
        stand.copy(budgetTag = heute, budgetAnzahl = (if (stand.budgetTag == heute) stand.budgetAnzahl else 0) + 1)

    /** Stand nach einem geglückten Abruf mit dem neuesten Schlaf laut Whoop. */
    fun nachErfolg(stand: Stand, jetztMs: Long, schlaf: Schlaf?): Stand {
        val neuerSchlaf = schlaf != null && schlaf.endeMs != stand.schlafEndeMs
        val vollstaendig = schlaf?.vollstaendig ?: stand.schlafVollstaendig
        return stand.copy(
            letzterErfolgMs = jetztMs,
            fehlversuche = 0,
            gesperrtBisMs = 0L,
            schlafEndeMs = schlaf?.endeMs ?: stand.schlafEndeMs,
            schlafVollstaendig = vollstaendig,
            // Zaehlt die Nachlade-Abrufe NACH dem ersten Sehen: neuer, noch offener Schlaf -> 0, damit der
            // erste Nachlade-Abstand (30 min) gilt.
            offeneVersuche = when {
                vollstaendig || neuerSchlaf -> 0
                else -> stand.offeneVersuche + 1
            },
        )
    }

    /** Stand nach einem gescheiterten Abruf. [sperreBisMs] > 0 bei 429 mit Retry-After. */
    fun nachFehler(stand: Stand, sperreBisMs: Long = 0L): Stand =
        stand.copy(fehlversuche = stand.fehlversuche + 1, gesperrtBisMs = maxOf(stand.gesperrtBisMs, sperreBisMs))

    /** Der neueste Hauptschlaf laut Whoop. */
    data class Schlaf(val endeMs: Long, val vollstaendig: Boolean)

    /**
     * Neuester Hauptschlaf (kein Nickerchen) aus den Whoop-Rohdaten. Fertig heißt: Schlaf UND die zu ihm
     * gehörende Erholung haben einen endgültigen Zustand (SCORED oder UNSCORABLE — ein nicht berechenbarer
     * Tag, etwa Band nicht getragen, wird durch Nachfragen auch nicht besser).
     *
     * @param schlaefe (id, endeMs, nap, scoreState)
     * @param erholungen (sleepId, scoreState)
     */
    fun neuesterSchlaf(
        schlaefe: List<SchlafRoh>,
        erholungen: List<ErholungRoh>,
    ): Schlaf? {
        val neuester = schlaefe.filter { !it.nap && it.endeMs > 0L }.maxByOrNull { it.endeMs } ?: return null
        val erholung = erholungen.firstOrNull { it.sleepId != null && it.sleepId == neuester.id }
        val fertig = neuester.scoreState in ENDGUELTIG && erholung?.scoreState in ENDGUELTIG
        return Schlaf(neuester.endeMs, fertig)
    }

    data class SchlafRoh(val id: String?, val endeMs: Long, val nap: Boolean, val scoreState: String?)
    data class ErholungRoh(val sleepId: String?, val scoreState: String?)

    private val ENDGUELTIG = setOf("SCORED", "UNSCORABLE")

    private const val MINUTE = 60_000L
    private const val STUNDE = 60L * MINUTE

    const val MINDESTABSTAND_AUTOMATIK_MS = 30L * MINUTE
    const val MINDESTABSTAND_VORDERGRUND_MS = 30L * MINUTE
    const val MINDESTABSTAND_AUSDRUECKLICH_MS = 10L * MINUTE

    /** Wartezeiten, solange der neueste Schlaf noch nicht fertig berechnet ist: 30 min, 1 h, 2 h, 3 h. */
    val OFFEN_ABSTAENDE_MS = longArrayOf(30L * MINUTE, 1L * STUNDE, 2L * STUNDE, 3L * STUNDE)

    /** So früh nach dem Ende eines Hauptschlafs kann frühestens der nächste fertig sein. */
    const val NEUER_SCHLAF_FRUEHESTENS_MS = 8L * STUNDE

    /** Langsamer Takt im Hintergrund, solange ein neuer Schlaf möglich ist und kein Hinweis vorliegt. */
    const val LANGSAM_MS = 4L * STUNDE

    /** Takt, wenn Frank die App öffnet und ein neuer Schlaf möglich ist. */
    const val NEUER_SCHLAF_VORDERGRUND_MS = 1L * STUNDE

    /** Takt, wenn Oura schon einen neueren Schlaf kennt. */
    const val HINWEIS_MS = 30L * MINUTE
    const val HINWEIS_TOLERANZ_MS = 1L * STUNDE

    /** Tageswerte (Belastung, Energie) bei offener App höchstens so alt. Im Hintergrund nie eigens. */
    const val TAGESWERTE_VORDERGRUND_MS = 3L * STUNDE

    const val TAGESBUDGET_AUTOMATIK = 8

    /** Abstand der Hintergrund-Prüfung, ob eine abgelehnte Anmeldung wieder angenommen wird. */
    const val REAUTH_PRUEFEN_MS = 6L * STUNDE
    private const val BACKOFF_START_MS = 15L * MINUTE
    private const val BACKOFF_MAX_MS = 4L * STUNDE
}
