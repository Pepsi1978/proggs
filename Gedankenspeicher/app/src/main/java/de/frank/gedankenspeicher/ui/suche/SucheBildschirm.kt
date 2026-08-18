package de.frank.gedankenspeicher.ui.suche

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.data.Suchtreffer
import de.frank.gedankenspeicher.ui.Suchzustand
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/**
 * **B-07 — die Suche über alle Sitzungen.**
 *
 * Ohne sie wächst die App sich selbst zu: wer über Wochen sammelt, findet den Gedanken von
 * vorletztem Dienstag durch Scrollen nicht wieder.
 */
@Composable
fun SucheBildschirm(
    zustand: Suchzustand,
    beiBegriff: (String) -> Unit,
    beiTreffer: (Suchtreffer) -> Unit,
    beiZurueck: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    val fokus = remember { FocusRequester() }

    // Das Suchfeld bekommt den Fokus sofort — sonst kostet jede Suche einen Extra-Tipp.
    LaunchedEffect(Unit) { runCatching { fokus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(farben.hintergrund)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = beiZurueck) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = farben.textMittel)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.eingabeRadius))
                    .border(1.dp, farben.rand, RoundedCornerShape(Masse.eingabeRadius))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (zustand.begriff.isEmpty()) {
                    Text("Suche in allen Notizen", style = schrift.eingabefeld, color = farben.textSchwach)
                }
                BasicTextField(
                    value = zustand.begriff,
                    onValueChange = beiBegriff,
                    textStyle = schrift.eingabefeld.copy(color = farben.textStark),
                    cursorBrush = SolidColor(farben.akzent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(fokus),
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        if (zustand.sucht) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = farben.akzent,
                trackColor = farben.rand,
            )
        }

        when {
            zustand.begriff.trim().length < 2 -> Mittig("Suche in allen Notizen")
            zustand.treffer.isEmpty() && !zustand.sucht -> Mittig("Nichts gefunden zu „${zustand.begriff.trim()}“")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Masse.seitenrand),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Nach Sitzung gruppiert, damit man sieht, aus welchem Zusammenhang ein
                // Treffer stammt (`02-UI-SPEC.md` B-07).
                zustand.treffer.groupBy { it.sitzungstitel }.forEach { (titel, treffer) ->
                    item(key = "kopf-$titel") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(titel, style = schrift.kartenUeberschrift, color = farben.akzent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${treffer.size} ${if (treffer.size == 1) "Treffer" else "Treffer"}",
                                style = schrift.zeitstempel,
                                color = farben.textSchwach,
                            )
                        }
                    }
                    items(treffer, key = { "${it.istKiAntwort}-${it.notizId}" }) { t ->
                        Trefferzeile(t, zustand.begriff.trim()) { beiTreffer(t) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Trefferzeile(treffer: Suchtreffer, begriff: String, beiDruck: () -> Unit) {
    val farben = Farben
    val schrift = Schriften
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = beiDruck)
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (treffer.istKiAntwort) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    Modifier.width(16.dp).height(16.dp),
                    tint = farben.akzent,
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                treffer.ueberschrift?.takeIf { it.isNotBlank() } ?: Repository.uhrzeit(treffer.erstelltAm),
                style = schrift.kartenUeberschrift,
                color = farben.textMittel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(Repository.zeitpunkt(treffer.erstelltAm), style = schrift.zeitstempel, color = farben.textSchwach)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = ausschnittMitHervorhebung(treffer.text, begriff, farben.akzentGedeckt, farben.akzent),
            style = schrift.einstellungErklaerung,
            color = farben.textSchwach,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Der Ausschnitt um die Fundstelle, mit unterlegtem Suchwort.
 *
 * Nicht der Anfang des Textes: bei einer langen Notiz steht die Fundstelle oft in der Mitte,
 * und ein Ausschnitt vom Anfang zeigt dann alles außer dem, was man gesucht hat.
 */
private fun ausschnittMitHervorhebung(
    text: String,
    begriff: String,
    unterlegung: androidx.compose.ui.graphics.Color,
    schriftfarbe: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    if (begriff.isBlank()) return AnnotatedString(text.take(160))
    val stelle = text.indexOf(begriff, ignoreCase = true)
    if (stelle < 0) return AnnotatedString(text.take(160))
    val von = (stelle - 60).coerceAtLeast(0)
    val bis = (stelle + begriff.length + 100).coerceAtMost(text.length)
    val ausschnitt = (if (von > 0) "…" else "") + text.substring(von, bis) + (if (bis < text.length) "…" else "")
    val neueStelle = ausschnitt.indexOf(begriff, ignoreCase = true)
    return buildAnnotatedString {
        if (neueStelle < 0) {
            append(ausschnitt)
            return@buildAnnotatedString
        }
        append(ausschnitt.substring(0, neueStelle))
        withStyle(SpanStyle(background = unterlegung, color = schriftfarbe)) {
            append(ausschnitt.substring(neueStelle, neueStelle + begriff.length))
        }
        append(ausschnitt.substring(neueStelle + begriff.length))
    }
}

@Composable
private fun Mittig(text: String) {
    val farben = Farben
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = Schriften.notiztext,
            color = farben.textSchwach,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
