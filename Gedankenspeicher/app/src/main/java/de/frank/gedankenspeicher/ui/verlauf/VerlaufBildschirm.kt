package de.frank.gedankenspeicher.ui.verlauf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.Verlaufseintrag
import de.frank.gedankenspeicher.ui.Verlaufszustand
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.PulsierenderRing
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.dauer
import de.frank.gedankenspeicher.ui.theme.glasleiste
import de.frank.gedankenspeicher.ui.theme.merkeDruck
import de.frank.gedankenspeicher.ui.theme.schimmer
import de.frank.gedankenspeicher.ui.theme.sinktEin

/**
 * **B-01 — der Verlauf.** Der Startbildschirm und der Ort, an dem die App lebt.
 *
 * Aufbau: Glas-Kopfleiste, darunter die scrollende Liste (neueste unten, wie ein Chat),
 * darunter die Glas-Fußleiste mit Textfeld, KI-Knopf und Aufnahmeknopf.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun VerlaufBildschirm(
    zustand: Verlaufszustand,
    istDunkel: Boolean,
    beiSchublade: () -> Unit,
    beiSuche: () -> Unit,
    beiErscheinungUmschalten: () -> Unit,
    beiEinstellungen: () -> Unit,
    beiEntwurf: (String) -> Unit,
    beiSenden: () -> Unit,
    beiAufnahme: () -> Unit,
    beiKi: () -> Unit,
    beiVorlesen: (String, String) -> Unit,
    beiVerbessern: (Notiz) -> Unit,
    beiRueckgaengig: (Notiz) -> Unit,
    beiNotizMenue: (Notiz) -> Unit,
    beiAntwortMenue: (de.frank.gedankenspeicher.data.KiAntwort) -> Unit,
    beiWiederholen: (Notiz) -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    val listenzustand = rememberLazyListState()

    // Der Verlauf steht an seinem Ende: die neueste Notiz ist die, die interessiert.
    //
    // **Ausser** nach einem Sprung aus der Suche: dann gilt das Sprungziel. Ohne diese
    // Ausnahme überfuhr der Auto-Scroll den Treffer und landete wieder unten — die Karte
    // leuchtete auf, aber ausserhalb des Bildes.
    LaunchedEffect(zustand.eintraege.size, zustand.sitzung?.id, zustand.hebeHervor) {
        if (zustand.eintraege.isEmpty()) return@LaunchedEffect
        val ziel = zustand.hebeHervor
        if (ziel != null) {
            val stelle = zustand.eintraege.indexOfFirst {
                it is Verlaufseintrag.NotizEintrag && it.notiz.id == ziel
            }
            if (stelle >= 0) {
                listenzustand.animateScrollToItem(stelle)
                return@LaunchedEffect
            }
        }
        listenzustand.animateScrollToItem(zustand.eintraege.size - 1 + if (zustand.wertetAus) 1 else 0)
    }

    Column(Modifier.fillMaxSize().background(farben.hintergrund)) {

        // ---- Kopfleiste (Glas, 56 dp) — sie steht fest und fährt beim Scrollen nicht weg.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glasleiste(farben)
                .statusBarsPadding()
                .height(Masse.kopfleiste)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = beiSchublade) {
                Icon(Icons.Outlined.Menu, "Sitzungen", tint = farben.textMittel)
            }
            Text(
                text = zustand.sitzung?.titel.orEmpty(),
                style = schrift.bildschirmtitel,
                color = farben.textStark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            IconButton(onClick = beiSuche) {
                Icon(Icons.Outlined.Search, "Suchen", tint = farben.textMittel)
            }
            // Zwischen Lupe und Zahnrad: hell ↔ dunkel, ohne den Umweg über die
            // Einstellungen. Das Symbol zeigt, wohin es geht, nicht wo man steht —
            // ein Mond bei Tag heisst „hier wird es dunkel".
            IconButton(onClick = beiErscheinungUmschalten) {
                Icon(
                    if (istDunkel) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    if (istDunkel) "Auf hell umschalten" else "Auf dunkel umschalten",
                    tint = farben.textMittel,
                )
            }
            IconButton(onClick = beiEinstellungen) {
                Icon(Icons.Outlined.Settings, "Einstellungen", tint = farben.textMittel)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))

        // ---- Der Verlauf
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                zustand.laedt -> LadendeListe()
                zustand.eintraege.isEmpty() && !zustand.wertetAus -> LeererVerlauf()
                else -> LazyColumn(
                    state = listenzustand,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Masse.seitenrand,
                        end = Masse.seitenrand,
                        top = Masse.kartenAbstand,
                        bottom = Masse.kartenAbstand,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Masse.kartenAbstand),
                ) {
                    items(zustand.eintraege, key = { eintrag ->
                        when (eintrag) {
                            is Verlaufseintrag.NotizEintrag -> "n${eintrag.notiz.id}"
                            is Verlaufseintrag.AntwortEintrag -> "a${eintrag.antwort.id}"
                        }
                    }) { eintrag ->
                        when (eintrag) {
                            is Verlaufseintrag.NotizEintrag -> {
                                val notiz = eintrag.notiz
                                val kennung = "notiz:${notiz.id}"
                                Notizkarte(
                                    notiz = notiz,
                                    liestVor = zustand.liestVor == kennung,
                                    vorleseAbsatz = zustand.vorleseAbsatz,
                                    verbessertGerade = notiz.id in zustand.verbessertGerade,
                                    hervorgehoben = zustand.hebeHervor == notiz.id,
                                    beiVorlesen = { beiVorlesen(kennung, notiz.text) },
                                    beiVerbessern = { beiVerbessern(notiz) },
                                    beiRueckgaengig = { beiRueckgaengig(notiz) },
                                    beiMenue = { beiNotizMenue(notiz) },
                                    beiWiederholen = { beiWiederholen(notiz) },
                                    beiEinstellungen = beiEinstellungen,
                                )
                            }

                            is Verlaufseintrag.AntwortEintrag -> {
                                val antwort = eintrag.antwort
                                val kennung = "antwort:${antwort.id}"
                                KiKarte(
                                    antwort = antwort,
                                    liestVor = zustand.liestVor == kennung,
                                    vorleseAbsatz = zustand.vorleseAbsatz,
                                    beiVorlesen = { beiVorlesen(kennung, antwort.text) },
                                    beiMenue = { beiAntwortMenue(antwort) },
                                )
                            }
                        }
                    }

                    if (zustand.wertetAus) {
                        item(key = "auswertung-laeuft") { KiKarteEntsteht() }
                    }
                }
            }
        }

        // ---- Fußleiste (Glas)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glasleiste(farben, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))

            // Über der Leiste: die laufende Aufnahmedauer.
            if (zustand.nimmtAuf) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(dauerText(zustand.aufnahmeDauerMs), style = schrift.zeitstempel, color = farben.akzent)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Masse.fussleiste)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Notizfeld(
                    wert = zustand.entwurf,
                    gesperrt = zustand.nimmtAuf,
                    beiAenderung = beiEntwurf,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                KiKnopf(aktiv = zustand.wertetAus, beiDruck = beiKi)
                Spacer(Modifier.width(8.dp))
                Aufnahmeknopf(
                    nimmtAuf = zustand.nimmtAuf,
                    hatText = zustand.entwurf.isNotBlank(),
                    gesperrt = zustand.mikrofonAbgelehnt && zustand.entwurf.isBlank(),
                    pegel = zustand.pegel,
                    beiDruck = { if (zustand.entwurf.isNotBlank()) beiSenden() else beiAufnahme() },
                )
            }
        }
    }
}

@Composable
private fun Notizfeld(
    wert: String,
    gesperrt: Boolean,
    beiAenderung: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = Farben
    val schrift = Schriften
    Box(
        modifier = modifier
            .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.eingabeRadius))
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.eingabeRadius))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (wert.isEmpty()) {
            Text("Notiz tippen …", style = schrift.eingabefeld, color = farben.textSchwach)
        }
        BasicTextField(
            value = wert,
            onValueChange = beiAenderung,
            enabled = !gesperrt,
            textStyle = schrift.eingabefeld.copy(color = farben.textStark),
            cursorBrush = SolidColor(farben.akzent),
            // Wächst bis sechs Zeilen und scrollt dann (`02-UI-SPEC.md` §4).
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * **Der KI-Knopf** — nach `00-PROJEKT.md` §0 der Kern der App. Er ist deshalb der einzige
 * Knopf neben dem Aufnahmeknopf, der einen Rand in der Akzentfarbe trägt.
 */
@Composable
private fun KiKnopf(aktiv: Boolean, beiDruck: () -> Unit) {
    val farben = Farben
    val druck = merkeDruck()
    val dreh by animateFloatAsState(
        targetValue = if (aktiv) 1f else 0f,
        animationSpec = tween(dauer(Dauern.KURZ), easing = Kurven.kurz),
        label = "kiWartet",
    )
    Box(
        modifier = Modifier
            .size(Masse.kiKnopf)
            .sinktEin(druck)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, farben.akzent, RoundedCornerShape(50))
            .background(if (aktiv) farben.akzentGedeckt else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(interactionSource = druck, indication = null, enabled = !aktiv, onClick = beiDruck),
        contentAlignment = Alignment.Center,
    ) {
        if (aktiv) {
            WanderndePunkte(farben.akzent)
        } else {
            Icon(
                Icons.Outlined.AutoAwesome,
                "Notizen auswerten",
                Modifier.size(22.dp).scale(1f - dreh * 0.2f),
                tint = farben.akzent,
            )
        }
    }
}

/**
 * **Der Aufnahmeknopf** — das einzige Bauteil, das leuchtet (`02-UI-SPEC.md` §1).
 *
 * Steht Text im Feld, wird er zum Senden-Knopf (M-06): das alte Symbol dreht heraus, das
 * neue herein.
 */
@Composable
private fun Aufnahmeknopf(
    nimmtAuf: Boolean,
    hatText: Boolean,
    gesperrt: Boolean,
    pegel: Float,
    beiDruck: () -> Unit,
) {
    val farben = Farben
    val druck = merkeDruck()
    // `dauer()` ist selbst @Composable und darf deshalb nicht erst im `transitionSpec`
    // aufgerufen werden — dort läuft kein Composable-Kontext mehr.
    val kurz = dauer(Dauern.KURZ)
    PulsierenderRing(aktiv = nimmtAuf, pegel = pegel, farbe = farben.akzent) {
        Box(
            modifier = Modifier
                .size(Masse.aufnahmeknopf)
                .sinktEin(druck, auf = 0.92f)
                .clip(RoundedCornerShape(50))
                .background(if (nimmtAuf) farben.akzentGedeckt else farben.hintergrundErhoben)
                .border(2.dp, if (gesperrt) farben.textSchwach else farben.akzent, RoundedCornerShape(50))
                .clickable(interactionSource = druck, indication = null, onClick = beiDruck),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = Triple(nimmtAuf, hatText, gesperrt),
                transitionSpec = {
                    (fadeIn(tween(kurz)) + scaleIn(tween(kurz), 0.7f)) togetherWith
                        (fadeOut(tween(kurz)) + scaleOut(tween(kurz), 0.7f))
                },
                label = "aufnahmesymbol",
            ) { (auf, text, aus) ->
                val symbol = when {
                    text -> Icons.Outlined.ArrowUpward
                    auf -> Icons.Filled.Stop
                    else -> Icons.Outlined.Mic
                }
                val beschreibung = when {
                    text -> "Notiz senden"
                    auf -> "Aufnahme beenden"
                    else -> "Notiz einsprechen"
                }
                Icon(
                    symbol,
                    beschreibung,
                    Modifier.size(26.dp),
                    tint = if (aus) farben.textSchwach else farben.akzent,
                )
            }
        }
    }
}

/** Der Leerzustand von B-01 (`02-UI-SPEC.md`, Zustand "Leer"). */
@Composable
private fun LeererVerlauf() {
    val farben = Farben
    val schrift = Schriften
    Column(
        modifier = Modifier.fillMaxSize().padding(Masse.seitenrand * 2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Mic,
            null,
            Modifier.size(72.dp),
            tint = farben.textSchwach.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(20.dp))
        Text("Sprich einfach los.", style = schrift.bildschirmtitel, color = farben.textMittel)
        Spacer(Modifier.height(8.dp))
        Text(
            "Alles, was dir zu diesem Thema einfällt — die KI fragst du später.",
            style = schrift.einstellungErklaerung,
            color = farben.textSchwach,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Drei Platzhalterkarten mit Schimmer, solange der Verlauf lädt. */
@Composable
private fun LadendeListe() {
    val farben = Farben
    Column(
        modifier = Modifier.fillMaxSize().padding(Masse.seitenrand),
        verticalArrangement = Arrangement.spacedBy(Masse.kartenAbstand),
    ) {
        listOf(96.dp, 128.dp, 80.dp).forEach { hoehe ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(hoehe)
                    .schimmer(farben, RoundedCornerShape(Masse.karteRadius)),
            )
        }
    }
}

/** mm:ss — die laufende Aufnahmedauer. */
private fun dauerText(ms: Long): String {
    val sekunden = ms / 1000
    return "%02d:%02d".format(sekunden / 60, sekunden % 60)
}

/** Wird von der Schublade gebraucht, um die Sitzungszeile zu beschriften. */
fun sitzungUntertitel(anzahl: Int, letzte: Long?): String = buildString {
    append(anzahl).append(if (anzahl == 1) " Notiz" else " Notizen")
    if (letzte != null) {
        append(" · ").append(de.frank.gedankenspeicher.data.Repository.zeitpunkt(letzte))
    }
}
