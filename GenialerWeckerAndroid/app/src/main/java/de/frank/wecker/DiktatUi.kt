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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.LocalSemantisch

/**
 * Eine kompakte Zeile direkt unter dem Vorlesetext: links „Diktieren“, rechts [vorlesen].
 * Aufgenommen wird nur nach Tippen und nur auf dem Gerät. Das fertig Erkannte wird sofort an den
 * aktuellen Entwurf angehängt ([anfuegen]) – nie überschrieben. Die Sprache folgt der Gerätesprache.
 */
@Composable
fun DiktatUndVorlesen(sprache: DiktatSprache, anfuegen: (String) -> Unit, vorlesen: @Composable () -> Unit) {
    val context = LocalContext.current
    val diktat = remember { OfflineDiktat(context.applicationContext) }
    // Die Sprache gehört zum Wecker; ein Wechsel beendet eine laufende Aufnahme in der alten Sprache.
    LaunchedEffect(sprache) { if (diktat.hoertZu) diktat.zuruecksetzen() }
    val zustand = diktat.zustand
    val aktuellesAnfuegen by rememberUpdatedState(anfuegen)

    // Fertiges Ergebnis sofort übernehmen: angehängt an den aktuellsten Entwurf, ohne Zwischenblase.
    LaunchedEffect(zustand) {
        if (zustand is DiktatZustand.Ergebnis) { aktuellesAnfuegen(zustand.text); diktat.zuruecksetzen() }
    }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis -> if (ereignis == Lifecycle.Event.ON_STOP) diktat.abbrechen() }
        lebenszyklus.addObserver(beobachter)
        onDispose {
            lebenszyklus.removeObserver(beobachter)
            diktat.abbrechen()
            // Beim Schließen des Editors: schon Gehörtes nicht wegwerfen, sondern noch anhängen.
            (diktat.zustand as? DiktatZustand.Ergebnis)?.let { aktuellesAnfuegen(it.text) }
            diktat.allesFreigeben()
        }
    }
    val mikrofon = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
        if (erlaubt) diktat.starten(sprache)
        else diktat.zeigeHinweis("Ohne Mikrofonfreigabe kein Diktat. Du kannst den Text jederzeit tippen.")
    }

    // Kurze Beschriftungen passen auf S24-Breite nebeneinander; bei großer Schrift bricht der rechte Knopf um statt überzulaufen.
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (diktat.hoertZu) GoldKnopf("■ Fertig", { diktat.beenden() }, hauptKnopf = true, beschreibung = "Diktat beenden und Text einfügen")
        else GoldKnopf("Diktieren", {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) diktat.starten(sprache)
            else mikrofon.launch(Manifest.permission.RECORD_AUDIO)
        }, symbol = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) })
        vorlesen()
    }
    // Knapper Status direkt unter den Knöpfen; kein eigenes Panel.
    val status: Pair<String, Boolean>? = when (zustand) {
        DiktatZustand.Pruefe -> "Sprachpaket ${sprache.anzeige} wird geprüft …" to false
        is DiktatZustand.Hoert -> ("● Hört zu (${sprache.anzeige})" + if (zustand.zwischentext.isNotBlank()) ": „${zustand.zwischentext}“" else " …") to false
        is DiktatZustand.Hinweis -> zustand.meldung to true
        else -> null
    }
    status?.let { (text, warnung) ->
        Text(text, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis,
            color = if (warnung) LocalSemantisch.current.warnung else if (zustand is DiktatZustand.Hoert) LocalSemantisch.current.fehler else LocalGold.current.textGedaempft)
    }
    if ((zustand as? DiktatZustand.Hinweis)?.sprachpaketLadbar == true)
        StillerKnopf("Sprachpaket ${sprache.anzeige} laden", { diktat.sprachpaketLaden() })
}
