package de.frank.updatestation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import de.frank.updatestation.AppEintrag
import de.frank.updatestation.BuildConfig
import de.frank.updatestation.Einstellungen
import de.frank.updatestation.InstallStatus
import de.frank.updatestation.Status
import de.frank.updatestation.Versionslog
import de.frank.updatestation.Zustand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Aktionen(
    val pruefen: () -> Unit,
    val installiere: (AppEintrag) -> Unit,
    val installiereAlle: () -> Unit,
    val verbindeDrive: () -> Unit,
    val waehleOrdner: () -> Unit,
    val erlaubeInstallation: () -> Unit,
    val erlaubeBenachrichtigungen: () -> Unit,
    val speicherePfad: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HauptScreen(
    zustand: Zustand,
    quelle: String?,
    drivePfad: String,
    darfInstallieren: Boolean,
    darfBenachrichtigen: Boolean,
    snackbar: SnackbarHostState,
    aktionen: Aktionen,
) {
    var einstellungenOffen by remember { mutableStateOf(false) }
    var verlaufFuer by remember { mutableStateOf<AppEintrag?>(null) }
    val updates = zustand.eintraege.filter { it.status == Status.UPDATE }
    val warnungen = zustand.eintraege.filter { it.status == Status.SIGNATUR_ANDERS || it.status == Status.APK_FEHLT }
    val aktuell = zustand.eintraege.filter { it.status == Status.AKTUELL || it.status == Status.INSTALLIERT_NEUER }
    val fehlend = zustand.eintraege.filter { it.status == Status.NICHT_INSTALLIERT }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { innen ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innen),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Kopf(
                    zustand = zustand,
                    anzahlUpdates = updates.size,
                    eingerichtet = quelle != null,
                    onPruefen = aktionen.pruefen,
                    onEinstellungen = { einstellungenOffen = true },
                )
            }

            if (quelle == null) {
                item { Einrichtung(aktionen.verbindeDrive, aktionen.waehleOrdner) }
            }
            if (quelle == "drive" && zustand.anmeldungNoetig) {
                item {
                    Hinweis(Icons.Rounded.CloudSync, "Google Drive neu verbinden",
                        "Die Anmeldung ist abgelaufen oder wurde entzogen.", "Verbinden", MaterialTheme.colorScheme.primary,
                        aktionen.verbindeDrive)
                }
            }
            if (!darfInstallieren && quelle != null) {
                item {
                    Hinweis(Icons.Rounded.Security, "Installationen erlauben",
                        "Damit UpdateStation Updates einspielen kann, braucht sie die Erlaubnis „Unbekannte Apps installieren“.",
                        "Erlauben", Farben.Bernstein, aktionen.erlaubeInstallation)
                }
            }
            if (!darfBenachrichtigen) {
                item {
                    Hinweis(Icons.Rounded.NotificationsOff, "Benachrichtigungen sind aus",
                        "Ohne Benachrichtigung erfährst du nicht, wenn ein Update bereitliegt.",
                        "Einschalten", Farben.Bernstein, aktionen.erlaubeBenachrichtigungen)
                }
            }
            zustand.fehler?.let { text ->
                item {
                    Hinweis(Icons.Rounded.ErrorOutline, "Prüfung fehlgeschlagen", text, "Erneut", MaterialTheme.colorScheme.error, aktionen.pruefen)
                }
            }

            if (updates.isNotEmpty()) {
                item {
                    Abschnitt(
                        titel = if (updates.size == 1) "1 Update bereit" else "${updates.size} Updates bereit",
                        aktion = if (updates.size > 1) "Alle aktualisieren" else null,
                        onAktion = aktionen.installiereAlle,
                    )
                }
                updates.forEach { e ->
                    item(key = "u_" + e.paket) {
                        UpdateKarte(e, zustand.installationen[e.paket], darfInstallieren, { verlaufFuer = e }) { aktionen.installiere(e) }
                    }
                }
            }

            warnungen.forEach { e ->
                item(key = "w_" + e.paket) { WarnKarte(e) }
            }

            if (aktuell.isNotEmpty()) {
                item { Abschnitt("Auf dem neuesten Stand") }
                item { AktuellGruppe(aktuell) { verlaufFuer = it } }
            }

            if (fehlend.isNotEmpty()) {
                item { NichtInstalliertGruppe(fehlend, zustand.installationen, darfInstallieren, aktionen.installiere) }
            }

            if (quelle != null && zustand.eintraege.isEmpty() && !zustand.prueftGerade && zustand.fehler == null) {
                item { LeerZustand() }
            }

            item { Fuss() }
        }
    }

    if (einstellungenOffen) {
        ModalBottomSheet(onDismissRequest = { einstellungenOffen = false }) {
            EinstellungenInhalt(quelle, drivePfad, aktionen) { einstellungenOffen = false }
        }
    }
    verlaufFuer?.let { e ->
        ModalBottomSheet(onDismissRequest = { verlaufFuer = null }) { Verlauf(e) }
    }
}

// ---------------------------------------------------------------------------------------------

/** Neuerungen zwischen installierter und angebotener Version, aus dem Versionslog in update.json. */
@Composable
private fun Neuerungen(e: AppEintrag) {
    val neu = e.fund.manifest.versionslog
        .filter { it.versionCode > (e.installiertCode ?: 0L) }
        .sortedByDescending { it.versionCode }
        .take(4)
    if (neu.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Neu in dieser Version", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        neu.forEach { v ->
            Row {
                Text(v.versionName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
                Text(v.notiz, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Versionsverlauf: Log aus der installierten App (Asset) plus neuere Einträge aus dem Update. */
@Composable
private fun Verlauf(e: AppEintrag) {
    val context = LocalContext.current
    val installiert = remember(e.paket, e.installiertCode) { Versionslog.installiert(context, e.paket) }
    val alle = (installiert + e.fund.manifest.versionslog).distinctBy { it.versionCode }.sortedByDescending { it.versionCode }
    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(e.paket, e.label, 48.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(e.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (e.installiertCode != null) "Installiert: ${e.installiertName} · Build ${e.installiertCode}" else "Nicht auf diesem Handy",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Versionsverlauf", style = MaterialTheme.typography.titleMedium)
        if (installiert.isEmpty() && e.installiertCode != null) {
            Text(
                "Die installierte Version bringt noch keinen Versionslog mit – der Verlauf stammt aus dem Update-Ordner.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(alle.size) { i ->
                val v = alle[i]
                val markierung = when {
                    e.installiertCode == null -> null
                    v.versionCode == e.installiertCode -> "Installiert"
                    v.versionCode > e.installiertCode -> "Neu"
                    else -> null
                }
                Row(Modifier.padding(vertical = 8.dp)) {
                    Box(
                        Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape)
                            .background(if (markierung == "Neu") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(v.versionName, style = MaterialTheme.typography.titleSmall.fett)
                            Text("  Build ${v.versionCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            if (markierung != null) VersionChip(markierung, hervorgehoben = markierung == "Neu")
                        }
                        if (v.stand.isNotBlank()) {
                            Text(v.stand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (v.notiz.isNotBlank()) Text(v.notiz, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun Kopf(zustand: Zustand, anzahlUpdates: Int, eingerichtet: Boolean, onPruefen: () -> Unit, onEinstellungen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Farben.kopfVerlauf)
            .statusBarsPadding()
            .padding(start = 22.dp, end = 12.dp, top = 8.dp, bottom = 26.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("UpdateStation", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Deine Apps immer auf dem neuesten Stand", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = onEinstellungen) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Einstellungen", tint = Color.White)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (anzahlUpdates > 0) Icons.Rounded.SystemUpdate else Icons.Rounded.CheckCircle,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !eingerichtet -> "Noch nicht eingerichtet"
                            zustand.prueftGerade && zustand.eintraege.isEmpty() -> "Suche nach Updates …"
                            anzahlUpdates == 1 -> "1 Update verfügbar"
                            anzahlUpdates > 1 -> "$anzahlUpdates Updates verfügbar"
                            else -> "Alles aktuell"
                        },
                        style = MaterialTheme.typography.titleLarge, color = Color.White,
                    )
                    Text(
                        text = if (zustand.letztePruefung > 0) "Geprüft ${zeit(zustand.letztePruefung)} · automatisch alle 30 Min." else "Automatische Prüfung alle 30 Minuten",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            if (eingerichtet) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onPruefen,
                    enabled = !zustand.prueftGerade,
                    modifier = Modifier.fillMaxWidth().padding(end = 10.dp).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Farben.Indigo,
                        disabledContainerColor = Color.White.copy(alpha = 0.7f), disabledContentColor = Farben.Indigo,
                    ),
                ) {
                    DrehendesIcon(zustand.prueftGerade)
                    Spacer(Modifier.width(10.dp))
                    Text(if (zustand.prueftGerade) "Prüfe …" else "Jetzt prüfen", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DrehendesIcon(dreht: Boolean) {
    val winkel = if (dreht) {
        val t = rememberInfiniteTransition(label = "drehen")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "winkel").value
    } else 0f
    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp).rotate(winkel))
}

@Composable
private fun Abschnitt(titel: String, aktion: String? = null, onAktion: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (aktion != null) TextButton(onClick = onAktion) { Text(aktion) }
    }
}

@Composable
private fun Karte(modifier: Modifier = Modifier, rand: Brush? = null, content: @Composable () -> Unit) {
    val form = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (rand != null) Modifier.border(1.5.dp, rand, form) else Modifier),
        shape = form,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) { content() }
}

@Composable
private fun UpdateKarte(e: AppEintrag, install: InstallStatus?, darfInstallieren: Boolean, onVerlauf: () -> Unit, onInstallieren: () -> Unit) {
    val m = e.fund.manifest
    Karte(Modifier.animateContentSize(), rand = Farben.randVerlauf) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.clickable(onClick = onVerlauf), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(e.paket, e.label, 54.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VersionChip(e.installiertName ?: "?", hervorgehoben = false)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(horizontal = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        VersionChip(m.versionName, hervorgehoben = true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Build ${e.installiertCode} → ${m.versionCode} · ${groesse(m.groesse)}" +
                    (if (m.erstelltAm.isNotBlank()) " · bereitgestellt ${m.erstelltAm}" else ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Neuerungen(e)
            Spacer(Modifier.height(14.dp))
            InstallBereich(install, darfInstallieren, "Aktualisieren", onInstallieren)
        }
    }
}

@Composable
private fun InstallBereich(install: InstallStatus?, darfInstallieren: Boolean, knopf: String, onInstallieren: () -> Unit) {
    when (install) {
        is InstallStatus.Laedt -> Fortschritt("Wird geladen … ${install.prozent} %", install.prozent / 100f)
        InstallStatus.Prueft -> Fortschritt("Prüfe Prüfsumme, Version und Signatur …", null)
        InstallStatus.WartetAufBestaetigung -> Zeile(Icons.Rounded.TouchApp, "Bitte die Installation im Systemdialog bestätigen.", MaterialTheme.colorScheme.primary)
        InstallStatus.Fertig -> Zeile(Icons.Rounded.CheckCircle, "Installiert", MaterialTheme.colorScheme.secondary)
        else -> {
            if (install is InstallStatus.Fehler) {
                Zeile(Icons.Rounded.ErrorOutline, install.text, MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = onInstallieren,
                enabled = darfInstallieren,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Download, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (install is InstallStatus.Fehler) "Erneut versuchen" else knopf)
            }
        }
    }
}

@Composable
private fun Fortschritt(text: String, wert: Float?) {
    Column {
        if (wert != null) {
            LinearProgressIndicator(progress = { wert }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
        }
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Zeile(icon: ImageVector, text: String, farbe: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = farbe, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = farbe)
    }
}

@Composable
private fun VersionChip(text: String, hervorgehoben: Boolean) {
    val farbe = if (hervorgehoben) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(farbe.copy(alpha = if (hervorgehoben) 0.16f else 0.10f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = farbe, fontWeight = if (hervorgehoben) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun WarnKarte(e: AppEintrag) {
    val m = e.fund.manifest
    val signatur = e.status == Status.SIGNATUR_ANDERS
    val farbe = if (signatur) MaterialTheme.colorScheme.error else Farben.Bernstein
    Karte {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            AppIcon(e.paket, e.label, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.label, style = MaterialTheme.typography.titleMedium)
                Text("${e.installiertName} → ${m.versionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Zeile(
                    if (signatur) Icons.Rounded.Warning else Icons.Rounded.HourglassTop,
                    if (signatur) "Signatur passt nicht zur installierten App – wird aus Sicherheitsgründen nicht installiert."
                    else "Update angekündigt, die APK wird noch synchronisiert.",
                    farbe,
                )
            }
        }
    }
}

@Composable
private fun AktuellGruppe(liste: List<AppEintrag>, onVerlauf: (AppEintrag) -> Unit) {
    Karte {
        Column(Modifier.padding(vertical = 6.dp)) {
            liste.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 74.dp, end = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { onVerlauf(e) }.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(e.paket, e.label, 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (e.status == Status.INSTALLIERT_NEUER) "Version ${e.installiertName} · neuer als das Update (${e.fund.manifest.versionName})"
                            else "Version ${e.installiertName}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.CheckCircle, "aktuell", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun NichtInstalliertGruppe(
    liste: List<AppEintrag>,
    installationen: Map<String, InstallStatus>,
    darfInstallieren: Boolean,
    onInstallieren: (AppEintrag) -> Unit,
) {
    var offen by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { offen = !offen }.padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Nicht auf diesem Handy (${liste.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        AnimatedVisibility(offen) {
            Karte {
                Column(Modifier.padding(vertical = 6.dp)) {
                    liste.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(e.paket, e.label, 42.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.label, style = MaterialTheme.typography.bodyLarge)
                                    Text("${e.paket} · ${e.fund.manifest.versionName}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            val status = installationen[e.paket]
                            if (status != null || e.fund.apkRef != null) {
                                Spacer(Modifier.height(10.dp))
                                if (status == null) {
                                    OutlinedButton(onClick = { onInstallieren(e) }, enabled = darfInstallieren, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                        Text("Installieren")
                                    }
                                } else {
                                    InstallBereich(status, darfInstallieren, "Installieren") { onInstallieren(e) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hinweis(icon: ImageVector, titel: String, text: String, knopf: String, farbe: Color, onKlick: () -> Unit) {
    Karte {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(farbe.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = farbe, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titel, style = MaterialTheme.typography.titleSmall.fett)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onKlick) { Text(knopf) }
        }
    }
}

@Composable
private fun Einrichtung(onDrive: () -> Unit, onOrdner: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Wo liegen deine Updates?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        Text(
            "Der Skill „apk update“ legt jede neue APK mit einer update.json in „Meine Ablage › Dokumente › Updates“. " +
                "Wähle, wie UpdateStation diesen Ordner liest.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        QuellenKachel(Icons.Rounded.CloudSync, "Google Drive", "Liest den Ordner direkt aus deinem Google-Konto. Empfohlen.", true, onDrive)
        QuellenKachel(Icons.Rounded.FolderOpen, "Ordner auf dem Handy", "Für einen Ordner, den eine Sync-App aus Google Drive spiegelt.", false, onOrdner)
    }
}

@Composable
private fun QuellenKachel(icon: ImageVector, titel: String, text: String, empfohlen: Boolean, onKlick: () -> Unit) {
    val form = RoundedCornerShape(22.dp)
    Surface(
        onClick = onKlick,
        shape = form,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().then(if (empfohlen) Modifier.border(1.5.dp, Farben.randVerlauf, form) else Modifier),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Farben.kopfVerlauf), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(titel, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LeerZustand() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Inbox, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("Noch keine Updates im Ordner", style = MaterialTheme.typography.titleMedium)
        Text(
            "Sobald der Skill „apk update“ eine APK ablegt, erscheint sie hier.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Fuss() {
    Text(
        "UpdateStation ${BuildConfig.VERSION_NAME} · Stand ${BuildConfig.VERSION_BUMPED_AT}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).navigationBarsPadding(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun EinstellungenInhalt(quelle: String?, drivePfad: String, aktionen: Aktionen, schliessen: () -> Unit) {
    var pfad by rememberSaveable { mutableStateOf(drivePfad) }
    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        Text(
            "Quelle: " + when (quelle) { "drive" -> "Google Drive"; "ordner" -> "Ordner auf dem Handy"; else -> "noch keine" },
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { schliessen(); aktionen.verbindeDrive() }, modifier = Modifier.weight(1f)) { Text("Google Drive") }
            OutlinedButton(onClick = { schliessen(); aktionen.waehleOrdner() }, modifier = Modifier.weight(1f)) { Text("Ordner wählen") }
        }
        OutlinedTextField(
            value = pfad,
            onValueChange = { pfad = it },
            label = { Text("Ordner in Google Drive") },
            supportingText = { Text("Standard: ${Einstellungen.STANDARD_PFAD}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { aktionen.speicherePfad(pfad.trim().trim('/')); schliessen() }, modifier = Modifier.fillMaxWidth()) {
            Text("Pfad speichern und prüfen")
        }
        Text(
            "UpdateStation prüft automatisch alle 30 Minuten, sobald Internet da ist, und meldet jedes neue Update einmal. " +
                "Installiert wird nur, wenn die Build-Nummer höher ist als die installierte und die Signatur passt.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppIcon(paket: String, label: String, groesse: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val bild = remember(paket) {
        runCatching { context.packageManager.getApplicationIcon(paket).toBitmap(144, 144).asImageBitmap() }.getOrNull()
    }
    if (bild != null) {
        Image(bild, contentDescription = null, modifier = Modifier.size(groesse).clip(RoundedCornerShape(groesse * 0.28f)))
    } else {
        Box(
            Modifier.size(groesse).clip(RoundedCornerShape(groesse * 0.28f)).background(Farben.kopfVerlauf),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun groesse(bytes: Long): String =
    if (bytes <= 0) "Größe unbekannt" else String.format(Locale.GERMANY, "%.1f MB", bytes / 1_000_000.0)

private fun zeit(ms: Long): String {
    val jetzt = Calendar.getInstance()
    val dann = Calendar.getInstance().apply { timeInMillis = ms }
    val uhr = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(ms))
    return when {
        jetzt.get(Calendar.YEAR) == dann.get(Calendar.YEAR) && jetzt.get(Calendar.DAY_OF_YEAR) == dann.get(Calendar.DAY_OF_YEAR) -> "heute, $uhr"
        else -> SimpleDateFormat("dd.MM., HH:mm", Locale.GERMANY).format(Date(ms))
    }
}
