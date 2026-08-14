package de.frank.stacklabor.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.codex.CodexModell
import de.frank.stacklabor.codex.Denkstufe
import de.frank.stacklabor.daten.entitaeten.BewertungMitInhalt
import de.frank.stacklabor.daten.entitaeten.EigeneFrageE
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.FortschrittsErzaehler
import de.frank.stacklabor.ui.bausteine.PegelBalken
import de.frank.stacklabor.ui.bausteine.SchimmerFlaeche
import de.frank.stacklabor.ui.bausteine.StreamenderText
import de.frank.stacklabor.ui.bausteine.SymbolKnopf
import de.frank.stacklabor.ui.bausteine.VerlaufsKante
import de.frank.stacklabor.ui.bausteine.entsaettigtPulsierend
import de.frank.stacklabor.ui.bausteine.glasflaeche
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import de.frank.stacklabor.vorlesen.Anbieter
import de.frank.stacklabor.vorlesen.StimmenKatalog
import de.frank.stacklabor.vorlesen.VorleseEinstellungen
import de.frank.stacklabor.vorlesen.VorleseSteuerung
import de.frank.stacklabor.vorlesen.VorleseZustand
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/*
 * PLAN — B-07 „Auswertung im Vollbild“ (F-16, M-11 … M-13, M-15).
 *
 *  1. Aufbau von oben nach unten, jedes Maß aus `02-UI-SPEC.md` §6/B-07: Kopfleiste 57 dp,
 *     Metazeile 32 dp, Fließtext 15 sp auf 22 dp Zeilenhöhe mit 16 dp Rand, fester Sockel 60 dp.
 *  2. Vorgelesen wird ausschließlich der Fließtext, absatzweise. Die Absätze werden hier nach
 *     derselben Regel zerlegt wie in `VorleseSteuerung.zerlege` (Leerzeile trennt, Leerraum
 *     wird eingeebnet) — nur dann zeigt `aktuellerAbsatz` auf denselben Absatz, der hier steht.
 *  3. Die Sprech-Markierung ist EINE Fläche hinter der Absatzspalte. Jeder Absatz meldet seine
 *     Lage; die Fläche wandert in 200 ms dorthin, statt an jedem Absatz ein- und auszublenden —
 *     genau das meint „die Kante gleitet mit“ (M-15).
 *  4. Das Vorlesen hängt nicht am Bildschirm, sondern an einem prozessweiten Halter. Verlässt
 *     Frank B-07, spricht der Vordergrunddienst weiter; kehrt er zurück, steht der Knopf richtig.
 *  5. Zustände: lädt · fertig · Vorlesen läuft · Vorlese-Fehler · offline · noch nicht
 *     ausgewertet. Ohne Netz bleibt der gespeicherte Text vollständig lesbar.
 *
 * Die Bezeichner folgen der Schreibweise des übrigen Quelltextes (ae/oe/ue); Kommentare und
 * angezeigte Texte tragen echte Umlaute.
 */

/** Höhe der Metazeile und des festen Sockels — gemessen (B-07). */
private val METAZEILE_HOEHE = 32.dp
private val SOCKEL_HOEHE = 60.dp

/** Die Kante der Sprech-Markierung gleitet in 200 ms mit (M-15). */
private const val MARKIERUNG_MS = 200

/** Trennt Absätze genau wie `VorleseSteuerung`, damit die Hervorhebung nicht verrutscht. */
private val ABSATZ_TRENNER = Regex("\\r?\\n\\s*\\r?\\n")
private val LEERRAUM_IM_ABSATZ = Regex("\\s+")

private val ZEITFORM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMANY)

/** Was der Bildschirm gerade weiß — „noch nichts geladen“ ist etwas anderes als „nichts da“. */
private sealed interface Ladestand {
    data object Laedt : Ladestand
    data class Fertig(val bewertung: BewertungMitInhalt?) : Ladestand
}

/**
 * **B-07 — Auswertung im Vollbild.** Der volle Text der KI, vorlesbar über den festen Sockel
 * (F-16). Wird von B-02, B-09 und B-15 aus aufgerufen.
 */
@Composable
fun AuswertungsBildschirm(app: StackLaborApp, steuerung: NavHostController, stackId: String) {
    val quelle = remember(app, stackId) {
        app.datenbank.bewertungDao().juengsteBewertungMitInhalt(stackId)
            .map<BewertungMitInhalt?, Ladestand> { Ladestand.Fertig(it) }
    }
    val stand by quelle.collectAsStateWithLifecycle(initialValue = Ladestand.Laedt)
    val fragen by app.datenbank.frageDao().fragenDesStacks(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val vorlesen = remember(app) { VorleseHalter.hole(app) }
    val vorleseZustand by vorlesen.zustand.collectAsStateWithLifecycle()
    val gesprochenerAbsatz by vorlesen.aktuellerAbsatz.collectAsStateWithLifecycle()
    val pegel by vorlesen.pegel.collectAsStateWithLifecycle()
    val netzDa = netzVorhanden()

    // Das Vorlesen wird beim Verlassen bewusst NICHT beendet (F-16): Es läuft über den
    // Vordergrunddienst weiter, mit Stopp-Knopf in der Benachrichtigung.

    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste("Auswertung", steuerung)

        when (val jetzt = stand) {
            is Ladestand.Laedt -> WarteAnsicht(Modifier.weight(1f))

            is Ladestand.Fertig -> {
                val bewertung = jetzt.bewertung
                if (bewertung == null) {
                    NochNichtAusgewertet(Modifier.weight(1f))
                } else {
                    Metazeile(bewertung)
                    Auswertungstext(
                        bewertung = bewertung,
                        fragen = fragen,
                        gesprochenerAbsatz = gesprochenerAbsatz,
                        untenInset = untenInset,
                        modifier = Modifier.weight(1f),
                    )
                    VorleseSockel(
                        zustand = vorleseZustand,
                        pegel = pegel,
                        netzDa = netzDa,
                        untenInset = untenInset,
                        aufLesen = { vorlesen.lies(bewertung.bewertung.gesamt) },
                        aufPause = { vorlesen.pause() },
                        aufWeiter = { vorlesen.weiter() },
                        aufStopp = { vorlesen.stopp() },
                    )
                }
            }
        }
    }
}

// ── Metazeile ───────────────────────────────────────────────────────────────────

/** „ausgewertet 14.08. 07:44 · Terra · hoch“ — 32 dp, 12 sp, `--txt2`. */
@Composable
private fun Metazeile(bewertung: BewertungMitInhalt) {
    val zeitpunkt = remember(bewertung.bewertung.zeitpunkt) {
        Instant.ofEpochMilli(bewertung.bewertung.zeitpunkt)
            .atZone(ZoneId.systemDefault())
            .format(ZEITFORM)
    }
    val modell = CodexModell.ausWert(bewertung.bewertung.modell)
        .bezeichnung.substringAfterLast(' ')
    val denkstufe = Denkstufe.ausWert(bewertung.bewertung.denkstufe).bezeichnung.lowercase()

    Box(
        Modifier
            .fillMaxWidth()
            .height(METAZEILE_HOEHE)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            "ausgewertet $zeitpunkt · $modell · $denkstufe",
            style = Schrift.klein,
            color = Farben.textSchwach,
            maxLines = 1,
        )
    }
}

// ── Fließtext, Hinweise, Antworten ──────────────────────────────────────────────

/**
 * Der scrollende Teil: Fließtext mit Sprech-Markierung, darunter die Hinweise und die
 * Antworten auf die eigenen Fragen.
 */
@Composable
private fun Auswertungstext(
    bewertung: BewertungMitInhalt,
    fragen: List<EigeneFrageE>,
    gesprochenerAbsatz: Int,
    untenInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val fliesstext = bewertung.bewertung.gesamt
    val absaetze = remember(fliesstext) { zerlegeInAbsaetze(fliesstext) }
    val hinweise = remember(bewertung.bewertung.hinweise) {
        bewertung.bewertung.hinweise.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Frisch ist ein Text, der ANKOMMT, während der Bildschirm offen ist — nur er streamt
    // wortweise herein (M-13). Ein gespeicherter Text steht sofort vollständig da.
    var zuletztGezeigt by remember { mutableStateOf<String?>(null) }
    var frisch by remember { mutableStateOf(false) }
    LaunchedEffect(fliesstext) {
        frisch = zuletztGezeigt != null && zuletztGezeigt != fliesstext
        zuletztGezeigt = fliesstext
    }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(
            start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp + untenInset,
        ),
    ) {
        item(key = "fliesstext") {
            FliesstextMitMarkierung(absaetze, gesprochenerAbsatz, frisch)
        }

        if (hinweise.isNotEmpty()) {
            item(key = "hinweise-titel") { Abschnitt("Hinweise") }
            items(hinweise.size, key = { "hinweis-$it" }) { stelle ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("•", style = Schrift.fliesstext, color = Farben.gelbText)
                    Spacer(Modifier.width(8.dp))
                    Text(hinweise[stelle], style = Schrift.fliesstext, color = Farben.text)
                }
            }
        }

        if (bewertung.antworten.isNotEmpty()) {
            item(key = "antworten-titel") { Abschnitt("Antworten auf deine Fragen") }
            items(bewertung.antworten.size, key = { "antwort-$it" }) { stelle ->
                val antwort = bewertung.antworten[stelle]
                val frage = fragen.firstOrNull { it.id == antwort.frageId }?.text
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        frage ?: "Frage nicht mehr vorhanden",
                        style = Schrift.grundMittel,
                        color = Farben.text,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(antwort.text, style = Schrift.fliesstext, color = Farben.textSchwach)
                }
            }
        }
    }
}

/**
 * Die Absätze des Fließtextes samt Sprech-Markierung (M-15).
 *
 * DECISION: Die Markierung ist eine einzige Fläche hinter der Spalte. Jeder Absatz meldet über
 * `onGloballyPositioned` seine Lage in der Spalte; Oberkante und Höhe der Fläche wandern in
 * 200 ms auf die Werte des gesprochenen Absatzes. Eine Fläche je Absatz würde stattdessen
 * blinken — die Kante gliche nicht mit.
 */
@Composable
private fun FliesstextMitMarkierung(
    absaetze: List<String>,
    gesprochenerAbsatz: Int,
    frisch: Boolean,
) {
    val dichte = LocalDensity.current
    val lagen = remember(absaetze) { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val lage = lagen[gesprochenerAbsatz]

    val bewegung = tween<androidx.compose.ui.unit.Dp>(MARKIERUNG_MS, easing = Kurven.leit)
    val oben by animateDpAsState(
        targetValue = with(dichte) { (lage?.first ?: 0f).toDp() },
        animationSpec = bewegung,
        label = "markierungOben",
    )
    val hoehe by animateDpAsState(
        targetValue = with(dichte) { (lage?.second ?: 0f).toDp() },
        animationSpec = bewegung,
        label = "markierungHoehe",
    )
    val deckkraft by animateFloatAsState(
        targetValue = if (lage != null) 1f else 0f,
        animationSpec = tween(MARKIERUNG_MS, easing = Kurven.zustand),
        label = "markierungDeckkraft",
    )

    Box(Modifier.fillMaxWidth()) {
        if (deckkraft > 0f) {
            Box(
                Modifier
                    .offset(y = oben)
                    .fillMaxWidth()
                    .height(hoehe)
                    .clip(Formen.feld)
                    .background(Farben.akzent.copy(alpha = 0.12f * deckkraft)),
            )
        }
        Column(Modifier.fillMaxWidth()) {
            absaetze.forEachIndexed { nummer, absatz ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { masse ->
                            // Nur bei echter Änderung schreiben — sonst löst jede Messung eine
                            // neue Zusammensetzung aus und die läuft im Kreis.
                            val neu = masse.positionInParent().y to masse.size.height.toFloat()
                            if (lagen[nummer] != neu) lagen[nummer] = neu
                        }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                ) {
                    if (frisch) {
                        StreamenderText(absatz, Schrift.fliesstext)
                    } else {
                        Text(absatz, style = Schrift.fliesstext, color = Farben.text)
                    }
                }
            }
        }
    }
}

// ── Sockel ──────────────────────────────────────────────────────────────────────

/**
 * Der feste Sockel: 60 dp, Glasfläche, Vorlesen / Pause / Stopp und die drei Pegelbalken
 * (F-16, M-15). Ohne Netz ist der Vorlese-Knopf ausgegraut; darüber steht der Grund.
 */
@Composable
private fun VorleseSockel(
    zustand: VorleseZustand,
    pegel: Float,
    netzDa: Boolean,
    untenInset: androidx.compose.ui.unit.Dp,
    aufLesen: () -> Unit,
    aufPause: () -> Unit,
    aufWeiter: () -> Unit,
    aufStopp: () -> Unit,
) {
    val spricht = zustand == VorleseZustand.SPRICHT
    val pausiert = zustand == VorleseZustand.PAUSIERT
    val fehler = (zustand as? VorleseZustand.FEHLER)?.meldung

    Column {
        VerlaufsKante()

        // Der Sockel bleibt exakt 60 dp hoch — Hinweise stehen deshalb darüber, nie darin.
        if (!netzDa) {
            SockelHinweis(
                "Ohne Internet kann nicht vorgelesen werden. Der gespeicherte Text bleibt lesbar.",
                Farben.textSchwach,
            )
        } else if (fehler != null) {
            SockelHinweis(fehler, Farben.rot)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .glasflaeche()
                .height(SOCKEL_HOEHE)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Chip(
                beschriftung = if (pausiert) "Weiter" else "Vorlesen",
                aktiv = netzDa && !spricht,
            ) {
                if (!netzDa || spricht) return@Chip
                if (pausiert) aufWeiter() else aufLesen()
            }

            SymbolKnopf(
                symbol = Icons.Rounded.Pause,
                beschreibung = "Vorlesen anhalten",
                farbe = if (spricht) Farben.text else Farben.deaktiviert,
            ) { if (spricht) aufPause() }

            SymbolKnopf(
                symbol = Icons.Rounded.Stop,
                beschreibung = "Vorlesen beenden",
                farbe = if (spricht || pausiert) Farben.text else Farben.deaktiviert,
            ) { if (spricht || pausiert) aufStopp() }

            Spacer(Modifier.weight(1f))

            // Anzeige, kein Knopf: die drei Balken und das Lautsprecher-Zeichen sagen nur,
            // ob gerade gesprochen wird.
            Row(
                Modifier.semantics {
                    contentDescription = if (spricht) "Es wird vorgelesen" else "Vorlesen ruht"
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PegelBalken(if (spricht) pegel else 0f)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (spricht) {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    contentDescription = null,
                    tint = if (spricht) Farben.akzent else Farben.textSchwach,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(untenInset))
    }
}

@Composable
private fun SockelHinweis(text: String, farbe: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = Schrift.klein,
        color = farbe,
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.erhoeht)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

// ── Warte- und Leerzustand ──────────────────────────────────────────────────────

/**
 * Solange die Auswertung noch nicht da ist: Schimmer-Platzhalter (M-11), entsättigt pulsierende
 * Ampeln (M-12) und die Fortschrittserzählung (M-13).
 */
@Composable
private fun WarteAnsicht(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier.entsaettigtPulsierend(true),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WartendeAmpel(Farben.gruen)
            WartendeAmpel(Farben.gelb)
            WartendeAmpel(Farben.rot)
        }
        Spacer(Modifier.height(12.dp))

        FortschrittsErzaehler(laeuft = true)
        Spacer(Modifier.height(16.dp))

        // Sieben Zeilen im Takt des Fließtextes, die letzte kürzer — wie ein Absatzende.
        val breiten = listOf(1f, 1f, 0.94f, 1f, 0.88f, 1f, 0.55f)
        breiten.forEach { anteil ->
            SchimmerFlaeche(
                Modifier
                    .fillMaxWidth(anteil)
                    .height(12.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WartendeAmpel(farbe: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(Masse.loeslichPunkt).clip(CircleShape).background(farbe))
}

/** „Noch nicht ausgewertet“ — mit dem Weg zurück nach B-02, wo der Knopf dafür steht. */
@Composable
private fun NochNichtAusgewertet(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Dieser Stack wurde noch nicht ausgewertet.",
            style = Schrift.name,
            color = Farben.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Auf dem Stack-Bildschirm steht dafür der Knopf „Diesen Stack auswerten“.",
            style = Schrift.grund,
            color = Farben.textSchwach,
        )
    }
}

/** Abschnitts-Überschrift zwischen Fließtext, Hinweisen und Antworten. */
@Composable
private fun Abschnitt(text: String) {
    Text(
        text.uppercase(),
        style = Schrift.rubrik,
        color = Farben.textSchwach,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

// ── Hilfen ──────────────────────────────────────────────────────────────────────

/**
 * Zerlegt den Fließtext in Absätze — Zeichen für Zeichen dieselbe Regel wie
 * `VorleseSteuerung.zerlege`: Leerzeile trennt, Leerraum innerhalb eines Absatzes wird
 * eingeebnet. Nur so zeigt `aktuellerAbsatz` auf denselben Absatz, der hier steht.
 */
private fun zerlegeInAbsaetze(text: String): List<String> =
    text.split(ABSATZ_TRENNER)
        .map { absatz -> absatz.trim().replace(LEERRAUM_IM_ABSATZ, " ") }
        .filter { absatz -> absatz.isNotBlank() }

/**
 * Meldet, ob gerade ein Netz da ist — alle drei Stimmen kommen aus dem Netz (F-16).
 * Der Rückruf hält die Anzeige aktuell, ohne dass Frank den Bildschirm neu öffnen muss.
 */
@Composable
private fun netzVorhanden(): Boolean {
    val zusammenhang = LocalContext.current
    var vorhanden by remember { mutableStateOf(pruefeNetz(zusammenhang)) }

    DisposableEffect(zusammenhang) {
        val verwaltung = zusammenhang
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (verwaltung == null) {
            onDispose { }
        } else {
            val rueckruf = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(netz: Network) {
                    vorhanden = true
                }

                override fun onLost(netz: Network) {
                    vorhanden = pruefeNetz(zusammenhang)
                }
            }
            verwaltung.registerDefaultNetworkCallback(rueckruf)
            onDispose { verwaltung.unregisterNetworkCallback(rueckruf) }
        }
    }
    return vorhanden
}

private fun pruefeNetz(zusammenhang: Context): Boolean {
    val verwaltung = zusammenhang
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val netz = verwaltung.activeNetwork ?: return false
    val faehigkeiten = verwaltung.getNetworkCapabilities(netz) ?: return false
    return faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Hält die eine Vorlese-Steuerung des Prozesses.
 *
 * DECISION: Sie hängt bewusst nicht am Bildschirm. Verlässt Frank B-07, läuft die Wiedergabe
 * über den Vordergrunddienst weiter (F-16); käme die Steuerung mit dem Bildschirm, bräche der
 * Ton beim Zurückgehen ab und der Knopf stünde beim Wiederkommen falsch.
 */
private object VorleseHalter {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var steuerung: VorleseSteuerung? = null

    fun hole(app: StackLaborApp): VorleseSteuerung = steuerung ?: synchronized(this) {
        steuerung ?: VorleseSteuerung(
            context = app,
            einstellungen = einstellungenFluss(app),
        ).also { steuerung = it }
    }

    /** Anbieter, Stimme, Tempo und Absatzpause kommen laufend aus B-10 (F-18). */
    private fun einstellungenFluss(app: StackLaborApp): StateFlow<VorleseEinstellungen> = combine(
        app.einstellungen.ttsAnbieter,
        app.einstellungen.ttsStimme,
        app.einstellungen.ttsTempo,
        app.einstellungen.ttsPauseMs,
    ) { anbieterWert, stimme, tempo, pause ->
        val anbieter = Anbieter.entries.firstOrNull {
            it.name == anbieterWert || it.kennung == anbieterWert
        } ?: StimmenKatalog.VORGABE_ANBIETER
        VorleseEinstellungen(
            anbieter = anbieter,
            stimmeId = StimmenKatalog.gueltigeStimme(anbieter, stimme),
            tempo = tempo,
            absatzPauseMs = pause.toLong(),
        )
    }.stateIn(bereich, SharingStarted.Eagerly, VorleseEinstellungen())
}
