package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.tts.TtsCatalog
import de.frank.experimente.tts.TtsProvider
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.Pillenwahl
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Effektstufe
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.lichtsaum

private val MODELLE = listOf("gpt-5.6-sol" to "GPT 5.6 Sol", "gpt-5.6-terra" to "GPT 5.6 Terra", "gpt-5.6-luna" to "GPT 5.6 Luna")
private val EFFORT = listOf(
    "low" to "Niedrig", "medium" to "Mittel", "high" to "Hoch", "xhigh" to "Sehr hoch", "max" to "Maximal",
)
/**
 * Anbieter und Stimmen kommen **ausschliesslich** aus `TtsCatalog` — derselben Quelle, aus
 * der auch `Vorleser` liest.
 *
 * Vorher standen hier kurze, von Hand getippte Listen: vier von dreissig Chirp-3-Stimmen,
 * drei von sechs Edge-Stimmen, und die Anbieter-Kennungen wichen ab (`qwen` statt
 * `qwen_clone`). Damit fiel „Meine Stimme" im Vorleser in den else-Zweig und es sprach
 * immer Edge. Eine zweite Liste neben dem Katalog ist die Ursache — deshalb gibt es sie
 * nicht mehr.
 */
private fun stimmenVon(anbieter: String): List<Pair<String, String>> = when (anbieter) {
    TtsProvider.GOOGLE_CLOUD.id -> TtsCatalog.googleVoices.map { it.id to it.id }
    TtsProvider.EDGE.id -> TtsCatalog.edgeVoices.map { it.id to it.id }
    else -> emptyList()
}

/**
 * **B-08 — Einstellungen.** Sieben Abschnitte in der Reihenfolge des Entwurfs:
 * Modelle (F-22) · Stimme (F-23) · Zugänge (F-24) · Erinnerungen (F-25) ·
 * **Effekte (F-41, neu)** · Erscheinung (F-26) · Über mich (→ B-09).
 */
@Composable
fun Einstellungen(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val einstellungen = remember { modell.einstellungenZugriff() }

    val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
    val effektstufe by modell.effektstufe.collectAsStateWithLifecycle()
    val angemeldet by modell.angemeldet.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val liestVor by modell.liestVor.collectAsStateWithLifecycle()
    val geraetecode by modell.geraetecode.collectAsStateWithLifecycle()
    val meldetAn by modell.meldetAn.collectAsStateWithLifecycle()

    // Die gewaehlten Werte liegen im Zustand, nicht nur in den Einstellungen: nur was die
    // Composition LIEST, zeichnet sie nach einer Aenderung neu. Ein blosser Zaehler, der
    // hochgesetzt aber nirgends gelesen wird, bewegt nichts - das Feld zeigte dann weiter
    // den alten Wert.
    var anbieter by remember { mutableStateOf(einstellungen.ttsAnbieter) }
    var tempo by remember { mutableStateOf(einstellungen.sprechtempo) }
    var modellExp by remember { mutableStateOf(einstellungen.modellExperimente) }
    var effortExp by remember { mutableStateOf(einstellungen.effortExperimente) }
    var modellLog by remember { mutableStateOf(einstellungen.modellLogbuch) }
    var effortLog by remember { mutableStateOf(einstellungen.effortLogbuch) }
    var stimmeGoogle by remember { mutableStateOf(einstellungen.stimmeGoogle) }
    var stimmeEdge by remember { mutableStateOf(einstellungen.stimmeEdge) }
    var morgensAn by remember { mutableStateOf(einstellungen.erinnerungMorgensAn) }
    var abendsAn by remember { mutableStateOf(einstellungen.erinnerungAbendsAn) }

    Bildschirmgeruest(
        kopfInnen = 8.dp,
        kopfLinks = { Rundknopf(Symbole.Zurueck, "Zurück", modell::zurueck) },
        kopf = { Titel("Einstellungen", klein = true) },
        ueberlagerung = {
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
        },
    ) {
        // --- Modelle (F-22) ---------------------------------------------------------
        item("kopf-modelle") { Zwischenueberschrift("Modelle") }
        item("modelle") {
            Abschnittskarte {
                Gruppe("Experimente") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Auswahl("Modell", MODELLE, modellExp, Modifier.weight(1f)) {
                            modellExp = it
                            einstellungen.modellExperimente = it
                        }
                        Auswahl("Effort", EFFORT, effortExp, Modifier.weight(1f)) {
                            effortExp = it
                            einstellungen.effortExperimente = it
                        }
                    }
                }
                Gruppe("Logbuch", Modifier.padding(top = 18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Auswahl("Modell", MODELLE, modellLog, Modifier.weight(1f)) {
                            modellLog = it
                            einstellungen.modellLogbuch = it
                        }
                        Auswahl("Effort", EFFORT, effortLog, Modifier.weight(1f)) {
                            effortLog = it
                            einstellungen.effortLogbuch = it
                        }
                    }
                }
                Text(
                    text = "Das Logbuch darf ein anderes Modell benutzen als die Experimente.",
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }

        // --- Stimme (F-23) ----------------------------------------------------------
        item("kopf-stimme") { Zwischenueberschrift("Stimme", Modifier.padding(top = 16.dp)) }
        item("stimme") {
            Abschnittskarte {
                Auswahl(
                    beschriftung = "Anbieter",
                    eintraege = TtsCatalog.providers.map { it.id to it.label },
                    gewaehlt = anbieter,
                ) { anbieter = it; einstellungen.ttsAnbieter = it }

                when (anbieter) {
                    TtsProvider.GOOGLE_CLOUD.id -> Auswahl(
                        beschriftung = "Stimme",
                        eintraege = stimmenVon(anbieter),
                        gewaehlt = stimmeGoogle,
                        mono = true,
                        modifier = Modifier.padding(top = 16.dp),
                    ) { stimmeGoogle = it; einstellungen.stimmeGoogle = it }

                    TtsProvider.EDGE.id -> Auswahl(
                        beschriftung = "Stimme",
                        eintraege = stimmenVon(anbieter),
                        gewaehlt = stimmeEdge,
                        mono = true,
                        modifier = Modifier.padding(top = 16.dp),
                    ) { stimmeEdge = it; einstellungen.stimmeEdge = it }

                    else -> EigeneStimmen(modell)
                }

                // Sprechgeschwindigkeit 0,70 bis 1,30 in Schritten von 0,05.
                Column(Modifier.padding(top = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Beschriftung("Sprechgeschwindigkeit")
                        Text(
                            text = "%.2f".format(tempo).replace('.', ','),
                            style = schriften.zahl,
                            color = farben.aktion,
                        )
                    }
                    Slider(
                        value = tempo,
                        onValueChange = { tempo = it },
                        onValueChangeFinished = { einstellungen.sprechtempo = tempo },
                        valueRange = 0.7f..1.3f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = farben.aktion,
                            activeTrackColor = farben.aktion,
                            inactiveTrackColor = farben.rand,
                            activeTickColor = farben.aufAktion,
                            inactiveTickColor = farben.blass,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("0,70", "1,00", "1,30").forEach {
                            Text(it, style = schriften.stufe, color = farben.blass)
                        }
                    }
                }

                KnopfUmrandet(
                    text = if (liestVor) "Probe läuft …" else "Probe hören",
                    beiKlick = modell::hoerProbe,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    farbe = if (liestVor) farben.aktion else farben.text,
                )
            }
        }

        // --- Zugänge (F-24) ---------------------------------------------------------
        item("kopf-zugaenge") { Zwischenueberschrift("Zugänge", Modifier.padding(top = 16.dp)) }
        item("zugaenge") {
            Abschnittskarte {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("OpenAI Codex", style = schriften.knopf, color = farben.text)
                        Text(
                            text = if (angemeldet) {
                                "angemeldet" + (modell.angemeldetAls?.let { " · $it" } ?: "")
                            } else {
                                "nicht angemeldet"
                            },
                            style = schriften.daten,
                            color = if (angemeldet) farben.erledigt else farben.gedaempft,
                        )
                    }
                    when {
                        angemeldet -> KnopfUmrandet("Abmelden", modell::meldeAb)
                        meldetAn -> KnopfUmrandet("Abbrechen", modell::brichAnmeldungAb)
                        else -> KnopfBetont("Anmelden", modell::meldeAn)
                    }
                }

                // F-24 — der Benutzercode, den Frank auf der OpenAI-Seite eintippt.
                geraetecode?.let { code -> Geraetecodekarte(code, modell) }

                Text(
                    text = "Codex läuft über deine Anmeldung, nicht über einen Schlüssel.",
                    style = schriften.erklaerung,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Schluessel("Groq", "Transkription", einstellungen.groqSchluessel) {
                    einstellungen.groqSchluessel = it
                }
                Schluessel("Google Cloud", "Chirp-3-Stimmen", einstellungen.googleTtsSchluessel) {
                    einstellungen.googleTtsSchluessel = it
                }
                Schluessel("Alibaba", "eigene Stimme", einstellungen.qwenSchluessel) {
                    einstellungen.qwenSchluessel = it
                }
            }
        }

        // --- Erinnerungen (F-25) ----------------------------------------------------
        item("kopf-erinnerungen") { Zwischenueberschrift("Erinnerungen", Modifier.padding(top = 16.dp)) }
        item("erinnerung-morgens") {
            Erinnerung(
                name = "Erinnerung morgens",
                zeit = einstellungen.erinnerungMorgensZeit,
                an = morgensAn,
            ) {
                morgensAn = it
                einstellungen.erinnerungMorgensAn = it
                modell.erinnerungenNeuSetzen()
            }
        }
        item("erinnerung-abends") {
            Erinnerung(
                name = "Erinnerung abends",
                zeit = einstellungen.erinnerungAbendsZeit,
                an = abendsAn,
            ) {
                abendsAn = it
                einstellungen.erinnerungAbendsAn = it
                modell.erinnerungenNeuSetzen()
            }
        }

        // --- Effekte (F-41, NEU) ----------------------------------------------------
        item("kopf-effekte") { Zwischenueberschrift("Effekte", Modifier.padding(top = 16.dp)) }
        item("effekte") {
            Column {
                Pillenwahl(
                    eintraege = Effektstufe.entries.map { it.anzeige },
                    gewaehlt = Effektstufe.entries.indexOf(effektstufe),
                    beiWahl = { modell.setzeEffektstufe(Effektstufe.entries[it]) },
                )
                Text(
                    text = "Auf Aus bleibt jede Funktion vollständig bedienbar.",
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        // --- Erscheinung (F-26) -----------------------------------------------------
        item("kopf-erscheinung") { Zwischenueberschrift("Erscheinung", Modifier.padding(top = 16.dp)) }
        item("erscheinung") {
            Pillenwahl(
                eintraege = listOf("Dunkel", "Hell", "Wie das System"),
                gewaehlt = when (erscheinung) {
                    Erscheinung.DUNKEL -> 0
                    Erscheinung.HELL -> 1
                    Erscheinung.SYSTEM -> 2
                },
                beiWahl = {
                    modell.setzeErscheinung(
                        when (it) {
                            0 -> Erscheinung.DUNKEL
                            1 -> Erscheinung.HELL
                            else -> Erscheinung.SYSTEM
                        },
                    )
                },
            )
        }

        // --- Über mich (→ B-09) -----------------------------------------------------
        item("kopf-ueber") { Zwischenueberschrift("Über mich", Modifier.padding(top = 16.dp)) }
        item("selbstbild") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(farben.erhoeht)
                    .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
                    .clickable { modell.gehe(Ziel.SELBSTBILD) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Selbstbild", style = schriften.fliesstext, color = farben.text)
                Icon(Symbole.Weiter, null, tint = farben.gedaempft, modifier = Modifier.size(20.dp))
            }
        }

        // Über den Entwurf hinaus: die Version steht sichtbar in der App, damit ein
        // angekommenes Update sofort erkennbar ist.
        item("version") {
            Text(
                text = "Version ${de.frank.experimente.BuildConfig.VERSION_NAME} " +
                    "(${de.frank.experimente.BuildConfig.VERSION_BUMPED_AT})",
                style = schriften.stufe,
                color = farben.blass,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}

/**
 * **B-09 — Selbstbild** (F-21). Ein großes, frei beschreibbares Feld: kein Raster, keine
 * Felder, keine Fragen — ein Fließtext, der als **erster** Block in jede Vorschlagsanfrage
 * eingeht.
 */
@Composable
fun Selbstbild(modell: AppViewModel) {
    val farben = LocalFarben.current
    val feld by modell.selbstbildFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { modell.ladeSelbstbildInsFeld() }

    Bildschirmgeruest(
        kopfInnen = 8.dp,
        kopfLinks = {
            Rundknopf(
                symbol = Symbole.Zurueck,
                beschriftung = "Zurück zu Einstellungen",
                beiKlick = {
                    // F-21: das Selbstbild wird beim Verlassen gespeichert.
                    modell.speichereSelbstbild()
                    modell.zurueck()
                },
            )
        },
        kopf = { Titel("Selbstbild", klein = true) },
        ueberlagerung = {
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
        },
    ) {
        item("feld") {
            Eingabefeld(
                text = feld.text,
                beiAenderung = { modell.setzeText(modell.selbstbildFeldFluss(), it) },
                platzhalter = "Erzähl der App, wer du bist.",
                mindesthoehe = 360.dp,
            )
        }
        item("knoepfe") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KnopfUmrandet(
                    text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                    beiKlick = {
                        if (feld.kannZurueck) modell.nimmZurueck(modell.selbstbildFeldFluss())
                        else modell.verbessere(modell.selbstbildFeldFluss())
                    },
                    modifier = Modifier.weight(1f),
                    farbe = farben.aktion,
                )
                Box(
                    Modifier
                        .size(width = 56.dp, height = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(farben.aktion)
                        .clickable { modell.sprechknopf(modell.selbstbildFeldFluss()) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (nimmtAuf) Symbole.Stopp else Symbole.Mikrofon,
                        contentDescription = "Selbstbild einsprechen",
                        tint = farben.aufAktion,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Bauteile von B-08
// ---------------------------------------------------------------------------------------

/** Ein Abschnitt: Karte mit 1 dp Rand, Radius 20 dp, Innenabstand 18 dp. */
@Composable
private fun Abschnittskarte(inhalt: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val farben = LocalFarben.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            .padding(18.dp),
        content = inhalt,
    )
}

/** Eine Gruppe innerhalb eines Abschnitts, mit Titel in Fraunces 17/23. */
@Composable
private fun Gruppe(
    titel: String,
    modifier: Modifier = Modifier,
    inhalt: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = titel,
            style = LocalSchriften.current.gruppentitel,
            color = LocalFarben.current.text,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        inhalt()
    }
}

/** Die Beschriftung über einem Feld: Inter 13/18 in *Gedämpft*. */
@Composable
private fun Beschriftung(text: String) {
    Text(
        text = text,
        style = LocalSchriften.current.feldbeschriftung,
        color = LocalFarben.current.gedaempft,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/**
 * Ein Auswahlfeld: 48 dp hoch, Radius 14 dp, 1 dp Rand, Fläche *Erhöht*, rechts das kleine
 * Dreieck — wie das `<select>` im Entwurf. Ein Druck **klappt eine Liste auf**, aus der
 * gewählt wird; er schaltet nicht weiter.
 *
 * Die Liste trägt die Sprache der App: Fläche *Fläche*, 1 dp Rand *Rand*, Radius 14 dp, je
 * Eintrag mindestens 48 dp Tippfläche, der gewählte in *Aktion* mit einem Haken davor —
 * damit die Farbe die Auswahl nicht allein trägt.
 */
@Composable
private fun Auswahl(
    beschriftung: String,
    eintraege: List<Pair<String, String>>,
    gewaehlt: String,
    modifier: Modifier = Modifier,
    platzhalter: String = "—",
    mono: Boolean = false,
    beiWahl: (String) -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var offen by remember { mutableStateOf(false) }
    val angezeigt = eintraege.firstOrNull { it.first == gewaehlt }?.second ?: platzhalter
    // Stimmkennungen setzt der Entwurf in JetBrains Mono - sie sind Daten, kein Fliesstext.
    val stil = if (mono) {
        schriften.daten
    } else {
        schriften.knopfKlein.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400))
    }

    // Das kleine Dreieck dreht sich beim Aufklappen (M-13, 120 ms, Hauskurve).
    val stufe = LocalEffektstufe.current
    val drehung by animateFloatAsState(
        targetValue = if (offen) 180f else 0f,
        animationSpec = tween(dauer(Bewegung.KURZ, stufe), easing = Bewegung.ruhig),
        label = "aufklappen",
    )

    Column(modifier) {
        Beschriftung(beschriftung)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(farben.erhoeht)
                    .border(1.dp, if (offen) farben.aktion else farben.rand, RoundedCornerShape(14.dp))
                    .clickable { offen = true }
                    .padding(start = 13.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = angezeigt,
                    style = stil,
                    color = if (angezeigt == platzhalter) farben.blass else farben.text,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Symbole.Aufklappen,
                    contentDescription = null,
                    tint = if (offen) farben.aktion else farben.gedaempft,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = drehung },
                )
            }

            DropdownMenu(
                expanded = offen,
                onDismissRequest = { offen = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = farben.flaeche,
                border = androidx.compose.foundation.BorderStroke(1.dp, farben.rand),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                eintraege.forEach { (wert, anzeige) ->
                    val aktiv = wert == gewaehlt
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = anzeige,
                                style = stil,
                                color = if (aktiv) farben.aktion else farben.text,
                            )
                        },
                        leadingIcon = {
                            if (aktiv) {
                                Icon(
                                    Symbole.KastenVoll,
                                    contentDescription = "gewählt",
                                    tint = farben.aktion,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Box(Modifier.size(20.dp))
                            }
                        },
                        onClick = {
                            offen = false
                            beiWahl(wert)
                        },
                    )
                }
            }
        }
    }
}

/**
 * **F-23 Schritt 3 — „Meine Stimme".**
 *
 * Frank hat bei Alibaba bereits Stimmen hinterlegt. Sie stehen hier als Auswahl, damit er
 * die 46 Zeichen lange Kennung nicht abtippen muss; darunter kann er eine **weitere**
 * aufnehmen und die gewählte wieder löschen.
 *
 * Die Liste wird beim Öffnen abgerufen und nach jeder Aufnahme erneut — eine gerade
 * registrierte Stimme steht damit sofort zur Wahl.
 */
@Composable
private fun EigeneStimmen(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val stimmen by modell.stimmen.collectAsStateWithLifecycle()
    val laden by modell.stimmenLaden.collectAsStateWithLifecycle()
    val gewaehlt by modell.stimmeQwen.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { modell.ladeStimmen() }

    Column(Modifier.padding(top = 16.dp)) {
        Auswahl(
            beschriftung = "Stimme",
            eintraege = stimmen.map { it.id to it.name },
            gewaehlt = gewaehlt,
            platzhalter = when {
                laden -> "wird geladen …"
                stimmen.isEmpty() -> "noch keine Stimme aufgenommen"
                else -> "keine gewählt"
            },
            beiWahl = modell::waehleStimme,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KnopfUmrandet(
                text = if (nimmtAuf) "Aufnahme beenden" else "Neue Stimme aufnehmen",
                beiKlick = { modell.nimmStimmeAuf() },
                modifier = Modifier.weight(1f),
                farbe = if (nimmtAuf) farben.aktion else farben.text,
                randfarbe = if (nimmtAuf) farben.aktion else farben.rand,
            )
            KnopfUmrandet(
                text = "Neu laden",
                beiKlick = modell::ladeStimmen,
                farbe = farben.gedaempft,
            )
        }

        if (gewaehlt.isNotBlank()) {
            Textknopf(
                text = "Gewählte Stimme löschen",
                beiKlick = { modell.loescheStimme(gewaehlt) },
                farbe = farben.warnung,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            text = "Deine eigene Stimme wird über Alibaba erzeugt. Der Zugang dafür steht " +
                "unter „Zugänge“.",
            style = schriften.erklaerung,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * **F-24 — der Gerätecode.** Solange die App auf die Bestätigung wartet, steht der
 * Benutzercode hier: groß, in JetBrains Mono mit weiter Laufweite, damit man ihn beim
 * Abtippen nicht verliest — und daneben „Kopieren", das ihn in die Zwischenablage legt.
 *
 * Der Code kommt von OpenAI in der Form `ABCD-EFGHI` (vier Zeichen, Bindestrich, fünf).
 * Er wird **wörtlich** angezeigt, nicht umformatiert: was auf dem Bildschirm steht, ist
 * genau das, was die Seite erwartet.
 *
 * Ein Druck auf den Code selbst kopiert ihn ebenfalls — die Fläche ist größer als der
 * Knopf und damit schneller zu treffen.
 */
@Composable
private fun Geraetecodekarte(code: de.frank.experimente.auth.Geraetecode, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ablage = androidx.compose.ui.platform.LocalClipboardManager.current

    val kopieren = {
        ablage.setText(androidx.compose.ui.text.AnnotatedString(code.benutzercode))
        modell.melde("Code kopiert.")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(farben.erhoeht)
            .border(1.dp, farben.aktion.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Diesen Code auf der geöffneten Seite eintippen",
            style = schriften.feldbeschriftung,
            color = farben.gedaempft,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = code.benutzercode,
                style = schriften.zahl.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        26f, androidx.compose.ui.unit.TextUnitType.Sp,
                    ),
                    lineHeight = androidx.compose.ui.unit.TextUnit(
                        32f, androidx.compose.ui.unit.TextUnitType.Sp,
                    ),
                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                        3f, androidx.compose.ui.unit.TextUnitType.Sp,
                    ),
                ),
                color = farben.aktion,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = kopieren)
                    .padding(vertical = 4.dp),
            )
            KnopfUmrandet(
                text = "Kopieren",
                beiKlick = kopieren,
                farbe = farben.aktion,
                randfarbe = farben.aktion,
            )
        }
        Text(
            text = code.adresse,
            style = schriften.stufe,
            color = farben.blass,
            modifier = Modifier.padding(top = 10.dp),
        )
        Textknopf(
            text = "Seite erneut öffnen",
            beiKlick = { modell.oeffneAnmeldeseite(code.adresse) },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Ein Schlüsselfeld (F-24): verdeckt, daneben das Auge zum Sichtbarmachen. */
@Composable
private fun Schluessel(name: String, zweck: String, wert: String, beiAenderung: (String) -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var text by remember { mutableStateOf(wert) }
    var sichtbar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.randWeich))
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(name, style = schriften.knopfKlein, color = farben.text)
            Text(zweck, style = schriften.stufe, color = farben.blass)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(farben.erhoeht)
                    .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; beiAenderung(it) },
                    singleLine = true,
                    textStyle = schriften.daten.copy(color = farben.text),
                    cursorBrush = SolidColor(farben.aktion),
                    visualTransformation = if (sichtbar) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
                    .clickable { sichtbar = !sichtbar },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (sichtbar) Symbole.AugeAus else Symbole.Auge,
                    contentDescription = if (sichtbar) "$name-Zugang verbergen" else "$name-Zugang zeigen",
                    tint = if (sichtbar) farben.aktion else farben.gedaempft,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Eine Erinnerung (F-25): 56 dp hoch, Radius 14 dp, rechts die Uhrzeit und der Schalter
 * (48 × 28 dp, Knopf 22 dp, `left` wandert in 200 ms mit der Hauskurve — M-27).
 */
@Composable
private fun Erinnerung(name: String, zeit: String, an: Boolean, beiWechsel: (Boolean) -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    var jetzt by remember(an) { mutableStateOf(an) }

    val knopfLinks by animateDpAsState(
        targetValue = if (jetzt) 23.dp else 3.dp,
        animationSpec = tween(dauer(Bewegung.MITTEL, stufe), easing = Bewegung.ruhig),
        label = "schalter",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(farben.erhoeht)
            .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = schriften.fliesstext, color = farben.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(zeit, style = schriften.zahl.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400)), color = farben.gedaempft)
            Box(
                Modifier
                    .size(width = 48.dp, height = 28.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (jetzt) farben.aktion else farben.rand)
                    .clickable { jetzt = !jetzt; beiWechsel(jetzt) },
            ) {
                Box(
                    Modifier
                        .offset(x = knopfLinks, y = 3.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White),
                )
            }
        }
    }
}
