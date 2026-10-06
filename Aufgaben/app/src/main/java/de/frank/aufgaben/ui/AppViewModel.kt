package de.frank.aufgaben.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.aufgaben.audio.Diktat
import de.frank.aufgaben.audio.GroqTranscriber
import de.frank.aufgaben.audio.MicRecorder
import de.frank.aufgaben.auth.CodexAuthManager
import de.frank.aufgaben.auth.DeviceAuthInfo
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.Erkennung
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Schritt
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.Wiederholung
import de.frank.aufgaben.data.kurzerTitel
import de.frank.aufgaben.erinnerung.Toene
import de.frank.aufgaben.ki.AufgabenKi
import de.frank.aufgaben.ki.notTitel
import de.frank.aufgaben.tts.TtsManager
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Bildschirm {
    data object Liste : Bildschirm
    data class Bearbeiten(val id: Long?) : Bildschirm
    data object Einstellungen : Bildschirm
    data class Fokus(val id: Long) : Bildschirm
}

enum class Aufnahme { AUS, LAEUFT, VERARBEITET }

/** Wohin eine gezogene Aufgabe fällt. */
sealed interface Ziel {
    /** Auf einen Tag; [minuten] = null entfernt die Uhrzeit, [behalteZeit] lässt sie unverändert. */
    data class Tag(val tag: Long, val minuten: Int? = null, val behalteZeit: Boolean = false) : Ziel
    data class Prio(val prioritaet: Prioritaet) : Ziel
    data object Erledigt : Ziel
    data object Loeschen : Ziel
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AufgabenRepository.get(app)
    val einstellungen = Einstellungen.get(app)
    val auth = CodexAuthManager(app)
    private val ki = AufgabenKi(auth, einstellungen)
    val tts = TtsManager(app)
    private val mikro = MicRecorder(app)

    val aufgaben: StateFlow<List<Aufgabe>> = repo.alle.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var heute by mutableLongStateOf(Tage.heute()); private set
    var bildschirm by mutableStateOf<Bildschirm>(Bildschirm.Liste); private set
    var meldung by mutableStateOf<String?>(null); private set
    var rueckgaengig by mutableStateOf<(() -> Unit)?>(null); private set
    var konfetti by mutableIntStateOf(0); private set
    var hervorgehoben by mutableStateOf<Long?>(null)

    // ---- Editor ----
    var eId by mutableStateOf<Long?>(null)
    var eTitel by mutableStateOf("")
    var eText by mutableStateOf("")
    var ePrio by mutableStateOf(Prioritaet.SPAETER)
    var eTag by mutableStateOf<Long?>(null)
    var eMinuten by mutableStateOf<Int?>(null)
    var eDauer by mutableIntStateOf(30)
    var eErinnerung by mutableStateOf(false)
    var eVorlauf by mutableIntStateOf(10)
    var eVorlesen by mutableStateOf(true)
    var eAlsWecker by mutableStateOf(false)
    var eWdh by mutableStateOf(Wiederholung.KEINE)
    val eSchritte = mutableStateListOf<Schritt>()
    var eVorschlag by mutableStateOf<Erkennung.Vorschlag?>(null)
    var textVorKorrektur by mutableStateOf<String?>(null); private set
    private var korrekturFassungen = emptyList<String>()
    var korrigiert by mutableStateOf(false); private set
    private var editorQuelle: Aufgabe? = null

    // ---- Aufnahme ----
    var aufnahme by mutableStateOf(Aufnahme.AUS); private set
    var aufnahmeStart by mutableLongStateOf(0L); private set
    var mikrofonAnfragen: () -> Unit = {}
    var hinweiseAnfragen: () -> Unit = {}
    private var nachAufnahmeSpeichern = false
    private var aufnahmeJob: Job? = null

    // ---- KI-Verbindung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set
    var kiFehler by mutableStateOf<String?>(null); private set
    var kiVerbindet by mutableStateOf(false); private set

    // ---- Vorlesen ----
    var sprichtId by mutableStateOf<String?>(null); private set

    // ---- Fokus ----
    var fokusEnde by mutableLongStateOf(0L); private set
    var fokusRest by mutableLongStateOf(0L); private set
    var fokusLaeuft by mutableStateOf(false); private set
    private var fokusJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                val bisMitternacht = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
                delay(bisMitternacht.coerceIn(1_000, 60 * 60_000L) + 500)
                tagPruefen()
            }
        }
    }

    fun tagPruefen() {
        val jetzt = Tage.heute()
        if (jetzt != heute) heute = jetzt
        kiVerbunden = auth.isConnected
    }

    fun zeige(b: Bildschirm) { bildschirm = b }

    fun zurueck(): Boolean = when (bildschirm) {
        is Bildschirm.Bearbeiten -> { editorVerlassen(); true }
        Bildschirm.Liste -> false
        Bildschirm.Einstellungen -> {
            // Stimme oder Tempo könnten sich geändert haben: fehlende Sprachfassungen nachholen.
            de.frank.aufgaben.erinnerung.Ansage.anstossen(getApplication())
            bildschirm = Bildschirm.Liste
            true
        }
        else -> { bildschirm = Bildschirm.Liste; true }
    }

    fun melde(text: String, undo: (() -> Unit)? = null) {
        meldung = text
        rueckgaengig = undo
    }

    fun meldungWeg() { meldung = null; rueckgaengig = null }

    // ================= Editor =================

    fun neueAufgabe(mitMikro: Boolean = true, tag: Long? = null) {
        editorQuelle = null
        eId = null; eTitel = ""; eText = ""; ePrio = Prioritaet.SPAETER; eTag = tag; eMinuten = null
        eDauer = 30; eErinnerung = false; eVorlauf = einstellungen.vorlaufStandard; eWdh = Wiederholung.KEINE
        eVorlesen = einstellungen.vorlesenStandard; eAlsWecker = false
        eSchritte.clear(); eVorschlag = null; textVorKorrektur = null; korrekturFassungen = emptyList()
        bildschirm = Bildschirm.Bearbeiten(null)
        if (mitMikro && einstellungen.autoMikro && einstellungen.groqKey.isNotBlank()) mikroTippen()
    }

    fun oeffne(id: Long) {
        viewModelScope.launch {
            val a = repo.eine(id) ?: return@launch
            editorQuelle = a
            eId = a.id; eTitel = a.titel; eText = a.text; ePrio = a.prio; eTag = a.tag; eMinuten = a.minuten
            eDauer = a.dauer; eErinnerung = a.erinnerung; eVorlauf = a.vorlauf; eWdh = a.wdh
            eVorlesen = a.vorlesen; eAlsWecker = a.alsWecker
            eSchritte.clear(); eSchritte.addAll(a.schritte)
            eVorschlag = null; textVorKorrektur = null; korrekturFassungen = emptyList()
            bildschirm = Bildschirm.Bearbeiten(id)
        }
    }

    fun textGeaendert(neu: String) {
        eText = neu
        textVorKorrektur = null
        korrekturFassungen = emptyList()
    }

    fun setzeZeit(minuten: Int?) {
        eMinuten = minuten
        if (minuten != null && !eErinnerung && editorQuelle?.erinnerung != true) eErinnerung = true
        if (minuten != null && eTag == null) eTag = heute
        if (minuten != null) hinweiseAnfragen()
    }

    fun setzeTag(tag: Long?) {
        eTag = tag
        if (tag != null && ePrio == Prioritaet.SPAETER) ePrio = Prioritaet.MITTEL
        if (tag == null) eMinuten = null
    }

    fun vorschlagUebernehmen() {
        val v = eVorschlag ?: return
        v.tag?.let { setzeTag(it) }
        v.minuten?.let { setzeZeit(it) }
        v.prioritaet?.let { ePrio = it }
        eVorschlag = null
    }

    fun korrigieren() {
        if (korrigiert) return
        val quelle = textVorKorrektur ?: eText
        if (quelle.isBlank()) { melde("Sprich oder schreibe zuerst etwas."); return }
        if (!kiVerbunden) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); return }
        korrigiert = true
        viewModelScope.launch {
            try {
                val besser = ki.verbessere(quelle, korrekturFassungen)
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

    /** Stellt den gesprochenen Text wieder her; der nächste Tipp auf Korrektur bringt eine neue Fassung. */
    fun korrekturZuruecknehmen() {
        val original = textVorKorrektur ?: return
        eText = original
        textVorKorrektur = null
    }

    private fun editorVerlassen() {
        if (aufnahme != Aufnahme.AUS || eTitel.isNotBlank() || eText.isNotBlank()) speichern()
        else bildschirm = Bildschirm.Liste
    }

    fun verwerfen() {
        mikro.release()
        aufnahmeJob?.cancel()
        aufnahme = Aufnahme.AUS
        bildschirm = Bildschirm.Liste
    }

    fun speichern() {
        when (aufnahme) {
            Aufnahme.LAEUFT -> { nachAufnahmeSpeichern = true; aufnahmeBeenden(); return }
            Aufnahme.VERARBEITET -> { nachAufnahmeSpeichern = true; return }
            Aufnahme.AUS -> Unit
        }
        val titelEingabe = eTitel.trim()
        val text = eText.trim()
        if (titelEingabe.isEmpty() && text.isEmpty() && eSchritte.isEmpty()) {
            bildschirm = Bildschirm.Liste
            return
        }
        val kiTitel = titelEingabe.isEmpty() && text.isNotEmpty()
        val titel = titelEingabe.ifEmpty { if (text.isNotEmpty()) notTitel(text) else kurzerTitel(eSchritte.first().text) }
        val prio = if (eTag != null && ePrio == Prioritaet.SPAETER) Prioritaet.MITTEL else ePrio
        val basis = editorQuelle ?: Aufgabe(titel = titel)
        val neu = basis.copy(
            titel = titel, text = text, prioritaet = prio.name, tag = eTag, minuten = if (eTag == null) null else eMinuten,
            dauer = eDauer, erinnerung = eErinnerung && eMinuten != null, vorlauf = eVorlauf, wiederholung = eWdh.name,
            vorlesen = eVorlesen, alsWecker = eAlsWecker,
            titelVonKi = if (kiTitel) false else basis.titelVonKi && titelEingabe == basis.titel,
            schritteJson = Aufgabe.schritteAlsJson(eSchritte.filter { it.text.isNotBlank() }),
        )
        bildschirm = Bildschirm.Liste
        viewModelScope.launch {
            val id = if (editorQuelle == null) repo.neu(neu) else { repo.speichere(neu); neu.id }
            hervorgehoben = id
            melde(if (editorQuelle == null) "Aufgabe angelegt" else "Gespeichert")
            if (kiTitel && kiVerbunden && einstellungen.kiTitel) titelErzeugen(id, text)
        }
    }

    private fun titelErzeugen(id: Long, text: String) {
        viewModelScope.launch {
            runCatching { ki.titel(text) }.onSuccess { t ->
                if (t.isNotBlank()) repo.eine(id)?.let { repo.speichere(it.copy(titel = kurzerTitel(t), titelVonKi = true)) }
            }
        }
    }

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
        if (bildschirm !is Bildschirm.Bearbeiten || aufnahme != Aufnahme.AUS) return
        tts.stop()
        if (mikro.start(viewModelScope)) {
            aufnahme = Aufnahme.LAEUFT
            aufnahmeStart = System.currentTimeMillis()
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
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
                if (wav == null) { melde("Es wurde nichts aufgenommen."); return@launch }
                val text = Diktat(transcriber).transkribiere(wav).text.trim()
                if (text.isBlank()) {
                    melde("Keine Sprache erkannt.")
                } else {
                    eText = if (eText.isBlank()) text else eText.trimEnd() + " " + text
                    textVorKorrektur = null
                    korrekturFassungen = emptyList()
                    val v = Erkennung.erkenne(eText)
                    eVorschlag = v.takeUnless { it.leer || (it.tag == eTag && it.minuten == eMinuten) }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Die Spracherkennung ist fehlgeschlagen.")
            } finally {
                transcriber.shutdown()
                aufnahme = Aufnahme.AUS
                if (nachAufnahmeSpeichern) { nachAufnahmeSpeichern = false; speichern() }
            }
        }
    }

    // ================= Liste =================

    fun verschiebe(a: Aufgabe, ziel: Ziel) {
        viewModelScope.launch {
            when (ziel) {
                is Ziel.Tag -> {
                    val prio = if (a.prio == Prioritaet.SPAETER) Prioritaet.MITTEL else a.prio
                    val minuten = if (ziel.behalteZeit) a.minuten else ziel.minuten
                    val erinnerung = if (minuten != null && a.minuten == null) true else a.erinnerung
                    val vorlauf = if (minuten != null && a.minuten == null) einstellungen.vorlaufStandard else a.vorlauf
                    val vorlesen = if (minuten != null && a.minuten == null) einstellungen.vorlesenStandard else a.vorlesen
                    repo.speichere(a.copy(tag = ziel.tag, minuten = minuten, prioritaet = prio.name, erinnerung = erinnerung && minuten != null, vorlauf = vorlauf, vorlesen = vorlesen))
                    if (minuten != null) hinweiseAnfragen()
                }
                is Ziel.Prio -> repo.speichere(a.copy(prioritaet = ziel.prioritaet.name, tag = null, minuten = null))
                Ziel.Erledigt -> erledigen(a, true)
                Ziel.Loeschen -> loeschen(a)
            }
        }
    }

    fun erledigen(a: Aufgabe, erledigt: Boolean) {
        viewModelScope.launch {
            val folge = repo.setzeErledigt(a, erledigt)
            if (erledigt) {
                konfetti++
                melde("„${a.titel}“ erledigt" + (folge?.tag?.let { " · nächstes Mal ${Tage.datum(it)}" } ?: "")) {
                    viewModelScope.launch {
                        folge?.let { repo.loesche(it) }
                        repo.eine(a.id)?.let { repo.setzeErledigt(it, false) }
                    }
                }
            }
        }
    }

    fun loeschen(a: Aufgabe) {
        viewModelScope.launch {
            repo.loesche(a)
            melde("Gelöscht") { viewModelScope.launch { repo.wiederherstellen(a) } }
        }
    }

    fun schrittUmschalten(a: Aufgabe, index: Int) {
        viewModelScope.launch {
            val s = a.schritte.toMutableList()
            if (index !in s.indices) return@launch
            s[index] = s[index].copy(erledigt = !s[index].erledigt)
            repo.speichere(a.copy(schritteJson = Aufgabe.schritteAlsJson(s)))
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

    fun tagVorlesen(tag: Long, liste: List<Aufgabe>) {
        val offen = liste.filter { !it.erledigt }
        val name = if (tag == heute) "Heute" else if (tag == heute + 1) "Morgen" else Tage.datum(tag)
        val text = if (offen.isEmpty()) "$name steht nichts an." else buildString {
            append("$name ${if (offen.size == 1) "steht eine Aufgabe" else "stehen ${offen.size} Aufgaben"} an. ")
            offen.filter { it.minuten != null }.sortedBy { it.minuten }.forEach { append("Um ${Tage.zeit(it.minuten!!)} Uhr: ${it.titel}. ") }
            val rest = offen.filter { it.minuten == null }.sortedBy { it.prio.rang }
            if (rest.isNotEmpty()) {
                append(if (rest.size == offen.size) "" else "Außerdem: ")
                rest.forEach { append("${it.titel}. ") }
            }
        }
        vorlesen("tag_$tag", text)
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

    // ================= Fokus =================

    fun fokusStarten(id: Long) {
        fokusRest = einstellungen.fokusMinuten * 60_000L
        bildschirm = Bildschirm.Fokus(id)
        fokusFortsetzen()
    }

    fun fokusFortsetzen() {
        fokusJob?.cancel()
        fokusEnde = System.currentTimeMillis() + fokusRest
        fokusLaeuft = true
        fokusJob = viewModelScope.launch {
            while (true) {
                fokusRest = (fokusEnde - System.currentTimeMillis()).coerceAtLeast(0)
                if (fokusRest == 0L) break
                delay(250)
            }
            fokusLaeuft = false
            Toene.spiele(getApplication(), einstellungen.ton, einstellungen.lautstaerke)
            melde("Fokuszeit vorbei – gut gemacht!")
        }
    }

    fun fokusPause() { fokusJob?.cancel(); fokusLaeuft = false }

    fun fokusBeenden() { fokusJob?.cancel(); fokusLaeuft = false; bildschirm = Bildschirm.Liste }

    fun tonProbe() { Toene.spiele(getApplication(), einstellungen.ton, einstellungen.lautstaerke) }

    /** Übernimmt eine selbst gewählte MP3 (z. B. aus Suno) als Erinnerungston. */
    fun eigenenTonWaehlen(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val (wert, name) = withContext(Dispatchers.IO) { Toene.eigenenUebernehmen(getApplication(), uri) }
                einstellungen.ton = wert
                einstellungen.tonName = name
                melde("Eigener Ton: $name")
                tonProbe()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Der Ton konnte nicht übernommen werden.")
            }
        }
    }

    override fun onCleared() {
        mikro.release()
        tts.shutdown()
        super.onCleared()
    }
}
