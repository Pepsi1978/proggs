@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import de.frank.wecker.design.LocalGestalt

/**
 * Offline-Diktat für den Vorlesetext. Aufgenommen wird nur nach ausdrücklichem Tippen; das Ergebnis
 * wird nie ungefragt in den Text geschrieben, sondern wartet editierbar auf „Anfügen“ oder „Verwerfen“.
 * Verlässt man den Editor oder die App, endet die Aufnahme sofort.
 */
@Composable
fun DiktatBereich(anfuegen: (String) -> Unit) {
    val context = LocalContext.current
    val diktat = remember { OfflineDiktat(context.applicationContext) }
    var sprache by rememberSaveable { mutableStateOf(DiktatSprache.DE) }
    val zustand = diktat.zustand
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis -> if (ereignis == Lifecycle.Event.ON_STOP) diktat.abbrechen() }
        lebenszyklus.addObserver(beobachter)
        onDispose { lebenszyklus.removeObserver(beobachter); diktat.abbrechen(); diktat.allesFreigeben() }
    }
    val mikrofon = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
        if (erlaubt) diktat.starten(sprache)
        else hinweisOhneMikrofon(diktat)
    }
    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) diktat.starten(sprache)
        else mikrofon.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Diktieren (offline)", style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiktatSprache.entries.forEach { eintrag ->
                Chip3D(eintrag == sprache, { if (!diktat.hoertZu) sprache = eintrag }, eintrag.anzeige)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (diktat.hoertZu) {
                GoldKnopf("■ Diktat beenden", { diktat.beenden() }, hauptKnopf = true)
                StillerKnopf("Abbrechen", { diktat.zuruecksetzen() })
            } else {
                GoldKnopf("Diktieren", { start() }, aktiviert = zustand !is DiktatZustand.Ergebnis,
                    symbol = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) })
            }
        }
        when (zustand) {
            DiktatZustand.Bereit -> Text("Tippe auf „Diktieren“ und sprich. Die Erkennung läuft ausschließlich auf diesem Gerät; der Text wird erst nach deiner Bestätigung übernommen.",
                style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            DiktatZustand.Pruefe -> Text("Offline-Sprachpaket ${sprache.anzeige} wird geprüft …", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            is DiktatZustand.Hoert -> Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text("● Aufnahme läuft (${sprache.anzeige}) …", style = MaterialTheme.typography.bodyMedium, color = LocalSemantisch.current.fehler)
                if (zustand.zwischentext.isNotBlank()) Text("„${zustand.zwischentext}“", style = MaterialTheme.typography.bodyMedium)
            }
            is DiktatZustand.Ergebnis -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eingabefeld(zustand.text, diktat::ergebnisAendern, "Erkannter Text – vor dem Übernehmen bearbeitbar",
                        Modifier.fillMaxWidth().heightIn(min = 96.dp), einzeilig = false)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldKnopf("Am Ende anfügen", { val text = zustand.text.trim(); if (text.isNotEmpty()) anfuegen(text); diktat.zuruecksetzen() },
                            aktiviert = zustand.text.isNotBlank())
                        StillerKnopf("Verwerfen", { diktat.zuruecksetzen() })
                    }
                }
            }
            is DiktatZustand.Hinweis -> Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(zustand.meldung, style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
                }
                if (zustand.sprachpaketLadbar) GoldKnopf("Sprachpaket ${sprache.anzeige} über Android laden", { diktat.sprachpaketLaden() })
            }
        }
    }
}

private fun hinweisOhneMikrofon(diktat: OfflineDiktat) = diktat.zeigeHinweis(
    "Ohne Mikrofonfreigabe ist kein Diktat möglich. Du kannst den Text jederzeit tippen; die Freigabe lässt sich in den App-Einstellungen erteilen.")
