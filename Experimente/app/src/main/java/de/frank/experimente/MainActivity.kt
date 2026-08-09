package de.frank.experimente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.ExperimenteNavigation
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.ExperimenteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val einstellungen = (application as ExperimenteApp).einstellungen

        setContent {
            val erscheinungId by einstellungen.erscheinungFlow.collectAsState()
            ExperimenteTheme(erscheinung = Erscheinung.ausId(erscheinungId)) {
                val vm: AppViewModel = viewModel()
                ExperimenteNavigation(vm)
            }
        }
    }
}
