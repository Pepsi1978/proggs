package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.Funktionen
import de.frank.jarvis.gedaechtnis.Funktionsdatei
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ob die Funktionsübersicht offen ist (Vollbild über allem, auch über dem Live-Gedächtnis). */
object FunktionenAnsicht {
    var offen by mutableStateOf(false)
    // Der Entwurf lebt hier statt im Bildschirm: Zurück-Geste oder „Hey Jarvis“ schließen nur die Ansicht, der Entwurf bleibt bis zum Speichern oder Abbrechen.
    var bearbeite by mutableStateOf(false)
    var entwurf by mutableStateOf("")
    /** Die Fassung, von der der Entwurf ausging: Revision, Text und geprüfter Stand. */
    var ausRevision by mutableIntStateOf(-1)
    var basis by mutableStateOf("")
    var ausGeprueft by mutableIntStateOf(0)
}

/**
 * Die Funktionsübersicht zum Lesen und Bearbeiten: was Jarvis in der installierten Version kann. Oben der Stand (geprüfte
 * und installierte Version, noch nicht eingearbeitete Versionen, Hinweise der Pflege), darunter der Text. Speichern gelingt
 * nur, wenn seitdem niemand sonst gespeichert hat; sonst bleibt der Entwurf offen, ein zweites Speichern überschreibt bewusst.
 */
@Composable
fun FunktionenBildschirm(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val app = LocalContext.current.applicationContext
    val bereich = rememberCoroutineScope()
    BackHandler { FunktionenAnsicht.offen = false }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Funktionen.lade(app) } }
    val stand by Funktionen.stand.collectAsState()
    val pflege by Funktionen.pflegeText.collectAsState()
    val pflegt by Funktionen.laeuft.collectAsState()
    val a = FunktionenAnsicht
    var meldung by remember { mutableStateOf("") }
    var offen by remember { mutableStateOf(emptyList<de.frank.jarvis.gedaechtnis.Versionseintrag>()) }
    // Das Versionslog liest die Platte: nicht im Oberflächen-Faden.
    LaunchedEffect(stand?.geprueftCode) {
        val code = stand?.geprueftCode ?: return@LaunchedEffect
        offen = withContext(Dispatchers.IO) { Funktionsdatei.offeneVersionen(Funktionen.versionslog(app), code, Funktionen.installiertCode) }
    }
    var vorfassung by remember { mutableStateOf(false) }
    LaunchedEffect(stand?.revision) { vorfassung = withContext(Dispatchers.IO) { Funktionen.hatVorfassung(app) } }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).antippen { FunktionenAnsicht.offen = false }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
            }
            Text("Funktionsübersicht", Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        val s = stand
        if (s == null) {
            Text("Wird geladen …", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
            return@Column
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "kopf") {
                Text(
                    "Was Jarvis in der installierten Version kann. Jarvis liest das nur bei Bedarf (funktionen_lesen), auch in ChatGPT und ohne GitHub; es steht nicht im Live-Gedächtnis. " +
                        "Nach jedem Update arbeitet Jarvis die neuen Versionen aus dem Versionslog selbst ein. Deine eigenen Zeilen ändert die Pflege nicht, sie schlägt dir Änderungen nur vor.",
                    color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item(key = "stand") {
                Karte(toenung = f.primaer) {
                    Text("Geprüft bis Jarvis ${s.geprueftName.ifEmpty { "?" }}", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Installiert: ${Funktionen.installiertName}" + (if (s.gepflegt > 0) " · gepflegt ${Funktionen.zeit(s.gepflegt)}" else "") +
                            (if (s.manuell > 0) " · von dir bearbeitet ${Funktionen.zeit(s.manuell)}" else "") + " · ${s.text.length} Zeichen",
                        color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                    if (offen.isNotEmpty()) Text("Noch nicht eingearbeitet: " + offen.joinToString(", ") { it.name } + ". Bis dahin nennt Jarvis sie beim Lesen dazu.",
                        color = f.primaer, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
                    if (pflege.isNotEmpty()) Text(pflege, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
                    if (meldung.isNotEmpty()) Text(meldung, color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    if (s.hinweise.isNotEmpty()) {
                        Text("Vorschläge der Pflege zu deinen Zeilen:", color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        s.hinweise.forEach { Text("• $it", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp)) }
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (offen.isNotEmpty() || Funktionsdatei.pflegeFaellig(s.geprueftCode, Funktionen.installiertCode))
                            Knopf(if (pflegt) "Pflegt …" else "Jetzt einarbeiten", Modifier.weight(1f), Icons.Rounded.Refresh, haupt = false) {
                                if (!pflegt) { meldung = ""; Funktionen.pflegeVonHand(app) }
                            }
                        if (vorfassung && !a.bearbeite) Knopf("Rückgängig", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo, haupt = false) {
                            bereich.launch { meldung = if (withContext(Dispatchers.IO) { Funktionen.rueckgaengig(app) }) "Vorherige Fassung wiederhergestellt." else "Keine vorherige Fassung." }
                        }
                    }
                }
            }
            item(key = "text") {
                Karte {
                    if (a.bearbeite) {
                        BasicTextField(
                            a.entwurf, { a.entwurf = it },
                            Modifier.fillMaxWidth().heightIn(min = 240.dp).glas(f, 12.dp, erhoeht = 0.2f, fuellung = f.flaecheStark).padding(12.dp),
                            textStyle = TextStyle(color = f.text, fontSize = 14.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(f.primaer),
                        )
                        Text("„## “ beginnt einen Bereich, „- “ einen Punkt. Knapp halten: so wenig Worte wie möglich, so viele wie nötig.", color = f.textSchwach, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Knopf("Speichern", Modifier.weight(1f), Icons.Rounded.Check) {
                                bereich.launch {
                                    val ok = withContext(Dispatchers.IO) { Funktionen.speichereVonHand(app, a.entwurf, a.ausRevision, a.basis, a.ausGeprueft) }
                                    if (ok) { a.bearbeite = false; meldung = "Gespeichert." }
                                    else {
                                        // Inzwischen hat die Pflege (oder ein zweites Fenster) gespeichert: Entwurf bleibt, das nächste Speichern überschreibt bewusst.
                                        // Basis und geprüfter Stand bleiben die des Entwurfs: Was die Pflege einarbeitete, wird dann erneut eingearbeitet.
                                        a.ausRevision = Funktionen.stand.value?.revision ?: a.ausRevision
                                        meldung = "Inzwischen hat Jarvis die Übersicht geändert. Dein Entwurf ist noch da; nochmal Speichern überschreibt die neue Fassung, Jarvis arbeitet seine Änderungen danach erneut ein."
                                    }
                                }
                            }
                            Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { a.bearbeite = false }
                        }
                    } else {
                        Text(s.text.trim(), color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
                        Knopf("Bearbeiten", Modifier.padding(top = 12.dp).fillMaxWidth(), Icons.Rounded.Edit, haupt = false) {
                            a.entwurf = s.text.trim(); a.basis = s.text; a.ausRevision = s.revision; a.ausGeprueft = s.geprueftCode; meldung = ""; a.bearbeite = true
                        }
                    }
                }
            }
        }
    }
}
