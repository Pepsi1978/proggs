package de.frank.gedankenspeicher.ui.verlauf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Anhang
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.anhaengeAusJson
import de.frank.gedankenspeicher.data.Nachtraege
import de.frank.gedankenspeicher.data.Notizzustand
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.tts.Absaetze
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.dauer
import de.frank.gedankenspeicher.ui.theme.schwebendeKarte
import de.frank.gedankenspeicher.ui.theme.wanderndesLeuchten

/**
 * **Eine Notiz im Verlauf** (`02-UI-SPEC.md` B-01).
 *
 * Aufbau von oben nach unten: Überschrift und Zeitstempel in einer Zeile, darunter der Text,
 * darunter rechts die beiden kleinen Knöpfe. Alle Zustände aus dem Spec sind hier
 * abgebildet — auch die, in denen die Karte noch keinen Text hat.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun Notizkarte(
    notiz: Notiz,
    liestVor: Boolean,
    vorleseAbsatz: Int,
    verbessertGerade: Boolean,
    hervorgehoben: Boolean,
    beiVorlesen: () -> Unit,
    beiVerbessern: () -> Unit,
    beiRueckgaengig: () -> Unit,
    beiMenue: () -> Unit,
    beiWiederholen: () -> Unit,
    beiEinstellungen: () -> Unit,
    beiAnhangTitel: (Anhang) -> Unit,
    beiNachtrag: () -> Unit = {},
    beiVerschieben: () -> Unit = {},
    beiKopieren: () -> Unit = {},
    beiLoeschen: () -> Unit = {},
) {
    val farben = Farben
    val schrift = Schriften

    // M-11: der Suchtreffer leuchtet einmal auf.
    val hebung by animateFloatAsState(
        targetValue = if (hervorgehoben) 1f else 0f,
        animationSpec = tween(dauer(Dauern.STANDARD), easing = Kurven.standard),
        label = "hervorhebung",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .schwebendeKarte(farben, Masse.karteRadius)
            .then(
                if (hebung > 0f) {
                    Modifier.background(
                        farben.akzentGedeckt.copy(alpha = farben.akzentGedeckt.alpha * hebung),
                        RoundedCornerShape(Masse.karteRadius),
                    )
                } else {
                    Modifier
                },
            )
            .wanderndesLeuchten(farben.akzentGedeckt, verbessertGerade)
            .combinedClickable(onLongClick = beiMenue, onClick = {})
            .padding(Masse.karteInnen),
    ) {
        Column {
            Kopfzeile(notiz, farben.textMittel, farben.textSchwach)
            Spacer(Modifier.height(8.dp))

            when (notiz.zustand) {
                Notizzustand.TRANSKRIBIERT_GERADE, Notizzustand.AUFNEHMEND ->
                    WanderndePunkte(farben.textSchwach)

                Notizzustand.WARTET_AUF_TRANSKRIPTION -> HinweisZeile(
                    symbol = { Icon(Icons.Outlined.CloudOff, null, Modifier.size(16.dp), tint = farben.textSchwach) },
                    text = "Wartet auf Netz",
                    farbe = farben.textSchwach,
                )

                Notizzustand.TRANSKRIPTION_FEHLGESCHLAGEN -> Column {
                    Text("Transkription fehlgeschlagen", style = schrift.notiztext, color = farben.fehler)
                    TextButton(onClick = beiWiederholen) {
                        Text("Nochmal versuchen", style = schrift.knopf, color = farben.akzent)
                    }
                }

                Notizzustand.NICHTS_VERSTANDEN -> Column {
                    Text("Nichts verstanden", style = schrift.notiztext, color = farben.textSchwach)
                    TextButton(onClick = beiWiederholen) {
                        Text("Nochmal versuchen", style = schrift.knopf, color = farben.akzent)
                    }
                }

                Notizzustand.KEIN_SCHLUESSEL -> Column {
                    Text("Für die Transkription fehlt der Groq-Schlüssel.", style = schrift.notiztext, color = farben.fehler)
                    TextButton(onClick = beiEinstellungen) {
                        Text("Einstellungen öffnen", style = schrift.knopf, color = farben.akzent)
                    }
                }

                Notizzustand.FERTIG -> if (notiz.text.isNotBlank()) {
                    NachtragsAbschnitte(
                        text = notiz.text,
                        liestVor = liestVor,
                        vorleseAbsatz = vorleseAbsatz,
                    )
                }
            }

            val anhaenge = remember(notiz.anhaengeJson) { anhaengeAusJson(notiz.anhaengeJson) }
            if (anhaenge.isNotEmpty()) {
                Spacer(Modifier.height(if (notiz.text.isBlank()) 2.dp else 10.dp))
                Anhangsliste(anhaenge, beiTitel = { anhang, titel ->
                    beiAnhangTitel(anhang.copy(name = titel.ifBlank { anhang.beschriftung }))
                })
            }

            if (notiz.zustand == Notizzustand.FERTIG) {
                Spacer(Modifier.height(10.dp))
                // Eine einzige Reihe, alle Aktionen nebeneinander: Vorlesen, Verbessern,
                // Nachtrag, Verschieben, Kopieren, Löschen. Alle tragen dieselbe Farbe und
                // Stärke — keines soll durch Aufmachung wichtiger scheinen als die anderen.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Kartenknopf(
                        symbol = if (liestVor) Icons.Filled.StopCircle else Icons.Outlined.VolumeUp,
                        beschreibung = if (liestVor) "Vorlesen anhalten" else "Notiz vorlesen",
                        farbe = farben.textMittel,
                        beiDruck = beiVorlesen,
                    )
                    Spacer(Modifier.width(4.dp))
                    if (notiz.istVerbessert) {
                        Kartenknopf(
                            symbol = Icons.Outlined.Undo,
                            beschreibung = "Verbesserung rückgängig machen",
                            farbe = farben.textMittel,
                            beiDruck = beiRueckgaengig,
                        )
                    } else {
                        Kartenknopf(
                            symbol = Icons.Outlined.AutoFixHigh,
                            beschreibung = "Text verbessern",
                            farbe = if (verbessertGerade) farben.akzent else farben.textMittel,
                            beiDruck = beiVerbessern,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Kartenknopf(
                        symbol = Icons.Outlined.Mic,
                        beschreibung = "Nachtrag einsprechen",
                        farbe = farben.textMittel,
                        beiDruck = beiNachtrag,
                    )
                    Spacer(Modifier.width(4.dp))
                    Kartenknopf(
                        symbol = Icons.Outlined.DriveFileMove,
                        beschreibung = "In andere Sitzung verschieben",
                        farbe = farben.textMittel,
                        beiDruck = beiVerschieben,
                    )
                    Spacer(Modifier.width(4.dp))
                    Kartenknopf(
                        symbol = Icons.Outlined.ContentCopy,
                        beschreibung = "Text kopieren",
                        farbe = farben.textMittel,
                        beiDruck = beiKopieren,
                    )
                    Spacer(Modifier.width(4.dp))
                    Kartenknopf(
                        symbol = Icons.Outlined.DeleteOutline,
                        beschreibung = "Notiz löschen",
                        farbe = farben.textMittel,
                        beiDruck = beiLoeschen,
                    )
                }
            }
        }
    }
}

@Composable
private fun Kopfzeile(notiz: Notiz, farbeUeberschrift: Color, farbeZeit: Color) {
    val schrift = Schriften
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            // M-08: bis die KI-Überschrift da ist, steht die Uhrzeit als Platzhalter — und
            // wird von der Überschrift überblendet, nicht ersetzt.
            androidx.compose.animation.AnimatedVisibility(
                visible = notiz.ueberschrift.isNullOrBlank(),
                enter = fadeIn(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)),
                exit = fadeOut(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)),
            ) {
                Text(
                    Repository.uhrzeit(notiz.erstelltAm),
                    style = schrift.kartenUeberschrift,
                    color = farbeZeit,
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !notiz.ueberschrift.isNullOrBlank(),
                enter = fadeIn(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)) +
                    slideInVertically(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)) { 8 },
                exit = fadeOut(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)),
            ) {
                Text(
                    notiz.ueberschrift.orEmpty(),
                    style = schrift.kartenUeberschrift,
                    color = farbeUeberschrift,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(Repository.zeitpunkt(notiz.letzteTaetigkeit), style = schrift.zeitstempel, color = farbeZeit)
    }
}

/**
 * Der Notiztext in seinen Abschnitten: der Ursprung zuerst, dann jeder Nachtrag unter
 * seiner kleinen Überschrift mit Trennstrich — so sieht man auf einen Blick, wo der
 * Nachtrag beginnt und wann er gesprochen wurde.
 *
 * Die Absatzzählung des Vorlesers läuft über den **ganzen** Text, Überschriftenzeilen
 * eingerechnet; hier wird derselbe Zähler abschnittsweise weitergeführt, damit die
 * Hervorhebung des gesprochenen Absatzes an der richtigen Stelle leuchtet.
 */
@Composable
private fun NachtragsAbschnitte(text: String, liestVor: Boolean, vorleseAbsatz: Int) {
    val farben = Farben
    val schrift = Schriften
    val abschnitte = remember(text) { Nachtraege.abschnitte(text) }
    var zaehler = 0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        abschnitte.forEach { abschnitt ->
            if (abschnitt.nachtragVom != null) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Notes,
                            null,
                            Modifier.size(14.dp),
                            tint = farben.akzent,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Nachtrag vom ${abschnitt.nachtragVom}",
                            style = schrift.zeitstempel,
                            color = farben.akzent,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
                }
                // Die Überschriftenzeile ist für den Vorleser ein eigener Absatz.
                zaehler += 1
            }
            val absaetze = remember(abschnitt.text) { Absaetze.teile(abschnitt.text) }
            AbsatzText(
                text = abschnitt.text,
                hervorgehobenerAbsatz =
                    if (liestVor && vorleseAbsatz in zaehler until zaehler + absaetze.size) {
                        vorleseAbsatz - zaehler
                    } else {
                        -1
                    },
            )
            zaehler += absaetze.size
        }
    }
}

/**
 * Der Text, in Absätzen — und mit der Hervorhebung des gerade gesprochenen (M-09).
 *
 * Zerlegt wird mit **derselben** Funktion, die auch der Vorleser benutzt. Zwei getrennte
 * Zerlegungen würden bei jedem längeren Absatz auseinanderlaufen, und die Hervorhebung
 * stünde beim falschen Text.
 */
@Composable
fun AbsatzText(
    text: String,
    hervorgehobenerAbsatz: Int,
    stil: androidx.compose.ui.text.TextStyle = Schriften.notiztext,
) {
    val farben = Farben
    // Nur neu zerlegen, wenn sich der Text ändert — nicht bei jedem Scrollschritt und
    // nicht bei jedem Wechsel des vorgelesenen Absatzes.
    val absaetze = remember(text) { Absaetze.teile(text) }
    if (absaetze.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        absaetze.forEachIndexed { nr, absatz ->
            val hervor = nr == hervorgehobenerAbsatz
            val staerke by animateFloatAsState(
                targetValue = if (hervor) 1f else 0f,
                animationSpec = tween(dauer(Dauern.STANDARD), easing = Kurven.standard),
                label = "absatz$nr",
            )
            Text(
                text = absatz,
                style = stil,
                color = farben.textStark,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        farben.akzentGedeckt.copy(alpha = farben.akzentGedeckt.alpha * staerke),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = if (staerke > 0f) 6.dp else 0.dp, vertical = if (staerke > 0f) 4.dp else 0.dp),
            )
        }
    }
}

@Composable
private fun HinweisZeile(symbol: @Composable () -> Unit, text: String, farbe: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        symbol()
        Spacer(Modifier.width(6.dp))
        Text(text, style = Schriften.zeitstempel, color = farbe)
    }
}

/**
 * Die drei Punkte, die nacheinander aufleuchten (`03-MOTION-SPEC.md` §7): je 160 ms versetzt,
 * Periode 1200 ms.
 */
@Composable
fun WanderndePunkte(farbe: Color) {
    val takt = rememberInfiniteTransition(label = "punkte")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { nr ->
            val wert by takt.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = nr * 160, easing = Kurven.puls),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "punkt$nr",
            )
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .size(7.dp)
                    .scale(wert)
                    .background(farbe.copy(alpha = wert), RoundedCornerShape(50)),
            )
        }
    }
}

/**
 * Ein kleiner Knopf an der Karte: 20 dp Symbol, 36 dp Fläche — in einer **44 dp großen**
 * Tippfläche, wie `02-UI-SPEC.md` §9 sie überall verlangt.
 *
 * Die Welle bekommt ausdrücklich Farbe und Durchmesser aus dem Spec (36 dp, `akzentGedeckt`)
 * statt der Material-Vorgabe — sonst läge sie über der ganzen 44-dp-Fläche und wirkte plump.
 */
@Composable
fun Kartenknopf(
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    beschreibung: String,
    farbe: Color,
    beiDruck: () -> Unit,
) {
    val farben = Farben
    Box(
        modifier = Modifier.size(Masse.tippflaeche),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Masse.kartenSymbolFlaeche)
                .clip(RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 18.dp, color = farben.akzent),
                    onClick = beiDruck,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol, beschreibung, Modifier.size(Masse.kartenSymbol), tint = farbe)
        }
    }
}
