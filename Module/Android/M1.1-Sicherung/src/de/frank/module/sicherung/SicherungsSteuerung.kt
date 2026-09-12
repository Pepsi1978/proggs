// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v1
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.net.Uri
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Eine Sicherungsdatei, wie sie in der Auswahlliste steht. */
data class SicherungsEintrag(val quelle: Uri, val name: String, val geschriebenAm: String)

/**
 * Alles, was die Oberfläche über die Sicherung wissen muss.
 *
 * **Hier steht kein Compose.** Wie die Sicherung aussieht, entscheidet jede App für sich mit
 * ihren eigenen Bausteinen und Farben — das Modul liefert nur, was angezeigt werden soll, und
 * nimmt entgegen, was angetippt wurde. Genau dadurch sind die Funktionen überall dieselben,
 * während sich das Aussehen der App anpasst.
 */
data class SicherungsZustand(
    val ordner: String? = null,
    val stand: String = "",
    val laeuft: Boolean = false,
    val geprueft: Boolean = false,
    val autoSicherung: Boolean = false,
    /** Was angehakt ist. */
    val umfang: Set<SicherungsTeil> = emptySet(),
    /** „12 Einträge, 3 Fragen — etwa 40 KB" */
    val vorschauUmfang: String = "",
    /** Liegt eine Datei zum Einspielen bereit, steht sie hier. */
    val bereit: Uri? = null,
    val vorschauText: String = "",
    val auswahl: List<SicherungsEintrag> = emptyList(),
    val auswahlLaeuft: Boolean = false,
    val fehler: String = "",
)

/** Wo sich merkt, welche Teile angehakt sind. Die App entscheidet, wohin das geht. */
interface UmfangSpeicher {
    fun teile(): Set<SicherungsTeil>
    fun setze(teil: SicherungsTeil, aktiv: Boolean)
    fun autoSicherung(): Boolean
    fun setzeAutoSicherung(an: Boolean)
}

/**
 * Die Steuerung hinter den Knöpfen.
 *
 * Sie hält den Zustand, ruft den [SicherungsDienst] und fängt Fehler ab, damit in der
 * Oberfläche ein Satz steht statt eines Absturzes.
 */
class SicherungsSteuerung(
    private val dienst: SicherungsDienst,
    private val speicher: UmfangSpeicher,
    private val inhalt: SicherungsInhalt,
    private val bereich: CoroutineScope,
) {
    private val _zustand = MutableStateFlow(
        SicherungsZustand(
            umfang = speicher.teile(),
            autoSicherung = speicher.autoSicherung(),
        ),
    )
    val zustand: StateFlow<SicherungsZustand> = _zustand.asStateFlow()

    private var nachOrdnerWahlSichern = false

    /** Was das letzte Einspielen angelegt hat — nur dafür gilt der Zurück-Pfeil. */
    private var letzteSpur: Einspielspur? = null

    /** Alles, was angehakt werden kann — in der Reihenfolge, die die App vorgibt. */
    val teile: List<SicherungsTeil> get() = inhalt.teile

    /** Der gemerkte Ordner als Adresse — der Dateiwähler startet darin. */
    val ordnerUri: Uri? get() = dienst.sicherungsOrdner

    fun aktualisiere() {
        _zustand.value = _zustand.value.copy(
            ordner = dienst.ordnerName(),
            stand = dienst.standText(),
            geprueft = dienst.istGeprueft(),
            umfang = speicher.teile(),
            autoSicherung = speicher.autoSicherung(),
        )
        zaehleVoraussichtlichenUmfang()
    }

    /**
     * Schaltet einen Teil der Sicherung an oder ab.
     *
     * Der letzte Haken lässt sich nicht entfernen: Eine Sicherung ohne Inhalt wäre eine Datei,
     * die aussieht wie eine Sicherung und keine ist — das fällt erst auf, wenn man sie braucht.
     */
    fun schalteTeil(teil: SicherungsTeil, aktiv: Boolean) {
        if (!aktiv && _zustand.value.umfang.size <= 1) {
            _zustand.value = _zustand.value.copy(fehler = "Mindestens ein Punkt muss gesichert werden.")
            return
        }
        speicher.setze(teil, aktiv)
        _zustand.value = _zustand.value.copy(umfang = speicher.teile(), fehler = "")
        zaehleVoraussichtlichenUmfang()
    }

    /**
     * Sagt vorab, wie viel die nächste Sicherung umfasst und wie groß sie etwa wird.
     *
     * Ohne diese Zeile tippt man auf „Jetzt sichern" und weiß hinterher nicht, ob gerade drei
     * Kilobyte oder zwei Megabyte in die Cloud gewandert sind.
     */
    fun zaehleVoraussichtlichenUmfang() {
        bereich.launch {
            runCatching { dienst.voraussichtlich() }
                .onSuccess { (zahlen, bytes) ->
                    val teile = zahlen.anzahl.filterValues { it > 0 }.map { (art, wieViele) -> "$wieViele $art" }
                    _zustand.value = _zustand.value.copy(
                        vorschauUmfang = if (teile.isEmpty()) {
                            "Nichts ausgewählt."
                        } else {
                            teile.joinToString(", ") + " — etwa ${lesbareGroesse(bytes)}"
                        },
                    )
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(vorschauUmfang = "")
                }
        }
    }

    /** Die gespeicherte Freigabe reicht aus; nur beim ersten Mal einen Ordner wählen. */
    fun sichereJetzt(ordnerWaehlen: () -> Unit) {
        if (_zustand.value.laeuft) return
        if (dienst.sicherungsOrdner == null) {
            nachOrdnerWahlSichern = true
            ordnerWaehlen()
            return
        }
        starteSicherung()
    }

    private fun starteSicherung() {
        _zustand.value = _zustand.value.copy(laeuft = true, fehler = "")
        bereich.launch {
            runCatching { dienst.sichere() }
                .onSuccess { stand ->
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        stand = stand,
                        geprueft = dienst.istGeprueft(),
                    )
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        stand = dienst.standText(),
                        fehler = fehler.message ?: "Die Sicherung ist fehlgeschlagen.",
                    )
                }
        }
    }

    fun waehleOrdner(ordnerWaehlen: () -> Unit) {
        nachOrdnerWahlSichern = false
        ordnerWaehlen()
    }

    /** Android hat einen Ordner geliefert — oder der Benutzer hat abgebrochen. */
    fun ordnerGewaehlt(ordner: Uri?) {
        if (ordner == null) {
            // Abgebrochen: Der Schalter darf nicht auf „an" stehen bleiben, wenn nie ein
            // Ordner gewählt wurde — sonst wartet man auf Sicherungen, die nicht kommen.
            nachOrdnerWahlSichern = false
            _zustand.value = _zustand.value.copy(autoSicherung = speicher.autoSicherung())
            return
        }
        dienst.merkeOrdner(ordner)
        _zustand.value = _zustand.value.copy(ordner = dienst.ordnerName(), fehler = "")
        if (nachOrdnerWahlSichern) {
            nachOrdnerWahlSichern = false
            starteSicherung()
        }
    }

    fun vergissOrdner() {
        dienst.vergissOrdner()
        speicher.setzeAutoSicherung(false)
        _zustand.value = _zustand.value.copy(ordner = null, autoSicherung = false)
    }

    fun schalteAutoSicherung(an: Boolean, ordnerWaehlen: () -> Unit) {
        speicher.setzeAutoSicherung(an)
        _zustand.value = _zustand.value.copy(autoSicherung = an)
        // Ein Schalter auf „an" ohne Ordner schreibt nie etwas. Statt das nur zu erklären,
        // wird gleich gefragt.
        if (an && dienst.sicherungsOrdner == null) {
            nachOrdnerWahlSichern = false
            ordnerWaehlen()
        }
    }

    /**
     * Zeigt die Sicherungen im Ordner zur Auswahl.
     *
     * Die App zeigt die Liste selbst, statt den Dateiwähler von Android zu öffnen — aus dem
     * führt die Zurück-Geste Ordner für Ordner heraus statt zurück in die App.
     */
    fun zeigeAuswahl(beiSystemwahl: () -> Unit) {
        if (dienst.sicherungsOrdner == null) {
            beiSystemwahl()
            return
        }
        _zustand.value = _zustand.value.copy(auswahlLaeuft = true, fehler = "")
        bereich.launch {
            runCatching { dienst.sicherungen() }
                .onSuccess { dateien ->
                    _zustand.value = _zustand.value.copy(
                        auswahlLaeuft = false,
                        auswahl = dateien.map {
                            SicherungsEintrag(it.uri, it.name, BackupStatus.formatiere(it.geaendertAm))
                        },
                    )
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(
                        auswahlLaeuft = false,
                        fehler = fehler.message ?: "Die Sicherungen ließen sich nicht lesen.",
                    )
                }
        }
    }

    fun verwirfAuswahl() {
        _zustand.value = _zustand.value.copy(auswahl = emptyList(), auswahlLaeuft = false)
    }

    /** Erst ansehen, dann einspielen — nie ungefragt in die Datenbank. */
    fun zeigeVorschau(quelle: Uri) {
        _zustand.value = _zustand.value.copy(laeuft = true, auswahl = emptyList(), fehler = "")
        bereich.launch {
            runCatching { dienst.vorschauVon(quelle) }
                .onSuccess { vorschau ->
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        bereit = quelle,
                        vorschauText = inhalt.fasseZusammen(vorschau),
                    )
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        bereit = null,
                        fehler = fehler.message ?: "Die Sicherung ließ sich nicht lesen.",
                    )
                }
        }
    }

    fun verwirfVorschau() {
        _zustand.value = _zustand.value.copy(bereit = null, vorschauText = "")
    }

    fun spieleEin() {
        val quelle = _zustand.value.bereit ?: return
        _zustand.value = _zustand.value.copy(laeuft = true, bereit = null, vorschauText = "")
        bereich.launch {
            runCatching { dienst.stelleWiederHerAus(quelle) }
                .onSuccess { ergebnis ->
                    letzteSpur = ergebnis.spur
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        stand = inhalt.fasseZusammen(ergebnis.vorschau),
                    )
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(
                        laeuft = false,
                        fehler = fehler.message ?: "Das Einspielen ist fehlgeschlagen.",
                    )
                }
        }
    }

    /** Ob gerade etwas zurückgenommen werden kann — sonst bleibt der Knopf weg. */
    val kannZurueckNehmen: Boolean get() = dienst.kannZurueckNehmen && letzteSpur != null

    fun nimmEinspielenZurueck() {
        val spur = letzteSpur ?: return
        bereich.launch {
            runCatching { dienst.nimmZurueck(spur) }
                .onSuccess { wieViele ->
                    letzteSpur = null
                    _zustand.value = _zustand.value.copy(stand = "$wieViele Sätze wieder entfernt.")
                }
                .onFailure { fehler ->
                    if (fehler is CancellationException) throw fehler
                    _zustand.value = _zustand.value.copy(
                        fehler = fehler.message ?: "Das Zurücknehmen ist fehlgeschlagen.",
                    )
                }
        }
    }

    fun verwirfFehler() {
        _zustand.value = _zustand.value.copy(fehler = "")
    }

    private fun lesbareGroesse(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Byte"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.GERMANY, "%.1f MB", bytes / 1024.0 / 1024.0)
    }
}
