package de.frank.gedankenspeicher.ui.verlauf

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.ui.Bearbeitungszustand
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.blattgrund
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/**
 * **B-08 — eine Notiz bearbeiten.**
 *
 * Sprecherkennung liegt manchmal daneben, und eine KI-Überschrift trifft nicht immer. Hier
 * lassen sich beide richtigstellen. Der Zeitstempel bleibt unangetastet: er sagt, wann der
 * Gedanke da war, nicht wann er zuletzt getippt wurde.
 */
@Composable
fun BearbeitenBlatt(
    zustand: Bearbeitungszustand,
    beiAenderung: (String, String) -> Unit,
    beiAbbrechen: () -> Unit,
    beiSpeichern: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    val notiz = zustand.notiz ?: return

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
            .padding(horizontal = Masse.seitenrand),
    ) {
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(32.dp).height(4.dp).background(farben.rand, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Abbrechen",
                style = schrift.knopf,
                color = farben.textMittel,
                modifier = Modifier.clickable(onClick = beiAbbrechen).padding(vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Speichern",
                style = schrift.knopf,
                // Ist nichts geändert, ist Speichern ausgegraut und wirkungslos.
                color = if (zustand.geaendert) farben.akzent else farben.textSchwach,
                modifier = Modifier
                    .clickable(enabled = zustand.geaendert, onClick = beiSpeichern)
                    .padding(vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Feld(
            wert = zustand.ueberschrift,
            platzhalter = "Überschrift",
            einzeilig = true,
            beiAenderung = { beiAenderung(it, zustand.text) },
        )
        Spacer(Modifier.height(10.dp))
        Feld(
            wert = zustand.text,
            platzhalter = "Text der Notiz",
            einzeilig = false,
            beiAenderung = { beiAenderung(zustand.ueberschrift, it) },
        )

        Spacer(Modifier.height(10.dp))
        Text(
            Repository.zeitpunkt(notiz.erstelltAm),
            style = schrift.zeitstempel,
            color = farben.textSchwach,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Feld(wert: String, platzhalter: String, einzeilig: Boolean, beiAenderung: (String) -> Unit) {
    val farben = Farben
    val schrift = Schriften
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (einzeilig) 48.dp else 160.dp)
            .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.profilRadius))
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.profilRadius))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (wert.isEmpty()) {
            Text(platzhalter, style = schrift.eingabefeld, color = farben.textSchwach)
        }
        BasicTextField(
            value = wert,
            onValueChange = beiAenderung,
            textStyle = schrift.eingabefeld.copy(color = farben.textStark),
            cursorBrush = SolidColor(farben.akzent),
            singleLine = einzeilig,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Ein Eintrag im Menü, das der lange Druck auf eine Karte öffnet (F-08). */
@Composable
fun Menueeintrag(text: String, gesperrt: Boolean = false, gefaehrlich: Boolean = false, beiDruck: () -> Unit) {
    val farben = Farben
    Text(
        text = text,
        style = Schriften.einstellung,
        color = when {
            gesperrt -> farben.textSchwach.copy(alpha = 0.5f)
            gefaehrlich -> farben.fehler
            else -> farben.textStark
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !gesperrt, onClick = beiDruck)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

/** Ein Blatt mit Einträgen — das Menü zum langen Druck. */
@Composable
fun MenueBlatt(titel: String?, inhalt: @Composable () -> Unit) {
    val farben = Farben
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                farben.blattgrund,
                RoundedCornerShape(topStart = Masse.blattRadius, topEnd = Masse.blattRadius),
            )
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(32.dp).height(4.dp).background(farben.rand, RoundedCornerShape(2.dp)))
        }
        if (titel != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                titel,
                style = Schriften.kartenUeberschrift,
                color = farben.textSchwach,
                modifier = Modifier.padding(horizontal = 20.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        inhalt()
        Spacer(Modifier.height(8.dp))
    }
}
