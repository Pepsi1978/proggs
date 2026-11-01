package de.frank.jarvis.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.ablage.AblageSuche
import de.frank.jarvis.ablage.AblageSucheZustand
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.ablage.Auszug
import de.frank.jarvis.ablage.Eintrag
import de.frank.jarvis.ablage.Erinnerungen
import de.frank.jarvis.ablage.Prompts
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val JE_DOKUMENT = 100_000
/** Mehr Text hält die Suche nicht im Speicher; ältere Einträge darüber hinaus sind nur noch über Titel und Dateinamen auffindbar. */
private const val TEXT_GESAMT = 16_000_000

/** Der lesbare Inhalt eines Eintrags: sein Text und der Text seiner Text- und Office-Dateien (PDF und Medien nur über Namen und Beschreibung). */
private fun suchInhalt(s: AblageSpeicher, e: Eintrag): String = buildString {
    append(s.text(e, JE_DOKUMENT))
    for (a in e.anhaenge) {
        if (length >= JE_DOKUMENT) break
        val text = runCatching {
            when (a.art) {
                Art.TEXT, Art.MARKDOWN, Art.CODE, Art.DATEN, Art.TABELLE_TEXT -> Auszug.lies(s.datei(a), JE_DOKUMENT).text
                Art.HTML -> AblageSuche.ohneAuszeichnung(Auszug.lies(s.datei(a)).text)
                Art.DOKUMENT, Art.TABELLE, Art.PRAESENTATION -> Auszug.office(s.datei(a), a.endung)
                else -> null
            }
        }.getOrNull()
        if (!text.isNullOrBlank()) append('\n').append(text.take(JE_DOKUMENT - length))
    }
}

/** Liest nur, was sich geändert hat. Läuft immer nur einmal zugleich und bricht bei einer neueren Änderung nach dem laufenden Dokument ab. */
private suspend fun baueSuchraum(z: AblageSucheZustand, alle: List<Eintrag>, speicher: AblageSpeicher, context: Context): AblageSuche.Suchraum = z.sperre.withLock {
    val lauf = currentCoroutineContext()
    var rest = TEXT_GESAMT
    val kennungen = HashSet<String>()
    fun index(kennung: String, stand: Any, dokument: (Boolean) -> AblageSuche.Dokument): AblageSuche.Index {
        lauf.ensureActive()
        kennungen += kennung
        val x = z.indizes[kennung]?.takeIf { it.first == stand }?.second ?: try {
            AblageSuche.Index(dokument(rest > 0))
        } catch (e: OutOfMemoryError) {
            rest = 0
            AblageSuche.Index(dokument(false))
        }.also { z.indizes[kennung] = stand to it }
        rest -= x.dokument.inhalt.length
        return x
    }
    val prompts = Prompts.alle(context).map { p ->
        index("p:" + p.erstellt, listOf(p.nummer, p.geaendert, p.titel, p.text)) { _ ->
            AblageSuche.Dokument("p:" + p.erstellt, p.titel.ifBlank { "Prompt ${p.nummer}" }, "Prompt", p.text, maxOf(p.geaendert, p.erstellt))
        }
    }
    val erinnerungen = Erinnerungen.alle(context).map { r ->
        index("r:" + r.id, r.geaendert) { _ ->
            AblageSuche.Dokument("r:" + r.id, r.zeile, listOfNotNull("Erinnerung", r.datum, r.uhrzeit).joinToString(" · "), r.kurz + "\n" + r.text, maxOf(r.geaendert, r.erstellt))
        }
    }
    val eintraege = alle.map { e ->
        index("e:" + e.id, e.geaendert) { mitInhalt ->
            val neben = (listOf(e.beschreibung) + e.anhaenge.flatMap { listOf(it.originalName, it.beschreibung) }).filter { it.isNotBlank() }.joinToString(" · ")
            AblageSuche.Dokument("e:" + e.id, e.titel, neben, if (mitInhalt) suchInhalt(speicher, e) else "", e.geaendert)
        }
    }
    z.indizes.keys.retainAll(kennungen)
    // Neue oder gelöschte Einträge: Die KI kannte sie nicht, ihre Antworten gelten nicht mehr.
    if (z.raum?.indizes?.mapTo(HashSet()) { it.dokument.kennung } != kennungen) z.ki.clear()
    AblageSuche.Suchraum(prompts + erinnerungen + eintraege).also { z.raum = it }
}

/**
 * Die Suche über die ganze Ablage, auch Prompts und Erinnerungen. Die Worttreffer stehen sofort da; nach einer Tipp-Pause
 * oder der Eingabetaste wählt ein günstiges Modell nach Bedeutung die wahrscheinlichsten und setzt sie nach vorn.
 */
@Composable
internal fun AblageSucheSeite(
    vm: AppViewModel, z: AblageSucheZustand, alle: List<Eintrag>, geladen: Boolean, aktionen: AblageAktionen, listenStand: LazyListState,
    text: String, textAendern: (String) -> Unit, fokusNoetig: Boolean, fokusErledigt: () -> Unit,
    schliessen: () -> Unit, oeffnen: (Eintrag) -> Unit, auswaehlen: (Eintrag) -> Unit, loeschen: (Eintrag) -> Unit, zumReiter: (String) -> Unit,
) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val speicher = remember { AblageZentrale.speicher(context) }
    val promptStand by Prompts.stand.collectAsState()
    val erinnerungStand by Erinnerungen.stand.collectAsState()
    val raum by ladeImHintergrund(z.raum, alle, geladen, promptStand, erinnerungStand, behalten = true) {
        if (!geladen) z.raum else try {
            baueSuchraum(z, alle, speicher, context)
        } catch (e: OutOfMemoryError) {
            Logbuch.fehler("Ablage-Suche", e)
            z.indizes.clear()
            AblageSuche.Suchraum(emptyList())
        }
    }
    val anfrage = text.trim()
    var reihe by remember(anfrage) { mutableStateOf(z.ki[anfrage]) }
    var liste by remember { mutableStateOf(if (z.listeFuer == anfrage) z.liste else emptyList()) }
    LaunchedEffect(anfrage, raum, reihe) {
        val x = raum
        if (x == null || anfrage.isEmpty()) { liste = emptyList(); return@LaunchedEffect }
        if (z.listeFuer != anfrage) delay(200)
        val lauf = currentCoroutineContext()
        val neu = withContext(Dispatchers.Default) { AblageSuche.mischen(AblageSuche.suche(x, anfrage) { lauf.ensureActive() }, reihe.orEmpty(), x, anfrage) }
        liste = neu; z.liste = neu; z.listeFuer = anfrage
    }
    LaunchedEffect(anfrage) { if (z.gescrolltFuer != anfrage) { z.gescrolltFuer = anfrage; listenStand.scrollToItem(0) } }
    var kiLaeuft by remember { mutableStateOf(false) }
    var kiFehler by remember { mutableStateOf(false) }
    // Eingabetaste: nicht auf die Tipp-Pause warten.
    var sofortFuer by remember { mutableStateOf<String?>(null) }
    val kiBereit = vm.modellBereit
    val raumDa = raum != null
    LaunchedEffect(anfrage, raumDa, kiBereit, sofortFuer) {
        kiFehler = false
        kiLaeuft = false
        val x = raum
        if (x == null || anfrage.length < 3 || !kiBereit || z.ki.containsKey(anfrage)) return@LaunchedEffect
        // Lang genug, dass nicht jedes halb getippte Wort eine Anfrage auslöst.
        if (sofortFuer != anfrage) delay(2000)
        kiLaeuft = true
        try {
            val kandidaten = withContext(Dispatchers.Default) { AblageSuche.kiKandidaten(x, AblageSuche.suche(x, anfrage)) }
            val antwort = vm.ablageKiReihenfolge(anfrage, kandidaten)
            if (z.ki.size >= 50) z.ki.clear()
            z.ki[anfrage] = antwort
            reihe = antwort
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logbuch.fehler("Ablage-Suche", e)
            kiFehler = true
        } finally {
            kiLaeuft = false
        }
    }
    val fokus = remember { FocusRequester() }
    // Nur beim Öffnen über den Knopf: Kommt man aus einem Treffer zurück, soll die Tastatur die Liste nicht verdecken.
    LaunchedEffect(Unit) { if (fokusNoetig) { runCatching { fokus.requestFocus() }; fokusErledigt() } }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Rundknopf(Icons.Rounded.Close, "Suche schließen", aktion = schliessen)
            Eingabe(text, textAendern, "Was suchst du?", Modifier.weight(1f).focusRequester(fokus), senden = { sofortFuer = anfrage })
        }
        val r = reihe
        val hinweis = when {
            raum == null -> "Lese die Ablage …"
            anfrage.isEmpty() -> "Stichworte oder eine Frage eingeben. Gesucht wird in Titeln, Texten, Dateien, Prompts und Erinnerungen."
            kiLaeuft -> "${liste.size} Treffer · KI sucht nach der Bedeutung …"
            kiFehler -> "${liste.size} Treffer nach Wortübereinstimmung, die KI war nicht erreichbar."
            r != null && r.isNotEmpty() -> "Die ${liste.size} wahrscheinlichsten Treffer, von der KI geordnet." + (if ((raum?.indizes?.size ?: 0) > AblageSuche.KI_HOECHSTENS) " Sie prüft die Worttreffer und die ${AblageSuche.KI_HOECHSTENS} neuesten Einträge." else "")
            r != null -> if (liste.isEmpty()) "Nichts gefunden. Andere Worte probieren." else "${liste.size} Treffer nach Wortübereinstimmung, die KI fand nichts eindeutig Passendes."
            liste.isEmpty() -> if (kiBereit && anfrage.length >= 3) "Noch keine Worttreffer, gleich sucht die KI nach der Bedeutung …" else "Nichts gefunden. Andere Worte probieren."
            else -> "${liste.size} Treffer nach Wortübereinstimmung."
        }
        Text(hinweis, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = if (kiFehler) f.gefahr else f.textSchwach, fontSize = 12.sp)
        LazyColumn(Modifier.weight(1f), state = listenStand, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(liste, key = { it.dokument.kennung }) { fund ->
                val kennung = fund.dokument.kennung
                val istEintrag = kennung.startsWith("e:")
                val eintrag = if (istEintrag) alle.firstOrNull { it.id == kennung.drop(2) } else null
                // Gerade gelöscht, die Trefferliste ist noch nicht neu berechnet: Zeile weglassen.
                if (!istEintrag || eintrag != null) Column {
                    if (eintrag != null) {
                        EintragZeile(eintrag, false, aktionen, tippen = { oeffnen(eintrag) }, lange = { auswaehlen(eintrag) }, loeschen = { loeschen(eintrag) })
                    } else {
                        val istPrompt = kennung.startsWith("p:")
                        Row(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f).antippen {
                            if (istPrompt) { zumReiter(PROMPTS_REITER); vm.promptOffen = kennung.drop(2).toLongOrNull() }
                            else { zumReiter(ERINNERUNG_REITER); vm.erinnerungOffen = kennung.drop(2) }
                        }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (istPrompt) Icons.Rounded.Bolt else Icons.Rounded.NotificationsActive, null, tint = f.primaer, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(fund.dokument.titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(fund.dokument.nebentext, color = f.textSchwach, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (fund.auszug.isNotBlank()) Text(fund.auszug, Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp), color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
