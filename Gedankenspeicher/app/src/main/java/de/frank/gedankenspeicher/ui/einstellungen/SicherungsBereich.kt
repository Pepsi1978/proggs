package de.frank.gedankenspeicher.ui.einstellungen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.module.sicherung.SpeicherTeil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Optische Referenz: KompassKern/EinstellungenScreen + Auswahl/Mehrfachauswahl.
 * Werte und Aufbau absichtlich aus der Referenz, wie für diesen Einbau bestellt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SicherungsBereich(anbindung: SicherungsAnbindung, dunkel: Boolean) {
    val zustand by anbindung.zustand.collectAsStateWithLifecycle()
    val gold = Color(if (dunkel) 0xFFE3B341 else 0xFF8B6914)
    val aufGold = Color(if (dunkel) 0xFF1A1408 else 0xFFFFFFFF)
    val flaeche = Color(if (dunkel) 0xFF181818 else 0xFFFFFFFF)
    val text = Color(if (dunkel) 0xFFEDE7DA else 0xFF1B1710)
    val gedaempft = Color(if (dunkel) 0xFFA79C86 else 0xFF6B6151)
    val rahmen = Color(if (dunkel) 0xFF2C2620 else 0xFFE6DFCF)
    val eingabe = Color(if (dunkel) 0xFF141414 else 0xFFF7F3EA)
    val goldGedaempft = Color(if (dunkel) 0xFFC9922B else 0xFFA9812A)
    val erhoeht = Color(if (dunkel) 0xFF282828 else 0xFFF4EFE3)
    val hintergrund = Color(if (dunkel) 0xFF121212 else 0xFFFAF7F0)
    val schema = if (dunkel) darkColorScheme(primary = gold, onPrimary = aufGold, surface = flaeche, onSurface = text,
        primaryContainer = goldGedaempft, onPrimaryContainer = aufGold, secondary = goldGedaempft, onSecondary = aufGold,
        tertiary = Color(0xFFC25E00), onTertiary = Color.White, background = hintergrund, onBackground = text,
        surfaceVariant = erhoeht, onSurfaceVariant = gedaempft, outline = rahmen, outlineVariant = rahmen,
        error = Color(0xFFFF5252), onError = Color(0xFF1A0000))
        else lightColorScheme(primary = gold, onPrimary = aufGold, surface = flaeche, onSurface = text,
            primaryContainer = goldGedaempft, onPrimaryContainer = aufGold, secondary = goldGedaempft, onSecondary = aufGold,
            tertiary = Color(0xFFA34F00), onTertiary = Color.White, background = hintergrund, onBackground = text,
            surfaceVariant = erhoeht, onSurfaceVariant = gedaempft, outline = rahmen, outlineVariant = rahmen,
            error = Color(0xFFFF5252), onError = Color.White)
    // Eine eigene Typography verhindert, dass die Inter-Schrift der Host-App hineinerbt.
    fun stil(groesse: Int, hoehe: Int, gewicht: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = FontFamily.Default, fontSize = groesse.sp, lineHeight = hoehe.sp, fontWeight = gewicht)
    MaterialTheme(colorScheme = schema, typography = Typography(
        titleMedium = stil(17, 23, FontWeight.SemiBold), bodyMedium = stil(15, 23), bodySmall = stil(13, 19),
        labelLarge = stil(14, 18, FontWeight.Medium), labelMedium = stil(12, 16, FontWeight.Medium),
    )) {
        Column(Modifier.fillMaxWidth().background(flaeche, RoundedCornerShape(14.dp))
            .border(1.dp, rahmen, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Text("Sicherung", style = MaterialTheme.typography.titleMedium, color = gold)
            Spacer(Modifier.height(8.dp))
            var offen by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text("Was gesichert wird", style = MaterialTheme.typography.labelMedium, color = gedaempft)
                Spacer(Modifier.padding(top = 3.dp))
                Box {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(eingabe, RoundedCornerShape(10.dp))
                        .border(1.dp, if (offen) gold else rahmen, RoundedCornerShape(10.dp))
                        .clickable(enabled = !zustand.laeuft) { offen = true }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${zustand.umfang.size} von ${SpeicherTeil.entries.size} aktiv", Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                            color = if (zustand.umfang.size == SpeicherTeil.entries.size) text else Color(0xFFFFB300))
                        Icon(Icons.Default.ArrowDropDown, "Auswahl öffnen", tint = gold)
                    }
                    DropdownMenu(offen, { offen = false }, Modifier.background(flaeche).fillMaxWidth(0.92f)) {
                        SpeicherTeil.entries.forEach { teil ->
                            val an = teil in zustand.umfang
                            Row(Modifier.fillMaxWidth().clickable(enabled = !zustand.laeuft) { anbindung.schalteTeil(teil, !an) }
                                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 2.dp).size(18.dp)
                                    .background(if (an) gold else Color.Transparent, RoundedCornerShape(4.dp))
                                    .border(2.dp, if (an) gold else rahmen, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                    if (an) Icon(Icons.Default.Check, null, Modifier.size(13.dp), tint = aufGold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(teil.titel, style = MaterialTheme.typography.bodyMedium, color = text)
                                    Text(teil.erklaerung, style = MaterialTheme.typography.bodySmall, color = gedaempft)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                            Text("Fertig", Modifier.clickable { offen = false }.padding(8.dp), style = MaterialTheme.typography.labelLarge, color = gold)
                        }
                    }
                }
            }
            Text("Nicht in der Sicherung: abgewählte Teile, nur beim Anbieter gespeicherte eigene Stimmen und temporäre Vorlesedateien.", style = MaterialTheme.typography.bodyMedium, color = gedaempft)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Von allein sichern", style = MaterialTheme.typography.bodyMedium, color = text)
                    Text(if (zustand.automatisch && zustand.ordner == null) "Es fehlt noch ein Ordner — bis dahin wird nichts geschrieben."
                        else "Nach jeder Änderung wird im Hintergrund gesichert, sobald zwei Minuten Ruhe war — und beim Verlassen der App sofort.",
                        style = MaterialTheme.typography.bodySmall, color = gedaempft)
                }
                Spacer(Modifier.width(8.dp))
                Switch(zustand.automatisch, anbindung::schalteAuto, enabled = !zustand.laeuft,
                    colors = SwitchDefaults.colors(checkedThumbColor = aufGold, checkedTrackColor = gold))
            }
            Spacer(Modifier.height(8.dp))
            Text(zustand.ordner ?: "Noch kein Ordner gewählt", style = MaterialTheme.typography.bodyMedium,
                color = if (zustand.ordner == null) gedaempft else gold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zustand.geprueft) { Text("✓", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF7D)); Spacer(Modifier.width(6.dp)) }
                Text(zustand.stand, style = MaterialTheme.typography.bodyMedium, color = if (zustand.geprueft) Color(0xFF4CAF7D) else gedaempft)
            }
            if (zustand.naechste.isNotBlank()) Text("Nächste Sicherung: ${zustand.naechste}", style = MaterialTheme.typography.bodyMedium, color = gedaempft)
            if (zustand.vorschau.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(gold.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(zustand.vorschau, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = text)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = anbindung::verwerfeVorschau, enabled = !zustand.laeuft) { Text("OK", color = gold) }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    ReferenzKnopf("Jetzt einspielen", gesperrt = zustand.laeuft, beiKlick = anbindung::spieleEin)
                    Spacer(Modifier.width(8.dp))
                    ReferenzKnopf("Abbrechen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::verwerfeVorschau)
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReferenzKnopf("Jetzt sichern", laedt = zustand.laeuft, beiKlick = anbindung::sichereJetzt)
                ReferenzKnopf("Ordner wählen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::waehleOrdner)
                if (zustand.ordner != null) ReferenzKnopf("Ordner vergessen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::vergissOrdner)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReferenzKnopf("Wiederherstellen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::zeigeAuswahl)
                ReferenzKnopf("Neueste wiederherstellen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::neueste)
                ReferenzKnopf("Andere Datei öffnen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::externeDatei)
            }
            if (zustand.ruecknahme) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ReferenzKnopf("↶  Rückgängig", true, gesperrt = zustand.laeuft, beiKlick = anbindung::rueckgaengig)
                    Spacer(Modifier.width(8.dp))
                    Text("Letztes Einspielen zurücknehmen", style = MaterialTheme.typography.bodySmall, color = gedaempft)
                }
            }
            if (zustand.auswahl.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Welche Sicherung?", Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.labelLarge,
                    color = Color(if (dunkel) 0xFFC9922B else 0xFFA9812A))
                zustand.auswahl.forEach { eintrag ->
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !zustand.laeuft) { anbindung.waehleSicherung(eintrag.uri) }
                        .padding(vertical = 6.dp)) {
                        Text(eintrag.name, style = MaterialTheme.typography.bodyMedium, color = gold)
                        Text("geschrieben am ${SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY).format(Date(eintrag.geaendertAm))} Uhr",
                            style = MaterialTheme.typography.bodySmall, color = gedaempft)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ReferenzKnopf("Abbrechen", true, gesperrt = zustand.laeuft, beiKlick = anbindung::verwerfeAuswahl)
            }
        }
    }
}

@Composable
private fun ReferenzKnopf(text: String, zurueckhaltend: Boolean = false, laedt: Boolean = false, gesperrt: Boolean = false, beiKlick: () -> Unit) {
    val ton = MaterialTheme.colorScheme.primary
    Row(Modifier.heightIn(min = 48.dp)
        .background(if (zurueckhaltend) ton.copy(alpha = 0.10f) else ton, RoundedCornerShape(10.dp))
        .border(1.dp, ton.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        .clickable(enabled = !laedt && !gesperrt, onClick = beiKlick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (laedt) {
            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = if (zurueckhaltend) ton else MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (zurueckhaltend) ton else MaterialTheme.colorScheme.onPrimary)
    }
}
