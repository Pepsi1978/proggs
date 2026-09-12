package de.frank.gedankenspeicher.ui.einstellungen

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.InvalidationTracker
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.data.Datenbank
import de.frank.gedankenspeicher.data.settings.Einstellungen
import de.frank.module.sicherung.*
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SicherungsAnsicht(
    val umfang: Set<SpeicherTeil> = SpeicherTeil.entries.toSet(),
    val automatisch: Boolean = false,
    val ordner: String? = null,
    val stand: String = "Noch nicht gesichert",
    val geprueft: Boolean = false,
    val naechste: String = "",
    val vorschau: String = "",
    val auswahl: List<Sicherungsdatei> = emptyList(),
    val laeuft: Boolean = false,
    val ruecknahme: Boolean = false,
)

/** Knopflogik wie in der Kompass-Referenz; Sicherungsablauf und Autosicherung kommen aus M1.1. */
class SicherungsAnbindung(
    private val ctx: Context,
    private val db: Datenbank,
    private val einstellungen: Einstellungen,
    private val codex: CodexAuthManager,
    private val scope: CoroutineScope,
    private val ordnerWaehlen: () -> Unit,
    private val dateiWaehlen: () -> Unit,
    private val melde: (String) -> Unit,
    private val nachEinspielen: () -> Unit,
) {
    private val prefs = ctx.getSharedPreferences("gedankenspeicher_sicherung", Context.MODE_PRIVATE)
    private val inhalt = SpeicherInhalt(ctx, db, einstellungen, codex)
    private val _zustand = MutableStateFlow(SicherungsAnsicht(
        umfang = umfang(), automatisch = einstellungen.driveSicherungAn,
    ))
    val zustand = _zustand.asStateFlow()
    @Volatile private var geschlossen = false
    private var nachOrdner = false
    private var autoNachOrdner = false
    private var vorschauDatei: File? = null
    private var spur: Einspielspur? = null
    private val protokoll = object : SicherungsProtokoll {
        override fun info(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) {
            if (stelle == "SicherungsDienst" || stelle == "AutoSicherung") scope.launch { aktualisiere() }
        }
        override fun warn(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) {
            android.util.Log.w("M1.1", "$stelle: $meldung")
            scope.launch { aktualisiere() }
        }
    }
    private val dienst = SicherungsDienst(ctx, inhalt,
        SicherungsNamen("gedankenspeicher", einstellungenDatei = "gedankenspeicher_sicherung"),
        ruecknahme = inhalt, protokoll = protokoll, umfangGeber = { umfang() })
    private val auto = AutoSicherung(dienst, {
        !geschlossen && einstellungen.driveSicherungAn && !_zustand.value.laeuft && vorschauDatei == null
    }, protokoll)
    private val beobachter = object : InvalidationTracker.Observer("notiz", "sitzung", "ki_antwort", "ordner", "auswertungsprofil") {
        override fun onInvalidated(tables: Set<String>) { scope.launch { auto.melde("Bestand geändert"); aktualisiere() } }
    }
    private val einstellungsBeobachter = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key !in setOf("drive_zeit", "drive_groesse", "drive_ordner")) scope.launch { auto.melde("Einstellungen geändert") }
    }

    init {
        // EncryptedSharedPreferences können nicht als offene Modul-Preferences weiterbenutzt werden.
        // Einmalige, ausdrücklich freigegebene Übernahme; ein vergessener Ordner kommt nie zurück.
        if (!prefs.getBoolean("altbestand_uebernommen", false)) {
            val editor = prefs.edit().putBoolean("altbestand_uebernommen", true)
                .putLong("zuletzt_gesichert", einstellungen.letzteSicherungZeit)
                .putBoolean("zuletzt_geprueft", false)
            if (einstellungen.sicherungsordner.isNotBlank()) editor.putString("sicherungs_ordner", einstellungen.sicherungsordner)
            check(editor.commit())
        }
        db.invalidationTracker.addObserver(beobachter)
        einstellungen.beobachteSicherungsdaten(einstellungsBeobachter)
        codex.beobachteSicherungsdaten(einstellungsBeobachter)
        ProcessLifecycleOwner.get().lifecycle.addObserver(auto)
        scope.launch { aktualisiere(); auto.melde("App geöffnet") }
    }

    private fun umfang(): Set<SpeicherTeil> {
        val ids = prefs.getStringSet("umfang", null)
        return SpeicherTeil.entries.filter { ids == null || it.id in ids }.toSet().ifEmpty { SpeicherTeil.entries.toSet() }
    }

    fun schalteTeil(teil: SpeicherTeil, an: Boolean) {
        if (_zustand.value.laeuft) return
        val neu = umfang().toMutableSet()
        if (an) neu.add(teil) else neu.remove(teil)
        if (neu.isEmpty()) return melde("Mindestens ein Teil muss angehakt bleiben.")
        prefs.edit().putStringSet("umfang", neu.map { it.id }.toSet()).apply()
        _zustand.update { it.copy(umfang = neu) }
        auto.melde("Sicherungsumfang geändert")
        scope.launch { aktualisiere() }
    }

    fun schalteAuto(an: Boolean) {
        if (_zustand.value.laeuft) return
        einstellungen.driveSicherungAn = an
        _zustand.update { it.copy(automatisch = an) }
        if (an && dienst.sicherungsOrdner == null) {
            autoNachOrdner = true; waehleOrdner()
        } else if (an) auto.melde("Autosicherung eingeschaltet")
    }

    fun waehleOrdner() { if (!_zustand.value.laeuft) ordnerWaehlen() }
    fun ordnerAbgebrochen() {
        if (autoNachOrdner && dienst.sicherungsOrdner == null) schalteAuto(false)
        autoNachOrdner = false; nachOrdner = false
    }
    fun merkeOrdner(uri: Uri) = arbeite {
        withContext(Dispatchers.IO) { dienst.merkeOrdner(uri); einstellungen.sicherungsordner = uri.toString() }
        autoNachOrdner = false
        if (nachOrdner) { nachOrdner = false; dienst.sichere() }
        auto.melde("Sicherungsordner gewählt")
    }
    fun vergissOrdner() = arbeite {
        withContext(Dispatchers.IO) { dienst.vergissOrdner(); einstellungen.sicherungsordner = "" }
        einstellungen.driveSicherungAn = false
        _zustand.update { it.copy(automatisch = false, auswahl = emptyList()) }
        vorschauDatei?.delete(); vorschauDatei = null
        _zustand.update { it.copy(vorschau = "") }
    }

    fun sichereJetzt() {
        if (_zustand.value.laeuft) return
        if (dienst.sicherungsOrdner == null) { nachOrdner = true; ordnerWaehlen(); return }
        arbeite { melde(dienst.sichere()) }
    }

    private suspend fun alleSicherungen(): List<Sicherungsdatei> = withContext(Dispatchers.IO) {
        val neu = dienst.sicherungen()
        val alt = dienst.sicherungsOrdner?.let { DocumentFile.fromTreeUri(ctx, it) }?.listFiles().orEmpty()
            .filter { it.isFile && (it.name.orEmpty().startsWith("gedankenspeicher-") && (it.name.orEmpty().endsWith(".db") || it.name.orEmpty().endsWith(".zip"))) }
            .map { Sicherungsdatei(it.uri, it.name.orEmpty(), it.lastModified()) }
        (neu + alt).sortedByDescending { it.geaendertAm }
    }
    fun zeigeAuswahl() = arbeite {
        if (dienst.sicherungsOrdner == null) { ordnerWaehlen(); return@arbeite }
        val liste = alleSicherungen()
        _zustand.update { it.copy(auswahl = liste) }
        if (liste.isEmpty()) melde("In diesem Ordner liegt noch keine Sicherung.")
    }
    fun neueste() = arbeite {
        val quelle = alleSicherungen().firstOrNull() ?: return@arbeite melde("Keine Sicherung im gewählten Ordner gefunden.")
        vorschau(quelle.uri)
    }
    fun externeDatei() { if (!_zustand.value.laeuft) dateiWaehlen() }
    fun waehleSicherung(uri: Uri) = arbeite { vorschau(uri) }
    private suspend fun vorschau(uri: Uri) {
        val (datei, text) = inhalt.bereiteVor(uri)
        vorschauDatei?.delete(); vorschauDatei = datei
        // M1.1 prüft auch den konvertierten Altbestand vor dem Bestätigungsknopf.
        val geprueft = dienst.vorschauVon(Uri.fromFile(datei))
        _zustand.update { it.copy(vorschau = if (geprueft.erstelltAm == "älteres Format") "$text. Fehlende Gedanken ergänzen; enthaltene Einstellungen und Profile übernehmen." else text, auswahl = emptyList()) }
    }
    fun verwerfeVorschau() {
        if (_zustand.value.laeuft) return
        vorschauDatei?.delete(); vorschauDatei = null
        _zustand.update { it.copy(vorschau = "") }
    }
    fun verwerfeAuswahl() { if (!_zustand.value.laeuft) _zustand.update { it.copy(auswahl = emptyList()) } }
    fun spieleEin() = arbeite {
        val datei = vorschauDatei ?: return@arbeite
        spur = dienst.stelleWiederHerAus(Uri.fromFile(datei)).spur
        datei.delete(); vorschauDatei = null
        _zustand.update { it.copy(vorschau = "", ruecknahme = spur != null) }
        nachEinspielen()
        melde("Sicherung eingespielt. Vorhandene Gedanken bleiben erhalten.")
    }
    fun rueckgaengig() = arbeite {
        val letzteSpur = spur ?: return@arbeite
        val anzahl = dienst.nimmZurueck(letzteSpur)
        spur = null
        _zustand.update { it.copy(ruecknahme = false) }
        nachEinspielen()
        melde("Einspielen zurückgenommen: $anzahl Datensätze. Später bearbeitete Gedanken bleiben erhalten.")
    }

    private fun arbeite(block: suspend () -> Unit) {
        if (geschlossen || _zustand.value.laeuft) return
        _zustand.update { it.copy(laeuft = true) }
        scope.launch {
            try { block() }
            catch (abbruch: CancellationException) { throw abbruch }
            catch (fehler: Exception) { melde(fehler.message ?: "Die Sicherung ist fehlgeschlagen.") }
            finally {
                inhalt.aufraeumen()
                _zustand.update { it.copy(laeuft = false) }
                aktualisiere()
                auto.melde("Sicherungsaktion beendet")
            }
        }
    }
    suspend fun aktualisiere() {
        if (geschlossen) return
        try {
            val (ordner, groesse) = withContext(Dispatchers.IO) { dienst.ordnerName() to dienst.voraussichtlich() }
            _zustand.update { it.copy(ordner = ordner, stand = dienst.standText(), geprueft = dienst.istGeprueft(),
                automatisch = einstellungen.driveSicherungAn,
                naechste = groesse.first.anzahl.entries.joinToString { e -> "${e.value} ${e.key}" } + " — etwa ${groesse.second / 1024} kB") }
        } catch (abbruch: CancellationException) { throw abbruch }
        catch (fehler: Exception) { _zustand.update { it.copy(stand = fehler.message ?: "Ordner nicht erreichbar") } }
    }
    fun beende() {
        geschlossen = true
        auto.beende()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(auto)
        db.invalidationTracker.removeObserver(beobachter)
        einstellungen.entferneSicherungsbeobachter(einstellungsBeobachter)
        codex.entferneSicherungsbeobachter(einstellungsBeobachter)
        vorschauDatei?.delete()
        inhalt.aufraeumen()
    }
}
