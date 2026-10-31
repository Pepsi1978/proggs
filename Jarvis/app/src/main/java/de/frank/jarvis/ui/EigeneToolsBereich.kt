package de.frank.jarvis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.HelferFaehigkeit
import de.frank.jarvis.faehigkeit.MacJarvisFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.helfer.BauStand
import de.frank.jarvis.helfer.Helfer
import de.frank.jarvis.helfer.HelferAblauf
import de.frank.jarvis.helfer.HelferKatalog
import de.frank.jarvis.helfer.HelferZustand
import de.frank.jarvis.helfer.RechercheModelle
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Einstellungen → Eigene Tools: Inhaltsverzeichnis, je Helfer ein Detailbereich, oben der Pausenschalter. Selten Gebrauchtes ist eingeklappt. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EigeneToolsBereich(vm: AppViewModel) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val stand by HelferKatalog.stand.collectAsState()
    val bauStand by HelferAblauf.stand.collectAsState()
    val eigene = remember(stand) { HelferKatalog.alle(c, mitPapierkorb = true) }
    val macListe = remember(stand) { HelferKatalog.macHelfer(c) }
    val bauten = remember(bauStand) { HelferAblauf.auftraege(c) }
    val macDa by produceState<Boolean?>(null, stand) {
        value = withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Register.alle(c).filterIsInstance<MacJarvisFaehigkeit>().first().macErreichbar() }.getOrDefault(false) }
    }
    var offen by rememberSaveable { mutableStateOf("") }
    val e = vm.einstellungen
    val auswahl = (eigene + macListe).firstOrNull { it.kennung == offen }

    if (auswahl != null) {
        androidx.activity.compose.BackHandler { offen = "" }
        HelferDetail(vm, auswahl, macDa) { offen = "" }
        return
    }

    Column(Modifier.fillMaxWidth().glas(f, 18.dp, erhoeht = 0.3f).padding(14.dp)) {
        Zeile("Automatik", if (e.helferAutomatik) "An" else "Pausiert", if (e.helferAutomatik) f.erfolg else f.gefahr)
        Text("Fehlt Jarvis für eine Aufgabe eine Fähigkeit, lässt er sich selbst einen Helfer bauen, prüft ihn und macht deine Aufgabe damit fertig. Pausiert baut er nichts Neues; fertige Helfer bleiben nutzbar.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("An", aktiv = e.helferAutomatik) { e.helferAutomatik = true; HelferAblauf.nachPause(c) }
            Chip("Pause", aktiv = !e.helferAutomatik) { e.helferAutomatik = false }
        }
    }

    val laufend = bauten.filter { it.optString("stand") in BauStand.OFFEN || it.optString("stand") == BauStand.BLOCKIERT }.take(5)
    if (laufend.isNotEmpty()) Bereich("Gerade in Arbeit", aufklappbar = false) {
        laufend.forEach { b ->
            Text(b.optString("name"), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Text(BauStand.text(b.optString("stand")) + " · " + b.optString("schritt"), color = f.textLeise, fontSize = 13.sp)
            if (b.optString("stand") in BauStand.OFFEN) Knopf("Abbrechen", Modifier.padding(top = 6.dp), haupt = false) { vm.meldung = HelferAblauf.abbrechen(c, b.optString("id")) }
        }
    }

    val aufHandy = eigene.filter { it.zustand != HelferZustand.PAPIERKORB }
    Bereich("Auf dem Handy (${aufHandy.size})", aufklappbar = false) {
        if (aufHandy.isEmpty()) Text("Noch keine Helfer. Sie entstehen von selbst, wenn eine Aufgabe sie braucht.", color = f.textSchwach, fontSize = 14.sp)
        aufHandy.forEach { h -> HelferZeile(h, macDa) { offen = h.kennung } }
    }
    Bereich("Auf dem Mac (${macListe.size})", aufklappbar = false) {
        val gesehen = HelferKatalog.macZuletztGesehen(c)
        val zeit = if (gesehen == 0L) "noch nie gemeldet" else "Stand vom " + java.text.SimpleDateFormat("dd.MM., HH:mm", java.util.Locale.GERMANY).format(java.util.Date(gesehen)) + " Uhr"
        Text((if (macDa == true) "Mac erreichbar · " else "Mac gerade nicht erreichbar · ") + zeit, color = if (macDa == true) f.erfolg else f.textLeise, fontSize = 13.sp)
        macListe.forEach { h -> HelferZeile(h, macDa) { offen = h.kennung } }
    }
    val papierkorb = eigene.filter { it.zustand == HelferZustand.PAPIERKORB }
    if (papierkorb.isNotEmpty()) Bereich("Papierkorb (${papierkorb.size})") { papierkorb.forEach { h -> HelferZeile(h, macDa) { offen = h.kennung } } }

    Bereich("Recherche und Grenzen") {
        val modell = RechercheModelle.aus(e.rechercheModell)
        Unterzeile("Modell für gründliche Recherchen")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RechercheModelle.ERLAUBT.forEach { m -> Chip(m.label, aktiv = m == modell) { e.rechercheModell = m.apiId; e.rechercheEffort = m.normalizeEffort(de.frank.jarvis.auth.ReasoningEffort.fromLabel(e.rechercheEffort)).apiValue } }
        }
        val verfuegbar = remember(modell) { de.frank.jarvis.auth.CodexAuthManager(c).kannDenken(modell) }
        if (!verfuegbar) Text("${modell.label} ist gerade nicht verfügbar (Anmeldung oder Schlüssel fehlt). Jarvis nimmt dann kein kleineres Modell, sondern gibt die Recherche an den Programmierer.", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Unterzeile("Denkstufe")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            modell.supportedEfforts.forEach { s -> Chip(s.titel(modell), aktiv = e.rechercheEffort == s.apiValue) { e.rechercheEffort = s.apiValue } }
        }
        Unterzeile("Parallele Rechercheagenten höchstens")
        ZahlChips(listOf(1, 3, 5, 7), e.rechercheParallel) { e.rechercheParallel = it }
        Unterzeile("Werkzeugbauten gleichzeitig höchstens")
        ZahlChips(listOf(1, 2, 3), e.helferParallel) { e.helferParallel = it }
        Unterzeile("Werkzeugbauten je Tag höchstens")
        ZahlChips(listOf(3, 10, 30), e.helferProTag) { e.helferProTag = it }
        Text("Der Programmierer ist Claude Opus 5.5 über die vorhandenen Wege. Jeder neue Helfer wird automatisch getestet und gründlich geprüft (höchstens 100 Befunde). Kosten, neue Konten und zusätzliche Zugriffe brauchen immer deine Freigabe; Schweigen ersetzt sie nie.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZahlChips(werte: List<Int>, aktuell: Int, setzen: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { werte.forEach { n -> Chip(n.toString(), aktiv = aktuell == n) { setzen(n) } } }
}

@Composable
private fun HelferZeile(h: Helfer, macDa: Boolean?, oeffnen: () -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().clickable(onClick = oeffnen).padding(vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.name, Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            val nichtDa = h.ort == "mac" && h.einsatzbereit && macDa != true
            Text(if (nichtDa) "Mac aus" else HelferZustand.text(h.zustand, h.aktiv), color = when {
                nichtDa -> f.textLeise
                h.einsatzbereit -> f.erfolg
                h.zustand == HelferZustand.FEHLERHAFT -> f.gefahr
                h.zustand == HelferZustand.WARTET_AUF_FREIGABE -> f.primaer
                else -> f.textLeise
            }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(h.zweck, color = f.textLeise, fontSize = 13.sp, maxLines = 2)
    }
}

@Composable
private fun HelferDetail(vm: AppViewModel, h: Helfer, macDa: Boolean?, zurueck: () -> Unit) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var loeschenFrage by rememberSaveable { mutableStateOf(false) }
    fun aktion(a: String) = scope.launch {
        val r = Register.alle(c).filterIsInstance<HelferFaehigkeit>().first().verwalten(h.kennung, a)
        vm.meldung = r.text
    }
    Knopf("Zurück zur Übersicht", Modifier.fillMaxWidth(), haupt = false, aktion = zurueck)
    Bereich(h.name, aufklappbar = false) {
        val nichtDa = h.ort == "mac" && macDa != true
        Zeile("Zustand", HelferZustand.text(h.zustand, h.aktiv), if (h.einsatzbereit) f.erfolg else f.textLeise)
        if (nichtDa) Zeile("Mac", "Gerade nicht erreichbar", f.textLeise)
        Zeile("Ausführungsort", if (h.ort == "mac") "Jarvis Mac OS" else "Dieses Handy", f.text)
        Zeile("Version", h.version.ifEmpty { "–" }, f.text)
        LangText("Wofür", h.zweck)
        if (h.faehigkeiten.isNotEmpty()) LangText("Fähigkeiten", h.faehigkeiten.joinToString("\n") { "• $it" })
        LangText("Aufruf", "Jarvis nutzt ihn von selbst (helfer_ausfuehren, Kennung ${h.kennung}). Du kannst auch einfach danach fragen.")
        val props = h.eingabe.optJSONObject("properties")
        if (props != null && props.length() > 0) LangText("Eingaben", props.keys().asSequence().joinToString("\n") { k -> "• $k" + props.optJSONObject(k)?.optString("description")?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty() })
        if (h.ausgabe.isNotBlank()) LangText("Ergebnis", h.ausgabe)
        if (h.voraussetzungen.isNotEmpty()) LangText("Voraussetzungen", h.voraussetzungen.joinToString("\n") { "• $it" })
        LangText("Konten und Rechte", h.rechte.alsText())
        if (h.grenzen.isNotEmpty()) LangText("Bekannte Grenzen", h.grenzen.joinToString("\n") { "• $it" })
        if (h.brauchtAppUpdate) LangText("Hinweis", "Dieser Helfer braucht ein App-Update.")
        val zeit = h.pruefung.optLong("zeit").takeIf { it > 0 }?.let { java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMANY).format(java.util.Date(it * 1000)) + " Uhr: " }.orEmpty()
        LangText("Letzte Prüfung", zeit + h.pruefung.optString("ergebnis", "keine"))
        if (h.review.has("befunde")) LangText("Review", "${h.review.optInt("befunde")} Befunde, ${h.review.optInt("kritisch_offen")} kritisch offen. ${h.review.optString("zusammenfassung")}")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (h.zustand == HelferZustand.WARTET_AUF_FREIGABE && h.ort != "mac") {
            Text("Dieser Helfer möchte: ${h.rechte.alsText()}. Ohne deine Freigabe läuft er nicht.", color = f.text, fontSize = 14.sp)
            Knopf("Freigeben", Modifier.fillMaxWidth()) { HelferKatalog.freigeben(c, h.kennung); HelferAblauf.nachFreigabe(c, h.kennung) }
        }
        if (h.zustand == HelferZustand.WARTET_AUF_FREIGABE && h.ort == "mac") Text("Die Freigabe erteilst du am Mac unter Einstellungen → Eigene Tools.", color = f.text, fontSize = 14.sp)
        if (h.zustand == HelferZustand.PAPIERKORB) {
            Knopf("Wiederherstellen", Modifier.fillMaxWidth(), haupt = false) { aktion("wiederherstellen") }
            Knopf("Endgültig löschen …", Modifier.fillMaxWidth(), haupt = false, farbe = f.gefahr) { loeschenFrage = true }
            if (loeschenFrage) {
                val geteilt = HelferKatalog.geteilteKonten(c, h)
                Text("Gelöscht werden Programm, alle Fassungen und der eigene Speicher dieses Helfers. Konten und Schlüssel bleiben erhalten" +
                    (if (geteilt.isEmpty()) "." else ", auch die von anderen Helfern mitgenutzten: ${geteilt.joinToString(", ")}.") + " Andere Helfer hängen nicht von diesem ab.", color = f.text, fontSize = 14.sp)
                Knopf("Ja, endgültig löschen", Modifier.fillMaxWidth(), farbe = f.gefahr) { HelferKatalog.endgueltigLoeschen(c, h.kennung); loeschenFrage = false; zurueck() }
            }
        } else {
            Knopf(if (h.aktiv) "Deaktivieren" else "Aktivieren", Modifier.fillMaxWidth(), haupt = false) { aktion(if (h.aktiv) "deaktivieren" else "aktivieren") }
            if (h.ort != "mac") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Knopf("Nach oben", Modifier.weight(1f), haupt = false) { HelferKatalog.verschieben(c, h.kennung, -1) }
                    Knopf("Nach unten", Modifier.weight(1f), haupt = false) { HelferKatalog.verschieben(c, h.kennung, 1) }
                }
                if (HelferKatalog.hatVorherigeFassung(c, h.kennung)) Knopf("Letzte geprüfte Fassung", Modifier.fillMaxWidth(), haupt = false) { aktion("rueckrollen") }
            }
            Knopf("In den Papierkorb", Modifier.fillMaxWidth(), haupt = false) { aktion("papierkorb"); zurueck() }
        }
    }
}

@Composable
private fun LangText(titel: String, text: String) {
    val f = LocalFarben.current
    Unterzeile(titel)
    Text(text, color = f.text, fontSize = 14.sp, lineHeight = 19.sp)
}
