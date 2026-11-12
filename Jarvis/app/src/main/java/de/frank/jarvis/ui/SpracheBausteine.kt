package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.speech.VorleseZustand
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/**
 * Lautsprecher-Knopf: liest [text] absatzweise vor. Leuchtet nur für seine eigene [quelle];
 * ein zweiter Tipp hält an.
 */
@Composable
fun Vorleseknopf(vm: AppViewModel, quelle: String, titel: String, text: String, groesse: Dp = 44.dp, schlicht: Boolean = false) {
    val f = LocalFarben.current
    val stand by vm.vorleser.stand.collectAsState()
    val meiner = stand.quelle == quelle && stand.zustand != VorleseZustand.AUS
    // schlicht = ohne Glas-Einfassung, als einfaches Symbol wie die übrigen Knöpfe der Antwortleiste.
    val form = if (schlicht) Modifier.size(groesse) else Modifier.size(groesse).glas(f, groesse / 2, erhoeht = 0.5f, toenung = if (meiner) f.primaer else null)
    val symbol = if (schlicht) 19.dp else groesse * 0.45f
    Box(form.antippen { vm.lies(quelle, titel, text) }, contentAlignment = Alignment.Center) {
        when {
            meiner && stand.zustand == VorleseZustand.LAEDT -> Kern(if (schlicht) 22.dp else groesse * 0.6f, f.primaer, aktiv = true)
            meiner -> Icon(Icons.Rounded.Stop, "Vorlesen anhalten", tint = f.primaer, modifier = Modifier.size(symbol))
            else -> Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Vorlesen", tint = f.textLeise, modifier = Modifier.size(symbol))
        }
    }
}

/**
 * Vorlesesteuerung wie in einem Player (Tagesauswertung): Anfangs nur der Lautsprecher. Läuft das Vorlesen, wird derselbe
 * rechte Knopf zu Pause bzw. Weiter, links daneben erscheint Stopp. Pause hält mitten im Satz an, Weiter setzt genau dort
 * fort (der Vorleser pausiert den Player, statt neu zu beginnen). Stopp und das reguläre Ende stellen den Lautsprecher wieder her.
 */
@Composable
fun VorleseSteuerung(vm: AppViewModel, quelle: String, titel: String, text: String, groesse: Dp = 44.dp) {
    val f = LocalFarben.current
    val stand by vm.vorleser.stand.collectAsState()
    val zustand = if (stand.quelle == quelle) stand.zustand else VorleseZustand.AUS
    val symbol = groesse * 0.45f
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (zustand != VorleseZustand.AUS) {
            Box(Modifier.size(groesse).glas(f, groesse / 2, erhoeht = 0.5f).antippen { vm.vorlesenStoppen(quelle) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Stop, "Vorlesen beenden", tint = f.textLeise, modifier = Modifier.size(symbol))
            }
            Spacer(Modifier.width(8.dp))
        }
        val aktiv = zustand != VorleseZustand.AUS
        Box(
            Modifier.size(groesse).glas(f, groesse / 2, erhoeht = 0.5f, toenung = if (aktiv) f.primaer else null).antippen {
                when (zustand) {
                    VorleseZustand.AUS -> vm.lies(quelle, titel, text)
                    VorleseZustand.PAUSIERT -> vm.vorlesenWeiter(quelle)
                    else -> vm.vorlesenPause(quelle)
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            when (zustand) {
                VorleseZustand.AUS -> Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Vorlesen", tint = f.textLeise, modifier = Modifier.size(symbol))
                // Während der erste Ton erzeugt wird, schon antippbar: Pause hält dann vor dem ersten Satz an.
                VorleseZustand.LAEDT -> Kern(groesse * 0.6f, f.primaer, aktiv = true)
                VorleseZustand.SPRICHT -> Icon(Icons.Rounded.Pause, "Vorlesen pausieren", tint = f.primaer, modifier = Modifier.size(symbol))
                VorleseZustand.PAUSIERT -> Icon(Icons.Rounded.PlayArrow, "Vorlesen fortsetzen", tint = f.primaer, modifier = Modifier.size(symbol))
            }
        }
    }
}

/** Einstellungen für Spracheingabe (Groq) und Vorlesen (Edge, Google, Alibaba, eigene Stimme). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpracheEinstellungen(vm: AppViewModel) {
    val f = LocalFarben.current
    val s = vm.sprache
    val e = vm.einstellungen
    // Die Sprach-Einstellungen melden Änderungen nicht selbst an die Oberfläche: nach jedem Tipp neu zeichnen.
    var stand by remember { mutableIntStateOf(0) }
    fun geaendert(block: () -> Unit) { block(); stand++ }
    stand.let { }

    Bereich("Spracheingabe") {
        val pl = de.frank.jarvis.speech.ParakeetLokal
        val lokal = s.transkriptionLokal
        val modellDa = pl.bereit(vm.getApplication())
        val eingerichtet = if (lokal) modellDa else s.groqApiKey.isNotBlank()
        Zeile("Mikrofon", if (eingerichtet) (if (lokal) "Offline eingerichtet" else "Eingerichtet") else if (lokal) "Offline-Modell fehlt" else "Groq-Schlüssel fehlt", if (eingerichtet) f.erfolg else f.textLeise)
        Unterzeile("Transkription")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Groq Whisper (online)", aktiv = !lokal) { geaendert { vm.transkriptionLokalSetzen(false) } }
            Chip("Parakeet primeline (offline)", aktiv = lokal) { geaendert { vm.transkriptionLokalSetzen(true) } }
        }
        if (lokal) {
            Text("Deutsches Diktat ganz auf dem Handy: parakeet-primeline (deutscher Fine-Tune von NVIDIA Parakeet TDT 0.6B v3, int8, sherpa-onnx, nur CPU mit 4 Threads). " +
                "Nach dem einmaligen Download kein Internet, keine Cloud, kein Schlüssel, auch im Flugmodus. Fehlt das Modell, gibt es eine Meldung, nie einen Rückfall auf Groq. " +
                "Das Wörterbuch unten gilt auch hier und erhöht die Trefferquote seiner Begriffe. Whisper-Filter gelten nur für Groq; „Stille vorab“ wirkt auch hier. Deine anderen Apps nutzen dieses Modell mit, ohne es selbst zu laden.",
                color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            val mb = { b: Long -> "${b / 1_048_576} MB" }
            val laeuft = vm.parakeetDownload
            Zeile("Modell", when {
                laeuft != null -> "Lädt … ${(laeuft * 100).toInt()} %"
                modellDa -> "Bereit, ${mb(pl.belegteBytes(vm.getApplication()))} belegt"
                else -> "Nicht geladen (${mb(pl.GESAMT_BYTES)})"
            }, if (modellDa) f.erfolg else f.textLeise)
            pl.messung?.let { m ->
                val zeilen = buildList {
                    add("Laden und Aufwärmen: ${"%.1f".format(m.ladeMs / 1000.0)} s, Arbeitsspeicher etwa ${m.ramMb} MB")
                    if (m.audioMs > 0) add("Letzte Mitschrift: ${"%.1f".format(m.audioMs / 1000.0)} s Aufnahme in ${"%.2f".format(m.rechenMs / 1000.0)} s gerechnet")
                }
                Text("Gemessen auf diesem Handy: " + zeilen.joinToString("; ") + ".", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            when {
                laeuft != null -> Unit
                !modellDa -> Knopf("Modell herunterladen (${mb(pl.GESAMT_BYTES)}, einmalig)", Modifier.padding(top = 10.dp).fillMaxWidth()) { geaendert { vm.parakeetHerunterladen() } }
                else -> Knopf("Modell löschen", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { geaendert { vm.parakeetLoeschen() } }
            }
        }
        if (!lokal) Text("Das Mikrofon in der Eingabeblase schreibt mit Groq (${s.whisperModell}${if (s.whisperModell == de.frank.module.transkriptionsmodul.GroqTranscriber.MODELL_LARGE_V3 && de.frank.module.transkriptionsmodul.GroqTranscriber.largeV3Gesperrt()) ", derzeit Turbo – Large V3 gesperrt" else ""}) mit und setzt den Text ins Feld. Jedes weitere Diktat kommt hinten dran; gesendet wird mit dem Pfeil. „Hey Jarvis“ sendet weiter direkt.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        SchluesselVerweis(vm, "Groq-Schlüssel", s.groqApiKey.isNotBlank())
        Unterzeile("Filter gegen erfundene Texte bei Stille")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Stille vorab", aktiv = s.filterStilleVorabAn) { geaendert { s.filterStilleVorabAn = !s.filterStilleVorabAn } }
            Chip("Segment-Werte", aktiv = s.filterSegmentmetrikenAn) { geaendert { s.filterSegmentmetrikenAn = !s.filterSegmentmetrikenAn } }
            Chip("Zeitstempel", aktiv = s.filterZeitstempelAn) { geaendert { s.filterZeitstempelAn = !s.filterZeitstempelAn } }
            Chip("Floskeln", aktiv = s.filterFloskelnAn) { geaendert { s.filterFloskelnAn = !s.filterFloskelnAn } }
        }
        val gt = de.frank.module.transkriptionsmodul.GroqTranscriber
        Unterzeile("Whisper-Modell")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Erneute Wahl hebt die 30-Minuten-Sperre nach einer Groq-Ablehnung auf: large-v3 wird sofort wieder versucht.
            Chip("Large V3 Turbo (schnell)", aktiv = s.whisperModell != gt.MODELL_LARGE_V3) { geaendert { s.whisperModell = gt.MODELL_TURBO; gt.sperreAufheben() } }
            Chip("Large V3 (genauer)", aktiv = s.whisperModell == gt.MODELL_LARGE_V3) { geaendert { s.whisperModell = gt.MODELL_LARGE_V3; gt.sperreAufheben() } }
        }
        Text("Large V3 muss in der Groq-Konsole für deine Organisation freigeschaltet sein; sonst nimmt Jarvis still Turbo.", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Unterzeile("Persönliches Wörterbuch")
        Chip("Wörterbuch mitsenden", aktiv = s.woerterbuchAn) { geaendert { s.woerterbuchAn = !s.woerterbuchAn } }
        if (s.woerterbuchAn) {
            var begriffe by rememberSaveable(s.woerterbuchBegriffe) { mutableStateOf(s.woerterbuchBegriffe) }
            Text("Begriffe (durch Komma oder Zeilenumbruch getrennt)", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Eingabe(begriffe, { begriffe = it }, "Begriffe", Modifier.fillMaxWidth(), einzeilig = false)
            if (begriffe != s.woerterbuchBegriffe) Knopf("Begriffe speichern", Modifier.padding(top = 10.dp).fillMaxWidth()) { geaendert { s.woerterbuchBegriffe = begriffe }; vm.meldung = "Wörterbuch gespeichert." }
        }
    }

    Bereich("Vorlesen") {
        val anbieter = TtsCatalog.providers.firstOrNull { it.id == s.ttsProvider } ?: TtsCatalog.DEFAULT_PROVIDER
        Zeile("Stimme", anbieter.label, f.erfolg)
        Text("Jarvis liest absatzweise vor und lädt die nächsten Absätze schon im Voraus, damit keine Pausen entstehen.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Unterzeile("Anbieter")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TtsCatalog.providers.forEach { p -> Chip(p.label, aktiv = p == anbieter) { geaendert { s.ttsProvider = p.id } } }
        }
        when (anbieter) {
            TtsProvider.EDGE -> {
                Unterzeile("Stimme (ohne Schlüssel nutzbar)")
                StimmChips(vm, anbieter, TtsCatalog.edgeVoices, s.edgeTtsVoice) { geaendert { s.edgeTtsVoice = it } }
            }
            TtsProvider.GOOGLE_CLOUD -> {
                SchluesselVerweis(vm, "Google-Cloud-Schlüssel", s.googleTtsApiKey.isNotBlank())
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.googleVoices, s.googleTtsVoice) { geaendert { s.googleTtsVoice = it } }
            }
            TtsProvider.GEMINI_FLASH, TtsProvider.GEMINI_LITE -> {
                val flash = anbieter == TtsProvider.GEMINI_FLASH
                SchluesselVerweis(vm, "Gemini-Stimme: Schlüssel", s.geminiTtsKeyFrei.isNotBlank() || s.geminiTtsKeyBezahlt.isNotBlank())
                Unterzeile("Welcher Schlüssel")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecureSettings.GEMINI_TTS_SCHLUESSEL_WAHLEN.forEach { (id, name) ->
                        Chip(name, aktiv = s.geminiTtsSchluesselWahl == id) { geaendert { s.geminiTtsSchluesselWahl = id } }
                    }
                }
                Text("Was danach kommt, wenn der Schlüssel scheitert, legst du unten in der Rückfall-Reihenfolge fest.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, if (flash) TtsCatalog.geminiFlashVoices else TtsCatalog.geminiLiteVoices,
                    if (flash) s.geminiFlashTtsVoice else s.geminiLiteTtsVoice) { id -> geaendert { if (flash) s.geminiFlashTtsVoice = id else s.geminiLiteTtsVoice = id } }
            }
            TtsProvider.AZURE -> {
                SchluesselVerweis(vm, "Azure-Speech-Schlüssel", s.azureTtsKey.isNotBlank())
                Schluesselfeld(vm, "Azure-Region (z. B. westeurope)", s.azureTtsRegion, geheim = false) { geaendert { s.azureTtsRegion = it } }
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.azureVoices, s.azureTtsVoice) { geaendert { s.azureTtsVoice = it } }
                Text("Kostenlose Stufe F0: 500.000 Zeichen im Monat für alle normalen Neural-Stimmen. Die HD-Stimmen (Seraphina HD, Florian HD) " +
                    "laufen nur mit der bezahlten Stufe S0 und sprechen in eigenem Tempo.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            TtsProvider.QWEN -> {
                SchluesselVerweis(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey.isNotBlank())
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.qwenVoices, s.qwenStandardVoice) { geaendert { s.qwenStandardVoice = it } }
            }
            TtsProvider.QWEN_CLONE -> {
                SchluesselVerweis(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey.isNotBlank())
                Schluesselfeld(vm, "Kennung deiner eigenen Stimme", s.qwenTtsVoiceId, geheim = false) { geaendert { s.qwenTtsVoiceId = it } }
            }
        }
        val rueckfall = s.ttsRueckfallKette
        RueckfallKette(vm, rueckfall, anbieter) { neu -> geaendert { s.ttsRueckfallKette = neu } }
        if (TtsProvider.GOOGLE_CLOUD.id in rueckfall &&anbieter != TtsProvider.GOOGLE_CLOUD && s.googleTtsApiKey.isBlank()) {
            SchluesselVerweis(vm, "Google-Cloud-Schlüssel (für den Rückfall)", s.googleTtsApiKey.isNotBlank())
        }
        // Ein Tempo je Stimme, die es auswertet: die Hauptstimme und jede Rückfallstufe, jeweils nur einmal.
        val mitTempo = (listOf(anbieter) + rueckfall.mapNotNull { TtsCatalog.rueckfallStufe(it)?.anbieter }).distinct().filter { it.kenntTempo }
        Unterzeile("Sprechtempo")
        if (mitTempo.isEmpty()) {
            Text("Die gewählten Stimmen sprechen alle im eigenen Tempo.", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        mitTempo.forEach { p ->
            val tempo = s.ttsTempo(p.id)
            Text(p.label + if (p == anbieter) "" else " (Rückfall)", color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.9f to "Ruhig", 1.0f to "Normal", 1.1f to "Zügig", 1.2f to "Schnell").forEach { (wert, name) ->
                    Chip(name, aktiv = kotlin.math.abs(tempo - wert) < 0.04f) { geaendert { s.setzeTtsTempo(p.id, wert) } }
                }
            }
        }
        if (mitTempo.isNotEmpty()) {
            Text("Jede Stimme behält ihr eigenes Tempo, auch wenn sie als Rückfallstufe einspringt.",
                color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Knopf("Probe hören", Modifier.padding(top = 14.dp).fillMaxWidth(), haupt = false) { vm.lies("probe", "Probe", "Hallo Frank. So klinge ich, wenn ich dir etwas vorlese.") }

        Unterzeile("Von selbst vorlesen")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Antworten auf Gesprochenes", aktiv = e.antwortenVorlesen) { e.antwortenVorlesen = !e.antwortenVorlesen }
            Chip("Antwort nach Diktat (Mikrofon + Pfeil)", aktiv = e.diktatAntwortVorlesen) { geaendert { e.diktatAntwortVorlesen = !e.diktatAntwortVorlesen } }
        }
        Text("Meldungen vorlesen (Agenten, Losfahren, Timer, Update-Ansagen …) mit eigener Stimme und Lautstärke: Einstellungen → Benachrichtigungen.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

/**
 * Die Rückfall-Reihenfolge des Vorlesens: nummerierte Stufen mit ↑ ↓ ✕, darunter alle übrigen Stufen zum Hinzufügen.
 * Jede Stufe liest mit der Stimme, die bei ihrem Anbieter eingestellt ist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RueckfallKette(vm: AppViewModel, kette: List<String>, anbieter: TtsProvider, setze: (List<String>) -> Unit) {
    val f = LocalFarben.current
    val s = vm.sprache
    Unterzeile("Rückfall-Reihenfolge")
    Text("Scheitert die Stimme oben (etwa Kontingent erschöpft), probiert Jarvis diese Stufen der Reihe nach, jede mit der dort eingestellten Stimme. " +
        "Eine Stufe ohne Schlüssel wird übersprungen. Leer = kein Rückfall.",
        color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    kette.forEachIndexed { i, id ->
        val stufe = TtsCatalog.rueckfallStufe(id) ?: return@forEachIndexed
        val schluesselFehlt = !s.rueckfallBereit(stufe)
        val gleich = stufe.anbieter == anbieter && stufe.schluessel == null
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${i + 1}. ${stufe.titel}", color = f.text, fontSize = 14.sp)
                if (schluesselFehlt || gleich) {
                    Text(if (gleich) "Ist die Stimme oben, wird übersprungen" else "Nicht eingerichtet, wird übersprungen", color = f.textSchwach, fontSize = 11.sp)
                }
            }
            if (i > 0) Chip("↑", aktiv = false, modifier = Modifier.padding(start = 6.dp)) { setze(kette.toMutableList().apply { add(i - 1, removeAt(i)) }) }
            if (i < kette.lastIndex) Chip("↓", aktiv = false, modifier = Modifier.padding(start = 6.dp)) { setze(kette.toMutableList().apply { add(i + 1, removeAt(i)) }) }
            Chip("✕", aktiv = false, modifier = Modifier.padding(start = 6.dp)) { setze(kette - id) }
        }
    }
    val uebrig = TtsCatalog.rueckfallStufen.filter { it.id !in kette }
    if (uebrig.isNotEmpty()) {
        Text("Hinzufügen:", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            uebrig.forEach { st -> Chip("+ ${st.titel}", aktiv = false) { setze(kette + st.id) } }
        }
    }
}

/** Gelb des Favoritensterns bei allen Stimmenauswahlen. */
val FavoritGelb = androidx.compose.ui.graphics.Color(0xFFFFC107)

/**
 * Stimmen eines Vorlese-Anbieters als Chips. Tippen wählt, langes Drücken setzt oder entfernt den Favoriten;
 * Favoriten tragen einen gelben Stern und stehen vorn. Vorlesen und Benachrichtigungen teilen dieselben Favoriten.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StimmChips(vm: AppViewModel, anbieter: TtsProvider, stimmen: List<de.frank.jarvis.tts.TtsVoice>, gewaehlt: String, waehlen: (String) -> Unit) {
    val art = "tts_" + anbieter.id
    fun favorit(v: de.frank.jarvis.tts.TtsVoice) = de.frank.jarvis.tts.StimmFavoriten.favoritSchluessel(art, v.id) in vm.stimmFavoriten
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stimmen.sortedByDescending { favorit(it) }.forEach { v ->
            val fav = favorit(v)
            Chip(v.name, aktiv = gewaehlt == v.id, icon = if (fav) Icons.Rounded.Star else null, iconFarbe = FavoritGelb,
                lange = { vm.stimmFavoritUmschalten(art, v.id, v.name) }, langeText = if (fav) "Favorit entfernen" else "Als Favorit markieren") { waehlen(v.id) }
        }
    }
    Text("Langes Drücken: ★ Favorit an/aus, Favoriten stehen vorn.", color = LocalFarben.current.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun Schluesselfeld(vm: AppViewModel, titel: String, gespeichert: String, geheim: Boolean = true, speichern: (String) -> Unit) {
    var wert by rememberSaveable(titel) { mutableStateOf(gespeichert) }
    Unterzeile(titel)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Eingabe(wert, { wert = it }, "einfügen", Modifier.weight(1f), geheim = geheim)
        Spacer(Modifier.width(8.dp))
        Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { wert = it } }
    }
    if (wert.trim() != gespeichert) Knopf("Speichern", Modifier.padding(top = 10.dp).fillMaxWidth()) { speichern(wert.trim()); vm.meldung = "Gespeichert." }
}


/**
 * Aufklappfeld: zeigt die gewählte Option und öffnet beim Tippen alle Optionen. Das Menü dockt direkt unter (oder bei
 * wenig Platz über) dem Feld an, ist genau so breit und nie höher als der freie Platz im Fenster; lange Listen scrollen.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun Auswahlfeld(titel: String, unter: String = "", inhalt: @Composable ColumnScope.(schliessen: () -> Unit) -> Unit) {
    val f = LocalFarben.current
    var offen by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(offen, { offen = it }, Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .background(if (offen) f.primaer.copy(alpha = 0.14f) else f.flaeche, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (unter.isNotEmpty()) Text(unter, color = f.textLeise, fontSize = 13.sp)
            }
            Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (offen) "Auswahl schließen" else "Auswahl öffnen", tint = f.text)
        }
        val schema = androidx.compose.material3.MaterialTheme.colorScheme.copy(onSurface = f.text, onSurfaceVariant = f.primaer)
        androidx.compose.material3.MaterialTheme(colorScheme = schema) {
            ExposedDropdownMenu(
                offen, { offen = false }, shape = RoundedCornerShape(14.dp), containerColor = menueFlaeche(f),
                tonalElevation = 0.dp, shadowElevation = 10.dp, border = androidx.compose.foundation.BorderStroke(1.dp, f.rand),
            ) {
                inhalt { offen = false }
                // Etwas Luft unter dem letzten Eintrag, damit er nie an der Menükante klebt.
                Spacer(Modifier.size(6.dp))
            }
        }
    }
}

/** Menüpunkt mit Titel, kurzer Erklärung und Preis; der gewählte steht fett mit Haken. */
@Composable
private fun WahlEintrag(titel: String, beschreibung: String, preis: String, gewaehlt: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(titel, color = if (gewaehlt) f.primaer else f.text, fontSize = 15.sp, fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Normal)
                if (beschreibung.isNotEmpty()) Text(beschreibung, color = f.textLeise, fontSize = 13.sp)
                if (preis.isNotEmpty()) Text(preis, color = f.primaer, fontSize = 13.sp)
            }
        },
        onClick = aktion,
        trailingIcon = { if (gewaehlt) Icon(Icons.Rounded.Check, null, tint = f.primaer) },
    )
}
