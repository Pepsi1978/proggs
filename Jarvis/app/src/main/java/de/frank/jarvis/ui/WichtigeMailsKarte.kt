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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
}

/**
 * Startbereich „Wichtige E-Mails“: eingeklappt nur die Anzahl („3 wichtige E-Mails“, bei bestätigtem Leerstand „Keine wichtigen E-Mails“),
 * aufgeklappt alle offenen wichtigen Einträge in höchstens drei Zeilen. Antippen öffnet die Nachricht direkt in Gmail und blendet den Eintrag
 * bis zum nächsten Aktualisieren aus; rechts hinter einem senkrechten Strich oben die Glühbirne (neue Session mit der Erklärung der Mail),
 * darunter der Pfeil „unwichtig“. Heute und gestern (Ortszeit) mit grünem Rahmen. Fehler stehen als Fehler da, nie als leerer Posteingang.
 * Darunter eigens aufklappbar „Alle E-Mails“: der ganze Posteingang (auch gelesene und wichtige), Antippen öffnet die Mail in Gmail, rechts ein
 * zweigeteiltes Feld (Pfeil hoch = wichtig, runter = unwichtig, erneut antippen = wieder automatisch), sofort wirksam.
 */
@Composable
fun WichtigeMailsKarte(griff: Modifier = Modifier, erklaeren: (WichtigeMails.Eintrag) -> Unit = {}) {
    val f = LocalFarben.current
    val ui = LocalContext.current
    val context = ui.applicationContext
    val s by remember(context) { WichtigeMails.stand(context) }.collectAsState()
    val offen = MailKartenZustand.wichtigOffen
    val alleOffen = MailKartenZustand.alleOffen
    val drehung by animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")
    val drehungAlle by animateFloatAsState(if (alleOffen) 180f else 0f, label = "pfeilAlle")
    // Noch nie geprüft (etwa nach der Installation): einmal von selbst, ohne aufs Wächter-Fenster zu warten.
    LaunchedEffect(Unit) { if (s.geprueft == 0L && s.fehler == null && !s.laeuft) WichtigeMails.aktualisieren(context) }
    val sichtbar = s.sichtbar
    val schutz = remember { KnopfSchutz() }
    Karte(modifier = griff.animateContentSize().antippenMitSchutz(schutz, imSchutz = { WichtigeMails.aktualisieren(context) }) { MailKartenZustand.wichtigOffen = !offen }) {
        Row(verticalAlignment = Alignment.Top) {
            KartenSymbol(KartenArt.MAIL, Modifier.padding(end = 10.dp), wert = if (sichtbar.isNotEmpty()) 1f else 0f)
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        sichtbar.isNotEmpty() -> if (sichtbar.size == 1) "1 wichtige E-Mail" else "${sichtbar.size} wichtige E-Mails"
                        s.eintraege.isNotEmpty() -> "Keine weiteren wichtigen E-Mails"
                        s.bestaetigt -> "Keine wichtigen E-Mails"
                        else -> "Wichtige E-Mails"
                    },
                    color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                )
                val frische = sichtbar.count { it.frisch }
                Text(
                    when {
                        s.laeuft -> "Prüft den Posteingang …"
                        s.fehler != null -> s.fehler!!
                        s.geprueft > 0 -> "Stand " + Instant.ofEpochMilli(s.geprueft).atZone(ZoneId.systemDefault()).let { z ->
                            if (z.toLocalDate() == LocalDate.now()) z.format(DateTimeFormatter.ofPattern("HH:mm")) + " Uhr" else z.format(DateTimeFormatter.ofPattern("d.M., HH:mm")) + " Uhr"
                        } + if (frische > 0) " · $frische von heute/gestern" else ""
                        else -> "Noch nicht geprüft"
                    },
                    color = when { s.laeuft -> f.primaer; s.fehler != null -> f.gefahr; else -> f.textLeise },
                    fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            AktualisierenKnopf(s.laeuft, "Wichtige E-Mails aktualisieren", "Wichtige E-Mails werden geprüft", schutz) { WichtigeMails.aktualisieren(context) }
        }
        if (sichtbar.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Wichtige E-Mails zuklappen" else "Wichtige E-Mails aufklappen", tint = f.textLeise, modifier = Modifier.size(26.dp).rotate(drehung))
            }
        }
        if (offen && sichtbar.isNotEmpty()) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sichtbar.forEach { e ->
                    key(e.msgId) {
                        MailEintrag(e, erklaeren = { erklaeren(e) }, unwichtig = { WichtigeMails.bewerten(context, e.msgId, -1) }) {
                            WichtigeMails.ausblenden(context, e.msgId); WichtigeMails.oeffnen(ui, e)
                        }
                    }
                }
            }
        }
        // „Alle E-Mails“: eigener Kopf mit eigenem Pfeil; sein Antippen klappt nur diesen Bereich.
        if (s.posteingang.isNotEmpty()) {
            val ungelesen = s.posteingang.count { !it.gelesen }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp))
                    .antippen { MailKartenZustand.alleOffen = !alleOffen }
                    .semantics { stateDescription = if (alleOffen) "aufgeklappt" else "zugeklappt" }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Alle E-Mails", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    " · ${s.posteingang.size}" + if (ungelesen > 0) " · $ungelesen ungelesen" else "",
                    Modifier.weight(1f), color = f.textLeise, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Rounded.KeyboardArrowDown, if (alleOffen) "Alle E-Mails zuklappen" else "Alle E-Mails aufklappen", tint = f.textLeise, modifier = Modifier.size(26.dp).rotate(drehungAlle))
            }
            if (alleOffen) {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.posteingang.forEach { e ->
                        key(e.msgId) { AlleEintrag(e, bewerten = { r -> WichtigeMails.bewerten(context, e.msgId, r) }) { WichtigeMails.oeffnen(ui, e) } }
                    }
                }
            }
        }
    }
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

/** Eine Hälfte des Pfeilfelds: mindestens 48 dp hoch, gewählt = gefüllt und für TalkBack als „ausgewählt“ angesagt. */
@Composable
private fun PfeilFeld(modifier: Modifier, hoch: Boolean, gewaehlt: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    val farbe = if (hoch) f.primaer else f.gefahr
    val name = if (hoch) "Als wichtig markieren" else "Als unwichtig markieren"
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(if (gewaehlt) farbe.copy(alpha = 0.22f) else Color.Transparent)
            .antippen(rolle = Role.RadioButton, aktion = aktion)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                selected = gewaehlt
                stateDescription = if (gewaehlt) (if (hoch) "als wichtig markiert, erneut tippen hebt auf" else "als unwichtig markiert, erneut tippen hebt auf") else "nicht markiert"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (hoch) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = if (gewaehlt) farbe else f.textLeise, modifier = Modifier.size(if (gewaehlt) 28.dp else 24.dp))
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
            PfeilFeld(Modifier.weight(1f), hoch = true, gewaehlt = e.bewertung == 1) { bewerten(1) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(f.rand))
            PfeilFeld(Modifier.weight(1f), hoch = false, gewaehlt = e.bewertung == -1) { bewerten(-1) }
        }
    }
}

@Composable
private fun MailEintrag(e: WichtigeMails.Eintrag, erklaeren: () -> Unit, unwichtig: () -> Unit, oeffnen: () -> Unit) {
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
    // Oben die Glühbirne wie bisher, darunter der Pfeil „unwichtig“ (nimmt die Mail sofort aus diesem Bereich, unter „Alle E-Mails“ bleibt sie).
    Column(Modifier.width(52.dp).fillMaxHeight()) {
        Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp).antippen(aktion = erklaeren), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lightbulb, "E-Mail erklären", tint = f.primaer, modifier = Modifier.size(24.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (frisch) FRISCH_GRUEN else f.rand))
        PfeilFeld(Modifier.weight(1f), hoch = false, gewaehlt = false, aktion = unwichtig)
    }
    }
}
