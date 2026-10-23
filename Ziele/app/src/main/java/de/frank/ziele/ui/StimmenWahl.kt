package de.frank.ziele.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.data.Einstellungen
import de.frank.ziele.tts.NeueStimmenPlayer
import de.frank.ziele.tts.TtsCatalog
import de.frank.ziele.tts.TtsProvider
import de.frank.ziele.tts.TtsVoice
import de.frank.ziele.ui.theme.Chip
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d

/** Das Gelb der Favoritensterne. */
val FavoritGelb = Color(0xFFFFC107)

/**
 * Eine Stimmenliste als Chips: Tippen wählt, langes Drücken markiert als Favorit (oder nimmt heraus).
 * Favoriten stehen vorn und tragen einen gelben Stern.
 */
@Composable
fun StimmenReihe(stimmen: List<TtsVoice>, gewaehlt: String, e: Einstellungen, waehle: (String) -> Unit) {
    val favoriten = e.favoriten
    ChipReihe {
        stimmen.sortedByDescending { it.id in favoriten }.forEach { v ->
            StimmChip(v.name, aktiv = gewaehlt == v.id, favorit = v.id in favoriten,
                lang = { e.favoriten = if (v.id in favoriten) favoriten - v.id else favoriten + v.id }) { waehle(v.id) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StimmChip(text: String, aktiv: Boolean, favorit: Boolean, lang: () -> Unit, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = f.primaer
    val grund = if (aktiv) Modifier.knopf3d(c, c.copy(alpha = 0.82f), 999.dp, f.dunkel)
    else Modifier.glas(f, 999.dp, erhoeht = 0.4f, fuellung = f.flaeche)
    Row(
        Modifier.height(38.dp).then(grund).clip(RoundedCornerShape(999.dp))
            .combinedClickable(role = Role.Button, onLongClickLabel = if (favorit) "Favorit entfernen" else "Als Favorit markieren",
                onLongClick = lang, onClick = aktion)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (favorit) Text("★", color = FavoritGelb, fontSize = 14.sp)
        Text(text, color = if (aktiv) Color.White else f.text, fontSize = 14.sp, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/**
 * Die Rückfall-Reihenfolge des Vorlesens (wie in Jarvis): nummerierte Stufen mit ↑ ↓ ✕, darunter alle übrigen Stufen
 * zum Hinzufügen. Jede Stufe liest mit der Stimme, die bei ihrem Anbieter eingestellt ist.
 */
@Composable
fun RueckfallKette(e: Einstellungen) {
    val f = LocalFarben.current
    val kette = e.rueckfallKette
    val anbieter = e.ttsAnbieter
    Text("Rückfall-Reihenfolge", color = f.textLeise, fontSize = 13.sp)
    Text("Scheitert die Stimme oben (etwa Kontingent erschöpft), probiert Ziele diese Stufen der Reihe nach, jede mit der dort eingestellten Stimme. " +
        "Eine Stufe ohne Schlüssel wird übersprungen. Leer = kein Rückfall.",
        color = f.textLeise, fontSize = 12.sp)
    kette.forEachIndexed { i, id ->
        val stufe = TtsCatalog.rueckfallStufe(id) ?: return@forEachIndexed
        val gleich = e.stufeWieHaupt(stufe, anbieter)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${i + 1}. ${stufe.titel}", color = f.text, fontSize = 14.sp)
                if (gleich || !e.rueckfallBereit(stufe)) {
                    Text(if (gleich) "Versucht die Stimme oben schon, wird übersprungen" else "Nicht eingerichtet, wird übersprungen", color = f.textLeise, fontSize = 11.sp)
                }
            }
            if (i > 0) Chip("↑", false) { e.rueckfallKette = kette.toMutableList().apply { add(i - 1, removeAt(i)) } }
            if (i < kette.lastIndex) Chip("↓", false) { e.rueckfallKette = kette.toMutableList().apply { add(i + 1, removeAt(i)) } }
            Chip("✕", false) { e.rueckfallKette = kette - id }
        }
    }
    val uebrig = TtsCatalog.rueckfallStufen.filter { it.id !in kette }
    if (uebrig.isNotEmpty()) {
        Text("Hinzufügen:", color = f.textLeise, fontSize = 12.sp)
        ChipReihe { uebrig.forEach { st -> Chip("+ ${st.titel}", false) { e.rueckfallKette = kette + st.id } } }
    }
    // Bewusst ohne isBlank-Bedingung: Sonst verschwände das Feld nach dem ersten getippten Zeichen.
    if (TtsProvider.GOOGLE_CLOUD.id in kette && anbieter != TtsProvider.GOOGLE_CLOUD.id) {
        Feld("Google-Cloud-Schlüssel (für den Rückfall)", e.googleKey, geheim = true) { e.googleKey = it }
    }
    if (TtsProvider.AZURE.id in kette && anbieter != TtsProvider.AZURE.id) {
        Feld("Azure-Speech-Schlüssel (für den Rückfall)", e.azureKey, geheim = true) { e.azureKey = it }
    }
}

/** Schlüssel und Stimmen für Gemini 3.8 Flash / Flash-Lite TTS und Azure Neural (wie in Jarvis). */
@Composable
fun NeueStimmenBereich(e: Einstellungen) {
    val f = LocalFarben.current
    when (e.ttsAnbieter) {
        TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id -> {
            val flash = e.ttsAnbieter == TtsProvider.GEMINI_FLASH.id
            Feld("Gemini-Stimme: Schlüssel kostenlos (Projekt ohne Abrechnung)", e.geminiKeyFrei, geheim = true) { e.geminiKeyFrei = it }
            Feld("Gemini-Stimme: Schlüssel bezahlt (Projekt mit Abrechnung)", e.geminiKeyBezahlt, geheim = true) { e.geminiKeyBezahlt = it }
            Text("Welcher Schlüssel", color = f.textLeise, fontSize = 13.sp)
            ChipReihe {
                NeueStimmenPlayer.GEMINI_TTS_SCHLUESSEL_WAHLEN.forEach { (id, name) ->
                    Chip(name, e.geminiSchluesselWahl == id) { e.geminiSchluesselWahl = id }
                }
            }
            Text("Was danach kommt, wenn der Schlüssel scheitert, legst du unten in der Rückfall-Reihenfolge fest.",
                color = f.textLeise, fontSize = 12.sp)
            if (e.geminiKeyFrei.isBlank() && e.geminiKeyBezahlt.isBlank()) {
                Text("Für die Gemini-Stimmen fehlt noch ein Schlüssel (aistudio.google.com).", color = f.gefahr, fontSize = 12.sp)
            }
            Text("Stimme (langes Drücken: ★ Favorit)", color = f.textLeise, fontSize = 13.sp)
            if (flash) StimmenReihe(TtsCatalog.geminiFlashVoices, e.geminiFlashStimme, e) { e.geminiFlashStimme = it }
            else StimmenReihe(TtsCatalog.geminiLiteVoices, e.geminiLiteStimme, e) { e.geminiLiteStimme = it }
        }
        TtsProvider.AZURE.id -> {
            Feld("Azure-Speech-Schlüssel (KEY 1)", e.azureKey, geheim = true) { e.azureKey = it }
            Feld("Azure-Region (z. B. westeurope)", e.azureRegion) { e.azureRegion = it }
            if (e.azureKey.isBlank()) {
                Text("Für die Azure-Stimmen fehlt noch der Schlüssel deiner Speech-Ressource.", color = f.gefahr, fontSize = 12.sp)
            }
            Text("Stimme (langes Drücken: ★ Favorit)", color = f.textLeise, fontSize = 13.sp)
            StimmenReihe(TtsCatalog.azureVoices, e.azureStimme, e) { e.azureStimme = it }
        }
    }
}
