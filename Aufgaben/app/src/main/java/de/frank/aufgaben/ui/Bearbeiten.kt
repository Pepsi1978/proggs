package de.frank.aufgaben.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Schritt
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.TITEL_MAX
import de.frank.aufgaben.data.Wiederholung
import de.frank.aufgaben.ui.theme.Chip
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun BearbeitenBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val neu = vm.eId == null
    var loeschenFragen by remember { mutableStateOf(false) }
    if (loeschenFragen) AlertDialog(
        onDismissRequest = { loeschenFragen = false },
        icon = { Icon(Icons.Rounded.DeleteOutline, null, tint = f.gefahr) },
        title = { Text("Aufgabe löschen?", color = f.text, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Möchtest du „${vm.eTitel.ifBlank { vm.eText.take(60) }.ifBlank { "diese Aufgabe" }}“ wirklich löschen?",
                color = f.textLeise, fontSize = 15.sp,
            )
        },
        confirmButton = {
            Text(
                "Löschen", color = f.gefahr, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.antippen {
                    loeschenFragen = false
                    val id = vm.eId
                    vm.verwerfen()
                    vm.aufgaben.value.firstOrNull { it.id == id }?.let { vm.loeschen(it) }
                }.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        dismissButton = {
            Text(
                "Abbrechen", color = f.primaer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.antippen { loeschenFragen = false }.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        containerColor = f.hgOben,
    )
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück (speichert)") { vm.zurueck() }
                Text(if (neu) "Neue Aufgabe" else "Aufgabe", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                if (!neu) RundKnopf(Icons.Rounded.SelfImprovement, "Fokus starten") { vm.eId?.let { vm.speichern(); vm.fokusStarten(it) } }
                RundKnopf(if (neu) Icons.Rounded.Close else Icons.Rounded.DeleteOutline, if (neu) "Verwerfen" else "Löschen") {
                    // Neue Aufgaben verwirft der Knopf sofort, gespeicherte erst nach Rückfrage.
                    loeschenFragen = !neu
                    if (neu) vm.verwerfen()
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Überschrift
                Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.2f).padding(18.dp)) {
                    Box {
                        if (vm.eTitel.isEmpty()) Text("Überschrift (optional)", color = f.textSchwach, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        BasicTextField(
                            // Höchstens TITEL_MAX Zeichen, damit der Titel im Widget ganz zu sehen ist. Ältere, längere
                            // Titel lassen sich weiter kürzen, aber nicht verlängern.
                            vm.eTitel, { neu -> vm.eTitel = if (neu.length <= TITEL_MAX || neu.length < vm.eTitel.length) neu else neu.take(maxOf(TITEL_MAX, vm.eTitel.length)) },
                            textStyle = TextStyle(color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(f.primaer), singleLine = false, maxLines = 3,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (vm.eTitel.isNotBlank()) Text(
                        "${vm.eTitel.length}/$TITEL_MAX Zeichen",
                        color = if (vm.eTitel.length > TITEL_MAX) f.primaer else f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                    if (vm.eTitel.isBlank()) Text(
                        if (vm.kiVerbunden) "✨ Ohne Überschrift erzeugt die KI einen passenden Titel." else "Ohne Überschrift werden die ersten Wörter zum Titel.",
                        color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
                // Text + Diktat
                Column(Modifier.fillMaxWidth().einblenden(60).glas(f, erhoeht = 1.2f).padding(18.dp)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 110.dp)) {
                        if (vm.eText.isEmpty()) Text(
                            if (vm.aufnahme == Aufnahme.LAEUFT) "Ich höre zu …" else "Sprich oder schreibe deine Aufgabe …",
                            color = f.textSchwach, fontSize = 17.sp,
                        )
                        BasicTextField(
                            vm.eText, vm::textGeaendert,
                            textStyle = TextStyle(color = f.text, fontSize = 17.sp, lineHeight = 25.sp),
                            cursorBrush = SolidColor(f.primaer),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val zurueck = vm.textVorKorrektur != null
                        AktionsKnopf(
                            if (zurueck) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.AutoFixHigh,
                            if (zurueck) "Zurück" else "KI-Korrektur",
                            beschaeftigt = vm.korrigiert,
                            modifier = Modifier.weight(1f),
                        ) { if (zurueck) vm.korrekturZuruecknehmen() else vm.korrigieren() }
                        if (zurueck) AktionsKnopf(Icons.Rounded.AutoFixHigh, "Neue Fassung", beschaeftigt = vm.korrigiert, modifier = Modifier.weight(1f)) { vm.korrigieren() }
                        val spricht = vm.sprichtId == "editor"
                        AktionsKnopf(if (spricht) Icons.Rounded.StopCircle else Icons.Rounded.VolumeUp, if (spricht) "Stopp" else "Vorlesen", modifier = Modifier.weight(1f)) {
                            vm.vorlesen("editor", listOf(vm.eTitel, vm.eText).filter { it.isNotBlank() }.joinToString(". "))
                        }
                    }
                }
                // Mikrofon
                Mikrofon(vm)
                // Erkannter Vorschlag
                AnimatedVisibility(vm.eVorschlag != null) {
                    val v = vm.eVorschlag
                    if (v != null) Row(
                        Modifier.fillMaxWidth().knopf3d(f.primaer.copy(alpha = 0.9f), f.sekundaer.copy(alpha = 0.85f), 18.dp, f.dunkel).antippen { vm.vorschlagUebernehmen() }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text("Erkannt – übernehmen?", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text(
                                listOfNotNull(v.tag?.let { Tage.datum(it) }, v.minuten?.let { "${Tage.zeit(it)} Uhr" }, v.prioritaet?.anzeige).joinToString(" · "),
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(Icons.Rounded.Check, null, tint = Color.White)
                    }
                }
                // Priorität
                Block("Priorität") {
                    ChipReihe {
                        Prioritaet.entries.forEach { p ->
                            Chip(p.anzeige, vm.ePrio == p, farbe = f.prio(p)) { vm.ePrio = p }
                        }
                    }
                    if (vm.eTag != null && vm.ePrio == Prioritaet.SPAETER) Text("In Heute und Morgen gilt mindestens „Mittel“.", color = f.textLeise, fontSize = 12.sp)
                }
                // Wann
                Block("Wann") {
                    val heute = vm.heute
                    ChipReihe {
                        Chip("Ohne Tag", vm.eTag == null) { vm.setzeTag(null) }
                        Chip("Heute", vm.eTag == heute, farbe = f.sekundaer) { vm.setzeTag(heute) }
                        Chip("Morgen", vm.eTag == heute + 1, farbe = f.primaer) { vm.setzeTag(heute + 1) }
                        val anderes = vm.eTag != null && vm.eTag != heute && vm.eTag != heute + 1
                        Chip(if (anderes) Tage.datum(vm.eTag!!) else "Datum …", anderes, icon = Icons.Rounded.CalendarMonth, farbe = f.tertiaer) {
                            val d = LocalDate.ofEpochDay(vm.eTag ?: heute)
                            DatePickerDialog(context, { _, j, m, t -> vm.setzeTag(LocalDate.of(j, m + 1, t).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                        }
                    }
                    ChipReihe {
                        Chip(
                            vm.eMinuten?.let { "${Tage.zeit(it)} Uhr" } ?: "Uhrzeit hinzufügen", vm.eMinuten != null, icon = Icons.Rounded.Schedule,
                        ) {
                            val m = vm.eMinuten ?: 9 * 60
                            TimePickerDialog(context, { _, h, mi -> vm.setzeZeit(h * 60 + mi) }, m / 60, m % 60, true).show()
                        }
                        if (vm.eMinuten != null) Chip("Ohne Uhrzeit", false, icon = Icons.Rounded.Close) { vm.setzeZeit(null) }
                    }
                    if (vm.eMinuten != null) {
                        Text("Dauer", color = f.textLeise, fontSize = 13.sp)
                        ChipReihe { listOf(15, 30, 45, 60, 90, 120).forEach { d -> Chip(if (d < 60) "$d Min." else "${d / 60}${if (d % 60 == 30) ",5" else ""} Std.", vm.eDauer == d) { vm.eDauer = d } } }
                    }
                }
                // Erinnerung
                AnimatedVisibility(vm.eMinuten != null) {
                    Block("Erinnerung") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, null, tint = f.sekundaer)
                            Text(if (vm.eErinnerung) "Benachrichtigung ist an" else "Keine Benachrichtigung", color = f.text, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                            Switch(vm.eErinnerung, { vm.eErinnerung = it; if (it) vm.hinweiseAnfragen() }, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
                        }
                        if (vm.eErinnerung) {
                            ChipReihe {
                                listOf(0, 5, 10, 15, 30, 60).forEach { v -> Chip(if (v == 0) "Pünktlich" else "$v Min. vorher", vm.eVorlauf == v) { vm.eVorlauf = v } }
                            }
                            Schalter("Aufgabe vorlesen (6 Fassungen, auch offline)", vm.eVorlesen) { vm.eVorlesen = it }
                            if (vm.eVorlesen) {
                                val vorlesetext = vm.eText.trim().ifBlank { vm.eTitel.trim() }
                                Text(
                                    if (vorlesetext.isBlank()) "Vorgelesen wird der Aufgabentext – sprich oder schreibe ihn oben ein."
                                    else "Vorgelesen wird: „${vorlesetext.take(160)}${if (vorlesetext.length > 160) " …" else ""}“",
                                    color = f.textLeise, fontSize = 12.sp,
                                )
                                if (vm.textVorKorrektur == null && vm.kiVerbunden && vorlesetext.isNotBlank()) Text(
                                    "Tipp: Die KI-Korrektur macht daraus eine kurze Anweisung wie „Brenne die Solo-CD für Papa.“",
                                    color = f.textSchwach, fontSize = 12.sp,
                                )
                            }
                            Schalter("Als Wecker – läuft, bis du ausschaltest", vm.eAlsWecker) { vm.eAlsWecker = it; if (it) vm.hinweiseAnfragen() }
                            if (vm.eAlsWecker) Text(
                                if (vm.eVorlesen) "Ton und Vorlesen wiederholen sich, bis du oben in der Benachrichtigung „Ausschalten“ tippst."
                                else "Der Ton wiederholt sich, bis du oben in der Benachrichtigung „Ausschalten“ tippst.",
                                color = f.textLeise, fontSize = 12.sp,
                            )
                        }
                    }
                }
                // Wiederholung
                Block("Wiederholung") {
                    ChipReihe { Wiederholung.entries.forEach { w -> Chip(w.anzeige, vm.eWdh == w) { vm.eWdh = w } } }
                }
                // Checkliste
                Block("Checkliste") {
                    vm.eSchritte.forEachIndexed { i, s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkkreis(s.erledigt, f.primaer, groesse = 20) { vm.eSchritte[i] = s.copy(erledigt = !s.erledigt) }
                            Box(Modifier.weight(1f)) {
                                if (s.text.isEmpty()) Text("Schritt …", color = f.textSchwach, fontSize = 15.sp)
                                BasicTextField(
                                    s.text, { vm.eSchritte[i] = s.copy(text = it) },
                                    textStyle = TextStyle(color = f.text, fontSize = 15.sp, textDecoration = if (s.erledigt) TextDecoration.LineThrough else null),
                                    cursorBrush = SolidColor(f.primaer), modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Box(Modifier.size(36.dp).antippen { vm.eSchritte.removeAt(i) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, "Schritt entfernen", tint = f.textSchwach, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Chip("Schritt hinzufügen", false, icon = Icons.Rounded.Add) { vm.eSchritte.add(Schritt("", false)) }
                }
                Spacer(Modifier.height(110.dp))
            }
        }
        // Speichern
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)
                .height(58.dp).knopf3d(f.primaer, f.sekundaer, 20.dp, f.dunkel).antippen { vm.speichern() },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(if (vm.aufnahme == Aufnahme.LAEUFT) "Aufnahme beenden & speichern" else "Speichern", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Mikrofon(vm: AppViewModel) {
    val f = LocalFarben.current
    val laeuft = vm.aufnahme == Aufnahme.LAEUFT
    val verarbeitet = vm.aufnahme == Aufnahme.VERARBEITET
    val puls = rememberInfiniteTransition(label = "mikro")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "p")
    var sekunden by remember { mutableLongStateOf(0L) }
    LaunchedEffect(laeuft) { while (laeuft) { sekunden = (System.currentTimeMillis() - vm.aufnahmeStart) / 1000; delay(250) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(104.dp).drawBehind {
                if (laeuft) for (i in 0..2) {
                    val q = (p + i / 3f) % 1f
                    drawCircle(f.sekundaer.copy(alpha = 0.35f * (1f - q)), size.minDimension / 2 * (0.8f + 0.7f * q))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(84.dp).graphicsLayer { val s = if (laeuft) 1f + 0.04f * kotlin.math.sin(p * 6.28f) else 1f; scaleX = s; scaleY = s }
                    .knopf3d(if (laeuft) f.gefahr else f.primaer, if (laeuft) f.sekundaer else f.sekundaer, 99.dp, f.dunkel)
                    .antippen { vm.mikroTippen() },
                contentAlignment = Alignment.Center,
            ) {
                if (verarbeitet) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                else Icon(if (laeuft) Icons.Rounded.Stop else Icons.Rounded.Mic, if (laeuft) "Aufnahme beenden" else "Einsprechen", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Text(
            when {
                laeuft -> "%d:%02d · Tippen zum Beenden".format(sekunden / 60, sekunden % 60)
                verarbeitet -> "Whisper transkribiert …"
                vm.einstellungen.groqKey.isBlank() -> "Für Sprache: Groq-Schlüssel in den Einstellungen"
                else -> "Tippen und sprechen"
            },
            color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AktionsKnopf(icon: ImageVector, text: String, beschaeftigt: Boolean = false, modifier: Modifier = Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Row(
        modifier.height(44.dp).glas(f, 14.dp, 0.7f, f.flaecheStark).antippen(aktion = aktion).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (beschaeftigt) CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Icon(icon, null, tint = f.primaer, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = f.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun Block(titel: String, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().einblenden(100).glas(f, erhoeht = 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titel, color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        inhalt()
    }
}

@Composable
fun ChipReihe(inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { inhalt() }
}

@Composable
private fun Leer() = Canvas(Modifier.size(1.dp)) {}
