package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.archiviert
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen

/**
 * Einstellungen → Archiv: Gespräche über 30 Tage und Programmier-Sessions über 10 Tage (ohne Stern). Antippen öffnet das Gespräch;
 * wer darin weiterschreibt, holt es zurück in die Leiste. Jarvis durchsucht das Archiv mit gedaechtnis_suchen (Quelle „archiv“).
 */
@Composable
internal fun ArchivReiter(vm: AppViewModel) {
    val f = LocalFarben.current
    var suche by remember { mutableStateOf("") }
    // Seitenweise: Die Einstellungen scrollen als Ganzes, hunderte Einträge auf einmal würden ruckeln.
    var zeigen by remember { mutableIntStateOf(50) }
    val projektIds = remember(vm.projekte) { vm.projekte.mapTo(HashSet()) { it.id } }
    // Das offene Gespräch steht in der Leiste, nicht zugleich hier.
    val archiv = remember(vm.sitzungen, projektIds, vm.sitzungId) { val jetzt = System.currentTimeMillis(); vm.sitzungen.filter { it.id != vm.sitzungId && it.archiviert(jetzt, projektIds) } }
    val treffer = remember(archiv, suche) { if (suche.isBlank()) archiv else archiv.filter { it.titel.contains(suche.trim(), ignoreCase = true) } }
    Bereich("Archiv der Gespräche", aufklappbar = false) {
        Text(
            "Gespräche, die älter als 30 Tage sind, und Programmier-Sessions, die älter als 10 Tage sind, stehen hier statt in der Seitenleiste. " +
                "Favoriten mit Stern bleiben immer in der Leiste, und eine neue Nachricht oder Meldung holt ein Gespräch zurück. " +
                "Sag zum Beispiel „Schau mal ins Archiv, findest du etwas über …“, dann sucht Jarvis darin.",
            color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Eingabe(suche, { suche = it; zeigen = 50 }, "Im Titel suchen", Modifier.fillMaxWidth().padding(top = 10.dp))
        if (treffer.isEmpty()) Text(if (archiv.isEmpty()) "Das Archiv ist leer." else "Kein Titel passt.", Modifier.padding(top = 12.dp), color = f.textLeise, fontSize = 14.sp)
        Column(Modifier.padding(top = 8.dp)) {
            treffer.take(zeigen).forEach { s ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .antippen { vm.sitzungOeffnen(s.id); vm.reiter = Reiter.JARVIS }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(s.titel.ifBlank { "Gespräch" }, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text((if (s.programmierung) "Programmierung · " else "") + zeitKurz(s.geaendert) + " · " + if (s.anzahl == 1) "1 Nachricht" else "${s.anzahl} Nachrichten", color = f.textLeise, fontSize = 12.sp)
                }
            }
        }
        if (treffer.size > zeigen) Knopf("Weitere ${minOf(50, treffer.size - zeigen)} anzeigen", Modifier.fillMaxWidth().padding(top = 6.dp), haupt = false) { zeigen += 50 }
    }
}
