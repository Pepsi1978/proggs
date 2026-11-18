package de.frank.jarvis.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.sicherung.CloudSicherung
import de.frank.jarvis.ui.theme.LocalFarben
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Einstellungen › System: Cloud-Sicherung nach Google Drive (Apps Backup › Jarvis) und Wiederherstellen. */
@Composable
internal fun CloudSicherungKarte() {
    val f = LocalFarben.current
    val c = LocalContext.current
    LaunchedEffect(Unit) { CloudSicherung.aktualisiere(c) }
    val stand by CloudSicherung.stand.collectAsState()
    var meldung by remember { mutableStateOf<String?>(null) }
    var gewaehlt by remember { mutableStateOf<Uri?>(null) }
    val ordnerwahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) meldung = if (CloudSicherung.ordnerSetzen(c, uri)) "Ordner gespeichert. Die erste Sicherung kannst du gleich starten." else "Der Ordner ließ sich nicht dauerhaft freigeben."
    }
    val dateiwahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> gewaehlt = uri }

    Bereich("Cloud-Sicherung (Google Drive)") {
        val alt = stand.letzteErfolg == 0L || System.currentTimeMillis() - stand.letzteErfolg > 2 * 86_400_000L
        Zeile("Ordner", if (stand.ordnerGewaehlt) "Gewählt" else "Noch nicht gewählt", if (stand.ordnerGewaehlt) f.erfolg else f.gefahr)
        Zeile(
            "Letzte Sicherung",
            if (stand.laeuft) "Läuft gerade …" else if (stand.letzteErfolg == 0L) "Noch keine" else SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN).format(Date(stand.letzteErfolg)),
            if (alt) f.gefahr else f.erfolg,
        )
        stand.letzterFehler?.let { Text("Zuletzt gescheitert: $it", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        Text(
            "Jarvis sichert einmal am Tag alles, was er weiß und gelernt hat: Gedächtnis, Gespräche, Tagebuch-Kurzfassungen, Tagesauswertungen, Listen, Ablage, Biomarker und alle Einstellungen. " +
                "Ziel: Meine Ablage › Dokumente › Apps Backup › Jarvis. Die letzten 14 Sicherungen bleiben. API-Schlüssel, Passwörter und Anmeldungen sind bewusst nicht dabei, die trägst du nach einer Wiederherstellung selbst ein.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
        )
        Knopf(if (stand.ordnerGewaehlt) "Ordner ändern" else "Ordner in Google Drive wählen", Modifier.padding(top = 12.dp).fillMaxWidth(), haupt = !stand.ordnerGewaehlt) { ordnerwahl.launch(null) }
        if (stand.ordnerGewaehlt) Knopf(if (stand.laeuft) "Sichert …" else "Jetzt sichern", Modifier.padding(top = 8.dp).fillMaxWidth()) {
            meldung = "Sicherung läuft …"
            CloudSicherung.jetzt(c) { meldung = it }
        }
        Unterzeile("Wiederherstellen")
        Text("Nach einem neuen Handy: Jarvis installieren, hier die Sicherung aus Drive wählen. Jarvis spielt sie ein und startet neu.", color = f.textSchwach, fontSize = 13.sp)
        val wahl = gewaehlt
        if (wahl == null) Knopf("Sicherung wählen", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { dateiwahl.launch(arrayOf("application/zip", "application/octet-stream")) }
        else {
            Text("Achtung: Der jetzige Stand auf diesem Gerät wird durch die Sicherung ersetzt.", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Knopf("Einspielen und neu starten", Modifier.padding(top = 8.dp).fillMaxWidth(), farbe = f.gefahr) {
                meldung = "Prüfe die Sicherung …"
                CloudSicherung.wiederherstellenVorbereiten(c, wahl) { meldung = it }
                gewaehlt = null
            }
            Knopf("Abbrechen", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { gewaehlt = null }
        }
        meldung?.let { Text(it, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
    }
}
