package de.frank.experimente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Task
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.MonitorZustand
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Etikettart
import de.frank.experimente.ui.components.Fortschrittsring
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.LeererZustand
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.SchwebenderPlusknopf
import de.frank.experimente.ui.components.Skelett
import de.frank.experimente.ui.components.Tagewahl
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.components.Einflug
import de.frank.experimente.ui.components.merkeDruck
import de.frank.experimente.ui.components.federdruck
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.Funkenwolke
import de.frank.experimente.ui.theme.Leistensymbole
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.lichtsaum
import de.frank.experimente.ui.theme.neigung
import de.frank.experimente.ui.theme.wandernderRand

/**
 * **B-10 — Monitor.** Der Startbildschirm und das Gesicht der App (A-21).
 *
 * Er sammelt alles, was Frank sich vorgenommen hat, in zwei Abschnitten: **„Läuft"** mit
 * höchstens drei Experimenten und **„Steht an"** mit beliebig vielen. Dazwischen steht die
 * **eine** To-Do-Liste des Tages (F-07). Beides ist ohne Netz vollständig lesbar (A-22).
 *
 * Er trägt die aufwendigste Gestaltung des Entwurfs: Lichtgrund (`E-01`), Glasleisten
 * (`E-03`), Schein (`E-05`), wandernder Rand um laufende Karten (`E-06`), Kipp-Parallaxe
 * (`E-08`), Funken beim Start (`E-15`).
 *
 * Fünf Zustände — `LEER` · `NUR_ANSTEHEND` · `LAEUFT` · `VOLL` · `ANLEGEN` — dazu *lädt*
 * mit den Schimmer-Skeletten (`E-13`).
 */
@Composable
fun Monitor(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val zustand by modell.monitorZustand.collectAsStateWithLifecycle()
    val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
    val laufende by modell.laufende.collectAsStateWithLifecycle()
    val anstehende by modell.anstehende.collectAsStateWithLifecycle()
    val istVoll by modell.istVoll.collectAsStateWithLifecycle()
    val aufgeklappt by modell.aufgeklappt.collectAsStateWithLifecycle()
    val funkelt by modell.funkelt.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val anlegenOffen by modell.anlegenOffen.collectAsStateWithLifecycle()

    Bildschirmgeruest(
        mitUnterkante = true,
        kopf = {
            Titel("Monitor")
            Box(Modifier.weight(1f))
            // F-26: der Schnellschalter zeigt, was gerade gilt.
            Rundknopf(
                symbol = symbolFuerErscheinung(erscheinung),
                beschriftung = beschriftungFuer(erscheinung),
                beiKlick = modell::naechsteErscheinung,
            )
            Rundknopf(
                symbol = Symbole.Einstellungen,
                beschriftung = "Einstellungen",
                beiKlick = { modell.gehe(Ziel.EINSTELLUNGEN) },
            )
        },
        leiste = Ziel.MONITOR,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            SchwebenderPlusknopf("Eigenes Experiment anlegen", modell::oeffneAnlegen)
            // Die Anlegeflaeche liegt ueber dem Bildschirm - die Meldungen muessen ueber
            // IHR liegen. Vorher lagen sie darunter: die Stoerung war unsichtbar, und der
            // Druck auf "Nochmal" traf die Flaeche dahinter und schloss die Anlegeflaeche.
            if (anlegenOffen) Anlegeflaeche(modell)
            Meldungen(
                stoerung = stoerung,
                beiNochmal = modell::schliesseMeldung,
                hinweis = hinweis,
            )
        },
    ) {
        // Die Zählzeile — sie sagt in Worten, was die Abschnitte zeigen (kein Effekt trägt
        // Information allein).
        item("zaehlzeile") {
            Text(
                text = "Steht an: ${anstehende.size} · Läuft: ${laufende.size}",
                style = schriften.daten,
                color = farben.gedaempft,
            )
        }

        if (zustand == MonitorZustand.LAEDT) {
            items(3, key = { "skelett$it" }) { Skelett() }
            return@Bildschirmgeruest
        }

        if (zustand == MonitorZustand.LEER) {
            item("leer") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 104.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Hier steht noch nichts. Leg dir eines an oder hol dir Vorschläge.",
                        style = schriften.abschnittstitel.copy(lineHeight = 30.sp),
                        color = farben.text,
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.height(24.dp))
                    Textknopf("Vorschläge holen", { modell.gehe(Ziel.HEUTE) })
                }
            }
            return@Bildschirmgeruest
        }

        // --- Abschnitt „Läuft" ----------------------------------------------------------
        item("kopf-laeuft") {
            Zwischenueberschrift("Läuft", Modifier.padding(top = 12.dp))
        }
        if (laufende.isEmpty()) {
            item("laeuft-leer") { LeererZustand("Noch läuft nichts. Starte eines von unten.") }
        } else {
            items(laufende, key = { "lauf${it.id}" }) { experiment ->
                Einflug(laufende.indexOf(experiment)) {
                Laufkarte(
                    experiment = experiment,
                    modell = modell,
                    offen = aufgeklappt == experiment.id,
                    funkelt = funkelt == experiment.id,
                )
                }
            }

            // --- F-07: die EINE To-Do-Liste des Tages ---------------------------------
            val gruppen = laufende.map { it to modell.heutigeAufgaben(it) }.filter { it.second.isNotEmpty() }
            if (gruppen.isNotEmpty()) {
                item("kopf-todo") {
                    Zwischenueberschrift("Heute zu tun", Modifier.padding(top = 12.dp))
                }
                item("todo") { Tagesliste(gruppen, modell) }
            }
        }

        // --- Abschnitt „Steht an" -------------------------------------------------------
        item("kopf-steht-an") {
            Zwischenueberschrift("Steht an", Modifier.padding(top = 16.dp))
        }
        if (istVoll) {
            item("voll-hinweis") { VollHinweis() }
        }
        if (anstehende.isEmpty()) {
            item("steht-an-leer") { LeererZustand("Nichts vorgemerkt.") }
        } else {
            items(anstehende, key = { "wart${it.id}" }) { experiment ->
                Einflug(anstehende.indexOf(experiment)) {
                Anstehendkarte(
                    experiment = experiment,
                    modell = modell,
                    offen = aufgeklappt == experiment.id,
                    gesperrt = istVoll,
                    nr = anstehende.indexOf(experiment),
                    anzahl = anstehende.size,
                )
                }
            }
        }
    }
}

/**
 * Eine **Laufkarte**: der Rahmen ist 1,5 dp stark und trägt den wandernden Lichtsaum
 * (`E-06`), darunter liegt der Schein `0 0 22px color-mix(Aktion 20%)`. Innen: Fläche
 * *Erhöht*, Radius 18,5 dp, Innenabstand 21 dp, Lichtsaum oben.
 *
 * Aufgeklappt (F-40) zeigt sie Beschreibung, die heutigen Aufgaben und die zwei Knöpfe
 * „Gespräch" (→ B-02) und „Wie ist es gelaufen?" (→ B-03).
 */
@Composable
private fun Laufkarte(
    experiment: Experiment,
    modell: AppViewModel,
    offen: Boolean,
    funkelt: Boolean,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    val kippung = neigung(stufe)
    val quelle = merkeDruck()

    val heutige = modell.heutigeAufgaben(experiment)
    val fertig = heutige.count { it.doneAt != null }
    val anteil = if (heutige.isEmpty()) 0f else fertig.toFloat() / heutige.size
    val zahlen by modell.auswertungsZahlen.collectAsStateWithLifecycle()
    val auswertungsZahl = zahlen[experiment.id] ?: 0

    var dauerOffen by remember(experiment.id) { mutableStateOf(false) }
    if (dauerOffen) {
        Dauerwahl(
            experiment = experiment,
            laufendenTag = modell.tagNummerVon(experiment),
            beiSchliessen = { dauerOffen = false },
            beiWahl = { neu ->
                dauerOffen = false
                modell.aendereDauer(experiment, neu)
            },
        )
    }

    // E-15 · M-84 — die Funken laufen 1200 ms, sobald das Experiment gestartet wurde.
    val funkenweg by animateFloatAsState(
        targetValue = if (funkelt) 1f else 0f,
        animationSpec = tween(if (funkelt) Bewegung.FUNKEN else 0, easing = Bewegung.gerade),
        label = "funken",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = kippung.umX
                rotationY = kippung.umY
                cameraDistance = 12f * density
            }
            .federdruck(quelle)
            .then(
                if (stufe.schein) {
                    Modifier.shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = farben.aktion.copy(alpha = 0.20f),
                        spotColor = farben.aktion.copy(alpha = 0.20f),
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(20.dp))
            .wandernderRand(farben, stufe)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(18.5.dp))
            .background(farben.erhoeht)
            .lichtsaum(farben.text, 0.14f)
            .clickable(interactionSource = quelle, indication = null) { modell.klappeUm(experiment.id) },
    ) {
        Column(Modifier.padding(21.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(experiment.title, style = schriften.kartentitel, color = farben.text)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Etikett(modell.stufenwort(experiment), Etikettart.STUFE)
                        // Antippbar: hierüber wird die Dauer geändert (F-37, nachträglich).
                        Etikett(
                            text = modell.tagText(experiment),
                            art = Etikettart.ANGABE,
                            beiKlick = { dauerOffen = true },
                            beschriftung = "Dauer ändern, zurzeit ${modell.tagText(experiment)}",
                        )
                        Etikett(experiment.origin.etikett, Etikettart.HERKUNFT)
                    }
                }
                Box(Modifier.width(14.dp))
                // M-87 — der Ring zeigt den Stand der HEUTIGEN Aufgaben, sonst nichts.
                Fortschrittsring(anteil, "$fertig von ${heutige.size}")
            }

            // F-40 · M-86 — die Karte klappt weich auf: `spring(0.8, 300)`.
            AnimatedVisibility(
                visible = offen,
                enter = expandVertically(
                    spring(Bewegung.KLAPP_DAEMPFUNG, Bewegung.KLAPP_STEIFE),
                ) + fadeIn(tween(dauer(Bewegung.KLAPPEN, stufe))),
                exit = shrinkVertically(spring(Bewegung.KLAPP_DAEMPFUNG, Bewegung.KLAPP_STEIFE)),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(farben.randWeich))
                    Text(
                        text = experiment.description,
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Zwischenueberschrift("Aufgaben heute", Modifier.padding(top = 14.dp, bottom = 8.dp))
                    heutige.forEach { aufgabe ->
                        Text(
                            text = "· ${aufgabe.text}",
                            style = schriften.fliesstextKlein,
                            color = farben.gedaempft,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    // Der Verlauf ist im Monitor sichtbar, nicht nur ahnbar: die Zahl sagt,
                    // dass jede Aufnahme erhalten ist, der Knopf darunter führt zu ihnen.
                    val zahl = auswertungsZahl
                    if (zahl > 0) {
                        Text(
                            text = "$zahl ${if (zahl == 1) "Auswertung" else "Auswertungen"} " +
                                "bisher — jede einzeln unter „Wie ist es gelaufen?“",
                            style = schriften.stufe,
                            color = farben.blass,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KnopfUmrandet(
                            text = "Gespräch",
                            beiKlick = { modell.oeffneGespraech(experiment) },
                            modifier = Modifier.weight(1f),
                        )
                        KnopfBetont(
                            text = "Wie ist es gelaufen?",
                            beiKlick = { modell.oeffneAuswertung(experiment) },
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
            }
        }
        if (funkelt) {
            Funkenwolke(farben, stufe, funkenweg, Modifier.matchParentSize())
        }
    }
}

/**
 * Eine **Anstehendkarte** im Abschnitt „Steht an": Fläche *Fläche* mit 92 % Deckkraft, 1 dp Rand,
 * Radius 20 dp, Innenabstand 18 dp.
 *
 * Links der Griff zum Umsortieren (F-38), rechts das Kreuz zum Herausnehmen (F-39); ein
 * Wischen nach links tut dasselbe. Der Knopf „Starten" ist gesperrt, solange drei laufen —
 * der Grund steht als Satz darüber (A-26).
 */
@Composable
private fun Anstehendkarte(
    experiment: Experiment,
    modell: AppViewModel,
    offen: Boolean,
    gesperrt: Boolean,
    nr: Int,
    anzahl: Int,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    val dichte = LocalDensity.current

    var wischweg by remember { mutableFloatStateOf(0f) }
    var ziehweg by remember { mutableFloatStateOf(0f) }
    val zieht = ziehweg != 0f

    var dauerOffen by remember(experiment.id) { mutableStateOf(false) }
    if (dauerOffen) {
        Dauerwahl(
            experiment = experiment,
            // Ein anstehendes Experiment läuft noch nicht — jede Dauer ist erlaubt.
            laufendenTag = null,
            beiSchliessen = { dauerOffen = false },
            beiWahl = { neu ->
                dauerOffen = false
                modell.aendereDauer(experiment, neu)
            },
        )
    }

    // M-85 — die Karte hebt sich beim Ziehen ab: 160 ms, Hauskurve.
    val hebung by animateFloatAsState(
        targetValue = if (zieht) 1.02f else 1f,
        animationSpec = tween(dauer(Bewegung.ABHEBEN, stufe), easing = Bewegung.ruhig),
        label = "abheben",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = wischweg
                translationY = ziehweg
                scaleX = hebung
                scaleY = hebung
            }
            .then(
                if (zieht && stufe.schein) {
                    Modifier.shadow(18.dp, RoundedCornerShape(20.dp), clip = false)
                } else Modifier
            )
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche.copy(alpha = 0.92f))
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            // F-39 — nach links wischen nimmt die Karte aus dem Monitor.
            .pointerInput(experiment.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (wischweg < -with(dichte) { 110.dp.toPx() }) {
                            modell.nimmAusMonitor(experiment, aufMerkliste = true)
                        }
                        wischweg = 0f
                    },
                    onDragCancel = { wischweg = 0f },
                ) { _, weite -> wischweg = (wischweg + weite).coerceAtMost(0f) }
            },
    ) {
        // Der Warnton, der beim Wischen durchscheint.
        if (wischweg < 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha((-wischweg / with(dichte) { 110.dp.toPx() }).coerceIn(0f, 1f))
                    .background(farben.warnung.copy(alpha = 0.16f)),
            )
        }
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // F-38 — langer Druck auf den Griff, dann ziehen.
                Icon(
                    imageVector = Symbole.Griff,
                    contentDescription = "Ziehen zum Umsortieren",
                    tint = if (zieht) farben.aktion else farben.blass,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .pointerInput(experiment.id, anzahl) {
                            detectDragGesturesAfterLongPress(
                                onDragEnd = {
                                    val schritte = Math.round(ziehweg / with(dichte) { 96.dp.toPx() })
                                    ziehweg = 0f
                                    if (schritte != 0) modell.sortiere(experiment, nr + schritte)
                                },
                                onDragCancel = { ziehweg = 0f },
                            ) { _, weite -> ziehweg += weite.y }
                        },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { modell.klappeUm(experiment.id) },
                ) {
                    Text(experiment.title, style = schriften.kartentitel, color = farben.text)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Auch vor dem Start änderbar — dort ist die Dauer am ehesten falsch.
                        Etikett(
                            text = modell.dauerwort(experiment),
                            art = Etikettart.ANGABE,
                            beiKlick = { dauerOffen = true },
                            beschriftung = "Dauer ändern, zurzeit ${modell.dauerwort(experiment)}",
                        )
                        Etikett(experiment.origin.etikett, Etikettart.HERKUNFT)
                    }
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { modell.nimmAusMonitor(experiment, aufMerkliste = true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Symbole.Schliessen,
                        contentDescription = "Aus dem Monitor nehmen",
                        tint = farben.blass,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = offen,
                enter = expandVertically(spring(Bewegung.KLAPP_DAEMPFUNG, Bewegung.KLAPP_STEIFE)) +
                    fadeIn(tween(dauer(Bewegung.KLAPPEN, stufe))),
                exit = shrinkVertically(spring(Bewegung.KLAPP_DAEMPFUNG, Bewegung.KLAPP_STEIFE)),
            ) {
                Text(
                    text = experiment.description,
                    style = schriften.kartentext,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            KnopfBetont(
                text = "Starten",
                beiKlick = { modell.starteAnstehendes(experiment) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                aktiv = !gesperrt,
                gross = true,
            )
        }
    }
}

/**
 * Der Hinweis über „Steht an", sobald drei laufen — `color-mix(Warnung 14%)` mit 1 dp Rand
 * `color-mix(Warnung 40%)`, Radius 14 dp. Der Wortlaut steht in UI-Spec §8.
 */
@Composable
private fun VollHinweis() {
    val farben = LocalFarben.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(farben.warnung.copy(alpha = 0.14f))
            .border(1.dp, farben.warnung.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst.",
            style = LocalSchriften.current.fliesstextKlein,
            color = farben.text,
        )
    }
}

/**
 * **F-07 — die eine To-Do-Liste des Tages.** Ein Kasten mit 1 dp Rand und Radius 20 dp; je
 * laufendem Experiment eine Zwischenüberschrift und darunter seine heutigen Aufgaben.
 * Nicht eine Liste je Experiment — eine Liste für den Tag.
 */
@Composable
private fun Tagesliste(gruppen: List<Pair<Experiment, List<Task>>>, modell: AppViewModel) {
    val farben = LocalFarben.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp)),
    ) {
        gruppen.forEach { (experiment, aufgaben) ->
            Zwischenueberschrift(
                text = experiment.title,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            aufgaben.forEach { aufgabe -> Aufgabenzeile(aufgabe, modell) }
        }
    }
}

/**
 * Eine Zeile der To-Do-Liste (F-08): mindestens 48 dp hoch, 1 dp Trennlinie oben,
 * Kästchen 24 dp, Text Inter 15/22. Abgehakt färbt sich das Kästchen in *Erledigt* und der
 * Text in *Blass* — die Form ändert sich mit, nicht nur die Farbe.
 */
@Composable
private fun Aufgabenzeile(aufgabe: Task, modell: AppViewModel) {
    val farben = LocalFarben.current
    val fertig = aufgabe.doneAt != null
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { modell.hakenSchalten(aufgabe) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (fertig) Symbole.KastenVoll else Symbole.KastenLeer,
            contentDescription = if (fertig) "Erledigt" else "Offen",
            tint = if (fertig) farben.erledigt else farben.text,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = aufgabe.text,
            style = LocalSchriften.current.aufgabenzeile,
            color = if (fertig) farben.blass else farben.text,
        )
    }
}

/**
 * F-26 — das Symbol zeigt den **gerade aktiven** Modus.
 *
 * Vorher kündigte es den nächsten an. Das las sich von außen wie eine Falschanzeige: bei
 * hellem Bildschirm stand dort der Mond, und wer die Sonne sah, saß im Dunkeln. Ein
 * Anzeigeelement soll den Zustand berichten, nicht das, was ein Druck aus ihm machen würde —
 * das gehört in die Beschriftung, die die Sprachausgabe vorliest.
 *
 * Sonne = Hell · Mond = Dunkel · das „A" mit den Strahlen = der Systemdarstellung folgend.
 */
internal fun symbolFuerErscheinung(aktiv: Erscheinung) = when (aktiv) {
    Erscheinung.HELL -> Leistensymbole.Sonne
    Erscheinung.DUNKEL -> Leistensymbole.Mond
    Erscheinung.SYSTEM -> Leistensymbole.Automatik
}

/** Was gerade gilt — und wohin der nächste Druck führt. Beides in einem Satz. */
internal fun beschriftungFuer(aktiv: Erscheinung): String {
    val jetzt = when (aktiv) {
        Erscheinung.HELL -> "Hellmodus"
        Erscheinung.DUNKEL -> "Dunkelmodus"
        Erscheinung.SYSTEM -> "Automatik, folgt der Systemdarstellung"
    }
    val gleich = when (aktiv.naechste()) {
        Erscheinung.HELL -> "Hellmodus"
        Erscheinung.DUNKEL -> "Dunkelmodus"
        Erscheinung.SYSTEM -> "Automatik"
    }
    return "$jetzt aktiv — weiter zu $gleich"
}

/**
 * **Die Dauer eines Experiments nachträglich ändern.**
 *
 * Erreichbar über die Tagesangabe auf der Karte („Tag 4 von 5"). Sie war bisher eine reine
 * Anzeige: die KI legte die Dauer beim Anlegen fest, und danach ließ sie sich nirgends mehr
 * berichtigen — aus „die nächsten sechs, sieben Tage" wurden zwei, und dabei blieb es.
 *
 * Beim Verlängern kommen die Aufgaben für die neuen Tage dazu. Beim Kürzen wird nichts
 * gelöscht: die Aufgaben der wegfallenden Tage bleiben stehen und sind nach einer erneuten
 * Verlängerung unverändert wieder da.
 */
@Composable
internal fun Dauerwahl(
    experiment: Experiment,
    laufendenTag: Int?,
    beiWahl: (Int) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var tage by remember(experiment.id) { mutableIntStateOf(experiment.days) }
    // Ein laufendes Experiment kann nicht kürzer werden als der Tag, an dem es steht.
    val kleinstes = (laufendenTag ?: 1).coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = beiSchliessen,
        containerColor = farben.flaeche,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Wie lange?", style = schriften.kartentitel, color = farben.text) },
        text = {
            Column {
                Text(
                    text = experiment.title,
                    style = schriften.fliesstext,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Tagewahl(
                    tage = tage,
                    beiAenderung = { tage = it },
                    kleinstes = kleinstes,
                    beschriftung = "Gesamtdauer",
                )
                Text(
                    text = when {
                        tage > experiment.days ->
                            "Die Aufgaben für die neuen Tage kommen dazu."
                        tage < experiment.days ->
                            "Es endet früher. Die Aufgaben der späteren Tage bleiben gespeichert."
                        else -> "Unverändert."
                    },
                    style = schriften.stufe,
                    color = farben.blass,
                    modifier = Modifier.padding(top = 14.dp),
                )
                if (laufendenTag != null && laufendenTag > 1) {
                    Text(
                        text = "Es läuft im Moment an Tag $laufendenTag — kürzer geht es nicht.",
                        style = schriften.stufe,
                        color = farben.blass,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { beiWahl(tage) }, enabled = tage != experiment.days) {
                Text(
                    text = "Übernehmen",
                    style = schriften.knopf,
                    color = if (tage != experiment.days) farben.aktion else farben.blass,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = beiSchliessen) {
                Text("Abbrechen", style = schriften.knopf, color = farben.gedaempft)
            }
        },
    )
}
