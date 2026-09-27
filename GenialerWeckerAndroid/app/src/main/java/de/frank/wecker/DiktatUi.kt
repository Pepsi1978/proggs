@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.LocalSemantisch

/**
 * Eine kompakte Zeile direkt unter dem Vorlesetext: links „Diktieren“, rechts [vorlesen].
 * Erkannt wird mit dem eingebauten Whisper-Modell direkt auf dem Gerät, ohne Internet.
 * Das fertig Erkannte wird sofort an den aktuellen Entwurf angehängt ([anfuegen]) – nie überschrieben.
 */
@Composable
fun DiktatUndVorlesen(sprache: DiktatSprache, anfuegen: (String) -> Unit, vorlesen: @Composable () -> Unit) {
    val context = LocalContext.current
    val diktat = remember { OfflineDiktat(context.applicationContext) }
    val whisper = remember { WhisperDiktat(context.applicationContext) }

    // Die Sprache gehört zum Wecker; ein Wechsel beendet eine laufende Aufnahme in der alten Sprache.
    LaunchedEffect(sprache) { if (diktat.hoertZu) diktat.zuruecksetzen(); if (whisper.hoertZu) whisper.zuruecksetzen() }
    val aktuellesAnfuegen by rememberUpdatedState(anfuegen)

    // Fertiges Ergebnis sofort übernehmen: angehängt an den aktuellsten Entwurf, ohne Zwischenblase.
    LaunchedEffect(diktat.zustand) {
        (diktat.zustand as? DiktatZustand.Ergebnis)?.let { aktuellesAnfuegen(it.text); diktat.zuruecksetzen() }
    }
    LaunchedEffect(whisper.zustand) {
        (whisper.zustand as? DiktatZustand.Ergebnis)?.let { aktuellesAnfuegen(it.text); whisper.zuruecksetzen() }
    }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis -> if (ereignis == Lifecycle.Event.ON_STOP) { diktat.abbrechen(); whisper.abbrechen() } }
        lebenszyklus.addObserver(beobachter)
        onDispose {
            lebenszyklus.removeObserver(beobachter)
            diktat.abbrechen()
            // Beim Schließen des Editors: schon Gehörtes nicht wegwerfen, sondern noch anhängen.
            (diktat.zustand as? DiktatZustand.Ergebnis)?.let { aktuellesAnfuegen(it.text) }
            diktat.allesFreigeben()
            whisper.freigeben()
        }
    }
    fun loslegen() {
        // Whisper ist fest eingebaut; nur ein Build ohne Modell nutzt das Android-Diktat.
        if (WhisperModell.bereit(context)) whisper.starten(sprache.code) else diktat.starten(sprache)
    }
    val mikrofon = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
        if (erlaubt) loslegen()
        else whisper.zeigeHinweis("Ohne Mikrofonfreigabe kein Diktat. Du kannst den Text jederzeit tippen.")
    }
    fun mitFreigabe(aktion: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) aktion()
        else mikrofon.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Kurze Beschriftungen passen auf S24-Breite nebeneinander; bei großer Schrift bricht der rechte Knopf um statt überzulaufen.
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            whisper.hoertZu -> GoldKnopf("■ Fertig", { whisper.beenden() }, hauptKnopf = true, beschreibung = "Diktat beenden und Text einfügen")
            whisper.erkennt -> GoldKnopf("Erkenne …", {}, aktiviert = false, laedt = true)
            diktat.hoertZu -> GoldKnopf("■ Fertig", { diktat.beenden() }, hauptKnopf = true, beschreibung = "Diktat beenden und Text einfügen")
            else -> GoldKnopf("Diktieren", { mitFreigabe(::loslegen) }, symbol = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) })
        }
        vorlesen()
    }
    if (whisper.hoertZu) Pegel(whisper.pegel, whisper.sekunden)
    // Knapper Status direkt unter den Knöpfen; kein eigenes Panel.
    val status: Pair<String, Boolean>? = when {
        whisper.erkennt -> "Wird auf dem Handy erkannt …" to false
        whisper.zustand is DiktatZustand.Hinweis -> (whisper.zustand as DiktatZustand.Hinweis).meldung to true
        else -> when (val zustand = diktat.zustand) {
            DiktatZustand.Pruefe -> "Sprachpaket ${sprache.anzeige} wird geprüft …" to false
            is DiktatZustand.Hoert -> ("● Hört zu (${sprache.anzeige})" + if (zustand.zwischentext.isNotBlank()) ": „${zustand.zwischentext}“" else " …") to false
            is DiktatZustand.Hinweis -> zustand.meldung to true
            else -> null
        }
    }
    status?.let { (text, warnung) ->
        Text(text, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis,
            color = if (warnung) LocalSemantisch.current.warnung else LocalGold.current.textGedaempft)
    }
    if ((diktat.zustand as? DiktatZustand.Hinweis)?.sprachpaketLadbar == true)
        StillerKnopf("Sprachpaket ${sprache.anzeige} laden", { diktat.sprachpaketLaden() })
}

/** Aufnahmepegel als ruhiger Balken mit Laufzeit – man sieht, dass das Handy zuhört. */
@Composable
private fun Pegel(pegel: Float, sekunden: Int) {
    val gold = LocalGold.current
    val weich by animateFloatAsState(pegel, label = "pegel")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("● ${sekunden / 60}:${"%02d".format(sekunden % 60)}", style = MaterialTheme.typography.labelLarge, color = LocalSemantisch.current.fehler)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(gold.textGedaempft.copy(alpha = .2f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((.04f + weich * .96f).coerceIn(0f, 1f)).clip(RoundedCornerShape(50))
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = .6f), gold.primaer))))
        }
    }
}
