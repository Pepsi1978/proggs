package de.frank.longevity.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.longevity.audio.Diktat
import de.frank.longevity.audio.GroqTranscriber
import de.frank.longevity.audio.MicRecorder
import de.frank.longevity.auth.CodexAuthManager
import de.frank.longevity.auth.DeviceAuthInfo
import de.frank.longevity.data.Einstellungen
import de.frank.longevity.data.Evidenz
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.Kategorie
import de.frank.longevity.data.Punkt
import de.frank.longevity.data.Repository
import de.frank.longevity.data.platz
import de.frank.longevity.ki.Art
import de.frank.longevity.ki.KiArbeit
import de.frank.longevity.ki.LongevityKi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Bildschirm {
    data object Liste : Bildschirm
    data class Detail(val id: Long) : Bildschirm
    data object Neu : Bildschirm
    data object Einstellungen : Bildschirm
    data object Protokoll : Bildschirm
    data object Prompt : Bildschirm
}

enum class Aufnahme { AUS, LAEUFT, VERARBEITET }

/** Ein nächster Schritt für den Heute-Kopf. */
data class Schritt(val faktor: Faktor, val index: Int, val punkt: Punkt)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    val einstellungen = Einstellungen.get(app)
    val auth = CodexAuthManager(app)
    private val ki = LongevityKi(auth, einstellungen) { standardPrompt() }
    private val mikro = MicRecorder(app)

    val alle: StateFlow<List<Faktor>> = repo.alle.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var bildschirm by mutableStateOf<Bildschirm>(Bildschirm.Liste); private set
    private val verlauf = ArrayDeque<Bildschirm>()
    var meldung by mutableStateOf<String?>(null); private set
    var rueckgaengig by mutableStateOf<(() -> Unit)?>(null); private set
    var konfetti by mutableIntStateOf(0); private set
    var hervorgehoben by mutableStateOf<Long?>(null)
    /** Kurzbewertung der KI zu eingesprochenen Ideen, je Faktor-ID (bis zum Neustart). */
    val bewertungen = mutableStateMapOf<Long, String>()

    // ---- Eingabe einer neuen Idee ----
    var eText by mutableStateOf("")
    var textVorKorrektur by mutableStateOf<String?>(null); private set
    private var korrekturFassungen = emptyList<String>()
    var korrigiert by mutableStateOf(false); private set

    // ---- Aufnahme ----
    var aufnahme by mutableStateOf(Aufnahme.AUS); private set
    var aufnahmeStart by mutableLongStateOf(0L); private set
    var mikrofonAnfragen: () -> Unit = {}
    var hinweiseAnfragen: () -> Unit = {}
    /** Was nach dem Transkribieren passieren soll (Auswerten bzw. Beitrag in die Diskussion senden). */
    private var nachAufnahme: (() -> Unit)? = null
    private var aufnahmeJob: Job? = null

    // ---- KI-Verbindung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set
    var kiFehler by mutableStateOf<String?>(null); private set
    var kiVerbindet by mutableStateOf(false); private set

    // ---- Aktualisierung ----
    /** Sicherheitsabfrage vor dem großen Lauf ist offen. */
    var aktualisierenFrage by mutableStateOf(false); private set
    /** Wird erhöht, wenn der Prompt von außen ersetzt wurde (Import, Standard) – der Editor lädt dann neu. */
    var promptStand by mutableIntStateOf(0); private set

    init {
        KiArbeit.protokollLaden(app.filesDir)
        viewModelScope.launch {
            repo.startinhalteAnlegen()
            if (!einstellungen.verboteUmgestellt) {
                repo.verboteUmstellen()
                einstellungen.verboteUmgestellt = true
            }
        }
    }

    fun pruefen() { kiVerbunden = auth.isConnected }

    // ================= Navigation =================

    fun zeige(b: Bildschirm) {
        if (b == bildschirm) return
        if (bildschirm != Bildschirm.Neu) verlauf.addLast(bildschirm)
        bildschirm = b
    }

    fun zurueck(): Boolean {
        if (bildschirm == Bildschirm.Liste) return false
        if (bildschirm == Bildschirm.Neu || bildschirm == Bildschirm.Protokoll) abbrechenAufnahme()
        if (bildschirm == Bildschirm.Neu) { eText = ""; textVorKorrektur = null; korrekturFassungen = emptyList() }
        bildschirm = verlauf.removeLastOrNull()?.takeIf { it != bildschirm } ?: Bildschirm.Liste
        return true
    }

    fun oeffne(id: Long) = zeige(Bildschirm.Detail(id))

    fun melde(text: String, undo: (() -> Unit)? = null) {
        meldung = text
        rueckgaengig = undo
    }

    fun meldungWeg() { meldung = null; rueckgaengig = null }

    // ================= Neue Idee =================

    fun neueIdee() {
        eText = ""; textVorKorrektur = null; korrekturFassungen = emptyList()
        zeige(Bildschirm.Neu)
        if (einstellungen.autoMikro && einstellungen.groqKey.isNotBlank()) mikroTippen()
    }

    fun textGeaendert(neu: String) {
        eText = neu
        textVorKorrektur = null
        korrekturFassungen = emptyList()
    }

    fun korrigieren() {
        if (korrigiert) return
        val quelle = textVorKorrektur ?: eText
        if (quelle.isBlank()) { melde("Sprich oder schreibe zuerst etwas."); return }
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); return }
        korrigiert = true
        viewModelScope.launch {
            try {
                val besser = ki.korrigiere(quelle, korrekturFassungen)
                if (besser.isBlank()) { melde("Die KI hat keine Fassung geliefert."); return@launch }
                textVorKorrektur = quelle
                korrekturFassungen = korrekturFassungen + besser
                eText = besser
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Der Text konnte nicht verbessert werden.")
            } finally {
                korrigiert = false
            }
        }
    }

    fun korrekturZuruecknehmen() {
        val original = textVorKorrektur ?: return
        eText = original
        textVorKorrektur = null
    }

    /** Wertet die Idee mit der KI aus und ordnet sie in die Liste ein. Ohne KI wird sie unten angehängt. */
    fun auswerten() {
        if (erstNachAufnahme(::auswerten)) return
        val idee = eText.trim()
        if (idee.isBlank()) { melde("Sprich oder schreibe zuerst deine Idee."); return }
        if (!kiVerbunden) {
            viewModelScope.launch {
                val titel = idee.split(Regex("\\s+")).take(8).joinToString(" ").trimEnd('.', ',')
                val id = repo.einfuegen(
                    Faktor(titel = titel, kurz = idee, kategorie = Kategorie.SINN.name, evidenz = Evidenz.LOGISCH.name, eigen = true, notiz = idee, wirkung = 10, neu = true),
                    Int.MAX_VALUE,
                )
                hervorgehoben = id
                melde("Ohne KI unten angehängt – verbinde ChatGPT für die Einordnung.")
            }
            bildschirm = Bildschirm.Liste; verlauf.clear()
            return
        }
        if (KiArbeit.laeuft) { melde("Die KI arbeitet gerade noch – bitte kurz warten."); return }
        hinweiseAnfragen()
        val gestartet = KiArbeit.starte(getApplication(), Art.AUSWERTEN, idee.take(80)) { fortschritt ->
            val liste = repo.liste()
            val a = ki.auswerten(idee, liste, fortschritt)
            val dup = a.duplikatVon?.let { repo.einer(it) }
            if (dup != null) {
                val ergaenzt = dup.copy(
                    erklaerung = dup.erklaerung.trimEnd() + "\n\nDeine Idee: " + a.faktor.erklaerung,
                    notiz = listOf(dup.notiz, idee).filter { it.isNotBlank() }.joinToString("\n"),
                    neu = true,
                )
                repo.speichere(ergaenzt)
                KiArbeit.bezugId = dup.id
                bewertungen[dup.id] = a.bewertung
                KiArbeit.ergebnis = "Schon vorhanden: Platz ${dup.platz(repo.liste())} „${dup.titel}“ – deine Idee wurde ergänzt."
                hervorgehoben = dup.id
            } else {
                val id = repo.einfuegen(a.faktor, a.rang)
                KiArbeit.bezugId = id
                bewertungen[id] = a.bewertung
                val platz = repo.einer(id)?.platz(repo.liste()) ?: "${a.rang}"
                KiArbeit.ergebnis = if (a.faktor.raeuber) "Als Lebenszeit-Räuber eingeordnet auf Platz $platz: „${a.faktor.titel}“"
                else "Eingeordnet auf Platz $platz: „${a.faktor.titel}“"
                hervorgehoben = id
            }
        }
        if (gestartet) { bildschirm = Bildschirm.Liste; verlauf.clear(); eText = "" }
    }

    // ================= KI-Arbeiten in der Liste =================

    /** Knopf „Aktualisieren“: läuft schon etwas, zur Diskussion; sonst erst nachfragen – der Lauf ist groß. */
    fun aktualisierenAnfragen() {
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); zeige(Bildschirm.Einstellungen); return }
        if (KiArbeit.laeuft) { zeige(Bildschirm.Protokoll); return }
        aktualisierenFrage = true
    }

    fun aktualisierenAbbrechen() { aktualisierenFrage = false }

    fun aktualisieren() {
        aktualisierenFrage = false
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); zeige(Bildschirm.Einstellungen); return }
        if (KiArbeit.laeuft) { zeige(Bildschirm.Protokoll); return }
        hinweiseAnfragen()
        KiArbeit.starte(getApplication(), Art.AKTUALISIEREN, "Rangfolge prüfen") { fortschritt ->
            val a = ki.aktualisieren(repo.liste(), fortschritt)
            repo.uebernimm(a.liste, a.vorschlaege)
            einstellungen.letzteAktualisierung = System.currentTimeMillis()
            KiArbeit.ergebnis = buildString {
                append(if (a.veraendert == 0) "Die Reihenfolge ist aktuell." else "${a.veraendert} Faktoren haben den Platz gewechselt.")
                if (a.vorschlaege.isNotEmpty()) append(" ${a.vorschlaege.size} neue Vorschläge.")
                if (a.zusammenfassung.isNotBlank()) append("\n\n").append(a.zusammenfassung)
            }
        }
    }

    /** Der eigene Beitrag in der Diskussion: läuft durch alle Agenten, die Gutachterin bildet den Konsens. */
    fun einwandSenden() {
        if (erstNachAufnahme(::einwandSenden)) return
        val text = eText.trim()
        if (text.isBlank()) { melde("Sprich oder schreibe zuerst deinen Beitrag."); return }
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); return }
        if (KiArbeit.laeuft) { melde("Die KI arbeitet gerade noch – bitte kurz warten."); return }
        hinweiseAnfragen()
        val bisher = KiArbeit.protokoll.toList()
        val gestartet = KiArbeit.starte(getApplication(), Art.EINWAND, text.take(80)) { fortschritt ->
            val a = ki.einwand(repo.liste(), text, bisher, fortschritt)
            repo.uebernimm(a.liste, a.vorschlaege, alteVorschlaegeBehalten = true)
            KiArbeit.ergebnis = buildString {
                append(a.einordnung.ifBlank { a.zusammenfassung })
                append("\n\n")
                append(if (a.veraendert == 0) "Die Reihenfolge bleibt gleich." else "${a.veraendert} Faktoren haben den Platz gewechselt.")
                if (a.vorschlaege.isNotEmpty()) append(" ${a.vorschlaege.size} neue Vorschläge.")
            }.trim()
        }
        if (gestartet) { eText = ""; textVorKorrektur = null; korrekturFassungen = emptyList() }
    }

    /** Läuft noch eine Aufnahme, erst transkribieren und danach [weiter] ausführen. */
    private fun erstNachAufnahme(weiter: () -> Unit): Boolean = when (aufnahme) {
        Aufnahme.LAEUFT -> { nachAufnahme = weiter; aufnahmeBeenden(); true }
        Aufnahme.VERARBEITET -> { nachAufnahme = weiter; true }
        Aufnahme.AUS -> false
    }

    // ================= Aktualisierungs-Prompt =================

    fun standardPrompt(): String =
        getApplication<Application>().assets.open("aktualisierung.md").bufferedReader().use { it.readText() }

    /** Der Prompt, mit dem der nächste Lauf arbeitet. */
    fun wirksamerPrompt(): String = einstellungen.aktualisierungsPrompt.ifBlank { standardPrompt() }

    val eigenerPrompt: Boolean get() = einstellungen.aktualisierungsPrompt.isNotBlank()

    fun promptSpeichern(text: String) {
        einstellungen.aktualisierungsPrompt = if (text.trim() == standardPrompt().trim()) "" else text
        melde("Aktualisierungs-Prompt gespeichert")
    }

    fun promptStandard() {
        val alt = einstellungen.aktualisierungsPrompt
        einstellungen.aktualisierungsPrompt = ""
        promptStand++
        melde("Standard-Prompt wiederhergestellt") { einstellungen.aktualisierungsPrompt = alt; promptStand++ }
    }

    fun promptExportieren(uri: android.net.Uri, text: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ok = runCatching {
                // "wt": Drive & Co. kürzen sonst nicht – ein kürzerer Prompt behielte das Ende des alten.
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }.isSuccess
            melde(if (ok) "Prompt exportiert" else "Export fehlgeschlagen")
        }
    }

    fun promptImportieren(uri: android.net.Uri) {
        viewModelScope.launch {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { getApplication<Application>().contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }.getOrNull()
            }
            when {
                text == null -> melde("Die Datei konnte nicht gelesen werden.")
                !text.lineSequence().any { it.startsWith("## ") } -> melde("Das ist kein Aktualisierungs-Prompt – es fehlen die „## “-Abschnitte.")
                else -> {
                    val alt = einstellungen.aktualisierungsPrompt
                    einstellungen.aktualisierungsPrompt = text.removePrefix("\uFEFF")
                    promptStand++
                    melde("Prompt importiert und gespeichert") { einstellungen.aktualisierungsPrompt = alt; promptStand++ }
                }
            }
        }
    }

    fun vertiefen(f: Faktor) {
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); return }
        if (KiArbeit.laeuft) { melde("Die KI arbeitet gerade noch – bitte kurz warten."); return }
        hinweiseAnfragen()
        KiArbeit.starte(getApplication(), Art.VERTIEFEN, f.titel, f.id) { fortschritt ->
            val aktuell = repo.einer(f.id) ?: return@starte
            repo.speichere(ki.vertiefen(aktuell, repo.liste(), fortschritt))
            KiArbeit.ergebnis = "„${aktuell.titel}“ ist jetzt vertieft."
        }
    }

    fun vorschlagAnnehmen(f: Faktor) {
        viewModelScope.launch { repo.vorschlagAnnehmen(f); hervorgehoben = f.id; melde("„${f.titel}“ aufgenommen") }
    }

    fun vorschlagVerwerfen(f: Faktor) {
        viewModelScope.launch { repo.loesche(f); melde("Vorschlag verworfen") { viewModelScope.launch { repo.wiederherstellen(f) } } }
    }

    fun loeschen(f: Faktor) {
        viewModelScope.launch {
            repo.loesche(f)
            if (bildschirm == Bildschirm.Detail(f.id)) zurueck()
            melde("„${f.titel}“ gelöscht") { viewModelScope.launch { repo.wiederherstellen(f) } }
        }
    }

    fun verschiebe(f: Faktor, neuerRang: Int) {
        viewModelScope.launch { repo.verschiebe(f, neuerRang) }
    }

    fun punktUmschalten(f: Faktor, index: Int) {
        viewModelScope.launch {
            val aktuell = repo.einer(f.id) ?: return@launch
            val p = aktuell.punkte.toMutableList()
            if (index !in p.indices) return@launch
            p[index] = p[index].copy(erledigt = !p[index].erledigt)
            repo.speichere(aktuell.copy(punkteJson = Faktor.punkteAlsJson(p)))
            if (p[index].erledigt) konfetti++
        }
    }

    fun zielUmschalten(f: Faktor) {
        viewModelScope.launch {
            val aktuell = repo.einer(f.id) ?: return@launch
            val erreicht = !aktuell.zielErreicht
            repo.speichere(aktuell.copy(zielErreicht = erreicht))
            if (erreicht) {
                konfetti++
                melde(if (aktuell.raeuber) "Abgestellt – „${aktuell.titel}“ ist abgehakt" else "Ziel erreicht – „${aktuell.titel}“ ist abgehakt") { viewModelScope.launch { repo.einer(f.id)?.let { repo.speichere(it.copy(zielErreicht = false)) } } }
            }
        }
    }

    /** Die drei wirksamsten offenen nächsten Schritte: je Faktor der erste offene Punkt, nach Jahren (Plus wie Minus). */
    fun heute(liste: List<Faktor>): List<Schritt> = liste.asSequence()
        .filter { !it.vorschlag && !it.zielErreicht }
        .sortedByDescending { kotlin.math.abs(it.jahre) }
        .mapNotNull { f -> f.punkte.withIndex().firstOrNull { !it.value.erledigt }?.let { Schritt(f, it.index, it.value) } }
        .take(3).toList()

    // ================= Aufnahme =================

    fun mikroTippen() {
        when (aufnahme) {
            Aufnahme.AUS -> {
                if (einstellungen.groqKey.isBlank()) { melde("Bitte zuerst den Groq-Schlüssel in den Einstellungen eintragen."); return }
                mikrofonAnfragen()
            }
            Aufnahme.LAEUFT -> aufnahmeBeenden()
            Aufnahme.VERARBEITET -> Unit
        }
    }

    fun mikrofonErlaubnisErgebnis(ok: Boolean) {
        if (!ok) { melde("Ohne Mikrofon-Erlaubnis geht die Spracheingabe nicht."); return }
        if ((bildschirm != Bildschirm.Neu && bildschirm != Bildschirm.Protokoll) || aufnahme != Aufnahme.AUS) return
        if (mikro.start(viewModelScope)) {
            aufnahme = Aufnahme.LAEUFT
            aufnahmeStart = System.currentTimeMillis()
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun abbrechenAufnahme() {
        if (aufnahme == Aufnahme.AUS) return
        nachAufnahme = null
        mikro.release()
        aufnahmeJob?.cancel()
        aufnahme = Aufnahme.AUS
    }

    private fun aufnahmeBeenden() {
        aufnahme = Aufnahme.VERARBEITET
        aufnahmeJob = viewModelScope.launch {
            val transcriber = GroqTranscriber(
                apiKey = einstellungen.groqKey,
                filterStille = einstellungen.filterStille,
                filterMetriken = einstellungen.filterMetriken,
                filterZeitstempel = einstellungen.filterZeitstempel,
                filterFloskeln = einstellungen.filterFloskeln,
            )
            try {
                val wav = mikro.stop()
                if (wav == null) { melde("Es wurde nichts aufgenommen."); nachAufnahme = null; return@launch }
                val text = Diktat(transcriber).transkribiere(wav).text.trim()
                if (text.isBlank()) {
                    melde("Keine Sprache erkannt.")
                    nachAufnahme = null
                } else {
                    eText = if (eText.isBlank()) text else eText.trimEnd() + " " + text
                    textVorKorrektur = null
                    korrekturFassungen = emptyList()
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Die Spracherkennung ist fehlgeschlagen.")
                nachAufnahme = null
            } finally {
                transcriber.shutdown()
                aufnahme = Aufnahme.AUS
                nachAufnahme?.let { weiter -> nachAufnahme = null; weiter() }
            }
        }
    }

    // ================= KI-Verbindung =================

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

    override fun onCleared() {
        mikro.release()
        super.onCleared()
    }
}
