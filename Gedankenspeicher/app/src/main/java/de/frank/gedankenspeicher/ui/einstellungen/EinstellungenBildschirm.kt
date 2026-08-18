package de.frank.gedankenspeicher.ui.einstellungen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.BuildConfig
import de.frank.gedankenspeicher.auth.CodexModel
import de.frank.gedankenspeicher.auth.ReasoningEffort
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.data.settings.Websuche
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
 * Sieben Gruppen in der Reihenfolge des Specs. Sie steht bewusst nicht unter „später":
 * ohne Groq-Schlüssel und Codex-Anmeldung ist die App eine Notiz-App ohne KI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinstellungenBildschirm(
    erscheinung: String,
    codexVerbunden: Boolean,
    codexKonto: String?,
    codexModell: String,
    codexEffort: String,
    websucheGrundhaltung: String,
    groqSchluessel: String,
    ttsAnbieter: String,
    ttsStimme: String,
    googleSchluessel: String,
    qwenSchluessel: String,
    probeLaeuft: Boolean,
    driveAn: Boolean,
    letzteSicherung: Long,
    letzteGroesse: Long,
    beiErscheinung: (String) -> Unit,
    beiVerbinden: () -> Unit,
    beiTrennen: () -> Unit,
    beiModell: (String) -> Unit,
    beiEffort: (String) -> Unit,
    beiWebsuche: (String) -> Unit,
    beiProfile: () -> Unit,
    beiGroq: (String) -> Unit,
    beiAnbieter: (String) -> Unit,
    beiStimme: (String) -> Unit,
    beiGoogleSchluessel: (String) -> Unit,
    beiQwenSchluessel: (String) -> Unit,
    beiProbe: () -> Unit,
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

            // 2 — Codex (F-11)
            Gruppe("Codex") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Verbindung", style = schrift.einstellung, color = farben.textMittel)
                        Text(
                            if (codexVerbunden) "verbunden${codexKonto?.let { " als $it" }.orEmpty()}" else "nicht verbunden",
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
                Spacer(Modifier.height(14.dp))
                Beschriftung("Modell")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CodexModel.entries.forEach { m ->
                        Wahlfeld(m.label, m.apiId == codexModell) { beiModell(m.apiId) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Beschriftung("Effort")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReasoningEffort.entries.forEach { e ->
                        Wahlfeld(e.label, e.apiValue == codexEffort) { beiEffort(e.apiValue) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Beschriftung("Websuche")
                Erklaerung("Die Grundhaltung. Im KI-Blatt lässt sie sich für eine einzelne Auswertung überstimmen.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Websuche.entries.forEach { w ->
                        Wahlfeld(w.label, w.id == websucheGrundhaltung) { beiWebsuche(w.id) }
                    }
                }
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

            // 4 — Transkription (F-03)
            Gruppe("Transkription") {
                Schluesselfeld("Groq-Schlüssel", groqSchluessel, beiGroq)
                Spacer(Modifier.height(6.dp))
                Erklaerung("Modell: whisper-large-v3-turbo · vier Schichten gegen Halluzinationen")
            }

            // 5 — Stimme (F-18)
            Gruppe("Stimme") {
                Beschriftung("Dienst")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TtsProvider.entries.forEach { p ->
                        val fehlt = when (p) {
                            TtsProvider.GOOGLE_CLOUD -> googleSchluessel.isBlank()
                            TtsProvider.QWEN_CLONE -> qwenSchluessel.isBlank()
                            else -> false
                        }
                        Box(Modifier.alphaWennFehlt(fehlt)) {
                            Wahlfeld(p.label, p.id == ttsAnbieter) { if (!fehlt) beiAnbieter(p.id) }
                        }
                    }
                }
                val gewaehlt = TtsProvider.entries.firstOrNull { it.id == ttsAnbieter } ?: TtsProvider.EDGE
                if (gewaehlt == TtsProvider.GOOGLE_CLOUD && googleSchluessel.isBlank()) {
                    Erklaerung("Für Google Chirp 3 HD fehlt der Schlüssel.")
                }
                if (gewaehlt == TtsProvider.QWEN_CLONE && qwenSchluessel.isBlank()) {
                    Erklaerung("Für die eigene Stimme fehlen Schlüssel und Sprachprobe.")
                }
                Spacer(Modifier.height(12.dp))
                when (gewaehlt) {
                    TtsProvider.EDGE -> {
                        Beschriftung("Stimme")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TtsCatalog.edgeVoices.forEach { v ->
                                Wahlfeld(v.name, v.id == ttsStimme) { beiStimme(v.id) }
                            }
                        }
                    }
                    TtsProvider.GOOGLE_CLOUD -> {
                        Schluesselfeld("Google-Cloud-Schlüssel", googleSchluessel, beiGoogleSchluessel)
                        Spacer(Modifier.height(10.dp))
                        Beschriftung("Stimme")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TtsCatalog.googleVoices.take(12).forEach { v ->
                                Wahlfeld(v.name, v.id == ttsStimme) { beiStimme(v.id) }
                            }
                        }
                    }
                    TtsProvider.QWEN_CLONE -> Schluesselfeld("Qwen-Schlüssel", qwenSchluessel, beiQwenSchluessel)
                    TtsProvider.GERAET -> Erklaerung("Es spricht, was Android mitbringt — ohne Netz, ohne Schlüssel.")
                }
                Spacer(Modifier.height(14.dp))
                GeranderterKnopf(
                    beschriftung = if (probeLaeuft) "Probe anhalten" else "Probe hören",
                    beiDruck = beiProbe,
                )
            }

            // 6 — Sicherung (F-17)
            Gruppe("Sicherung") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Nach Google Drive sichern", style = schrift.einstellung, color = farben.textMittel)
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
    Text(text, style = Schriften.einstellung, color = Farben.textMittel, modifier = Modifier.padding(bottom = 6.dp))

@Composable
private fun Erklaerung(text: String) =
    Text(text, style = Schriften.einstellungErklaerung, color = Farben.textSchwach, modifier = Modifier.padding(vertical = 4.dp))

/**
 * Ein Schlüsselfeld: verdeckt, mit Augensymbol zum Anzeigen. Fehlt der Schlüssel, trägt das
 * Feld einen Rand in der Fehlerfarbe — sonst sucht man den Grund an der falschen Stelle.
 */
@Composable
private fun Schluesselfeld(beschriftung: String, wert: String, beiAenderung: (String) -> Unit) {
    val farben = Farben
    val schrift = Schriften
    var sichtbar by remember { mutableStateOf(false) }
    Column {
        Beschriftung(beschriftung)
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
                .background(
                    Brush.linearGradient(listOf(ihre.hintergrund, ihre.hintergrundErhoben)),
                )
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
        )
    }
}

/** Ein Dienst ohne Schlüssel ist ausgegraut — und bleibt es, bis der Schlüssel da ist. */
private fun Modifier.alphaWennFehlt(fehlt: Boolean): Modifier =
    if (fehlt) this.alpha(0.45f) else this
