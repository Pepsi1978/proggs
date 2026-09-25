package de.frank.newskompass.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.newskompass.ai.CodexModell
import de.frank.newskompass.data.model.BildModus
import de.frank.newskompass.data.model.Denkstufen
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.data.model.TtsAnbieter
import de.frank.newskompass.observability.KompassLog
import de.frank.newskompass.tts.TtsCatalog
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Momentaufnahme aller Einstellungen — die Oberfläche beobachtet genau diese eine Grösse. */
data class EinstellungenStand(
    val themen: List<Thema>,
    val modellId: String,
    val denktiefe: String,
    val modelle: List<CodexModell>,
    val bildModus: BildModus,
    val maxKiBilder: Int,
    val bilderUnterstuetzt: Boolean,
    val design: DesignModus,
    val ttsAnbieter: TtsAnbieter,
    val googleStimme: String,
    val edgeStimme: String,
    val qwenStimmeId: String,
    val sprechtempo: Float,
    val hatGoogleSchluessel: Boolean,
    val hatAlibabaSchluessel: Boolean,
    val zeitplanAktiv: Boolean,
)

class EinstellungenStore(context: Context) {

    private val offen: SharedPreferences =
        context.getSharedPreferences("newskompass_prefs", Context.MODE_PRIVATE)

    private val geheim: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val schluessel = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "newskompass_secure_prefs",
            schluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _stand = MutableStateFlow(lies())
    val stand: StateFlow<EinstellungenStand> = _stand.asStateFlow()

    // --- Werte, die der Vorlese-Manager direkt liest ----------------------------------------

    val ttsAnbieter: TtsAnbieter get() = TtsAnbieter.fromId(offen.getString(K_TTS, null).orEmpty())
    val googleStimme: String get() = offen.getString(K_GOOGLE_STIMME, null) ?: TtsCatalog.STANDARD_GOOGLE_STIMME
    val edgeStimme: String get() = offen.getString(K_EDGE_STIMME, null) ?: TtsCatalog.STANDARD_EDGE_STIMME
    val qwenStimmeId: String get() = offen.getString(K_QWEN_STIMME, null).orEmpty()
    val sprechtempo: Float get() = offen.getFloat(K_TEMPO, 1.0f)
    val googleSchluessel: String get() = geheim.getString(K_GOOGLE_KEY, null).orEmpty()
    val alibabaSchluessel: String get() = geheim.getString(K_ALIBABA_KEY, null).orEmpty()

    // --- Lesen -------------------------------------------------------------------------------

    private fun lies(): EinstellungenStand {
        val modelle = leseModelle()
        val modellId = offen.getString(K_MODELL, null) ?: STANDARD_MODELL
        return EinstellungenStand(
            themen = leseThemen(),
            modellId = modellId,
            denktiefe = offen.getString(K_DENKTIEFE, null) ?: "medium",
            modelle = modelle,
            bildModus = BildModus.fromId(offen.getString(K_BILDMODUS, null)),
            maxKiBilder = offen.getInt(K_MAX_KI, 8),
            bilderUnterstuetzt = offen.getBoolean(K_BILDER_OK, true),
            design = DesignModus.fromId(offen.getString(K_DESIGN, null)),
            ttsAnbieter = ttsAnbieter,
            googleStimme = googleStimme,
            edgeStimme = edgeStimme,
            qwenStimmeId = qwenStimmeId,
            sprechtempo = sprechtempo,
            hatGoogleSchluessel = runCatching { googleSchluessel.isNotBlank() }.getOrDefault(false),
            hatAlibabaSchluessel = runCatching { alibabaSchluessel.isNotBlank() }.getOrDefault(false),
            zeitplanAktiv = offen.getBoolean(K_ZEITPLAN, true),
        )
    }

    private fun leseThemen(): List<Thema> {
        val roh = offen.getString(K_THEMEN, null) ?: return listOf(Thema(UUID.randomUUID().toString(), STANDARD_KI_THEMA))
        return runCatching {
            val liste = JSONArray(roh)
            (0 until liste.length()).map {
                val eintrag = liste.getJSONObject(it)
                Thema(eintrag.getString("id"), eintrag.optString("text"))
            }
        }.getOrElse {
            KompassLog.warn("Einstellungen", "leseThemen", "Themenliste unlesbar", mapOf("grund" to it.message))
            listOf(Thema(UUID.randomUUID().toString(), STANDARD_KI_THEMA))
        }
    }

    private fun leseModelle(): List<CodexModell> {
        val roh = offen.getString(K_MODELLE, null) ?: return Denkstufen.bekannteModelle
        return runCatching {
            val liste = JSONArray(roh)
            (0 until liste.length()).map {
                val m = liste.getJSONObject(it)
                val stufen = m.getJSONArray("stufen")
                CodexModell(
                    m.getString("id"),
                    m.getString("name"),
                    (0 until stufen.length()).map(stufen::getString),
                    m.optString("standard", "medium"),
                )
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: Denkstufen.bekannteModelle
    }

    // --- Schreiben ---------------------------------------------------------------------------

    private fun schreibe(block: SharedPreferences.Editor.() -> Unit) {
        offen.edit().apply(block).apply()
        _stand.value = lies()
    }

    fun setzeThemen(themen: List<Thema>) = schreibe {
        val liste = JSONArray()
        themen.forEach { liste.put(JSONObject().put("id", it.id).put("text", it.text)) }
        putString(K_THEMEN, liste.toString())
    }

    fun setzeModell(id: String) = schreibe {
        putString(K_MODELL, id)
        // Kennt das neue Modell die bisherige Denktiefe nicht, auf dessen Standard wechseln.
        val modell = _stand.value.modelle.firstOrNull { it.id == id }
        val tiefe = offen.getString(K_DENKTIEFE, null) ?: "medium"
        if (modell != null && tiefe !in modell.stufen) putString(K_DENKTIEFE, modell.standardStufe)
    }

    fun setzeDenktiefe(stufe: String) = schreibe { putString(K_DENKTIEFE, stufe) }

    fun setzeModelle(modelle: List<CodexModell>) {
        if (modelle.isEmpty()) return
        schreibe {
            val liste = JSONArray()
            modelle.forEach {
                liste.put(
                    JSONObject().put("id", it.id).put("name", it.name)
                        .put("stufen", JSONArray(it.stufen)).put("standard", it.standardStufe),
                )
            }
            putString(K_MODELLE, liste.toString())
        }
    }

    fun setzeBildModus(modus: BildModus) = schreibe { putString(K_BILDMODUS, modus.id) }
    fun setzeMaxKiBilder(anzahl: Int) = schreibe { putInt(K_MAX_KI, anzahl.coerceIn(0, 30)) }
    fun setzeBilderUnterstuetzt(ja: Boolean) = schreibe { putBoolean(K_BILDER_OK, ja) }
    fun setzeDesign(modus: DesignModus) = schreibe { putString(K_DESIGN, modus.id) }
    fun setzeTtsAnbieter(anbieter: TtsAnbieter) = schreibe { putString(K_TTS, anbieter.id) }
    fun setzeGoogleStimme(id: String) = schreibe { putString(K_GOOGLE_STIMME, id) }
    fun setzeEdgeStimme(id: String) = schreibe { putString(K_EDGE_STIMME, id) }
    fun setzeQwenStimme(id: String) = schreibe { putString(K_QWEN_STIMME, id) }
    fun setzeTempo(tempo: Float) = schreibe { putFloat(K_TEMPO, tempo.coerceIn(0.6f, 1.6f)) }
    fun setzeZeitplan(aktiv: Boolean) = schreibe { putBoolean(K_ZEITPLAN, aktiv) }

    fun setzeGoogleSchluessel(wert: String) {
        geheim.edit().putString(K_GOOGLE_KEY, wert.trim()).apply()
        _stand.value = lies()
    }

    fun setzeAlibabaSchluessel(wert: String) {
        geheim.edit().putString(K_ALIBABA_KEY, wert.trim()).apply()
        _stand.value = lies()
    }

    companion object {
        const val STANDARD_MODELL = "gpt-6-sol"

        /** Der erste Block — frei editierbar, ohne eigene Überschrift. */
        const val STANDARD_KI_THEMA =
            "KI-News: Was gibt es Neues bei den großen Frontier-Modellen von OpenAI, Anthropic, " +
                "Google, Meta, xAI, Mistral, DeepSeek und Qwen, und was bewegt die KI-Welt allgemein " +
                "— neue Modelle, Releases, Forschung, Firmen, Regulierung."

        private const val K_THEMEN = "themen"
        private const val K_MODELL = "modell"
        private const val K_DENKTIEFE = "denktiefe"
        private const val K_MODELLE = "modelle_cache"
        private const val K_BILDMODUS = "bildmodus"
        private const val K_MAX_KI = "max_ki_bilder"
        private const val K_BILDER_OK = "bilder_unterstuetzt"
        private const val K_DESIGN = "design"
        private const val K_TTS = "tts_anbieter"
        private const val K_GOOGLE_STIMME = "google_stimme"
        private const val K_EDGE_STIMME = "edge_stimme"
        private const val K_QWEN_STIMME = "qwen_stimme"
        private const val K_TEMPO = "tempo"
        private const val K_ZEITPLAN = "zeitplan"
        private const val K_GOOGLE_KEY = "google_key"
        private const val K_ALIBABA_KEY = "alibaba_key"
    }
}
