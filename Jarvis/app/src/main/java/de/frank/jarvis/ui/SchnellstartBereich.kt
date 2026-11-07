package de.frank.jarvis.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Welche von Franks eigenen Apps im Schnellstart stehen, in seiner Reihenfolge. Gespeichert wird nur die Liste der Pakete:
 * Entfernen nimmt die Verknüpfung heraus, die App selbst und ihre Daten bleiben unberührt.
 */
object Schnellstart {
    private const val DATEI = "schnellstart"
    private const val SCHLUESSEL = "pakete"
    val VORGABE = listOf("de.frank.aufgaben", "de.frank.genialeideen")

    /** Franks eigene Apps erkennt Jarvis an ihren Paketnamen (genau dieser Name oder darunter). */
    private val EIGENE = listOf("de.frank", "com.bestjournal", "com.entropyjournal")

    /** Gespeicherte Auswahl; lebt über den Startbildschirm hinaus, damit Scrollen nichts neu lädt. */
    private var auswahl: SnapshotStateList<String>? = null

    /** Symbole und Namen der Apps, einmal geladen und bei jeder Rückkehr in die App frisch abgeglichen. */
    val symbole = mutableStateOf(mapOf<String, AppEintrag>())

    fun auswahl(context: Context): SnapshotStateList<String> = auswahl ?: mutableStateListOf<String>().also { liste ->
        val p = context.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        liste.addAll(if (!p.contains(SCHLUESSEL)) VORGABE else p.getString(SCHLUESSEL, "").orEmpty().split(',').filter { it.isNotBlank() })
        auswahl = liste
    }

    fun speichern(context: Context, pakete: List<String>) {
        context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, pakete.joinToString(",")).apply()
    }

    fun istEigene(paket: String, eigenesPaket: String): Boolean =
        paket != eigenesPaket && !paket.startsWith("$eigenesPaket.") && EIGENE.any { paket == it || paket.startsWith("$it.") }

    /** Alle installierten, startbaren eigenen Apps außer Jarvis und außer [ausser], alphabetisch nach Namen. */
    fun installierte(context: Context, ausser: Set<String>): List<AppEintrag> {
        val pm = context.packageManager
        val start = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(start, 0) }.getOrDefault(emptyList())
            .map { it.activityInfo.packageName }.distinct()
            .filter { it !in ausser && istEigene(it, context.packageName) }
            .mapNotNull { symbole.value[it] ?: eintrag(context, it) }
            .sortedBy { it.name.lowercase() }
    }

    fun eintrag(context: Context, paket: String): AppEintrag? = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(paket, 0)
        if (pm.getLaunchIntentForPackage(paket) == null) return null
        AppEintrag(paket, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info).toBitmap(144, 144).asImageBitmap())
    }.getOrNull()

    fun starten(context: Context, paket: String): Boolean = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(paket) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}

class AppEintrag(val paket: String, val name: String, val symbol: ImageBitmap)

/** Leichtes Wackeln im Bearbeitungszustand; läuft nur, solange es gebraucht wird. */
@Composable
private fun wackelWinkel(): Float {
    val wackeln = rememberInfiniteTransition(label = "wackeln")
    val winkel by wackeln.animateFloat(-2.6f, 2.6f, infiniteRepeatable(tween(130), RepeatMode.Reverse), label = "winkel")
    return winkel
}

/**
 * Schnellstart auf dem Startbildschirm: Franks eigene Apps als Symbole, Antippen startet sie. Langes Drücken lässt alle Symbole
 * wackeln und blendet darunter den Bereich „Entfernen“ ein; ein dorthin gezogenes Symbol verschwindet nur aus dem Schnellstart.
 * Über andere Symbole gezogen, rückt es live an deren Platz; beim Loslassen wird die neue Reihenfolge gespeichert.
 * Der runde Plus-Knopf bietet alle übrigen installierten eigenen Apps an.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SchnellstartBereich(vm: AppViewModel) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val haptik = LocalHapticFeedback.current
    val bewegung = LocalBewegung.current
    val scope = rememberCoroutineScope()
    val pakete = remember { Schnellstart.auswahl(context) }
    val eintraege by Schnellstart.symbole
    // Bei jeder Rückkehr in die App neu abgleichen: frisch installierte Apps erscheinen, deinstallierte verschwinden, neue Symbole kommen.
    var rueckkehr by remember { mutableIntStateOf(0) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { rueckkehr++; onPauseOrDispose { } }
    LaunchedEffect(pakete.toList(), rueckkehr) {
        val liste = pakete.toList()
        val frisch = withContext(Dispatchers.IO) { liste.mapNotNull { Schnellstart.eintrag(context, it) }.associateBy { it.paket } }
        Schnellstart.symbole.value = Schnellstart.symbole.value.filterKeys { it !in liste } + frisch
    }
    var bearbeiten by rememberSaveable { mutableStateOf(false) }
    var auswahlOffen by rememberSaveable { mutableStateOf(false) }
    var ziehend by remember { mutableStateOf<String?>(null) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    var ziehMitte by remember { mutableStateOf(Offset.Zero) }
    var zone by remember { mutableStateOf(Rect.Zero) }
    val verschwindend = remember { mutableStateListOf<String>() }
    var neu by remember { mutableStateOf<String?>(null) }
    // Umordnen: Lage jedes Symbols (ohne Zieh-Versatz, aus dem letzten Layout). Nach einem Platzwechsel wird erst wieder
    // getauscht, wenn das neue Layout die Lagen aufgefrischt hat; sonst würde das Symbol sofort zurückspringen.
    val lagen = remember { mutableMapOf<String, Rect>() }
    var umgeordnet by remember { mutableStateOf(false) }
    var wartetAufLayout by remember { mutableStateOf(false) }

    /** Rückt das gezogene Symbol live an den Platz unter dem Finger; es bleibt dabei unter dem Finger. */
    fun umordnen(paket: String) {
        if (wartetAufLayout) return
        val finger = ziehMitte + versatz
        val reihe = pakete.filter { it in eintraege && it !in verschwindend }
        val von = reihe.indexOf(paket)
        val nach = reihe.indexOfFirst { p -> p != paket && lagen[p]?.contains(finger) == true }
        if (von < 0 || nach < 0) return
        val zielMitte = lagen[reihe[nach]]?.center ?: return
        val neuReihe = reihe.toMutableList().apply { add(nach, removeAt(von)) }
        // Nicht installierte oder gerade verschwindende Apps behalten ihre Stelle, nur die sichtbaren wechseln die Plätze.
        val iter = neuReihe.iterator()
        val ganz = pakete.map { if (it in eintraege && it !in verschwindend) iter.next() else it }
        pakete.clear(); pakete.addAll(ganz)
        ziehMitte = zielMitte
        versatz = finger - zielMitte
        umgeordnet = true
        wartetAufLayout = true
        haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val ueberZone by remember {
        derivedStateOf { ziehend != null && zone != Rect.Zero && zone.inflate(12f).contains(ziehMitte + versatz) }
    }
    LaunchedEffect(ueberZone) { if (ueberZone) haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(bearbeiten) { if (!bearbeiten) zone = Rect.Zero }
    val sichtbar = pakete.filter { it in eintraege && it !in verschwindend }
    LaunchedEffect(sichtbar.isEmpty()) { if (sichtbar.isEmpty() && verschwindend.isEmpty()) bearbeiten = false }

    // Sofort speichern, danach weich ausblenden; mehrere Entfernungen hintereinander stören sich nicht.
    fun entfernen(paket: String) {
        if (paket in verschwindend) return
        verschwindend.add(paket)
        Schnellstart.speichern(context, pakete.filter { it != paket })
        scope.launch {
            delay(230)
            pakete.remove(paket)
            verschwindend.remove(paket)
        }
    }
    fun starten(e: AppEintrag) {
        if (!Schnellstart.starten(context, e.paket)) vm.meldung = "${e.name} ließ sich nicht starten."
    }

    Karte {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Schnellstart", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (bearbeiten) "Ziehen zum Umordnen, nach unten zum Entfernen" else "Deine Apps mit einem Tipp öffnen",
                    color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            AnimatedVisibility(bearbeiten, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "Fertig", Modifier.clip(RoundedCornerShape(14.dp)).antippen { bearbeiten = false }.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = f.primaer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        val winkel = if (bearbeiten && bewegung) wackelWinkel() else 0f
        FlowRow(
            // Beim Ziehen liegt die ganze Reihe über dem Löschbereich, damit das Symbol sichtbar darüber schwebt.
            Modifier.fillMaxWidth().zIndex(if (ziehend != null) 1f else 0f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pakete.forEachIndexed { index, paket ->
                val e = eintraege[paket] ?: return@forEachIndexed
                androidx.compose.runtime.key(paket) {
                    val ziehtMich = ziehend == paket
                    val aktuell by rememberUpdatedState(e)
                    val imBearbeiten by rememberUpdatedState(bearbeiten)
                    val gehtWeg = paket in verschwindend
                    val gehtWegAktuell by rememberUpdatedState(gehtWeg)
                    // Mitte des Symbols ohne Neuaufbau mitführen (kein State: ändert sich bei jedem Scrollschritt).
                    val mitte = remember { arrayOf(Offset.Zero) }
                    // Erscheinen (neu hinzugefügt) und Verschwinden (entfernt) federn weich.
                    val auftritt = remember { Animatable(if (neu == paket) 0f else 1f) }
                    LaunchedEffect(Unit) { if (auftritt.value < 1f) auftritt.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
                    val weg by animateFloatAsState(if (gehtWeg) 0f else 1f, tween(220), label = "weg")
                    val gross by animateFloatAsState(if (ziehtMich) (if (ueberZone) 0.86f else 1.14f) else 1f, spring(dampingRatio = 0.6f), label = "gross")
                    // Jedes zweite Symbol wackelt gegenläufig, damit es lebendig und nicht mechanisch wirkt.
                    val phase = if (index % 2 == 0) 1f else -1f
                    Column(
                        Modifier.width(76.dp)
                            .zIndex(if (ziehtMich) 1f else 0f)
                            // Lage und Gesten vor Verschiebung, Skalierung und Wackeln: so laufen Finger und Symbol genau gleich.
                            .onGloballyPositioned {
                                val b = it.boundsInRoot()
                                val c = b.center
                                mitte[0] = c
                                lagen[paket] = b
                                if (ziehend == paket) {
                                    // Nach einem Platzwechsel bleibt der Finger, wo er ist: Versatz an die neue Lage anpassen.
                                    if (wartetAufLayout) { versatz = ziehMitte + versatz - c; wartetAufLayout = false }
                                    // Scrollt die Liste während des Ziehens, wandert der Bezugspunkt mit (wie der Löschbereich).
                                    ziehMitte = c
                                }
                            }
                            .pointerInput(paket) {
                                detectTapGestures {
                                    if (gehtWegAktuell) return@detectTapGestures
                                    if (imBearbeiten) bearbeiten = false else starten(aktuell)
                                }
                            }
                            .pointerInput(paket) {
                                // Immer erst nach langem Drücken: So bleibt der Startbildschirm über den Symbolen scrollbar,
                                // und der Bearbeitungszustand startet die Geste nicht mitten im Ziehen neu.
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (gehtWegAktuell) return@detectDragGesturesAfterLongPress
                                        versatz = Offset.Zero
                                        ziehMitte = mitte[0]
                                        umgeordnet = false
                                        wartetAufLayout = false
                                        ziehend = paket
                                        haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                                        bearbeiten = true
                                    },
                                    onDragEnd = {
                                        if (ziehend == paket && zone != Rect.Zero && zone.inflate(12f).contains(ziehMitte + versatz)) {
                                            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                                            entfernen(paket)
                                        } else if (ziehend == paket && umgeordnet) Schnellstart.speichern(context, pakete.toList())
                                        if (ziehend == paket) { ziehend = null; versatz = Offset.Zero; umgeordnet = false }
                                    },
                                    onDragCancel = {
                                        // Auch abgebrochen bleibt die schon gezeigte Reihenfolge stehen und wird gespeichert.
                                        if (ziehend == paket && umgeordnet) Schnellstart.speichern(context, pakete.toList())
                                        if (ziehend == paket) { ziehend = null; versatz = Offset.Zero; umgeordnet = false }
                                    },
                                    onDrag = { c, d ->
                                        if (ziehend == paket) {
                                            c.consume(); versatz += d
                                            if (!(zone != Rect.Zero && zone.inflate(12f).contains(ziehMitte + versatz))) umordnen(paket)
                                        }
                                    },
                                )
                            }
                            .graphicsLayer {
                                translationX = if (ziehtMich) versatz.x else 0f
                                translationY = if (ziehtMich) versatz.y else 0f
                                val s = auftritt.value * weg * gross
                                scaleX = s; scaleY = s
                                alpha = (auftritt.value * weg).coerceIn(0f, 1f)
                                rotationZ = if (!ziehtMich) winkel * phase else 0f
                            }
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                onClick("${e.name} öffnen") { starten(aktuell); true }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(58.dp).glas(f, 18.dp, erhoeht = if (ziehtMich) 1.4f else 0.6f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(e.symbol, null, Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)))
                        }
                        Text(
                            e.name, Modifier.padding(top = 6.dp), color = f.text, fontSize = 12.sp, lineHeight = 14.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            // Runder Plus-Knopf: weitere eigene Apps hinzufügen.
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(58.dp).glas(f, 29.dp, erhoeht = 0.6f, toenung = f.primaer)
                        .antippen { bearbeiten = false; auswahlOffen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, "App zum Schnellstart hinzufügen", tint = f.primaer, modifier = Modifier.size(28.dp))
                }
                Text("Hinzufügen", Modifier.padding(top = 6.dp), color = f.textLeise, fontSize = 12.sp, maxLines = 1)
            }
        }
        // Löschbereich: erscheint im Bearbeitungszustand, leuchtet rot auf, sobald ein Symbol darüber schwebt.
        AnimatedVisibility(bearbeiten, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val aktiv = ueberZone
            val hg by animateColorAsState(if (aktiv) f.gefahr.copy(alpha = 0.22f) else f.gefahr.copy(alpha = 0.07f), label = "zoneHg")
            val randFarbe by animateColorAsState(if (aktiv) f.gefahr else f.gefahr.copy(alpha = 0.45f), label = "zoneRand")
            val zoneGross by animateFloatAsState(if (aktiv) 1.04f else 1f, spring(dampingRatio = 0.5f), label = "zoneGross")
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(64.dp)
                    // Gemessen vor dem Aufblähen: die Trefferfläche hängt nicht vom Schwebezustand ab.
                    .onGloballyPositioned { zone = it.boundsInRoot() }
                    .graphicsLayer { scaleX = zoneGross; scaleY = zoneGross }
                    .clip(RoundedCornerShape(20.dp)).background(hg)
                    .border(1.5.dp, randFarbe, RoundedCornerShape(20.dp)),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Delete, null, tint = f.gefahr, modifier = Modifier.size(if (aktiv) 26.dp else 22.dp))
                Text(
                    if (aktiv) "Loslassen zum Entfernen" else "Hierher ziehen: aus dem Schnellstart entfernen",
                    Modifier.padding(start = 10.dp), color = f.gefahr, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (bearbeiten) Text(
            "Die App selbst und ihre Daten bleiben erhalten.",
            Modifier.padding(top = 8.dp).fillMaxWidth(), color = f.textLeise, fontSize = 12.sp, textAlign = TextAlign.Center,
        )
    }

    if (auswahlOffen) {
        val blatt = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var gewaehlt by remember { mutableStateOf(false) }
        val angebot by androidx.compose.runtime.produceState<List<AppEintrag>?>(null) {
            val schon = pakete.toSet()
            value = withContext(Dispatchers.IO) { Schnellstart.installierte(context, schon) }
        }
        ModalBottomSheet(onDismissRequest = { auswahlOffen = false }, sheetState = blatt, containerColor = menueFlaeche(f)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("App hinzufügen", color = f.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Deine eigenen Apps auf diesem Handy.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                val liste = angebot
                when {
                    liste == null -> Text("Wird gesucht …", color = f.textLeise, fontSize = 15.sp)
                    liste.isEmpty() -> Text("Alle deine Apps stehen schon im Schnellstart.", color = f.textLeise, fontSize = 15.sp)
                    else -> LazyColumn(Modifier.fillMaxWidth().height((liste.size * 70).coerceAtMost(460).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liste, key = { it.paket }) { a ->
                            Row(
                                Modifier.fillMaxWidth().glas(f, 16.dp, erhoeht = 0.3f).antippen {
                                    // Nur der erste Tipp zählt: ein schneller Doppeltipp fügt keine zweite App hinzu.
                                    if (gewaehlt) return@antippen
                                    gewaehlt = true
                                    Schnellstart.symbole.value = Schnellstart.symbole.value + (a.paket to a)
                                    neu = a.paket
                                    if (a.paket !in pakete) pakete.add(a.paket)
                                    Schnellstart.speichern(context, pakete.toList())
                                    scope.launch { blatt.hide() }.invokeOnCompletion { auswahlOffen = false }
                                }.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(a.symbol, null, Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)))
                                Text(a.name, Modifier.weight(1f).padding(start = 14.dp), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Box(Modifier.size(32.dp).clip(CircleShape).background(f.primaer.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Add, "Hinzufügen", tint = f.primaer, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
