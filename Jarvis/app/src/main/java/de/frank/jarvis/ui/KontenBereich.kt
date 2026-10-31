package de.frank.jarvis.ui

import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import de.frank.jarvis.helfer.KontenRegister
import de.frank.jarvis.helfer.KontoEintrag
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.delay

/** Wie lange ein Schlüssel nach dem Fingerabdruck sichtbar bleibt. Danach und beim Verlassen ist er wieder verdeckt. */
private const val SICHTBAR_S = 30

/** Verweis an den Stellen, wo früher ein Schlüsselfeld stand: Die Schlüssel liegen nur noch zentral. */
@Composable
fun SchluesselVerweis(vm: AppViewModel, titel: String, eingetragen: Boolean) {
    val f = LocalFarben.current
    Zeile(titel, if (eingetragen) "Eingetragen" else "Fehlt", if (eingetragen) f.erfolg else f.gefahr)
    Text("Schlüssel ändern: Konten und APIs", color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp).clickable { vm.einstellungsReiter = "konten" })
}

/** Der zentrale Bereich „Konten und APIs“: alle Schlüssel untereinander, normalerweise verdeckt. */
@Composable
fun KontenBereich(vm: AppViewModel) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val stand by KontenRegister.stand.collectAsState()
    val eintraege = remember(stand) { KontenRegister.alle(c) }
    var offen by rememberSaveable { mutableStateOf("") }
    Bereich("Konten und APIs", aufklappbar = false) {
        Text("Alle Schlüssel an einer Stelle, verschlüsselt mit dem Hauptschlüssel im Sicherheitschip des Handys. Anzeigen geht nur nach Fingerabdruck oder Gerätesperre. Helfer bekommen nie den Schlüssel selbst, nur die Funktion, ihn für erlaubte Adressen zu benutzen.",
            color = f.textSchwach, fontSize = 13.sp)
        eintraege.forEach { k ->
            KontoZeile(vm, k, offen == k.id) { offen = if (offen == k.id) "" else k.id }
        }
    }
    NeuerZugang(vm)
    Bereich("Was der Schutz leistet") {
        Text("Geschützt: die Dateien der App, Sicherungen und Exporte (die Schlüssel lassen sich nicht auf ein anderes Gerät mitnehmen), Chats, Prompts und Protokolle (Werte werden aus Helfer-Ergebnissen geschwärzt) sowie alle Helfer (sie sehen keinen Rohschlüssel). " +
            "Nicht geschützt: ein gerootetes oder vollständig kompromittiertes Handy und fremder Code, der in der laufenden Jarvis-App selbst steckt. Die automatische Nutzung läuft ohne Entsperren, das ist bewusst getrennt von der Anzeige. " +
            "Bei Verdacht: den Schlüssel beim Dienst widerrufen, hier ersetzen oder für Helfer sperren.", color = f.textSchwach, fontSize = 13.sp)
    }
}

@Composable
private fun KontoZeile(vm: AppViewModel, k: KontoEintrag, offen: Boolean, umschalten: () -> Unit) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val activity = LocalContext.current as? FragmentActivity
    val stand by KontenRegister.stand.collectAsState()
    val da = remember(k.id, offen, stand) { k.lesen().isNotBlank() }
    val gesperrt = remember(k.id, offen, stand) { KontenRegister.gesperrt(c, k.id) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = umschalten), verticalAlignment = Alignment.CenterVertically) {
            Text(k.titel, Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(when { gesperrt -> "Für Helfer gesperrt"; da -> "Eingetragen"; else -> "Fehlt" }, color = when { gesperrt -> f.gefahr; da -> f.erfolg; else -> f.textLeise }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(k.beschreibung, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
        if (!offen) return@Column
        val helfer = remember(k.id) { KontenRegister.verwendetVonHelfern(c, k.id) }
        if (k.dienst.isNotBlank()) Text("Dienst: ${k.dienst}", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        if (helfer.isNotEmpty()) Text("Betroffene Helfer: ${helfer.joinToString(", ")}", color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        KontenRegister.letzteNutzung(c, k.id)?.let { (wer, wann) ->
            Text("Zuletzt von $wer genutzt: " + java.text.SimpleDateFormat("dd.MM., HH:mm", java.util.Locale.GERMANY).format(java.util.Date(wann)) + " Uhr", color = f.textSchwach, fontSize = 13.sp)
        }
        // Anzeige nur nach Nachweis, kurz, und nie in Bildschirmfotos oder der App-Übersicht.
        var sichtbar by remember(k.id) { mutableStateOf<String?>(null) }
        var rest by remember(k.id) { mutableIntStateOf(0) }
        if (sichtbar != null) {
            Text(sichtbar!!, color = f.primaer, fontSize = 14.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
            Text("Wird in $rest Sekunden verdeckt.", color = f.textSchwach, fontSize = 12.sp)
            LaunchedEffect(sichtbar) { rest = SICHTBAR_S; while (rest > 0) { delay(1_000); rest-- }; sichtbar = null }
            DisposableEffect(Unit) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
            }
        }
        var neu by remember(k.id) { mutableStateOf("") }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Eingabe(neu, { neu = it }, if (da) "neuen Schlüssel einfügen" else "Schlüssel einfügen", Modifier.weight(1f), geheim = true)
            Spacer(Modifier.width(8.dp))
            Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { neu = it } }
        }
        if (neu.isNotBlank()) {
            if (helfer.isNotEmpty() && da) Text("Der Austausch gilt sofort auch für: ${helfer.joinToString(", ")}.", color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Knopf("Speichern", Modifier.padding(top = 8.dp).fillMaxWidth()) { k.schreiben(neu.trim()); neu = ""; sichtbar = null; KontenRegister.stand.value++; vm.kontoGeaendert(k.id); vm.meldung = "Gespeichert." }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (da && sichtbar == null) Knopf("Anzeigen", Modifier.weight(1f), haupt = false) {
                val a = activity ?: return@Knopf
                nachweisen(a, { vm.meldung = it }) { sichtbar = k.lesen() }
            }
            Knopf(if (gesperrt) "Für Helfer entsperren" else "Für Helfer sperren", Modifier.weight(1f), haupt = false) {
                if (!KontenRegister.sperren(c, k.id, !gesperrt)) vm.meldung = "Der Kontenspeicher ist gerade nicht lesbar; die Sperre bleibt, wie sie ist."
            }
        }
        // Bei Verdacht auf Missbrauch: Wert entfernen, mit Bestätigung und Blick auf die Betroffenen.
        var leerenFrage by remember(k.id) { mutableStateOf(false) }
        if (da && k.fest) {
            if (!leerenFrage) Knopf("Schlüssel entfernen …", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false, farbe = f.gefahr) { leerenFrage = true }
            else {
                Text("Danach funktioniert nicht mehr: ${k.verwendetVon}" + (if (helfer.isEmpty()) "." else ", dazu ${helfer.joinToString(", ")}."), color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Knopf("Ja, Schlüssel entfernen", Modifier.padding(top = 6.dp).fillMaxWidth(), farbe = f.gefahr) { k.schreiben(""); sichtbar = null; leerenFrage = false; KontenRegister.stand.value++; vm.kontoGeaendert(k.id); vm.meldung = "Entfernt." }
            }
        }
        if (!k.fest) Knopf("Zugang entfernen", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false, farbe = f.gefahr) {
            vm.meldung = KontenRegister.entfernen(c, k.id) ?: "Entfernt."
        }
    }
}

private fun nachweisen(a: FragmentActivity, fehler: (String) -> Unit, erfolg: () -> Unit) {
    val art = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    if (BiometricManager.from(a).canAuthenticate(art) != BiometricManager.BIOMETRIC_SUCCESS) { fehler("Ohne Fingerabdruck oder Gerätesperre bleibt der Schlüssel verdeckt."); return }
    BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = erfolg()
    }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Schlüssel anzeigen").setSubtitle("Mit Fingerabdruck oder Gerätesperre").setAllowedAuthenticators(art).build())
}

@Composable
private fun NeuerZugang(vm: AppViewModel) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    Bereich("Neuen Zugang hinzufügen") {
        var titel by rememberSaveable { mutableStateOf("") }
        var beschreibung by rememberSaveable { mutableStateOf("") }
        var dienst by rememberSaveable { mutableStateOf("") }
        var wert by remember { mutableStateOf("") }
        Unterzeile("Name")
        Eingabe(titel, { titel = it }, "zum Beispiel YouTube", Modifier.fillMaxWidth())
        Unterzeile("Zwei kurze Sätze: Was macht er, wofür wird er verwendet?")
        Eingabe(beschreibung, { beschreibung = it }, "Beschreibung", Modifier.fillMaxWidth())
        Unterzeile("Dienst")
        Eingabe(dienst, { dienst = it }, "zum Beispiel console.cloud.google.com", Modifier.fillMaxWidth())
        Unterzeile("Schlüssel")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eingabe(wert, { wert = it }, "einfügen", Modifier.weight(1f), geheim = true)
            Spacer(Modifier.width(8.dp))
            Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { wert = it } }
        }
        Knopf("Hinzufügen", Modifier.padding(top = 12.dp).fillMaxWidth()) {
            val fehler = KontenRegister.neu(c, titel, beschreibung, dienst, wert)
            vm.meldung = fehler ?: "Hinzugefügt."
            if (fehler == null) { titel = ""; beschreibung = ""; dienst = ""; wert = "" }
        }
        Text("Jarvis legt nie selbst Konten bei fremden Diensten an. Einen Schlüssel trägst immer du hier ein.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
