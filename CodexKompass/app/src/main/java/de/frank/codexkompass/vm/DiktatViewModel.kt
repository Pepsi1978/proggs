package de.frank.codexkompass.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.codexkompass.KompassContainer
import de.frank.codexkompass.observability.KompassLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DiktatStufe { AUS, NIMMT_AUF, SCHREIBT_AB }

/**
 * Zustand des Diktierens.
 *
 * [ziel] sagt, WELCHES Mikrofon leuchtet und wohin der Text geht. Ohne diese Angabe würden bei
 * mehreren Mikrofonknöpfen auf einem Bildschirm alle gleichzeitig als aktiv erscheinen — und
 * der Text landete womöglich im falschen Feld.
 */
data class DiktatZustand(
    val stufe: DiktatStufe = DiktatStufe.AUS,
    val ziel: String = "",
    val fehler: String = "",
    val hinweis: String = "",
)

/**
 * Nimmt auf und schreibt ab (Referenz, Baustein F).
 *
 * Ein leeres Ergebnis ist hier ein GÜLTIGER Ausgang, kein Fehler: Genau dann haben die
 * Halluzinations-Schichten gegriffen, weil nichts Verwertbares gesprochen wurde. Der Benutzer
 * bekommt einen Hinweis statt einer Fehlermeldung — sonst wirkt der Schutz wie eine Störung.
 */
class DiktatViewModel(private val container: KompassContainer) : ViewModel() {

    private val _zustand = MutableStateFlow(DiktatZustand())
    val zustand: StateFlow<DiktatZustand> = _zustand.asStateFlow()

    private var laufenderJob: Job? = null

    /** Startet oder beendet die Aufnahme für [ziel]. Das Ergebnis geht an [beiText]. */
    fun schalteUm(ziel: String, beiText: (String) -> Unit) {
        when (_zustand.value.stufe) {
            DiktatStufe.NIMMT_AUF -> beendeUndSchreibeAb(beiText)
            DiktatStufe.SCHREIBT_AB -> Unit // Läuft schon; ein zweiter Tipp tut nichts.
            DiktatStufe.AUS -> starte(ziel)
        }
    }

    private fun starte(ziel: String) {
        val ging = container.mikrofon.starte(viewModelScope)
        if (!ging) {
            _zustand.value = DiktatZustand(
                fehler = "Die Aufnahme ging nicht los. Prüf, ob die App das Mikrofon benutzen " +
                    "darf, und ob es gerade eine andere App belegt.",
            )
            return
        }
        _zustand.value = DiktatZustand(stufe = DiktatStufe.NIMMT_AUF, ziel = ziel)
        KompassLog.info("DiktatViewModel", "starte", "Aufnahme läuft", mapOf("ziel" to ziel))
    }

    private fun beendeUndSchreibeAb(beiText: (String) -> Unit) {
        val ziel = _zustand.value.ziel
        _zustand.value = _zustand.value.copy(stufe = DiktatStufe.SCHREIBT_AB)
        laufenderJob = viewModelScope.launch {
            try {
                val wav = container.mikrofon.stoppe()
                if (wav == null || wav.isEmpty()) {
                    _zustand.value = DiktatZustand(hinweis = "Es kam keine Aufnahme zustande.")
                    return@launch
                }
                val text = container.transkribierer.transkribiere(wav)
                if (text.isBlank()) {
                    _zustand.value = DiktatZustand(
                        hinweis = "Es wurde nichts Gesprochenes erkannt. Sprich beim nächsten " +
                            "Mal etwas näher ans Mikrofon.",
                    )
                    return@launch
                }
                beiText(text)
                _zustand.value = DiktatZustand()
                KompassLog.info(
                    "DiktatViewModel",
                    "beendeUndSchreibeAb",
                    "Text erkannt",
                    mapOf("ziel" to ziel, "zeichen" to text.length),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                _zustand.value = DiktatZustand(
                    fehler = fehler.message ?: "Das Abschreiben ist fehlgeschlagen.",
                )
                KompassLog.error(
                    "DiktatViewModel",
                    "beendeUndSchreibeAb",
                    "Abschreiben fehlgeschlagen",
                    mapOf("grund" to fehler.message),
                )
            }
        }
    }

    fun brichAb() {
        laufenderJob?.cancel()
        laufenderJob = null
        container.mikrofon.gibFrei()
        _zustand.value = DiktatZustand()
    }

    fun loescheMeldung() {
        _zustand.value = _zustand.value.copy(fehler = "", hinweis = "")
    }

    override fun onCleared() {
        container.mikrofon.gibFrei()
        super.onCleared()
    }
}
