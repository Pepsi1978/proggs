package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.OpenLauncherEfforts
import de.frank.jarvis.faehigkeit.OpenLauncherKatalog
import de.frank.jarvis.faehigkeit.ProgrammiererVorlage
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/**
 * Knopf „OpenLauncher“ des Programmierbuttons: bildschirmfüllende Modellauswahl mit Franks Windows-Angebot, danach Effort,
 * Profil, Modus und CLI. Eine Ebene über der App wie die Prompt-Ansicht. Das Raster passt die Spaltenzahl der Breite an und
 * scrollt in jeder Höhe: So bleibt alles auf dem Fold 8 zugeklappt und aufgeklappt, hoch und quer erreichbar; die Wahl übersteht Falten und Drehen.
 */
@Composable
fun OpenLauncherWahlEbene(vm: AppViewModel) {
    if (!vm.openLauncherWahlOffen) return
    val f = LocalFarben.current
    val context = LocalContext.current
    var versuch by remember { mutableStateOf(0) }
    val geladen by produceState<Result<OpenLauncherKatalog.Angebot>?>(null, versuch) {
        value = null
        value = try { Result.success(OpenLauncherKatalog.angebot(context)) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
    }
    val angebot = geladen?.getOrNull()
    var modellId by rememberSaveable { mutableStateOf("") }
    var effort by rememberSaveable { mutableStateOf("") }
    var profil by rememberSaveable { mutableStateOf("") }
    var modus by rememberSaveable { mutableStateOf("") }
    // Bereiche, die Frank gegenüber dem Anfangszustand umgeschaltet hat (Gruppen-Id, Zeilen).
    var umgeschaltet by rememberSaveable { mutableStateOf("") }

    val modell = angebot?.modelle?.firstOrNull { "${it.anbieter}\t${it.slug}" == modellId }
    val efforts = if (modell != null) angebot.efforts(modell) else emptyList()
    val clis = modell?.clis.orEmpty()
    val stufe = when {
        modell == null -> 0
        efforts.isNotEmpty() && effort.isEmpty() -> 1
        profil.isEmpty() -> 2
        modus.isEmpty() -> 3
        else -> 4
    }
    fun fertig(m: OpenLauncherKatalog.Modell, gewaehlterModus: String, cli: String) = vm.openLauncherWahlUebernehmen(ProgrammiererVorlage.FreieWahl(
        m.name, m.anbieterName, m.slug,
        if (effort.isEmpty()) ProgrammiererVorlage.OHNE_EFFORT else OpenLauncherEfforts.name(effort),
        OpenLauncherEfforts.PROFILE.first { it.wert == profil }.name,
        OpenLauncherEfforts.MODI.first { it.wert == gewaehlterModus }.name,
        OpenLauncherKatalog.CLI_NAMEN.getValue(cli),
    ))
    val zurueck: () -> Unit = {
        when (stufe) {
            0 -> vm.openLauncherWahlSchliessen()
            1 -> modellId = ""
            2 -> if (efforts.isEmpty()) modellId = "" else effort = ""
            3 -> profil = ""
            else -> modus = ""
        }
    }
    BackHandler(onBack = zurueck)

    Box(Modifier.fillMaxSize().background(f.hgOben).pointerInput(Unit) { detectTapGestures { } }.windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).antippen(aktion = zurueck), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text) }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(listOf("OpenLauncher: Modell", "Effort", "Profil", "Modus", "CLI")[stufe], color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (modell != null) Text(
                        listOfNotNull(modell.name, effort.takeIf { it.isNotEmpty() }?.let(OpenLauncherEfforts::name),
                            OpenLauncherEfforts.PROFILE.firstOrNull { it.wert == profil }?.name, OpenLauncherEfforts.MODI.firstOrNull { it.wert == modus }?.name).joinToString(" · "),
                        color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.size(48.dp).antippen { vm.openLauncherWahlSchliessen() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
            }
            when {
                geladen == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                angebot == null -> Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(geladen?.exceptionOrNull()?.message ?: "Das Angebot von OpenLauncher ist gerade nicht lesbar.", color = f.gefahr, fontSize = 15.sp, modifier = Modifier.padding(bottom = 14.dp))
                    Knopf("Erneut versuchen") { versuch++ }
                }
                // Je Stufe ein eigenes Raster: Jede beginnt oben.
                else -> key(stufe) {
                    LazyVerticalGrid(
                        GridCells.Adaptive(minSize = 270.dp), Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (stufe) {
                            0 -> OpenLauncherKatalog.sortiert(angebot.gruppen).forEach { g ->
                                val zeilen = umgeschaltet.split('\n')
                                val offen = OpenLauncherKatalog.anfangsOffen(g) != (g.id in zeilen)
                                item(key = "g:${g.id}", span = { GridItemSpan(maxLineSpan) }) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).antippen {
                                        umgeschaltet = (if (g.id in zeilen) zeilen - g.id else zeilen + g.id).joinToString("\n")
                                    }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(g.titel, Modifier.weight(1f), color = f.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        Text("${g.modelle.size}", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                                        Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen", tint = f.textLeise, modifier = Modifier.rotate(if (offen) 0f else -90f))
                                    }
                                }
                                if (offen) items(g.modelle, key = { "m:${g.id}:${it.slug}" }) { m ->
                                    WahlZeile(m.name) {
                                        // Eine frühere Wahl gilt nach einem Modellwechsel nicht weiter.
                                        effort = ""; profil = ""; modus = ""
                                        modellId = "${m.anbieter}\t${m.slug}"
                                    }
                                }
                            }
                            1 -> items(efforts, key = { it }) { e -> WahlZeile(OpenLauncherEfforts.name(e), OpenLauncherEfforts.beschreibung(e)) { effort = e } }
                            2 -> items(OpenLauncherEfforts.PROFILE, key = { it.wert }) { p -> WahlZeile(p.name, p.beschreibung) { profil = p.wert } }
                            3 -> items(OpenLauncherEfforts.MODI, key = { it.wert }) { w ->
                                WahlZeile(w.name, w.beschreibung) {
                                    // Nur ein CLI im Angebot: keine weitere Frage, die Auswahl ist fertig.
                                    if (clis.size == 1 && modell != null) fertig(modell, w.wert, clis.single()) else modus = w.wert
                                }
                            }
                            else -> items(clis, key = { it }) { c -> WahlZeile(OpenLauncherKatalog.CLI_NAMEN.getValue(c)) { if (modell != null) fertig(modell, modus, c) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WahlZeile(titel: String, beschreibung: String = "", aktion: () -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).glas(f, 16.dp, erhoeht = 0.3f).antippen(aktion = aktion).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center) {
        Text(titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (beschreibung.isNotEmpty()) Text(beschreibung, color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
