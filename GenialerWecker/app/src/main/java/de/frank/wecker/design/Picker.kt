package de.frank.wecker.design

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Uhrzeit- und Datumswahl als Compose-Bausteine statt als Plattform-Dialoge.
 *
 * Die alten `android.app.TimePickerDialog`/`android.app.DatePickerDialog` holen ihr Aussehen aus
 * `res/values/styles.xml` (Theme.Material.Light) — sie erschienen deshalb in jedem Design und auch
 * im Dunkelmodus als weißer Material-1-Systemdialog. Die Bausteine hier ziehen ihre Farben aus dem
 * MaterialTheme-ColorScheme und aus [LocalGold]; sie passen sich damit jedem Design an.
 *
 * Bewusst ohne Animation: es gibt hier keine Dauer- oder Endlosbewegung, deshalb wird
 * `LocalBewegungReduziert` gar nicht erst gebraucht.
 *
 * Alle Maße unten sind gegen Material 3 1.3.1 nachgeschlagen (Compose-BOM 2025.01.01), nicht
 * geschätzt: Tokens aus `TimePickerTokens`/`DatePickerModalTokens`, Abstände aus `TimePicker.kt`
 * und `DatePicker.kt`.
 */

/**
 * Spiegelt `HOEHENANTEIL` aus Dialoge.kt: Der [DesignDialog] deckelt sich über
 * `heightIn(max = Fensterhöhe × 0,82)`. Der Wert steht dort `private`, deshalb hier noch einmal —
 * wird er in Dialoge.kt geändert, muss er hier mitgezogen werden.
 */
private const val DIALOG_HOEHENANTEIL = 0.82f

/** Spiegelt `MAX_BREITE` aus Dialoge.kt — dieselbe Auflage wie beim Höhenanteil. */
private val DIALOG_MAX_BREITE = 560.dp

/** Der waagerechte Außenabstand des Dialograhmens aus Dialoge.kt: 24 dp je Seite. */
private val DIALOG_RAND = 24.dp

/**
 * Höhenbedarf des **stehenden** Zifferblatts. Nachgerechnet:
 * Zeitanzeige 80 dp (`TimePickerTokens.TimeSelectorContainerHeight`)
 * + 36 dp Abstand (`ClockDisplayBottomMargin`)
 * + 256 dp Zifferblatt (`TimePickerTokens.ClockDialContainerSize`)
 * + 24 dp Fußabstand (`ClockFaceBottomMargin`)
 * = 396 dp, dazu die 8 dp Luft oben und unten aus dieser Datei → **412 dp**.
 */
private val UHR_HOCH_BEDARF = 412.dp

/**
 * Im Querformat wählt Material 3 von sich aus die **liegende** Fassung: `defaultTimePickerLayoutType`
 * vergleicht schlicht `screenHeightDp < screenWidthDp`. Dann stehen Zeitanzeige und Zifferblatt
 * nebeneinander, die Höhe ist nur noch 256 dp + 24 dp Fußabstand = 280 dp, mit den 8 dp Luft oben
 * und unten → **296 dp**.
 */
private val UHR_QUER_BEDARF = 296.dp

/**
 * Dafür braucht die liegende Fassung Breite:
 * 96 + 24 + 96 dp Zeitanzeige (`TimeSelectorContainerWidth`, `DisplaySeparatorWidth`)
 * + 36 dp Abstand (`ClockDisplayBottomMargin`, im Querformat waagerecht)
 * + 256 dp Zifferblatt = **508 dp**.
 * Ist weniger Platz da, würde das Zifferblatt seitlich abgeschnitten — dann lieber die Tippeingabe.
 */
private val UHR_QUER_BREITE = 508.dp

/**
 * Höhenbedarf des Monatskalenders. Nachgerechnet:
 * Kopfbereich mindestens 120 dp (`DatePickerModalTokens.HeaderContainerHeight`; der Wert wird
 * unabhängig davon gesetzt, ob ein `title` übergeben wurde)
 * + Monatsnavigation 56 dp (`MonthYearHeight`, als `requiredHeight`)
 * + Wochentagszeile 48 dp (`RecommendedSizeForAccessibility`)
 * + sechs Wochenzeilen à 48 dp = 288 dp (`RecommendedSizeForAccessibility * MaxCalendarRows`,
 *   ebenfalls als `requiredHeight`)
 * = **512 dp**.
 *
 * Gegenprobe: Material gibt dem ganzen Datumsdialog 568 dp (`ContainerHeight`) — die Differenz von
 * 56 dp ist genau dessen eigene Knopfzeile. Die Rechnung geht also auf.
 *
 * `requiredHeight` heißt dabei: Der Kalender **schrumpft nicht**, wenn der Platz knapp wird, er malt
 * über den Rand hinaus. Es gibt kein Zurechtstutzen, deshalb muss die Schwelle stimmen.
 */
private val KALENDER_BEDARF = 512.dp

/**
 * Der Platz, der dem Dialog**inhalt** tatsächlich bleibt — nicht die rohe Bildschirmhöhe.
 *
 * `LocalConfiguration.screenHeightDp` war die falsche Größe: Der Rahmen in Dialoge.kt nimmt davon
 * nur 82 %, und von diesen 82 % gehen Titelzeile, Innenabstände und Knopfzeile ab. Bei 500 dp
 * Bildschirmhöhe blieben so statt 500 dp nur rund 270 dp übrig — das Zifferblatt (412 dp) passte
 * nie, wurde bisher aber trotzdem gewählt und abgeschnitten.
 *
 * Rechnung (alle Zahlen aus Dialoge.kt und Knopf3D.kt):
 * ```
 *   Fensterhöhe × 0,82                    Obergrenze des Rahmens (heightIn)
 * − Titelzeile                            Innenabstand + Zeilenhöhe des Titelstils
 * − 24 dp                                 Innenabstand der Inhaltsspalte (12 oben + 12 unten)
 * − Knopfzeile                            4 dp oben + Innenabstand unten + Knopfhöhe
 * ```
 * Die Knopfhöhe ist die des `GoldKnopf`: 2 × 13 dp Innenabstand um eine labelLarge-Zeile (20 sp).
 *
 * Schriftskalierung: Jeder sp-Wert läuft durch `Density.toDp()`. Das zieht die Systemschriftgröße
 * mit — und zwar richtig, auch für die nichtlineare Skalierung ab Android 14, die ein einfaches
 * `× fontScale` verfehlen würde. Die App-eigene Schriftskalierung aus `GenialeIdeenTheme` verändert
 * nur `fontSize`, nicht `lineHeight`; die Zeilenhöhen unten bleiben deshalb gültig.
 *
 * Beispiel Schlicht bei einfacher Systemschrift: Titel 20 + 24 = 44 dp, Inhaltsspalte 24 dp,
 * Knopfzeile 4 + 20 + 46 = 70 dp → 138 dp Rahmenverbrauch. Für den Kalender (512 dp) braucht es
 * damit (512 + 138) / 0,82 ≈ **793 dp** Fensterhöhe, für das stehende Zifferblatt (412 dp)
 * (412 + 138) / 0,82 ≈ **671 dp**.
 */
@Composable
private fun dialogInhaltsHoehe(): Dp {
    val dichte = LocalDensity.current
    val design = LocalDesignTokens.current.design
    val orbit = design == Design.ORBIT
    // Denselben Innenabstand vergibt `DialogKoerper` in Dialoge.kt.
    val innen = if (orbit) 14.dp else 20.dp

    // Zeilenhöhe des Titelstils, den `DialogTitel` für dieses Design wählt:
    // Orbit labelLarge (20 sp), Traumraum titleLarge (28 sp), Morgenruhe/Schlicht titleMedium (24 sp).
    val titelZeile = with(dichte) {
        when (design) {
            Design.ORBIT -> 20.sp.toDp()
            Design.TRAUMRAUM -> 28.sp.toDp()
            else -> 24.sp.toDp()
        }
    }
    // Orbit setzt 12 dp oben, 12 dp unten und darunter eine 1 dp hohe Trennlinie; die anderen
    // Designs nur `innen` oben und nichts unten.
    val titel = titelZeile + if (orbit) 25.dp else innen

    val knopf = 26.dp + with(dichte) { 20.sp.toDp() }
    // Ab etwa anderthalbfacher Systemschrift bricht die `FlowRow` in Dialoge.kt um: Auf einem
    // 360-dp-Telefon bleiben der Knopfzeile rund 272 dp (360 − 2 × 24 Rand − 2 × 20 innen),
    // „Abbrechen" und „Übernehmen" brauchen bei 1,5-facher Schrift zusammen aber schon rund
    // 350 dp. Dann zählt die Knopfhöhe doppelt, dazu 8 dp Zeilenabstand.
    val knopfZeilen = if (dichte.fontScale >= 1.5f) 2 else 1
    val knoepfe = 4.dp + innen + knopf * knopfZeilen + if (knopfZeilen > 1) 8.dp else 0.dp

    val rahmen = LocalConfiguration.current.screenHeightDp.dp * DIALOG_HOEHENANTEIL
    return (rahmen - titel - 24.dp - knoepfe).coerceAtLeast(0.dp)
}

/**
 * Die Breite, die dem Dialoginhalt bleibt: Der Rahmen zieht 24 dp Außenabstand je Seite ab und ist
 * auf 560 dp gedeckelt; davon gehen noch die seitlichen Innenabstände der Inhaltsspalte ab.
 * Gebraucht wird das nur für die liegende Fassung der Uhr, die in die Breite baut.
 */
@Composable
private fun dialogInhaltsBreite(): Dp {
    val innen = if (LocalDesignTokens.current.design == Design.ORBIT) 14.dp else 20.dp
    val rahmen = minOf(LocalConfiguration.current.screenWidthDp.dp - DIALOG_RAND * 2, DIALOG_MAX_BREITE)
    return (rahmen - innen * 2).coerceAtLeast(0.dp)
}

/** Uhrzeitwahl in der Farbwelt des Designs. Immer 24-Stunden-Format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeitWahlDialog(stunde: Int, minute: Int, aufAbbruch: () -> Unit, aufWahl: (Int, Int) -> Unit) {
    val gold = LocalGold.current
    // 24-Stunden-Format ist Pflicht: die App zeigt Weckzeiten nirgends mit AM/PM, und der
    // bisherige Plattform-Dialog wurde ebenfalls fest mit `is24HourView = true` geöffnet.
    val zustand = rememberTimePickerState(initialHour = stunde, initialMinute = minute, is24Hour = true)

    val konfig = LocalConfiguration.current
    // Material 3 entscheidet selbst zwischen stehender und liegender Fassung, und zwar genau so:
    // `defaultTimePickerLayoutType` vergleicht `screenHeightDp < screenWidthDp`. Dieselbe Bedingung
    // muss hier stehen — sonst rechnet die Schwelle mit der Fassung, die gar nicht gezeichnet wird.
    val quer = konfig.screenHeightDp < konfig.screenWidthDp
    val raum = dialogInhaltsHoehe()
    val breite = dialogInhaltsBreite()
    val platzFuerZifferblatt =
        if (quer) raum >= UHR_QUER_BEDARF && breite >= UHR_QUER_BREITE else raum >= UHR_HOCH_BEDARF

    val farben = TimePickerDefaults.colors(
        clockDialColor = gold.flaecheErhoeht,
        clockDialSelectedContentColor = gold.aufPrimaer,
        clockDialUnselectedContentColor = gold.textPrimaer,
        selectorColor = gold.primaer,
        // Die AM/PM-Umschaltung erscheint im 24-Stunden-Format nie; die Farben stehen trotzdem
        // in der Palette, damit nichts Fremdes durchschlägt, falls Material sie doch zeichnet.
        periodSelectorBorderColor = gold.rahmen,
        periodSelectorSelectedContainerColor = gold.primaer,
        periodSelectorUnselectedContainerColor = gold.flaecheErhoeht,
        periodSelectorSelectedContentColor = gold.aufPrimaer,
        periodSelectorUnselectedContentColor = gold.textPrimaer,
        timeSelectorSelectedContainerColor = gold.primaer,
        timeSelectorUnselectedContainerColor = gold.flaecheErhoeht,
        timeSelectorSelectedContentColor = gold.aufPrimaer,
        timeSelectorUnselectedContentColor = gold.textPrimaer,
    )

    DesignDialog(
        titel = "Uhrzeit wählen",
        aufSchliessen = aufAbbruch,
        // Nur hier verlässt ein Wert den Dialog. Abbrechen, Wegtippen und Zurück schreiben nichts.
        bestaetigung = { GoldKnopf("Übernehmen", { aufWahl(zustand.hour, zustand.minute) }) },
        abbruch = { StillerKnopf("Abbrechen", aufAbbruch) },
        inhalt = {
            // Sicherheitsnetz, falls die Rechnung oben trotzdem zu knapp liegt:
            // `weight(1f, fill = false)` gibt dem Inhalt nur den Rest der gedeckelten Dialoghöhe —
            // die Knopfzeile wird also nie hinausgeschoben — und darin darf gescrollt werden.
            //
            // Das ist **kein** verschachteltes gleichachsiges Scrollen (Bug-Almanach
            // jetpack-compose §6.1): Weder `TimePicker` noch `TimeInput` bringen einen eigenen
            // senkrechten Scroller mit (das Zifferblatt ist ein eigenes Layout, die Tippeingabe
            // sind zwei Textfelder), und die Höhe, die der Scrollcontainer sieht, ist über
            // `heightIn(max = …)` des Rahmens begrenzt — nie „infinity". Dieselbe Bauweise nutzt
            // `DesignTextDialog` in Dialoge.kt bereits.
            //
            // Bildschirmtastatur: `screenHeightDp` ändert sich durch die IME **nicht**, die
            // Schwelle oben kann sie also gar nicht sehen. Genau dafür ist der Scroll da —
            // schrumpft das Dialogfenster (adjustResize), bleibt alles erreichbar; schiebt das
            // System es statt dessen hoch (adjustPan), bringt es das Feld selbst ins Bild.
            val platz = Modifier
                .align(Alignment.CenterHorizontally)
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
            if (platzFuerZifferblatt) {
                TimePicker(state = zustand, modifier = platz, colors = farben)
            } else {
                TimeInput(state = zustand, modifier = platz, colors = farben)
            }
        },
    )
}

/**
 * Datumswahl in der Farbwelt des Designs. [fruehestes] begrenzt nach unten (null = keine Grenze).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatumWahlDialog(
    datum: LocalDate,
    fruehestes: LocalDate?,
    aufAbbruch: () -> Unit,
    aufWahl: (LocalDate) -> Unit,
) {
    val gold = LocalGold.current
    val heute = remember { LocalDate.now() }
    val platzFuerKalender = dialogInhaltsHoehe() >= KALENDER_BEDARF

    // Der Jahresbereich ist **keine** fachliche Grenze — er sagt nur, was der Kalender überhaupt
    // anbietet. Basis ist deshalb der regulär unterstützte Bereich von Material 3
    // (`DatePickerDefaults.YearRange`, nachgeschlagen: 1900..2100). Erweitert wird er allein, wenn
    // vorhandene Daten außerhalb liegen: Der vorgewählte Tag darf nie ausgeschlossen sein, sonst
    // wirft `DatePickerState` beim Anlegen.
    //
    // Früher startete der Bereich beim kleinsten vorhandenen Jahr. Damit sperrte ein Startdatum in
    // 2026 jedes frühere Jahr aus — ein Intervall-Anker ließ sich nicht mehr auf 2025 legen. Eine
    // Untergrenze setzt ausschließlich [fruehestes] über `SelectableDates`; ist es null, schränkt
    // hier nichts ein.
    val jahre = remember(datum, fruehestes, heute) {
        val regulaer = DatePickerDefaults.YearRange
        val unten = minOf(regulaer.first, datum.year, heute.year, fruehestes?.year ?: Int.MAX_VALUE)
        val oben = maxOf(regulaer.last, datum.year, heute.year, fruehestes?.year ?: Int.MIN_VALUE)
        unten..oben
    }

    // Tage vor [fruehestes] sind nicht wählbar. Material 3 übergibt hier UTC-Millisekunden,
    // deshalb wird auch hier ausschließlich über ZoneOffset.UTC zurückgerechnet.
    val waehlbar = remember(fruehestes) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (fruehestes == null) return true
                return !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(fruehestes)
            }

            override fun isSelectableYear(year: Int): Boolean = fruehestes == null || year >= fruehestes.year
        }
    }

    // Zeitzonenfalle: DatePicker rechnet ausschließlich in UTC-Millisekunden. Mit der
    // Systemzeitzone würde der Tagesbeginn östlich von Greenwich vor Mitternacht UTC liegen –
    // das Datum spränge um einen Tag zurück. Deshalb hier wie dort ZoneOffset.UTC.
    val zustand = rememberDatePickerState(
        initialSelectedDateMillis = datum.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = jahre,
        // Bei wenig Platz startet die Wahl in der Tippeingabe; der Umschalter bleibt,
        // sodass der Kalender bei genug Platz weiterhin einen Tipp entfernt ist.
        initialDisplayMode = if (platzFuerKalender) DisplayMode.Picker else DisplayMode.Input,
        selectableDates = waehlbar,
    )

    // `initialDisplayMode` greift nur beim Anlegen, der Modus selbst wird über die Drehung hinweg
    // gesichert. Ohne diesen Nachzug stünde nach dem Drehen ins Querformat wieder der Kalender da —
    // abgeschnitten. Der Effekt hängt allein am Platz, ein späteres Umschalten von Hand bleibt.
    LaunchedEffect(platzFuerKalender) {
        if (!platzFuerKalender && zustand.displayMode == DisplayMode.Picker) zustand.displayMode = DisplayMode.Input
    }

    // Außerhalb des Inhalts gemerkt, damit der Scrollstand einen Moduswechsel übersteht.
    val scrollZustand = rememberScrollState()

    val farben = DatePickerDefaults.colors(
        // Die Fläche zeichnet der Dialog selbst; ein eigener Container würde als Kasten im Kasten stehen.
        containerColor = Color.Transparent,
        titleContentColor = gold.textGedaempft,
        headlineContentColor = gold.textPrimaer,
        weekdayContentColor = gold.textGedaempft,
        subheadContentColor = gold.textGedaempft,
        navigationContentColor = gold.primaer,
        yearContentColor = gold.textPrimaer,
        disabledYearContentColor = gold.textGedaempft.copy(alpha = .38f),
        currentYearContentColor = gold.primaer,
        selectedYearContentColor = gold.aufPrimaer,
        selectedYearContainerColor = gold.primaer,
        disabledSelectedYearContentColor = gold.aufPrimaer.copy(alpha = .38f),
        disabledSelectedYearContainerColor = gold.primaer.copy(alpha = .38f),
        dayContentColor = gold.textPrimaer,
        disabledDayContentColor = gold.textGedaempft.copy(alpha = .38f),
        selectedDayContentColor = gold.aufPrimaer,
        selectedDayContainerColor = gold.primaer,
        disabledSelectedDayContentColor = gold.aufPrimaer.copy(alpha = .38f),
        disabledSelectedDayContainerColor = gold.primaer.copy(alpha = .38f),
        todayContentColor = gold.primaer,
        todayDateBorderColor = gold.primaer,
        dividerColor = gold.rahmen,
    )

    val gewaehlt = zustand.selectedDateMillis

    DesignDialog(
        titel = "Datum wählen",
        aufSchliessen = aufAbbruch,
        bestaetigung = {
            GoldKnopf(
                "Übernehmen",
                {
                    // Nur hier verlässt ein Wert den Dialog — und nur ein tatsächlich gewählter.
                    gewaehlt?.let { aufWahl(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                // In der Tippeingabe bleibt die Auswahl leer, solange die Eingabe unvollständig
                // oder vor dem frühesten Tag liegt. Dann darf „Übernehmen“ nichts speichern.
                aktiviert = gewaehlt != null,
            )
        },
        abbruch = { StillerKnopf("Abbrechen", aufAbbruch) },
        inhalt = {
            // `weight(1f, fill = false)` hält die Knopfzeile auch dann im Dialog, wenn der Kalender
            // mehr Platz haben möchte, als übrig ist.
            //
            // Gescrollt werden darf hier **nur** die Tippeingabe. Der Kalender bringt mit seiner
            // Jahresauswahl ein eigenes senkrecht scrollendes `LazyVerticalGrid` mit; läge ein
            // `verticalScroll` darüber, stünde gleichachsiges Scrollen ineinander und die Messung
            // liefe in „infinity constraints" (Bug-Almanach jetpack-compose §6.1) — und zwar erst
            // beim Antippen der Jahreszahl, also spät und schwer zu finden. Im Kalendermodus ist
            // deshalb allein die Schwelle oben der Schutz, und sie ist gegen die echten
            // Material-Maße gerechnet.
            //
            // In der Tippeingabe dagegen gibt es keinen inneren Scroller, und genau dort geht die
            // Bildschirmtastatur auf. Der Scroll fängt ab, was die Schwelle nicht sehen kann:
            // `screenHeightDp` ändert sich durch die IME nicht. Schrumpft das Dialogfenster
            // (adjustResize), bleibt das Feld erreichbar; schiebt das System es hoch (adjustPan),
            // erledigt es das selbst.
            val eingabe = zustand.displayMode == DisplayMode.Input
            DatePicker(
                state = zustand,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .then(if (eingabe) Modifier.verticalScroll(scrollZustand) else Modifier),
                // Der Dialog trägt bereits seine Überschrift; eine zweite wäre doppelt.
                title = null,
                showModeToggle = true,
                colors = farben,
            )
        },
    )
}
