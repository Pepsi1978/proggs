package de.frank.wecker.design

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
 * Der Höhenanteil, den die Picker beim [DesignDialog] anfordern.
 *
 * Die Vorgabe des Rahmens sind 0,82 der Fensterhöhe. Das ist für Text- und Listendialoge richtig —
 * für die Picker war es ein Deckel: Bei 751 dp Fensterhöhe (Samsung Fold, Außenschirm) blieben nach
 * Abzug von Titel, Knopfzeile und Innenabständen nur rund 478 dp Inhalt, und der Monatskalender
 * braucht 512 dp am Stück ([KALENDER_BEDARF], `requiredHeight`, schrumpft nicht). Die Datumswahl
 * startete deshalb auf einem ganz gewöhnlichen Telefon in der Tippeingabe.
 *
 * Uhr und Kalender können nicht schrumpfen, deshalb fordern sie mit 1,0 alles an. Gedeckelt bleiben
 * sie trotzdem: `dialogHoechstHoehe` in Dialoge.kt zieht Systemleisten und etwas Luft ab, und
 * darüber hinaus wächst der Rahmen nicht — im Querformat greift diese harte Grenze, nicht der
 * Anteil. Wo es dann immer noch zu eng ist, bleibt die kompakte Fassung der Ausweg.
 */
private const val PICKER_HOEHENANTEIL = 1f

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
 * Breitenbedarf des **stehenden** Zifferblatts: 256 dp (`TimePickerTokens.ClockDialContainerSize`).
 * Die Zeitanzeige darüber ist mit 96 + 24 + 96 = 216 dp schmaler, das Zifferblatt gibt also das Maß.
 * Auf den gängigen Telefonbreiten wird diese Schwelle nie scharf (320 dp Schirm lassen 272 dp Inhalt
 * übrig); sie fängt geteilte Fenster und sehr schmale Schirme ab.
 */
private val UHR_HOCH_BREITE = 256.dp

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
 * **Breiten**bedarf des Monatskalenders — bis hierher wurde nur die Höhe geprüft, und genau das war
 * die Lücke. Nachgerechnet gegen Material 3 1.3.1 (aus dem AAR im Gradle-Cache ausgelesen, nicht
 * geschätzt):
 * ```
 *   7 Spalten × 48 dp   `RecommendedSizeForAccessibility`, als `requiredSize` je Tagesfeld = 336 dp
 * + 2 × 12 dp           `DatePickerHorizontalPadding`, der eigene Rand der Wochen- und Tageszeilen
 * = 360 dp
 * ```
 * Gegenprobe: `DatePickerModalTokens.ContainerWidth` ist ebenfalls 360 dp, und `DateEntryContainer`
 * setzt genau diesen Wert als `sizeIn(minWidth = …)`. Die Rechnung geht also auf.
 *
 * Was bei weniger Breite passiert: `sizeIn` beugt sich den eingehenden Grenzen, der Kalender wird
 * also schmaler gemessen — die Tagesfelder darin aber **nicht**, `requiredSize` ignoriert jede
 * Vorgabe. Die Tageszeile ordnet mit `Arrangement.SpaceEvenly` an; bleibt zu wenig Platz, wird der
 * Zwischenraum negativ und die Tagesfelder schieben sich übereinander und über den Rand hinaus.
 * Das sieht auf den ersten Blick noch nach Kalender aus, die Tippflächen überlappen sich aber.
 */
private val KALENDER_BREITE = 360.dp

/**
 * Der Platz, der dem Dialog**inhalt** tatsächlich bleibt — nicht die rohe Bildschirmhöhe.
 *
 * `LocalConfiguration.screenHeightDp` war die falsche Größe: Der Rahmen in Dialoge.kt deckelt sich,
 * und von dem, was übrig bleibt, gehen Titelzeile, Innenabstände und Knopfzeile ab. Bei 500 dp
 * Bildschirmhöhe blieben so statt 500 dp nur rund 300 dp übrig — das Zifferblatt (412 dp) passte
 * nie, wurde vor dieser Rechnung aber trotzdem gewählt und abgeschnitten.
 *
 * Rechnung (alle Zahlen aus Dialoge.kt und Knopf3D.kt):
 * ```
 *   dialogHoechstHoehe(1,0)               Obergrenze des Rahmens (heightIn), siehe Dialoge.kt:
 *                                         Fensterhöhe − Systemleisten − 2 × 8 dp Luft
 * − Titelzeile                            Innenabstand + Zeilenhöhe des Titelstils
 * − 24 dp                                 Innenabstand der Inhaltsspalte (12 oben + 12 unten)
 * − Knopfzeile                            4 dp oben + Innenabstand unten + Knopfhöhe
 * ```
 * Die Knopfhöhe ist die des `GoldKnopf`: 2 × 13 dp Innenabstand um eine labelLarge-Zeile (20 sp).
 * Gerechnet wird mit demselben [PICKER_HOEHENANTEIL], den die Dialoge unten an den Rahmen geben —
 * beide Seiten laufen durch dieselbe Funktion, die Zahl kann nicht mehr auseinanderlaufen.
 *
 * Schriftskalierung: Jeder sp-Wert läuft durch `Density.toDp()`. Das zieht die Systemschriftgröße
 * mit — und zwar richtig, auch für die nichtlineare Skalierung ab Android 14, die ein einfaches
 * `× fontScale` verfehlen würde. Die App-eigene Schriftskalierung aus `GenialeIdeenTheme` verändert
 * nur `fontSize`, nicht `lineHeight`; die Zeilenhöhen unten bleiben deshalb gültig.
 *
 * Beispiel Schlicht bei einfacher Systemschrift: Titel 20 + 24 = 44 dp, Inhaltsspalte 24 dp,
 * Knopfzeile 4 + 20 + 46 = 70 dp → 138 dp Rahmenverbrauch. Für den Kalender (512 dp) braucht es
 * damit 512 + 138 = **650 dp** Rahmenhöhe, für das stehende Zifferblatt (412 dp) 550 dp.
 *
 * Auf das Gerät gerechnet, auf dem die Datumswahl in der Tippeingabe landete (751 dp Fensterhöhe):
 * ```
 *   Gestensteuerung, Leisten 24 + 24 dp:  751 − 48 − 16 = 687 → 549 dp Inhalt → Kalender ✓
 *   Drei Knöpfe,     Leisten 24 + 48 dp:  751 − 72 − 16 = 663 → 525 dp Inhalt → Kalender ✓
 * ```
 * Der Kalender gewinnt, solange `Systemleisten ≤ 85 dp` sind (751 − Leisten − 16 − 138 ≥ 512).
 * Darüber — sehr hohe Leisten, oder schlicht ein niedrigeres Fenster — fällt die Wahl wie bisher
 * auf die Tippeingabe zurück: eng, aber vollständig bedienbar. Zum Vergleich die alte Deckelung:
 * 751 × 0,82 = 616 − 138 = 478 dp, also 34 dp zu wenig für den Kalender.
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
    // 360-dp-Telefon bleiben der Knopfzeile rund 272 dp (360 − 2 × 24 Rand − 2 × 20 innen; gibt der
    // Rahmen für den Kalender Rand ab, werden es mehr — die Annahme liegt dann auf der sicheren
    // Seite, weil sie zu viel Höhe abzieht, nie zu wenig),
    // „Abbrechen" und „Übernehmen" brauchen bei 1,5-facher Schrift zusammen aber schon rund
    // 350 dp. Dann zählt die Knopfhöhe doppelt, dazu 8 dp Zeilenabstand.
    val knopfZeilen = if (dichte.fontScale >= 1.5f) 2 else 1
    val knoepfe = 4.dp + innen + knopf * knopfZeilen + if (knopfZeilen > 1) 8.dp else 0.dp

    // Dieselbe Funktion, die auch der Rahmen selbst benutzt — und hier im App-Fenster aufgerufen,
    // genau wie dort (im Dialogfenster melden die Systemleisten 0 dp, siehe Dialoge.kt).
    val rahmen = dialogHoechstHoehe(PICKER_HOEHENANTEIL)
    return (rahmen - titel - 24.dp - knoepfe).coerceAtLeast(0.dp)
}

/**
 * Der seitliche Rand, den ein Picker-Dialog sich noch leisten kann, ohne [mindestBreite] zu
 * unterschreiten — und zwar nur so wenig wie nötig, nie weniger als nötig.
 *
 * Warum überhaupt: Der gewohnte Rahmen zieht 24 dp Außenabstand je Seite ab, dazu 20 dp Innenabstand
 * je Seite (bei Orbit 14 dp). Vom Schirm bleiben dem Inhalt damit:
 * ```
 *   320 dp → 320 − 48 − 40 = 232 dp
 *   360 dp → 360 − 48 − 40 = 272 dp
 *   393 dp → 393 − 48 − 40 = 305 dp
 *   412 dp → 412 − 48 − 40 = 324 dp
 * ```
 * Der Monatskalender braucht 360 dp ([KALENDER_BREITE]) und schrumpft nicht. Auf **keinem** dieser
 * Telefone hätte er gepasst — auf dem Prüfgerät (475 dp breit) dagegen schon, dort blieben 387 dp.
 * Deshalb wird nicht einfach auf die Tippeingabe zurückgefallen, sondern zuerst der Rahmen dünner
 * gezogen. Mit `inhaltRandSeitlich = 0` (die Material-Picker bringen ihren eigenen Innenabstand mit,
 * ein zweiter läge nur doppelt darüber) und diesem Rand ergibt sich:
 * ```
 *   320 dp → Rand 0    → 320 dp Inhalt → zu schmal, Tippeingabe
 *   360 dp → Rand 0    → 360 dp Inhalt → Kalender, Karte liegt randlos an
 *   393 dp → Rand 16,5 → 360 dp Inhalt → Kalender
 *   412 dp → Rand 24   → 364 dp Inhalt → Kalender, Optik unverändert
 *   475 dp → Rand 24   → 427 dp Inhalt → Kalender, Optik unverändert (Prüfgerät)
 * ```
 * Die Deckelung auf [DIALOG_RAND_SEITLICH] ist der Punkt: Wo der Platz reicht, sieht der Dialog
 * genauso aus wie jeder andere. Erst darunter gibt er Rand ab, und ab 360 dp Schirmbreite abwärts
 * liegt die Karte bündig an den Bildschirmkanten — bewusst in Kauf genommen, weil ein vollständiger
 * Kalender mehr wert ist als 24 dp Luft.
 */
@Composable
private fun pickerRand(mindestBreite: Dp): Dp =
    ((LocalConfiguration.current.screenWidthDp.dp - mindestBreite) / 2).coerceIn(0.dp, DIALOG_RAND_SEITLICH)

/**
 * Die Breite, die dem Dialoginhalt bei diesem [rand] bleibt. Der Rahmen deckelt sich zusätzlich auf
 * [DIALOG_MAX_BREITE]; der seitliche Innenabstand entfällt, weil die Picker-Dialoge unten
 * `inhaltRandSeitlich = 0.dp` übergeben. Dieselbe Zahl geht an [DesignDialog] und in die Schwelle —
 * sie können nicht auseinanderlaufen.
 *
 * Im Querformat ist `screenWidthDp` ab API 35 die volle Fensterbreite, eine seitliche Navigations-
 * leiste ist also **nicht** abgezogen. Für die liegende Uhr fängt das die Deckelung auf 560 dp ab
 * (bei 800 dp Schirm bleiben 560 dp, gebraucht werden 508 dp); exakt ist die Rechnung dort nicht.
 */
@Composable
private fun pickerInhaltsBreite(rand: Dp): Dp =
    minOf(LocalConfiguration.current.screenWidthDp.dp - rand * 2, DIALOG_MAX_BREITE).coerceAtLeast(0.dp)

/**
 * Höhe eines Zifferfelds der Tippeingabe — und zugleich die Kante, an der zugeschnitten wird.
 *
 * Aus dem Material-3-AAR im Gradle-Cache ausgelesen (1.3.1, Compose-BOM 2025.01.01), nicht
 * geschätzt: `TimePickerTextField` setzt an seinem Textfeld
 * `Modifier.size(TimeInputTokens.TimeFieldContainerWidth, TimeInputTokens.TimeFieldContainerHeight)`
 * = 96 × 72 dp. Der Innenabstand der Umrandung ist dabei **null** — die `DecorationBox` bekommt
 * ausdrücklich `PaddingValues(0.dp)` —, dem Text stehen die 72 dp also vollständig zur Verfügung,
 * mehr aber auch nicht. `size` beugt sich zwar eingehenden Grenzen, im Dialog kommt von oben aber
 * keine engere Vorgabe an; praktisch sind es immer genau 72 dp.
 */
private val ZIFFERNFELD_HOEHE = 72.dp

/**
 * Eine Typografie, deren `displayMedium` noch in ein [ZIFFERNFELD_HOEHE] hohes Feld passt — oder
 * `null`, wenn nichts zu deckeln ist und die Tippeingabe unverändert bleiben darf.
 *
 * **Warum ausgerechnet `displayMedium`.** Die Zifferneingabe holt ihren Textstil über den Token
 * `TimeInputTokens.TimeFieldLabelTextFont`, und der zeigt auf `DisplayMedium` — in der M3-Skala
 * 45 sp Schrift bei 52 sp Zeilenhöhe (beides aus dem AAR ausgelesen). `TimeInputImpl` legt genau
 * diesen Stil als `LocalTextStyle` über den ganzen Baustein: Stundenfeld, Minutenfeld **und** den
 * Doppelpunkt dazwischen, der seinen Stil ebenfalls nur aus `LocalTextStyle` liest (der Token
 * `TimeFieldSeparatorFont` steht zwar in der Tabelle, wird im Code aber nirgends abgefragt). Ein
 * einziger Stil deckt also alles ab, was in die 72-dp-Felder muss. Die Beschriftungen
 * „Stunde"/„Minute" laufen dagegen über `bodySmall`, stehen **unter** den Feldern und werden nicht
 * zugeschnitten — die bleiben hier bewusst unangetastet und skalieren voll mit.
 *
 * **Warum nicht wie `GedeckelteSchrift` in WeckerUi.kt die Dichte deckeln.** `Density(d, f)` baut
 * eine **lineare** Dichte. Die echte `LocalDensity` auf dem Gerät ist das nicht: `AndroidComposeView`
 * legt sie mit `Density(context)` an, und die hängt sich einen `FontScaleConverter` aus
 * `FontScaleConverterFactory.forScale(fontScale)` an (nachgelesen in `ui-unit` 1.7.7 im
 * Gradle-Cache). Tauschte man die gegen eine lineare Dichte, würde die Schrift dort kleiner, wo das
 * System sie längst gebändigt hat, und alle Beschriftungen im Baustein schrumpften mit. Hier wird
 * deshalb nur der eine Stil angefasst, der es nötig hat.
 *
 * **Linear oder nichtlinear — hier wird nicht unterschieden, sondern gemessen.** Der verbreitete
 * Satz „bis Android 13 linear, ab Android 14 die Kurve" gilt für Compose so nicht: die Kurve ist in
 * `androidx.compose.ui.unit.fontscaling` **nachgebaut** und greift ohne jede SDK-Abfrage, also auf
 * jeder Android-Fassung. Aus der Stützstellen-Tabelle im AAR (zwischen den Stützstellen wird linear
 * interpoliert, 30 sp und 100 sp sind die beiden obersten):
 * ```
 *   Systemschrift 1,0   kein Konverter (erst ab 1,03), rein proportional   45 sp → 45,0 dp
 *   Systemschrift 1,5   30 sp → 30 dp, 100 sp → 100 dp                     45 sp → 45,0 dp
 *   Systemschrift 2,0   30 sp → 38 dp, 100 sp → 100 dp                     45 sp → 51,3 dp
 * ```
 * Bei 1,5 wächst große Schrift also gar nicht, bei 2,0 um ein Siebtel — die oft zitierten 90 dp
 * kommen bis 2,0 nie zustande. Genau deshalb wird hier nicht gerechnet, sondern mit dem
 * `TextMeasurer` durch die echte Dichte gemessen: Er nimmt den Konverter mit, wie er gerade ist.
 * Das fängt auch die drei Fälle ab, die eine feste Grenze verfehlen würde:
 *  * Oberhalb der größten Stützstelle (2,0) legt `forScale` eine Ein-Punkt-Tabelle an und rechnet
 *    wieder rein proportional — bei 2,5 werden aus 45 sp tatsächlich rund 112 dp.
 *  * Die App-eigene Schriftskalierung (`GenialeIdeenTheme`, 0,85 bis 1,4) hebt in `skaliert()` nur
 *    `fontSize`, nicht `lineHeight`. Bei Faktor 1,4 stehen 63 sp in einer 52-sp-Zeile: der
 *    Schriftkasten ragt darüber hinaus, die Ziffern-Tinte (rund 45 sp) sitzt noch im Kasten. Eine
 *    Rechnung allein über die Zeilenhöhe würde diese Verschiebung gar nicht sehen.
 *  * Jedes Design darf über `titelSchrift` eine eigene Schrift für die Display-Stile mitbringen.
 *    Wie hoch deren Ober- und Unterlängen sind, weiß nur die Schrift selbst.
 *
 * **Welches Maß gemessen wird — und warum es bewusst eine Stufe strenger ist.** Gemessen wird mit
 * `lineHeight = Unspecified` der **Schriftkasten** aus Ober- und Unterlänge. Zugeschnitten wird in
 * Wahrheit bei `min(Zeilenkasten, 72 dp)`: Der M3-Stil trägt `LineHeightStyle(Center, Trim.None)`,
 * eine einzeilige Messung ergibt also genau die Zeilenhöhe, und das Textfeld klemmt sie auf die
 * Feldhöhe. Die Ziffern-**Tinte** ist mit rund 0,7 em noch einmal deutlich kleiner als der
 * Schriftkasten und hat darin je Seite etwa 0,2 em Luft. „Schriftkasten ≤ Feld" ist damit strenger
 * als „Tinte ≤ Feld" — und zwar absichtlich: Es ist dasselbe Verhältnis, das Material selbst mit
 * 52 sp Zeile in 72 dp Feld einhält, es hält die Ziffern mittig statt bündig an der Kante, und es
 * bleibt die sichere Richtung, wenn eine Design-Schrift ungewöhnliche Metriken mitbringt. Der
 * Preis: In seltenen Kombinationen (kastenreiche Schrift × App-Skalierung × Systemschrift 2,0) kann
 * die Deckelung um wenige Prozent greifen, ohne dass schon etwas sichtbar abgeschnitten wäre.
 *
 * **Bedienbarkeit.** Gedeckelt wird erst, wenn der Schriftkasten die 72 dp überschreitet, und dann
 * nur so weit, bis er hineinpasst — die Ziffern bleiben also immer so groß, wie das Feld es
 * überhaupt zulässt. Bei gewöhnlicher App-Schriftgröße greift die Deckelung auf keiner Systemstufe
 * bis 2,0: 51,3 dp Schrift ergeben mit Roboto-Metrik (1,17 em) rund 60 dp Kasten, also gut 12 dp
 * Luft — was die Design-Schrift daraus macht, misst der Code selbst, geschätzt wird nichts. Die
 * Deckelung ist ein Fangnetz für die Fälle oben, keine generelle Verkleinerung. Und sie rührt nur
 * an der Schrift: Die Tippflächen bleiben mit 96 × 72 dp unverändert weit über den geforderten
 * 48 dp.
 */
@Composable
private fun gedeckelteZifferSchrift(): Typography? {
    val messer = rememberTextMeasurer()
    val typo = MaterialTheme.typography
    val dichte = LocalDensity.current
    return remember(messer, typo, dichte.density, dichte.fontScale) {
        val stil = typo.displayMedium
        val groesse = stil.fontSize.value
        val zeile = stil.lineHeight.value
        // Unspezifizierte Größen sind NaN; dann gibt es nichts zu rechnen.
        if (groesse.isNaN() || groesse <= 0f) return@remember null
        val grenze = with(dichte) { ZIFFERNFELD_HOEHE.toPx() }
        // `lineHeight = Unspecified` heißt: kein vorgegebener Zeilenkasten, die Schrift gibt ihre
        // eigene Höhe an. Das ist der Schriftkasten aus Ober- und Unterlänge — das strengere Maß,
        // siehe den Absatz oben; `lineHeightStyle` bleibt dabei wirkungslos, weil es nichts zu
        // verteilen gibt.
        fun hoeheBei(faktor: Float): Float = messer.measure(
            "00",
            stil.copy(fontSize = (groesse * faktor).sp, lineHeight = TextUnit.Unspecified),
            maxLines = 1, softWrap = false,
        ).size.height.toFloat()
        fun mitFaktor(faktor: Float) = typo.copy(
            displayMedium = stil.copy(
                fontSize = (groesse * faktor).sp,
                // Die Zeilenhöhe zieht mit, sonst stünde eine kleine Schrift in einem unverändert
                // hohen Zeilenkasten und säße nicht mehr mittig im Feld.
                lineHeight = if (zeile.isNaN()) stil.lineHeight else (zeile * faktor).sp,
            ),
        )
        if (hoeheBei(1f) <= grenze) return@remember null
        // Halbierung zwischen einer sicher passenden Untergrenze und der zu großen Wunschgröße —
        // dasselbe Verfahren wie `passendeUhrGroesse` in WeckerUi.kt. 0,4 ist die Notbremse: Wer
        // die Ziffern bei 45 sp Grundgröße unter 18 sp drückt, hat ein anderes Problem.
        var passt = 0.4f
        var zuGross = 1f
        if (hoeheBei(passt) > grenze) return@remember mitFaktor(passt)
        repeat(7) {
            val mitte = (passt + zuGross) / 2f
            if (hoeheBei(mitte) <= grenze) passt = mitte else zuGross = mitte
        }
        mitFaktor(passt)
    }
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
    // So viel Breite braucht die Fassung, die Material zeichnen würde — danach richtet sich, wie
    // viel Rand der Rahmen noch abgeben kann.
    val mindestBreite = if (quer) UHR_QUER_BREITE else UHR_HOCH_BREITE
    val schmalerRand = pickerRand(mindestBreite)
    val breite = pickerInhaltsBreite(schmalerRand)
    val platzFuerZifferblatt =
        if (quer) raum >= UHR_QUER_BEDARF && breite >= UHR_QUER_BREITE
        else raum >= UHR_HOCH_BEDARF && breite >= UHR_HOCH_BREITE
    // Reicht es trotzdem nicht, steht die schmale Tippeingabe im Dialog — dann darf er seinen
    // gewohnten Rand behalten. Der Rand hängt bewusst nicht am Zifferblatt-Platz „gerade eben",
    // sondern an derselben Entscheidung, die auch den Inhalt wählt: beide sind für eine
    // Bildschirmlage fest, die Rahmenbreite springt also nicht.
    val rand = if (platzFuerZifferblatt) schmalerRand else DIALOG_RAND_SEITLICH

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
        // Das Zifferblatt kann nicht schrumpfen; der Rahmen gibt ihm alles bis zur harten Grenze.
        hoehenAnteil = PICKER_HOEHENANTEIL,
        randSeitlich = rand,
        // `TimePicker` und `TimeInput` bringen ihren eigenen Innenabstand mit und stehen mittig;
        // ein zweiter Innenabstand nähme nur Breite weg. Titel und Knopfzeile behalten ihren.
        inhaltRandSeitlich = 0.dp,
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
                // Die Zifferneingabe schneidet ihre Ziffern **innen** zu (feste 72 dp hohe Felder);
                // der Scroll oben hilft dagegen nicht, er sieht die Überlänge gar nicht. Siehe
                // [gedeckelteZifferSchrift] für die Messung und die Grenze.
                val enger = gedeckelteZifferSchrift()
                if (enger == null) {
                    // Der Normalfall: nichts zu deckeln, also auch kein zusätzlicher Wrapper —
                    // der auf dem Gerät geprüfte Stand bleibt Bit für Bit derselbe.
                    TimeInput(state = zustand, modifier = platz, colors = farben)
                } else {
                    // `MaterialTheme` reicht nur Kompositionswerte weiter und legt **keinen**
                    // Layout-Knoten dazwischen: `align` und `weight(1f, fill = false)` aus [platz]
                    // wirken unverändert gegen die Spalte des Dialogs. Farbschema und Formen
                    // übernimmt der Aufruf ungefragt aus dem umgebenden Theme.
                    MaterialTheme(typography = enger) {
                        TimeInput(state = zustand, modifier = platz, colors = farben)
                    }
                }
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
    // Zwei Maße, nicht eines: Bis hierher entschied allein die Höhe, und auf einem 360-dp-Telefon
    // erschien deshalb ein Kalender, der mit 360 dp gar nicht in die 272 dp Inhaltsbreite passte —
    // seine Tagesfelder sind `requiredSize`, sie schoben sich übereinander (siehe
    // [KALENDER_BREITE]). Der Rahmen gibt jetzt erst so viel Rand ab, wie der Kalender braucht,
    // und nur wenn das immer noch nicht reicht, fällt die Wahl auf die Tippeingabe.
    val schmalerRand = pickerRand(KALENDER_BREITE)
    val platzFuerKalender =
        dialogInhaltsHoehe() >= KALENDER_BEDARF && pickerInhaltsBreite(schmalerRand) >= KALENDER_BREITE
    // Ohne Kalender steht nur die schmale Tippeingabe im Dialog — dann behält er seinen
    // gewohnten Rand. Der Wert hängt am Platz, nicht am gerade gewählten Modus: Beim Umschalten
    // von Hand soll die Rahmenbreite nicht springen.
    val rand = if (platzFuerKalender) schmalerRand else DIALOG_RAND_SEITLICH

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
        // Bei wenig Platz startet die Wahl in der Tippeingabe; dort verschwindet dann auch der
        // Umschalter (`showModeToggle` unten), damit der zu große Kalender gar nicht erst
        // erreichbar ist.
        initialDisplayMode = if (platzFuerKalender) DisplayMode.Picker else DisplayMode.Input,
        selectableDates = waehlbar,
    )

    // `initialDisplayMode` greift nur beim Anlegen, der Modus selbst wird über die Drehung hinweg
    // gesichert. Ohne diesen Nachzug stünde nach dem Drehen ins Querformat wieder der Kalender da —
    // abgeschnitten.
    //
    // Dieser Effekt allein hat nicht gereicht, und das war die zweite Engstelle: Er läuft nur, wenn
    // sich [platzFuerKalender] **ändert**. Der Umschalter stand aber immer da, der Nutzer konnte den
    // Kalender im Querformat also jederzeit wieder einschalten — der Wert änderte sich dabei nicht,
    // der Effekt lief nicht erneut, und die 512 dp Kalenderhöhe standen in rund 344 dp Fensterhöhe
    // minus Rahmen. Gescrollt werden darf im Kalendermodus nicht (Jahresauswahl, siehe unten),
    // also verschwindet stattdessen der Umschalter: `showModeToggle = platzFuerKalender`.
    // Material blendet damit nur das Symbol in der Kopfzeile aus; `displayMode` bleibt von hier aus
    // setzbar, dieser Effekt arbeitet unverändert weiter. Abbrechen und Übernehmen stehen in der
    // Knopfzeile des Rahmens und sind davon ohnehin nicht berührt.
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
        // Derselbe Anteil, mit dem `dialogInhaltsHoehe()` oben die Schwelle gerechnet hat.
        hoehenAnteil = PICKER_HOEHENANTEIL,
        // Und dieselbe Breitenrechnung, mit der `platzFuerKalender` entschieden wurde.
        randSeitlich = rand,
        // Der `DatePicker` polstert sich mit 12 dp je Seite selbst (`DatePickerHorizontalPadding`);
        // ein zweiter Innenabstand käme nur oben drauf und nähme dem Raster seine 360 dp.
        inhaltRandSeitlich = 0.dp,
        inhalt = {
            // `weight(1f, fill = false)` hält die Knopfzeile auch dann im Dialog, wenn der Kalender
            // mehr Platz haben möchte, als übrig ist.
            //
            // Gescrollt werden darf hier **nur** die Tippeingabe. Der Kalender bringt mit seiner
            // Jahresauswahl ein eigenes senkrecht scrollendes `LazyVerticalGrid` mit — genau das
            // Muster, vor dem der Bug-Almanach jetpack-compose §6.1 warnt: gleichachsiges Scrollen
            // ineinander. Material begrenzt die Höhe dieses Gitters an seiner Aufrufstelle zwar
            // selbst (`requiredHeight(48 dp × 6 − 56 dp)`), die Messung liefe also nicht in
            // „infinity constraints" — darauf bauen wir aber nicht: Der Gestenkonflikt bliebe, und
            // die Auflage kann mit der nächsten Material-Version fallen. Im Kalendermodus sind
            // deshalb allein die beiden Schwellen oben der Schutz — Höhe **und** Breite, beide
            // gegen die echten Material-Maße gerechnet — und der Umschalter, der bei zu wenig
            // Platz gar nicht erst erscheint.
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
                // Kein Platz, kein Umschalter: Ein Modus, der nicht vollständig auf den Schirm
                // passt, darf auch nicht wählbar sein (siehe der Effekt weiter oben).
                showModeToggle = platzFuerKalender,
                colors = farben,
            )
        },
    )
}
