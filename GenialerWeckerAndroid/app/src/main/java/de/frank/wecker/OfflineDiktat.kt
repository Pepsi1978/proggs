package de.frank.wecker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Die vier Diktatsprachen mit festen BCP-47-Tags; Deutsch ist voreingestellt. */
enum class DiktatSprache(val tag: String, val anzeige: String) {
    DE("de-DE", "Deutsch"), EN("en-US", "Englisch"), FR("fr-FR", "Französisch"), ES("es-ES", "Spanisch")
}

sealed interface DiktatZustand {
    data object Bereit : DiktatZustand
    data object Pruefe : DiktatZustand
    data class Hoert(val zwischentext: String) : DiktatZustand
    /** Erkannter Text, noch nicht übernommen; bleibt editierbar, bis bewusst eingefügt oder verworfen. */
    data class Ergebnis(val text: String) : DiktatZustand
    data class Hinweis(val meldung: String, val sprachpaketLadbar: Boolean = false) : DiktatZustand
}

/** Reine Entscheidungen, ohne Android-Laufzeit prüfbar. */
object DiktatLogik {
    /**
     * Diktatsprache aus Gerätesprache, sonst Region (keine Standort- oder Kontoabfrage).
     * @return Sprache und ob sie wirklich zur Gerätesprache passt (false = Rückfall auf Deutsch)
     */
    fun spracheFuer(sprache: String, region: String): Pair<DiktatSprache, Boolean> = when (sprache.lowercase()) {
        "de" -> DiktatSprache.DE to true
        "fr" -> DiktatSprache.FR to true
        "es" -> DiktatSprache.ES to true
        "en" -> DiktatSprache.EN to true
        else -> when (region.uppercase()) {
            "DE", "AT", "CH", "LI" -> DiktatSprache.DE to true
            "FR" -> DiktatSprache.FR to true
            "ES" -> DiktatSprache.ES to true
            "US", "GB", "IE", "AU", "CA", "NZ" -> DiktatSprache.EN to true
            // Die App ist zuerst deutsch: unbekannte Gerätesprachen diktieren auf Deutsch, mit Hinweis.
            else -> DiktatSprache.DE to false
        }
    }

    /** Genau passendes Tag zuerst, sonst dieselbe Sprache in anderer Region (z. B. de-AT für de-DE). */
    fun passt(sprachen: List<String>, tag: String): Boolean =
        sprachen.any { it.equals(tag, ignoreCase = true) } ||
            sprachen.any { it.substringBefore('-').equals(tag.substringBefore('-'), ignoreCase = true) }

    enum class Unterstuetzung { INSTALLIERT, WIRD_GELADEN, LADBAR, NICHT_UNTERSTUETZT }

    fun unterstuetzung(installiert: List<String>, ladend: List<String>, ladbar: List<String>, tag: String) = when {
        passt(installiert, tag) -> Unterstuetzung.INSTALLIERT
        passt(ladend, tag) -> Unterstuetzung.WIRD_GELADEN
        passt(ladbar, tag) -> Unterstuetzung.LADBAR
        else -> Unterstuetzung.NICHT_UNTERSTUETZT
    }

    /** Verständliche Meldung zu den SpeechRecognizer-Fehlercodes; nie ein Hinweis auf Cloud-Dienste. */
    fun fehlertext(code: Int, sprache: DiktatSprache): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Es wurde nichts verstanden. Tippe erneut auf „Diktieren“ und sprich deutlich – oder schreib den Text von Hand."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Ohne Mikrofonfreigabe ist kein Diktat möglich. Du kannst den Text jederzeit tippen."
        SpeechRecognizer.ERROR_AUDIO -> "Das Mikrofon lieferte keinen Ton. Prüfe, ob eine andere App es gerade benutzt."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Die Spracherkennung ist gerade belegt. Versuch es gleich noch einmal."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
            "${sprache.anzeige} wird von der Offline-Erkennung dieses Geräts nicht unterstützt. Bitte tippe den Text."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Für ${sprache.anzeige} ist kein Offline-Sprachpaket installiert."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            "Die Offline-Erkennung ist gerade nicht bereit. Bitte tippe den Text oder versuch es später erneut."
        else -> "Das Diktat ist fehlgeschlagen (Code $code). Du kannst den Text jederzeit von Hand schreiben."
    }
}

/**
 * Diktat ausschließlich auf dem Gerät: [SpeechRecognizer.createOnDeviceSpeechRecognizer] (Android 12+).
 * Es gibt bewusst keinen Rückfall auf die normale Erkennung, die Audio an einen Dienst schicken kann.
 * Alle Aufrufe laufen auf dem Hauptthread, wie SpeechRecognizer es verlangt.
 */
class OfflineDiktat(private val context: Context) {
    var zustand by mutableStateOf<DiktatZustand>(DiktatZustand.Bereit)
        private set
    private var erkenner: SpeechRecognizer? = null
    private var lader: SpeechRecognizer? = null
    private var sprache = DiktatSprache.DE

    val hoertZu: Boolean get() = zustand is DiktatZustand.Hoert || zustand == DiktatZustand.Pruefe

    fun starten(gewaehlt: DiktatSprache) {
        freigeben()
        sprache = gewaehlt
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            zustand = DiktatZustand.Hinweis("Offline-Diktat braucht Android 12 oder neuer. Bitte tippe den Text von Hand.")
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            zustand = DiktatZustand.Hinweis("Dieses Gerät bietet keine Spracherkennung, die ohne Internet arbeitet. Bitte tippe den Text von Hand – ein Online-Dienst wird bewusst nicht verwendet.")
            return
        }
        // Trotz Verfügbarkeitsprüfung kann das Binden des Dienstes scheitern: Hinweis statt Absturz im Editor.
        val neu = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }.getOrElse {
            zustand = DiktatZustand.Hinweis("Die Offline-Spracherkennung des Geräts ließ sich nicht starten. Bitte tippe den Text von Hand.")
            return
        }
        erkenner = neu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pruefeUndHoere(neu) else hoere(neu)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun pruefeUndHoere(neu: SpeechRecognizer) {
        zustand = DiktatZustand.Pruefe
        neu.checkRecognitionSupport(absicht(sprache), context.mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(unterstuetzung: RecognitionSupport) {
                if (erkenner !== neu) return
                when (DiktatLogik.unterstuetzung(unterstuetzung.installedOnDeviceLanguages,
                    unterstuetzung.pendingOnDeviceLanguages, unterstuetzung.supportedOnDeviceLanguages, sprache.tag)) {
                    DiktatLogik.Unterstuetzung.INSTALLIERT -> hoere(neu)
                    DiktatLogik.Unterstuetzung.WIRD_GELADEN -> beendeMit(DiktatZustand.Hinweis(
                        "Das Offline-Sprachpaket ${sprache.anzeige} wird gerade von Android geladen. Versuch es gleich noch einmal."))
                    DiktatLogik.Unterstuetzung.LADBAR -> beendeMit(DiktatZustand.Hinweis(
                        "Für ${sprache.anzeige} ist noch kein Offline-Sprachpaket installiert. Android kann es einmalig herunterladen; danach funktioniert das Diktat ohne Internet.",
                        sprachpaketLadbar = true))
                    DiktatLogik.Unterstuetzung.NICHT_UNTERSTUETZT -> beendeMit(DiktatZustand.Hinweis(
                        "${sprache.anzeige} wird von der Offline-Erkennung dieses Geräts nicht unterstützt. Bitte tippe den Text."))
                }
            }
            // Manche Erkenner können die Unterstützung nicht melden; dann ehrlich versuchen – Fehler kommen im Listener an.
            override fun onError(fehler: Int) { if (erkenner === neu) hoere(neu) }
        })
    }

    private fun hoere(neu: SpeechRecognizer) {
        neu.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (erkenner === neu) zustand = DiktatZustand.Hoert("") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(teile: Bundle?) {
                val text = teile?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (erkenner === neu && text.isNotBlank()) zustand = DiktatZustand.Hoert(text)
            }
            override fun onResults(ergebnis: Bundle?) {
                if (erkenner !== neu) return
                val text = ergebnis?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                beendeMit(if (text.isBlank()) DiktatZustand.Hinweis(DiktatLogik.fehlertext(SpeechRecognizer.ERROR_NO_MATCH, sprache))
                    else DiktatZustand.Ergebnis(text))
            }
            override fun onError(fehler: Int) {
                if (erkenner !== neu) return
                val bisher = (zustand as? DiktatZustand.Hoert)?.zwischentext.orEmpty()
                // Schon Gehörtes geht nicht verloren: es wartet als Ergebnis auf bewusstes Übernehmen.
                beendeMit(if (bisher.isNotBlank()) DiktatZustand.Ergebnis(bisher) else DiktatZustand.Hinweis(
                    DiktatLogik.fehlertext(fehler, sprache),
                    sprachpaketLadbar = fehler == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        zustand = DiktatZustand.Hoert("")
        neu.startListening(absicht(sprache))
    }

    /** Beendet die Aufnahme; das bis dahin Gesagte kommt als Endergebnis. */
    fun beenden() { erkenner?.stopListening() }

    /** Sofortiger Abbruch, z. B. beim Verlassen des Editors. Schon Gehörtes bleibt zur Übernahme erhalten. */
    fun abbrechen() {
        val bisher = (zustand as? DiktatZustand.Hoert)?.zwischentext.orEmpty()
        erkenner?.cancel()
        freigeben()
        zustand = if (bisher.isNotBlank()) DiktatZustand.Ergebnis(bisher)
            else if (zustand is DiktatZustand.Hoert || zustand == DiktatZustand.Pruefe) DiktatZustand.Bereit else zustand
    }

    fun zeigeHinweis(meldung: String) { freigeben(); zustand = DiktatZustand.Hinweis(meldung) }

    fun ergebnisAendern(text: String) { if (zustand is DiktatZustand.Ergebnis) zustand = DiktatZustand.Ergebnis(text) }
    fun zuruecksetzen() { abbrechen(); zustand = DiktatZustand.Bereit }

    /** Lässt Android das Offline-Sprachpaket laden (dokumentierte Systemfunktion ab Android 13). */
    fun sprachpaketLaden() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lader?.destroy()
        lader = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { it.triggerModelDownload(absicht(sprache)) } }
            .getOrElse { zustand = DiktatZustand.Hinweis("Android konnte den Download nicht starten. Bitte lade das Sprachpaket in den Android-Einstellungen unter „Spracherkennung“."); return }
        zustand = DiktatZustand.Hinweis("Android lädt das Sprachpaket ${sprache.anzeige}. Sobald es fertig ist, tippe erneut auf „Diktieren“.")
    }

    fun freigeben() {
        erkenner?.destroy(); erkenner = null
    }

    fun allesFreigeben() { freigeben(); lader?.destroy(); lader = null }

    private fun beendeMit(neu: DiktatZustand) { freigeben(); zustand = neu }

    private fun absicht(sprache: DiktatSprache) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, sprache.tag)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
}
