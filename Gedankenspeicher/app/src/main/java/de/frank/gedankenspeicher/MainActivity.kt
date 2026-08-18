package de.frank.gedankenspeicher

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.gedankenspeicher.data.Auswertungsprofil
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.ui.Anmeldezustand
import de.frank.gedankenspeicher.ui.Eingabefrage
import de.frank.gedankenspeicher.ui.Rueckfrage
import de.frank.gedankenspeicher.ui.HauptViewModel
import de.frank.gedankenspeicher.ui.einstellungen.AnmeldungBildschirm
import de.frank.gedankenspeicher.ui.einstellungen.EinstellungenBildschirm
import de.frank.gedankenspeicher.ui.einstellungen.ProfilEditor
import de.frank.gedankenspeicher.ui.einstellungen.ProfileBildschirm
import de.frank.gedankenspeicher.ui.ki.KiBlatt
import de.frank.gedankenspeicher.ui.sitzungen.Abdunklung
import de.frank.gedankenspeicher.ui.sitzungen.Schublade
import de.frank.gedankenspeicher.ui.suche.SucheBildschirm
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Erscheinung
import de.frank.gedankenspeicher.ui.theme.GedankenspeicherTheme
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.dauer
import de.frank.gedankenspeicher.ui.verlauf.BearbeitenBlatt
import de.frank.gedankenspeicher.ui.verlauf.MenueBlatt
import de.frank.gedankenspeicher.ui.verlauf.Menueeintrag
import de.frank.gedankenspeicher.ui.verlauf.VerlaufBildschirm
import kotlinx.coroutines.launch

/** Die Anwendungsklasse — sie hält nichts, existiert aber für das Manifest. */
class GedankenspeicherApp : Application()

/** Welcher Bildschirm gerade oben liegt. */
private enum class Ziel { VERLAUF, EINSTELLUNGEN, PROFILE, ANMELDUNG, SUCHE }

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.foundation.ExperimentalFoundationApi
class MainActivity : ComponentActivity() {

    private lateinit var modell: HauptViewModel

    private val mikrofonFrage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
        if (erlaubt) {
            modell.mikrofonErlaubt()
            modell.aufnahmeUmschalten()
        } else {
            modell.mikrofonAbgelehnt()
            modell.melde("Ohne Mikrofon kann ich dich nicht hören.")
        }
    }

    /**
     * F-17 — der Ordner, in den gesichert wird.
     *
     * Über den Ordner-Wähler von Android statt über die Google-Drive-Schnittstelle: liegt
     * dort der Drive-Ordner, landet die Sicherung in Drive, ohne dass die App eine zweite
     * Anmeldung und ein zweites Zugriffsrecht braucht.
     */
    private val ordnerWahl = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            modell.ordnerwahlErledigt()
            return@registerForActivityResult
        }
        // Dauerhaft, sonst gilt das Recht nur bis zum nächsten App-Start und jede weitere
        // Sicherung fragt erneut nach dem Ordner.
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        modell.merkeSicherungsordner(uri)
    }

    private val antwortMikrofonFrage =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
            if (erlaubt) {
                modell.mikrofonErlaubt()
                modell.antwortAufnahmeUmschalten()
            } else {
                modell.mikrofonAbgelehnt()
                modell.melde("Ohne Mikrofon kann ich dich nicht hören.")
            }
        }

    private val stimmMikrofonFrage =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt ->
            if (erlaubt) {
                modell.mikrofonErlaubt()
                modell.nimmStimmeAuf()
            } else {
                modell.mikrofonAbgelehnt()
                modell.melde("Ohne Mikrofon kann ich deine Stimme nicht aufnehmen.")
            }
        }

    /** F-17 — die Sicherungsdatei, aus der wiederhergestellt wird. */
    private val dateiWahl = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) modell.dateiwahlErledigt() else modell.stelleWiederHerAus(uri)
    }

    /**
     * Nach dem Austausch der Datenbankdatei startet die App neu.
     *
     * Ein Neustart des Prozesses, nicht nur der Activity: die Room-Instanz, die Flows und
     * das ViewModel hängen alle an der alten Datei. Ein halber Neustart liesse Reste davon
     * stehen — und die zeigten weiter die Daten, die gerade ersetzt wurden.
     */
    fun starteNeu() {
        val absicht = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(absicht)
        finish()
        Runtime.getRuntime().exit(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: HauptViewModel = viewModel()
            modell = vm
            val erscheinungId by vm.erscheinung.collectAsStateWithLifecycle()

            GedankenspeicherTheme(erscheinung = Erscheinung.vonId(erscheinungId)) {
                Oberflaeche(
                    vm = vm,
                    beiMikrofon = { starteAufnahmeMitRecht(vm) },
                    beiAnmelden = { starteAnmeldung(vm) },
                    beiTeilen = { sitzung -> teileSitzung(vm, sitzung) },
                    beiOrdnerwahl = { ordnerWahl.launch(null) },
                    beiAntwortMikrofon = { starteAntwortAufnahmeMitRecht(vm) },
                    beiDateiwahl = { dateiWahl.launch(arrayOf("*/*")) },
                    beiNeustart = { starteNeu() },
                    beiStimmMikrofon = { starteStimmaufnahmeMitRecht(vm) },
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // `01-FUNKTIONS-SPEC.md` §6: eine laufende Aufnahme wird beendet wie ein zweiter
        // Tipp, die Sprachausgabe hört auf. Die Auswertung läuft weiter.
        if (::modell.isInitialized) modell.inDenHintergrund()
    }

    override fun onResume() {
        super.onResume()
        // F-04: wartende Aufnahmen nachreichen, sobald die App wieder vorn ist.
        if (::modell.isInitialized) modell.reicheWartendeNach()
    }

    /**
     * Fragt das Mikrofonrecht erst hier — beim ersten Druck auf den Aufnahmeknopf, nicht
     * beim Start (`00-PROJEKT.md` §3).
     */
    private fun starteAufnahmeMitRecht(vm: HauptViewModel) {
        val hat = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hat) vm.aufnahmeUmschalten() else mikrofonFrage.launch(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * F-09, Schritt 5 — die Antwort auf die Rückfrage einsprechen.
     *
     * Dasselbe Recht, derselbe Weg wie beim Notiz-Mikrofon: die Frage nach `RECORD_AUDIO`
     * kommt beim ersten Druck, nicht beim Start.
     */
    private fun starteAntwortAufnahmeMitRecht(vm: HauptViewModel) {
        val hat = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hat) {
            vm.antwortAufnahmeUmschalten()
        } else {
            antwortMikrofonFrage.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /** F-18 — eine eigene Stimme aufnehmen. Dasselbe Recht, derselbe Weg. */
    private fun starteStimmaufnahmeMitRecht(vm: HauptViewModel) {
        val hat = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hat) vm.nimmStimmeAuf() else stimmMikrofonFrage.launch(Manifest.permission.RECORD_AUDIO)
    }

    /** F-11 — der Gerätecode-Ablauf. Der Browser wird von `CodexAuthManager` selbst geöffnet. */
    private fun starteAnmeldung(vm: HauptViewModel) {
        lifecycleScope.launch {
            vm.anmeldungBeginnt()
            try {
                val ergebnis = vm.codex.login(this@MainActivity) { info ->
                    vm.anmeldungCodeDa(info.userCode, info.verificationUri)
                }
                vm.anmeldungErfolgreich(ergebnis.email)
            } catch (fehler: Exception) {
                vm.anmeldungFehlgeschlagen(fehler.message ?: "Die Anmeldung ist fehlgeschlagen.")
            }
        }
    }

    /** F-16 — die fertige Markdown-Datei über den Android-Teilen-Dialog weitergeben. */
    private fun teileSitzung(vm: HauptViewModel, sitzung: Sitzung) {
        lifecycleScope.launch {
            try {
                val datei = vm.exportdatei(sitzung)
                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "$packageName.dateien",
                    datei,
                )
                val absicht = Intent(Intent.ACTION_SEND).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, sitzung.titel)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(absicht, "Sitzung teilen"))
            } catch (fehler: Exception) {
                vm.melde(fehler.message ?: "Der Export ist fehlgeschlagen.")
            }
        }
    }
}

/**
 * **Die Oberfläche als Ganzes.**
 *
 * Ein einziger Baum statt eines Navigations-Grafen: die Bildschirme dieser App sind vier
 * Überlagerungen über einem Verlauf, und die Übergänge aus `03-MOTION-SPEC.md` §4 lassen
 * sich so genau so bauen, wie sie gemessen sind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
private fun Oberflaeche(
    vm: HauptViewModel,
    beiMikrofon: () -> Unit,
    beiAnmelden: () -> Unit,
    beiTeilen: (Sitzung) -> Unit,
    beiOrdnerwahl: () -> Unit,
    beiAntwortMikrofon: () -> Unit,
    beiDateiwahl: () -> Unit,
    beiNeustart: () -> Unit,
    beiStimmMikrofon: () -> Unit,
) {
    val ctx = LocalContext.current
    val verlauf by vm.verlauf.collectAsStateWithLifecycle()
    val kiBlatt by vm.kiBlatt.collectAsStateWithLifecycle()
    val suche by vm.suche.collectAsStateWithLifecycle()
    val bearbeitung by vm.bearbeitung.collectAsStateWithLifecycle()
    val anmeldung by vm.anmeldung.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle(emptyList())
    val erscheinungId by vm.erscheinung.collectAsStateWithLifecycle()
    val liestVor by vm.vorleser.laeuft.collectAsStateWithLifecycle()
    val codexVerbunden by vm.codexVerbunden.collectAsStateWithLifecycle()
    val codexKonto by vm.codexKonto.collectAsStateWithLifecycle()
    val codexModell by vm.codexModell.collectAsStateWithLifecycle()
    val codexEffort by vm.codexEffort.collectAsStateWithLifecycle()
    val websuche by vm.websucheGrundhaltung.collectAsStateWithLifecycle()
    val groq by vm.groqSchluessel.collectAsStateWithLifecycle()
    val ttsAnbieter by vm.ttsAnbieter.collectAsStateWithLifecycle()
    val ttsStimme by vm.ttsStimme.collectAsStateWithLifecycle()
    val googleSchluessel by vm.googleSchluessel.collectAsStateWithLifecycle()
    val qwenSchluessel by vm.qwenSchluessel.collectAsStateWithLifecycle()
    val eigeneStimmen by vm.eigeneStimmen.collectAsStateWithLifecycle()
    val stimmenLaden by vm.stimmenLaden.collectAsStateWithLifecycle()
    val nimmtStimmeAuf by vm.nimmtStimmeAuf.collectAsStateWithLifecycle()
    val driveAn by vm.driveAn.collectAsStateWithLifecycle()

    var ziel by remember { mutableStateOf(Ziel.VERLAUF) }
    var schubladeOffen by remember { mutableStateOf(false) }
    var notizMenue by remember { mutableStateOf<Notiz?>(null) }
    var antwortMenue by remember { mutableStateOf<KiAntwort?>(null) }
    var sitzungsMenue by remember { mutableStateOf<Sitzung?>(null) }
    var verschiebeNotiz by remember { mutableStateOf<Notiz?>(null) }
    var loeschfrage by remember { mutableStateOf<Notiz?>(null) }
    var sitzungLoeschfrage by remember { mutableStateOf<Sitzung?>(null) }
    var umbenennen by remember { mutableStateOf<Sitzung?>(null) }
    var profilEditor by remember { mutableStateOf<Auswertungsprofil?>(null) }
    val meldungen = remember { SnackbarHostState() }
    val bereich = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(verlauf.meldung) {
        verlauf.meldung?.let {
            meldungen.showSnackbar(it)
            vm.meldungGesehen()
        }
    }

    // Fehlt der Sicherungsordner, öffnet sich der Wähler — statt einer Meldung, die nur
    // sagt, dass etwas fehlt, aber keinen Weg dorthin zeigt.
    val ordnerFehlt by vm.sicherungsordnerFehlt.collectAsStateWithLifecycle()
    LaunchedEffect(ordnerFehlt) {
        if (ordnerFehlt) beiOrdnerwahl()
    }

    // Beim Betreten der Einstellungen die eigenen Stimmen holen — sonst steht dort
    // „noch keine Stimme aufgenommen", obwohl bei Alibaba welche liegen.
    LaunchedEffect(ziel, qwenSchluessel) {
        if (ziel == Ziel.EINSTELLUNGEN && qwenSchluessel.isNotBlank() && eigeneStimmen.isEmpty()) {
            vm.ladeEigeneStimmen()
        }
    }

    val sucheDatei by vm.sucheSicherungsdatei.collectAsStateWithLifecycle()
    LaunchedEffect(sucheDatei) {
        if (sucheDatei) beiDateiwahl()
    }

    val neustartNoetig by vm.neustartNoetig.collectAsStateWithLifecycle()

    // Die Zurückgeste, Ebene für Ebene (Aufgabe: „ich möchte nicht aus der App fliegen").
    //
    // Ohne diese Handler reicht Android die Geste an das System durch und beendet die
    // Activity — aus den Einstellungen flog man aus der App statt in den Verlauf. Die
    // Reihenfolge ist die des Aufbaus: erst die tiefste offene Ebene, zuletzt der Verlauf.
    // Auf dem Verlauf selbst wird **nicht** abgefangen: dort ist Zurück = App verlassen,
    // und das soll auch so bleiben.
    BackHandler(enabled = ziel == Ziel.PROFILE) { ziel = Ziel.EINSTELLUNGEN }
    BackHandler(enabled = ziel == Ziel.ANMELDUNG) { ziel = Ziel.EINSTELLUNGEN }
    BackHandler(enabled = ziel == Ziel.EINSTELLUNGEN) { ziel = Ziel.VERLAUF }
    BackHandler(enabled = ziel == Ziel.SUCHE) {
        vm.leereSuche()
        ziel = Ziel.VERLAUF
    }
    BackHandler(enabled = schubladeOffen && ziel == Ziel.VERLAUF) { schubladeOffen = false }
    // Wer beim Tippen zurückwischt, will das Feld schliessen — nicht die App verlassen.
    BackHandler(enabled = verlauf.bearbeiteteNotiz != null && ziel == Ziel.VERLAUF) {
        vm.beendeInlineBearbeitung()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Ab 400 dp ist das Innendisplay aufgeklappt — dann bekommt die Schublade ihre
        // breitere Fassung (320 statt 280 dp).
        val breit = maxWidth >= 400.dp

        VerlaufBildschirm(
            zustand = verlauf,
            istDunkel = Erscheinung.vonId(erscheinungId).farben.istDunkel,
            beiSchublade = { schubladeOffen = true },
            beiSuche = { ziel = Ziel.SUCHE },
            beiErscheinungUmschalten = vm::erscheinungUmschalten,
            beiEinstellungen = { ziel = Ziel.EINSTELLUNGEN },
            beiEntwurf = vm::setzeEntwurf,
            beiSenden = vm::sendeEntwurf,
            beiAufnahme = beiMikrofon,
            beiKi = vm::oeffneKiBlatt,
            beiVorlesen = vm::lesenUmschalten,
            beiVerbessern = vm::verbessere,
            beiRueckgaengig = vm::macheVerbesserungRueckgaengig,
            beiNotizMenue = { notizMenue = it },
            beiTippenImText = vm::beginneInlineBearbeitung,
            beiKartenEntwurf = vm::setzeInlineEntwurf,
            beiBearbeitenFertig = vm::beendeInlineBearbeitung,
            beiAntwortMenue = { antwortMenue = it },
            beiWiederholen = vm::versucheTranskriptionErneut,
        )

        // ---- Schublade (M-02)
        AnimatedVisibility(
            visible = schubladeOffen,
            enter = fadeIn(tween(dauer(Dauern.BLATT), easing = Kurven.blatt)),
            exit = fadeOut(tween(dauer(Dauern.BLATT), easing = Kurven.blatt)),
        ) {
            Abdunklung(staerke = 1f) { schubladeOffen = false }
        }
        AnimatedVisibility(
            visible = schubladeOffen,
            enter = slideInHorizontally(tween(dauer(Dauern.BLATT), easing = Kurven.blatt)) { -it },
            exit = slideOutHorizontally(tween(dauer(Dauern.BLATT), easing = Kurven.blatt)) { -it },
        ) {
            Schublade(
                sitzungen = verlauf.sitzungen,
                offeneSitzung = verlauf.sitzung?.id,
                breit = breit,
                beiWahl = { id ->
                    vm.wechsleSitzung(id)
                    schubladeOffen = false
                },
                beiNeue = {
                    vm.neueSitzung()
                    schubladeOffen = false
                },
                beiMenue = { sitzungsMenue = it },
                beiEinstellungen = {
                    schubladeOffen = false
                    ziel = Ziel.EINSTELLUNGEN
                },
            )
        }

        // ---- Die überlagernden Bildschirme, jeweils mit dem gemessenen Übergang
        SchiebtVonRechts(sichtbar = ziel == Ziel.EINSTELLUNGEN) {
            EinstellungenBildschirm(
                erscheinung = erscheinungId,
                codexVerbunden = codexVerbunden,
                codexKonto = codexKonto,
                codexModell = codexModell,
                codexEffort = codexEffort,
                websucheGrundhaltung = websuche,
                groqSchluessel = groq,
                ttsAnbieter = ttsAnbieter,
                ttsStimme = ttsStimme,
                googleSchluessel = googleSchluessel,
                qwenSchluessel = qwenSchluessel,
                eigeneStimmen = eigeneStimmen,
                stimmenLaden = stimmenLaden,
                nimmtStimmeAuf = nimmtStimmeAuf,
                probeLaeuft = liestVor,
                driveAn = driveAn,
                letzteSicherung = vm.einstellungen.letzteSicherungZeit,
                letzteGroesse = vm.einstellungen.letzteSicherungGroesse,
                beiErscheinung = vm::setzeErscheinung,
                beiVerbinden = {
                    ziel = Ziel.ANMELDUNG
                    beiAnmelden()
                },
                beiTrennen = vm::trenneCodex,
                beiModell = vm::setzeModell,
                beiEffort = vm::setzeEffort,
                beiWebsuche = vm::setzeWebsucheGrundhaltung,
                beiProfile = { ziel = Ziel.PROFILE },
                beiGroq = vm::setzeGroqSchluessel,
                beiAnbieter = vm::setzeTtsAnbieter,
                beiStimme = vm::setzeTtsStimme,
                beiGoogleSchluessel = vm::setzeGoogleSchluessel,
                beiQwenSchluessel = vm::setzeQwenSchluessel,
                beiStimmenLaden = vm::ladeEigeneStimmen,
                beiStimmeAufnehmen = beiStimmMikrofon,
                beiStimmeLoeschen = vm::loescheEigeneStimme,
                beiProbe = vm::spieleProbe,
                beiDrive = vm::setzeDrive,
                beiJetztSichern = vm::sichereJetzt,
                beiWiederherstellen = vm::stelleWiederHer,
                beiZurueck = { ziel = Ziel.VERLAUF },
            )
        }

        SchiebtVonRechts(sichtbar = ziel == Ziel.PROFILE) {
            ProfileBildschirm(
                profile = profile,
                beiAktivieren = { p ->
                    bereich.launch {
                        if (!vm.aktiviereProfil(p)) {
                            vm.melde("Ein leeres Profil lässt sich nicht aktivieren.")
                        }
                    }
                },
                beiBearbeiten = { profilEditor = it },
                beiZurueck = { ziel = Ziel.EINSTELLUNGEN },
            )
        }

        SchiebtVonRechts(sichtbar = ziel == Ziel.ANMELDUNG) {
            AnmeldungBildschirm(
                zustand = anmeldung,
                beiOeffnen = {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(anmeldung.adresse)))
                    }
                },
                beiKopieren = {
                    val ablage = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    ablage.setPrimaryClip(ClipData.newPlainText("Gerätecode", anmeldung.code))
                    vm.melde("Code kopiert.")
                },
                beiNeuerCode = beiAnmelden,
                beiZurueck = { ziel = Ziel.EINSTELLUNGEN },
            )
        }

        AnimatedVisibility(
            visible = ziel == Ziel.SUCHE,
            enter = fadeIn(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)),
            exit = fadeOut(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)),
        ) {
            SucheBildschirm(
                zustand = suche,
                beiBegriff = vm::setzeSuchbegriff,
                beiTreffer = { treffer ->
                    vm.springeZu(treffer.sitzungId, treffer.notizId)
                    vm.leereSuche()
                    ziel = Ziel.VERLAUF
                },
                beiZurueck = {
                    vm.leereSuche()
                    ziel = Ziel.VERLAUF
                },
            )
        }

        // ---- Blätter
        if (kiBlatt.offen) {
            ModalBottomSheet(
                onDismissRequest = vm::schliesseKiBlatt,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                KiBlatt(
                    zustand = kiBlatt,
                    nimmtAntwortAuf = verlauf.nimmtAuf,
                    beiGanzeSitzung = vm::setzeGanzeSitzung,
                    beiWebsuche = vm::setzeWebsuche,
                    beiWebsucheKi = vm::setzeWebsucheKiEntscheidet,
                    beiAntwort = vm::setzeKiAntwort,
                    beiAntwortEinsprechen = beiAntwortMikrofon,
                    beiProfil = {
                        vm.schliesseKiBlatt()
                        ziel = Ziel.PROFILE
                    },
                    beiAuswerten = vm::werteAus,
                    beiVerbinden = {
                        vm.schliesseKiBlatt()
                        ziel = Ziel.ANMELDUNG
                        beiAnmelden()
                    },
                    beiErneut = vm::holeRueckfrageErneut,
                )
            }
        }

        bearbeitung.notiz?.let {
            ModalBottomSheet(
                onDismissRequest = vm::schliesseBearbeitung,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                BearbeitenBlatt(
                    zustand = bearbeitung,
                    beiAenderung = vm::setzeBearbeitung,
                    beiAbbrechen = vm::schliesseBearbeitung,
                    beiSpeichern = vm::speichereBearbeitung,
                )
            }
        }

        profilEditor?.let { profil ->
            var name by remember(profil.nummer) { mutableStateOf(profil.name) }
            var anweisung by remember(profil.nummer) { mutableStateOf(profil.anweisung) }
            ModalBottomSheet(
                onDismissRequest = { profilEditor = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                ProfilEditor(
                    name = name,
                    anweisung = anweisung,
                    beiName = { name = it },
                    beiAnweisung = { anweisung = it },
                    beiZuruecksetzen = {
                        vm.setzeProfilZurueck(profil)
                        profilEditor = null
                    },
                    beiSpeichern = {
                        vm.speichereProfil(profil.copy(name = name.ifBlank { profil.name }, anweisung = anweisung))
                        profilEditor = null
                    },
                )
            }
        }

        // ---- Menüs zum langen Druck (F-08, F-12)
        notizMenue?.let { notiz ->
            ModalBottomSheet(
                onDismissRequest = { notizMenue = null },
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                MenueBlatt(titel = notiz.ueberschrift) {
                    Menueeintrag("Text bearbeiten") {
                        vm.oeffneBearbeitung(notiz)
                        notizMenue = null
                    }
                    Menueeintrag("In andere Sitzung verschieben", gesperrt = verlauf.sitzungen.size < 2) {
                        verschiebeNotiz = notiz
                        notizMenue = null
                    }
                    Menueeintrag("Text kopieren") {
                        val ablage = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        ablage.setPrimaryClip(ClipData.newPlainText("Notiz", notiz.text))
                        vm.melde("Text kopiert.")
                        notizMenue = null
                    }
                    Menueeintrag("Notiz löschen", gefaehrlich = true) {
                        loeschfrage = notiz
                        notizMenue = null
                    }
                }
            }
        }

        antwortMenue?.let { antwort ->
            ModalBottomSheet(
                onDismissRequest = { antwortMenue = null },
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                MenueBlatt(titel = "Auswertung") {
                    Menueeintrag("Text kopieren") {
                        val ablage = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        ablage.setPrimaryClip(ClipData.newPlainText("Auswertung", antwort.text))
                        vm.melde("Text kopiert.")
                        antwortMenue = null
                    }
                    Menueeintrag("Auswertung löschen", gefaehrlich = true) {
                        vm.loescheAntwort(antwort)
                        antwortMenue = null
                    }
                }
            }
        }

        sitzungsMenue?.let { sitzung ->
            ModalBottomSheet(
                onDismissRequest = { sitzungsMenue = null },
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                MenueBlatt(titel = sitzung.titel) {
                    Menueeintrag("Umbenennen") {
                        umbenennen = sitzung
                        sitzungsMenue = null
                    }
                    Menueeintrag("Als Markdown exportieren") {
                        beiTeilen(sitzung)
                        sitzungsMenue = null
                    }
                    Menueeintrag("Sitzung löschen", gefaehrlich = true) {
                        sitzungLoeschfrage = sitzung
                        sitzungsMenue = null
                    }
                }
            }
        }

        verschiebeNotiz?.let { notiz ->
            ModalBottomSheet(
                onDismissRequest = { verschiebeNotiz = null },
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
            ) {
                MenueBlatt(titel = "In welche Sitzung?") {
                    verlauf.sitzungen.forEach { s ->
                        Menueeintrag(s.titel, gesperrt = s.id == notiz.sitzungId) {
                            vm.verschiebeNotiz(notiz, s.id)
                            verschiebeNotiz = null
                        }
                    }
                }
            }
        }

        // ---- Rückfragen
        loeschfrage?.let { notiz ->
            Rueckfrage(
                titel = "Diese Notiz löschen?",
                text = "Das lässt sich nicht rückgängig machen.",
                bestaetigung = "Löschen",
                beiJa = {
                    vm.loescheNotiz(notiz)
                    loeschfrage = null
                },
                beiNein = { loeschfrage = null },
            )
        }

        sitzungLoeschfrage?.let { sitzung ->
            val anzahl by vm.repo.notizzahl(sitzung.id).collectAsStateWithLifecycle(0)
            Rueckfrage(
                titel = "Sitzung mit $anzahl ${if (anzahl == 1) "Notiz" else "Notizen"} löschen?",
                text = "Das lässt sich nicht rückgängig machen.",
                bestaetigung = "Löschen",
                beiJa = {
                    vm.loescheSitzung(sitzung)
                    sitzungLoeschfrage = null
                },
                beiNein = { sitzungLoeschfrage = null },
            )
        }

        umbenennen?.let { sitzung ->
            var neuerName by remember(sitzung.id) { mutableStateOf(sitzung.titel) }
            Eingabefrage(
                titel = "Sitzung umbenennen",
                wert = neuerName,
                beiAenderung = { neuerName = it },
                beiJa = {
                    vm.benenneSitzungUm(sitzung.id, neuerName)
                    umbenennen = null
                },
                beiNein = { umbenennen = null },
            )
        }

        if (neustartNoetig) {
            Rueckfrage(
                titel = "Wiederhergestellt",
                text = "Die App startet jetzt neu, damit die wiederhergestellten Notizen geladen werden.",
                bestaetigung = "Neu starten",
                beiJa = beiNeustart,
                // Kein Weg daran vorbei: die Datenbankdatei ist schon ausgetauscht, die
                // laufende App zeigt einen Stand, den es nicht mehr gibt.
                beiNein = beiNeustart,
            )
        }

        SnackbarHost(
            hostState = meldungen,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }
}

/** Der Übergang „schiebt von rechts herein" aus `03-MOTION-SPEC.md` §4. */
@Composable
private fun SchiebtVonRechts(sichtbar: Boolean, inhalt: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = sichtbar,
        enter = slideInHorizontally(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)) { it },
        exit = slideOutHorizontally(tween(dauer(Dauern.STANDARD), easing = Kurven.standard)) { it },
    ) {
        inhalt()
    }
}
