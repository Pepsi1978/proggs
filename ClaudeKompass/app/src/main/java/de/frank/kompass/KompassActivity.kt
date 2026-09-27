package de.frank.kompass

import android.Manifest
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
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.kompass.observability.KompassLog
import de.frank.kompass.ui.KompassApp
import de.frank.kompass.ui.theme.KompassTheme

/**
 * Der einzige Bildschirm der App.
 *
 * Erbt von [FragmentActivity], weil die biometrische Abfrage das verlangt — mit einer reinen
 * `ComponentActivity` liesse sich die App-Sperre nicht anzeigen.
 */
open class KompassActivity : FragmentActivity() {

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

    /**
     * Ein Ordner statt einer einzelnen Datei: Nur so kann die App die vorige Sicherung stehen
     * lassen und alles Ältere selbst wegräumen — ohne bei jedem Sichern nachzufragen.
     */
    private val ordnerWahl = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { ordner -> einstellungenModell.sicherungsOrdnerGewaehlt(ordner, ::oeffneOrdnerWahl) }

    private val sicherungsWahl = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { ergebnis ->
        if (ergebnis.resultCode == RESULT_OK) {
            ergebnis.data?.data?.let { quelle -> einstellungenModell.stelleWiederHer(quelle) }
        }
    }

    /**
     * Das Einstellungs-Modell, an das die Launcher ihr Ergebnis geben.
     *
     * Bewusst über den ViewModelProvider und nicht aus der Komposition heraus gemerkt: Der
     * Dateiwähler kann diesen Vorgang verdrängen, und nach dem Neustart trifft sein Ergebnis
     * ein, BEVOR das erste Mal gezeichnet wurde. Ein aus der Komposition gemerktes Modell wäre
     * dann noch leer — der gewählte Ordner wäre still verloren und man hätte die Wahl
     * scheinbar grundlos noch einmal zu treffen. Der Schlüssel ist derselbe, den
     * `viewModel(factory = fabrik)` vergibt, also ist es dieselbe Instanz.
     */
    private val einstellungenModell: de.frank.kompass.vm.EinstellungenViewModel by lazy {
        ViewModelProvider(
            this,
            KompassViewModelFactory(container),
        )[de.frank.kompass.vm.EinstellungenViewModel::class.java]
    }

    private fun oeffneOrdnerWahl() {
        runCatching { ordnerWahl.launch(einstellungenModell.sicherungsOrdnerUri) }
            .onFailure { zeige("Die Ordnerauswahl liess sich nicht öffnen: ${it.message}") }
    }

    /** Eine bestimmte Sicherungsdatei wählen — der Wähler startet im gemerkten Ordner. */
    private fun oeffneSicherungsWahl() {
        runCatching {
            val absicht = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                einstellungenModell.sicherungsOrdnerUri?.let { ordner ->
                    putExtra(
                        android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                        android.provider.DocumentsContract.buildDocumentUriUsingTree(
                            ordner,
                            android.provider.DocumentsContract.getTreeDocumentId(ordner),
                        ),
                    )
                }
            }
            sicherungsWahl.launch(absicht)
        }.onFailure { zeige("Die Dateiauswahl liess sich nicht öffnen: ${it.message}") }
    }

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

            KompassTheme(modus = themeModus) {
                val fabrik = KompassViewModelFactory(container)
                KompassApp(
                    referenz = viewModel(factory = fabrik),
                    chat = viewModel(factory = fabrik),
                    einstellungen = viewModel(factory = fabrik),
                    diktat = viewModel(factory = fabrik),
                    gesperrt = gesperrt,
                    beiEntsperren = ::entsperre,
                    beiOrdnerWaehlen = ::oeffneOrdnerWahl,
                    beiSicherungWaehlen = ::oeffneSicherungsWahl,
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

    override fun onDestroy() {
        if (isFinishing) container.vorlesen.stoppe()
        super.onDestroy()
    }
}

/** Erzeugt die Modelle mit dem Container — ohne Rahmenwerk, weil es nur vier sind. */
class KompassViewModelFactory(private val container: KompassContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(de.frank.kompass.vm.ReferenzViewModel::class.java) ->
            de.frank.kompass.vm.ReferenzViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.kompass.vm.ChatViewModel::class.java) ->
            de.frank.kompass.vm.ChatViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.kompass.vm.EinstellungenViewModel::class.java) ->
            de.frank.kompass.vm.EinstellungenViewModel(container) as T
        modelClass.isAssignableFrom(de.frank.kompass.vm.DiktatViewModel::class.java) ->
            de.frank.kompass.vm.DiktatViewModel(container) as T
        else -> throw IllegalArgumentException("Unbekanntes Modell: ${modelClass.name}")
    }
}
