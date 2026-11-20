package de.frank.jarvis.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
 * Startbereich „Wichtige E-Mails“: eingeklappt nur die Anzahl („3 wichtige E-Mails“, bei bestätigtem Leerstand „Keine wichtigen E-Mails“),
 * aufgeklappt alle offenen wichtigen Einträge in höchstens drei Zeilen. Antippen öffnet die Nachricht direkt in Gmail und blendet den Eintrag
 * bis zum nächsten Aktualisieren aus. Heute und gestern (Ortszeit) mit grünem Rahmen. Fehler stehen als Fehler da, nie als leerer Posteingang.
 */
@Composable
fun WichtigeMailsKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val s by remember(context) { WichtigeMails.stand(context) }.collectAsState()
    var offen by rememberSaveable { mutableStateOf(false) }
    val drehung by animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")
    // Noch nie geprüft (etwa nach der Installation): einmal von selbst, ohne aufs Wächter-Fenster zu warten.
    LaunchedEffect(Unit) { if (s.geprueft == 0L && s.fehler == null && !s.laeuft) WichtigeMails.aktualisieren(context) }
    val sichtbar = s.sichtbar
    val schutz = remember { KnopfSchutz() }
    Karte(modifier = griff.animateContentSize().antippenMitSchutz(schutz, imSchutz = { WichtigeMails.aktualisieren(context) }) { offen = !offen }) {
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
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Einträge zuklappen" else "Einträge aufklappen", tint = f.textLeise, modifier = Modifier.size(26.dp).rotate(drehung))
            }
        }
        if (offen && sichtbar.isNotEmpty()) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sichtbar.forEach { e -> MailEintrag(e) { WichtigeMails.ausblenden(context, e.msgId); WichtigeMails.oeffnen(context, e) } }
            }
        }
    }
}

@Composable
private fun MailEintrag(e: WichtigeMails.Eintrag, oeffnen: () -> Unit) {
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
    Column(
        Modifier.fillMaxWidth()
            .background(f.flaeche, form)
            .then(if (frisch) Modifier.border(2.dp, FRISCH_GRUEN, form) else Modifier.border(1.dp, f.rand, form))
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
}
