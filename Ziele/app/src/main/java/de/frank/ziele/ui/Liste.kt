package de.frank.ziele.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d

@Composable
fun ListeBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val ziele by vm.ziele.collectAsState()
    val vorschlaege by vm.vorschlaege.collectAsState()
    val laeuft by vm.auswertungStand.collectAsState()
    val nachZiel = vorschlaege.groupBy { it.zielId }
    val aktiv = ziele.filter { it.zustand == ZielStatus.AKTIV }
    val pausiert = ziele.filter { it.zustand == ZielStatus.PAUSIERT }
    val erreicht = ziele.filter { it.zustand == ZielStatus.ERREICHT }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ziele", color = f.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when (aktiv.size) { 0 -> "Was willst du erreichen?"; 1 -> "Ein Ziel in Arbeit"; else -> "${aktiv.size} Ziele in Arbeit" } +
                                if (erreicht.isNotEmpty()) " · ${erreicht.size} erreicht" else "",
                            color = f.textLeise, fontSize = 14.sp,
                        )
                    }
                    RundKnopf(if (vm.sprichtId == "alle") Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, "Ziele vorlesen") { vm.alleZieleVorlesen() }
                    RundKnopf(Icons.Rounded.EditNote, "Kontextdatei") { vm.zeige(Bildschirm.Kontext) }
                    ModusKnopf(vm)
                    RundKnopf(Icons.Rounded.Settings, "Einstellungen") { vm.zeige(Bildschirm.Einstellungen) }
                }
            }
            if (ziele.isEmpty()) item { LeereListe(vm) }
            items(aktiv, key = { it.id }) { z -> ZielKarte(vm, z, nachZiel[z.id].orEmpty(), laeuft[z.id]) }
            if (pausiert.isNotEmpty()) {
                item { Abschnitt("Pausiert") }
                items(pausiert, key = { it.id }) { z -> ZielKarte(vm, z, nachZiel[z.id].orEmpty(), laeuft[z.id]) }
            }
            if (erreicht.isNotEmpty()) {
                item { Abschnitt("Erreicht 🏆") }
                items(erreicht, key = { it.id }) { z -> ZielKarte(vm, z, nachZiel[z.id].orEmpty(), laeuft[z.id]) }
            }
        }
        // Unten rechts: einsprechen (Haupt-Weg) und tippen.
        Row(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(52.dp).glas(f, 99.dp, 1.4f, f.flaecheStark).antippen { vm.neuesZiel(mitMikro = false) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "Neues Ziel eintippen", tint = f.primaer, modifier = Modifier.size(28.dp))
            }
            MikroKnopf(Aufnahme.AUS, 68.dp) { vm.neuesZiel(mitMikro = true) }
        }
    }
}

@Composable
private fun Abschnitt(titel: String) {
    val f = LocalFarben.current
    Text(titel.uppercase(), color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 24.dp, top = 10.dp))
}

@Composable
private fun LeereListe(vm: AppViewModel) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().padding(16.dp).glas(f, erhoeht = 1.2f).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎯", fontSize = 40.sp)
        Text("Dein erstes Ziel", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tippe unten rechts aufs Mikrofon und sprich einfach, was du erreichen willst und warum. Die App ordnet es für dich. " +
                "Danach recherchiert die Auswertung, wie du das Ziel am besten erreichst, und schlägt dir die wichtigsten Aufgaben vor. " +
                "Mit dem Plus landet jede Aufgabe direkt in Geniale Aufgaben.",
            color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Text(
            "Tipp: Sprich in der Kontextdatei (Notizsymbol oben) ein, was die KI über dich wissen soll – Alltag, Dienstplan, Vorlieben.",
            color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ZielKarte(vm: AppViewModel, z: Ziel, vorschlaege: List<Vorschlag>, schritt: String?) {
    val f = LocalFarben.current
    val aktiv = vorschlaege.filter { !it.verworfen }
    val notwendig = aktiv.filter { it.gruppe == Kategorie.NOTWENDIG }
    val anteil = if (notwendig.isEmpty()) 0f else notwendig.count { it.erledigt } / notwendig.size.toFloat()
    val gedimmt = z.zustand != ZielStatus.AKTIV
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .glas(f, erhoeht = if (gedimmt) 0.5f else 1.2f, toenung = if (z.zustand == ZielStatus.ERREICHT) f.erfolg else f.primaer).animateContentSize()
            .antippen { vm.zeige(Bildschirm.Detail(z.id)) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).glas(f, 16.dp, 0.6f, f.primaer.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Text(z.symbol, fontSize = 26.sp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(z.titel, color = if (gedimmt) f.textLeise else f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    z.frist?.let { Etikett(fristText(it, vm.heute), if (it < vm.heute && z.zustand == ZielStatus.AKTIV) f.gefahr else f.primaer) }
                    if (z.zustand != ZielStatus.AKTIV) Etikett(z.zustand.anzeige)
                    if (notwendig.isNotEmpty()) Etikett("${notwendig.count { it.erledigt }}/${notwendig.size} notwendig")
                }
            }
            if (notwendig.isNotEmpty()) Fortschrittsring(anteil, 48.dp)
        }
        when {
            schritt != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Text(schritt, color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
            }
            z.strategie.isNotBlank() -> Text(z.strategie, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            z.beschreibung.isNotBlank() -> Text(z.beschreibung, color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        // Jedes Ziel hat seinen eigenen Auswertungsknopf.
        if (schritt == null && z.zustand != ZielStatus.ERREICHT) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.height(36.dp).knopf3d(f.primaer, f.tertiaer, 99.dp, f.dunkel).antippen { vm.auswerten(z.id) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = f.aufPrimaer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (z.ausgewertetAm == null) "Auswerten" else "Neu auswerten", color = f.aufPrimaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                // Füllt freie Plätze (abgelehnt oder ausgeblendet) mit neuen Vorschlägen, ohne eine ganze neue Auswertung.
                if (z.ausgewertetAm != null) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.height(36.dp).glas(f, 99.dp, 0.8f, f.flaecheStark).antippen { vm.nachfuellen(z.id) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = f.primaer, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Neue Aufgaben", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                val offen = aktiv.count { it.aufgabeId == null }
                if (aktiv.isNotEmpty()) Text(if (offen > 0) "$offen offen" else "alles übernommen", color = f.textSchwach, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
