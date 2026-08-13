package de.frank.experimente

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Navigation
import de.frank.experimente.ui.theme.ExperimenteTheme

class MainActivity : ComponentActivity() {

    private val modell: AppViewModel by viewModels()

    override fun onCreate(zustand: Bundle?) {
        super.onCreate(zustand)
        enableEdgeToEdge()

        werteAbsichtAus(intent)

        setContent {
            val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
            val effektstufe by modell.effektstufe.collectAsStateWithLifecycle()

            // F-01 Schritt 1: die Mikrofon-Erlaubnis wird beim ersten Druck auf einen
            // Sprechknopf angefragt. Die Frage kann nur eine Activity stellen, deshalb
            // steht der Starter hier und nicht im Modell.
            val fragtMikrofon by modell.fragtMikrofon.collectAsStateWithLifecycle()
            val mikrofonfrage = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { erlaubt -> modell.mikrofonBeantwortet(erlaubt) }
            LaunchedEffect(fragtMikrofon) {
                if (fragtMikrofon) mikrofonfrage.launch(Manifest.permission.RECORD_AUDIO)
            }

            ExperimenteTheme(erscheinung = erscheinung, effektstufe = effektstufe) {
                Navigation(modell)
            }
        }
    }

    /**
     * Die Erinnerung öffnet die App über `FLAG_ACTIVITY_CLEAR_TOP`. Läuft sie bereits, gibt
     * es dabei **kein** `onCreate` — die Absicht kommt hier an. Ohne diese Stelle blieb der
     * Druck auf die Abend-Erinnerung wirkungslos, sobald die App noch im Speicher lag.
     */
    override fun onNewIntent(absicht: Intent) {
        super.onNewIntent(absicht)
        setIntent(absicht)
        werteAbsichtAus(absicht)
    }

    /**
     * Kam die App über die Abend-Erinnerung, öffnet sie B-01 im Abend-Zustand (F-25).
     *
     * Das Extra wird danach entfernt: sonst sprang die App bei jeder Neuerzeugung der
     * Activity — Systemtheme-Wechsel, Drehen, Aufklappen des Foldables — zurück in den
     * Abend, auch mitten am Tag.
     */
    private fun werteAbsichtAus(absicht: Intent?) {
        if (absicht?.getBooleanExtra(EXTRA_ABEND, false) == true) {
            absicht.removeExtra(EXTRA_ABEND)
            modell.zeigeAbend()
        }
    }

    override fun onStart() {
        super.onStart()
        // Hat sich der Kalendertag geändert, während die App im Hintergrund lag, wird alles
        // Tagesbezogene nachgezogen — sonst schriebe sie weiter in den Vortag.
        modell.beimZurueckkehren()
    }

    override fun onStop() {
        super.onStop()
        // §6: Beim Wechsel in den Hintergrund endet eine laufende Aufnahme, eine laufende
        // Wiedergabe wird gestoppt. Laufende KI-Anfragen laufen zu Ende.
        modell.beimVerlassen()
    }

    companion object {
        const val EXTRA_ABEND = "abend"
    }
}
