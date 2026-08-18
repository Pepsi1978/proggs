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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/**
 * **B-02 — die Sitzungs-Schublade.**
 *
 * Sie fährt von links herein (M-02) und ist auf beiden Displays des Fold 8 eine Schublade:
 * 297 dp zugeklappt und 440 dp aufgeklappt sind beide zu schmal, um sie dauerhaft neben dem
 * Verlauf stehen zu lassen — der Text bliebe dann auf Briefmarkenbreite.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun Schublade(
    sitzungen: List<Sitzung>,
    offeneSitzung: Long?,
    breit: Boolean,
    beiWahl: (Long) -> Unit,
    beiNeue: () -> Unit,
    beiMenue: (Sitzung) -> Unit,
    beiEinstellungen: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

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

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(sitzungen, key = { it.id }) { sitzung ->
                Sitzungszeile(
                    sitzung = sitzung,
                    offen = sitzung.id == offeneSitzung,
                    beiWahl = { beiWahl(sitzung.id) },
                    beiMenue = { beiMenue(sitzung) },
                )
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

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
private fun Sitzungszeile(
    sitzung: Sitzung,
    offen: Boolean,
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
            .background(if (offen) farben.akzentGedeckt else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(onClick = beiWahl, onLongClick = beiMenue),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(Masse.sitzungszeile - 16.dp)
                .background(
                    if (offen) farben.akzent else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                sitzung.titel,
                style = schrift.sitzungsname,
                color = if (offen) farben.textStark else farben.textMittel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                de.frank.gedankenspeicher.data.Repository.zeitpunkt(sitzung.zuletztGeoeffnet),
                style = schrift.zeitstempel,
                color = farben.textSchwach,
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
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.52f * staerke))
            .clickable(
                interactionSource = androidx.compose.runtime.remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = beiDruck,
            ),
    )
}
