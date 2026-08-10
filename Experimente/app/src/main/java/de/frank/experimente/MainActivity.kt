package de.frank.experimente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

        // Kam die App über die Abend-Erinnerung, öffnet sie B-01 im Abend-Zustand (F-25).
        if (intent?.getBooleanExtra(EXTRA_ABEND, false) == true) modell.zeigeAbend()

        setContent {
            val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
            ExperimenteTheme(erscheinung = erscheinung) {
                Navigation(modell)
            }
        }
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
