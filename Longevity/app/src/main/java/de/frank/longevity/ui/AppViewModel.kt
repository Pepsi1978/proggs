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
}

enum class Aufnahme { AUS, LAEUFT, VERARBEITET }

/** Ein nächster Schritt für den Heute-Kopf. */
data class Schritt(val faktor: Faktor, val index: Int, val punkt: Punkt)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    val einstellungen = Einstellungen.get(app)
    val auth = CodexAuthManager(app)
    private val ki = LongevityKi(auth, einstellungen)
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
    private var nachAufnahmeAuswerten = false
    private var aufnahmeJob: Job? = null

    // ---- KI-Verbindung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set
    var kiFehler by mutableStateOf<String?>(null); private set
    var kiVerbindet by mutableStateOf(false); private set

    init {
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
        if (bildschirm == Bildschirm.Neu) abbrechenAufnahme()
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
        when (aufnahme) {
            Aufnahme.LAEUFT -> { nachAufnahmeAuswerten = true; aufnahmeBeenden(); return }
            Aufnahme.VERARBEITET -> { nachAufnahmeAuswerten = true; return }
            Aufnahme.AUS -> Unit
        }
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

    fun aktualisieren() {
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
        if (bildschirm != Bildschirm.Neu || aufnahme != Aufnahme.AUS) return
        if (mikro.start(viewModelScope)) {
            aufnahme = Aufnahme.LAEUFT
            aufnahmeStart = System.currentTimeMillis()
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun abbrechenAufnahme() {
        if (aufnahme == Aufnahme.AUS) return
        nachAufnahmeAuswerten = false
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
                if (wav == null) { melde("Es wurde nichts aufgenommen."); nachAufnahmeAuswerten = false; return@launch }
                val text = Diktat(transcriber).transkribiere(wav).text.trim()
                if (text.isBlank()) {
                    melde("Keine Sprache erkannt.")
                    nachAufnahmeAuswerten = false
                } else {
                    eText = if (eText.isBlank()) text else eText.trimEnd() + " " + text
                    textVorKorrektur = null
                    korrekturFassungen = emptyList()
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Die Spracherkennung ist fehlgeschlagen.")
                nachAufnahmeAuswerten = false
            } finally {
                transcriber.shutdown()
                aufnahme = Aufnahme.AUS
                if (nachAufnahmeAuswerten) { nachAufnahmeAuswerten = false; auswerten() }
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
