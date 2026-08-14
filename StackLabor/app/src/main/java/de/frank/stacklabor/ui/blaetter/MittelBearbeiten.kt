package de.frank.stacklabor.ui.blaetter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.entitaeten.Darreichungsform
import de.frank.stacklabor.daten.entitaeten.Einheit
import de.frank.stacklabor.daten.entitaeten.EintragMitMittel
import de.frank.stacklabor.daten.entitaeten.MittelE
import de.frank.stacklabor.daten.entitaeten.StackEintragE
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.VerlaufsKante
import de.frank.stacklabor.ui.bausteine.glasflaeche
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import de.frank.stacklabor.daten.entitaeten.Loeslichkeit as LoeslichkeitE

/*
 * PLAN (gilt fuer alle fuenf Blaetter dieses Pakets)
 * 1. Ein gemeinsamer `BlattRahmen` traegt das gemessene Verhalten aus M-20: von unten
 *    einfahren (300 ms, `Kurven.blatt`), dahinter 32 % Abdunklung, Radius 22 dp oben.
 * 2. Der Rahmen schliesst NICHT sofort, sondern faehrt zuerst aus und meldet danach
 *    `aufSchliessen()` — sonst waere die Ausfahrt nie sichtbar.
 * 3. Jedes Blatt haelt seinen Formularzustand lokal und schreibt erst beim Sichern in das
 *    Repository; so bleibt eine abgebrochene Bearbeitung folgenlos.
 * 4. Kein Bedienelement ist tot: ein gesperrtes „Sichern" zeigt beim Tippen den Grund an,
 *    statt gar nicht zu reagieren.
 */

/**
 * **B-05 — Mittel bearbeiten (F-03).**
 *
 * Zwei erkennbar getrennte Bereiche: **Stammdaten** gelten ueberall (und sagen das mit
 * „Gilt in N Stacks"), **In diesem Stack** gilt nur hier. Genau diese Trennung ist der Kern
 * von F-03 — ohne sie waere nie klar, was eine Aenderung anderswo anrichtet.
 */
@Composable
fun MittelBearbeitenBlatt(
    app: StackLaborApp,
    eintragId: String,
    aufSchliessen: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val eintrag by app.datenbank.stackDao().eintrag(eintragId)
        .collectAsStateWithLifecycle(initialValue = null)

    val mittelId = eintrag?.mittelId
    val mittelFluss: Flow<MittelE?> = remember(mittelId) {
        if (mittelId == null) flowOf(null) else app.datenbank.mittelDao().mittel(mittelId)
    }
    val mittel by mittelFluss.collectAsStateWithLifecycle(initialValue = null)

    val anzahlFluss: Flow<Int> = remember(mittelId) {
        if (mittelId == null) flowOf(0) else app.datenbank.mittelDao().anzahlStacksFuerMittel(mittelId)
    }
    val anzahlStacks by anzahlFluss.collectAsStateWithLifecycle(initialValue = 0)

    val stackId = eintrag?.stackId
    val geschwisterFluss: Flow<List<EintragMitMittel>> = remember(stackId) {
        if (stackId == null) flowOf(emptyList()) else app.datenbank.stackDao().eintraegeMitMittel(stackId)
    }
    val geschwister by geschwisterFluss.collectAsStateWithLifecycle(initialValue = emptyList())

    // ── Formularzustand ──────────────────────────────────────────────────────────
    var geladen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var loeslichkeit by remember { mutableStateOf(LoeslichkeitE.WASSER) }
    var form by remember { mutableStateOf(Darreichungsform.KAPSEL) }
    var hersteller by remember { mutableStateOf("") }
    var durchfallrisiko by remember { mutableStateOf(false) }
    var beistoffe by remember { mutableStateOf("") }

    var stueckzahl by remember { mutableStateOf("1") }
    var menge by remember { mutableStateOf("") }
    var einheit by remember { mutableStateOf(Einheit.MG) }
    var varianteB by remember { mutableStateOf(false) }
    var mengeB by remember { mutableStateOf("") }
    var taeglich by remember { mutableStateOf(true) }
    var frequenzTage by remember { mutableStateOf("2") }
    var alterniertMit by remember { mutableStateOf(setOf<String>()) }
    var zusatztext by remember { mutableStateOf("") }
    var grundGezeigt by remember { mutableStateOf(false) }

    LaunchedEffect(eintrag, mittel) {
        val e = eintrag
        val m = mittel
        if (!geladen && e != null && m != null) {
            name = m.name
            loeslichkeit = m.loeslichkeit
            form = m.darreichungsform
            hersteller = m.hersteller
            durchfallrisiko = m.durchfallrisiko
            beistoffe = m.beistoffe
            stueckzahl = e.stueckzahl.toString()
            menge = e.mengeJeStueck?.let { kommazahl(it) } ?: ""
            einheit = e.einheit
            varianteB = e.dosisVarianteB != null
            mengeB = e.dosisVarianteB?.let { kommazahl(it) } ?: ""
            taeglich = e.frequenzTage <= 1
            frequenzTage = if (e.frequenzTage > 1) e.frequenzTage.toString() else "2"
            alterniertMit = e.alterniertMit.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            zusatztext = e.zusatztext
            geladen = true
        }
    }

    // ── Ableitungen fuer Vorschau und Sperre ─────────────────────────────────────
    val stueck = stueckzahl.toIntOrNull() ?: 0
    val mengeWert = menge.replace(',', '.').toDoubleOrNull()
    val mengeFehler = mengeWert == null || mengeWert <= 0.0 || stueck <= 0
    val nameFehler = name.isBlank()
    val darfSichern = !mengeFehler && !nameFehler && eintrag != null && mittel != null
    val mengeBWert = mengeB.replace(',', '.').toDoubleOrNull()

    BlattRahmen(
        titel = "Mittel bearbeiten",
        untertitel = mittel?.name.orEmpty(),
        aufSchliessen = aufSchliessen,
        sockel = { schliessen ->
            Column {
                if (grundGezeigt && !darfSichern) {
                    Text(
                        if (nameFehler) "Der Name darf nicht leer sein." else "Bitte Stueckzahl und Menge eintragen.",
                        style = Schrift.klein,
                        color = Farben.rot,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                BlattKnopf("Sichern", gesperrt = !darfSichern) {
                    val e = eintrag
                    val m = mittel
                    if (!darfSichern || e == null || m == null) {
                        grundGezeigt = true
                        return@BlattKnopf
                    }
                    bereich.launch {
                        app.repository.aktualisiereMittel(
                            m.copy(
                                name = name.trim(),
                                loeslichkeit = loeslichkeit,
                                darreichungsform = form,
                                hersteller = hersteller.trim(),
                                durchfallrisiko = durchfallrisiko,
                                beistoffe = beistoffe.trim(),
                            ),
                        )
                        app.repository.aktualisiereEintrag(
                            eintragMitFormular(
                                e, stueck, mengeWert, einheit, varianteB, mengeBWert,
                                taeglich, frequenzTage, alterniertMit, zusatztext,
                            ),
                        )
                        schliessen()
                    }
                }
            }
        },
    ) { _ ->
        // ── Bereich 1: Stammdaten ────────────────────────────────────────────────
        BlattRubrik("Stammdaten")
        BlattHinweis(
            if (anzahlStacks > 1) "Gilt in $anzahlStacks Stacks" else "Gilt in $anzahlStacks Stack",
        )
        BlattFeldZeile("Name", name, { name = it }, "Name des Mittels", fehler = nameFehler, maxZeichen = 80)

        BlattBeschriftung("Löslichkeit")
        BlattChipZeile {
            Chip("Wasser", loeslichkeit == LoeslichkeitE.WASSER) { loeslichkeit = LoeslichkeitE.WASSER }
            Spacer(Modifier.width(8.dp))
            Chip("Fett", loeslichkeit == LoeslichkeitE.FETT) { loeslichkeit = LoeslichkeitE.FETT }
            Spacer(Modifier.width(8.dp))
            Chip("Beides", loeslichkeit == LoeslichkeitE.BEIDES) { loeslichkeit = LoeslichkeitE.BEIDES }
        }

        BlattBeschriftung("Darreichungsform")
        BlattChipZeile {
            Darreichungsform.entries.forEachIndexed { stelle, wert ->
                if (stelle > 0) Spacer(Modifier.width(8.dp))
                Chip(formText(wert), form == wert) { form = wert }
            }
        }

        BlattFeldZeile("Hersteller", hersteller, { hersteller = it }, "z. B. Thorne", maxZeichen = 60)
        BlattSchalterZeile("Durchfallrisiko", durchfallrisiko) { durchfallrisiko = it }
        BlattFeldZeile(
            "Beistoffe", beistoffe, { beistoffe = it },
            "z. B. Magnesiumstearat, Cellulose", zeilen = 2, maxZeichen = 200,
        )

        BlattTrenner()

        // ── Bereich 2: nur in diesem Stack ───────────────────────────────────────
        BlattRubrik("In diesem Stack")
        BlattHinweis("Diese Angaben gelten nur hier, nicht im Katalog.")

        BlattBeschriftung("Stückzahl × Menge")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BlattFeld(
                wert = stueckzahl, aufAenderung = { stueckzahl = it.filter { z -> z.isDigit() } },
                platzhalter = "1", modifier = Modifier.width(64.dp),
                fehler = stueck <= 0, nurZahlen = true, maxZeichen = 3,
            )
            Text("×", style = Schrift.name, color = Farben.textSchwach, modifier = Modifier.padding(horizontal = 8.dp))
            BlattFeld(
                wert = menge, aufAenderung = { menge = it.filter { z -> z.isDigit() || z == ',' || z == '.' } },
                platzhalter = "80", modifier = Modifier.widthIn(min = 72.dp).weight(1f),
                fehler = mengeWert == null || mengeWert <= 0.0, nurZahlen = true, maxZeichen = 8,
            )
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.weight(1.1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Einheit.entries.forEachIndexed { stelle, wert ->
                    if (stelle > 0) Spacer(Modifier.width(6.dp))
                    Chip(einheitText(wert), einheit == wert) { einheit = wert }
                }
            }
        }
        // Live-Vorschau — rechnet bei jedem Tastendruck mit.
        Text(
            text = if (mengeFehler) "Menge fehlt — Sichern ist gesperrt"
            else dosisVorschau(stueck, mengeWert ?: 0.0, einheit),
            style = Schrift.klein,
            color = if (mengeFehler) Farben.rot else Farben.akzent,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        )

        BlattSchalterZeile("Zweite Dosis-Variante", varianteB) { varianteB = it }
        if (varianteB) {
            BlattFeldZeile(
                "Menge der Variante B", mengeB,
                { mengeB = it.filter { z -> z.isDigit() || z == ',' || z == '.' } },
                "z. B. 40", nurZahlen = true, maxZeichen = 8,
            )
            Text(
                if (mengeBWert == null || mengeBWert <= 0.0) "Ohne Menge bleibt Variante B leer."
                else dosisVorschau(stueck.coerceAtLeast(1), mengeBWert, einheit),
                style = Schrift.klein,
                color = if (mengeBWert == null || mengeBWert <= 0.0) Farben.textSchwach else Farben.akzent,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
        }

        BlattBeschriftung("Frequenz")
        BlattChipZeile {
            Chip("täglich", taeglich) { taeglich = true }
            Spacer(Modifier.width(8.dp))
            Chip("alle N Tage", !taeglich) { taeglich = false }
            if (!taeglich) {
                Spacer(Modifier.width(8.dp))
                BlattFeld(
                    wert = frequenzTage,
                    aufAenderung = { frequenzTage = it.filter { z -> z.isDigit() } },
                    platzhalter = "3", modifier = Modifier.width(64.dp),
                    nurZahlen = true, maxZeichen = 2,
                )
                Text(
                    "Tage",
                    style = Schrift.klein, color = Farben.textSchwach,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        BlattBeschriftung("alterniert mit")
        val andere = geschwister.filter { it.eintrag.id != eintragId }
        if (andere.isEmpty()) {
            BlattHinweis("In diesem Stack liegt kein weiteres Mittel.")
        } else {
            BlattChipZeile {
                andere.forEachIndexed { stelle, g ->
                    if (stelle > 0) Spacer(Modifier.width(8.dp))
                    val gewaehlt = g.eintrag.mittelId in alterniertMit
                    Chip(g.mittel.name, gewaehlt) {
                        alterniertMit = if (gewaehlt) alterniertMit - g.eintrag.mittelId
                        else alterniertMit + g.eintrag.mittelId
                    }
                }
            }
        }

        BlattFeldZeile(
            "Zusatztext für die KI", zusatztext, { zusatztext = it },
            "geht wörtlich in die Anfrage ein", zeilen = 3, maxZeichen = 300,
        )
        Text(
            "${zusatztext.length}/300",
            style = Schrift.winzig, color = Farben.textSchwach,
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 12.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Baut den geaenderten Eintrag — ausgelagert, damit der Sockel lesbar bleibt. */
private fun eintragMitFormular(
    e: StackEintragE,
    stueck: Int,
    mengeWert: Double?,
    einheit: Einheit,
    varianteB: Boolean,
    mengeBWert: Double?,
    taeglich: Boolean,
    frequenzTage: String,
    alterniertMit: Set<String>,
    zusatztext: String,
): StackEintragE = e.copy(
    stueckzahl = stueck.coerceAtLeast(1),
    mengeJeStueck = mengeWert,
    einheit = einheit,
    dosisVarianteB = if (varianteB) mengeBWert else null,
    frequenzTage = if (taeglich) 1 else (frequenzTage.toIntOrNull() ?: 2).coerceAtLeast(2),
    alterniertMit = alterniertMit.joinToString(","),
    zusatztext = zusatztext.trim(),
)

// ── Gemeinsame Bausteine aller fuenf Blaetter ───────────────────────────────────

/**
 * Der Rahmen jedes Blattes — M-20 exakt wie gemessen.
 *
 * Von unten einfahren in 300 ms mit `Kurven.blatt`, dahinter eine Abdunklung von 32 %,
 * oben 22 dp Radius (`Formen.blatt`). Bei „Bewegung reduzieren" faellt die Dauer auf 0 ms,
 * die Abdunklung bleibt — sie traegt die Aussage „dahinter ist gesperrt".
 *
 * Beim Schliessen faehrt das Blatt zuerst aus und meldet erst danach `aufSchliessen()`.
 */
@Composable
internal fun BlattRahmen(
    titel: String,
    aufSchliessen: () -> Unit,
    untertitel: String = "",
    sockel: (@Composable (schliessen: () -> Unit) -> Unit)? = null,
    schwebend: (@Composable BoxScope.(schliessen: () -> Unit) -> Unit)? = null,
    inhalt: @Composable ColumnScope.(schliessen: () -> Unit) -> Unit,
) {
    val reduziert = LocalBewegungReduziert.current
    var offen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { offen = true }

    val fortschritt by animateFloatAsState(
        targetValue = if (offen) 1f else 0f,
        animationSpec = tween(if (reduziert) 0 else Dauer.BLATT, easing = Kurven.blatt),
        finishedListener = { wert -> if (wert == 0f) aufSchliessen() },
        label = "blatt",
    )
    val schliessen: () -> Unit = { offen = false }
    BackHandler(enabled = true, onBack = schliessen)

    var hoehePx by remember { mutableIntStateOf(0) }
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maximaleHoehe = maxHeight * 0.92f

        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.32f * fortschritt))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = schliessen,
                )
                .semantics { contentDescription = "Blatt schließen" },
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maximaleHoehe)
                .onSizeChanged { hoehePx = it.height }
                // Vor der ersten Messung steht das Blatt ausserhalb des Bildes — sonst
                // blitzt es im ersten Bild an seiner Endlage auf.
                .offset {
                    IntOffset(0, if (hoehePx == 0) 4000 else ((1f - fortschritt) * hoehePx).roundToInt())
                }
                .clip(Formen.blatt)
                .background(Farben.flaeche)
                .border(1.dp, Farben.rand, Formen.blatt)
                .imePadding(),
        ) {
            // Griff — zeigt, dass das Blatt zu einem Rand gehoert.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Farben.rand, RoundedCornerShape(2.dp)),
            )
            Row(
                Modifier.fillMaxWidth().height(Masse.tippflaeche + 8.dp).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titel, style = Schrift.kopfzeile, color = Farben.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (untertitel.isNotBlank()) {
                        Text(
                            untertitel, style = Schrift.klein, color = Farben.textSchwach,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(Masse.tippflaeche)
                        .clickable(onClick = schliessen)
                        .semantics { contentDescription = "Schließen" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Farben.text, modifier = Modifier.size(22.dp))
                }
            }
            BlattTrenner()

            Box(Modifier.weight(1f, fill = false)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    inhalt(schliessen)
                }
                schwebend?.invoke(this, schliessen)
            }

            if (sockel != null) {
                VerlaufsKante()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .glasflaeche()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    sockel(schliessen)
                }
            }
            Spacer(Modifier.height(untenInset + 4.dp))
        }
    }
}

/** Abschnitts-Ueberschrift innerhalb eines Blattes. */
@Composable
internal fun BlattRubrik(text: String) {
    Text(
        text.uppercase(),
        style = Schrift.rubrik,
        color = Farben.textSchwach,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** Die kleine Beschriftung ueber einem Bedienelement. */
@Composable
internal fun BlattBeschriftung(text: String) {
    Text(
        text,
        style = Schrift.klein,
        color = Farben.textSchwach,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
    )
}

/** Eine erklaerende Zeile — z. B. „Gilt in 3 Stacks". */
@Composable
internal fun BlattHinweis(text: String, farbe: Color = Farben.textSchwach) {
    Text(text, style = Schrift.klein, color = farbe, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
}

/** Trennlinie zwischen zwei Bereichen eines Blattes. */
@Composable
internal fun BlattTrenner() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Farben.rand))
}

/** Eine waagerecht rollende Chip-Reihe — haelt auch sechs Einheiten auf 297 dp aus. */
@Composable
internal fun BlattChipZeile(inhalt: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        inhalt()
    }
}

/**
 * Ein Eingabefeld nach §5: Radius 10 dp, 1 dp Rand, im Fehlerfall `--rot`.
 * Die Hoehe liegt nie unter der Mindest-Tippflaeche von 44 dp.
 */
@Composable
internal fun BlattFeld(
    wert: String,
    aufAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
    fehler: Boolean = false,
    zeilen: Int = 1,
    maxZeichen: Int = 0,
    nurZahlen: Boolean = false,
) {
    val hoehe = if (zeilen <= 1) Masse.tippflaeche else Masse.tippflaeche + 20.dp * (zeilen - 1)
    Box(
        modifier
            .heightIn(min = hoehe)
            .clip(Formen.feld)
            .background(Farben.grund)
            .border(1.dp, if (fehler) Farben.rot else Farben.rand, Formen.feld)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = if (zeilen <= 1) Alignment.CenterStart else Alignment.TopStart,
    ) {
        if (wert.isEmpty()) {
            Text(platzhalter, style = Schrift.grund, color = Farben.textSchwach, maxLines = zeilen)
        }
        BasicTextField(
            value = wert,
            onValueChange = { neu -> if (maxZeichen <= 0 || neu.length <= maxZeichen) aufAenderung(neu) },
            textStyle = Schrift.grund.copy(color = Farben.text),
            singleLine = zeilen <= 1,
            maxLines = zeilen,
            cursorBrush = SolidColor(Farben.akzent),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (nurZahlen) KeyboardType.Number else KeyboardType.Text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Beschriftung und Feld als fertige Zeile. */
@Composable
internal fun BlattFeldZeile(
    bezeichnung: String,
    wert: String,
    aufAenderung: (String) -> Unit,
    platzhalter: String,
    fehler: Boolean = false,
    zeilen: Int = 1,
    maxZeichen: Int = 0,
    nurZahlen: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(bezeichnung, style = Schrift.klein, color = Farben.textSchwach)
        Spacer(Modifier.height(4.dp))
        BlattFeld(
            wert = wert, aufAenderung = aufAenderung, platzhalter = platzhalter,
            modifier = Modifier.fillMaxWidth(), fehler = fehler, zeilen = zeilen,
            maxZeichen = maxZeichen, nurZahlen = nurZahlen,
        )
    }
}

/** Eine Schalterzeile in voller Tippflaeche. */
@Composable
internal fun BlattSchalterZeile(bezeichnung: String, an: Boolean, aufAenderung: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Masse.tippflaeche + 8.dp)
            .clickable { aufAenderung(!an) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(bezeichnung, style = Schrift.grund, color = Farben.text, modifier = Modifier.weight(1f))
        Switch(checked = an, onCheckedChange = aufAenderung)
    }
}

/**
 * Der Knopf im Sockel eines Blattes.
 *
 * Ein gesperrter Knopf bleibt **anklickbar** und meldet den Grund — ein Knopf, der auf
 * Beruehrung gar nichts tut, ist aus Sicht des Benutzers kaputt.
 */
@Composable
internal fun BlattKnopf(
    beschriftung: String,
    gesperrt: Boolean = false,
    gefahr: Boolean = false,
    aufKlick: () -> Unit,
) {
    val flaeche = when {
        gesperrt -> Farben.deaktiviert
        gefahr -> Farben.rot
        else -> Farben.akzent
    }
    val schrift = if (gesperrt) Farben.textSchwach else if (Farben.istHell) Color.White else Farben.grund
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Masse.tippflaeche)
            .clip(Formen.knopf)
            .background(flaeche)
            .clickable(onClick = aufKlick),
        contentAlignment = Alignment.Center,
    ) {
        Text(beschriftung, style = Schrift.grundMittel, color = schrift)
    }
}

// ── Kleine Umrechnungen ────────────────────────────────────────────────────────

/** „2 × 80 mg = 160 mg" — dieselbe Schreibweise wie in der Mittel-Liste. */
internal fun dosisVorschau(stueck: Int, menge: Double, einheit: Einheit): String {
    val e = einheitText(einheit)
    return if (stueck > 1) {
        "$stueck × ${kommazahl(menge)} $e = ${kommazahl(menge * stueck)} $e"
    } else {
        "${kommazahl(menge)} $e"
    }
}

/** Ganze Zahlen ohne Nachkomma, sonst mit deutschem Komma. */
internal fun kommazahl(wert: Double): String =
    if (wert % 1.0 == 0.0) wert.toInt().toString() else wert.toString().replace('.', ',')

internal fun einheitText(einheit: Einheit): String = when (einheit) {
    Einheit.MG -> "mg"
    Einheit.UG -> "µg"
    Einheit.G -> "g"
    Einheit.ML -> "ml"
    Einheit.IE -> "IE"
    Einheit.STUECK -> "Stück"
}

internal fun formText(form: Darreichungsform): String = when (form) {
    Darreichungsform.KAPSEL -> "Kapsel"
    Darreichungsform.TABLETTE -> "Tablette"
    Darreichungsform.LOEFFEL -> "Löffel"
    Darreichungsform.TASSE -> "Tasse"
    Darreichungsform.PULVER -> "Pulver"
    Darreichungsform.SONSTIGE -> "Sonstige"
}

/** Zeitstempel im Blattformat: `dd.MM. HH:mm`. */
internal fun blattZeitstempel(zeitpunkt: Long): String =
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date(zeitpunkt))
