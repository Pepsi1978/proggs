package de.frank.codexkompass.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.codexkompass.KompassContainer
import de.frank.codexkompass.ai.Prompts
import de.frank.codexkompass.data.local.EintragEntity
import de.frank.codexkompass.data.local.FrageEntity
import de.frank.codexkompass.data.local.SuchTreffer
import de.frank.codexkompass.data.model.Bereich
import de.frank.codexkompass.observability.KompassLog
import de.frank.codexkompass.observability.probe
import de.frank.codexkompass.tts.VorleseZustand
import de.frank.codexkompass.update.LaufFortschritt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ein Eintrag mit allem, was die Liste für ihn braucht. */
data class ListenEintrag(
    val eintrag: EintragEntity,
    val fragen: List<FrageEntity>,
    val kannZurueck: Boolean,
) {
    val istNeu: Boolean get() = eintrag.neuImLauf > 0L
}

/** Der Zustand eines Nachschlage-Bereichs. */
data class ReferenzZustand(
    val laedt: Boolean = true,
    val aktive: List<ListenEintrag> = emptyList(),
    val entfernte: List<ListenEintrag> = emptyList(),
    val ausgeklappt: Set<String> = emptySet(),
    val fragenOffen: Set<String> = emptySet(),
    val arbeitetAn: Set<String> = emptySet(),
    val meldung: String = "",
    val fehler: String = "",
)

/**
 * Steuert die drei Nachschlage-Bereiche, die Suche und den Aktualisieren-Knopf.
 *
 * Alle drei Bereiche verhalten sich für den Benutzer gleich — deshalb hat auch nur ein
 * Modell die Verantwortung. Getrennte Modelle hätten denselben Ablauf dreimal enthalten und
 * damit drei Stellen, an denen er auseinanderlaufen kann.
 */
@OptIn(FlowPreview::class)
class ReferenzViewModel(private val container: KompassContainer) : ViewModel() {

    private val repository = container.repository

    private val ausgeklappt = MutableStateFlow<Set<String>>(emptySet())
    private val fragenOffen = MutableStateFlow<Set<String>>(emptySet())
    private val arbeitetAn = MutableStateFlow<Set<String>>(emptySet())
    private val meldungen = MutableStateFlow("")
    private val fehler = MutableStateFlow("")

    private val fragenAlle = repository.beobachteAlleFragen()
    private val historieAnzahlen = repository.beobachteHistorieAnzahlen()

    val vorleseZustand: StateFlow<VorleseZustand> = container.vorlesen.zustand

    // --- Aktualisieren ----------------------------------------------------------------------

    private val _lauf = MutableStateFlow(LaufFortschritt())
    val lauf: StateFlow<LaufFortschritt> = _lauf.asStateFlow()
    private var laufJob: Job? = null

    val letzterErfolg = repository.beobachteLetztenErfolg()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --- Suche -----------------------------------------------------------------------------

    private val _suchtext = MutableStateFlow("")
    val suchtext: StateFlow<String> = _suchtext.asStateFlow()

    private val _treffer = MutableStateFlow<List<SuchTreffer>>(emptyList())
    val treffer: StateFlow<List<SuchTreffer>> = _treffer.asStateFlow()

    private val _suchtGerade = MutableStateFlow(false)
    val suchtGerade: StateFlow<Boolean> = _suchtGerade.asStateFlow()

    val suchVerlauf = repository.beobachteSuchVerlauf()
        .map { liste -> liste.map { it.anfrage } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Während des Tippens suchen, mit kurzer Beruhigung. Ohne die Entprellung liefe bei
        // jedem Tastendruck eine Abfrage — bei schneller Eingabe ruckelt die Liste dann.
        _suchtext
            .debounce(250)
            .onEach { text ->
                if (text.isBlank()) {
                    _treffer.value = emptyList()
                    _suchtGerade.value = false
                    return@onEach
                }
                _suchtGerade.value = true
                _treffer.value = repository.suche(text)
                _suchtGerade.value = false
            }
            .launchIn(viewModelScope)
    }

    fun setzeSuchtext(text: String) {
        _suchtext.value = text
    }

    fun merkeSuche() {
        val text = _suchtext.value
        viewModelScope.launch { repository.merkeSuchAnfrage(text) }
    }

    fun leereSuche() {
        _suchtext.value = ""
        _treffer.value = emptyList()
    }

    fun leereSuchVerlauf() = viewModelScope.launch { repository.leereSuchVerlauf() }

    fun loescheSuchAnfrage(anfrage: String) =
        viewModelScope.launch { repository.loescheSuchAnfrage(anfrage) }

    // --- Bereiche ---------------------------------------------------------------------------

    /**
     * Je Bereich genau EIN Fluss, einmal gebaut und danach wiederverwendet.
     *
     * Ohne diesen Zwischenspeicher entstünde bei jeder Neuzeichnung ein neuer `stateIn`-Fluss.
     * Der beginnt bei seinem Anfangswert — und der heisst hier `laedt = true`. Die Liste bliebe
     * damit dauerhaft im Ladezustand stehen, obwohl die Daten längst da sind (Almanach
     * jetpack-compose §2.14: roher Fluss pro Neuzeichnung neu erzeugt).
     *
     * Die Regel dahinter gilt allgemein: Eine Funktion eines Ansichtsmodells, die einen Fluss
     * ZURUECKGIBT, darf ihn nicht bei jedem Aufruf neu bauen.
     */
    private val zustaende = mutableMapOf<Bereich, StateFlow<ReferenzZustand>>()

    fun zustandFuer(bereich: Bereich): StateFlow<ReferenzZustand> =
        synchronized(zustaende) {
            zustaende.getOrPut(bereich) { baueZustand(bereich) }
        }

    /** Zählt, wie oft je Bereich gebaut wurde — Grundlage der Sonde in [baueZustand]. */
    private val bauZaehler = mutableMapOf<Bereich, Int>()

    private fun baueZustand(bereich: Bereich): StateFlow<ReferenzZustand> {
        // Zweite Schicht neben dem Zwischenspeicher: Sollte jemand den Speicher später
        // entfernen, fällt das hier sofort auf, statt sich als „Liste laedt ewig" zu zeigen —
        // ein Fehlerbild, dem man die Ursache nicht ansieht.
        val anzahl = (bauZaehler[bereich] ?: 0) + 1
        bauZaehler[bereich] = anzahl
        probe(
            anzahl == 1,
            "Der Zustandsfluss wurde fuer denselben Bereich mehrfach gebaut — die Liste bliebe " +
                "im Ladezustand haengen",
            "ReferenzViewModel",
            "baueZustand",
            mapOf("bereich" to bereich.id, "anzahl" to anzahl),
        )
        return combine(
            repository.beobachteAktive(bereich),
            repository.beobachteEntfernte(bereich),
            fragenAlle,
            historieAnzahlen,
            combine(ausgeklappt, fragenOffen, arbeitetAn, meldungen, fehler) { a, f, w, m, e ->
                Bedienzustand(a, f, w, m, e)
            },
        ) { aktive, entfernte, fragen, anzahlen, bedien ->
            val nachEintrag = fragen.groupBy { it.eintragId }
            ReferenzZustand(
                laedt = false,
                aktive = aktive.map {
                    ListenEintrag(it, nachEintrag[it.id].orEmpty(), (anzahlen[it.id] ?: 0) > 0)
                },
                entfernte = entfernte.map {
                    ListenEintrag(it, nachEintrag[it.id].orEmpty(), (anzahlen[it.id] ?: 0) > 0)
                },
                ausgeklappt = bedien.ausgeklappt,
                fragenOffen = bedien.fragenOffen,
                arbeitetAn = bedien.arbeitetAn,
                meldung = bedien.meldung,
                fehler = bedien.fehler,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferenzZustand())
    }

    private data class Bedienzustand(
        val ausgeklappt: Set<String>,
        val fragenOffen: Set<String>,
        val arbeitetAn: Set<String>,
        val meldung: String,
        val fehler: String,
    )

    fun schalteAusgeklappt(id: String) {
        ausgeklappt.value = ausgeklappt.value.let { if (id in it) it - id else it + id }
    }

    fun schalteFragenListe(id: String) {
        fragenOffen.value = fragenOffen.value.let { if (id in it) it - id else it + id }
    }

    fun loescheMeldung() {
        meldungen.value = ""
    }

    fun loescheFehler() {
        fehler.value = ""
        container.vorlesen.loescheFehler()
    }

    // --- Vorlesen ---------------------------------------------------------------------------

    fun liesVor(eintrag: EintragEntity) {
        val text = buildString {
            append(eintrag.name).append(". ")
            append(eintrag.kurz).append("\n\n")
            append(eintrag.erklaerung)
            if (eintrag.entfernt && eintrag.ersatz.isNotBlank()) {
                append("\n\nWas an seine Stelle getreten ist: ").append(eintrag.ersatz)
            }
        }
        container.vorlesen.schalteUm(eintrag.id, text)
    }

    fun liesAntwortVor(frage: FrageEntity) {
        container.vorlesen.schalteUm(
            "frage:${frage.id}",
            "Frage: ${frage.frage}\n\nAntwort: ${frage.antwort}",
        )
    }

    fun stoppeVorlesen() = container.vorlesen.stoppe()

    // --- Ausführlicher erklären und zurück ---------------------------------------------------

    fun vertiefe(eintrag: EintragEntity) {
        if (eintrag.id in arbeitetAn.value) return
        if (!container.codex.istVerbunden) {
            fehler.value = "Für eine ausführlichere Erklärung braucht es die Anmeldung bei " +
                "Codex. Du findest sie in den Einstellungen."
            return
        }
        arbeitetAn.value = arbeitetAn.value + eintrag.id
        // Ein Eintrag ohne deutsche Erklärung braucht zuerst eine Grundfassung, keine
        // Vertiefung. Ihn durch die Vertiefen-Anweisung zu schicken hiesse, das Modell zu
        // bitten, einen leeren Text ausführlicher zu machen — und die Stufe stünde danach auf
        // eins, obwohl es die erste Fassung ist.
        val brauchtGrundfassung = eintrag.erklaerung.isBlank()
        viewModelScope.launch {
            try {
                val antwort = container.codex.frage(
                    anweisung = if (brauchtGrundfassung) {
                        Prompts.neuerEintragAnweisung(eintrag.bereich)
                    } else {
                        Prompts.vertiefeAnweisung(eintrag.stufe)
                    },
                    eingabe = if (brauchtGrundfassung) {
                        Prompts.neuerEintragEingabe(
                            eintrag.name,
                            eintrag.quelleEnglisch,
                            eintrag.seitVersion,
                        )
                    } else {
                        Prompts.vertiefeEingabe(eintrag)
                    },
                    modellId = container.einstellungen.modellId,
                    denktiefe = container.einstellungen.denktiefe.apiValue,
                )
                val geputzt = antwort.trim()
                if (geputzt.isBlank()) {
                    fehler.value = "Es kam keine ausführlichere Fassung zurück. Versuch es noch einmal."
                    return@launch
                }
                if (brauchtGrundfassung) {
                    val json = Prompts.leseJsonObjekt(geputzt)
                    val erklaerung = json?.optString("erklaerung")?.takeIf(String::isNotBlank) ?: geputzt
                    repository.sichereEintrag(
                        eintrag.copy(
                            kurz = json?.optString("kurz")?.takeIf(String::isNotBlank) ?: eintrag.kurz,
                            kategorie = json?.optString("kategorie")?.takeIf(String::isNotBlank)
                                ?: eintrag.kategorie,
                            erklaerung = erklaerung,
                            zuletztGeaendert = System.currentTimeMillis(),
                        ),
                    )
                    meldungen.value = "Die deutsche Erklärung steht jetzt da."
                    return@launch
                }
                repository.vertiefeErklaerung(eintrag.id, geputzt)
                meldungen.value = "Ausführlichere Fassung eingesetzt (Stufe ${eintrag.stufe + 1})."
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (problem: Exception) {
                fehler.value = problem.message ?: "Die ausführlichere Fassung ist fehlgeschlagen."
                KompassLog.error(
                    "ReferenzViewModel",
                    "vertiefe",
                    "Vertiefen fehlgeschlagen",
                    mapOf("id" to eintrag.id, "grund" to problem.message),
                )
            } finally {
                arbeitetAn.value = arbeitetAn.value - eintrag.id
            }
        }
    }

    fun machRueckgaengig(eintrag: EintragEntity) {
        viewModelScope.launch {
            val ging = repository.machErklaerungRueckgaengig(eintrag.id)
            meldungen.value = if (ging) {
                "Vorherige Fassung wiederhergestellt."
            } else {
                "Es gibt keine frühere Fassung mehr."
            }
        }
    }

    // --- Fragen -----------------------------------------------------------------------------

    fun stelleFrage(eintrag: EintragEntity, frage: String) {
        val text = frage.trim()
        if (text.isBlank()) return
        if (!container.codex.istVerbunden) {
            fehler.value = "Zum Fragen braucht es die Anmeldung bei Codex. Du findest sie in " +
                "den Einstellungen."
            return
        }
        fragenOffen.value = fragenOffen.value + eintrag.id
        viewModelScope.launch {
            val frageId = repository.starteFrage(eintrag.id, text)
            try {
                val gesammelt = StringBuilder()
                val antwort = container.codex.frage(
                    anweisung = Prompts.frageAnweisung(),
                    eingabe = Prompts.frageEingabe(eintrag, text),
                    modellId = container.einstellungen.modellId,
                    denktiefe = container.einstellungen.denktiefe.apiValue,
                ) { stueck ->
                    gesammelt.append(stueck)
                    // Mitschreiben, während die Antwort eintrifft — sonst steht die Frage
                    // minutenlang ohne sichtbare Regung da.
                    repository.aktualisiereFrageText(frageId, gesammelt.toString())
                }
                repository.beendeFrage(frageId, antwort.trim())
            } catch (abbruch: CancellationException) {
                repository.beendeFrage(frageId, "", "Die Anfrage wurde abgebrochen.")
                throw abbruch
            } catch (problem: Exception) {
                val meldung = problem.message ?: "Die Antwort ist fehlgeschlagen."
                repository.beendeFrage(frageId, "", meldung)
                fehler.value = meldung
            }
        }
    }

    fun loescheFrage(id: Long) = viewModelScope.launch { repository.loescheFrage(id) }

    // --- Aktualisieren ----------------------------------------------------------------------

    fun aktualisiere(erklaereAlles: Boolean = false) {
        if (_lauf.value.laeuft) return
        laufJob = viewModelScope.launch {
            container.aktualisierer.fuehreAus(erklaereAlles) { fortschritt -> _lauf.value = fortschritt }
        }
    }

    /**
     * Holt die offenen Erklärungen nach, ohne die Unterlagen erneut zu vergleichen.
     *
     * Der Weg dorthin ist der Knopf im Bericht: Kommen aus einer neuen Fassung sehr viele
     * Einträge auf einmal, wird nicht ungefragt für jeden eine Anfrage an das Modell gestellt.
     */
    fun holeOffeneErklaerungen() {
        if (_lauf.value.laeuft) return
        laufJob = viewModelScope.launch {
            container.aktualisierer.erklaereOffene { fortschritt -> _lauf.value = fortschritt }
        }
    }

    fun brichAktualisierungAb() {
        laufJob?.cancel()
        laufJob = null
        _lauf.value = LaufFortschritt(schritt = "Abgebrochen.")
    }

    fun loescheLaufMeldung() {
        _lauf.value = LaufFortschritt()
    }

    override fun onCleared() {
        container.vorlesen.stoppe()
        container.codex.brichAnfragenAb()
        super.onCleared()
    }
}
