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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Notiz
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
    /** Wird diese Notiz gerade in der Karte bearbeitet? */
    bearbeitet: Boolean,
    /** Der Stand des Bearbeitungsfeldes — mit Cursorstelle, deshalb kein blosser String. */
    entwurf: TextFieldValue,
    beiVorlesen: () -> Unit,
    beiVerbessern: () -> Unit,
    beiRueckgaengig: () -> Unit,
    beiMenue: () -> Unit,
    beiWiederholen: () -> Unit,
    beiEinstellungen: () -> Unit,
    /** Kurzer Tipp in den Text: die Zeichenstelle, an der der Cursor stehen soll. */
    beiTippenImText: (Int) -> Unit,
    beiEntwurf: (TextFieldValue) -> Unit,
    beiFertig: () -> Unit,
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
            // Nur wo kein bearbeitbarer Text steht: dort trägt der Rahmen den langen
            // Druck. Über dem Text tut das der Text selbst — zwei Gestenerkenner um
            // dieselbe Fläche vertragen sich nicht, und der äussere gewinnt immer.
            .combinedClickable(
                onLongClick = beiMenue,
                onClick = {},
                enabled = notiz.zustand != Notizzustand.FERTIG,
            )
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

                Notizzustand.FERTIG -> BearbeitbarerText(
                    text = notiz.text,
                    hervorgehobenerAbsatz = if (liestVor) vorleseAbsatz else -1,
                    bearbeitet = bearbeitet,
                    entwurf = entwurf,
                    beiTippenImText = beiTippenImText,
                    beiEntwurf = beiEntwurf,
                    beiFertig = beiFertig,
                    beiMenue = beiMenue,
                )
            }

            if (notiz.zustand == Notizzustand.FERTIG && bearbeitet) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Ein sichtbarer Ausgang. Das Feld verliesse den Bearbeitungszustand
                    // zwar auch beim Fokusverlust — aber ein Knopf sagt, dass man fertig
                    // ist, statt dass man es erraten muss.
                    TextButton(onClick = beiFertig) {
                        Text("Fertig", style = schrift.knopf, color = farben.akzent)
                    }
                }
            }

            if (notiz.zustand == Notizzustand.FERTIG && !bearbeitet) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                        symbol = if (liestVor) Icons.Filled.StopCircle else Icons.Outlined.VolumeUp,
                        beschreibung = if (liestVor) "Vorlesen anhalten" else "Notiz vorlesen",
                        farbe = if (liestVor) farben.akzent else farben.textMittel,
                        beiDruck = beiVorlesen,
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
        Text(Repository.zeitpunkt(notiz.erstelltAm), style = schrift.zeitstempel, color = farbeZeit)
    }
}

/**
 * Der Text, in Absätzen — und mit der Hervorhebung des gerade gesprochenen (M-09).
 *
 * Zerlegt wird mit **derselben** Funktion, die auch der Vorleser benutzt. Zwei getrennte
 * Zerlegungen würden bei jedem längeren Absatz auseinanderlaufen, und die Hervorhebung
 * stünde beim falschen Text.
 */
/**
 * **Der Notiztext — antippbar zum Bearbeiten.**
 *
 * Im Ruhezustand steht er in Absätzen da und trägt die Vorlese-Hervorhebung. Ein **kurzer**
 * Tipp verwandelt ihn in ein Eingabefeld, dessen Cursor genau an der angetippten Stelle
 * steht — nicht am Anfang und nicht am Ende.
 *
 * Warum das nicht einfach ein dauerhaftes Eingabefeld ist: Die Vorlese-Hervorhebung (M-09)
 * braucht Absätze als eigene Bauteile, ein Eingabefeld kennt nur einen durchgehenden Text.
 * Deshalb zwei Zustände — und der Übergang gehört dem kurzen Tipp.
 */
@Composable
private fun BearbeitbarerText(
    text: String,
    hervorgehobenerAbsatz: Int,
    bearbeitet: Boolean,
    entwurf: TextFieldValue,
    beiTippenImText: (Int) -> Unit,
    beiEntwurf: (TextFieldValue) -> Unit,
    beiFertig: () -> Unit,
    beiMenue: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    if (bearbeitet) {
        val fokus = remember { FocusRequester() }
        // Fokus und Tastatur kommen erst, wenn das Feld wirklich steht. Ein Aufruf im
        // selben Zeichendurchgang ginge ins Leere.
        LaunchedEffect(Unit) { runCatching { fokus.requestFocus() } }
        // Ob der Fokus jemals da war. Ohne diese Merkung beendet der allererste
        // `onFocusChanged`-Ruf (der immer mit „kein Fokus" kommt) die Bearbeitung sofort.
        var hatteFokus by remember { mutableStateOf(false) }

        BasicTextField(
            value = entwurf,
            onValueChange = beiEntwurf,
            textStyle = schrift.notiztext.copy(color = farben.textStark),
            cursorBrush = SolidColor(farben.akzent),
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.hintergrund, RoundedCornerShape(8.dp))
                .border(1.dp, farben.akzent, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .focusRequester(fokus)
                .onFocusChanged { zustand ->
                    // Wer woanders hintippt, ist fertig — das Gespeicherte geht nicht
                    // verloren, nur weil kein Knopf gedrückt wurde. Aber erst, wenn der
                    // Fokus wirklich einmal hier war.
                    if (zustand.isFocused) {
                        hatteFokus = true
                    } else if (hatteFokus) {
                        beiFertig()
                    }
                },
        )
        return
    }

    // Nur neu zerlegen, wenn sich der Text ändert — nicht bei jedem Scrollschritt und
    // nicht bei jedem Wechsel des vorgelesenen Absatzes.
    val absaetze = remember(text) { Absaetze.teile(text) }
    if (absaetze.isEmpty()) return

    // Wo jeder Absatz im **Originaltext** beginnt. `Absaetze.teile` glättet mehrfache
    // Leerzeichen und Zeilenumbrüche; die Stelle im geglätteten Absatz ist deshalb nicht
    // ohne Weiteres die Stelle im Original. Gesucht wird ab dem Ende des vorigen Absatzes,
    // damit ein zweimal vorkommender Absatz nicht auf den ersten Treffer fällt.
    val anfaenge = remember(text, absaetze) {
        var ab = 0
        absaetze.map { absatz ->
            val gefunden = text.indexOf(absatz, ab)
            val start = if (gefunden >= 0) gefunden else ab
            ab = start + absatz.length
            start
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        absaetze.forEachIndexed { nr, absatz ->
            val hervor = nr == hervorgehobenerAbsatz
            val staerke by animateFloatAsState(
                targetValue = if (hervor) 1f else 0f,
                animationSpec = tween(dauer(Dauern.STANDARD), easing = Kurven.standard),
                label = "absatz$nr",
            )
            var aufteilung by remember(absatz) { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = absatz,
                style = schrift.notiztext,
                color = farben.textStark,
                onTextLayout = { aufteilung = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        farben.akzentGedeckt.copy(alpha = farben.akzentGedeckt.alpha * staerke),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(
                        horizontal = if (staerke > 0f) 6.dp else 0.dp,
                        vertical = if (staerke > 0f) 4.dp else 0.dp,
                    )
                    .pointerInput(absatz, anfaenge) {
                        detectTapGestures(
                            onLongPress = { beiMenue() },
                            onTap = { stelle ->
                                // Aus dem Berührungspunkt wird die Zeichenstelle im Absatz —
                                // und daraus die Stelle im ganzen Notiztext.
                                val imAbsatz = aufteilung?.getOffsetForPosition(stelle) ?: 0
                                beiTippenImText(
                                    (anfaenge.getOrElse(nr) { 0 } + imAbsatz).coerceIn(0, text.length),
                                )
                            },
                        )
                    },
            )
        }
    }
}

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
