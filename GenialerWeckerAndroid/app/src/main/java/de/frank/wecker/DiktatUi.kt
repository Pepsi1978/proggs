@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
    // Erkennung wählen: beim ersten Einsprechen automatisch, danach über die kleine Zeile unter den Knöpfen.
    var auswahlOffen by remember { mutableStateOf(false) }
    var nachAuswahlStarten by remember { mutableStateOf(false) }
    var erkennungStand by remember { mutableIntStateOf(0) }
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
            else -> GoldKnopf("Einsprechen", {
                if (!TurboModell.auswahlGetroffen(context)) { nachAuswahlStarten = true; auswahlOffen = true } else mitFreigabe(::loslegen)
            }, symbol = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) })
        }
        vorlesen()
    }
    if (whisper.hoertZu) Pegel(whisper.pegel, whisper.sekunden)
    if (!whisper.hoertZu && !whisper.erkennt && WhisperModell.bereit(context)) {
        val premium = remember(erkennungStand) { TurboModell.aktiv(context) }
        Text("Erkennung: ${if (premium) "Premium" else "Standard"} · ändern",
            Modifier.clip(RoundedCornerShape(8.dp)).clickable { nachAuswahlStarten = false; auswahlOffen = true }.padding(vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = LocalGold.current.primaer)
    }
    if (auswahlOffen) ErkennungDialog { starten ->
        auswahlOffen = false; erkennungStand++
        if (starten && nachAuswahlStarten) mitFreigabe(::loslegen)
        nachAuswahlStarten = false
    }
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

/**
 * Auswahl der Spracherkennung: Standard (eingebaut) oder Premium (Whisper Large V3 Turbo, Download).
 * Der Download läuft im Hintergrund weiter und setzt nach einer Unterbrechung an derselben Stelle fort.
 * [schliessen] meldet, ob danach gleich aufgenommen werden soll.
 */
@Composable
internal fun ErkennungDialog(schliessen: (starten: Boolean) -> Unit) {
    val context = LocalContext.current
    val gold = LocalGold.current
    var turbo by remember { mutableStateOf(TurboModell.gewuenscht(context)) }
    val arbeiten by remember { TurboModell.laeuftFlow(context) }.collectAsStateWithLifecycle(emptyList())
    val laeuft = arbeiten.any { !it.state.isFinished }
    var bytes by remember { mutableLongStateOf(TurboModell.bytesDa(context)) }
    var geladen by remember { mutableStateOf(TurboModell.geladen(context)) }
    LaunchedEffect(laeuft) {
        while (true) { bytes = TurboModell.bytesDa(context); geladen = TurboModell.geladen(context); if (!laeuft) break; kotlinx.coroutines.delay(500) }
    }
    fun fertig() { TurboModell.waehlen(context, turbo); schliessen(true) }
    de.frank.wecker.design.DesignDialog(
        titel = "Erkennung wählen",
        aufSchliessen = { TurboModell.waehlen(context, turbo); schliessen(false) },
        bestaetigung = { StillerKnopf("Fertig", ::fertig) },
        inhalt = {
            ErkennungKarte(!turbo || !geladen, "Standard", "Eingebaut · sofort bereit") { turbo = false }
            Spacer(Modifier.height(10.dp))
            ErkennungKarte(turbo && geladen, "Premium · ${TurboModell.NAME}",
                "Etwa halb so viele Fehler, versteht Namen und Fachwörter besser.") { if (geladen) turbo = true }
            Spacer(Modifier.height(8.dp))
            when {
                geladen -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Geladen · ${TurboModell.groesseText}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                    StillerKnopf("Löschen", { TurboModell.loeschen(context); geladen = false; turbo = false; bytes = 0 })
                }
                laeuft -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val anteil = (bytes.toFloat() / TurboModell.GESAMT).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(gold.textGedaempft.copy(alpha = .2f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(anteil).clip(RoundedCornerShape(50))
                            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = .6f), gold.primaer))))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${(anteil * 100).toInt()} % · ${bytes / 1_000_000} von ${TurboModell.GESAMT / 1_000_000} MB\nLädt im Hintergrund weiter, auch wenn du die App schließt. Danach automatisch aktiv.",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                        StillerKnopf("Abbrechen", { TurboModell.abbrechen(context); bytes = TurboModell.bytesDa(context) })
                    }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nur per Download · ${TurboModell.groesseText}" + if (bytes > 0) " · ${bytes / 1_000_000} MB schon geladen" else "",
                        style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                    GoldKnopf(if (bytes > 0) "Download fortsetzen" else "Herunterladen · ${TurboModell.groesseText}",
                        { TurboModell.starteDownload(context); turbo = true }, Modifier.fillMaxWidth())
                }
            }
        },
    )
}

@Composable
private fun ErkennungKarte(aktiv: Boolean, titel: String, unter: String, waehlen: () -> Unit) {
    val gold = LocalGold.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (aktiv) gold.primaer.copy(alpha = .14f) else androidx.compose.ui.graphics.Color.Transparent)
        .border(1.dp, if (aktiv) gold.primaer else gold.rahmen, RoundedCornerShape(14.dp))
        .clickable(onClick = waehlen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.RadioButton(aktiv, null)
        Column(Modifier.weight(1f)) {
            Text(titel, style = MaterialTheme.typography.titleSmall, color = if (aktiv) gold.primaer else gold.textPrimaer)
            Text(unter, style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        }
    }
}
