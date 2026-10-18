package de.frank.jarvis.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.gedaechtnis.LernPosten
import de.frank.jarvis.gedaechtnis.Lernbericht
import de.frank.jarvis.gedaechtnis.Lernlauf
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Knopf „Jetzt aus den Gesprächen lernen“ mit dem Bericht des letzten Lernlaufs (von Hand oder automatisch): Zustand,
 * kurze Bilanz und aufklappbare Einzelheiten, darunter, ob die Automatik arbeitet und frühere Läufe.
 * [geaendert] meldet, dass ein Lauf fertig ist oder Frank etwas wiederhergestellt hat (Notizen neu laden).
 */
@Composable
fun LernlaufBereich(geaendert: () -> Unit) {
    val f = LocalFarben.current
    val app = LocalContext.current.applicationContext
    val bereich = rememberCoroutineScope()
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Lernlauf.lade(app) } }
    val berichte by Lernlauf.berichte.collectAsState()
    val juengster = berichte.firstOrNull()
    val laeuft = juengster?.zustand == Lernlauf.LAEUFT
    var automatik by remember { mutableStateOf("") }
    var hinweis by remember { mutableStateOf("") }
    var fruehere by remember { mutableStateOf(false) }
    LaunchedEffect(juengster?.id, juengster?.zustand, juengster?.ende) {
        automatik = withContext(Dispatchers.IO) { runCatching { Lernlauf.automatikText(app) }.getOrDefault("") }
        if (!laeuft) geaendert()
    }
    val antworten: (Lernbericht, LernPosten, String) -> Unit = { b, p, was ->
        bereich.launch {
            hinweis = withContext(Dispatchers.IO) { Lernlauf.antworte(app, b.id, p.schluessel, was) }
            geaendert()
        }
    }

    Knopf(if (laeuft) "Jarvis lernt und prüft …" else "Jetzt aus den Gesprächen lernen", Modifier.padding(top = 8.dp).fillMaxWidth(), Icons.Rounded.Psychology, haupt = false) {
        hinweis = if (laeuft || !Lernlauf.starteVonHand(app)) "Es läuft schon ein Lernlauf; sein Bericht steht hier und wächst mit." else ""
    }
    if (hinweis.isNotEmpty()) Text(hinweis, color = f.primaer, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
    if (automatik.isNotEmpty()) Text(automatik, color = f.textSchwach, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))

    if (juengster != null) BerichtAnsicht(juengster, offen = false, antworten = antworten)
    val aeltere = berichte.drop(1).take(10)
    if (aeltere.isNotEmpty()) {
        Text(if (fruehere) "Frühere Lernläufe ausblenden" else "Frühere Lernläufe (${aeltere.size})", color = f.primaer, fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp).antippen { fruehere = !fruehere })
        if (fruehere) aeltere.forEach { BerichtAnsicht(it, offen = false, antworten = antworten) }
    }
}

@Composable
private fun BerichtAnsicht(b: Lernbericht, offen: Boolean, antworten: (Lernbericht, LernPosten, String) -> Unit) {
    val f = LocalFarben.current
    var auf by remember(b.id) { mutableStateOf(offen) }
    val (titel, farbe) = when (b.zustand) {
        Lernlauf.LAEUFT -> "Lernlauf läuft" to f.primaer
        Lernlauf.UEBERSPRUNGEN -> "Übersprungen" to f.textLeise
        Lernlauf.FEHLER -> "Fehlgeschlagen" to f.gefahr
        else -> when {
            b.aenderungen > 0 -> "Gedächtnis aktualisiert" to f.erfolg
            b.pruefung -> "Geprüft – keine Änderungen nötig" to f.text
            else -> "Gelesen – nichts Neues zu lernen" to f.text
        }
    }
    val zeit = Instant.ofEpochMilli(b.start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE d.M., HH:mm", Locale.GERMAN))
    val dauer = if (b.ende > b.start) ((b.ende - b.start) / 1000).let { if (it < 90) " · $it Sek." else " · ${it / 60} Min." } else ""
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).animateContentSize().antippen { auf = !auf }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titel, Modifier.weight(1f), color = farbe, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (b.zustand == Lernlauf.LAEUFT) Kern(20.dp, f.primaer, aktiv = true)
        }
        Text("${b.ausloeser.replaceFirstChar { it.uppercase() }} · $zeit$dauer" + (if (b.pruefung) " · mit Aktualitätsprüfung" else " · nur neue Gespräche"),
            color = f.textSchwach, fontSize = 12.sp)
        val zeile = when (b.zustand) {
            Lernlauf.LAEUFT -> b.schritt.replaceFirstChar { it.uppercase() } + " …"
            Lernlauf.UEBERSPRUNGEN, Lernlauf.FEHLER -> b.grund
            else -> b.zusammenfassung().substringAfter(": ", "").ifEmpty { if (b.pruefung) "Alles Gemerkte ist noch aktuell." else "Keine neuen Wechsel in ruhenden Gesprächen." }
        }
        if (zeile.isNotBlank()) Text(zeile, color = f.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
        val details = b.neu.size + b.aktualisiert.size + b.entfernt.size + b.bestaetigen.size + b.geprueft.size + b.hinweise.size + b.fehler.size
        if (details > 0) Text(if (auf) "Einzelheiten ausblenden" else "Einzelheiten anzeigen", color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        if (auf) {
            Gruppe("Neu aufgenommen", b.neu, b, antworten)
            Gruppe("Aktualisiert", b.aktualisiert, b, antworten)
            Gruppe("Entfernt", b.entfernt, b, antworten)
            Gruppe("Braucht deine Bestätigung", b.bestaetigen, b, antworten)
            Gruppe("Geprüft, bleibt unverändert", b.geprueft, b, antworten)
            if (b.fehler.isNotEmpty()) Texte("Fehler", b.fehler, f.gefahr)
            if (b.hinweise.isNotEmpty()) Texte("Hinweise", b.hinweise, f.textLeise)
        }
    }
}

@Composable
private fun Gruppe(titel: String, posten: List<LernPosten>, b: Lernbericht, antworten: (Lernbericht, LernPosten, String) -> Unit) {
    if (posten.isEmpty()) return
    val f = LocalFarben.current
    Text(titel, color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    posten.forEach { p -> PostenZeile(p, b, antworten) }
}

@Composable
private fun PostenZeile(p: LernPosten, b: Lernbericht, antworten: (Lernbericht, LernPosten, String) -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(p.art + (if (p.status.isNotEmpty()) " · ${p.status}" else ""), color = f.textSchwach, fontSize = 11.sp)
        Text(p.text, color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
        if (p.vorher.isNotEmpty()) Text("Vorher: ${p.vorher}", color = f.textLeise, fontSize = 12.sp, lineHeight = 17.sp)
        if (p.grund.isNotEmpty()) Text(p.grund, color = f.textLeise, fontSize = 12.sp, lineHeight = 17.sp)
        if (p.beleg.isNotEmpty()) Text("Beleg: ${p.beleg}", color = f.textSchwach, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = FontFamily.Monospace)
        if (p.erledigt.isNotEmpty()) Text("✓ ${p.erledigt.replaceFirstChar { it.uppercase() }}", color = f.erfolg, fontSize = 12.sp)
        else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
            when (p.aktion) {
                "wiederherstellen" -> KleinerKnopf("Wiederherstellen", f.primaer) { antworten(b, p, "wiederherstellen") }
                "entfernen" -> {
                    KleinerKnopf("Entfernen", f.gefahr) { antworten(b, p, "entfernen") }
                    KleinerKnopf("Behalten", f.primaer) { antworten(b, p, "behalten") }
                }
            }
        }
    }
}

@Composable
private fun KleinerKnopf(text: String, farbe: Color, aktion: () -> Unit) {
    Text(text, color = farbe, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.antippen(aktion = aktion).padding(vertical = 6.dp, horizontal = 2.dp))
}

@Composable
private fun Texte(titel: String, zeilen: List<String>, farbe: Color) {
    val f = LocalFarben.current
    Text(titel, color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    zeilen.forEach { Text("• $it", color = farbe, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp)) }
}
