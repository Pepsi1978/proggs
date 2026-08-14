package de.frank.stacklabor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.frank.stacklabor.BuildConfig
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.ui.bausteine.AmpelBalken
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.LoeslichkeitsPunkte
import de.frank.stacklabor.ui.bausteine.Loeslichkeit
import de.frank.stacklabor.ui.bausteine.PlusKnopf
import de.frank.stacklabor.ui.bausteine.SymbolKnopf
import de.frank.stacklabor.ui.bausteine.glasflaeche
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.launch
import de.frank.stacklabor.daten.entitaeten.Loeslichkeit as LoeslichkeitE

/** Die Kopfleiste, die alle Unterbildschirme teilen: 57 dp, Glasfläche, Zurück-Knopf. */
@Composable
fun Kopfleiste(
    titel: String,
    steuerung: NavHostController,
    zusatz: @Composable () -> Unit = {},
) {
    val obenInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        Modifier
            .fillMaxWidth()
            .glasflaeche()
            .padding(top = obenInset)
            .height(Masse.kopfleiste)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SymbolKnopf(Icons.Rounded.ArrowBack, "Zurück") { steuerung.popBackStack() }
        Text(
            titel,
            style = Schrift.kopfzeile,
            color = Farben.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        zusatz()
    }
}

/** **B-03 — Ziel-Katalog.** Ziele einmal anlegen, überall verwenden (F-08). */
@Composable
fun ZielKatalogBildschirm(app: StackLaborApp, steuerung: NavHostController) {
    val bereich = rememberCoroutineScope()
    val ziele by app.datenbank.zielDao().alleZieleMitAnzahl()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var neuerText by remember { mutableStateOf("") }
    var eingabeOffen by remember { mutableStateOf(false) }
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Farben.grund)) {
        Column(Modifier.fillMaxSize()) {
            Kopfleiste("Ziel-Katalog", steuerung)

            if (eingabeOffen) {
                Row(
                    Modifier.fillMaxWidth().background(Farben.flaeche).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eingabefeld(
                        wert = neuerText,
                        aufAenderung = { neuerText = it },
                        platzhalter = "Neues Ziel …",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Chip("Sichern", neuerText.isNotBlank()) {
                        if (neuerText.isNotBlank()) {
                            bereich.launch { app.repository.legeZielAn(neuerText) }
                            neuerText = ""
                            eingabeOffen = false
                        }
                    }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 76.dp + untenInset),
            ) {
                if (ziele.isEmpty()) {
                    item { LeerHinweis("Noch kein Ziel angelegt.") }
                }
                items(ziele, key = { it.ziel.id }) { z ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(z.ziel.text, style = Schrift.name, color = Farben.text, maxLines = 1)
                        Text(
                            "in ${z.anzahlStacks} Stacks verwendet",
                            style = Schrift.klein, color = Farben.textSchwach,
                        )
                    }
                }
            }
        }
        PlusKnopf(
            beschreibung = "Neues Ziel anlegen",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Masse.plusRand, bottom = Masse.plusRand + untenInset),
        ) { eingabeOffen = true }
    }
}

/** **B-09 — Alle Stacks zusammen.** Tagesgesamtdosis und übergreifende Konkurrenzen (F-13). */
@Composable
fun GesamtBildschirm(app: StackLaborApp, steuerung: NavHostController) {
    val eintraege by app.datenbank.stackDao().alleAktivenEintraegeMitMittel()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Die Tagesgesamtdosis wird über die Mittel-Kennung summiert, nicht über den Namen —
    // sonst wären ein Leerzeichen oder ein Tippfehler zwei verschiedene Wirkstoffe (A-11).
    val summen = eintraege
        .groupBy { it.eintrag.mittelId }
        .map { (_, liste) ->
            val erstes = liste.first()
            val gesamt = liste.sumOf { (it.eintrag.mengeJeStueck ?: 0.0) * it.eintrag.stueckzahl }
            Triple(erstes.mittel.name, gesamt to erstes.eintrag.einheit, liste.size)
        }
        .sortedByDescending { it.third }

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste("Alle Stacks zusammen", steuerung)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp + untenInset),
        ) {
            item { Rubrik("Tagesgesamtdosis") }
            items(summen) { (name, mengeUndEinheit, anzahl) ->
                Row(
                    Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = Schrift.grund, color = Farben.text, maxLines = 1)
                        if (anzahl > 1) {
                            Text(
                                "in $anzahl Stacks",
                                style = Schrift.klein, color = Farben.textSchwach,
                            )
                        }
                    }
                    Text(
                        if (mengeUndEinheit.first > 0) "${zahlKurz(mengeUndEinheit.first)}" else "—",
                        style = Schrift.grundMittel,
                        color = if (anzahl > 1) Farben.gelbText else Farben.text,
                    )
                }
            }
        }
    }
}

/** **B-10 — Einstellungen.** Vorlesen, Codex, Daten, Darstellung (F-18). */
@Composable
fun EinstellungenBildschirm(app: StackLaborApp, steuerung: NavHostController) {
    val bereich = rememberCoroutineScope()
    val dunkel by app.einstellungen.dunkel.collectAsStateWithLifecycle(initialValue = false)
    val reduziert by app.einstellungen.bewegungReduziert.collectAsStateWithLifecycle(initialValue = false)
    val anbieter by app.einstellungen.ttsAnbieter.collectAsStateWithLifecycle(initialValue = "EDGE")
    val stimme by app.einstellungen.ttsStimme.collectAsStateWithLifecycle(initialValue = "")
    val modell by app.einstellungen.codexModell.collectAsStateWithLifecycle(initialValue = "")
    val denkstufe by app.einstellungen.codexDenkstufe.collectAsStateWithLifecycle(initialValue = "")
    val angemeldet by app.codexAnmeldung.istAngemeldet.collectAsStateWithLifecycle(initialValue = false)
    val konto by app.codexAnmeldung.konto.collectAsStateWithLifecycle(initialValue = null)
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste("Einstellungen", steuerung)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp + untenInset),
        ) {
            item { Rubrik("Vorlesen") }
            item { Zeile("Anbieter", anbieter) }
            item { Zeile("Stimme", stimme.substringAfterLast('-')) }

            item { Rubrik("Codex") }
            item {
                Zeile(
                    "Konto",
                    if (angemeldet) (konto ?: "angemeldet") else "nicht angemeldet",
                    aufKlick = { steuerung.navigate(Ziele.ANMELDUNG) },
                )
            }
            item { Zeile("Modell", modell) }
            item { Zeile("Denkstufe", denkstufe) }

            item { Rubrik("Daten") }
            item { Zeile("Startbestand neu einlesen", "6 Stacks · 72 Einträge") {
                bereich.launch { app.repository.leseStartbestandEin() }
            } }

            item { Rubrik("Darstellung") }
            item {
                SchalterZeile("Dunkle Darstellung", dunkel) {
                    bereich.launch { app.einstellungen.setzeDunkel(it) }
                }
            }
            item {
                SchalterZeile("Bewegung reduzieren", reduziert) {
                    bereich.launch { app.einstellungen.setzeBewegungReduziert(it) }
                }
            }

            item { Rubrik("Über") }
            item { Zeile("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})") }
        }
    }
}

/** **B-11 — Codex-Anmeldung.** Der Geräte-Flow (F-17). */
@Composable
fun AnmeldungsBildschirm(app: StackLaborApp, steuerung: NavHostController) {
    val bereich = rememberCoroutineScope()
    val zusammenhang = LocalContext.current
    var code by remember { mutableStateOf<String?>(null) }
    var adresse by remember { mutableStateOf("") }
    var meldung by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste("Codex-Anmeldung", steuerung)
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Öffne diese Seite und gib den Code ein:",
                style = Schrift.grund, color = Farben.textSchwach,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(Formen.karte)
                    .background(Farben.flaeche)
                    .border(1.dp, Farben.rand, Formen.karte),
                contentAlignment = Alignment.Center,
            ) {
                Text(code ?: "— — — —", style = Schrift.code, color = Farben.text)
            }
            Spacer(Modifier.height(12.dp))
            Text(adresse.ifBlank { "auth.openai.com/device" }, style = Schrift.grund, color = Farben.akzent)
            Spacer(Modifier.height(16.dp))
            Chip("Code holen", true) {
                bereich.launch {
                    runCatching { app.codexAnmeldung.starteAnmeldung() }
                        .onSuccess { code = it.userCode; adresse = it.verifizierungsUrl }
                        .onFailure { meldung = it.message ?: "Fehler" }
                }
            }
            if (meldung.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(meldung, style = Schrift.klein, color = Farben.rot)
            }
        }
    }
}

/** **B-14 — Mittel-Katalog.** Alle Mittel mit Suche (F-24, F-30). */
@Composable
fun KatalogBildschirm(app: StackLaborApp, steuerung: NavHostController) {
    var suche by remember { mutableStateOf("") }
    val mittel by app.datenbank.mittelDao().alleMittelMitAnzahl()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val gefiltert = remember(mittel, suche) {
        if (suche.isBlank()) mittel
        else mittel.filter { it.mittel.name.contains(suche, ignoreCase = true) }
    }
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste("Mittel-Katalog", steuerung)
        Box(Modifier.fillMaxWidth().background(Farben.flaeche).padding(12.dp)) {
            Eingabefeld(suche, { suche = it }, "Suchen …", Modifier.fillMaxWidth())
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp + untenInset),
        ) {
            if (gefiltert.isEmpty()) {
                item { LeerHinweis("Nichts gefunden.") }
            }
            items(gefiltert, key = { it.mittel.id }) { m ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoeslichkeitsPunkte(
                        when (m.mittel.loeslichkeit) {
                            LoeslichkeitE.WASSER -> Loeslichkeit.WASSER
                            LoeslichkeitE.FETT -> Loeslichkeit.FETT
                            LoeslichkeitE.BEIDES -> Loeslichkeit.BEIDES
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.mittel.name, style = Schrift.name, color = Farben.text, maxLines = 1)
                        Text(
                            listOf("in ${m.anzahlStacks} Stacks", m.mittel.hersteller)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = Schrift.klein, color = Farben.textSchwach, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ── Kleine gemeinsame Bausteine ─────────────────────────────────────────────────

@Composable
private fun Rubrik(text: String) {
    Text(
        text.uppercase(),
        style = Schrift.rubrik,
        color = Farben.textSchwach,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Zeile(bezeichnung: String, wert: String, aufKlick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (aufKlick != null) Modifier.clickable(onClick = aufKlick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(bezeichnung, style = Schrift.grund, color = Farben.text, modifier = Modifier.weight(1f))
        Text(wert, style = Schrift.klein, color = Farben.textSchwach, maxLines = 1)
    }
}

@Composable
private fun SchalterZeile(bezeichnung: String, an: Boolean, aufAenderung: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(bezeichnung, style = Schrift.grund, color = Farben.text, modifier = Modifier.weight(1f))
        Switch(checked = an, onCheckedChange = aufAenderung)
    }
}

@Composable
private fun LeerHinweis(text: String) {
    Text(
        text,
        style = Schrift.grund,
        color = Farben.textSchwach,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}

@Composable
private fun Eingabefeld(
    wert: String,
    aufAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(40.dp)
            .clip(Formen.feld)
            .background(Farben.grund)
            .border(1.dp, Farben.rand, Formen.feld)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (wert.isEmpty()) {
            Text(platzhalter, style = Schrift.grund, color = Farben.textSchwach)
        }
        BasicTextField(
            value = wert,
            onValueChange = aufAenderung,
            textStyle = Schrift.grund.copy(color = Farben.text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun zahlKurz(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else String.format("%.1f", w).replace('.', ',')
