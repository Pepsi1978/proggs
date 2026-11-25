package de.frank.wecker

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Sonnenstand
import de.frank.genialeideen.ui.theme.glanzBogen
import de.frank.genialeideen.ui.theme.heller
import de.frank.genialeideen.ui.theme.dunkler
import de.frank.genialeideen.ui.theme.tiefenSchatten
import de.frank.wecker.design.Design
import de.frank.wecker.design.Ebene
import de.frank.wecker.design.LocalGestalt
import de.frank.wecker.design.MaterialFlaeche
import de.frank.wecker.design.WeckerTheme

/**
 * Vollbild der Schlafenszeit-Erinnerung in der Art „Wecker“: folgt dem gewählten Design und
 * Materialsystem der App, mit 3D-Mondmedaillon, lesbarem Ansagetext sowie den Aktionen
 * „Verstanden“ (beendet) und „Nochmal anhören“ (wiederholt).
 */
class SchlafAnsageActivity : ComponentActivity() {
    private var settings: SecureSettings? = null

    private fun leseDarstellung(): Triple<String, Design, String> {
        if (!getSystemService(android.os.UserManager::class.java).isUserUnlocked) {
            return Triple("dark", Design.SCHLICHT, Ausrichtung.AUTOMATISCH)
        }
        val offen = settings ?: runCatching { SecureSettings(this) }.getOrNull()
        if (offen == null || !offen.verfuegbar) {
            runCatching { offen?.close() }
            settings = null
            return Triple("dark", Design.SCHLICHT, Ausrichtung.AUTOMATISCH)
        }
        settings = offen
        return Triple(
            runCatching { offen.theme }.getOrDefault("dark"),
            runCatching { Design.von(offen.design) }.getOrDefault(Design.SCHLICHT),
            runCatching { offen.ausrichtung }.getOrDefault(Ausrichtung.AUTOMATISCH),
        )
    }

    override fun onDestroy() {
        runCatching { settings?.close() }
        settings = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (themeStart, design, ausrichtung) = leseDarstellung()
        val dunkelStart = themeStart == "dark" || (themeStart == "system" && Sonnenstand.istDunkel())
        val rand = if (dunkelStart) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = rand, navigationBarStyle = rand)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val laufend by SchlafAnsageService.text.collectAsStateWithLifecycle()
            var gemerkt by rememberSaveable { mutableStateOf("") }
            LaunchedEffect(laufend) { if (laufend.isNotBlank()) gemerkt = laufend }
            val wirksam = wirksamesTheme(themeStart)
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = wirksam != "dark"
                    isAppearanceLightNavigationBars = wirksam != "dark"
                }
            }
            WeckerTheme(wirksam, design, ausrichtung) {
                val gold = LocalGold.current
                Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
                    LocalGestalt.current.Hintergrund(Modifier.fillMaxSize())
                    Column(
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .systemBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MaterialFlaeche(Modifier.fillMaxWidth(), ebene = Ebene.HERO) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // Plastisches 3D-Mondmedaillon im oberen Bereich
                                Box(
                                    Modifier.size(88.dp)
                                        .tiefenSchatten(gold.primaer, Hoehe.karteErhoeht, CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(gold.primaer.heller(0.20f), gold.primaer, gold.primaer.dunkler(0.24f)),
                                            ),
                                        )
                                        .glanzBogen(0.38f)
                                        .border(1.25f.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Bedtime, null, tint = gold.aufPrimaer, modifier = Modifier.size(44.dp))
                                }
                                Text(
                                    "SCHLAFENSZEIT",
                                    color = gold.primaer,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        letterSpacing = 2.6.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    gemerkt.removePrefix("Schlafenszeit-Erinnerung. ")
                                        .ifBlank { "Zeit, dich in Ruhe auf den Schlaf vorzubereiten." },
                                    color = gold.textPrimaer,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                                GoldKnopf(
                                    text = "Verstanden",
                                    aufTipp = { SchlafAnsage.stoppen(this@SchlafAnsageActivity); finish() },
                                    modifier = Modifier.fillMaxWidth(),
                                    hauptKnopf = true,
                                )
                                StillerKnopf(
                                    text = "Nochmal anhören",
                                    aufTipp = {
                                        if (gemerkt.isNotBlank()) {
                                            SchlafAnsage.abspielen(this@SchlafAnsageActivity, gemerkt, wecker = true, runden = 1)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    hervorgehoben = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
