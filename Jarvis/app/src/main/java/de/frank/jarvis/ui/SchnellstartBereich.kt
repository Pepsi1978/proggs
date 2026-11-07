package de.frank.jarvis.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Welche von Franks eigenen Apps im Schnellstart stehen, in seiner Reihenfolge. Gespeichert wird nur die Liste der Pakete:
 * Entfernen nimmt die Verknüpfung heraus, die App selbst und ihre Daten bleiben unberührt.
 */
object Schnellstart {
    private const val DATEI = "schnellstart"
    private const val SCHLUESSEL = "pakete"
    val VORGABE = listOf("de.frank.aufgaben", "de.frank.genialeideen")

    /** Franks eigene Apps erkennt Jarvis an ihren Paketnamen. */
    private val EIGENE = listOf("de.frank.", "com.bestjournal.", "com.entropyjournal")

    fun laden(context: Context): List<String> {
        val p = context.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        if (!p.contains(SCHLUESSEL)) return VORGABE
        return p.getString(SCHLUESSEL, "").orEmpty().split(',').filter { it.isNotBlank() }
    }

    fun speichern(context: Context, pakete: List<String>) {
        context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, pakete.joinToString(",")).apply()
    }

    fun istEigene(paket: String, eigenesPaket: String): Boolean =
        paket != eigenesPaket && !paket.startsWith("$eigenesPaket.") && EIGENE.any { paket.startsWith(it) || paket == it }

    /** Alle installierten, startbaren eigenen Apps (ohne Jarvis), alphabetisch nach Namen. */
    fun installierte(context: Context): List<AppEintrag> {
        val pm = context.packageManager
        val start = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(start, 0) }.getOrDefault(emptyList())
            .map { it.activityInfo.packageName }.distinct()
            .filter { istEigene(it, context.packageName) }
            .mapNotNull { eintrag(context, it) }
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

/**
 * Schnellstart auf dem Startbildschirm: Franks eigene Apps als Symbole, Antippen startet sie. Langes Drücken lässt alle Symbole
 * wackeln und blendet darunter den Bereich „Entfernen“ ein; ein dorthin gezogenes Symbol verschwindet nur aus dem Schnellstart.
 * Der runde Plus-Knopf bietet alle übrigen installierten eigenen Apps an.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SchnellstartBereich(vm: AppViewModel) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val haptik = LocalHapticFeedback.current
    val bewegung = LocalBewegung.current
    val pakete = remember { mutableStateListOf<String>().apply { addAll(Schnellstart.laden(context)) } }
    // Symbole und Namen einmal laden; nicht (mehr) installierte Apps stehen nicht da, bleiben aber gespeichert.
    val eintraege = remember { mutableStateOf(mapOf<String, AppEintrag>()) }
    LaunchedEffect(pakete.toList()) {
        val fehlend = pakete.filter { it !in eintraege.value }
        if (fehlend.isNotEmpty()) eintraege.value = eintraege.value + kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            fehlend.mapNotNull { Schnellstart.eintrag(context, it) }.associateBy { it.paket }
        }
    }
    var bearbeiten by remember { mutableStateOf(false) }
    var auswahlOffen by remember { mutableStateOf(false) }
    var ziehend by remember { mutableStateOf<String?>(null) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    var startMitte by remember { mutableStateOf(Offset.Zero) }
    var zone by remember { mutableStateOf(Rect.Zero) }
    var verschwindet by remember { mutableStateOf<String?>(null) }
    var neu by remember { mutableStateOf<String?>(null) }
    val ueberZone = ziehend != null && zone != Rect.Zero && zone.inflate(12f).contains(startMitte + versatz)
    LaunchedEffect(ueberZone) { if (ueberZone) haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    fun entfernen(paket: String) {
        verschwindet = paket
    }
    LaunchedEffect(verschwindet) {
        val p = verschwindet ?: return@LaunchedEffect
        delay(230)
        pakete.remove(p)
        Schnellstart.speichern(context, pakete.toList())
        verschwindet = null
        if (pakete.isEmpty()) bearbeiten = false
    }

    Karte {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Schnellstart", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (bearbeiten) "Zum Entfernen nach unten ziehen" else "Deine Apps mit einem Tipp öffnen",
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
        val wackeln = rememberInfiniteTransition(label = "wackeln")
        val winkel by wackeln.animateFloat(-2.6f, 2.6f, infiniteRepeatable(tween(130), RepeatMode.Reverse), label = "winkel")
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pakete.forEachIndexed { index, paket ->
                val e = eintraege.value[paket] ?: return@forEachIndexed
                androidx.compose.runtime.key(paket) {
                    val ziehtMich = ziehend == paket
                    var lage by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    // Erscheinen (neu hinzugefügt) und Verschwinden (entfernt) federn weich.
                    val auftritt = remember { Animatable(if (neu == paket) 0f else 1f) }
                    LaunchedEffect(Unit) { if (auftritt.value < 1f) auftritt.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
                    val weg by animateFloatAsState(if (verschwindet == paket) 0f else 1f, tween(220), label = "weg")
                    val gross by animateFloatAsState(if (ziehtMich) (if (ueberZone) 0.86f else 1.14f) else 1f, spring(dampingRatio = 0.6f), label = "gross")
                    // Jedes Symbol wackelt leicht versetzt, damit es lebendig und nicht mechanisch wirkt.
                    val phase = if (index % 2 == 0) 1f else -1f
                    Column(
                        Modifier.width(76.dp)
                            .zIndex(if (ziehtMich) 1f else 0f)
                            .onGloballyPositioned { lage = it }
                            .graphicsLayer {
                                translationX = if (ziehtMich) versatz.x else 0f
                                translationY = if (ziehtMich) versatz.y else 0f
                                val s = auftritt.value * weg * gross
                                scaleX = s; scaleY = s
                                alpha = (auftritt.value * weg).coerceIn(0f, 1f)
                                rotationZ = if (bearbeiten && bewegung && !ziehtMich) winkel * phase else 0f
                            }
                            .pointerInput(paket, bearbeiten) {
                                detectTapGestures {
                                    if (bearbeiten) bearbeiten = false
                                    else if (!Schnellstart.starten(context, paket)) vm.meldung = "${e.name} ließ sich nicht starten."
                                }
                            }
                            .pointerInput(paket, bearbeiten) {
                                val beginn: (Offset) -> Unit = {
                                    lage?.boundsInRoot()?.let { b -> startMitte = b.center }
                                    versatz = Offset.Zero
                                    ziehend = paket
                                    if (!bearbeiten) haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                                    bearbeiten = true
                                }
                                val ende: () -> Unit = {
                                    // Frisch aus den Zuständen lesen: der Wert aus der Komposition wäre in diesem Block veraltet.
                                    val drauf = zone != Rect.Zero && zone.inflate(12f).contains(startMitte + versatz)
                                    if (drauf) { haptik.performHapticFeedback(HapticFeedbackType.LongPress); entfernen(paket) }
                                    ziehend = null
                                    versatz = Offset.Zero
                                }
                                val ziehen: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit = { c, d -> c.consume(); versatz += d }
                                if (bearbeiten) detectDragGestures(onDragStart = beginn, onDragEnd = ende, onDragCancel = ende, onDrag = ziehen)
                                else detectDragGesturesAfterLongPress(onDragStart = beginn, onDragEnd = ende, onDragCancel = ende, onDrag = ziehen)
                            }
                            .semantics { contentDescription = "${e.name} öffnen" },
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
                        .antippen { bearbeiten = false; auswahlOffen = true }
                        .semantics { contentDescription = "App zum Schnellstart hinzufügen" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = f.primaer, modifier = Modifier.size(28.dp))
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
                    .graphicsLayer { scaleX = zoneGross; scaleY = zoneGross }
                    .onGloballyPositioned { zone = it.boundsInRoot() }
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
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val angebot by androidx.compose.runtime.produceState<List<AppEintrag>?>(null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Schnellstart.installierte(context) }
        }
        ModalBottomSheet(onDismissRequest = { auswahlOffen = false }, containerColor = menueFlaeche(f)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("App hinzufügen", color = f.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Deine eigenen Apps auf diesem Handy.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                val liste = angebot?.filter { it.paket !in pakete }
                when {
                    liste == null -> Text("Wird gesucht …", color = f.textLeise, fontSize = 15.sp)
                    liste.isEmpty() -> Text("Alle deine Apps stehen schon im Schnellstart.", color = f.textLeise, fontSize = 15.sp)
                    else -> LazyColumn(Modifier.fillMaxWidth().height((liste.size * 70).coerceAtMost(460).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liste, key = { it.paket }) { a ->
                            Row(
                                Modifier.fillMaxWidth().glas(f, 16.dp, erhoeht = 0.3f).antippen {
                                    eintraege.value = eintraege.value + (a.paket to a)
                                    neu = a.paket
                                    pakete.add(a.paket)
                                    Schnellstart.speichern(context, pakete.toList())
                                    scope.launch { auswahlOffen = false }
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
