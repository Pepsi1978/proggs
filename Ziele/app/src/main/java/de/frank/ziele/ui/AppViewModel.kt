package de.frank.ziele.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.ziele.audio.Diktat
import de.frank.ziele.audio.GroqTranscriber
import de.frank.ziele.audio.MicRecorder
import de.frank.ziele.auth.AnthropicKlient
import de.frank.ziele.auth.ChatTurn
import de.frank.ziele.auth.CodexAuthManager
import de.frank.ziele.auth.DeviceAuthInfo
import de.frank.ziele.bruecke.GenialeAufgaben
import de.frank.ziele.bruecke.Jarvis
import de.frank.ziele.data.Einstellungen
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Kontext
import de.frank.ziele.data.Sortierung
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.data.ZieleRepository
import de.frank.ziele.data.sortiert
import de.frank.ziele.ki.Abgleich
import de.frank.ziele.ki.Auswertung
import de.frank.ziele.tts.TtsManager
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Bildschirm {
    data object Liste : Bildschirm
    data class Detail(val id: Long) : Bildschirm
    data class Bearbeiten(val id: Long?) : Bildschirm
    data object Kontext : Bildschirm
    data object Einstellungen : Bildschirm
}

enum class Aufnahme { AUS, LAEUFT, VERARBEITET }

/** Wohin der gesprochene Text geht. */
enum class DiktatZiel { ZIEL, KONTEXT }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ZieleRepository.get(app)
    val einstellungen = Einstellungen.get(app)
    val auth = CodexAuthManager(app)
    val tts = TtsManager(app)
    private val mikro = MicRecorder(app)

    val ziele: StateFlow<List<Ziel>> = repo.ziele.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val vorschlaege: StateFlow<List<Vorschlag>> = repo.vorschlaege.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val auswertungStand = Auswertung.stand
    val auswertungFehler = Auswertung.fehler
    val kontext = Kontext.fluss(app)

    var bildschirm by mutableStateOf<Bildschirm>(Bildschirm.Liste); private set
    var meldung by mutableStateOf<String?>(null); private set
    var rueckgaengig by mutableStateOf<(() -> Unit)?>(null); private set
    var heute by mutableLongStateOf(LocalDate.now().toEpochDay()); private set

    // ---- Editor ----
    var eId by mutableStateOf<Long?>(null); private set
    var eTitel by mutableStateOf("")
    var eBeschreibung by mutableStateOf("")
    var eWarum by mutableStateOf("")
    var eFrist by mutableStateOf<Long?>(null)
    var eSymbol by mutableStateOf("🎯")
    var eStatus by mutableStateOf(ZielStatus.AKTIV)
    var eAuswerten by mutableStateOf(true)
    var ordnet by mutableStateOf(false); private set
    private var editorQuelle: Ziel? = null

    // ---- Aufnahme ----
    var aufnahme by mutableStateOf(Aufnahme.AUS); private set
    var aufnahmeStart by mutableLongStateOf(0L); private set
    var diktatZiel by mutableStateOf(DiktatZiel.ZIEL); private set
    var mikrofonAnfragen: () -> Unit = {}
    private var aufnahmeJob: Job? = null

    // ---- KI-Verbindung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    val kiBereit: Boolean get() = auth.kannDenken(einstellungen.modell)
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set
    var kiFehler by mutableStateOf<String?>(null); private set
    var kiVerbindet by mutableStateOf(false); private set

    // ---- Vorlesen ----
    var sprichtId by mutableStateOf<String?>(null); private set

    /** Vorschläge, deren Übernahme gerade läuft (Plus dreht sich). */
    var uebernimmt by mutableStateOf<Set<Long>>(emptySet()); private set

    init { abgleichen() }

    fun wiederDa() {
        heute = LocalDate.now().toEpochDay()
        kiVerbunden = auth.isConnected
        abgleichen()
    }

    /** Erledigt-Stand aus Geniale Aufgaben holen (still; nicht erreichbar heißt nur: alter Stand bleibt). */
    fun abgleichen() { viewModelScope.launch { runCatching { Abgleich.stand(getApplication()) } } }

    fun zeige(b: Bildschirm) { bildschirm = b }

    fun zurueck(): Boolean {
        when (val b = bildschirm) {
            Bildschirm.Liste -> return false
            is Bildschirm.Bearbeiten -> { aufnahmeVerwerfen(); bildschirm = b.id?.let { Bildschirm.Detail(it) } ?: Bildschirm.Liste }
            Bildschirm.Kontext -> { aufnahmeVerwerfen(); bildschirm = Bildschirm.Liste }
            else -> bildschirm = Bildschirm.Liste
        }
        return true
    }

    fun melde(text: String, undo: (() -> Unit)? = null) { meldung = text; rueckgaengig = undo }
    fun meldungWeg() { meldung = null; rueckgaengig = null }

    // ================= Ziele =================

    fun neuesZiel(mitMikro: Boolean = false) {
        editorQuelle = null
        eId = null; eTitel = ""; eBeschreibung = ""; eWarum = ""; eFrist = null; eSymbol = "🎯"; eStatus = ZielStatus.AKTIV
        eAuswerten = true
        bildschirm = Bildschirm.Bearbeiten(null)
        if (mitMikro) mikroTippen(DiktatZiel.ZIEL)
    }

    fun bearbeiten(z: Ziel) {
        editorQuelle = z
        eId = z.id; eTitel = z.titel; eBeschreibung = z.beschreibung; eWarum = z.warum; eFrist = z.frist; eSymbol = z.symbol; eStatus = z.zustand
        eAuswerten = false
        bildschirm = Bildschirm.Bearbeiten(z.id)
    }

    fun speichern() {
        val titel = eTitel.trim()
        if (titel.isEmpty()) { melde("Gib dem Ziel einen Titel – oder sprich es einfach ein."); return }
        val alt = editorQuelle
        val auswerten = eAuswerten
        val beschreibung = eBeschreibung.trim(); val warum = eWarum.trim(); val frist = eFrist; val symbol = eSymbol.ifBlank { "🎯" }; val status = eStatus
        viewModelScope.launch {
            // Frisch aus der Datenbank: Eine Auswertung kann während des Bearbeitens Bericht und Strategie geschrieben haben.
            val basis = alt?.let { repo.ziel(it.id) } ?: alt ?: Ziel(titel = titel)
            val neu = basis.copy(
                titel = titel, beschreibung = beschreibung, warum = warum, frist = frist, symbol = symbol, status = status.name,
                erreichtAm = if (status == ZielStatus.ERREICHT) (basis.erreichtAm ?: System.currentTimeMillis()) else null,
            )
            val id = if (alt == null) repo.neu(neu) else { repo.speichere(neu); neu.id }
            bildschirm = Bildschirm.Detail(id)
            if (alt == null && auswerten) auswerten(id) else melde(if (alt == null) "Ziel angelegt" else "Gespeichert")
            if (alt?.zustand != ZielStatus.ERREICHT && eStatus == ZielStatus.ERREICHT) melde("Glückwunsch! Ziel erreicht 🏆")
        }
    }

    fun statusSetzen(z: Ziel, s: ZielStatus) {
        viewModelScope.launch {
            repo.speichere(z.copy(status = s.name, erreichtAm = if (s == ZielStatus.ERREICHT) System.currentTimeMillis() else null))
            melde(when (s) { ZielStatus.ERREICHT -> "Glückwunsch! Ziel erreicht 🏆"; ZielStatus.PAUSIERT -> "Ziel pausiert"; ZielStatus.AKTIV -> "Ziel wieder aktiv" })
        }
    }

    fun loeschen(z: Ziel) {
        viewModelScope.launch {
            Auswertung.abbrechen(z.id)
            val v = repo.vorschlaege(z.id)
            repo.loesche(z)
            bildschirm = Bildschirm.Liste
            melde("„${z.titel}“ gelöscht") { viewModelScope.launch { repo.wiederherstellen(z, v) } }
        }
    }

    fun reihenfolge(ids: List<Long>) { viewModelScope.launch { repo.reihenfolge(ids) } }

    /**
     * Ordnet frei Gesprochenes in Titel, Beschreibung, Warum und Frist (KI). Ohne KI bleibt der Text in der Beschreibung
     * und die ersten Wörter werden zum Titel.
     */
    private fun zielOrdnen(gesprochen: String) {
        if (!kiBereit) {
            if (eTitel.isBlank()) eTitel = gesprochen.split(Regex("(?<=[.!?])\\s")).first().take(60).trimEnd('.', '!', '?')
            eBeschreibung = (eBeschreibung.trim() + " " + gesprochen).trim()
            return
        }
        ordnet = true
        viewModelScope.launch {
            try {
                val bisher = buildString {
                    if (eTitel.isNotBlank()) append("Bisheriger Titel: ").append(eTitel).append('\n')
                    if (eBeschreibung.isNotBlank()) append("Bisherige Beschreibung: ").append(eBeschreibung).append('\n')
                    if (eWarum.isNotBlank()) append("Bisheriges Warum: ").append(eWarum).append('\n')
                }
                val antwort = auth.streamChat(
                    instructions = "Frank hat ein persönliches Ziel frei eingesprochen (Spracherkennung, daher unsauber). Ordne es, ohne etwas zu erfinden. " +
                        "Heute ist ${LocalDate.now()}. Antworte NUR mit JSON: {\"titel\": \"kurz und klar, höchstens 50 Zeichen, als Ziel formuliert (z. B. „10 kg abnehmen“)\", " +
                        "\"beschreibung\": \"was genau erreicht werden soll, möglichst messbar, ein bis drei Sätze in Franks Worten\", " +
                        "\"warum\": \"warum es ihm wichtig ist, nur wenn er es gesagt hat, sonst leer\", " +
                        "\"frist\": \"JJJJ-MM-TT, nur wenn er ein Datum oder einen Zeitraum genannt hat, sonst leer\", \"symbol\": \"ein passendes Emoji\"}. " +
                        "Gibt es schon Angaben, ergänze sie mit dem Neuen und behalte, was nicht widersprochen wird.",
                    turns = listOf(ChatTurn("user", bisher + "Neu gesprochen: " + gesprochen)),
                    model = einstellungen.modell,
                    reasoningEffort = de.frank.ziele.auth.ReasoningEffort.LOW,
                )
                val o = Auswertung.jsonAus(antwort) ?: throw IllegalStateException("Die KI hat das Ziel nicht ordnen können.")
                o.optString("titel").trim().takeIf { it.isNotEmpty() }?.let { eTitel = it }
                o.optString("beschreibung").trim().takeIf { it.isNotEmpty() }?.let { eBeschreibung = it }
                o.optString("warum").trim().takeIf { it.isNotEmpty() }?.let { eWarum = it }
                o.optString("frist").trim().takeIf { it.length >= 10 }?.let { f -> runCatching { LocalDate.parse(f.take(10)).toEpochDay() }.getOrNull()?.let { eFrist = it } }
                o.optString("symbol").trim().takeIf { it.isNotEmpty() && it.length <= 8 && eSymbol == "🎯" }?.let { eSymbol = it }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                if (eTitel.isBlank()) eTitel = gesprochen.take(50)
                eBeschreibung = (eBeschreibung.trim() + " " + gesprochen).trim()
                melde(e.message ?: "Die KI hat das Ziel nicht ordnen können; der Text steht in der Beschreibung.")
            } finally {
                ordnet = false
            }
        }
    }

    // ================= Auswertung =================

    fun auswerten(id: Long, wunsch: String = "") {
        if (!kiBereit) {
            melde(if (einstellungen.modell.istAnthropic) "Bitte zuerst in den Einstellungen den Anthropic-API-Schlüssel eintragen." else "Bitte zuerst in den Einstellungen mit ChatGPT verbinden.")
            bildschirm = Bildschirm.Einstellungen
            return
        }
        val gestartet = Auswertung.starte(getApplication(), id, wunsch) { fehler ->
            melde(fehler?.let { "Auswertung: $it" } ?: "Auswertung fertig ✨")
        }
        if (gestartet) melde("Auswertung läuft – du kannst die App ruhig verlassen.")
    }

    fun auswertungAbbrechen(id: Long) = Auswertung.abbrechen(id)

    fun uebernehmen(v: Vorschlag) {
        if (v.aufgabeId != null || v.id in uebernimmt) return
        uebernimmt = uebernimmt + v.id
        viewModelScope.launch {
            try {
                Abgleich.uebernehmen(getApplication(), v.id)
                melde("„${v.titel}“ steht jetzt in Geniale Aufgaben ✓")
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Übernehmen hat nicht geklappt.")
            } finally {
                uebernimmt = uebernimmt - v.id
            }
        }
    }

    /** Alle offenen notwendigen Vorschläge eines Ziels auf einmal übernehmen. */
    fun alleNotwendigenUebernehmen(zielId: Long) {
        val offen = vorschlaege.value.filter { it.zielId == zielId && it.gruppe == Kategorie.NOTWENDIG && it.aufgabeId == null && !it.verworfen }
        if (offen.isEmpty()) { melde("Alle notwendigen Aufgaben sind schon übernommen."); return }
        uebernimmt = uebernimmt + offen.map { it.id }
        viewModelScope.launch {
            var gut = 0
            var fehler: String? = null
            offen.sortiert(Sortierung.REIHENFOLGE).forEach { v ->
                try { Abgleich.uebernehmen(getApplication(), v.id); gut++ } catch (c: CancellationException) { throw c } catch (e: Throwable) { fehler = e.message }
                uebernimmt = uebernimmt - v.id
            }
            melde(if (fehler == null) "$gut Aufgaben in Geniale Aufgaben übernommen ✓" else "$gut übernommen. $fehler")
        }
    }

    fun verwerfen(v: Vorschlag, verworfen: Boolean = true) {
        viewModelScope.launch {
            repo.speichere(v.copy(verworfen = verworfen))
            if (verworfen) melde("Vorschlag ausgeblendet") { viewModelScope.launch { repo.speichere(v.copy(verworfen = false)) } }
        }
    }

    fun sortieren(s: Sortierung) { einstellungen.sortierung = s }

    // ================= Kontextdatei =================

    fun kontextSpeichern(basis: String, text: String) { Kontext.schreibeGetippt(getApplication(), basis, text) }

    fun kontextVorlesen() {
        val text = Kontext.lies(getApplication()).trim()
        vorlesen("kontext", text.ifEmpty { "Deine Kontextdatei ist noch leer." })
    }

    // ================= Aufnahme =================

    fun mikroTippen(ziel: DiktatZiel) {
        when (aufnahme) {
            Aufnahme.AUS -> {
                if (einstellungen.groqKey.isBlank()) { melde("Bitte zuerst den Groq-Schlüssel in den Einstellungen eintragen (oder aus Jarvis übernehmen)."); return }
                diktatZiel = ziel
                mikrofonAnfragen()
            }
            Aufnahme.LAEUFT -> aufnahmeBeenden()
            Aufnahme.VERARBEITET -> Unit
        }
    }

    fun mikrofonErlaubnisErgebnis(ok: Boolean) {
        if (!ok) { melde("Ohne Mikrofon-Erlaubnis geht die Spracheingabe nicht."); return }
        if (aufnahme != Aufnahme.AUS) return
        tts.stop(); sprichtId = null
        if (mikro.start(viewModelScope)) {
            aufnahme = Aufnahme.LAEUFT
            aufnahmeStart = System.currentTimeMillis()
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun aufnahmeVerwerfen() {
        if (aufnahme == Aufnahme.LAEUFT) mikro.release()
        aufnahmeJob?.cancel()
        aufnahme = Aufnahme.AUS
    }

    private fun aufnahmeBeenden() {
        aufnahme = Aufnahme.VERARBEITET
        val wohin = diktatZiel
        aufnahmeJob = viewModelScope.launch {
            val transcriber = GroqTranscriber(
                apiKey = einstellungen.groqKey,
                model = einstellungen.whisperModell,
                woerterbuchPrompt = einstellungen.woerterbuchPrompt,
                filterStille = einstellungen.filterStille,
                filterMetriken = einstellungen.filterMetriken,
                filterZeitstempel = einstellungen.filterZeitstempel,
                filterFloskeln = einstellungen.filterFloskeln,
            )
            try {
                val wav = mikro.stop()
                if (wav == null) { melde("Es wurde nichts aufgenommen."); return@launch }
                val text = Diktat(transcriber).transkribiere(wav).text.trim()
                when {
                    text.isBlank() -> melde("Keine Sprache erkannt.")
                    wohin == DiktatZiel.KONTEXT -> { Kontext.ergaenze(getApplication(), text); melde("In die Kontextdatei übernommen ✓") }
                    else -> zielOrdnen(text)
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Die Spracherkennung ist fehlgeschlagen.")
            } finally {
                transcriber.shutdown()
                aufnahme = Aufnahme.AUS
            }
        }
    }

    // ================= Vorlesen =================

    fun vorlesen(schluessel: String, text: String) {
        if (sprichtId == schluessel) { tts.stop(); sprichtId = null; return }
        sprichtId = schluessel
        tts.speak(
            text = text,
            onStart = {},
            onComplete = { if (sprichtId == schluessel) sprichtId = null },
            onError = { e -> sprichtId = null; melde(e.message ?: "Vorlesen fehlgeschlagen.") },
        )
    }

    /** Liest Ziel, Strategie und die wichtigsten offenen Aufgaben in der gewählten Sortierung vor. */
    fun planVorlesen(z: Ziel) {
        val alle = vorschlaege.value.filter { it.zielId == z.id && !it.verworfen }
        val text = buildString {
            append("Dein Ziel: ").append(z.titel).append(". ")
            z.frist?.let { val t = it - heute; if (t > 0) append("Noch $t ${if (t == 1L) "Tag" else "Tage"}. ") }
            if (z.strategie.isNotBlank()) append(z.strategie.trim()).append(' ')
            val notwendig = alle.filter { it.gruppe == Kategorie.NOTWENDIG && !it.erledigt }.sortiert(einstellungen.sortierung)
            val hilfe = alle.filter { it.gruppe == Kategorie.UNTERSTUETZEND && !it.erledigt }.sortiert(einstellungen.sortierung)
            if (notwendig.isNotEmpty()) {
                append("Notwendig sind: ")
                notwendig.take(8).forEachIndexed { i, v -> append(if (i == 0) "" else ", ").append(v.titel) }
                append(". ")
            }
            if (hilfe.isNotEmpty()) {
                append("Unterstützend helfen: ")
                hilfe.take(5).forEachIndexed { i, v -> append(if (i == 0) "" else ", ").append(v.titel) }
                append(". ")
            }
            if (alle.isEmpty()) append("Noch gibt es keine Aufgabenvorschläge. Tippe auf Auswerten.")
        }
        vorlesen("plan_${z.id}", text)
    }

    fun berichtVorlesen(z: Ziel) = vorlesen("bericht_${z.id}", z.bericht.ifBlank { "Für dieses Ziel gibt es noch keine Auswertung." })

    fun alleZieleVorlesen() {
        val aktiv = ziele.value.filter { it.zustand == ZielStatus.AKTIV }
        val text = if (aktiv.isEmpty()) "Du hast gerade keine aktiven Ziele." else buildString {
            append(if (aktiv.size == 1) "Du hast ein aktives Ziel. " else "Du hast ${aktiv.size} aktive Ziele. ")
            aktiv.forEach { z ->
                append(z.titel).append(". ")
                z.frist?.let { val t = it - heute; if (t >= 0) append(if (t == 0L) "Heute ist der Stichtag. " else "Noch $t ${if (t == 1L) "Tag" else "Tage"}. ") }
            }
        }
        vorlesen("alle", text)
    }

    // ================= KI-Verbindung und Schlüssel =================

    fun kiVerbinden(activity: ComponentActivity) {
        if (kiVerbindet) return
        kiFehler = null
        kiVerbindet = true
        viewModelScope.launch {
            try {
                auth.login(activity) { info -> geraeteCode = info }
                kiVerbunden = true
                melde("Mit ChatGPT verbunden")
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                kiFehler = e.message
            } finally {
                geraeteCode = null
                kiVerbindet = false
            }
        }
    }

    fun kiAbbrechen() { auth.cancelLogin(); geraeteCode = null; kiVerbindet = false }

    fun kiTrennen() {
        auth.cancelLogin(); auth.logout()
        kiVerbunden = false; geraeteCode = null
    }

    /** Übernimmt Groq-, Anthropic- und Stimmen-Schlüssel aus Jarvis (Signatur-Brücke), damit nichts doppelt einzutippen ist. */
    fun schluesselAusJarvis(fertig: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val z = Jarvis.zugang(getApplication())
                val e = einstellungen
                var anzahl = 0
                fun nimm(name: String, setzen: (String) -> Unit) { z.optString(name).trim().takeIf { it.isNotEmpty() }?.let { setzen(it); anzahl++ } }
                nimm("groq") { e.groqKey = it }
                nimm("anthropic") { AnthropicKlient.schluesselSpeichern(getApplication(), it) }
                nimm("google_tts") { e.googleKey = it }
                nimm("gemini_frei") { e.geminiKeyFrei = it }
                nimm("gemini_bezahlt") { e.geminiKeyBezahlt = it }
                nimm("azure") { e.azureKey = it }
                nimm("azure_region") { e.azureRegion = it }
                nimm("qwen") { e.qwenKey = it }
                nimm("qwen_stimme") { e.qwenStimme = it }
                melde(if (anzahl == 0) "Jarvis hat keine Schlüssel geliefert." else "$anzahl Schlüssel aus Jarvis übernommen ✓")
                fertig()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Jarvis ist nicht erreichbar.")
            }
        }
    }

    fun aufgabenOeffnen() {
        val ctx = getApplication<Application>()
        val start = ctx.packageManager.getLaunchIntentForPackage(GenialeAufgaben.PAKET)
        if (start == null) { melde("Geniale Aufgaben ist nicht installiert."); return }
        ctx.startActivity(start.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onCleared() {
        mikro.release()
        tts.shutdown()
        super.onCleared()
    }
}
