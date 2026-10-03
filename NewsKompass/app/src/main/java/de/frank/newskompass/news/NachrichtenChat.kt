package de.frank.newskompass.news

import android.content.Context
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.audio.GroqTranscriber
import de.frank.newskompass.audio.MicRecorder
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.observability.KompassLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ChatRolle { DU, KI }

data class ChatNachricht(
    val id: Long,
    val rolle: ChatRolle,
    val text: String,
    /** Adressen, die die Websuche für diese Antwort benutzt hat. */
    val quellen: List<String> = emptyList(),
    /** Eine Fehlermeldung statt einer Antwort — sie geht nicht ins Gespräch mit der KI ein. */
    val fehler: Boolean = false,
    /** Die Antwort wurde mit „Stopp“ abgebrochen; [text] ist, was bis dahin kam. */
    val abgebrochen: Boolean = false,
)

data class ChatZustand(
    /** Gespräche je Meldungs-Kennung, in der Reihenfolge, in der sie geführt wurden. */
    val gespraeche: Map<String, List<ChatNachricht>> = emptyMap(),
    /** Die Meldung, zu der die KI gerade antwortet. */
    val antwortetFuer: String? = null,
    /** Was von der laufenden Antwort schon angekommen ist. */
    val teilText: String = "",
    val sprachStufe: SprachStufe = SprachStufe.BEREIT,
    /** Hinweis über dem Eingabefeld, etwa „Ich habe nichts verstanden.“ */
    val meldung: String = "",
    /** Die Meldung lässt sich nur in den Einstellungen beheben (Schlüssel, Anmeldung). */
    val zuEinstellungen: Boolean = false,
    /** Jede Antwort wird vorgelesen, auch getippte. Eingesprochene Fragen werden immer vorgelesen. */
    val autoVorlesen: Boolean = false,
)

/**
 * Das Gespräch mit der KI über eine einzelne Meldung.
 *
 * Getippt oder eingesprochen: Die Aufnahme läuft wie beim Mikrofon-Knopf über Groq Whisper mit den
 * vier Schichten gegen Stille-Halluzinationen und wird ohne weiteren Tipp abgeschickt; die Antwort
 * darauf wird vorgelesen, damit ein echtes Hin und Her entsteht. Die KI bekommt die Meldung samt
 * Quellen und dem bisherigen Gespräch und darf im Netz nachsehen.
 *
 * Gespräche leben so lange wie die App — Schließen und erneutes Öffnen des Chats behält sie.
 */
class NachrichtenChat(private val app: NewsApplication) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mikrofon = MicRecorder(app)
    private val ablage = app.getSharedPreferences("chat", Context.MODE_PRIVATE)

    private val _zustand = MutableStateFlow(ChatZustand(autoVorlesen = ablage.getBoolean(AUTO_VORLESEN, false)))
    val zustand: StateFlow<ChatZustand> = _zustand.asStateFlow()

    /** Zählt jeden Aufnahme-Start und -Abbruch — eine veraltete Transkription darf nichts mehr auslösen. */
    private var generation = 0
    private var naechsteId = 0L
    private var antwortJob: Job? = null
    /** Eine Antwort, deren Gespräch geleert wurde — ihr Teiltext darf nicht wieder auftauchen. */
    private var verworfenerJob: Job? = null
    /** Die Meldung, deren Chat gerade offen ist. Nur dorthin wird eine fertige Antwort vorgelesen. */
    private var offenFuer: String? = null
    private var mikrofonMeldung: Meldung? = null

    // --- Schreiben ------------------------------------------------------------------------------

    /** Schickt [text] zur Meldung ab. [vorlesen] liest die Antwort vor, sobald sie fertig ist. */
    fun sende(meldung: Meldung, text: String, vorlesen: Boolean = _zustand.value.autoVorlesen) {
        val frage = text.trim()
        if (frage.isBlank() || _zustand.value.antwortetFuer != null) return
        if (!app.codex.istVerbunden) {
            melde("Bitte zuerst in den Einstellungen bei Codex anmelden.", zuEinstellungen = true)
            return
        }
        val bisher = gespraech(meldung.id).filter { !it.fehler }
        haengeAn(meldung.id, ChatNachricht(++naechsteId, ChatRolle.DU, frage))
        _zustand.update { it.copy(antwortetFuer = meldung.id, teilText = "", meldung = "", zuEinstellungen = false) }
        KompassLog.info("NachrichtenChat", "sende", "Frage zur Meldung", mapOf("zeichen" to frage.length, "verlauf" to bisher.size))

        val stand = app.einstellungen.stand.value
        val denktiefe = stand.modelle.firstOrNull { it.id == stand.modellId }?.stufen
            ?.let { stufen -> "low".takeIf { it in stufen } ?: stand.denktiefe } ?: stand.denktiefe
        var job: Job? = null
        job = bereich.launch {
            try {
                val antwort = app.codex.frage(
                    anweisung = anweisung(),
                    eingabe = eingabe(meldung, bisher, frage),
                    modellId = stand.modellId,
                    denktiefe = denktiefe,
                    werkzeuge = JSONArray().put(JSONObject().put("type", "web_search")),
                    beiTeilstueck = { stueck -> _zustand.update { it.copy(teilText = it.teilText + stueck) } },
                )
                val sauber = QuellenFilter.entferne(antwort.text).ifBlank { "Dazu habe ich gerade keine Antwort bekommen. Frag gern noch einmal." }
                val nachricht = ChatNachricht(++naechsteId, ChatRolle.KI, sauber, quellen = antwort.quellen.distinct().take(MAX_QUELLEN))
                haengeAn(meldung.id, nachricht)
                if (vorlesen && offenFuer == meldung.id) app.vorleser.lies(vorleseId(meldung.id, nachricht.id), sauber)
            } catch (abbruch: CancellationException) {
                // Gestoppt: Was schon da war, bleibt als abgebrochene Antwort stehen.
                val teil = QuellenFilter.entferne(_zustand.value.teilText)
                if (teil.isNotBlank() && verworfenerJob !== job) haengeAn(meldung.id, ChatNachricht(++naechsteId, ChatRolle.KI, teil, abgebrochen = true))
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.warn("NachrichtenChat", "sende", "Antwort gescheitert", mapOf("grund" to fehler.message))
                haengeAn(
                    meldung.id,
                    ChatNachricht(++naechsteId, ChatRolle.KI, fehler.message ?: "Die Antwort ist fehlgeschlagen. Versuch es gleich noch einmal.", fehler = true),
                )
            } finally {
                if (antwortJob === job) {
                    antwortJob = null
                    _zustand.update { it.copy(antwortetFuer = null, teilText = "") }
                }
            }
        }
        antwortJob = job
    }

    /** Nach einem Fehler: die letzte Frage noch einmal stellen, ohne sie doppelt anzuzeigen. */
    fun wiederhole(meldung: Meldung) {
        if (_zustand.value.antwortetFuer != null) return
        val verlauf = gespraech(meldung.id)
        val frage = verlauf.lastOrNull { it.rolle == ChatRolle.DU } ?: return
        if (verlauf.lastOrNull()?.fehler != true) return
        _zustand.update { z -> z.copy(gespraeche = z.gespraeche + (meldung.id to verlauf.takeWhile { it.id != frage.id })) }
        sende(meldung, frage.text)
    }

    /** Hält die laufende Antwort an — nur diese, nicht die Recherche im Hintergrund. */
    fun stoppeAntwort() {
        antwortJob?.cancel()
    }

    /** Fängt das Gespräch zu [meldungId] von vorn an. */
    fun leere(meldungId: String) {
        if (_zustand.value.antwortetFuer == meldungId) {
            verworfenerJob = antwortJob
            stoppeAntwort()
        }
        if (app.vorleser.zustand.value.quelleId.startsWith("chat-$meldungId-")) app.vorleser.stoppe()
        _zustand.update { it.copy(gespraeche = it.gespraeche - meldungId) }
    }

    /** Der Chat zu [meldungId] ist zu sehen; null, sobald er geschlossen ist. */
    fun sichtbar(meldungId: String?) {
        offenFuer = meldungId
    }

    fun setzeAutoVorlesen(an: Boolean) {
        ablage.edit().putBoolean(AUTO_VORLESEN, an).apply()
        _zustand.update { it.copy(autoVorlesen = an) }
    }

    fun gespraech(meldungId: String): List<ChatNachricht> = _zustand.value.gespraeche[meldungId].orEmpty()

    // --- Sprechen -------------------------------------------------------------------------------

    /** Einmal tippen und sprechen, noch einmal tippen: Die Frage geht sofort ab. */
    fun tippeMikrofon(meldung: Meldung, erlaubnisDa: Boolean, frageErlaubnis: () -> Unit) {
        when (_zustand.value.sprachStufe) {
            SprachStufe.BEREIT -> {
                if (_zustand.value.antwortetFuer != null) return
                if (app.einstellungen.groqSchluessel.isBlank()) {
                    melde("Zum Einsprechen bitte in den Einstellungen einen Groq-Schlüssel hinterlegen.", zuEinstellungen = true)
                    return
                }
                if (!app.codex.istVerbunden) {
                    melde("Bitte zuerst in den Einstellungen bei Codex anmelden.", zuEinstellungen = true)
                    return
                }
                mikrofonMeldung = meldung
                if (erlaubnisDa) starte() else frageErlaubnis()
            }
            SprachStufe.NIMMT_AUF -> stoppeUndSende()
            SprachStufe.VERSTEHT -> Unit
        }
    }

    fun erlaubnisErhalten(erteilt: Boolean) {
        if (erteilt) starte() else melde("Ohne Mikrofon-Erlaubnis kann ich dich nicht hören.")
    }

    private fun starte() {
        if (_zustand.value.sprachStufe != SprachStufe.BEREIT || mikrofonMeldung == null) return
        // Sonst nimmt das Mikrofon die eigene Vorlesestimme mit auf.
        app.vorleser.stoppe()
        generation++
        if (mikrofon.start(bereich)) {
            _zustand.update { it.copy(sprachStufe = SprachStufe.NIMMT_AUF, meldung = "", zuEinstellungen = false) }
            KompassLog.info("NachrichtenChat", "starte", "Aufnahme gestartet")
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun stoppeUndSende() {
        val meldung = mikrofonMeldung ?: return
        _zustand.update { it.copy(sprachStufe = SprachStufe.VERSTEHT) }
        val meineGeneration = generation
        bereich.launch {
            var transkribierer: GroqTranscriber? = null
            try {
                val wav = mikrofon.stop()
                if (meineGeneration != generation) return@launch
                if (wav == null) {
                    melde("Ich habe nichts verstanden.")
                    return@launch
                }
                transkribierer = GroqTranscriber(app.einstellungen.groqSchluessel)
                val text = transkribierer.transcribe(wav).trim()
                if (meineGeneration != generation) return@launch
                if (text.isBlank()) {
                    melde("Ich habe nichts verstanden.")
                    return@launch
                }
                _zustand.update { it.copy(sprachStufe = SprachStufe.BEREIT) }
                sende(meldung, text, vorlesen = true)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.warn("NachrichtenChat", "stoppeUndSende", "Spracheingabe gescheitert", mapOf("grund" to fehler.message))
                if (meineGeneration == generation) melde(fehler.message ?: "Die Spracheingabe ist fehlgeschlagen.")
            } finally {
                transkribierer?.shutdown()
                if (meineGeneration == generation) _zustand.update { it.copy(sprachStufe = SprachStufe.BEREIT) }
            }
        }
    }

    /** Verwirft eine laufende Aufnahme, etwa wenn die App in den Hintergrund geht oder der Chat schließt. */
    fun brichAufnahmeAb() {
        if (_zustand.value.sprachStufe != SprachStufe.NIMMT_AUF) return
        generation++
        mikrofon.release()
        _zustand.update { it.copy(sprachStufe = SprachStufe.BEREIT) }
        KompassLog.info("NachrichtenChat", "brichAufnahmeAb", "Aufnahme verworfen")
    }

    fun loescheMeldung() = _zustand.update { it.copy(meldung = "", zuEinstellungen = false) }

    private fun melde(text: String, zuEinstellungen: Boolean = false) =
        _zustand.update { it.copy(meldung = text, zuEinstellungen = zuEinstellungen) }

    private fun haengeAn(meldungId: String, nachricht: ChatNachricht) =
        _zustand.update { it.copy(gespraeche = it.gespraeche + (meldungId to (it.gespraeche[meldungId].orEmpty() + nachricht))) }

    // --- Auftrag an die KI ----------------------------------------------------------------------

    private fun anweisung(): String {
        val heute = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMANY))
        return "Du bist die KI von News Kompass und sprichst mit dem Nutzer über genau eine Nachrichtenmeldung, " +
            "die er gerade gelesen hat. Heute ist $heute. Antworte auf Deutsch mit echten Umlauten, klar, " +
            "freundlich und auf Augenhöhe, wie in einem guten Gespräch. Bleib bei den Fakten: Mach deutlich, was in " +
            "der Meldung steht, was du zusätzlich weißt oder im Netz geprüft hast und was Einschätzung ist; " +
            "kennzeichne Vermutungen ausdrücklich und erfinde nichts. Betrifft die Frage neuere Entwicklungen oder " +
            "bist du unsicher, sieh mit der Websuche nach und nimm nur seriöse, faktenbasierte Quellen, keine " +
            "Propagandaseiten. Äußert der Nutzer eine Meinung, nimm sie ernst, nenne Argumente dafür und dagegen " +
            "und unterschiedliche Perspektiven. Antworte knapp, meist in 1 bis 3 kurzen Absätzen, außer der Nutzer " +
            "will ausdrücklich mehr. Gerne darfst du am Ende eine kurze Rückfrage stellen, die das Gespräch " +
            "weiterbringt. Schreibe reinen Fließtext: kein Markdown, keine Aufzählungszeichen, keine Überschriften, " +
            "keine Links und keine Quellenangaben im Text. Trenne Absätze durch eine Leerzeile. Deine Antwort wird " +
            "auch vorgelesen, also schreib gut sprechbar und ohne Abkürzungen, die man nicht aussprechen kann."
    }

    private fun eingabe(meldung: Meldung, bisher: List<ChatNachricht>, frage: String): String = buildString {
        appendLine("Die Meldung, über die ihr sprecht:")
        appendLine("Überschrift: ${meldung.titel}")
        if (meldung.wann.isNotBlank()) appendLine("Zeitpunkt: ${meldung.wann}")
        if (meldung.istUpdate) appendLine("(Folgemeldung zu einer früheren Nachricht)")
        appendLine()
        QuellenFilter.entferne(meldung.absaetze).forEach { appendLine(it).appendLine() }
        if (meldung.quellen.isNotEmpty()) {
            appendLine("Quellen der Meldung:")
            meldung.quellen.distinct().take(MAX_QUELLEN).forEach { appendLine("- $it") }
            appendLine()
        }
        val verlauf = bisher.filter { !it.fehler }.takeLast(MAX_VERLAUF)
        if (verlauf.isNotEmpty()) {
            appendLine("Bisheriges Gespräch:")
            verlauf.forEach { n ->
                append(if (n.rolle == ChatRolle.DU) "Nutzer: " else "Du: ")
                appendLine(n.text.replace("\n\n", "\n"))
            }
            appendLine()
        }
        appendLine("Neue Nachricht des Nutzers:")
        append(frage)
    }

    companion object {
        /** Vorlese-Kennung einer Antwort im Chat. */
        fun vorleseId(meldungId: String, nachrichtId: Long): String = "chat-$meldungId-$nachrichtId"

        private const val AUTO_VORLESEN = "auto_vorlesen"
        private const val MAX_QUELLEN = 6
        private const val MAX_VERLAUF = 20
    }
}
