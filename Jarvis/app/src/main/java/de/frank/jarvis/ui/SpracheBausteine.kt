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
                "Wörterbuch und Whisper-Filter gelten nur für Groq; „Stille vorab“ wirkt auch hier.",
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
        var groq by rememberSaveable { mutableStateOf(s.groqApiKey) }
        Unterzeile("Groq-Schlüssel")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eingabe(groq, { groq = it }, "gsk_…", Modifier.weight(1f), geheim = true)
            Spacer(Modifier.width(8.dp))
            Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { groq = it } }
        }
        if (groq.trim() != s.groqApiKey) Knopf("Schlüssel speichern", Modifier.padding(top = 10.dp).fillMaxWidth()) { geaendert { s.groqApiKey = groq.trim() }; vm.meldung = "Groq-Schlüssel gespeichert." }
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

    Bereich("Live-Gespräch") {
        val st = de.frank.jarvis.live.LiveOptionen.Stand(s)
        val bereit = vm.liveSchluessel(st).first.isNotBlank()
        Zeile("Orange Blase", if (bereit) "Eingerichtet" else if (st.istGemini) "Gemini-Schlüssel fehlt" else "OpenAI-Schlüssel fehlt", if (bereit) f.erfolg else f.textLeise)
        Text("Sprachgespräch mit OpenAI oder Google: Jarvis antwortet sofort, hört weiter zu und lässt sich unterbrechen. Für deine Daten fragt er den normalen Jarvis. " +
            "OpenAI-Schlüssel von platform.openai.com (eigene Abrechnung, nicht das ChatGPT-Abo), Gemini-Schlüssel von aistudio.google.com.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Schluesselfeld(vm, "OpenAI-API-Schlüssel", s.openAiApiKey) { geaendert { s.openAiApiKey = it } }
        Schluesselfeld(vm, "Gemini-Schlüssel, kostenlos (Projekt ohne Abrechnung)", s.geminiApiKey) { geaendert { s.geminiApiKey = it } }
        Schluesselfeld(vm, "Gemini-Schlüssel, bezahlt (Projekt mit Abrechnung)", s.geminiApiKeyBezahlt) { geaendert { s.geminiApiKeyBezahlt = it } }
        Text("Ein normaler AI-Studio-Schlüssel reicht. Kostenlos oder bezahlt entscheidet sein Projekt; im kostenlosen nutzt Google die Gespräche zum Training. " +
            "Welcher genutzt wird, stellst du unter „Gemini-Schlüssel“ ein.",
            color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        stand.let { }
        val lo = de.frank.jarvis.live.LiveOptionen

        // Schnellwahl: ein Tipp stellt alles Passende ein, mit geschätzten Kosten je Profil.
        val aktiv = lo.aktivesProfil(st)
        Unterzeile("Schnellwahl")
        Auswahlfeld(aktiv?.titel ?: "Eigene Mischung", lo.kurzfassung(st)) { schliessen ->
            lo.PROFILE.forEach { p ->
                WahlEintrag(p.titel, p.beschreibung,
                    lo.schaetzung(de.frank.jarvis.live.LiveOptionen.Stand(s, p.werte)).removePrefix("Geschätzt für 10 Minuten: ").let { "10 Minuten: $it" },
                    gewaehlt = p == aktiv) {
                    geaendert { p.werte.forEach { (k, v) -> st.setze(k, v) } }; vm.meldung = "Live-Gespräch: ${p.titel} eingestellt."; schliessen()
                }
            }
        }
        Text(lo.schaetzung(st), color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        Text("Annahme: halb du, halb Jarvis, 10 Wortwechsel; Preise in US-Dollar, Stand 17.10.2026. Echte Kosten zeigt die Live-Leiste. " +
            "Ausgabenlimit: platform.openai.com → Settings → Limits bzw. aistudio.google.com → Billing.",
            color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

        LiveKostenSumme(vm)

        // Stimme: Tippen wählt sie und spielt eine Hörprobe; das Menü bleibt offen, damit man weitere anhören kann.
        val stimmen = lo.alle.first { it.id == "stimme" }
        val probeArt = de.frank.jarvis.live.StimmProbe.art(st)
        val kontext = androidx.compose.ui.platform.LocalContext.current
        val stimmWerte = stimmen.werte(st)
        Unterzeile("Stimme" + if (st.istGemini) " (Gemini, 30 Stimmen)" else if (st.istLive) " (GPT-Live)" else " (Realtime)")
        Auswahlfeld(stimmWerte.firstOrNull { it.id == st["stimme"] }?.titel ?: st["stimme"]) { _ ->
            stimmWerte.sortedByDescending { de.frank.jarvis.live.StimmProbe.favoritSchluessel(probeArt, it.id) in vm.stimmFavoriten }.forEach { w ->
                val gespeichert = de.frank.jarvis.live.StimmProbe.vorhanden(kontext, probeArt, w.id)
                val laedt = vm.stimmProbeLaedt == w.id
                val favorit = de.frank.jarvis.live.StimmProbe.favoritSchluessel(probeArt, w.id) in vm.stimmFavoriten
                val gewaehlt = w.id == st["stimme"]
                // Eigene Zeile statt DropdownMenuItem: dessen Klick kennt kein langes Drücken.
                Row(
                    Modifier.fillMaxWidth().antippen(
                        lange = { vm.stimmFavoritUmschalten(probeArt, w.id, w.titel) },
                        langeText = if (favorit) "Favorit entfernen" else "Als Favorit markieren",
                    ) { geaendert { st.setze("stimme", w.id) }; vm.stimmProbe(probeArt, w.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (favorit) Icon(Icons.Rounded.Star, "Favorit", tint = FavoritGelb, modifier = Modifier.padding(end = 8.dp).size(20.dp))
                    Text((if (laedt) "⏳ " else if (gespeichert) "▶ " else "") + w.titel, color = if (gewaehlt) f.primaer else f.text,
                        fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (gewaehlt) Icon(Icons.Rounded.Check, null, tint = f.primaer)
                }
            }
        }
        Text("Tippen wählt und spielt eine Hörprobe (die erste einmalig unter 1 Cent, danach gespeichert: ▶). Langes Drücken: ★ Favorit an/aus, Favoriten stehen oben. " +
            (if (st.istGemini) "Alle Gemini-Stimmen sprechen Deutsch." else "Hör rein, welche am besten Deutsch spricht."),
            color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        // Feineinstellungen: jede Option einzeln. Zu Beginn zu, damit es übersichtlich bleibt.
        var feinOffen by rememberSaveable { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).background(f.flaeche, RoundedCornerShape(14.dp)).antippen { feinOffen = !feinOffen }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alle Einstellungen einzeln", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (feinOffen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (feinOffen) "Zuklappen" else "Aufklappen", tint = f.text)
        }
        if (feinOffen) lo.alle.filter { it.gilt(st) && it.id != "stimme" }.forEach { o ->
            val aktuell = st[o.id]
            val werte = o.werte(st)
            Unterzeile(o.titel)
            if (werte.all { it.beschreibung.isEmpty() && it.preis.isEmpty() }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    werte.forEach { w -> Chip(w.titel, aktiv = w.id == aktuell) { geaendert { st.setze(o.id, w.id) } } }
                }
            } else {
                val gewaehlt = werte.firstOrNull { it.id == aktuell }
                Auswahlfeld(gewaehlt?.titel ?: aktuell, gewaehlt?.preis.orEmpty()) { schliessen ->
                    werte.forEach { w -> WahlEintrag(w.titel, w.beschreibung, w.preis, gewaehlt = w.id == aktuell) { geaendert { st.setze(o.id, w.id) }; schliessen() } }
                }
            }
            Text(o.beschreibung, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
                Schluesselfeld(vm, "Google-Cloud-Schlüssel", s.googleTtsApiKey) { geaendert { s.googleTtsApiKey = it } }
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.googleVoices, s.googleTtsVoice) { geaendert { s.googleTtsVoice = it } }
            }
            TtsProvider.GEMINI_FLASH, TtsProvider.GEMINI_LITE -> {
                val flash = anbieter == TtsProvider.GEMINI_FLASH
                Schluesselfeld(vm, "Gemini-Stimme: Schlüssel kostenlos (Projekt ohne Abrechnung)", s.geminiTtsKeyFrei) { geaendert { s.geminiTtsKeyFrei = it } }
                Schluesselfeld(vm, "Gemini-Stimme: Schlüssel bezahlt (Projekt mit Abrechnung)", s.geminiTtsKeyBezahlt) { geaendert { s.geminiTtsKeyBezahlt = it } }
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
                Schluesselfeld(vm, "Azure-Speech-Schlüssel (KEY 1)", s.azureTtsKey) { geaendert { s.azureTtsKey = it } }
                Schluesselfeld(vm, "Azure-Region (z. B. westeurope)", s.azureTtsRegion, geheim = false) { geaendert { s.azureTtsRegion = it } }
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.azureVoices, s.azureTtsVoice) { geaendert { s.azureTtsVoice = it } }
                Text("Kostenlose Stufe F0: 500.000 Zeichen im Monat für alle normalen Neural-Stimmen. Die HD-Stimmen (Seraphina HD, Florian HD) " +
                    "laufen nur mit der bezahlten Stufe S0 und sprechen in eigenem Tempo.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            TtsProvider.QWEN -> {
                Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { geaendert { s.qwenTtsApiKey = it } }
                Unterzeile("Stimme")
                StimmChips(vm, anbieter, TtsCatalog.qwenVoices, s.qwenStandardVoice) { geaendert { s.qwenStandardVoice = it } }
            }
            TtsProvider.QWEN_CLONE -> {
                Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { geaendert { s.qwenTtsApiKey = it } }
                Schluesselfeld(vm, "Kennung deiner eigenen Stimme", s.qwenTtsVoiceId, geheim = false) { geaendert { s.qwenTtsVoiceId = it } }
            }
        }
        val rueckfall = s.ttsRueckfallKette
        RueckfallKette(vm, rueckfall, anbieter) { neu -> geaendert { s.ttsRueckfallKette = neu } }
        if (TtsProvider.GOOGLE_CLOUD.id in rueckfall &&anbieter != TtsProvider.GOOGLE_CLOUD && s.googleTtsApiKey.isBlank()) {
            Schluesselfeld(vm, "Google-Cloud-Schlüssel (für den Rückfall)", s.googleTtsApiKey) { geaendert { s.googleTtsApiKey = it } }
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
    fun favorit(v: de.frank.jarvis.tts.TtsVoice) = de.frank.jarvis.live.StimmProbe.favoritSchluessel(art, v.id) in vm.stimmFavoriten
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
 * Dauerhafte Kostensumme aller Live-Gespräche mit Durchschnitt je Gesprächsminute und -stunde. Berechnet (Nutzungsdaten der
 * API mal Listenpreis) und geschätzt (von OpenAI nicht bestätigte Zeit) stehen getrennt; Zurücksetzen mit Rückfrage.
 */
@Composable
private fun LiveKostenSumme(vm: AppViewModel) {
    val f = LocalFarben.current
    val k = vm.liveKostenBuch
    val lo = de.frank.jarvis.live.LiveOptionen
    val datum = java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", java.util.Locale.GERMANY)
    Unterzeile("Kosten seit ${datum.format(java.util.Date(k.seit))}")
    Zeile("Gesamt", (if (k.geschaetztUsd > 0) "ca. " else "") + lo.usdText(k.gesamtUsd), f.primaer)
    Zeile("Pro Gesprächsminute", k.proMinute?.let { "ca. " + lo.usdText(it) } ?: "noch keine Daten", f.text)
    Zeile("Pro Gesprächsstunde", k.proStunde?.let { "ca. " + lo.usdText(it) } ?: "noch keine Daten", f.text)
    Zeile("Berechnet aus API-Nutzungsdaten", lo.usdText(k.berechnetUsd), f.text)
    Zeile("Geschätzt", lo.usdText(k.geschaetztUsd), f.text)
    val minuten = k.dauerSek / 60.0
    val dauer = if (minuten < 60) String.format(java.util.Locale.GERMAN, "%.1f Minuten", minuten)
        else String.format(java.util.Locale.GERMAN, "%.2f Stunden", minuten / 60.0)
    Text(
        "Zeitbasis: $dauer Gesprächszeit in ${k.gespraeche} " + (if (k.gespraeche == 1) "Gespräch" else "Gesprächen") +
            (if (k.gespraeche > 0) ", ${k.bestaetigt} davon von OpenAI bestätigt" else "") +
            (if (k.zuletzt > 0) ". Zuletzt verbucht: ${datum.format(java.util.Date(k.zuletzt))}." else ".") +
            "\n\nBerechnet = gemeldete Nutzung mal Listenpreis (Gemini mit kostenlosem Schlüssel = 0 $). Geschätzt = von OpenAI nicht bestätigte Zeit mit 0,05 $ pro Minute. " +
            "Der Durchschnitt bezieht sich auf die Gesprächszeit. Die Live-Leiste zeigt „dieses Gespräch / gesamt“.\n\n" +
            "Keine Rechnung: Aufträge an den normalen Jarvis, Websuche, Steuern und Gespräche vor 1.8.97 fehlen. Verbindlich ist platform.openai.com → Usage." +
            (if (k.ohnePreis > 0) "\n\n${k.ohnePreis} Antworten hatten ein Modell ohne hinterlegten Preis und fehlen in der Summe." else ""),
        color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
    )
    var fragen by remember { mutableStateOf(false) }
    if (!fragen) {
        Knopf("Kostenzählung zurücksetzen", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { fragen = true }
    } else {
        Text("Summe, Durchschnitte und Zeitbasis auf null setzen? Die Zählung beginnt dann jetzt neu.", color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Knopf("Ja, zurücksetzen", Modifier.weight(1f)) { vm.liveKostenZuruecksetzen(); fragen = false }
            Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { fragen = false }
        }
    }
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
