package de.frank.stacklabor.codex

// PLAN (F-12/F-13/F-02): Die eigentliche Anfrage an Codex.
// 1. Die Antwort kommt als Server-Sent-Events; jedes Textstück wird sofort weitergereicht, damit
//    sich der Fließtext in der Oberfläche wortweise aufbaut (M-13).
// 2. Weil das Antwortformat erzwungenes JSON ist, wird aus dem Strom nur der Inhalt des Feldes
//    „gesamt" herausgelöst — die Oberfläche bekommt echten Fließtext statt roher Klammern.
// 3. Ab 13 aktiven Zielen wird die Anfrage in Läufe zu je 12 Zielen zerlegt (F-12, Regeln/Grenzen)
//    und die Teiltabellen werden lokal vereinigt.
// 4. Kaputtes JSON: ein Wiederholversuch mit der Auflage „nur JSON"; scheitert auch der, bleibt der
//    Fließtext erhalten und die Tabelle leer — die Ampeln werden grau statt falsch.
// 5. Netzfehler: ein automatischer zweiter Versuch nach 3 Sekunden.

import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/**
 * Stellt die Auswertungs-Anfragen an `chatgpt.com/backend-api/codex/responses`.
 *
 * Alle Netzarbeit läuft auf [Dispatchers.IO]. Fehler kommen als [CodexFehler] im [Result] zurück,
 * damit die Oberfläche zwischen „neu anmelden", „Kontingent" und „Netz" unterscheiden kann.
 */
class CodexDienst(private val anmeldung: CodexAnmeldung) {

    private val laufenderAufruf = AtomicReference<Call?>(null)

    /**
     * Wertet einen Stack (F-12) oder alle Stacks zusammen (F-13) aus.
     *
     * [aufFortschritt] wird mit jedem eintreffenden Textstück des Fließtextes gerufen.
     */
    suspend fun werteStackAus(
        anfrage: AuswertungsAnfrage,
        modell: CodexModell = CodexModell.VORGABE,
        denkstufe: Denkstufe = Denkstufe.VORGABE,
        aufFortschritt: (String) -> Unit = {},
    ): Result<CodexAntwort> = fuehreAus(anfrage, modell, denkstufe, emptyList(), aufFortschritt)

    /**
     * Konkurrenzprüfung für frisch hinzugefügte Mittel (F-02). Gemeldet werden nur Zellen und
     * Konkurrenzen, an denen mindestens eines der Mittel aus [neueMittel] beteiligt ist.
     */
    suspend fun pruefeNeuesMittel(
        anfrage: AuswertungsAnfrage,
        neueMittel: List<String>,
        modell: CodexModell = CodexModell.VORGABE,
        denkstufe: Denkstufe = Denkstufe.VORGABE,
    ): Result<CodexAntwort> {
        if (neueMittel.isEmpty()) return Result.success(CodexAntwort())
        return fuehreAus(anfrage, modell, denkstufe, neueMittel) {}
    }

    /** Bricht einen laufenden Aufruf ab (der Auswerten-Knopf ist währenddessen ein Abbruch-Knopf). */
    fun brichAb() {
        laufenderAufruf.getAndSet(null)?.cancel()
    }

    private suspend fun fuehreAus(
        anfrage: AuswertungsAnfrage,
        modell: CodexModell,
        denkstufe: Denkstufe,
        neueMittel: List<String>,
        aufFortschritt: (String) -> Unit,
    ): Result<CodexAntwort> = withContext(Dispatchers.IO) {
        try {
            val teile = zielGruppen(anfrage.ziele).map { teilZiele ->
                fuehreLaufAus(anfrage.copy(ziele = teilZiele), modell, denkstufe, neueMittel, aufFortschritt)
            }
            Result.success(teile.reduce { links, rechts -> links.vereinigtMit(rechts) })
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(alsCodexFehler(fehler))
        }
    }

    private suspend fun fuehreLaufAus(
        anfrage: AuswertungsAnfrage,
        modell: CodexModell,
        denkstufe: Denkstufe,
        neueMittel: List<String>,
        aufFortschritt: (String) -> Unit,
    ): CodexAntwort {
        val rohtext = holeRohtext(
            AuftragsText.nutzlast(anfrage, modell, denkstufe, nurJson = false, neueMittel = neueMittel),
            aufFortschritt,
        )
        parseAntwort(rohtext).getOrNull()?.let { return it }

        // DECISION: Der Wiederholversuch läuft ohne Fortschrittsmeldungen — sonst stünde der
        // Fließtext in der Oberfläche zweimal untereinander.
        val zweiterRohtext = holeRohtext(
            AuftragsText.nutzlast(anfrage, modell, denkstufe, nurJson = true, neueMittel = neueMittel),
        ) {}
        parseAntwort(zweiterRohtext).getOrNull()?.let { return it }

        // DECISION (F-12): Auch der zweite Versuch war kein lesbares JSON. Der Fließtext wird
        // behalten, die Tabelle bleibt leer — die Ampeln werden grau statt falsch.
        return CodexAntwort(gesamt = lesbarerText(zweiterRohtext).ifBlank { lesbarerText(rohtext) })
    }

    /** Holt einen Rohtext; bei einer Netzstörung genau ein zweiter Versuch nach 3 Sekunden. */
    private suspend fun holeRohtext(nutzlast: JSONObject, aufFortschritt: (String) -> Unit): String {
        var geliefert = false
        var versuch = 0
        while (true) {
            try {
                return sendeEinmal(nutzlast) { stueck ->
                    geliefert = true
                    aufFortschritt(stueck)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                currentCoroutineContext().ensureActive()
                // Nach bereits ausgeliefertem Text wird nicht wiederholt, sonst doppelt sich der
                // Fließtext in der Oberfläche.
                if (geliefert || versuch >= 1 || !istVoruebergehend(fehler)) throw fehler
                versuch++
                delay(NETZ_PAUSE_MS)
            }
        }
    }

    private suspend fun sendeEinmal(nutzlast: JSONObject, aufFortschritt: (String) -> Unit): String {
        val zugang = anmeldung.gueltigerZugriffsschluessel()
        val konto = anmeldung.kontoKennungFuer(zugang)
        val anfrage = Request.Builder()
            .url(CodexAnmeldung.ANTWORT_URL)
            .post(nutzlast.toString().toRequestBody(CodexAnmeldung.JSON_TYP))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $zugang")
            .header("originator", "codex_cli_rs")
            .header("User-Agent", "codex_cli_rs/0.0.0 (StackLabor)")
            .header("ChatGPT-Account-ID", konto)
            .build()

        val sammler = StromSammler()
        val fliesstext = FliesstextLeser()
        val verfolgt = anmeldung.mitNamensWiederholung {
            val aufruf = anmeldung.antwortClient().newCall(anfrage)
            laufenderAufruf.set(aufruf)
            try {
                VerfolgterAufruf(aufruf, aufruf.warteAufAntwort())
            } catch (fehler: Throwable) {
                laufenderAufruf.compareAndSet(aufruf, null)
                throw fehler
            }
        }

        try {
            verfolgt.antwort.use { antwort ->
                val koerper = antwort.body
                if (!antwort.isSuccessful) klassifiziereHttpFehler(antwort.code, koerper?.string().orEmpty())
                val strom = koerper?.charStream()?.buffered()
                    ?: throw netzFehler("Codex hat keinen Antwortstrom geliefert.")
                strom.use { offen ->
                    leseSse(
                        strom = offen,
                        aufDaten = { daten ->
                            val stueck = sammler.nimm(daten)
                            if (stueck != null) {
                                val neuerText = fliesstext.nimm(stueck)
                                if (neuerText.isNotEmpty()) aufFortschritt(neuerText)
                            }
                        },
                        istFertig = { sammler.istFertig },
                    )
                }
            }
        } catch (fehler: IOException) {
            if (verfolgt.aufruf.isCanceled()) throw CancellationException("Die Auswertung wurde abgebrochen.")
            throw fehler
        } finally {
            laufenderAufruf.compareAndSet(verfolgt.aufruf, null)
        }
        return sammler.ergebnis()
    }

    private fun istVoruebergehend(fehler: Exception): Boolean = when (fehler) {
        is CodexFehler -> fehler.wiederholbar || fehler.art == CodexFehlerArt.NETZ
        is IOException -> true
        else -> false
    }

    private fun alsCodexFehler(fehler: Exception): CodexFehler = when (fehler) {
        is CodexFehler -> fehler
        is IOException -> netzFehler("Die Antwort von Codex konnte nicht vollständig empfangen werden.", fehler)
        else -> netzFehler(fehler.message ?: "Die Auswertung ist fehlgeschlagen.", fehler)
    }

    private data class VerfolgterAufruf(val aufruf: Call, val antwort: Response)

    companion object {
        /** Ab 13 aktiven Zielen wird zerlegt (F-12, Regeln/Grenzen). */
        internal const val MAX_ZIELE_JE_LAUF = 12
        private const val NETZ_PAUSE_MS = 3_000L
    }
}

/**
 * Zerlegt die Ziele in Läufe zu höchstens [CodexDienst.MAX_ZIELE_JE_LAUF] Zielen.
 * Bei bis zu 24 Zielen sind das genau die zwei Läufe aus F-12 (Ziele 1–12 und der Rest);
 * darüber hinaus wird weiter zerlegt, statt einen zu grossen zweiten Lauf zu schicken.
 */
internal fun zielGruppen(ziele: List<ZielAngabe>): List<List<ZielAngabe>> {
    val sortiert = ziele.sortedBy { it.rang }
    if (sortiert.size <= CodexDienst.MAX_ZIELE_JE_LAUF) return listOf(sortiert)
    return sortiert.chunked(CodexDienst.MAX_ZIELE_JE_LAUF)
}

/** Sammelt die Streaming-Ereignisse und meldet Störungen als [CodexFehler]. */
internal class StromSammler {
    private val stuecke = StringBuilder()
    private var abschlussText: String? = null
    private var fertig = false

    val istFertig: Boolean get() = fertig

    /** Nimmt einen Datenblock an und liefert das neu eingetroffene Textstück, sonst null. */
    fun nimm(daten: String): String? {
        if (daten.isBlank() || daten == "[DONE]") return null
        val ereignis = runCatching { JSONObject(daten) }.getOrElse {
            throw netzFehler("Codex hat ein ungültiges Streaming-Ereignis geliefert.", it)
        }
        return when (ereignis.optString("type")) {
            "response.output_text.delta" -> ereignis.optString("delta").takeIf(String::isNotEmpty)?.also {
                stuecke.append(it)
            }
            "response.completed" -> {
                fertig = true
                abschlussText = ereignis.optJSONObject("response")?.let(::ausgabeText)
                null
            }
            "response.incomplete" -> throw netzFehler("Codex hat die Antwort unvollständig beendet.")
            "response.failed" -> throw stromFehler(ereignis.optJSONObject("response")?.optJSONObject("error"))
            "error" -> throw stromFehler(ereignis.optJSONObject("error") ?: ereignis)
            else -> null
        }
    }

    fun ergebnis(): String {
        if (!fertig) throw netzFehler("Die Verbindung zu Codex endete vor dem Ende der Antwort.")
        return abschlussText?.takeIf(String::isNotBlank)
            ?: stuecke.toString().takeIf(String::isNotBlank)
            ?: throw netzFehler("Codex hat keinen Antworttext geliefert.")
    }
}

/**
 * Löst aus dem noch unvollständigen Antwort-JSON den Fließtext heraus und liefert bei jedem
 * Aufruf nur das neu Hinzugekommene — daraus baut sich die Anzeige wortweise auf (M-13).
 */
internal class FliesstextLeser {
    private val roh = StringBuilder()
    private var bereitsGeliefert = 0

    fun nimm(stueck: String): String {
        roh.append(stueck)
        val sichtbar = fliesstextAus(roh.toString())
        if (sichtbar.length <= bereitsGeliefert) return ""
        val neu = sichtbar.substring(bereitsGeliefert)
        bereitsGeliefert = sichtbar.length
        return neu
    }
}

/**
 * Liest den bisher eingetroffenen Inhalt des Feldes „gesamt" aus einem angefangenen JSON.
 * Kommt statt JSON reiner Fließtext an, wird dieser unverändert zurückgegeben.
 */
internal fun fliesstextAus(rohtext: String): String {
    val schluessel = rohtext.indexOf("\"gesamt\"")
    if (schluessel < 0) return if (rohtext.trimStart().startsWith("{")) "" else rohtext
    val doppelpunkt = rohtext.indexOf(':', schluessel)
    if (doppelpunkt < 0) return ""
    val start = rohtext.indexOf('"', doppelpunkt + 1)
    if (start < 0) return ""

    val text = StringBuilder()
    var stelle = start + 1
    while (stelle < rohtext.length) {
        val zeichen = rohtext[stelle]
        when {
            zeichen == '\\' -> {
                if (stelle + 1 >= rohtext.length) return text.toString()
                when (val folge = rohtext[stelle + 1]) {
                    'n' -> { text.append('\n'); stelle += 2 }
                    't' -> { text.append('\t'); stelle += 2 }
                    'r' -> stelle += 2
                    'b', 'f' -> stelle += 2
                    'u' -> {
                        if (stelle + 5 >= rohtext.length) return text.toString()
                        val nummer = rohtext.substring(stelle + 2, stelle + 6).toIntOrNull(16)
                            ?: return text.toString()
                        text.append(nummer.toChar())
                        stelle += 6
                    }
                    else -> { text.append(folge); stelle += 2 }
                }
            }
            zeichen == '"' -> return text.toString()
            else -> { text.append(zeichen); stelle++ }
        }
    }
    return text.toString()
}

/** Macht aus einem nicht lesbaren Rohtext den bestmöglichen Fließtext. */
internal fun lesbarerText(rohtext: String): String {
    val ausJson = fliesstextAus(rohtext).trim()
    if (ausJson.isNotEmpty()) return ausJson
    return rohtext.trim()
}

/** Liest die SSE-Blöcke Zeile für Zeile und reicht jeden vollständigen Datenblock weiter. */
internal suspend fun leseSse(
    strom: BufferedReader,
    aufDaten: suspend (String) -> Unit,
    istFertig: () -> Boolean = { false },
) {
    val zeilen = mutableListOf<String>()
    suspend fun ausliefern() {
        if (zeilen.isNotEmpty()) {
            aufDaten(zeilen.joinToString("\n"))
            zeilen.clear()
        }
    }
    while (true) {
        val zeile = strom.readLine() ?: break
        when {
            zeile.isEmpty() -> {
                ausliefern()
                if (istFertig()) break
            }
            zeile.startsWith(":") -> Unit
            zeile.startsWith("data:") -> zeilen += zeile.removePrefix("data:").trimStart()
        }
    }
    ausliefern()
}

/** Zieht den Antworttext aus dem Abschluss-Ereignis. */
internal fun ausgabeText(antwort: JSONObject): String? {
    antwort.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val ausgabe = antwort.optJSONArray("output") ?: return null
    return buildString {
        for (aussen in 0 until ausgabe.length()) {
            val inhalt = ausgabe.optJSONObject(aussen)?.optJSONArray("content") ?: continue
            for (innen in 0 until inhalt.length()) {
                val stueck = inhalt.optJSONObject(innen) ?: continue
                val text = stueck.optString("text").takeIf(String::isNotBlank)
                    ?: stueck.optString("output_text").takeIf(String::isNotBlank)
                text?.let(::append)
            }
        }
    }.takeIf(String::isNotBlank)
}

/** Übersetzt einen Fehler aus dem Strom in die drei Fehlerklassen. */
internal fun stromFehler(fehler: JSONObject?): CodexFehler {
    val meldung = fehler?.optString("message")?.takeIf(String::isNotBlank)
        ?: "Codex hat die Antwort abgebrochen."
    val kennzeichen = listOfNotNull(fehler?.optString("code"), fehler?.optString("type"), meldung)
        .joinToString(" ")
        .lowercase()
    val art = when {
        kennzeichen.contains("quota") || kennzeichen.contains("rate_limit") -> CodexFehlerArt.KONTINGENT
        kennzeichen.contains("invalid_grant") || kennzeichen.contains("refresh_token_reused") ||
            kennzeichen.contains("unauthorized") || kennzeichen.contains("authentication") ->
            CodexFehlerArt.NEU_ANMELDEN
        else -> CodexFehlerArt.NETZ
    }
    return CodexFehler(art, meldung)
}
