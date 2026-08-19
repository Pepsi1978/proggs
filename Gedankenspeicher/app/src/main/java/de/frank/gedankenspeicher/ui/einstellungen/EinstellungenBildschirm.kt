package de.frank.gedankenspeicher.ui.einstellungen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.BuildConfig
import de.frank.gedankenspeicher.auth.CodexModel
import de.frank.gedankenspeicher.auth.ReasoningEffort
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.data.settings.Websuche
import de.frank.gedankenspeicher.tts.ClonedVoice
import de.frank.gedankenspeicher.tts.TtsCatalog
import de.frank.gedankenspeicher.tts.TtsProvider
import de.frank.gedankenspeicher.ui.ki.GeranderterKnopf
import de.frank.gedankenspeicher.ui.ki.Wahlfeld
import de.frank.gedankenspeicher.ui.theme.Erscheinung
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/**
 * **B-04 — die Einstellungen.**
 *
 * Die **Zugänge** stehen in einer eigenen Gruppe, nicht bei dem Dienst, den sie freischalten.
 * Vorher lagen sie darunter — und weil ein Dienst ohne Schlüssel ausgegraut war, kam man an
 * das Feld für seinen Schlüssel gar nicht heran: Google liess sich nicht wählen, weil der
 * Schlüssel fehlte, und der Schlüssel liess sich nicht eintragen, weil Google nicht gewählt
 * war. Ein geschlossener Kreis, aus dem es keinen Weg gab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinstellungenBildschirm(
    erscheinung: String,
    codexVerbunden: Boolean,
    codexKonto: String?,
    codexModell: String,
    codexEffort: String,
    verbesserungModell: String,
    verbesserungEffort: String,
    websucheGrundhaltung: String,
    groqSchluessel: String,
    ttsAnbieter: String,
    ttsStimme: String,
    googleSchluessel: String,
    qwenSchluessel: String,
    eigeneStimmen: List<ClonedVoice>,
    stimmenLaden: Boolean,
    nimmtStimmeAuf: Boolean,
    probeLaeuft: Boolean,
    fingerabdruckAn: Boolean,
    driveAn: Boolean,
    letzteSicherung: Long,
    letzteGroesse: Long,
    beiErscheinung: (String) -> Unit,
    beiVerbinden: () -> Unit,
    beiTrennen: () -> Unit,
    beiModell: (String) -> Unit,
    beiEffort: (String) -> Unit,
    beiVerbesserungModell: (String) -> Unit,
    beiVerbesserungEffort: (String) -> Unit,
    beiWebsuche: (String) -> Unit,
    beiProfile: () -> Unit,
    beiGroq: (String) -> Unit,
    beiAnbieter: (String) -> Unit,
    beiStimme: (String) -> Unit,
    beiGoogleSchluessel: (String) -> Unit,
    beiQwenSchluessel: (String) -> Unit,
    beiStimmenLaden: () -> Unit,
    beiStimmeAufnehmen: () -> Unit,
    beiStimmeLoeschen: (String) -> Unit,
    beiProbe: () -> Unit,
    beiFingerabdruck: (Boolean) -> Unit,
    beiDrive: (Boolean) -> Unit,
    beiJetztSichern: () -> Unit,
    beiWiederherstellen: () -> Unit,
    beiZurueck: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    Column(Modifier.fillMaxSize().background(farben.hintergrund)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(Masse.kopfleiste).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = beiZurueck) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = farben.textMittel)
            }
            Text("Einstellungen", style = schrift.bildschirmtitel, color = farben.textStark)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Masse.seitenrand)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1 — Erscheinung (F-15)
            Gruppe("Erscheinung") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Erscheinung.entries.forEach { e ->
                        Erscheinungskachel(
                            erscheinung = e,
                            gewaehlt = e.id == erscheinung,
                            modifier = Modifier.weight(1f),
                            beiDruck = { beiErscheinung(e.id) },
                        )
                    }
                }
            }

            // 2 — Auswertung · Codex (F-11)
            Gruppe("Auswertung — Codex") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Verbindung", style = schrift.einstellung, color = farben.textMittel)
                        Text(
                            if (codexVerbunden) {
                                "verbunden${codexKonto?.let { " als $it" }.orEmpty()}"
                            } else {
                                "nicht verbunden"
                            },
                            style = schrift.einstellungErklaerung,
                            color = if (codexVerbunden) farben.erfolg else farben.textSchwach,
                        )
                    }
                    GeranderterKnopf(
                        beschriftung = if (codexVerbunden) "Trennen" else "Verbinden",
                        farbe = if (codexVerbunden) farben.textMittel else farben.akzent,
                        beiDruck = if (codexVerbunden) beiTrennen else beiVerbinden,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Beschriftung("Modell")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CodexModel.entries.forEach { m ->
                        Wahlfeld(m.label, m.apiId == codexModell) { beiModell(m.apiId) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Beschriftung("Effort")
                Erklaerung("Wie gründlich das Modell nachdenkt, bevor es antwortet.")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReasoningEffort.entries.forEach { e ->
                        Wahlfeld(e.label, e.apiValue == codexEffort) { beiEffort(e.apiValue) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Beschriftung("Websuche")
                Erklaerung("Die Grundhaltung. Im KI-Blatt lässt sie sich für eine einzelne Auswertung überstimmen.")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Websuche.entries.forEach { w ->
                        Wahlfeld(w.label, w.id == websucheGrundhaltung) { beiWebsuche(w.id) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                // Was wirklich benutzt wird, steht hier ausgeschrieben — nicht nur als
                // hervorgehobenes Feld irgendwo darüber.
                Text(
                    text = buildString {
                        append("Es läuft: ")
                        append(CodexModel.fromLabel(codexModell).label)
                        append(" · Effort ").append(ReasoningEffort.fromLabel(codexEffort).label)
                        append(" · Websuche ").append(Websuche.vonId(websucheGrundhaltung).label)
                    },
                    style = schrift.einstellungErklaerung,
                    color = farben.akzent,
                )
            }

            // 2b — Textverbesserung · Codex (F-07)
            //
            // Ein eigener Bereich, weil die Textverbesserung etwas anderes tut als die
            // Auswertung: sie räumt einen Text auf, statt ihn zu durchdenken. Dafür zählt
            // Tempo, nicht Tiefe — und genau das lässt sich hier getrennt einstellen.
            Gruppe("Textverbesserung — Codex") {
                Erklaerung(
                    "Gilt für den Knopf „Text verbessern\" an einer Notiz — überall in der App. " +
                        "Die Auswertung bleibt davon unberührt.",
                )

                Spacer(Modifier.height(14.dp))
                Beschriftung("Modell")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CodexModel.entries.forEach { m ->
                        Wahlfeld(m.label, m.apiId == verbesserungModell) { beiVerbesserungModell(m.apiId) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Beschriftung("Effort")
                Erklaerung("Wie gründlich das Modell nachdenkt, bevor es antwortet.")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReasoningEffort.entries.forEach { e ->
                        Wahlfeld(e.label, e.apiValue == verbesserungEffort) { beiVerbesserungEffort(e.apiValue) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = buildString {
                        append("Es läuft: ")
                        append(CodexModel.fromLabel(verbesserungModell).label)
                        append(" · Effort ").append(ReasoningEffort.fromLabel(verbesserungEffort).label)
                    },
                    style = schrift.einstellungErklaerung,
                    color = farben.akzent,
                )
            }

            // 3 — Auswertungsprofile (F-10)
            Gruppe("Auswertungsprofile") {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = beiProfile).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Sechs Profile — genau eines ist aktiv",
                        style = schrift.einstellung,
                        color = farben.textMittel,
                        modifier = Modifier.weight(1f),
                    )
                    Text("öffnen", style = schrift.zeitstempel, color = farben.akzent)
                }
            }

            // 4 — Zugänge: alle drei Schlüssel, immer erreichbar
            Gruppe("Zugänge") {
                Schluesselfeld(
                    beschriftung = "Groq",
                    zweck = "Transkription",
                    wert = groqSchluessel,
                    beiAenderung = beiGroq,
                )
                Spacer(Modifier.height(14.dp))
                Schluesselfeld(
                    beschriftung = "Google Cloud",
                    zweck = "Chirp-3-HD-Stimmen",
                    wert = googleSchluessel,
                    beiAenderung = beiGoogleSchluessel,
                )
                Spacer(Modifier.height(14.dp))
                Schluesselfeld(
                    beschriftung = "Alibaba",
                    zweck = "deine eigene Stimme",
                    wert = qwenSchluessel,
                    beiAenderung = beiQwenSchluessel,
                )
            }

            // 5 — Stimme (F-18)
            Gruppe("Stimme") {
                Beschriftung("Dienst")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Jeder Dienst ist wählbar — auch ohne Schlüssel. Fehlt einer, sagt die
                    // Zeile darunter, welcher, statt den Weg dorthin zu versperren.
                    TtsProvider.entries.forEach { p ->
                        Wahlfeld(p.label, p.id == ttsAnbieter) { beiAnbieter(p.id) }
                    }
                }

                val gewaehlt = TtsProvider.entries.firstOrNull { it.id == ttsAnbieter } ?: TtsProvider.EDGE
                Spacer(Modifier.height(14.dp))

                when (gewaehlt) {
                    TtsProvider.EDGE -> {
                        Beschriftung("Stimme")
                        Erklaerung("Braucht nur Netz, keinen Schlüssel.")
                        StimmListe(
                            eintraege = TtsCatalog.edgeVoices.map { it.id to it.name },
                            gewaehlt = ttsStimme,
                            beiWahl = beiStimme,
                        )
                    }

                    TtsProvider.GOOGLE_CLOUD -> {
                        Beschriftung("Stimme")
                        if (googleSchluessel.isBlank()) {
                            Hinweis("Trag oben unter „Zugänge\" den Google-Cloud-Schlüssel ein.")
                        }
                        Erklaerung("${TtsCatalog.googleVoices.size} Chirp-3-HD-Stimmen.")
                        StimmListe(
                            eintraege = TtsCatalog.googleVoices.map { it.id to it.name },
                            gewaehlt = ttsStimme,
                            beiWahl = beiStimme,
                        )
                    }

                    TtsProvider.QWEN_CLONE -> EigeneStimme(
                        schluesselDa = qwenSchluessel.isNotBlank(),
                        stimmen = eigeneStimmen,
                        gewaehlt = ttsStimme,
                        laden = stimmenLaden,
                        nimmtAuf = nimmtStimmeAuf,
                        beiWahl = beiStimme,
                        beiLaden = beiStimmenLaden,
                        beiAufnehmen = beiStimmeAufnehmen,
                        beiLoeschen = beiStimmeLoeschen,
                    )

                    TtsProvider.GERAET -> Erklaerung(
                        "Es spricht, was Android mitbringt — ohne Netz, ohne Schlüssel.",
                    )
                }

                Spacer(Modifier.height(16.dp))
                GeranderterKnopf(
                    beschriftung = if (probeLaeuft) "Probe anhalten" else "Probe hören",
                    beiDruck = beiProbe,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 5b — Sicherheit: der Fingerabdruck für geschützte Notizen
            Gruppe("Sicherheit") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fingerabdruck", style = schrift.einstellung, color = farben.textMittel)
                        Text(
                            "Erlaubt es, Notizen zu schützen. Öffnen, Schützen und Freigeben gehen dann nur noch über den Fingerabdruck.",
                            style = schrift.einstellungErklaerung,
                            color = farben.textSchwach,
                        )
                    }
                    Switch(
                        checked = fingerabdruckAn,
                        onCheckedChange = beiFingerabdruck,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = farben.hintergrund,
                            checkedTrackColor = farben.akzent,
                            uncheckedThumbColor = farben.textSchwach,
                            uncheckedTrackColor = farben.hintergrundErhoben,
                            uncheckedBorderColor = farben.rand,
                        ),
                    )
                }
                if (!fingerabdruckAn) {
                    Erklaerung("Bereits geschützte Notizen bleiben geschützt und brauchen weiter den Fingerabdruck — nur neue lassen sich nicht mehr schützen.")
                }
            }

            // 6 — Sicherung (F-17)
            Gruppe("Sicherung") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("In einen Ordner sichern", style = schrift.einstellung, color = farben.textMittel)
                        Text(
                            if (letzteSicherung > 0) {
                                "zuletzt ${Repository.zeitpunkt(letzteSicherung)} · ${letzteGroesse / 1024} kB"
                            } else {
                                "noch nie gesichert"
                            },
                            style = schrift.einstellungErklaerung,
                            color = farben.textSchwach,
                        )
                    }
                    Switch(
                        checked = driveAn,
                        onCheckedChange = beiDrive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = farben.hintergrund,
                            checkedTrackColor = farben.akzent,
                            uncheckedThumbColor = farben.textSchwach,
                            uncheckedTrackColor = farben.hintergrundErhoben,
                            uncheckedBorderColor = farben.rand,
                        ),
                    )
                }
                if (driveAn) {
                    Erklaerung("Wähle beim ersten Sichern deinen Google-Drive-Ordner — dann liegt die Sicherung dort.")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GeranderterKnopf("Jetzt sichern", beiJetztSichern)
                        GeranderterKnopf("Wiederherstellen", beiWiederherstellen, farbe = farben.fehler)
                    }
                }
            }

            // 7 — Über
            Gruppe("Über") {
                Text(
                    "Gedankenspeicher ${BuildConfig.VERSION_NAME}",
                    style = schrift.einstellung,
                    color = farben.textMittel,
                )
                Text(BuildConfig.VERSION_BUMPED_AT, style = schrift.einstellungErklaerung, color = farben.textSchwach)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * **„Meine Stimme" (F-18).**
 *
 * Drei Dinge an einer Stelle: die vorhandenen Stimmen wählen, eine neue aufnehmen, eine
 * löschen. Ohne Schlüssel steht statt der Liste der eine Satz, der sagt, was fehlt — und wo
 * es einzutragen ist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EigeneStimme(
    schluesselDa: Boolean,
    stimmen: List<ClonedVoice>,
    gewaehlt: String,
    laden: Boolean,
    nimmtAuf: Boolean,
    beiWahl: (String) -> Unit,
    beiLaden: () -> Unit,
    beiAufnehmen: () -> Unit,
    beiLoeschen: (String) -> Unit,
) {
    val farben = Farben

    if (!schluesselDa) {
        Hinweis("Trag oben unter „Zugänge\" den Alibaba-Schlüssel ein — danach erscheinen deine Stimmen hier.")
        return
    }

    Beschriftung("Deine Stimmen")
    when {
        laden -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = farben.akzent)
            Spacer(Modifier.width(10.dp))
            Erklaerung("wird geladen …")
        }

        stimmen.isEmpty() -> Erklaerung("Noch keine Stimme aufgenommen.")

        else -> StimmListe(
            eintraege = stimmen.map { it.id to it.name },
            gewaehlt = gewaehlt,
            beiWahl = beiWahl,
        )
    }

    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        GeranderterKnopf(
            beschriftung = if (nimmtAuf) "Aufnahme beenden" else "Neue Stimme aufnehmen",
            farbe = if (nimmtAuf) farben.fehler else farben.akzent,
            beiDruck = beiAufnehmen,
        )
        IconButton(onClick = beiLaden) {
            Icon(Icons.Outlined.Refresh, "Stimmen neu laden", Modifier.size(20.dp), tint = farben.textMittel)
        }
        if (gewaehlt.isNotBlank() && stimmen.any { it.id == gewaehlt }) {
            IconButton(onClick = { beiLoeschen(gewaehlt) }) {
                Icon(Icons.Outlined.Delete, "Gewählte Stimme löschen", Modifier.size(20.dp), tint = farben.fehler)
            }
        }
    }
    if (nimmtAuf) {
        Spacer(Modifier.height(8.dp))
        Erklaerung("Sprich einige Sätze in normalem Tempo — je mehr, desto ähnlicher wird der Klang.")
    }
}

/** Eine Stimmliste als umbrechende Wahlfelder — sie kann dreissig Einträge lang werden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StimmListe(
    eintraege: List<Pair<String, String>>,
    gewaehlt: String,
    beiWahl: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        eintraege.forEach { (kennung, name) ->
            Wahlfeld(name, kennung == gewaehlt) { beiWahl(kennung) }
        }
    }
}

@Composable
private fun Gruppe(titel: String, inhalt: @Composable () -> Unit) {
    val farben = Farben
    Column {
        Text(
            titel,
            style = Schriften.kartenUeberschrift,
            color = farben.akzent,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.gruppeRadius))
                .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
                .padding(14.dp),
        ) {
            inhalt()
        }
    }
}

@Composable
private fun Beschriftung(text: String) =
    Text(text, style = Schriften.einstellung, color = Farben.textMittel, modifier = Modifier.padding(bottom = 4.dp))

@Composable
private fun Erklaerung(text: String) = Text(
    text,
    style = Schriften.einstellungErklaerung,
    color = Farben.textSchwach,
    modifier = Modifier.padding(bottom = 6.dp),
)

/** Ein Hinweis, der sagt, was fehlt — in der Fehlerfarbe, damit er nicht überlesen wird. */
@Composable
private fun Hinweis(text: String) = Text(
    text,
    style = Schriften.einstellungErklaerung,
    color = Farben.fehler,
    modifier = Modifier.padding(bottom = 8.dp),
)

/**
 * Ein Schlüsselfeld: verdeckt, mit Augensymbol zum Anzeigen.
 *
 * Fehlt der Schlüssel, trägt das Feld einen Rand in der Fehlerfarbe. Steht er, ein Häkchen —
 * sonst weiss man nach dem Eintragen nicht, ob er wirklich angekommen ist.
 */
@Composable
private fun Schluesselfeld(
    beschriftung: String,
    zweck: String,
    wert: String,
    beiAenderung: (String) -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    var sichtbar by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(beschriftung, style = schrift.einstellung, color = farben.textMittel)
            Spacer(Modifier.width(6.dp))
            Text(
                "· $zweck",
                style = schrift.einstellungErklaerung,
                color = farben.textSchwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (wert.isNotBlank()) {
                Icon(Icons.Outlined.Check, "hinterlegt", Modifier.size(16.dp), tint = farben.erfolg)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.hintergrund, RoundedCornerShape(Masse.profilRadius))
                .border(
                    1.dp,
                    if (wert.isBlank()) farben.fehler else farben.rand,
                    RoundedCornerShape(Masse.profilRadius),
                )
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (wert.isEmpty()) {
                    Text("noch nicht hinterlegt", style = schrift.eingabefeld, color = farben.textSchwach)
                }
                BasicTextField(
                    value = wert,
                    onValueChange = beiAenderung,
                    textStyle = schrift.eingabefeld.copy(color = farben.textStark),
                    cursorBrush = SolidColor(farben.akzent),
                    singleLine = true,
                    visualTransformation = if (sichtbar) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = { sichtbar = !sichtbar }) {
                Icon(
                    if (sichtbar) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (sichtbar) "Schlüssel verbergen" else "Schlüssel anzeigen",
                    Modifier.size(20.dp),
                    tint = farben.textSchwach,
                )
            }
        }
    }
}

/**
 * Eine Kachel je Erscheinung — mit einer Miniatur ihrer Farbwelt.
 *
 * Die Miniatur zeigt die drei Rollen, an denen man eine Erscheinung wirklich erkennt:
 * Grundfläche, erhobene Fläche, Akzent. Ein Name allein sagt nichts über das Aussehen.
 */
@Composable
private fun Erscheinungskachel(
    erscheinung: Erscheinung,
    gewaehlt: Boolean,
    modifier: Modifier = Modifier,
    beiDruck: () -> Unit,
) {
    val farben = Farben
    val ihre = erscheinung.farben
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Masse.profilRadius))
                .background(Brush.linearGradient(listOf(ihre.hintergrund, ihre.hintergrundErhoben)))
                .border(
                    if (gewaehlt) 2.dp else 1.dp,
                    if (gewaehlt) farben.akzent else farben.rand,
                    RoundedCornerShape(Masse.profilRadius),
                )
                .clickable(onClick = beiDruck),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(width = 26.dp, height = 5.dp).background(ihre.akzent, RoundedCornerShape(3.dp)))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(width = 20.dp, height = 4.dp).background(ihre.textMittel, RoundedCornerShape(2.dp)))
                if (gewaehlt) {
                    Spacer(Modifier.height(6.dp))
                    Icon(Icons.Outlined.Check, "gewählt", Modifier.size(16.dp), tint = ihre.akzent)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            erscheinung.label,
            style = Schriften.zeitstempel,
            color = if (gewaehlt) farben.akzent else farben.textSchwach,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
