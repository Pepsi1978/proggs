package de.frank.wecker

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Vollbild der Schlafenszeit-Erinnerung in der Art „Wecker“: Text der Ansage, „Verstanden“ beendet, „Nochmal anhören“ wiederholt. */
class SchlafAnsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val laufend by SchlafAnsageService.text.collectAsStateWithLifecycle()
            var gemerkt by remember { mutableStateOf("") }
            LaunchedEffect(laufend) { if (laufend.isNotBlank()) gemerkt = laufend }
            val gold = Color(0xFFE2C27A)
            MaterialTheme(colorScheme = darkColorScheme(primary = gold, onPrimary = Color(0xFF1A1408))) {
                Column(Modifier.fillMaxSize().background(Color(0xFF0E1220)).padding(28.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Schlafenszeit", color = gold, fontSize = 40.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Text(gemerkt.removePrefix("Schlafenszeit-Erinnerung. ").ifBlank { "Zeit, dich auf den Schlaf vorzubereiten." },
                        color = Color(0xFFF1ECE0), fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(40.dp))
                    Button({ SchlafAnsage.stoppen(this@SchlafAnsageActivity); finish() }, Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gold)) { Text("Verstanden", fontSize = 22.sp) }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton({ if (gemerkt.isNotBlank()) SchlafAnsage.abspielen(this@SchlafAnsageActivity, gemerkt, wecker = true, runden = 1) },
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Nochmal anhören", color = gold, fontSize = 18.sp) }
                }
            }
        }
    }
}
