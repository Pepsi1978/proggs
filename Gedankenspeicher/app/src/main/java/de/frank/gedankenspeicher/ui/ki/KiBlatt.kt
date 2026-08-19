package de.frank.gedankenspeicher.ui.ki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.auth.CodexModel
import de.frank.gedankenspeicher.auth.ReasoningEffort
import de.frank.gedankenspeicher.ui.KiBlattzustand
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.blattgrund
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.merkeDruck
import de.frank.gedankenspeicher.ui.theme.sinktEin
import de.frank.gedankenspeicher.ui.verlauf.WanderndePunkte

/**
 * **B-03 — das KI-Blatt.**
 *
 * Hier steht der Kern der App: die Rückfrage, die die KI zu den gesammelten Notizen stellt,
 * und Franks Antwort darauf. Die Rückfrage ist deshalb der optisch stärkste Text des Blattes
 * — sie ist der Grund, warum es dieses Blatt überhaupt gibt.
 */
@Composable
fun KiBlatt(
    zustand: KiBlattzustand,
    nimmtAntwortAuf: Boolean,
    codexModell: String,
    codexEffort: String,
    beiWebsuche: (Boolean) -> Unit,
    beiWebsucheKi: () -> Unit,
    beiAntwort: (String) -> Unit,
    beiAntwortEinsprechen: () -> Unit,
    beiAuswertungEinstellungen: () -> Unit,
    beiProfil: () -> Unit,
    beiAuswerten: () -> Unit,
    beiVerbinden: () -> Unit,
    beiErneut: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                farben.blattgrund,
                RoundedCornerShape(topStart = Masse.blattRadius, topEnd = Masse.blattRadius),
            )
            .border(
                1.dp,
                farben.rand,
                RoundedCornerShape(topStart = Masse.blattRadius, topEnd = Masse.blattRadius),
            )
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = Masse.seitenrand)
            .verticalScroll(rememberScrollState()),
    ) {
        // Ziehgriff
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(32.dp).height(4.dp).background(farben.rand, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(14.dp))

        Text("Auswertung", style = schrift.bildschirmtitel, color = farben.textStark)
        Spacer(Modifier.height(4.dp))
        Text(
            "${zustand.kontextzahl} ${if (zustand.kontextzahl == 1) "Eintrag" else "Einträge"} aus der ganzen Sitzung",
            style = schrift.einstellungErklaerung,
            color = farben.textSchwach,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiAuswertungEinstellungen)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${CodexModel.fromLabel(codexModell).label} · Effort ${ReasoningEffort.fromLabel(codexEffort).label}",
                style = schrift.einstellung,
                color = farben.textMittel,
                modifier = Modifier.weight(1f),
            )
            Text("ändern", style = schrift.zeitstempel, color = farben.akzent)
        }

        // Steht die Grundhaltung auf „KI entscheidet", zeigt die Zeile drei Wahlfelder statt
        // eines Schalters (`02-UI-SPEC.md` B-03, Punkt 3).
        if (zustand.websucheKiEntscheidet) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Websuche", style = schrift.einstellung, color = farben.textMittel, modifier = Modifier.weight(1f))
                Wahlfeld("aus", gewaehlt = false) { beiWebsuche(false) }
                Spacer(Modifier.width(6.dp))
                Wahlfeld("immer", gewaehlt = false) { beiWebsuche(true) }
                Spacer(Modifier.width(6.dp))
                Wahlfeld("KI entscheidet", gewaehlt = true) { beiWebsucheKi() }
            }
        } else {
            Schalterzeile(beschriftung = "Websuche", an = zustand.websuche, beiAenderung = beiWebsuche)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiProfil)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Profil: ${zustand.profil?.name ?: "—"}",
                style = schrift.einstellung,
                color = farben.textMittel,
                modifier = Modifier.weight(1f),
            )
            Text("ändern", style = schrift.zeitstempel, color = farben.akzent)
        }

        Spacer(Modifier.height(12.dp))

        // ---- Die Rückfrage: der wichtigste Text des Blattes.
        when {
            zustand.codexFehlt -> Column {
                Text("Codex ist nicht verbunden.", style = schrift.notiztext, color = farben.fehler)
                TextButton(onClick = beiVerbinden) {
                    Text("Jetzt verbinden", style = schrift.knopf, color = farben.akzent)
                }
            }

            zustand.holtFrage -> Column {
                WanderndePunkte(farben.akzent)
                Spacer(Modifier.height(6.dp))
                Text("Die KI liest deine Notizen …", style = schrift.zeitstempel, color = farben.textSchwach)
            }

            zustand.fehler != null -> Column {
                Text(zustand.fehler, style = schrift.notiztext, color = farben.fehler)
                TextButton(onClick = beiErneut) {
                    Text("Nochmal versuchen", style = schrift.knopf, color = farben.akzent)
                }
            }

            zustand.rueckfrage.isNotBlank() -> Text(
                text = zustand.rueckfrage,
                style = schrift.notiztext.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight(500),
                ),
                color = farben.textStark,
            )
        }

        if (zustand.rueckfrage.isNotBlank() && !zustand.codexFehlt) {
            Spacer(Modifier.height(14.dp))

            // ---- Antwortfeld mit Mikrofonknopf darin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.eingabeRadius))
                    .border(1.dp, farben.rand, RoundedCornerShape(Masse.eingabeRadius))
                    .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(Modifier.weight(1f)) {
                    if (zustand.antwort.isEmpty()) {
                        Text("Deine Antwort …", style = schrift.eingabefeld, color = farben.textSchwach)
                    }
                    BasicTextField(
                        value = zustand.antwort,
                        onValueChange = beiAntwort,
                        textStyle = schrift.eingabefeld.copy(color = farben.textStark),
                        cursorBrush = SolidColor(farben.akzent),
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(Masse.tippflaeche)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = beiAntwortEinsprechen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (nimmtAntwortAuf) Icons.Filled.Stop else Icons.Outlined.Mic,
                        if (nimmtAntwortAuf) "Aufnahme beenden" else "Antwort einsprechen",
                        Modifier.size(22.dp),
                        tint = if (nimmtAntwortAuf) farben.akzent else farben.textMittel,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            GefuellterKnopf(beschriftung = "Auswerten", beiDruck = beiAuswerten)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Schalterzeile(beschriftung: String, an: Boolean, beiAenderung: (Boolean) -> Unit) {
    val farben = Farben
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(beschriftung, style = Schriften.einstellung, color = farben.textMittel, modifier = Modifier.weight(1f))
        Switch(
            checked = an,
            onCheckedChange = beiAenderung,
            colors = SwitchDefaults.colors(
                checkedThumbColor = farben.hintergrund,
                checkedTrackColor = farben.akzent,
                uncheckedThumbColor = farben.textSchwach,
                uncheckedTrackColor = farben.hintergrundErhoben,
                uncheckedBorderColor = farben.rand,
            ),
        )
    }
}

/** Ein Wahlfeld — auch hier trägt der Akzent nie allein die Bedeutung: Rand **und** Fläche. */
@Composable
fun Wahlfeld(beschriftung: String, gewaehlt: Boolean, beiDruck: () -> Unit) {
    val farben = Farben
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (gewaehlt) farben.akzentGedeckt else Color.Transparent)
            .border(if (gewaehlt) 1.5.dp else 1.dp, if (gewaehlt) farben.akzent else farben.rand, RoundedCornerShape(50))
            .clickable(onClick = beiDruck)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            beschriftung,
            style = Schriften.zeitstempel,
            color = if (gewaehlt) farben.akzent else farben.textMittel,
        )
    }
}

/** Ein gefüllter Knopf über die volle Breite (`02-UI-SPEC.md` B-03, Punkt 7). */
@Composable
fun GefuellterKnopf(beschriftung: String, beiDruck: () -> Unit, aktiv: Boolean = true) {
    val farben = Farben
    val druck = merkeDruck()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .sinktEin(druck, auf = 0.97f)
            .clip(RoundedCornerShape(Masse.eingabeRadius))
            .background(if (aktiv) farben.akzent else farben.hintergrundErhoben)
            .clickable(interactionSource = druck, indication = null, enabled = aktiv, onClick = beiDruck)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            beschriftung,
            style = Schriften.knopf,
            color = if (aktiv) farben.hintergrund else farben.textSchwach,
        )
    }
}

/** Ein nur gerandeter Knopf — für die zweite Wahl neben einem gefüllten. */
@Composable
fun GeranderterKnopf(
    beschriftung: String,
    beiDruck: () -> Unit,
    farbe: Color? = null,
    modifier: Modifier = Modifier,
) {
    val farben = Farben
    val druck = merkeDruck()
    val ton = farbe ?: farben.akzent
    Box(
        modifier = modifier
            .sinktEin(druck, auf = 0.97f)
            .clip(RoundedCornerShape(Masse.eingabeRadius))
            .border(1.dp, ton, RoundedCornerShape(Masse.eingabeRadius))
            .clickable(interactionSource = druck, indication = null, onClick = beiDruck)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(beschriftung, style = Schriften.knopf, color = ton)
    }
}
