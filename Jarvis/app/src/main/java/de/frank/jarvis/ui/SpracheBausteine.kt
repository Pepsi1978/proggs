package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ContentPaste
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
        val eingerichtet = s.groqApiKey.isNotBlank()
        Zeile("Mikrofon", if (eingerichtet) "Eingerichtet" else "Groq-Schlüssel fehlt", if (eingerichtet) f.erfolg else f.textLeise)
        Text("Das Mikrofon in der Eingabeblase schreibt mit Groq (whisper-large-v3-turbo) mit und setzt den Text ins Feld. Jedes weitere Diktat kommt hinten dran; gesendet wird mit dem Pfeil. „Hey Jarvis“ sendet weiter direkt.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
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
    }

    Bereich("Live-Gespräch") {
        val bereit = s.openAiApiKey.isNotBlank()
        Zeile("Orange Blase", if (bereit) "Eingerichtet" else "OpenAI-Schlüssel fehlt", if (bereit) f.erfolg else f.textLeise)
        Text("Echtes Sprachgespräch mit OpenAI: Jarvis antwortet sofort mit Stimme, hört dabei weiter zu, und du kannst ihn jederzeit unterbrechen. " +
            "Für deine Daten fragt er im Hintergrund den normalen Jarvis mit allen Werkzeugen. Braucht einen eigenen API-Schlüssel von platform.openai.com; " +
            "abgerechnet wird dort, nicht über das ChatGPT-Abo. Preise in US-Dollar, Stand 16.10.2026.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Schluesselfeld(vm, "OpenAI-API-Schlüssel", s.openAiApiKey) { geaendert { s.openAiApiKey = it } }
        val st = de.frank.jarvis.live.LiveOptionen.Stand(s)
        stand.let { }
        val lo = de.frank.jarvis.live.LiveOptionen

        // Schnellwahl: ein Tipp stellt alles Passende ein, mit geschätzten Kosten je Profil.
        Unterzeile("Schnellwahl")
        val aktiv = lo.aktivesProfil(st)
        lo.PROFILE.forEach { p ->
            val an = p == aktiv
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .background(if (an) f.primaer.copy(alpha = 0.16f) else f.flaeche, RoundedCornerShape(16.dp))
                    .antippen { geaendert { p.werte.forEach { (k, v) -> st.setze(k, v) } }; vm.meldung = "Live-Gespräch: ${p.titel} eingestellt." }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(p.beschreibung, color = f.textLeise, fontSize = 13.sp)
                    Text(lo.schaetzung(de.frank.jarvis.live.LiveOptionen.Stand(s, p.werte)).removePrefix("Geschätzt für 10 Minuten: ").let { "10 Minuten: $it" }, color = f.primaer, fontSize = 13.sp)
                }
                if (an) Icon(Icons.Rounded.Check, null, tint = f.primaer)
            }
        }

        // Was gerade eingestellt ist und was es kostet.
        Unterzeile(if (aktiv == null) "Eingestellt (eigene Mischung)" else "Eingestellt: ${aktiv.titel}")
        Text(lo.kurzfassung(st), color = f.text, fontSize = 14.sp)
        Text(lo.schaetzung(st), color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Text("Annahme: je zur Hälfte du und Jarvis, 10 Wortwechsel. Im Gespräch zeigt die Live-Leiste die echten Kosten, am Ende kommt die Summe. " +
            "Verbrauch und ein festes Ausgabenlimit: platform.openai.com → Usage bzw. Settings → Limits.",
            color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

        // Stimme immer sichtbar: Tippen wählt sie aus und spielt eine deutsche Hörprobe.
        val stimmen = lo.alle.first { it.id == "stimme" }
        Unterzeile("Stimme" + if (st.istLive) " (GPT-Live)" else " (Realtime)")
        Text("Tippen wählt die Stimme und spielt eine kurze Hörprobe auf Deutsch. Die erste Probe je Stimme holt Jarvis einmalig von OpenAI (unter 1 Cent), danach ist sie gespeichert und kostenlos. " +
            "OpenAI nennt keine Liste deutscher Stimmen: Hör einfach rein, welche am besten Deutsch spricht. ▶ = Probe schon gespeichert.",
            color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stimmen.werte(st).forEach { w ->
                val gespeichert = de.frank.jarvis.live.StimmProbe.vorhanden(androidx.compose.ui.platform.LocalContext.current, st.istLive, w.id)
                val laedt = vm.stimmProbeLaedt == w.id
                Chip((if (laedt) "⏳ " else if (gespeichert) "▶ " else "") + w.titel, aktiv = w.id == st["stimme"]) {
                    geaendert { st.setze("stimme", w.id) }
                    vm.stimmProbe(st.istLive, w.id)
                }
            }
        }

        // Feineinstellungen: jede Option einzeln, mit Erklärung und Preis. Zu Beginn zu, damit es übersichtlich bleibt.
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
            Text(o.beschreibung, color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            if (werte.all { it.beschreibung.isEmpty() && it.preis.isEmpty() }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    werte.forEach { w -> Chip(w.titel, aktiv = w.id == aktuell) { geaendert { st.setze(o.id, w.id) } } }
                }
            } else {
                werte.forEach { w ->
                    val an = w.id == aktuell
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(if (an) f.primaer.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
                            .antippen { geaendert { st.setze(o.id, w.id) } }.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(w.titel, color = f.text, fontSize = 15.sp, fontWeight = if (an) FontWeight.Bold else FontWeight.Normal)
                            if (w.beschreibung.isNotEmpty()) Text(w.beschreibung, color = f.textLeise, fontSize = 13.sp)
                            if (w.preis.isNotEmpty()) Text(w.preis, color = f.primaer, fontSize = 13.sp)
                        }
                        if (an) Icon(Icons.Rounded.Check, null, tint = f.primaer)
                    }
                }
            }
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TtsCatalog.edgeVoices.forEach { v -> Chip(v.name, aktiv = s.edgeTtsVoice == v.id) { geaendert { s.edgeTtsVoice = v.id } } }
                }
            }
            TtsProvider.GOOGLE_CLOUD -> {
                Schluesselfeld(vm, "Google-Cloud-Schlüssel", s.googleTtsApiKey) { geaendert { s.googleTtsApiKey = it } }
                Unterzeile("Stimme")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TtsCatalog.googleVoices.forEach { v -> Chip(v.name, aktiv = s.googleTtsVoice == v.id) { geaendert { s.googleTtsVoice = v.id } } }
                }
            }
            TtsProvider.QWEN -> {
                Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { geaendert { s.qwenTtsApiKey = it } }
                Unterzeile("Stimme")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TtsCatalog.qwenVoices.forEach { v -> Chip(v.name, aktiv = s.qwenStandardVoice == v.id) { geaendert { s.qwenStandardVoice = v.id } } }
                }
            }
            TtsProvider.QWEN_CLONE -> {
                Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { geaendert { s.qwenTtsApiKey = it } }
                Schluesselfeld(vm, "Kennung deiner eigenen Stimme", s.qwenTtsVoiceId, geheim = false) { geaendert { s.qwenTtsVoiceId = it } }
            }
        }
        Unterzeile("Sprechtempo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.9f to "Ruhig", 1.0f to "Normal", 1.1f to "Zügig", 1.2f to "Schnell").forEach { (wert, name) ->
                Chip(name, aktiv = kotlin.math.abs(s.ttsSpeechRate - wert) < 0.04f) { geaendert { s.ttsSpeechRate = wert } }
            }
        }
        Knopf("Probe hören", Modifier.padding(top = 14.dp).fillMaxWidth(), haupt = false) { vm.lies("probe", "Probe", "Hallo Frank. So klinge ich, wenn ich dir etwas vorlese.") }

        Unterzeile("Von selbst vorlesen")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Antworten auf Gesprochenes", aktiv = e.antwortenVorlesen) { e.antwortenVorlesen = !e.antwortenVorlesen }
            Chip("Fertige Agenten-Ergebnisse", aktiv = e.ergebnisseVorlesen) { e.ergebnisseVorlesen = !e.ergebnisseVorlesen }
        }
    }

    Bereich("Update-Ansagen") {
        Zeile("Ansagen von UpdateStation", if (e.ansageVorlesen) "An" else "Aus", if (e.ansageVorlesen) f.erfolg else f.textLeise)
        Text("Liegt ein Update fertig geladen in UpdateStation, sagt Jarvis es an. Stimme, Tempo und Lautstärke dafür stellst du hier getrennt von Jarvis' eigener Stimme ein.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("An", aktiv = e.ansageVorlesen) { geaendert { e.ansageVorlesen = true } }
            Chip("Aus", aktiv = !e.ansageVorlesen) { geaendert { e.ansageVorlesen = false } }
        }
        if (e.ansageVorlesen) {
            // null = dieselbe Stimme wie Jarvis; sonst der Anbieter, aus dem die Ansage-Stimme kommt.
            var ansageAnbieter by remember { mutableStateOf(TtsCatalog.anbieterZu(e.ansageStimme)) }
            Unterzeile("Stimme")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Wie Jarvis", aktiv = ansageAnbieter == null) { ansageAnbieter = null; geaendert { e.ansageStimme = "" } }
                listOf(TtsProvider.EDGE to TtsCatalog.DEFAULT_EDGE_VOICE, TtsProvider.GOOGLE_CLOUD to TtsCatalog.DEFAULT_GOOGLE_VOICE, TtsProvider.QWEN to TtsCatalog.DEFAULT_QWEN_VOICE).forEach { (p, standard) ->
                    Chip(p.label, aktiv = ansageAnbieter == p) {
                        ansageAnbieter = p
                        if (TtsCatalog.anbieterZu(e.ansageStimme) != p) geaendert { e.ansageStimme = standard }
                    }
                }
            }
            val stimmen = when (ansageAnbieter) {
                TtsProvider.EDGE -> TtsCatalog.edgeVoices
                TtsProvider.GOOGLE_CLOUD -> TtsCatalog.googleVoices
                TtsProvider.QWEN -> TtsCatalog.qwenVoices
                else -> emptyList()
            }
            if (stimmen.isNotEmpty()) {
                Unterzeile("Stimme für Update-Ansagen")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stimmen.forEach { v -> Chip(v.name, aktiv = e.ansageStimme == v.id) { geaendert { e.ansageStimme = v.id } } }
                }
            }
            if (ansageAnbieter == TtsProvider.GOOGLE_CLOUD && s.googleTtsApiKey.isBlank()) Schluesselfeld(vm, "Google-Cloud-Schlüssel", s.googleTtsApiKey) { geaendert { s.googleTtsApiKey = it } }
            if (ansageAnbieter == TtsProvider.QWEN && s.qwenTtsApiKey.isBlank()) Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { geaendert { s.qwenTtsApiKey = it } }
            Unterzeile("Sprechtempo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0f to "Wie Jarvis", 0.9f to "Ruhig", 1.0f to "Normal", 1.1f to "Zügig", 1.2f to "Schnell").forEach { (wert, name) ->
                    Chip(name, aktiv = kotlin.math.abs(e.ansageTempo - wert) < 0.04f) { geaendert { e.ansageTempo = wert } }
                }
            }
            Unterzeile("Lautstärke")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 40, 60, 80, 100).forEach { prozent -> Chip(if (prozent == 100) "Maximal" else "$prozent %", aktiv = e.ansageLautstaerke == prozent) { geaendert { e.ansageLautstaerke = prozent } } }
            }
            Knopf("Probe hören", Modifier.padding(top = 14.dp).fillMaxWidth(), haupt = false) {
                de.frank.jarvis.speech.Ansage.ansage(vm.getApplication(), "UpdateStation", "Ein Update von Jarvis ist bereit in UpdateStation.")
            }
            Text("Jarvis stellt die Medienlautstärke nur für die Ansage so ein und danach wieder zurück. Bei „Nicht stören“ bleibt es bei der stillen Benachrichtigung.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
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
