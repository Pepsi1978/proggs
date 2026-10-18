package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben

/** Wie ein abgelaufener Timer klingelt: wie ein Wecker bis zum Ausschalten oder als kurze Meldung (Einstellungen → Benachrichtigungen). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerOptionen(vm: AppViewModel) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    run {
        Unterzeile("Wenn der Timer abläuft")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Wie ein Wecker", aktiv = e.timerWecker) { e.timerWecker = true }
            Chip("Kurze Meldung", aktiv = !e.timerWecker) { e.timerWecker = false }
        }
        Text(
            if (e.timerWecker) "Der Timer klingelt mit dem Weckton, auch bei „Nicht stören“, bis du ihn ausschaltest: in der Benachrichtigung mit „Aus“ oder „Schlummern“, " +
                "in Jarvis oben auf der Startseite (auch bei gesperrter App) oder per Sprache: „Stopp den Timer“. Wegwischen allein schaltet ihn nicht aus."
            else "Der Timer meldet sich mit einer Benachrichtigung, die klingelt, bis du sie wegwischst oder „Aus“ tippst.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
        )
        if (e.timerWecker) {
            Unterzeile("Lautstärke")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 50, 75, 100).forEach { p -> Chip("$p %", aktiv = e.timerLautstaerke == p) { e.timerLautstaerke = p } }
            }
            Text("Anteil der Wecker-Lautstärke deines Handys.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

            Unterzeile("Vibration")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.timerVibration) { e.timerVibration = true }
                Chip("Aus", aktiv = !e.timerVibration) { e.timerVibration = false }
            }

            Unterzeile("Schlummern")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10, 15).forEach { m -> Chip("$m min", aktiv = e.timerSchlummern == m) { e.timerSchlummern = m } }
            }
            Text("„Schlummern“ schaltet das Klingeln aus und startet den Timer mit dieser Zeit neu.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

            Unterzeile("Höchstens klingeln")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Bis zum Ausschalten", 5 to "5 min", 10 to "10 min", 30 to "30 min").forEach { (m, name) ->
                    Chip(name, aktiv = e.timerKlingelMax == m) { e.timerKlingelMax = m }
                }
            }
            Text("Danach verstummt der Timer und bleibt als stille Benachrichtigung stehen.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
