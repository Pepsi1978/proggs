@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.LocalSemantisch
import de.frank.wecker.design.DesignDialog

private fun preisText(preis: String?) = preis ?: "den in Google Play angezeigten Preis"

/** Eine gemeinsame, ehrliche Beschreibung für Paywall und Einstellungen. */
@Composable
private fun Angebot(zustand: FreischaltungsZustand) {
    Text("${Testzeitraum.TAGE} Tage alle Funktionen, danach einmalig ${preisText(zustand.preis)} für lebenslange Nutzung – kein Abo.",
        style = MaterialTheme.typography.bodyMedium)
    Text("Bereits gestellte Wecker klingeln immer weiter, auch ohne Kauf. Ausschalten, Löschen, Schlummern und Stoppen bleiben jederzeit möglich.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    if (zustand.preis == null) Text(if (zustand.playBereit) "Der Preis wird von Google Play geladen …"
        else "Der Preis kommt von Google Play und wird angezeigt, sobald Play erreichbar ist.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    Text("Internet brauchst du nur für den Kauf über Google Play. Die Stimmen sprechen und geweckt wird immer offline.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    if (zustand.hinweis.isNotBlank()) Text(zustand.hinweis, style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
}

@Composable
private fun Aktionen(zustand: FreischaltungsZustand, activity: Activity) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldKnopf(if (zustand.preis != null) "Für ${zustand.preis} freischalten" else "Freischalten", { Freischaltung.kaufen(activity) },
            aktiviert = !zustand.gekauft && !zustand.ausstehend, hauptKnopf = true)
        StillerKnopf("Kauf wiederherstellen", { Freischaltung.wiederherstellen() })
    }
}

/** Erscheint, wenn nach dem Test ein Wecker angelegt, bearbeitet oder eingeschaltet werden soll. */
@Composable
fun FreischaltungsDialog(activity: Activity, schliessen: () -> Unit) {
    val zustand by Freischaltung.zustand.collectAsStateWithLifecycle()
    // Freigeschaltet (z. B. Kauf gerade abgeschlossen): Dialog schließt sich, ohne im Zeichnen etwas auszulösen.
    if (zustand.darfBearbeiten) { androidx.compose.runtime.LaunchedEffect(Unit) { schliessen() }; return }
    DesignDialog(
        titel = "Testphase beendet",
        aufSchliessen = schliessen,
        bestaetigung = { StillerKnopf("Später", schliessen) },
        inhalt = {
            // Der Dialograhmen scrollt bewusst nicht selbst; bei großer Schrift auf S24-Breite scrollt der Inhalt hier.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text("Neue Wecker anlegen, bearbeiten und einschalten ist nach der Testphase nur mit der Freischaltung möglich.",
                    style = MaterialTheme.typography.bodyMedium)
                Angebot(zustand)
                Aktionen(zustand, activity)
            }
        },
    )
}

/** Status in den Einstellungen: Resttage, lebenslang freigeschaltet oder Test beendet. */
@Composable
fun FreischaltungsKarte(activity: Activity) {
    val zustand by Freischaltung.zustand.collectAsStateWithLifecycle()
    val status = when {
        zustand.gekauft -> "Lebenslang freigeschaltet"
        zustand.ausstehend -> "Zahlung wird von Google Play bestätigt"
        zustand.test.aktiv -> "Testphase: noch ${zustand.test.restTage} ${if (zustand.test.restTage == 1) "Tag" else "Tage"}"
        else -> "Testphase beendet"
    }
    Section("Freischaltung", collapsible = true, initiallyExpanded = !zustand.darfBearbeiten, summary = status,
        error = if (!zustand.darfBearbeiten) "Neue Wecker anlegen und bearbeiten ist gesperrt" else null) {
        Text(status, style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
        if (zustand.gekauft) {
            Text("Danke! Alle Funktionen sind dauerhaft freigeschaltet. Bei einer Neuinstallation mit demselben Google-Konto stellt „Kauf wiederherstellen“ die Freischaltung wieder her.",
                style = MaterialTheme.typography.bodySmall)
            if (zustand.hinweis.isNotBlank()) Text(zustand.hinweis, style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            StillerKnopf("Kauf wiederherstellen", { Freischaltung.wiederherstellen() })
        } else {
            Angebot(zustand)
            Aktionen(zustand, activity)
        }
    }
}
