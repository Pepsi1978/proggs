package de.frank.jarvis.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Anhang
import de.frank.jarvis.ablage.Eintrag
import de.frank.jarvis.ablage.Freigabe
import de.frank.jarvis.ablage.Weitergabe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** „Speichern unter“ mit dem Systemdialog, mit Dateiname und MIME-Typ je Datei (der Standardvertrag kennt nur einen festen Typ). */
private class SpeichernUnter : ActivityResultContract<Freigabe, Uri?>() {
    override fun createIntent(context: Context, input: Freigabe): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType(input.mime).putExtra(Intent.EXTRA_TITLE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/**
 * Alle Aktionen, die eine Datei der Ablage verlassen lassen. Meldungen sagen nur, was wirklich geschehen ist:
 * „Teilen“ öffnet die Auswahl (versendet ist damit noch nichts), „Download“ meldet erst nach der Kopie.
 */
class AblageAktionen internal constructor(
    private val context: Context,
    private val bereich: CoroutineScope,
    private val melde: (String) -> Unit,
    /** Hält die App-Sperre bis zu fünf Minuten zurück, während eine andere App die Datei bearbeitet. */
    private val extern: () -> Unit,
    private val starteSpeichernUnter: (Freigabe) -> Unit,
) {
    private val speicher get() = AblageZentrale.speicher(context)

    fun freigabe(a: Anhang): Freigabe = Freigabe(speicher.datei(a), a.originalName, a.mime)

    fun textFreigabe(e: Eintrag): Freigabe? = speicher.textDatei(e)?.let { Freigabe(it, AblageSpeicher.dateiname(e.titel) + ".md", "text/markdown") }

    /** Alle Dateien eines Eintrags: Text (falls vorhanden) und Anhänge. */
    fun freigaben(e: Eintrag): List<Freigabe> = listOfNotNull(textFreigabe(e)) + e.anhaenge.map(::freigabe)

    fun herunterladen(dateien: List<Freigabe>) {
        if (dateien.isEmpty()) return
        if (!Weitergabe.downloadOhneDialog) {
            if (dateien.size == 1) speichernUnter(dateien[0]) else melde("Auf diesem Android bitte jede Datei einzeln mit „Speichern unter“ sichern.")
            return
        }
        bereich.launch {
            val ergebnis = withContext(Dispatchers.IO) {
                dateien.map { f -> runCatching { Weitergabe.herunterladen(context, f) } }
            }
            val ok = ergebnis.mapNotNull { it.getOrNull() }
            val fehler = ergebnis.mapNotNull { it.exceptionOrNull() }
            melde(
                when {
                    fehler.isEmpty() && ok.size == 1 -> "Gespeichert unter Download/${Weitergabe.ORDNER}/${ok[0]}"
                    fehler.isEmpty() -> "${ok.size} Dateien gespeichert unter Download/${Weitergabe.ORDNER}/"
                    ok.isEmpty() -> "Download fehlgeschlagen: ${fehler[0].message}"
                    else -> "${ok.size} gespeichert, ${fehler.size} fehlgeschlagen (${fehler[0].message})."
                },
            )
        }
    }

    fun speichernUnter(f: Freigabe) { extern(); starteSpeichernUnter(f) }

    fun teilen(dateien: List<Freigabe>, betreff: String?) {
        if (dateien.isEmpty()) return
        extern()
        runCatching { Weitergabe.teilen(context, dateien, betreff) }
            .onSuccess { melde(if (dateien.size == 1) "Teilen-Auswahl geöffnet." else "Teilen-Auswahl mit ${dateien.size} Dateien geöffnet.") }
            .onFailure { melde("Teilen ließ sich nicht öffnen: ${it.message}") }
    }

    fun oeffnen(f: Freigabe, alternativeExport: Boolean = true) {
        extern()
        if (!Weitergabe.oeffnen(context, f)) {
            melde("Auf dem Handy ist keine App installiert, die „${f.name}“ öffnen kann." + if (alternativeExport) " Du kannst die Datei herunterladen oder teilen." else "")
        }
    }

    internal fun schreibeNach(f: Freigabe, ziel: Uri) {
        bereich.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Weitergabe.speichernUnter(context, f.datei, ziel) } }
            melde(r.fold({ "„${f.name}“ gespeichert." }, { "Speichern fehlgeschlagen: ${it.message}" }))
        }
    }
}

@Composable
fun rememberAblageAktionen(vm: AppViewModel): AblageAktionen {
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    val halter = remember { arrayOfNulls<AblageAktionen>(1) }
    // Pfad, Name und Typ überstehen eine Drehung des Handys, während der Systemdialog offen ist.
    val offen = rememberSaveable { mutableStateOf<List<String>?>(null) }
    val starter = rememberLauncherForActivityResult(SpeichernUnter()) { uri ->
        val f = offen.value?.let { Freigabe(File(it[0]), it[1], it[2]) }
        offen.value = null
        if (uri != null && f != null) halter[0]?.schreibeNach(f, uri)
    }
    return remember(context) {
        AblageAktionen(context, bereich, { vm.meldung = it }, { vm.sperrAufschubBis = System.currentTimeMillis() + 5 * 60_000L }) { f ->
            offen.value = listOf(f.datei.absolutePath, f.name, f.mime)
            runCatching { starter.launch(f) }.onFailure { vm.meldung = "Der Dialog „Speichern unter“ ließ sich nicht öffnen." }
        }.also { halter[0] = it }
    }
}
