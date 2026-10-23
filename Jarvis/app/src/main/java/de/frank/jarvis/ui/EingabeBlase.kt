package de.frank.jarvis.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.data.Anhang
import de.frank.jarvis.faehigkeit.AnthropicGuthabenFaehigkeit
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Das Orange der Live-Blase (wie in der ChatGPT-App). */
val LIVE_ORANGE = Color(0xFFF0876A)

/** Ein Plugin für das Plus-Menü. Vorbereitet: Neue Plugins kommen hier in [Plugins.alle]. */
data class Plugin(val id: String, val titel: String, val beschreibung: String)

object Plugins {
    val alle: List<Plugin> = emptyList()
}

/**
 * Die Eingabeblase wie in der ChatGPT-App: oben das Textfeld („Mit Jarvis arbeiten“, die Tastatur kommt erst beim
 * Hineintippen), darunter Plus-Menü, Modell und Aufwand, das Diktier-Mikrofon und rechts die orange Live-Blase,
 * die zum Senden-Pfeil wird, sobald Text oder ein Anhang da ist. Während eines Live-Gesprächs zeigt sie dessen Leiste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EingabeBlase(vm: AppViewModel, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    var plusOffen by remember { mutableStateOf(false) }
    var pluginsOffen by remember { mutableStateOf(false) }
    var modellOffen by remember { mutableStateOf(false) }
    val modell = vm.einstellungen.modell
    // Kein Abfragen im Leerlauf: Hier nur der gespeicherte Stand; frisch geholt wird beim Start und Ende einer Aufgabe.
    LaunchedEffect(vm, modell.istAnthropic) {
        if (modell.istAnthropic) vm.anthropicGuthabenZeigen()
    }
    val guthabenStand = if (modell.istAnthropic) vm.anthropicGuthaben?.takeIf { it.hinweis == null } else null
    val guthabenUsd = guthabenStand?.restUsd
    // Unter 5 € (EZB-Tageskurs aus der Guthaben-Abfrage) rot, sonst grün; ohne Kurs bleibt es grün.
    val guthabenKnapp = guthabenUsd != null && guthabenStand?.kurs?.let { guthabenUsd * it < 5.0 } == true
    // Das Guthaben steht eine Schriftstufe kleiner und nicht fett vor dem Modellnamen, getrennt durch „ · “.
    fun mitGuthaben(name: String) = buildAnnotatedString {
        if (guthabenUsd != null) {
            val stil = SpanStyle(color = if (guthabenKnapp) f.gefahr else f.erfolg, fontWeight = FontWeight.Normal, fontSize = 0.87.em)
            withStyle(stil) { append("(${AnthropicGuthabenFaehigkeit.betrag(guthabenUsd)} $") }
            // Was die letzte Aufgabe gekostet hat, als roter Abzug direkt hinter dem Guthaben; ohne Abzug kein „−0,00 $“.
            vm.anthropicKosten?.takeIf { it > 0.0 }?.let { k ->
                withStyle(stil.copy(color = f.gefahr)) { append(" −${AnthropicGuthabenFaehigkeit.betrag(k)} $") }
            }
            withStyle(stil) { append(")") }
            append(" · ")
        }
        append(name)
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).glas(f, 28.dp, erhoeht = 0.8f, fuellung = f.flaecheStark)) {
        if (vm.liveAktiv) { LiveLeiste(vm); return@Column }

        if (vm.anhaenge.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.anhaenge.forEach { a ->
                    Box {
                        AnhangKachel(a, 64.dp)
                        Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).antippen { vm.anhangWeg(a) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Anhang entfernen", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Oben: das Textfeld. Kein automatischer Fokus, damit die Tastatur erst beim Hineintippen kommt; nur „Programmierer“
        // im Plus-Menü fokussiert es. Eigener Feldzustand, damit der Cursor nach Vorspann und Diktat am Ende steht.
        var feld by remember { mutableStateOf(TextFieldValue(vm.entwurf, TextRange(vm.entwurf.length))) }
        // Von außen geändert (Vorspann, Diktat, nach dem Senden geleert): dann der neue Text mit dem Cursor am Ende.
        val wert = if (feld.text == vm.entwurf) feld else TextFieldValue(vm.entwurf, TextRange(vm.entwurf.length))
        LaunchedEffect(vm.entwurf) { if (feld.text != vm.entwurf) feld = TextFieldValue(vm.entwurf, TextRange(vm.entwurf.length)) }
        val fokus = remember { FocusRequester() }
        val tastatur = LocalSoftwareKeyboardController.current
        LaunchedEffect(vm.entwurfFokus) {
            if (!vm.entwurfFokus) return@LaunchedEffect
            vm.entwurfFokus = false
            // Das Plus-Menü schließt gerade und hält den Fokus noch: einige Male versuchen, die Tastatur erst nach Erfolg.
            repeat(10) {
                delay(80)
                if (runCatching { fokus.requestFocus() }.isSuccess) { tastatur?.show(); return@LaunchedEffect }
            }
        }
        val platzhalter = when {
            vm.nimmtAuf -> "Ich höre zu …"
            vm.schreibtMit -> "Schreibe mit …"
            else -> "Mit Jarvis arbeiten"
        }
        // Rechts oben ein fester Platz: mit Text der Papierkorb (leert die ganze Blase), gleich danach „Rückgängig“.
        // Immer gleich breit und hoch, damit die Blase beim ersten Zeichen und beim Leeren nicht springt.
        val leerbar = vm.entwurf.isNotBlank() && !vm.nimmtAuf && !vm.schreibtMit
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 4.dp)) {
            Box(Modifier.weight(1f).padding(top = 10.dp)) {
                if (vm.entwurf.isEmpty()) Text(platzhalter, color = f.textLeise.copy(alpha = 0.7f), fontSize = 17.sp)
                BasicTextField(
                    wert, { feld = it; vm.entwurf = it.text },
                    Modifier.fillMaxWidth().heightIn(min = 24.dp, max = 160.dp).focusRequester(fokus),
                    textStyle = TextStyle(color = f.text, fontSize = 17.sp, lineHeight = 23.sp),
                    cursorBrush = SolidColor(LIVE_ORANGE),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 6,
                )
            }
            Box(Modifier.padding(start = 4.dp).size(36.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                when {
                    leerbar -> Box(Modifier.size(36.dp).antippen { vm.entwurfLeeren() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Delete, "Text löschen", tint = f.textLeise, modifier = Modifier.size(22.dp))
                    }
                    vm.entwurf.isEmpty() && vm.leerenRueckgaengig -> Box(Modifier.size(36.dp).antippen { vm.entwurfZurueck() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, "Gelöschten Text zurückholen", tint = f.primaer, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Unten: Plus links, rechts Modell, Mikrofon und die Live- bzw. Senden-Blase.
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).antippen { plusOffen = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "Anhängen", tint = f.text, modifier = Modifier.size(28.dp))
            }
            if (vm.aktivePlugins.isNotEmpty()) Text("${vm.aktivePlugins.size} Plugin" + if (vm.aktivePlugins.size > 1) "s" else "", color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            // Rechtsbündig im freien Platz, aber begrenzt: zu langer Text kürzt den Modellnamen, statt Mikrofon und Senden hinauszuschieben.
            Row(Modifier.weight(1f).wrapContentWidth(Alignment.End).clip(RoundedCornerShape(12.dp)).antippen { modellOffen = true }.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(mitGuthaben(kurzName(modell)), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text(" " + vm.einstellungen.denkstufe.titel(vm.einstellungen.modell), color = f.text, fontSize = 15.sp, maxLines = 1)
            }
            Spacer(Modifier.width(4.dp))
            // Diktier-Mikrofon: rot, solange es aufnimmt; der Text kommt danach ins Feld.
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (vm.nimmtAuf) f.gefahr else Color.Transparent).antippen { vm.diktatTippen() },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    vm.schreibtMit -> Kern(26.dp, f.primaer, aktiv = true)
                    vm.nimmtAuf -> Icon(Icons.Rounded.Stop, "Aufnahme beenden", tint = Color.White, modifier = Modifier.size(22.dp))
                    else -> Icon(Icons.Rounded.Mic, "Diktieren", tint = f.text, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(6.dp))
            val sendbar = vm.entwurf.isNotBlank() || vm.anhaenge.isNotEmpty()
            // Symbol in der deckenden Grundfarbe: flaecheStark ist im dunklen Design fast durchsichtig, der Pfeil
            // verschwand dann im hellen Kreis.
            when {
                vm.denkt -> RundBlase(f.text, Icons.Rounded.Stop, "Abbrechen", f.hgOben) { vm.abbrechen() }
                sendbar -> RundBlase(f.text, Icons.Rounded.ArrowUpward, "Senden", f.hgOben) { vm.entwurfSenden() }
                else -> Box(Modifier.size(48.dp).clip(CircleShape).background(LIVE_ORANGE).antippen { vm.liveTippen() }, contentAlignment = Alignment.Center) {
                    Wellen(Color.White, Modifier.size(24.dp))
                }
            }
        }
    }

    if (plusOffen) ModalBottomSheet(onDismissRequest = { plusOffen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = menueFlaeche(f)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            MenueZeile(Icons.Rounded.PhotoCamera, "Kamera") { plusOffen = false; vm.kameraAnfragen() }
            MenueZeile(Icons.Rounded.PhotoLibrary, "Fotos") { plusOffen = false; vm.fotosAnfragen() }
            MenueZeile(Icons.Rounded.Folder, "Dateien") { plusOffen = false; vm.dateienAnfragen() }
            if (vm.nachtragMoeglich) {
                // Im Programmiergespräch: etwa einen Zentimeter neben „Programmierer“ der Knopf „+ Nachtrag“; er setzt „Nachtrag:“ vor den Text.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.antippen { plusOffen = false; vm.programmiererEinsetzen() }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Code, null, tint = f.text, modifier = Modifier.size(26.dp))
                        Text("Programmierer", color = f.text, fontSize = 18.sp, modifier = Modifier.padding(start = 20.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(f.flaeche).antippen { plusOffen = false; vm.nachtragEinsetzen() }
                        .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = f.primaer, modifier = Modifier.size(22.dp))
                        Text("Nachtrag", color = f.text, fontSize = 18.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else MenueZeile(Icons.Rounded.Code, "Programmierer") { plusOffen = false; vm.programmiererEinsetzen() }
            MenueZeile(Icons.Rounded.Extension, "Plugins") { plusOffen = false; pluginsOffen = true }
        }
    }

    if (pluginsOffen) ModalBottomSheet(onDismissRequest = { pluginsOffen = false }, containerColor = menueFlaeche(f)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("Plugins", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (Plugins.alle.isEmpty()) Text("Noch keine Plugins. Sobald es welche gibt, wählst du sie hier für die nächste Frage aus.", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            Plugins.alle.forEach { p ->
                val an = p.id in vm.aktivePlugins
                Row(Modifier.fillMaxWidth().padding(top = 10.dp).antippen { if (an) vm.aktivePlugins.remove(p.id) else vm.aktivePlugins.add(p.id) }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(p.beschreibung, color = f.textLeise, fontSize = 13.sp)
                    }
                    if (an) Icon(Icons.Rounded.Check, null, tint = f.primaer)
                }
            }
        }
    }

    if (modellOffen) ModalBottomSheet(onDismissRequest = { modellOffen = false }, containerColor = menueFlaeche(f)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("Modell", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            // Je Anbieter eine Gruppe: OpenAI über das ChatGPT-Konto, Anthropic über den eigenen API-Schlüssel.
            de.frank.jarvis.auth.KiAnbieter.entries.forEach { anbieter ->
            Text(anbieter.label + if (anbieter == de.frank.jarvis.auth.KiAnbieter.ANTHROPIC && !vm.anthropicVerbunden) " · Schlüssel fehlt" else "",
                color = f.textLeise, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
            CodexModel.von(anbieter).forEach { m ->
                val aktiv = m == vm.einstellungen.modell
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).antippen { vm.setzeModell(m) }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (aktiv) mitGuthaben(m.label) else AnnotatedString(m.label), color = f.text, fontSize = 17.sp, fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (aktiv) Icon(Icons.Rounded.Check, null, tint = f.primaer)
                }
            }
            }
            Text("Aufwand", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            vm.einstellungen.modell.supportedEfforts.forEach { st ->
                val aktiv = st == vm.einstellungen.denkstufe
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).antippen { vm.setzeDenkstufe(st); modellOffen = false }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(st.titel(vm.einstellungen.modell), color = f.text, fontSize = 17.sp, fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (aktiv) Icon(Icons.Rounded.Check, null, tint = f.primaer)
                }
            }
        }
    }
}

/** „GPT-6.1 Sol“ → „6.1 Sol“, wie in der ChatGPT-App; „Claude Opus 5.5“ → „Opus 5.5“. */
private fun kurzName(m: CodexModel) = m.label.removePrefix("GPT-").removePrefix("Claude ")

@Composable
private fun RundBlase(farbe: Color, icon: ImageVector, beschreibung: String, aufFarbe: Color, aktion: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(farbe).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = aufFarbe, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MenueZeile(icon: ImageVector, titel: String, aktion: () -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().antippen(aktion = aktion).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = f.text, modifier = Modifier.size(26.dp))
        Text(titel, color = f.text, fontSize = 18.sp, modifier = Modifier.padding(start = 20.dp))
    }
}

/** Das Symbol der Live-Blase: kleiner, großer, mittlerer und kleiner Strich. [pegel] lässt sie im Gespräch mitschwingen. */
@Composable
fun Wellen(farbe: Color, modifier: Modifier = Modifier, pegel: Float = 0f, bewegt: Boolean = false) {
    val anim = if (bewegt) rememberInfiniteTransition(label = "wellen").animateFloat(0.75f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "w").value else 1f
    Canvas(modifier) {
        val hoehen = listOf(0.38f, 1f, 0.66f, 0.38f)
        val breite = size.width / 9f
        val abstand = (size.width - breite * hoehen.size) / (hoehen.size - 1)
        hoehen.forEachIndexed { i, h ->
            val faktor = if (bewegt) (anim * (0.55f + pegel * 0.9f) * (if (i % 2 == 0) 1f else 1.1f)).coerceIn(0.3f, 1.25f) else 1f
            val hh = (size.height * h * faktor).coerceAtMost(size.height)
            drawRoundRect(farbe, Offset(i * (breite + abstand), (size.height - hh) / 2f), Size(breite, hh), CornerRadius(breite / 2f))
        }
    }
}

/** Aufnahmeanzeige im Live-Gespräch: fünf senkrechte Striche, die versetzt auf und ab schwingen; [pegel] verstärkt den Ausschlag. */
@Composable
private fun AudioStriche(farbe: Color, modifier: Modifier = Modifier, pegel: Float = 0f) {
    val phase by rememberInfiniteTransition(label = "striche").animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(900, easing = LinearEasing)), label = "p")
    Canvas(modifier) {
        val anzahl = 5
        val breite = size.width / 9f
        val abstand = (size.width - breite * anzahl) / (anzahl - 1)
        val ausschlag = 0.45f + pegel.coerceIn(0f, 1f) * 0.55f
        for (i in 0 until anzahl) {
            val welle = (kotlin.math.sin(phase + i * 1.3f) + 1f) / 2f
            val hh = size.height * (0.25f + 0.75f * welle * ausschlag)
            drawRoundRect(farbe, Offset(i * (breite + abstand), (size.height - hh) / 2f), Size(breite, hh), CornerRadius(breite / 2f))
        }
    }
}

/** Die Leiste während eines Live-Gesprächs: schwingende Blase, Status, stumm schalten und beenden. */
@Composable
private fun LiveLeiste(vm: AppViewModel) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Antippen unterbricht Jarvis: im Medienton mit Echosperre der einzige Weg, sonst eine Abkürzung zum Dazwischenreden.
        Box(Modifier.size(52.dp).clip(CircleShape).background(LIVE_ORANGE).antippen { vm.liveUnterbrechen() }, contentAlignment = Alignment.Center) {
            Wellen(Color.White, Modifier.size(26.dp), pegel = vm.livePegel, bewegt = true)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Live mit Jarvis", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(if (vm.liveStumm) "Stumm geschaltet (kostet weiter)" else vm.liveStatus, color = f.textLeise, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Welcher Tonweg und welches Mikrofon gerade wirklich laufen (Medienton oder Freisprechen).
            if (vm.liveAudioweg.isNotEmpty()) Text(vm.liveAudioweg, color = f.textLeise, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Dieses Gespräch / alle Live-Gespräche seit dem Zurücksetzen (Einstellungen → Live-Gespräch).
            Text("ca. " + de.frank.jarvis.live.LiveOptionen.usdText(vm.liveKosten) + " / gesamt " + de.frank.jarvis.live.LiveOptionen.usdText(vm.liveKostenGesamt),
                color = LIVE_ORANGE, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (vm.liveStumm) f.text else f.flaeche).antippen { vm.liveStummSchalten() }, contentAlignment = Alignment.Center) {
            Icon(if (vm.liveStumm) Icons.Rounded.MicOff else Icons.Rounded.Mic, if (vm.liveStumm) "Mikrofon an" else "Stumm schalten", tint = if (vm.liveStumm) f.hgOben else f.text, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(8.dp))
        // Rote Fläche mit schwingenden Audiostrichen statt Telefonhörer; Antippen beendet das Gespräch wie bisher.
        Box(Modifier.size(46.dp).clip(CircleShape).background(f.gefahr).antippen { vm.liveBeenden() }
            .semantics { contentDescription = "Live-Gespräch beenden" }, contentAlignment = Alignment.Center) {
            AudioStriche(Color.White, Modifier.size(26.dp, 22.dp), pegel = vm.livePegel)
        }
    }
}

/** Vorschau eines Anhangs: Bilder als Kachel, sonst Symbol mit Dateiname. */
@Composable
fun AnhangKachel(a: Anhang, groesse: Dp) {
    val f = LocalFarben.current
    val bild by produceState<ImageBitmap?>(null, a.pfad) {
        if (a.istBild) value = withContext(Dispatchers.IO) {
            runCatching {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(a.pfad, o)
                var stufe = 1
                while (maxOf(o.outWidth, o.outHeight) / (stufe * 2) >= 400) stufe *= 2
                BitmapFactory.decodeFile(a.pfad, BitmapFactory.Options().apply { inSampleSize = stufe })?.asImageBitmap()
            }.getOrNull()
        }
    }
    val form = RoundedCornerShape(14.dp)
    val b = bild
    if (b != null) {
        Image(b, a.name, Modifier.size(groesse).clip(form), contentScale = ContentScale.Crop)
    } else {
        Column(Modifier.widthIn(min = groesse, max = groesse * 2.2f).size(height = groesse, width = groesse * 2f).clip(form).background(f.flaeche).padding(8.dp), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Description, null, tint = f.primaer, modifier = Modifier.size(22.dp))
            Text(a.name, color = f.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
