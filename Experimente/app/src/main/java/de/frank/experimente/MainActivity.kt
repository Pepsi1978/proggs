package de.frank.experimente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.ExperimenteTheme
import de.frank.experimente.ui.theme.LocalAppColors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val einstellungen = (application as ExperimenteApp).einstellungen

        setContent {
            val erscheinungId by einstellungen.erscheinungFlow.collectAsState()
            ExperimenteTheme(erscheinung = Erscheinung.ausId(erscheinungId)) {
                val farben = LocalAppColors.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(farben.grund),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Experimente",
                        style = AppTypo.bildschirmtitel,
                        color = farben.text,
                    )
                }
            }
        }
    }
}
