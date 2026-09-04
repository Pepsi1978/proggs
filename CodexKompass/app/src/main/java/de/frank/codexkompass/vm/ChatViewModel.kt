package de.frank.codexkompass.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.codexkompass.KompassContainer
import de.frank.codexkompass.ai.Prompts
import de.frank.codexkompass.data.local.ChatNachrichtEntity
import de.frank.codexkompass.data.local.ChatSitzungEntity
import de.frank.codexkompass.observability.KompassLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatZustand(
    val aktiveSitzung: Long = 0L,
    val eingabe: String = "",
    val antwortet: Boolean = false,
    val fehler: String = "",
    val listeOffen: Boolean = true,
)

/**
 * Der Gesprächsbereich.
 *
 * Mehrere Gespräche liegen nebeneinander; jedes hat seinen eigenen Verlauf. Der Verlauf wandert
 * bei jeder Frage mit, damit Rückbezüge wie „und was war noch mal der Unterschied dazu?"
 * funktionieren. Ältere Nachrichten fallen ab einer Grenze weg — sonst wächst jede Anfrage
 * unbegrenzt und wird teurer, ohne besser zu werden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val container: KompassContainer) : ViewModel() {

    private val repository = container.repository

    private val _zustand = MutableStateFlow(ChatZustand())
    val zustand: StateFlow<ChatZustand> = _zustand.asStateFlow()

    private var antwortJob: Job? = null

    val sitzungen: StateFlow<List<ChatSitzungEntity>> = repository.beobachteSitzungen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nachrichten: StateFlow<List<ChatNachrichtEntity>> = _zustand
        .flatMapLatest { zustand ->
            if (zustand.aktiveSitzung == 0L) {
                flowOf(emptyList())
            } else {
                repository.beobachteNachrichten(zustand.aktiveSitzung)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vorleseZustand = container.vorlesen.zustand

    fun waehleSitzung(id: Long) {
        if (_zustand.value.aktiveSitzung == id) return
        container.vorlesen.stoppe()
        _zustand.value = _zustand.value.copy(aktiveSitzung = id, fehler = "", listeOffen = false)
    }

    fun legeSitzungAn() {
        viewModelScope.launch {
            val id = repository.legeSitzung(NEUER_TITEL)
            _zustand.value = _zustand.value.copy(aktiveSitzung = id, listeOffen = false, fehler = "")
            KompassLog.info("ChatViewModel", "legeSitzungAn", "Neues Gespräch", mapOf("id" to id))
        }
    }

    fun loescheSitzung(id: Long) {
        viewModelScope.launch {
            repository.loescheSitzung(id)
            if (_zustand.value.aktiveSitzung == id) {
                _zustand.value = _zustand.value.copy(aktiveSitzung = 0L, listeOffen = true)
            }
        }
    }

    fun schalteListe() {
        _zustand.value = _zustand.value.copy(listeOffen = !_zustand.value.listeOffen)
    }

    fun setzeEingabe(text: String) {
        _zustand.value = _zustand.value.copy(eingabe = text)
    }

    fun loescheFehler() {
        _zustand.value = _zustand.value.copy(fehler = "")
        container.vorlesen.loescheFehler()
    }

    fun schicke() {
        val frage = _zustand.value.eingabe.trim()
        if (frage.isBlank() || _zustand.value.antwortet) return
        if (!container.codex.istVerbunden) {
            _zustand.value = _zustand.value.copy(
                fehler = "Zum Reden braucht es die Anmeldung bei Codex. Du findest sie in den " +
                    "Einstellungen.",
            )
            return
        }

        antwortJob = viewModelScope.launch {
            // Ohne offenes Gespräch legt der erste Satz eines an — man soll nicht erst einen
            // Knopf suchen müssen, um eine Frage stellen zu können.
            val sitzungId = _zustand.value.aktiveSitzung.takeIf { it != 0L }
                ?: repository.legeSitzung(NEUER_TITEL).also {
                    _zustand.value = _zustand.value.copy(aktiveSitzung = it)
                }

            val bisher = repository.ladeNachrichten(sitzungId)
            repository.fuegeNachrichtEin(sitzungId, ROLLE_BENUTZER, frage)
            val antwortId = repository.fuegeNachrichtEin(sitzungId, ROLLE_KI, "")
            _zustand.value = _zustand.value.copy(eingabe = "", antwortet = true, fehler = "")

            try {
                val verlauf = bisher
                    .filter { it.text.isNotBlank() }
                    .takeLast(VERLAUF_NACHRICHTEN)
                    .map { it.rolle to it.text }
                val namensliste = Prompts.namensliste(repository.ladeKomplett())

                val gesammelt = StringBuilder()
                val antwort = container.codex.frage(
                    anweisung = Prompts.chatAnweisung(),
                    eingabe = Prompts.chatEingabe(verlauf, frage, namensliste),
                    modellId = container.einstellungen.modellId,
                    denktiefe = container.einstellungen.denktiefe.apiValue,
                ) { stueck ->
                    gesammelt.append(stueck)
                    repository.aktualisiereNachricht(antwortId, gesammelt.toString())
                }
                repository.aktualisiereNachricht(antwortId, antwort.trim())
                benenneWennNoetig(sitzungId, frage)
            } catch (abbruch: CancellationException) {
                repository.aktualisiereNachricht(antwortId, "", "Die Antwort wurde abgebrochen.")
                throw abbruch
            } catch (problem: Exception) {
                val meldung = problem.message ?: "Die Antwort ist fehlgeschlagen."
                repository.aktualisiereNachricht(antwortId, "", meldung)
                _zustand.value = _zustand.value.copy(fehler = meldung)
                KompassLog.error("ChatViewModel", "schicke", "Antwort fehlgeschlagen", mapOf("grund" to meldung))
            } finally {
                _zustand.value = _zustand.value.copy(antwortet = false)
            }
        }
    }

    fun brichAntwortAb() {
        antwortJob?.cancel()
        antwortJob = null
        container.codex.brichAnfragenAb()
        _zustand.value = _zustand.value.copy(antwortet = false)
    }

    /** Gibt einem noch namenlosen Gespräch einen Titel aus der ersten Frage. */
    private suspend fun benenneWennNoetig(sitzungId: Long, frage: String) {
        val sitzung = sitzungen.value.firstOrNull { it.id == sitzungId } ?: return
        if (sitzung.titel != NEUER_TITEL) return
        val titel = try {
            container.codex.frage(
                anweisung = Prompts.titelAnweisung(),
                eingabe = frage,
                modellId = container.einstellungen.modellId,
                denktiefe = "low",
            ).trim().trim('"', '„', '“').take(60)
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (problem: Exception) {
            // Ein fehlender Titel ist ein Schönheitsfehler. Statt die Antwort deswegen als
            // gescheitert darzustellen, wird der Anfang der Frage genommen.
            KompassLog.warn("ChatViewModel", "benenneWennNoetig", "Titel fehlgeschlagen", mapOf("grund" to problem.message))
            frage.take(60)
        }
        if (titel.isNotBlank()) repository.benenneSitzungUm(sitzungId, titel)
    }

    fun liesNachrichtVor(nachricht: ChatNachrichtEntity) {
        container.vorlesen.schalteUm("chat:${nachricht.id}", nachricht.text)
    }

    fun stoppeVorlesen() = container.vorlesen.stoppe()

    override fun onCleared() {
        container.vorlesen.stoppe()
        container.codex.brichAnfragenAb()
        super.onCleared()
    }

    companion object {
        const val ROLLE_BENUTZER = "benutzer"
        const val ROLLE_KI = "ki"
        const val NEUER_TITEL = "Neues Gespräch"

        /**
         * So viele frühere Nachrichten wandern mit. Genug für Rückbezüge, wenig genug, damit
         * ein langes Gespräch die Anfrage nicht ins Unermessliche wachsen lässt.
         */
        const val VERLAUF_NACHRICHTEN = 12
    }
}
