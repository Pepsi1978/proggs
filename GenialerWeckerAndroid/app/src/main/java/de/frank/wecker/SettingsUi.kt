@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import de.frank.genialerwecker.app.BuildConfig
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import de.frank.wecker.design.Design
import de.frank.wecker.design.LocalGestalt

@Composable
fun SettingsPage(vm: WeckerViewModel, activity: ComponentActivity) {
    val revision by vm.settingsRevision.collectAsStateWithLifecycle()
    val settings = vm.settings
    val busy by vm.busy.collectAsStateWithLifecycle()
    val permissions = rememberReadiness()
    val stimmenJeSprache by vm.stimmenJeSprache.collectAsStateWithLifecycle()
    val stimmenFehler by vm.stimmenFehler.collectAsStateWithLifecycle()
    val stimmenEngine by vm.stimmenEngine.collectAsStateWithLifecycle()
    var rate by remember(revision) { mutableFloatStateOf(settings.ttsSpeechRate) }
    // Zurück aus den Android-Einstellungen (Sprachdaten geladen): Stimmen neu prüfen.
    val lebenszyklus = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.ladeLokaleStimmen() }
        lebenszyklus.addObserver(beobachter)
        onDispose { lebenszyklus.removeObserver(beobachter) }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.settingsRevision.value++ }
    fun launch(action: String, packageUri: Boolean = false, extraPackage: Boolean = false) {
        runCatching { activity.startActivity(Intent(action).apply {
            if (packageUri) data = Uri.parse("package:${activity.packageName}")
            if (extraPackage) putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }) }
            .onFailure { vm.message.value = "Diese Einstellungsseite ist auf dem Gerät nicht verfügbar. Öffne die Android-App-Einstellungen." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = seitenAbstandOben(), bottom = 16.dp).navigationBarsPadding()) {
      DesignBlatt {
       Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val missing = permissions.count { !it.second }
        // Every requirement sits next to the one button that fixes it; nothing to search for.
        fun fix(name: String) {
            if (!Weckbereitschaft.beheben(activity, name) { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) })
                vm.message.value = "Diese Einstellungsseite ist auf dem Gerät nicht verfügbar. Öffne die Android-App-Einstellungen."
        }
        // Alles erteilt: nur eine zugeklappte Zeile wie „Benachrichtigungen“. Fehlt etwas, steht die Karte offen.
        // key() setzt den Klappzustand neu, sobald sich „fehlt etwas“ ändert.
        key(missing == 0) {
        Section("Weckbereitschaft", collapsible = missing == 0, initiallyExpanded = missing > 0,
            summary = if (missing == 0) "Alles bereit" else "$missing ${if (missing == 1) "Freigabe fehlt" else "Freigaben fehlen"}") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (missing == 0) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive, null,
                    tint = if (missing == 0) LocalSemantisch.current.erfolg else LocalSemantisch.current.warnung)
                Text(if (missing == 0) "Alles bereit: Der Wecker klingelt auch gesperrt und ohne Internet."
                    else "$missing ${if (missing == 1) "Freigabe fehlt" else "Freigaben fehlen"}. Tippe jeweils auf „Erlauben“.",
                    Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            permissions.forEach { (name, ready) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ready) "✓" else "○", color = if (ready) LocalSemantisch.current.erfolg else LocalSemantisch.current.warnung)
                    Text(name, Modifier.weight(1f).padding(start = 10.dp), color = if (ready) LocalGold.current.textPrimaer else LocalSemantisch.current.warnung)
                    if (ready) Text("erteilt", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                    else StillerKnopf("Erlauben", { fix(name) }, Modifier.semantics { contentDescription = "$name erlauben" }, hervorgehoben = true)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StillerKnopf("Nicht-stören-Modi öffnen", { launch("android.settings.ZEN_MODE_SETTINGS") })
                StillerKnopf("App-Info öffnen", { launch(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true) })
            }
            Text("Erlaube Wecker in allen verwendeten Nicht-stören-Modi und Routinen. Nach „Stopp erzwingen“ die App einmal öffnen. Ein ausgeschaltetes Telefon kann nicht wecken.", style = MaterialTheme.typography.bodySmall)
        }
        }
        BenachrichtigungenKarte(vm, activity)
        var ausrichtung by remember(revision) { mutableStateOf(settings.ausrichtung) }
        var design by remember(revision) { mutableStateOf(settings.design) }
        val modus by vm.theme.collectAsStateWithLifecycle()
        var statuszeile by remember(revision) { mutableStateOf(settings.statuszeileSichtbar) }
        val modusOptionen = listOf("light" to "Hell", "dark" to "Dunkel", "system" to "Automatisch")
        Section("Darstellung", collapsible = true, initiallyExpanded = false,
            summary = "${Design.von(design).anzeige} · ${modusOptionen.find { it.first == modus }?.second ?: "Hell"} · " +
                (Ausrichtung.optionen.find { it.first == ausrichtung }?.second ?: "Automatisch")) {
            // Direkt anklickbare Punkte statt großer Knöpfe mit Auswahlfenster — und kaum Erklärtext.
            AuswahlPunkte("Design", design, Design.entries.map { it.id to it.anzeige }) {
                design = it; settings.design = it; vm.settingsRevision.value++
            }
            HorizontalDivider(color = LocalGold.current.rahmen)
            AuswahlPunkte("Modus", modus, modusOptionen) { settings.theme = it }
            HorizontalDivider(color = LocalGold.current.rahmen)
            AuswahlPunkte("Ausrichtung", ausrichtung, Ausrichtung.optionen) {
                ausrichtung = it; settings.ausrichtung = it; Ausrichtung.anwenden(activity, it); vm.settingsRevision.value++
            }
            HorizontalDivider(color = LocalGold.current.rahmen)
            Toggle("Statuszeile anzeigen", statuszeile) { statuszeile = it; settings.statuszeileSichtbar = it }
        }
        val alle = stimmenJeSprache
        val deutschFehlt = alle?.get("de")?.isEmpty() == true
        // key(): Das Aufklappen richtet sich nach dem Prüfergebnis, das erst nach der ersten Anzeige kommt.
        key(alle == null, deutschFehlt) {
        Section("Vorlesen · Stimme & Tempo", collapsible = true, initiallyExpanded = deutschFehlt,
            summary = if (alle == null) "Offline-Stimmen werden geprüft …"
                else Sprachen.CODES.joinToString(" · ") { "${Sprachen.kurz(it)} ${if (alle[it].isNullOrEmpty()) "fehlt" else "✓"}" } + " · Tempo ${"%.2f".format(rate)}×",
            error = if (deutschFehlt) "Keine deutsche Offline-Stimme installiert" else null) {
            Text("Vorgelesen wird ausschließlich mit Stimmen, die auf diesem Gerät installiert sind; der Text verlässt das Handy nicht. Die Sprache stellst du je Wecker beim Text ein, hier die Stimme je Sprache.",
                style = MaterialTheme.typography.bodySmall)
            if (alle == null) Text("Installierte Stimmen werden geprüft …", style = MaterialTheme.typography.bodySmall)
            else {
                if (stimmenFehler.isNotBlank()) Text(stimmenFehler, color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                // Ohne Google-Sprachausgabe fehlen die bevorzugten deutschen Stimmen; ehrlich sagen, woher die Stimmen stammen.
                if (stimmenEngine != null && stimmenEngine != LokaleStimmen.GOOGLE) Text(
                    "Die Google-Sprachausgabe ist auf diesem Gerät nicht verfügbar. Es werden die Offline-Stimmen der Standard-Sprachausgabe verwendet.",
                    color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                Sprachen.CODES.forEachIndexed { i, code ->
                    if (i > 0) HorizontalDivider(color = LocalGold.current.rahmen)
                    SpracheStimmen(code, alle[code].orEmpty(), vm, busy, activity)
                }
                HorizontalDivider(color = LocalGold.current.rahmen)
                Text("Sprechtempo (alle Sprachen): ${"%.2f".format(rate)}×")
                Regler3D(rate, { rate = it }, bereich = .5f..2f,
                    aufAenderungFertig = { settings.ttsSpeechRate = rate; vm.settingsChanged() })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StillerKnopf("Sprachdaten in Android verwalten", { oeffneSprachdaten(activity, vm) })
                    StillerKnopf("Erneut prüfen", vm::ladeLokaleStimmen)
                }
                Text("Beim Speichern werden sechs Varianten mit behutsamen Tempo-Unterschieden offline vorbereitet. Beim Wecken läuft Variante 1 bis 6, dann wieder 1.", style = MaterialTheme.typography.bodySmall)
            }
        }
        }
        FreischaltungsKarte(activity)
        Section("Genialer Wecker") {
            Text("Version ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_BUMPED_AT}")
            Text("Weckt zuverlässig ohne Internet: Weckton, eigene Musik, vorgelesener Text mit einer Gerätestimme und Foto-Aufgabe.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
       }
      }
    }
}

/** One switch per notification type; for now only the sleep reminder. No master switch. */
@Composable
private fun BenachrichtigungenKarte(vm: WeckerViewModel, activity: ComponentActivity) {
    val context = activity
    // Explicit refresh instead of polling: resume (e.g. back from system settings), permission result and switch.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val on = remember(refresh) { SchlafErinnerung.enabled(context) }
    val blocked = remember(refresh) { SchlafErinnerung.blockiert(context) }
    val inexact = remember(refresh) { SchlafErinnerung.exaktFehlt(context) }
    val problems = remember(refresh, alarms) { SchlafErinnerung.statusFehler(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        SchlafErinnerung.syncAll(context); refresh++
    }
    fun open(intent: Intent) {
        try { activity.startActivity(intent) }
        catch (e: Exception) { vm.message.value = "Die Android-Einstellung konnte nicht geöffnet werden (${e.javaClass.simpleName})." }
    }
    Section("Benachrichtigungen", collapsible = true, initiallyExpanded = false,
        summary = "Schlafenszeit-Erinnerung ${if (on) SchlafErinnerung.leadMinutes(context).let { if (it == 0) "zur Schlafenszeit" else "$it Min. vorher" } else "aus"}" +
            "${if (on && blocked != null) " · gesperrt" else ""}",
        error = if (on && blocked != null) "Die Erinnerung ist eingeschaltet, wird von Android aber nicht angezeigt." else null) {
        Toggle("Schlafenszeit-Erinnerung", on) { checked ->
            // The switch shows the stored state only; a failed write keeps the old state and says so.
            val stored = SchlafErinnerung.setEnabled(context, checked)
            if (!stored) vm.message.value = "Die Einstellung konnte nicht gespeichert werden. Der bisherige Stand gilt weiter."
            refresh++
            if (stored && checked && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Text("Vor der berechneten Schlafenszeit, nur für aktive Wecker mit Schlafdauer.", style = MaterialTheme.typography.bodySmall)
        if (on) {
            // Beim Ziehen nur die Anzeige; gespeichert und neu geplant wird erst am Ende der Geste.
            var vorlauf by remember(refresh) { mutableIntStateOf(SchlafErinnerung.leadMinutes(context)) }
            Text(if (vorlauf == 0) "Vorlauf: zur Schlafenszeit" else "Vorlauf: $vorlauf Min. vorher",
                style = MaterialTheme.typography.titleMedium, color = LocalGold.current.primaer)
            // Die Neuplanung hängt weiterhin ausschließlich am Ende der Geste, nicht an jeder
            // Bewegung — sonst würde bei jedem Pixel neu terminiert.
            Regler3D(vorlauf.toFloat(), { vorlauf = it.roundToInt() },
                Modifier.semantics {
                    contentDescription = "Vorlauf der Schlafenszeit-Erinnerung"
                    stateDescription = if (vorlauf == 0) "zur Schlafenszeit" else "$vorlauf Minuten vorher"
                },
                bereich = 0f..SchlafPlan.LEAD_MAX_MINUTES.toFloat(), stufen = SchlafPlan.LEAD_MAX_MINUTES - 1,
                aufAenderungFertig = {
                    if (!SchlafErinnerung.setLeadMinutes(context, vorlauf)) {
                        vm.message.value = "Der Vorlauf konnte nicht gespeichert werden. Der bisherige Stand gilt weiter."
                        vorlauf = SchlafErinnerung.leadMinutes(context)
                    }
                    refresh++
                })
            Text("0 Minuten erinnert genau zur Schlafenszeit. Ausgeschaltet wird die Erinnerung allein über den Schalter.",
                style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            SchlafTonWahl()
            when (blocked) {
                "app" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Benachrichtigungen der App sind ausgeschaltet.", Modifier.weight(1f), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Erlauben", {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
                    }, hervorgehoben = true)
                }
                "kanal" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Der Kanal „Schlafenszeit-Erinnerung“ ist gesperrt.", Modifier.weight(1f), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Kanal öffnen", {
                        open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, SchlafErinnerung.CHANNEL))
                    }, hervorgehoben = true)
                }
                else -> if (alarms.none { it.enabled && it.sleepMinutes > 0 })
                    Text("Noch kein aktiver Wecker mit Schlafdauer.", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            }
            if (inexact) Text("Ohne Freigabe für genaue Zeiten kann die Erinnerung einige Minuten verspätet kommen.",
                style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
        }
        problems.forEach { Text(it, color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall) }
    }
}


/**
 * Klingelton und Lautstärke der Schlafenszeit-Erinnerung. Die Liste zeigt die Standard-Töne des
 * Handys; jeder lässt sich vorher anhören. „Erinnerung testen“ klingt genau so laut wie später
 * die echte Erinnerung — unabhängig von der Gerätelautstärke.
 */
@Composable
private fun SchlafTonWahl() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ton by remember { mutableStateOf(SchlafTon.gewaehlt(context)) }
    var lautstaerke by remember { mutableIntStateOf(SchlafTon.lautstaerke(context)) }
    var offen by rememberSaveable { mutableStateOf(false) }
    val titel = remember(ton) { SchlafTon.titel(context, ton) }
    DisposableEffect(Unit) { onDispose { SchlafTon.stoppen() } }
    HorizontalDivider(color = LocalGold.current.rahmen)
    Text("Klingelton der Erinnerung", style = MaterialTheme.typography.labelLarge)
    GoldKnopf(titel, { offen = true }, Modifier.fillMaxWidth(),
        symbol = { Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp)) })
    Text("Lautstärke der Erinnerung: $lautstaerke %", style = MaterialTheme.typography.bodyMedium)
    Regler3D(lautstaerke.toFloat(), { lautstaerke = it.roundToInt() }, bereich = 5f..100f,
        aufAenderungFertig = { SchlafTon.setLautstaerke(context, lautstaerke) })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldKnopf("Erinnerung testen", { SchlafTon.spielen(context, ton, lautstaerke) })
        StillerKnopf("Stoppen", { SchlafTon.stoppen() })
    }
    Text("Die Erinnerung klingt genau so laut wie hier beim Testen — egal wie laut oder leise das Handy gerade gestellt ist. Bei „Nicht stören“ bleibt sie stumm.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    if (offen) {
        val toene = remember { SchlafTon.alle(context) }
        de.frank.wecker.design.DesignDialog(
            titel = "Klingelton wählen",
            aufSchliessen = { SchlafTon.stoppen(); offen = false },
            bestaetigung = { StillerKnopf("Schließen", { SchlafTon.stoppen(); offen = false }) },
            inhalt = {
                if (toene.isEmpty()) Text("Auf diesem Gerät wurden keine Benachrichtigungstöne gefunden.", style = MaterialTheme.typography.bodySmall)
                else androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 420.dp).weight(1f, fill = false),
                    state = androidx.compose.foundation.lazy.rememberLazyListState(toene.indexOfFirst { it.uri == ton }.coerceAtLeast(0))) {
                    items(toene.size, key = { toene[it].uri }) { i ->
                        val eintrag = toene[i]
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f).heightIn(min = 48.dp).selectable(eintrag.uri == ton, interactionSource = null, indication = null,
                                role = androidx.compose.ui.semantics.Role.RadioButton) {
                                ton = eintrag.uri; SchlafTon.setGewaehlt(context, eintrag.uri)
                            }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(eintrag.uri == ton, null)
                                Text(eintrag.titel, Modifier.padding(start = 8.dp))
                            }
                            StillerKnopf("Anhören", { SchlafTon.spielen(context, eintrag.uri, lautstaerke) })
                        }
                    }
                }
            },
        )
    }
}


/** Eine kleine Gruppe direkt anklickbarer Auswahlpunkte mit Überschrift. */
@Composable
private fun AuswahlPunkte(titel: String, gewaehlt: String, optionen: List<Pair<String, String>>, waehlen: (String) -> Unit) {
    Text(titel, style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
    optionen.forEach { (id, name) ->
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).selectable(id == gewaehlt, interactionSource = null, indication = null,
            role = androidx.compose.ui.semantics.Role.RadioButton) { if (id != gewaehlt) waehlen(id) },
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(id == gewaehlt, null)
            Text(name, Modifier.padding(start = 8.dp))
        }
    }
}


/** Öffnet die Android-Seite zum Laden von Sprachdaten; ohne passende Seite ein klarer Hinweis. */
private fun oeffneSprachdaten(activity: ComponentActivity, vm: WeckerViewModel, sprache: String? = null) {
    val geoeffnet = LokaleStimmen.sprachdatenIntents().any { runCatching { activity.startActivity(it) }.isSuccess }
    if (!geoeffnet) vm.message.value = "Öffne in den Android-Einstellungen „Sprachausgabe“ und lade dort " +
        (sprache?.let { Sprachen.name(it) } ?: "die gewünschte Sprache") + " herunter."
}

/** Eine Sprache in den Vorlese-Einstellungen: Status, bevorzugte Stimme, Probe – oder die Ladehandlung. */
@Composable
private fun SpracheStimmen(code: String, stimmen: List<LokaleStimmeInfo>, vm: WeckerViewModel, busy: String, activity: ComponentActivity) {
    val revision by vm.settingsRevision.collectAsStateWithLifecycle()
    var offen by rememberSaveable(code) { mutableStateOf(false) }
    val gewuenscht = remember(revision, code) { vm.settings.stimmeFuer(code) }
    val gewaehlt = stimmen.firstOrNull { it.name == gewuenscht } ?: stimmen.firstOrNull()
    Text(Sprachen.name(code), style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
    if (stimmen.isEmpty()) {
        Text("Keine Offline-Stimme installiert. Wecker auf ${Sprachen.name(code)} bekommen bis dahin den Ersatzweckton statt der Ansage.",
            style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
        StillerKnopf("Sprachdaten ${Sprachen.name(code)} laden", { oeffneSprachdaten(activity, vm, code) }, hervorgehoben = true)
        return
    }
    Text(if (stimmen.size == 1) "1 Offline-Stimme installiert" else "${stimmen.size} Offline-Stimmen installiert",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    GoldKnopf(gewaehlt?.anzeige ?: "Stimme auswählen", { offen = !offen }, Modifier.fillMaxWidth())
    if (offen) stimmen.forEach { stimme ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(stimme == gewaehlt, interactionSource = null, indication = null,
            role = androidx.compose.ui.semantics.Role.RadioButton) {
            vm.settings.setzeStimme(code, stimme.name); vm.settingsChanged(); offen = false
        }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(stimme == gewaehlt, null)
            Text(stimme.anzeige, Modifier.padding(start = 8.dp))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldKnopf("Probe anhören", { vm.previewVoice(sprache = code) }, aktiviert = busy.isBlank())
        StillerKnopf("Stoppen", vm::stopPreview)
    }
}
