package de.frank.jarvis.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.WichtigeMails
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Grüner Rahmen für Post von heute und gestern (nur Rahmen, kein grüner Hintergrund). */
private val FRISCH_GRUEN = Color(0xFF3DBE7A)

/**
 * Aufklappzustand beider Bereiche für die ganze Laufzeit des Prozesses: Zurück aus Gmail findet Frank sie so vor, wie er sie verlassen hat
 * (die Scrollposition hält der Startbildschirm selbst).
 */
private object MailKartenZustand {
    var wichtigOffen by mutableStateOf(false)
    var alleOffen by mutableStateOf(false)
    var alleAnzahl by mutableStateOf(40)
}

/**
 * Startkarte „E-Mails“ im Stil der Mac-Zentrale: Kopf mit Stand und Aktualisieren, darunter immer zwei große, einzeln aufklappbare Felder.
 * „Wichtige E-Mails“ (im Kopf die Anzahl): alle offenen wichtigen Einträge in höchstens drei Zeilen; Antippen öffnet die Nachricht direkt in
 * Gmail und blendet den Eintrag bis zum nächsten Aktualisieren aus; rechts hinter einem senkrechten Strich die Glühbirne (neue Session mit
 * der Erklärung der Mail); Herunterstufen nur unter „Alle E-Mails“. Heute und gestern (Ortszeit) mit grünem Rahmen. Fehler stehen als Fehler da.
 * „Alle E-Mails“: der ganze Posteingang (auch gelesene und wichtige), Antippen öffnet die Mail in Gmail, rechts ein zweigeteiltes Feld
 * (dicker grüner Pfeil hoch = wichtig, roter runter = unwichtig, erneut antippen = wieder automatisch), sofort wirksam.
 */
@Composable
fun WichtigeMailsKarte(griff: Modifier = Modifier, erklaeren: (WichtigeMails.Eintrag) -> Unit = {}) {
    val f = LocalFarben.current
    val ui = LocalContext.current
    val context = ui.applicationContext
    val s by remember(context) { WichtigeMails.stand(context) }.collectAsState()
    // Noch nie geprüft (etwa nach der Installation): einmal von selbst, ohne aufs Wächter-Fenster zu warten.
    LaunchedEffect(Unit) { if (s.geprueft == 0L && s.fehler == null && !s.laeuft) WichtigeMails.aktualisieren(context) }
    val sichtbar = s.sichtbar
    val schutz = remember { KnopfSchutz() }
    Karte(modifier = griff) {
        val linie = trennlinie(f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            KartenSymbol(KartenArt.MAIL, Modifier.padding(end = 10.dp), wert = if (sichtbar.isNotEmpty()) 1f else 0f)
            Column(Modifier.weight(1f)) {
                Text("E-Mails", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        s.laeuft -> "Prüft den Posteingang …"
                        s.fehler != null -> s.fehler!!
                        s.geprueft > 0 -> "Stand " + Instant.ofEpochMilli(s.geprueft).atZone(ZoneId.systemDefault()).let { z ->
                            if (z.toLocalDate() == LocalDate.now()) z.format(DateTimeFormatter.ofPattern("HH:mm")) + " Uhr" else z.format(DateTimeFormatter.ofPattern("d.M., HH:mm")) + " Uhr"
                        }
                        else -> "Noch nicht geprüft"
                    },
                    color = when { s.laeuft -> f.primaer; s.fehler != null -> f.gefahr; else -> f.textLeise },
                    fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            AktualisierenKnopf(s.laeuft, "E-Mails aktualisieren", "E-Mails werden geprüft", schutz) { WichtigeMails.aktualisieren(context) }
        }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 14.dp), thickness = 1.dp, color = linie)

        AufklappFeld(MailKartenZustand.wichtigOffen, { MailKartenZustand.wichtigOffen = !MailKartenZustand.wichtigOffen }, kopf = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wichtige E-Mails", color = f.primaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (sichtbar.isNotEmpty()) Zahl(sichtbar.size)
            }
            val frische = sichtbar.count { it.frisch }
            Text(
                when {
                    sichtbar.isNotEmpty() -> (if (sichtbar.size == 1) "1 wichtige E-Mail" else "${sichtbar.size} wichtige E-Mails") + if (frische > 0) " · $frische von heute/gestern" else ""
                    s.eintraege.isNotEmpty() -> "Keine weiteren wichtigen E-Mails"
                    s.bestaetigt -> "Keine wichtigen E-Mails"
                    s.fehler != null -> "Nicht sicher geprüft"
                    else -> "Noch nicht geprüft"
                },
                color = if (sichtbar.isEmpty() && s.bestaetigt) f.erfolg else f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp),
            )
        }) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sichtbar.isEmpty()) Text(
                    when {
                        s.eintraege.isNotEmpty() -> "Geöffnete wichtige E-Mails erscheinen nach dem Aktualisieren wieder, solange sie offen sind."
                        s.bestaetigt -> "Hier steht nichts, was du prüfen oder beantworten musst."
                        else -> s.fehler ?: "Noch nicht geprüft."
                    },
                    color = if (s.bestaetigt) f.textLeise else f.gefahr, fontSize = 14.sp,
                )
                sichtbar.forEach { e ->
                    key(e.msgId) {
                        MailEintrag(e, erklaeren = { erklaeren(e) }) {
                            WichtigeMails.ausblenden(context, e.msgId); WichtigeMails.oeffnen(ui, e)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AufklappFeld(MailKartenZustand.alleOffen, { MailKartenZustand.alleOffen = !MailKartenZustand.alleOffen }, kopf = {
            val ungelesen = s.posteingang.count { !it.gelesen }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Alle E-Mails", color = f.primaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (s.posteingang.isNotEmpty()) Zahl(s.posteingang.size)
            }
            Text(
                if (s.posteingang.isEmpty()) (if (s.bestaetigt) "Posteingang leer" else "Noch nicht geladen")
                else "${s.posteingang.size} im Posteingang" + if (ungelesen > 0) " · $ungelesen ungelesen" else "",
                color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp),
            )
        }) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.posteingang.isEmpty()) Text(
                    if (s.bestaetigt) "Keine E-Mails im Posteingang." else s.fehler ?: "Noch nicht geladen.",
                    color = if (s.bestaetigt) f.textLeise else f.gefahr, fontSize = 14.sp,
                )
                // In Stücken zu 40: Ein großer Posteingang baut sonst Hunderte Zeilen auf einmal.
                s.posteingang.take(MailKartenZustand.alleAnzahl).forEach { e ->
                    key(e.msgId) { AlleEintrag(e, bewerten = { r -> WichtigeMails.bewerten(context, e.msgId, r) }) { WichtigeMails.oeffnen(ui, e) } }
                }
                val rest = s.posteingang.size - MailKartenZustand.alleAnzahl
                if (rest > 0) Text(
                    "Weitere ${minOf(rest, 40)} von $rest anzeigen",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).antippen { MailKartenZustand.alleAnzahl += 40 }.padding(vertical = 12.dp),
                    color = f.primaer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/** Zahl-Plakette im Feldkopf wie bei der Mac-Zentrale. */
@Composable
private fun Zahl(n: Int) {
    val f = LocalFarben.current
    Text("$n", Modifier.padding(start = 8.dp).glas(f, 10.dp, erhoeht = 0.2f, toenung = f.primaer).padding(horizontal = 8.dp, vertical = 2.dp),
        color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

private fun wannText(e: WichtigeMails.Eintrag): String {
    val z = Instant.ofEpochMilli(e.empfangen).atZone(ZoneId.systemDefault())
    val heute = LocalDate.now()
    return when (e.tag) {
        heute -> "Heute " + z.format(DateTimeFormatter.ofPattern("HH:mm"))
        heute.minusDays(1) -> "Gestern " + z.format(DateTimeFormatter.ofPattern("HH:mm"))
        else -> z.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
    }
}

/** Kräftiges Grün für „wichtig“ und Rot für „unwichtig“ (Frank, 22.11.2026: „richtig schön rot nach unten, grün nach oben“). */
private val PFEIL_GRUEN = Color(0xFF22C55E)
private val PFEIL_ROT = Color(0xFFEF4444)

/** Dicker Pfeil nach oben oder unten: [voll] = ausgemalt (aktiv), sonst nur als Umriss (leer). */
@Composable
private fun DickerPfeil(hoch: Boolean, farbe: Color, voll: Boolean, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val pfad = androidx.compose.ui.graphics.Path().apply {
            // Nach oben gezeichnet, für „unten“ gespiegelt.
            fun y(v: Float) = if (hoch) v * h else h - v * h
            moveTo(0.5f * w, y(0f))
            lineTo(w, y(0.52f))
            lineTo(0.72f * w, y(0.52f))
            lineTo(0.72f * w, y(1f))
            lineTo(0.28f * w, y(1f))
            lineTo(0.28f * w, y(0.52f))
            lineTo(0f, y(0.52f))
            close()
        }
        if (voll) drawPath(pfad, farbe)
        else drawPath(pfad, farbe, style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round,
        ))
    }
}

/**
 * Eine Hälfte des Pfeilfelds, mindestens 48 dp hoch. [aktiv] = so ist die Mail gerade eingestuft: Pfeil ausgemalt und farbig hinterlegt,
 * für TalkBack „ausgewählt“. Sonst nur der leere Umriss. [herkunft] sagt TalkBack, ob Frank oder Jarvis die Einstufung gesetzt hat.
 */
@Composable
private fun PfeilFeld(modifier: Modifier, hoch: Boolean, aktiv: Boolean, herkunft: String, gesperrt: Boolean = false, aktion: () -> Unit) {
    val farbe = if (hoch) PFEIL_GRUEN else PFEIL_ROT
    val name = if (hoch) "Als wichtig markieren" else "Als unwichtig markieren"
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(if (aktiv) farbe.copy(alpha = 0.18f) else Color.Transparent)
            .then(if (gesperrt) Modifier else Modifier.antippen(rolle = Role.RadioButton, aktion = aktion))
            .semantics(mergeDescendants = true) {
                if (gesperrt) disabled()
                contentDescription = name
                selected = aktiv
                stateDescription = if (aktiv) (if (hoch) "wichtig, $herkunft" else "unwichtig, $herkunft") else "nicht gewählt"
            },
        contentAlignment = Alignment.Center,
    ) {
        DickerPfeil(hoch, if (aktiv) farbe else farbe.copy(alpha = 0.7f), voll = aktiv, Modifier.size(24.dp))
    }
}

@Composable
private fun AlleEintrag(e: WichtigeMails.Eintrag, bewerten: (Int) -> Unit, oeffnen: () -> Unit) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .clip(form)
            .background(f.flaeche, form)
            .border(if (e.wichtig) 2.dp else 1.dp, if (e.wichtig) f.primaer.copy(alpha = 0.7f) else f.rand, form),
    ) {
        Column(
            Modifier.weight(1f)
                .antippen(aktion = oeffnen)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, Modifier.weight(1f), color = f.text, fontSize = 15.sp, fontWeight = if (e.gelesen) FontWeight.Normal else FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Text(wannText(e), color = f.textLeise, fontSize = 13.sp, maxLines = 1)
            }
            Text(e.betreff, color = f.text, fontSize = 14.sp, fontWeight = if (e.gelesen) FontWeight.Normal else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            Text(
                buildAnnotatedString {
                    if (e.wichtig) withStyle(SpanStyle(color = f.primaer, fontWeight = FontWeight.SemiBold)) { append("Wichtig · ") }
                    append(e.kurz.ifBlank { if (e.gelesen) "Gelesen" else "Ungelesen" })
                },
                color = f.textLeise, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(f.rand))
        Column(Modifier.width(52.dp).fillMaxHeight()) {
            // Grün leuchtet, wenn die Mail gerade unter „Wichtige E-Mails“ steht (von Frank hochgestuft oder von Jarvis als wichtig erkannt),
            // Rot nur, wenn Frank sie als unwichtig markiert hat; sonst sind beide leer. Nie beide zugleich.
            val gruen = e.bewertung == 1 || (e.bewertung == 0 && e.wichtig)
            val rot = e.bewertung == -1
            // Leuchtet Grün, weil Jarvis die Mail als wichtig erkannt hat, ist der Pfeil gesperrt (TalkBack: „deaktiviert“), statt still nichts zu tun.
            PfeilFeld(
                Modifier.weight(1f), hoch = true, aktiv = gruen, herkunft = if (e.bewertung == 1) "von dir markiert" else "von Jarvis erkannt",
                gesperrt = e.bewertung == 0 && e.wichtig,
            ) { bewerten(1) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(f.rand))
            PfeilFeld(Modifier.weight(1f), hoch = false, aktiv = rot, herkunft = "von dir markiert") { bewerten(-1) }
        }
    }
}

@Composable
private fun MailEintrag(e: WichtigeMails.Eintrag, erklaeren: () -> Unit, oeffnen: () -> Unit) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(14.dp)
    val frisch = e.frisch
    val z = Instant.ofEpochMilli(e.empfangen).atZone(ZoneId.systemDefault())
    val heute = LocalDate.now()
    val wann = when (e.tag) {
        heute -> "Heute " + z.format(DateTimeFormatter.ofPattern("HH:mm"))
        heute.minusDays(1) -> "Gestern " + z.format(DateTimeFormatter.ofPattern("HH:mm"))
        else -> z.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
    }
    // Links der Eintrag (Antippen öffnet Gmail wie bisher), rechts hinter einem senkrechten Strich über die ganze Höhe die Glühbirne „Erklären“.
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .clip(form)
            .background(f.flaeche, form)
            .then(if (frisch) Modifier.border(2.dp, FRISCH_GRUEN, form) else Modifier.border(1.dp, f.rand, form)),
    ) {
    Column(
        Modifier.weight(1f)
            .antippen(aktion = oeffnen)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        // Höchstens drei Zeilen: Absender mit Zeit, darunter die Kurzfassung in zwei Zeilen.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.name, Modifier.weight(1f), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(wann, color = if (frisch) FRISCH_GRUEN else f.textLeise, fontSize = 13.sp, fontWeight = if (frisch) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        }
        Text(
            buildAnnotatedString {
                if (e.antwort) withStyle(SpanStyle(color = f.primaer, fontWeight = FontWeight.SemiBold)) { append("Antwort nötig · ") }
                append(e.kurz)
            },
            color = f.textLeise, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
        )
    }
    Box(Modifier.fillMaxHeight().width(if (frisch) 2.dp else 1.dp).background(if (frisch) FRISCH_GRUEN else f.rand))
    // Nur die Glühbirne (Frank, 22.11.2026: kein Pfeil daneben, damit er nicht versehentlich herunterstuft; das geht unter „Alle E-Mails“).
    Box(Modifier.fillMaxHeight().width(52.dp).antippen(aktion = erklaeren), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Lightbulb, "E-Mail erklären", tint = f.primaer, modifier = Modifier.size(24.dp))
    }
    }
}
