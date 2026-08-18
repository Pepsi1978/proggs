package de.frank.gedankenspeicher.ui.einstellungen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.Auswertungsprofil
import de.frank.gedankenspeicher.ui.ki.GefuellterKnopf
import de.frank.gedankenspeicher.ui.ki.GeranderterKnopf
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.blattgrund
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.dauer

/**
 * **B-06 — die sechs Auswertungsprofile.**
 *
 * Die Zahl sechs ist fest: keines lässt sich hinzufügen, keines löschen. Genau eines trägt
 * das Häkchen — es ist zu keinem Zeitpunkt möglich, zwei zu setzen oder gar keines zu haben.
 */
@Composable
fun ProfileBildschirm(
    profile: List<Auswertungsprofil>,
    beiAktivieren: (Auswertungsprofil) -> Unit,
    beiBearbeiten: (Auswertungsprofil) -> Unit,
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
            Text("Auswertungsprofile", style = schrift.bildschirmtitel, color = farben.textStark)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Masse.seitenrand),
            verticalArrangement = Arrangement.spacedBy(Masse.kartenAbstand),
        ) {
            items(profile, key = { it.nummer }) { profil ->
                Profilzeile(
                    profil = profil,
                    beiAktivieren = { beiAktivieren(profil) },
                    beiBearbeiten = { beiBearbeiten(profil) },
                )
            }
        }
    }
}

@Composable
private fun Profilzeile(
    profil: Auswertungsprofil,
    beiAktivieren: () -> Unit,
    beiBearbeiten: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    val leer = profil.anweisung.isBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Masse.profilRadius))
            // Aktiv ist an Häkchen **und** Rand **und** Fläche zu erkennen (§9).
            .background(if (profil.istAktiv) farben.akzentGedeckt else farben.hintergrundErhoben)
            .border(
                if (profil.istAktiv) 1.5.dp else 1.dp,
                if (profil.istAktiv) farben.akzent else farben.rand,
                RoundedCornerShape(Masse.profilRadius),
            )
            .clickable(onClick = beiBearbeiten)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Haekchenfeld(
            gesetzt = profil.istAktiv,
            gesperrt = leer,
            beiDruck = beiAktivieren,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profil.name,
                style = schrift.kartenUeberschrift,
                color = if (leer) farben.textSchwach else farben.textStark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (leer) "Noch kein Text — antippen zum Ausfüllen" else profil.anweisung,
                style = schrift.einstellungErklaerung,
                color = farben.textSchwach,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = beiBearbeiten) {
            Icon(Icons.Outlined.Edit, "Profil bearbeiten", Modifier.size(20.dp), tint = farben.textMittel)
        }
    }
}

/**
 * **M-12 — das Häkchen springt um.**
 *
 * Das neue erscheint mit einem leichten Überschwingen (1,12 und zurück): so ist zu sehen,
 * dass es gesprungen *ist* und nicht schon immer dort stand.
 */
@Composable
private fun Haekchenfeld(gesetzt: Boolean, gesperrt: Boolean, beiDruck: () -> Unit) {
    val farben = Farben
    val groesse by animateFloatAsState(
        targetValue = if (gesetzt) 1f else 0f,
        animationSpec = tween(
            durationMillis = dauer(if (gesetzt) Dauern.STANDARD else Dauern.KURZ),
            easing = if (gesetzt) Kurven.ueberschwingen else Kurven.kurz,
        ),
        label = "haekchen",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(50))
            .background(if (gesetzt) farben.akzent else Color.Transparent)
            .border(
                1.5.dp,
                when {
                    gesperrt -> farben.textSchwach.copy(alpha = 0.4f)
                    gesetzt -> farben.akzent
                    else -> farben.rand
                },
                RoundedCornerShape(50),
            )
            .clickable(enabled = !gesperrt, onClick = beiDruck),
        contentAlignment = Alignment.Center,
    ) {
        if (groesse > 0f) {
            Icon(
                Icons.Outlined.Check,
                "aktiv",
                Modifier.size(15.dp).scale(groesse),
                tint = farben.hintergrund,
            )
        }
    }
}

/**
 * Der Editor als Blatt (`02-UI-SPEC.md` B-06).
 *
 * „Zurücksetzen" stellt den Auslieferungstext wieder her — bei den drei eigenen Profilen
 * leert es sie, denn ihr Auslieferungszustand *ist* leer.
 */
@Composable
fun ProfilEditor(
    name: String,
    anweisung: String,
    beiName: (String) -> Unit,
    beiAnweisung: (String) -> Unit,
    beiZuruecksetzen: () -> Unit,
    beiSpeichern: () -> Unit,
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
            .padding(Masse.seitenrand),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(32.dp).height(4.dp).background(farben.rand, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(16.dp))

        Text("Name", style = schrift.einstellung, color = farben.textMittel)
        Spacer(Modifier.height(6.dp))
        Feld(wert = name, beiAenderung = beiName, einzeilig = true, platzhalter = "Name des Profils")

        Spacer(Modifier.height(14.dp))
        Text("Anweisung an die KI", style = schrift.einstellung, color = farben.textMittel)
        Spacer(Modifier.height(6.dp))
        Feld(
            wert = anweisung,
            beiAenderung = beiAnweisung,
            einzeilig = false,
            platzhalter = "Wie soll die Antwort ausfallen?",
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GeranderterKnopf("Zurücksetzen", beiZuruecksetzen, farbe = farben.textMittel)
            Box(Modifier.weight(1f)) { GefuellterKnopf("Speichern", beiSpeichern) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Feld(wert: String, beiAenderung: (String) -> Unit, einzeilig: Boolean, platzhalter: String) {
    val farben = Farben
    val schrift = Schriften
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (einzeilig) 48.dp else 120.dp)
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
