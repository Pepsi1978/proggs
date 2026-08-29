package de.frank.claudekompass

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.claudekompass.data.Sicherung
import de.frank.claudekompass.data.SicherungsFehler
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.ui.KompassApp
import de.frank.claudekompass.ui.theme.ClaudeKompassTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Der einzige Bildschirm der App.
 *
 * Erbt von [FragmentActivity], weil die biometrische Abfrage das verlangt — mit einer reinen
 * `ComponentActivity` liesse sich die App-Sperre nicht anzeigen.
 */
class MainActivity : FragmentActivity() {

    private val container get() = (application as KompassApplication).container

    private val mikrofonErlaubnis = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { erteilt ->
        if (!erteilt) {
            zeige(
                "Ohne Zugriff aufs Mikrofon geht das Sprechen nicht. Du kannst ihn in den " +
                    "Android-Einstellungen der App nachträglich erteilen.",
            )
        }
    }

    private val exportZiel = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { adresse -> adresse?.let { schreibeSicherung(it) } }

    private val importQuelle = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { adresse -> adresse?.let { leseSicherung(it) } }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solange die App gesperrt sein kann, darf ihr Inhalt nicht in der Vorschau des
        // App-Umschalters landen (Referenz, Baustein I).
        if (container.einstellungen.appSperreAktiv) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        frageMikrofonAnWennNoetig()

        setContent {
            val themeModus by container.einstellungen.themeModus.collectAsState()
            val gesperrt by container.appSperre.gesperrt.collectAsState()
            val fenster = calculateWindowSizeClass(this)

            ClaudeKompassTheme(modus = themeModus) {
                val fabrik = KompassViewModelFactory(container)
                KompassApp(
                    referenz = viewModel(factory = fabrik),
                    chat = viewModel(factory = fabrik),
                    einstellungen = viewModel(factory = fabrik),
                    diktat = viewModel(factory = fabrik),
                    gesperrt = gesperrt,
                    beiEntsperren = ::entsperre,
                    beiExport = { exportZiel.launch(Sicherung.baueDateiname(zeitstempel())) },
                    beiImport = { importQuelle.launch(arrayOf("application/json", "*/*")) },
                    beiLogAnsehen = ::zeigeLog,
                    // Ab mittlerer Breite ist Platz für zwei Spalten. Auf dem Cover-Display des
                    // Fold ist das nicht der Fall — dort wird die Gesprächsliste überlagert.
                    breitGenugFuerZweiSpalten =
                    fenster.widthSizeClass != WindowWidthSizeClass.Compact,
                    themeModus = themeModus,
                    beiThemeWechsel = {
                        container.einstellungen.setzeThemeModus(themeModus.naechster())
                    },
                )
            }
        }
    }

    private fun entsperre() {
        container.appSperre.frageAb(
            activity = this,
            beiErfolg = { },
            beiFehler = { meldung -> zeige(meldung) },
        )
    }

    private fun frageMikrofonAnWennNoetig() {
        val hat = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hat) mikrofonErlaubnis.launch(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hatMitteilungen = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hatMitteilungen) {
                // Ohne eigene Abfrage: Die App braucht Mitteilungen nicht zwingend, und zwei
                // Abfragen direkt beim ersten Start sind eine Zumutung.
                KompassLog.debug("MainActivity", "onCreate", "Mitteilungen sind nicht erlaubt")
            }
        }
    }

    private fun schreibeSicherung(ziel: Uri) {
        lifecycleScope.launch {
            try {
                val inhalt = withContext(Dispatchers.IO) {
                    val eintraege = container.repository.ladeKomplett()
                    // Die Flüsse werden einmalig ausgelesen; für eine Momentaufnahme genügt das.
                    val fragenListe = container.repository.beobachteAlleFragen().first()
                    val sitzungen = container.repository.beobachteSitzungen().first()
                    val nachrichten = sitzungen.flatMap { container.repository.ladeNachrichten(it.id) }
                    Sicherung.schreibe(eintraege, fragenListe, sitzungen, nachrichten, zeitstempel())
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(ziel)?.use { strom ->
                        strom.write(inhalt.toByteArray(Charsets.UTF_8))
                    } ?: throw SicherungsFehler("Die Datei liess sich nicht zum Schreiben öffnen.")
                }
                zeige("Sicherung geschrieben.")
                KompassLog.info("MainActivity", "schreibeSicherung", "Sicherung geschrieben")
            } catch (fehler: Exception) {
                zeige(fehler.message ?: "Die Sicherung ist fehlgeschlagen.")
                KompassLog.error(
                    "MainActivity",
                    "schreibeSicherung",
                    "Sicherung fehlgeschlagen",
                    mapOf("grund" to fehler.message),
                )
            }
        }
    }

    private fun leseSicherung(quelle: Uri) {
        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(quelle)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw SicherungsFehler("Die Datei liess sich nicht öffnen.")
                }
                val (vorschau, json) = Sicherung.lies(text)
                // Vorschau vor dem Einspielen (Referenz, Baustein J.1). Zusammengeführt wird,
                // nicht ersetzt: Vorhandenes bleibt in jedem Fall erhalten.
                zeige(
                    "Sicherung vom ${vorschau.erstelltAm}: ${vorschau.fragen} Fragen, " +
                        "${vorschau.sitzungen} Gespräche, ${vorschau.eintraege} vertiefte " +
                        "Erklärungen werden ergänzt.",
                )
                withContext(Dispatchers.IO) { spieleEin(json) }
                zeige("Sicherung eingespielt.")
            } catch (fehler: Exception) {
                zeige(fehler.message ?: "Die Sicherung liess sich nicht einlesen.")
                KompassLog.error(
                    "MainActivity",
                    "leseSicherung",
                    "Einspielen fehlgeschlagen",
                    mapOf("grund" to fehler.message),
                )
            }
        }
    }

    private suspend fun spieleEin(json: JSONObject) {
        val repository = container.repository

        json.optJSONArray("eintraege")?.let { feld ->
            for (index in 0 until feld.length()) {
                val eintrag = feld.optJSONObject(index) ?: continue
                val id = eintrag.optString("id")
                val text = eintrag.optString("erklaerung")
                if (id.isNotBlank() && text.isNotBlank()) {
                    repository.vertiefeErklaerung(id, text)
                }
            }
        }

        json.optJSONArray("fragen")?.let { feld ->
            for (index in 0 until feld.length()) {
                val frage = feld.optJSONObject(index) ?: continue
                val eintragId = frage.optString("eintragId")
                val frageText = frage.optString("frage")
                if (eintragId.isBlank() || frageText.isBlank()) continue
                // Nur zu Einträgen, die es hier gibt — sonst würde der Fremdschlüssel greifen.
                if (repository.ladeEintrag(eintragId) == null) continue
                val id = repository.starteFrage(eintragId, frageText)
                repository.beendeFrage(id, frage.optString("antwort"))
            }
        }

        json.optJSONArray("sitzungen")?.let { feld ->
            for (index in 0 until feld.length()) {
                val sitzung = feld.optJSONObject(index) ?: continue
                val id = repository.legeSitzung(sitzung.optString("titel").ifBlank { "Eingespielt" })
                val nachrichten = sitzung.optJSONArray("nachrichten") ?: continue
                for (nummer in 0 until nachrichten.length()) {
                    val nachricht = nachrichten.optJSONObject(nummer) ?: continue
                    repository.fuegeNachrichtEin(
                        id,
                        nachricht.optString("rolle"),
                        nachricht.optString("text"),
                    )
                }
            }
        }
        repository.baueSuchIndexNeu()
    }

    private fun zeigeLog() {
        val pfad = KompassLog.path
        if (pfad == null) {
            zeige("Es gibt noch kein Protokoll.")
            return
        }
        zeige("Protokoll: $pfad")
        // Der Pfad steht zusätzlich im Systemprotokoll — von dort lässt er sich kopieren.
        KompassLog.info("MainActivity", "zeigeLog", "Protokoll angefragt", mapOf("pfad" to pfad))
    }

    private fun zeige(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun zeitstempel(): String =
        SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.GERMANY).format(Date())

    override fun onDestroy() {
        if (isFinishing) container.vorlesen.stoppe()
        super.onDestroy()
    }
}

/** Erzeugt die Modelle mit dem Container — ohne Rahmenwerk, weil es nur vier sind. */
class KompassViewModelFactory(private val container: KompassContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(de.frank.claudekompass.vm.ReferenzViewModel::class.java) ->
            de.frank.claudekompass.vm.ReferenzViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.claudekompass.vm.ChatViewModel::class.java) ->
            de.frank.claudekompass.vm.ChatViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.claudekompass.vm.EinstellungenViewModel::class.java) ->
            de.frank.claudekompass.vm.EinstellungenViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.claudekompass.vm.DiktatViewModel::class.java) ->
            de.frank.claudekompass.vm.DiktatViewModel(container) as T
        else -> throw IllegalArgumentException("Unbekanntes Modell: ${modelClass.name}")
    }
}
