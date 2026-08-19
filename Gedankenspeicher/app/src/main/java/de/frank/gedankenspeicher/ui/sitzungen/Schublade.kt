package de.frank.gedankenspeicher.ui.sitzungen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Ordner
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.ui.Schubladenansicht
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/** Das Gelb des Favoritensterns — in allen vier Erscheinungen gleich gut lesbar. */
val Favoritengold = Color(0xFFF5B72A)

/**
 * **B-02 — die Sitzungs-Schublade.**
 *
 * Sie fährt von links herein (M-02) und ist auf beiden Displays des Fold 8 eine Schublade:
 * 297 dp zugeklappt und 440 dp aufgeklappt sind beide zu schmal, um sie dauerhaft neben dem
 * Verlauf stehen zu lassen — der Text bliebe dann auf Briefmarkenbreite.
 *
 * Sie ist zweigeteilt: oben die Reiter (Alle Notizen, Favoriten, Geschützte Notizen,
 * Papierkorb, Ordner) — das ist die Auswahl. Unter dem Strich die Notizen der gewählten
 * Ansicht, die neueste zuoberst — das ist der Verlauf.
 *
 * **Was der Schutz verbirgt, ist der Inhalt, nicht der Name.** Eine geschützte Notiz steht
 * mit ihrem Schloss ganz normal unter „Alle Notizen", in ihrem Ordner und, wenn sie eine ist,
 * unter „Favoriten" — nur öffnen lässt sie sich erst nach dem Fingerabdruck. Ein Reiter,
 * der die Notiz ganz verschwinden lässt, wäre kein Schutz, sondern nur ein Versteck.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun Schublade(
    sitzungen: List<Sitzung>,
    ordner: List<Ordner>,
    ansicht: Schubladenansicht,
    gewaehlterOrdner: Long?,
    freigegebeneSitzung: Long?,
    offeneSitzung: Long?,
    breit: Boolean,
    beiWahl: (Sitzung) -> Unit,
    beiNeue: () -> Unit,
    beiMenue: (Sitzung) -> Unit,
    beiAnsicht: (Schubladenansicht) -> Unit,
    beiOrdnerwahl: (Long) -> Unit,
    beiOrdnerVerwalten: () -> Unit,
    beiPapierkorbLeeren: () -> Unit,
    beiEinstellungen: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    var ordnerOffen by rememberSaveable { mutableStateOf(false) }

    // „Alle Notizen" heißt alle: geschützte sind mitgezählt und mit aufgelistet, nur ihr
    // Inhalt bleibt bis zum Fingerabdruck zu. Draußen ist einzig, was im Papierkorb liegt.
    val alle = sitzungen.filter { it.geloeschtAm == null }
    val favoriten = alle.filter { it.favorit }
    val geschuetzte = alle.filter { it.geschuetzt }
    val papierkorb = sitzungen.filter { it.geloeschtAm != null }
    val offenerOrdner = ordner.firstOrNull { it.id == gewaehlterOrdner }

    val liste = when (ansicht) {
        Schubladenansicht.ALLE -> alle
        Schubladenansicht.FAVORITEN -> favoriten
        Schubladenansicht.GESCHUETZT -> geschuetzte
        Schubladenansicht.PAPIERKORB -> papierkorb
        Schubladenansicht.ORDNER -> alle.filter { it.ordnerId == offenerOrdner?.id }
    }

    val ueberschrift = when (ansicht) {
        Schubladenansicht.ALLE -> "Alle Notizen"
        Schubladenansicht.FAVORITEN -> "Favoriten"
        Schubladenansicht.GESCHUETZT -> "Geschützte Notizen"
        Schubladenansicht.PAPIERKORB -> "Papierkorb"
        Schubladenansicht.ORDNER -> offenerOrdner?.name ?: "Ordner"
    }

    Column(
        modifier = Modifier
            .width(if (breit) Masse.schubladeBreit else Masse.schubladeSchmal)
            .fillMaxHeight()
            .background(
                farben.hintergrund,
                RoundedCornerShape(topEnd = Masse.schubladeRadius, bottomEnd = Masse.schubladeRadius),
            )
            .border(
                1.dp,
                farben.rand,
                RoundedCornerShape(topEnd = Masse.schubladeRadius, bottomEnd = Masse.schubladeRadius),
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Gedankenspeicher", style = schrift.bildschirmtitel, color = farben.textStark)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Masse.profilRadius))
                .border(1.dp, farben.akzent, RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiNeue)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, Modifier.size(20.dp), tint = farben.akzent)
            Spacer(Modifier.width(8.dp))
            Text("Neue Sitzung", style = schrift.knopf, color = farben.akzent)
        }

        Spacer(Modifier.height(12.dp))

        // ---- Die Auswahl: Reiter
        Reiter(Icons.Outlined.Notes, "Alle Notizen", alle.size, ansicht == Schubladenansicht.ALLE) {
            beiAnsicht(Schubladenansicht.ALLE)
        }
        Reiter(
            symbol = Icons.Outlined.Star,
            beschriftung = "Favoriten",
            anzahl = favoriten.size,
            gewaehlt = ansicht == Schubladenansicht.FAVORITEN,
            symbolfarbe = Favoritengold,
        ) { beiAnsicht(Schubladenansicht.FAVORITEN) }
        Reiter(Icons.Outlined.Lock, "Geschützte Notizen", geschuetzte.size, ansicht == Schubladenansicht.GESCHUETZT) {
            beiAnsicht(Schubladenansicht.GESCHUETZT)
        }
        Reiter(Icons.Outlined.Delete, "Papierkorb", papierkorb.size, ansicht == Schubladenansicht.PAPIERKORB) {
            beiAnsicht(Schubladenansicht.PAPIERKORB)
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
        Spacer(Modifier.height(8.dp))

        // ---- Ordner: aufklappbar, „Ordner verwalten" steht darin an erster Stelle
        Reiter(
            symbol = Icons.Outlined.Folder,
            beschriftung = "Ordner",
            anzahl = ordner.size,
            gewaehlt = ansicht == Schubladenansicht.ORDNER,
            nachlauf = {
                Icon(
                    if (ordnerOffen) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                    null,
                    Modifier.size(18.dp),
                    tint = farben.textSchwach,
                )
            },
        ) { ordnerOffen = !ordnerOffen }

        if (ordnerOffen) {
            Column(Modifier.padding(start = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Masse.profilRadius))
                        .clickable(onClick = beiOrdnerVerwalten)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, null, Modifier.size(18.dp), tint = farben.akzent)
                    Spacer(Modifier.width(10.dp))
                    Text("Ordner verwalten", style = schrift.sitzungsname, color = farben.akzent)
                }
                if (ordner.isEmpty()) {
                    Text(
                        "Noch kein Ordner angelegt.",
                        style = schrift.zeitstempel,
                        color = farben.textSchwach,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                ordner.forEach { einer ->
                    Reiter(
                        symbol = Icons.Outlined.FolderOpen,
                        beschriftung = einer.name,
                        anzahl = alle.count { it.ordnerId == einer.id },
                        gewaehlt = ansicht == Schubladenansicht.ORDNER && offenerOrdner?.id == einer.id,
                    ) { beiOrdnerwahl(einer.id) }
                }
            }
        }

        // ---- Der Strich: darunter beginnt der Verlauf
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(farben.rand))
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                ueberschrift,
                style = schrift.kartenUeberschrift,
                color = farben.textSchwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ansicht == Schubladenansicht.PAPIERKORB && papierkorb.isNotEmpty()) {
                Text(
                    "Leeren",
                    style = schrift.zeitstempel,
                    color = farben.fehler,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = beiPapierkorbLeeren)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(liste, key = { it.id }) { sitzung ->
                Sitzungszeile(
                    sitzung = sitzung,
                    offen = sitzung.id == offeneSitzung,
                    ordnername = ordner.firstOrNull { it.id == sitzung.ordnerId }?.name,
                    zugesperrt = sitzung.geschuetzt && sitzung.id != freigegebeneSitzung,
                    // Aus dem Papierkorb wird nichts geöffnet — dort führt nur der lange
                    // Druck weiter, zum Wiederherstellen oder endgültigen Löschen.
                    beiWahl = { if (ansicht != Schubladenansicht.PAPIERKORB) beiWahl(sitzung) },
                    beiMenue = { beiMenue(sitzung) },
                )
            }
            if (liste.isEmpty()) {
                item {
                    Text(
                        "Hier liegt noch keine Notiz.",
                        style = schrift.zeitstempel,
                        color = farben.textSchwach,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiEinstellungen)
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp), tint = farben.textMittel)
            Spacer(Modifier.width(10.dp))
            Text("Einstellungen", style = schrift.sitzungsname, color = farben.textMittel)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Ein Reiter der Auswahl — Symbol, Beschriftung und die Anzahl dahinter. */
@Composable
private fun Reiter(
    symbol: ImageVector,
    beschriftung: String,
    anzahl: Int,
    gewaehlt: Boolean,
    symbolfarbe: Color? = null,
    nachlauf: (@Composable () -> Unit)? = null,
    beiDruck: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Masse.profilRadius))
            .background(if (gewaehlt) farben.akzentGedeckt else Color.Transparent)
            .clickable(onClick = beiDruck)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            symbol,
            null,
            Modifier.size(20.dp),
            tint = symbolfarbe ?: if (gewaehlt) farben.akzent else farben.textMittel,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            beschriftung,
            style = schrift.sitzungsname,
            fontWeight = if (gewaehlt) FontWeight.SemiBold else FontWeight.Normal,
            color = if (gewaehlt) farben.textStark else farben.textMittel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (anzahl > 0) {
            Text(anzahl.toString(), style = schrift.zeitstempel, color = farben.textSchwach)
            Spacer(Modifier.width(6.dp))
        }
        nachlauf?.invoke()
    }
}

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
private fun Sitzungszeile(
    sitzung: Sitzung,
    offen: Boolean,
    ordnername: String?,
    zugesperrt: Boolean,
    beiWahl: () -> Unit,
    beiMenue: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Masse.sitzungszeile)
            .clip(RoundedCornerShape(Masse.profilRadius))
            // Die offene Sitzung ist an **zwei** Dingen zu erkennen — Balken und Fläche.
            // Der Akzent trägt nie allein eine Bedeutung (`02-UI-SPEC.md` §9).
            .background(if (offen) farben.akzentGedeckt else Color.Transparent)
            .combinedClickable(onClick = beiWahl, onLongClick = beiMenue),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(Masse.sitzungszeile - 16.dp)
                .background(
                    if (offen) farben.akzent else Color.Transparent,
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sitzung.favorit) {
                    Icon(Icons.Outlined.Star, "Favorit", Modifier.size(14.dp), tint = Favoritengold)
                    Spacer(Modifier.width(4.dp))
                }
                if (sitzung.geschuetzt) {
                    Icon(
                        if (zugesperrt) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        if (zugesperrt) "Geschützt" else "Geschützt und freigegeben",
                        Modifier.size(14.dp),
                        tint = farben.textMittel,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    sitzung.titel,
                    style = schrift.sitzungsname,
                    color = if (offen) farben.textStark else farben.textMittel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                listOfNotNull(
                    de.frank.gedankenspeicher.data.Repository.zeitpunkt(sitzung.zuletztGeoeffnet),
                    ordnername,
                ).joinToString(" · "),
                style = schrift.zeitstempel,
                color = farben.textSchwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

/** Die abgedunkelte Fläche über B-01, solange die Schublade offen ist (M-02). */
@Composable
fun Abdunklung(staerke: Float, beiDruck: () -> Unit) {
    if (staerke <= 0f) return
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f * staerke))
            .clickable(
                interactionSource = androidx.compose.runtime.remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = beiDruck,
            ),
    )
}

/**
 * Die Sperrschicht über dem Verlauf einer geschützten Sitzung.
 *
 * Sie deckt den Inhalt zu, statt die Sitzung zu schliessen: wer den Fingerabdruck gibt,
 * steht danach genau da, wo er hinwollte. Der zweite Knopf führt in die Schublade zurück,
 * damit man aus einer zugesperrten Sitzung auch ohne Fingerabdruck wieder herauskommt.
 */
@Composable
fun Sperrschicht(titel: String, beiOeffnen: () -> Unit, beiUebersicht: () -> Unit) {
    val farben = Farben
    val schrift = Schriften
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(farben.hintergrund)
            .clickable(
                interactionSource = androidx.compose.runtime.remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = {},
            )
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Lock, null, Modifier.size(40.dp), tint = farben.akzent)
        Spacer(Modifier.height(16.dp))
        Text(
            titel,
            style = schrift.bildschirmtitel,
            color = farben.textStark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Diese Notiz ist geschützt. Ihr Inhalt erscheint erst nach dem Fingerabdruck.",
            style = schrift.zeitstempel,
            color = farben.textSchwach,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Masse.profilRadius))
                .border(1.dp, farben.akzent, RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiOeffnen)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LockOpen, null, Modifier.size(20.dp), tint = farben.akzent)
            Spacer(Modifier.width(10.dp))
            Text("Mit Fingerabdruck öffnen", style = schrift.knopf, color = farben.akzent)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Zu den Notizen",
            style = schrift.sitzungsname,
            color = farben.textMittel,
            modifier = Modifier
                .clip(RoundedCornerShape(Masse.profilRadius))
                .clickable(onClick = beiUebersicht)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
