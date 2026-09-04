package de.frank.claudekompass.vm

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.claudekompass.KompassContainer
import de.frank.claudekompass.data.model.Denktiefe
import de.frank.claudekompass.data.model.KiModell
import de.frank.claudekompass.data.model.TtsAnbieter
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.tts.GeklonteStimme
import de.frank.claudekompass.tts.TtsCatalog
import de.frank.claudekompass.ui.theme.ThemeModus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Was bei der Prüfung eines Schlüssels herauskam.
 *
 * Das Ergebnis gehört an das Feld, das geprüft wurde — nicht an den Kopf des Bildschirms.
 * Beim Tippen auf „Schlüssel prüfen" steht der Kopf längst ausserhalb des Sichtfelds; eine
 * Meldung dort wird schlicht nicht gesehen.
 */
data class PruefErgebnis(
    val laeuft: Boolean = false,
    val geglueckt: Boolean = false,
    val text: String = "",
) {
    val hatErgebnis: Boolean get() = text.isNotBlank()
}

/** Zustand des Einstellungs-Bildschirms. */
data class EinstellungenZustand(
    val ttsAnbieter: TtsAnbieter = TtsAnbieter.GOOGLE,
    val googleStimme: String = TtsCatalog.STANDARD_GOOGLE_STIMME,
    val edgeStimme: String = TtsCatalog.STANDARD_EDGE_STIMME,
    val qwenStimme: String = "",
    val tempo: Float = 1f,
    val lieblingsStimmen: Set<String> = emptySet(),
    val googleSchluessel: String = "",
    val alibabaSchluessel: String = "",
    val groqSchluessel: String = "",
    val groqModell: String = "",
    val filterSchichten: List<Boolean> = listOf(true, true, true, true),
    val modellId: String = KiModell.standard.apiId,
    val modellLabel: String = KiModell.standard.label,
    val eigeneModelle: List<String> = emptyList(),
    val denktiefe: Denktiefe = Denktiefe.MEDIUM,
    val themeModus: ThemeModus = ThemeModus.HELL,
    val appSperre: Boolean = false,
    val sperreNach: Int = 60,
    val codexEmail: String? = null,
    val codexVerbunden: Boolean = false,
    val geraeteCode: String = "",
    val meldeAnLaeuft: Boolean = false,
    val eigeneStimmen: List<GeklonteStimme> = emptyList(),
    val stimmenLaden: Boolean = false,
    val aufnahmeLaeuft: Boolean = false,
    val aufnahmeSchritt: String = "",
    /** Prüfergebnis je Schlüssel, abgelegt unter [SCHLUESSEL_GOOGLE] und den anderen. */
    val pruefungen: Map<String, PruefErgebnis> = emptyMap(),
    val meldung: String = "",
    val fehler: String = "",
    val schluesselAblageFehler: String? = null,
)

/**
 * Der Einstellungs-Bildschirm mit den Blöcken Vorlesen, Spracheingabe, KI, Darstellung,
 * Sicherheit und Sicherung (Referenz, Baustein G).
 *
 * Zu jedem Schlüssel gehört ein Prüfknopf, der einen echten kleinen Aufruf macht. Ein Feld,
 * in dem etwas steht, sagt nämlich noch nicht, dass es funktioniert — und der Unterschied
 * fällt sonst erst mitten in der Arbeit auf.
 */
class EinstellungenViewModel(private val container: KompassContainer) : ViewModel() {

    private val store = container.einstellungen

    private val _zustand = MutableStateFlow(lieAlles())
    val zustand: StateFlow<EinstellungenZustand> = _zustand.asStateFlow()

    val vorleseZustand = container.vorlesen.zustand
    val themeModus = store.themeModus

    private var anmeldeJob: Job? = null
    private var aufnahmeJob: Job? = null

    private fun lieAlles() = EinstellungenZustand(
        ttsAnbieter = store.ttsAnbieter,
        googleStimme = store.googleStimme,
        edgeStimme = store.edgeStimme,
        qwenStimme = store.qwenStimmeId,
        tempo = store.sprechtempo,
        lieblingsStimmen = store.lieblingsStimmen,
        googleSchluessel = store.googleSchluessel,
        alibabaSchluessel = store.alibabaSchluessel,
        groqSchluessel = store.groqSchluessel,
        groqModell = store.groqModell,
        filterSchichten = (1..4).map { store.filterSchichtAktiv(it) },
        modellId = store.modellId,
        modellLabel = store.modellLabel,
        eigeneModelle = store.eigeneModelle.toList().sorted(),
        denktiefe = store.denktiefe,
        themeModus = store.themeModus.value,
        appSperre = store.appSperreAktiv,
        sperreNach = store.sperreNachSekunden,
        codexEmail = container.codex.email,
        codexVerbunden = container.codex.istVerbunden,
        schluesselAblageFehler = if (store.geheimVerfuegbar) {
            null
        } else {
            "Die verschlüsselte Ablage lässt sich auf diesem Gerät nicht öffnen " +
                "(${store.geheimFehlerText}). Schlüssel können deshalb NICHT gespeichert " +
                "werden — sie im Klartext abzulegen wäre die schlechtere Lösung."
        },
    )

    private fun frischLesen() {
        _zustand.value = lieAlles().copy(
            meldung = _zustand.value.meldung,
            fehler = _zustand.value.fehler,
            eigeneStimmen = _zustand.value.eigeneStimmen,
            geraeteCode = _zustand.value.geraeteCode,
            meldeAnLaeuft = _zustand.value.meldeAnLaeuft,
            pruefungen = _zustand.value.pruefungen,
        )
    }

    // --- Vorlesen ---------------------------------------------------------------------------

    fun setzeAnbieter(anbieter: TtsAnbieter) {
        store.ttsAnbieter = anbieter
        frischLesen()
    }

    fun setzeStimme(anbieter: TtsAnbieter, stimme: String) {
        when (anbieter) {
            TtsAnbieter.GOOGLE -> store.googleStimme = stimme
            TtsAnbieter.EDGE -> store.edgeStimme = stimme
            TtsAnbieter.QWEN -> store.qwenStimmeId = stimme
        }
        frischLesen()
    }

    fun setzeTempo(tempo: Float) {
        store.sprechtempo = tempo
        frischLesen()
    }

    fun schalteLieblingsStimme(id: String) {
        val jetzt = store.lieblingsStimmen
        store.lieblingsStimmen = if (id in jetzt) jetzt - id else jetzt + id
        frischLesen()
    }

    fun probeAbspielen(anbieter: TtsAnbieter, stimme: String) {
        container.vorlesen.probiere(anbieter, stimme) { fehler ->
            _zustand.value = _zustand.value.copy(fehler = fehler)
        }
    }

    fun stoppeProbe() = container.vorlesen.stoppe()

    // --- Schlüssel --------------------------------------------------------------------------

    fun setzeGoogleSchluessel(wert: String) {
        store.googleSchluessel = wert
        verwerfePruefung(SCHLUESSEL_GOOGLE)
        frischLesen()
    }

    fun setzeAlibabaSchluessel(wert: String) {
        store.alibabaSchluessel = wert
        verwerfePruefung(SCHLUESSEL_ALIBABA)
        frischLesen()
    }

    fun setzeGroqSchluessel(wert: String) {
        store.groqSchluessel = wert
        verwerfePruefung(SCHLUESSEL_GROQ)
        frischLesen()
    }

    fun setzeGroqModell(wert: String) {
        store.groqModell = wert
        frischLesen()
    }

    fun schalteFilterSchicht(schicht: Int, aktiv: Boolean) {
        store.setzeFilterSchicht(schicht, aktiv)
        frischLesen()
    }

    /**
     * Prüft einen Schlüssel mit einem echten, winzigen Aufruf.
     *
     * Ein Feld mit Inhalt beweist nichts. Erst der Aufruf zeigt, ob der Schlüssel angenommen
     * wird und ob die nötige Schnittstelle im Konto überhaupt freigeschaltet ist.
     */
    fun pruefeGoogle() {
        setzePruefung(SCHLUESSEL_GOOGLE, PruefErgebnis(laeuft = true))
        viewModelScope.launch {
            var fehlgeschlagen = false
            container.vorlesen.probiere(TtsAnbieter.GOOGLE, store.googleStimme) { fehler ->
                fehlgeschlagen = true
                setzePruefung(SCHLUESSEL_GOOGLE, PruefErgebnis(geglueckt = false, text = fehler))
            }
            if (!fehlgeschlagen) {
                setzePruefung(
                    SCHLUESSEL_GOOGLE,
                    PruefErgebnis(
                        geglueckt = true,
                        text = "Verbunden. Wenn du jetzt eine Stimme hörst, stimmt der Schlüssel.",
                    ),
                )
            }
        }
    }

    fun pruefeAlibaba() {
        setzePruefung(SCHLUESSEL_ALIBABA, PruefErgebnis(laeuft = true))
        viewModelScope.launch {
            try {
                val stimmen = container.stimmVerwaltung.liste()
                _zustand.value = _zustand.value.copy(eigeneStimmen = stimmen)
                setzePruefung(
                    SCHLUESSEL_ALIBABA,
                    PruefErgebnis(
                        geglueckt = true,
                        text = if (stimmen.isEmpty()) {
                            "Verbunden. Eigene Stimmen sind noch keine hinterlegt — " +
                                "nimm unten eine auf."
                        } else {
                            "Verbunden. ${stimmen.size} eigene Stimme(n) gefunden."
                        },
                    ),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                setzePruefung(
                    SCHLUESSEL_ALIBABA,
                    PruefErgebnis(
                        geglueckt = false,
                        text = fehler.message ?: "Der Schlüssel wurde abgelehnt.",
                    ),
                )
            }
        }
    }

    fun pruefeGroq() {
        setzePruefung(SCHLUESSEL_GROQ, PruefErgebnis(laeuft = true))
        viewModelScope.launch {
            try {
                // Eine halbe Sekunde Ton reicht als Probe. Schicht 1 würde reine Stille gar
                // nicht senden; hier geht es aber ausdrücklich darum, den Schlüssel zu prüfen —
                // deshalb trägt die Probe einen leisen Ton.
                container.transkribierer.transkribiere(baueProbeAufnahme())
                setzePruefung(
                    SCHLUESSEL_GROQ,
                    PruefErgebnis(geglueckt = true, text = "Verbunden. Der Schlüssel wurde angenommen."),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                setzePruefung(
                    SCHLUESSEL_GROQ,
                    PruefErgebnis(
                        geglueckt = false,
                        text = fehler.message ?: "Der Schlüssel wurde abgelehnt.",
                    ),
                )
            }
        }
    }

    private fun setzePruefung(schluessel: String, ergebnis: PruefErgebnis) {
        _zustand.value = _zustand.value.copy(
            pruefungen = _zustand.value.pruefungen + (schluessel to ergebnis),
        )
    }

    /** Wird beim Tippen im Feld gerufen: Ein geänderter Schlüssel ist nicht mehr geprüft. */
    private fun verwerfePruefung(schluessel: String) {
        if (schluessel !in _zustand.value.pruefungen) return
        _zustand.value = _zustand.value.copy(
            pruefungen = _zustand.value.pruefungen - schluessel,
        )
    }

    private fun baueProbeAufnahme(): ByteArray {
        val rate = 16_000
        val werte = rate / 2
        val pcm = ByteArray(werte * 2)
        // Ein leiser Sinus, damit die Stille-Erkennung die Probe durchlässt und der Aufruf
        // wirklich beim Dienst ankommt.
        for (index in 0 until werte) {
            val wert = (Math.sin(index * 0.05) * 3000).toInt().toShort()
            pcm[index * 2] = (wert.toInt() and 0xFF).toByte()
            pcm[index * 2 + 1] = ((wert.toInt() shr 8) and 0xFF).toByte()
        }
        return de.frank.claudekompass.audio.WavSchneider.baueWav(pcm, rate)
    }

    // --- KI-Modell --------------------------------------------------------------------------

    fun setzeModell(label: String, apiId: String) {
        store.modellLabel = label
        store.modellId = apiId
        frischLesen()
    }

    /** Nimmt ein später erschienenes Modell auf, ohne dass die App dafür geändert werden muss. */
    fun ergaenzeModell(apiId: String) {
        val sauber = apiId.trim()
        if (sauber.isBlank()) return
        store.eigeneModelle = store.eigeneModelle + sauber
        setzeModell(sauber, sauber)
        _zustand.value = _zustand.value.copy(meldung = "Modell „$sauber“ ergänzt und ausgewählt.")
    }

    fun entferneModell(apiId: String) {
        store.eigeneModelle = store.eigeneModelle - apiId
        if (store.modellId == apiId) setzeModell(KiModell.standard.label, KiModell.standard.apiId)
        frischLesen()
    }

    fun setzeDenktiefe(tiefe: Denktiefe) {
        store.denktiefe = tiefe
        frischLesen()
    }

    // --- Codex-Anmeldung --------------------------------------------------------------------

    fun meldeAn(activity: ComponentActivity) {
        if (_zustand.value.meldeAnLaeuft) return
        _zustand.value = _zustand.value.copy(meldeAnLaeuft = true, geraeteCode = "", fehler = "", meldung = "")
        anmeldeJob = viewModelScope.launch {
            try {
                val ergebnis = container.codex.melde(activity) { anmeldung ->
                    _zustand.value = _zustand.value.copy(geraeteCode = anmeldung.benutzerCode)
                }
                _zustand.value = _zustand.value.copy(
                    meldeAnLaeuft = false,
                    geraeteCode = "",
                    meldung = "Angemeldet" + (ergebnis.email?.let { " als $it" } ?: "") + ".",
                )
                frischLesen()
            } catch (abbruch: CancellationException) {
                _zustand.value = _zustand.value.copy(meldeAnLaeuft = false, geraeteCode = "")
                throw abbruch
            } catch (fehler: Exception) {
                _zustand.value = _zustand.value.copy(
                    meldeAnLaeuft = false,
                    geraeteCode = "",
                    fehler = fehler.message ?: "Die Anmeldung ist fehlgeschlagen.",
                )
            }
        }
    }

    fun brichAnmeldungAb() {
        anmeldeJob?.cancel()
        anmeldeJob = null
        container.codex.brichAnmeldungAb()
        _zustand.value = _zustand.value.copy(meldeAnLaeuft = false, geraeteCode = "")
    }

    fun meldeAb() {
        container.codex.meldeAb()
        frischLesen()
        _zustand.value = _zustand.value.copy(meldung = "Von Codex abgemeldet.")
    }

    // --- Eigene Stimme ----------------------------------------------------------------------

    fun ladeEigeneStimmen() {
        _zustand.value = _zustand.value.copy(stimmenLaden = true)
        viewModelScope.launch {
            try {
                _zustand.value = _zustand.value.copy(
                    stimmenLaden = false,
                    eigeneStimmen = container.stimmVerwaltung.liste(),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                _zustand.value = _zustand.value.copy(
                    stimmenLaden = false,
                    fehler = fehler.message ?: "Die eigenen Stimmen konnten nicht geladen werden.",
                )
            }
        }
    }

    fun starteStimmAufnahme() {
        val ging = container.mikrofon.starte(viewModelScope, de.frank.claudekompass.audio.Mikrofon.KLON_ABTASTRATE)
        if (!ging) {
            _zustand.value = _zustand.value.copy(
                fehler = "Die Aufnahme ging nicht los. Prüf, ob die App das Mikrofon benutzen darf.",
            )
            return
        }
        _zustand.value = _zustand.value.copy(
            aufnahmeLaeuft = true,
            aufnahmeSchritt = "Lies den Text ruhig vor. Tipp danach auf Fertig.",
            fehler = "",
            meldung = "",
        )
    }

    fun beendeStimmAufnahme(name: String) {
        if (!_zustand.value.aufnahmeLaeuft) return
        _zustand.value = _zustand.value.copy(aufnahmeSchritt = "Aufnahme wird übertragen …")
        aufnahmeJob = viewModelScope.launch {
            try {
                val wav = container.mikrofon.stoppe()
                if (wav == null || wav.isEmpty()) {
                    _zustand.value = _zustand.value.copy(
                        aufnahmeLaeuft = false,
                        aufnahmeSchritt = "",
                        fehler = "Es kam keine Aufnahme zustande.",
                    )
                    return@launch
                }
                val id = container.stimmVerwaltung.lege(name, wav)
                store.qwenStimmeId = id
                store.ttsAnbieter = TtsAnbieter.QWEN
                _zustand.value = _zustand.value.copy(
                    aufnahmeLaeuft = false,
                    aufnahmeSchritt = "",
                    meldung = "Deine Stimme wurde angelegt und ist jetzt ausgewählt.",
                )
                ladeEigeneStimmen()
                frischLesen()
                KompassLog.info("EinstellungenViewModel", "beendeStimmAufnahme", "Eigene Stimme angelegt")
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                _zustand.value = _zustand.value.copy(
                    aufnahmeLaeuft = false,
                    aufnahmeSchritt = "",
                    fehler = fehler.message ?: "Die Stimme konnte nicht angelegt werden.",
                )
            }
        }
    }

    fun brichStimmAufnahmeAb() {
        aufnahmeJob?.cancel()
        aufnahmeJob = null
        container.mikrofon.gibFrei()
        _zustand.value = _zustand.value.copy(aufnahmeLaeuft = false, aufnahmeSchritt = "")
    }

    fun loescheEigeneStimme(id: String) {
        viewModelScope.launch {
            try {
                container.stimmVerwaltung.loesche(id)
                if (store.qwenStimmeId == id) store.qwenStimmeId = ""
                ladeEigeneStimmen()
                frischLesen()
                _zustand.value = _zustand.value.copy(meldung = "Stimme gelöscht.")
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                _zustand.value = _zustand.value.copy(
                    fehler = fehler.message ?: "Die Stimme konnte nicht gelöscht werden.",
                )
            }
        }
    }

    // --- Darstellung und Sicherheit ----------------------------------------------------------

    fun setzeTheme(modus: ThemeModus) {
        store.setzeThemeModus(modus)
        frischLesen()
    }

    fun setzeSperreNach(sekunden: Int) {
        store.sperreNachSekunden = sekunden
        frischLesen()
    }

    fun schalteAppSperre(activity: androidx.fragment.app.FragmentActivity, an: Boolean) {
        if (!an) {
            container.appSperre.schalteAus()
            frischLesen()
            return
        }
        container.appSperre.schalteEin(
            activity = activity,
            beiErfolg = {
                frischLesen()
                _zustand.value = _zustand.value.copy(meldung = "App-Sperre ist eingeschaltet.")
            },
            beiFehler = { meldung ->
                _zustand.value = _zustand.value.copy(fehler = meldung)
            },
        )
    }

    fun loescheMeldungen() {
        _zustand.value = _zustand.value.copy(meldung = "", fehler = "")
        container.vorlesen.loescheFehler()
    }

    companion object {
        const val SCHLUESSEL_GOOGLE = "google"
        const val SCHLUESSEL_ALIBABA = "alibaba"
        const val SCHLUESSEL_GROQ = "groq"
    }

    override fun onCleared() {
        container.mikrofon.gibFrei()
        container.vorlesen.stoppe()
        super.onCleared()
    }
}
