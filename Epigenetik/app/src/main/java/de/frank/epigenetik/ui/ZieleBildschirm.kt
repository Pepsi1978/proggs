package de.frank.epigenetik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.Zielkatalog
import de.frank.epigenetik.data.normalisiere
import de.frank.epigenetik.data.objekte
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import org.json.JSONObject

@Composable
fun ZieleBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val ziele = alle.filter { it.typEnum == Typ.ZIEL }.sortedByDescending { it.erstellt }
    val vorhanden = ziele.map { it.schluessel }.toSet()

    Seitenliste(vm, "Ziele", "Was willst du epigenetisch erreichen?") {
        item(key = "eingabe") {
            Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.3f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Eigenes Ziel einsprechen oder eintippen", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).heightIn(min = 52.dp).glas(f, 18.dp, 0f, f.flaecheStark).padding(14.dp)) {
                        if (vm.zielEingabe.isEmpty()) Text("z. B. Gelenke gesund halten bis ins Alter", color = f.textSchwach, fontSize = 15.sp)
                        BasicTextField(vm.zielEingabe, vm::zielEingabeAendern, textStyle = TextStyle(color = f.text, fontSize = 15.sp), cursorBrush = SolidColor(f.primaer), modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.width(10.dp))
                    MikroKnopf(vm, DiktatZiel.ZIEL)
                }
                HauptKnopf("Ziel auswerten", Icons.Rounded.ArrowForward, Modifier.fillMaxWidth()) { vm.zielAbsenden() }
                Text("Oder eins der häufigen Ziele antippen:", color = f.textLeise, fontSize = 12.sp)
                ChipFluss {
                    Zielkatalog.alle.filter { normalisiere(it.name) !in vorhanden }.forEach { z ->
                        Chip("${z.emoji} ${z.name}", false) { vm.recherchiere(Typ.ZIEL, listOf(z.name)) }
                    }
                }
            }
        }
        if (ziele.isEmpty()) item(key = "leer") {
            LeerHinweis("🎯", "Noch keine Ziele", "Jedes Ziel wird in epigenetische Schalter übersetzt – mit den besten Nahrungsergänzungsmitteln, Lebensmitteln und Gewohnheiten dafür.")
        }
        items(ziele, key = { it.id }) { r ->
            val o = runCatching { JSONObject(r.json) }.getOrNull()
            val emoji = o?.optString("emoji")?.takeIf { it.isNotBlank() } ?: Zielkatalog.alle.firstOrNull { normalisiere(it.name) == r.schluessel }?.emoji ?: "🎯"
            val top = o?.optJSONArray("supplements").objekte().take(3).joinToString(" · ") { it.optString("name") }
            val schalter = o?.optJSONArray("schalter").objekte().take(4).joinToString(" · ") { Schalterkatalog.zu(it.optString("id"))?.name ?: it.optString("id") }
            Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 0.9f).antippen { vm.oeffne(r.id) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 26.sp)
                    Text(o?.optString("titel")?.ifBlank { null } ?: r.name, color = f.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                }
                if (r.offen) LaufBalken()
                when {
                    r.offen -> Text("Wird ausgewertet …", color = f.textLeise, fontSize = 12.sp)
                    r.fehler.isNotBlank() && !r.fertig -> Text(r.fehler, color = f.gefahr, fontSize = 12.sp)
                    else -> {
                        if (schalter.isNotBlank()) Text("Schalter: $schalter", color = f.textLeise, fontSize = 12.sp, maxLines = 2)
                        if (top.isNotBlank()) Text("Beste Mittel: $top", color = f.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                    }
                }
            }
        }
    }
}
