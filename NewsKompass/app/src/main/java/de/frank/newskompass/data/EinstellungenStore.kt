package de.frank.newskompass.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.newskompass.ai.CodexModell
import de.frank.newskompass.data.model.Ausfuehrlichkeit
import de.frank.newskompass.data.model.BildModus
import de.frank.newskompass.data.model.Denkstufen
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.data.model.FarbDesign
import de.frank.newskompass.data.model.Rhythmus
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.data.model.TtsAnbieter
import de.frank.newskompass.observability.KompassLog
import de.frank.newskompass.tts.TtsCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Momentaufnahme aller Einstellungen — die Oberfläche beobachtet genau diese eine Grösse. */
@androidx.compose.runtime.Immutable
data class EinstellungenStand(
    val themen: List<Thema>,
    val modellId: String,
    val denktiefe: String,
    /** Live-Katalog plus die per KI gefundenen und geprüften Modelle. */
    val modelle: List<CodexModell>,
    /** Per KI-Suche gefunden und mit einer Probeanfrage bestätigt — fehlen (noch) im Live-Katalog. */
    val kiModelle: List<CodexModell>,
    val bildModus: BildModus,
    val ausfuehrlichkeit: Ausfuehrlichkeit,
    val maxKiBilder: Int,
    val bilderUnterstuetzt: Boolean,
    val design: DesignModus,
    val farbDesign: FarbDesign,
    val ttsAnbieter: TtsAnbieter,
    val googleStimme: String,
    val edgeStimme: String,
    val qwenStimmeId: String,
    val sprechtempo: Float,
    val hatGoogleSchluessel: Boolean,
    val hatAlibabaSchluessel: Boolean,
    val hatGroqSchluessel: Boolean,
    val zeitplanAktiv: Boolean,
    /** Letzte vollständig geprüfte Archivsicherung (0 = noch keine) und ihre Kurzbeschreibung. */
    val letzteSicherungUm: Long,
    val letzteSicherungText: String,
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

    init {
        // Die bisher globale Auswahl einmal in bestehende Themen übernehmen und fest speichern.
        // Danach darf der Regler für freie Sprachfragen die Themen nicht mehr verändern.
        val roh = offen.getString(K_THEMEN, null)
        val brauchtMigration = roh == null || runCatching {
            val liste = JSONArray(roh)
            (0 until liste.length()).any { !liste.getJSONObject(it).has("ausfuehrlichkeit") }
        }.getOrDefault(false)
        if (brauchtMigration) setzeThemen(_stand.value.themen)
    }

    // --- Werte, die der Vorlese-Manager direkt liest ----------------------------------------

    val ttsAnbieter: TtsAnbieter get() = TtsAnbieter.fromId(offen.getString(K_TTS, null).orEmpty())
    val googleStimme: String get() = offen.getString(K_GOOGLE_STIMME, null) ?: TtsCatalog.STANDARD_GOOGLE_STIMME
    val edgeStimme: String get() = offen.getString(K_EDGE_STIMME, null) ?: TtsCatalog.STANDARD_EDGE_STIMME
    val qwenStimmeId: String get() = offen.getString(K_QWEN_STIMME, null).orEmpty()
    val sprechtempo: Float get() = offen.getFloat(K_TEMPO, 1.0f)
    val googleSchluessel: String get() = geheim.getString(K_GOOGLE_KEY, null).orEmpty()
    val alibabaSchluessel: String get() = geheim.getString(K_ALIBABA_KEY, null).orEmpty()
    val groqSchluessel: String get() = geheim.getString(K_GROQ_KEY, null).orEmpty()

    // --- Lesen -------------------------------------------------------------------------------

    private fun lies(): EinstellungenStand {
        val katalog = leseModelle()
        val kiModelle = leseKiModelle().filter { ki -> katalog.none { it.id == ki.id } }
        val modelle = katalog + kiModelle
        val modellId = offen.getString(K_MODELL, null) ?: STANDARD_MODELL
        return EinstellungenStand(
            themen = leseThemen(),
            modellId = modellId,
            denktiefe = offen.getString(K_DENKTIEFE, null) ?: "medium",
            modelle = modelle,
            kiModelle = kiModelle,
            bildModus = BildModus.fromId(offen.getString(K_BILDMODUS, null)),
            ausfuehrlichkeit = Ausfuehrlichkeit.fromId(offen.getString(K_AUSFUEHRLICHKEIT, null)),
            maxKiBilder = offen.getInt(K_MAX_KI, 8),
            bilderUnterstuetzt = offen.getBoolean(K_BILDER_OK, true),
            design = DesignModus.fromId(offen.getString(K_DESIGN, null)),
            farbDesign = FarbDesign.fromId(offen.getString(K_FARBDESIGN, null)),
            ttsAnbieter = ttsAnbieter,
            googleStimme = googleStimme,
            edgeStimme = edgeStimme,
            qwenStimmeId = qwenStimmeId,
            sprechtempo = sprechtempo,
            hatGoogleSchluessel = runCatching { googleSchluessel.isNotBlank() }.getOrDefault(false),
            hatAlibabaSchluessel = runCatching { alibabaSchluessel.isNotBlank() }.getOrDefault(false),
            hatGroqSchluessel = runCatching { groqSchluessel.isNotBlank() }.getOrDefault(false),
            zeitplanAktiv = offen.getBoolean(K_ZEITPLAN, true),
            letzteSicherungUm = offen.getLong(K_SICHERUNG_UM, 0L),
            letzteSicherungText = offen.getString(K_SICHERUNG_TEXT, null).orEmpty(),
        )
    }

    private fun leseThemen(): List<Thema> {
        // Das Standardthema trägt eine feste ID: Solange die Liste nie gespeichert wurde, entsteht es bei
        // jedem Lesen neu — mit zufälliger ID fände der Wecker sein Thema im nächsten Prozess nicht mehr.
        val bisherigeAusfuehrlichkeit = Ausfuehrlichkeit.fromId(offen.getString(K_AUSFUEHRLICHKEIT, null))
        val roh = offen.getString(K_THEMEN, null)
            ?: return listOf(Thema(STANDARD_THEMA_ID, STANDARD_KI_THEMA, ausfuehrlichkeit = bisherigeAusfuehrlichkeit))
        return runCatching {
            val liste = JSONArray(roh)
            (0 until liste.length()).map {
                val eintrag = liste.getJSONObject(it)
                // Ältere Einträge kennen nur id und text — sie bekommen den bisherigen Rahmen 4 bis 7
                // und den bisherigen Zeitplan 5 und 17 Uhr.
                val zeiten = eintrag.optJSONArray("uhrzeiten")
                Thema(
                    eintrag.getString("id"),
                    eintrag.optString("text"),
                    eintrag.optInt("min", Thema.STANDARD_MIN),
                    eintrag.optInt("max", Thema.STANDARD_MAX),
                    zeiten?.let { z -> (0 until z.length()).map(z::getInt) } ?: Thema.STANDARD_UHRZEITEN,
                    Rhythmus.ausJson(eintrag.optJSONObject("rhythmus")),
                    eintrag.optString("ueberschrift"),
                    eintrag.optString("ueberschriftFuer"),
                    Ausfuehrlichkeit.fromId(eintrag.optString("ausfuehrlichkeit", bisherigeAusfuehrlichkeit.id)),
                ).normiert()
            }
        }.getOrElse {
            KompassLog.warn("Einstellungen", "leseThemen", "Themenliste unlesbar", mapOf("grund" to it.message))
            listOf(Thema(STANDARD_THEMA_ID, STANDARD_KI_THEMA))
        }
    }

    private fun leseModelle(): List<CodexModell> =
        leseModellListe(offen.getString(K_MODELLE, null))?.takeIf { it.isNotEmpty() } ?: Denkstufen.bekannteModelle

    private fun leseKiModelle(): List<CodexModell> = leseModellListe(offen.getString(K_KI_MODELLE, null)).orEmpty()

    private fun leseModellListe(roh: String?): List<CodexModell>? {
        if (roh == null) return null
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
        }.getOrNull()
    }

    private fun modellJson(modelle: List<CodexModell>): String {
        val liste = JSONArray()
        modelle.forEach {
            liste.put(
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("stufen", JSONArray(it.stufen)).put("standard", it.standardStufe),
            )
        }
        return liste.toString()
    }

    // --- Schreiben ---------------------------------------------------------------------------

    /** Synchronisiert: Oberfläche und Hintergrundaufträge schreiben — sonst gewinnt womöglich ein veralteter Stand. */
    @Synchronized
    private fun schreibe(block: SharedPreferences.Editor.() -> Unit) {
        offen.edit().apply(block).apply()
        _stand.value = lies()
    }

    fun setzeThemen(themen: List<Thema>) = schreibe {
        val liste = JSONArray()
        themen.map(Thema::normiert).forEach {
            liste.put(
                JSONObject().put("id", it.id).put("text", it.text).put("min", it.minMeldungen).put("max", it.maxMeldungen)
                    .put("uhrzeiten", JSONArray(it.uhrzeiten)).put("rhythmus", it.rhythmus.zuJson())
                    .put("ueberschrift", it.ueberschrift).put("ueberschriftFuer", it.ueberschriftFuer)
                    .put("ausfuehrlichkeit", it.ausfuehrlichkeit.id),
            )
        }
        putString(K_THEMEN, liste.toString())
    }

    fun setzeModell(id: String) = schreibe {
        putString(K_MODELL, id)
        // Kennt das neue Modell die bisherige Denktiefe nicht, auf dessen Standard wechseln.
        val modell = _stand.value.modelle.firstOrNull { it.id == id }
        val tiefe = offen.getString(K_DENKTIEFE, null) ?: "medium"
        if (modell != null && tiefe !in modell.stufen) putString(K_DENKTIEFE, modell.standardStufe)
    }

    /** Hängt geprüfte KI-Funde an; ein schon gespeicherter Fund mit gleicher Kennung wird ersetzt. */
    fun merkeKiModelle(neu: List<CodexModell>) = schreibe {
        val alt = leseKiModelle().filter { a -> neu.none { it.id == a.id } }
        putString(K_KI_MODELLE, modellJson(alt + neu))
    }

    /** Entfernt einen KI-Fund wieder; war er gewählt, gilt danach das Standardmodell. */
    fun vergissKiModell(id: String) = schreibe {
        putString(K_KI_MODELLE, modellJson(leseKiModelle().filter { it.id != id }))
        if (offen.getString(K_MODELL, null) == id) {
            remove(K_MODELL)
            // Die Denktiefe muss zum Standardmodell passen, sonst lehnt der Dienst jede Anfrage ab.
            val ersatz = _stand.value.modelle.firstOrNull { it.id == STANDARD_MODELL }
            val tiefe = offen.getString(K_DENKTIEFE, null) ?: "medium"
            if (ersatz != null && ersatz.stufen.isNotEmpty() && tiefe !in ersatz.stufen) putString(K_DENKTIEFE, ersatz.standardStufe)
            else if (ersatz == null && tiefe !in listOf("low", "medium", "high")) putString(K_DENKTIEFE, "medium")
        }
    }

    fun setzeDenktiefe(stufe: String) = schreibe { putString(K_DENKTIEFE, stufe) }

    fun setzeModelle(modelle: List<CodexModell>) {
        // Unveränderte Liste nicht neu schreiben — sonst zeichnet die ganze Einstellungsseite neu, sobald die Modelle ankommen.
        // Verglichen wird mit dem gespeicherten Katalog — der Stand enthält zusätzlich die KI-Funde.
        if (modelle.isEmpty() || modelle == leseModellListe(offen.getString(K_MODELLE, null))) return
        schreibe {
            putString(K_MODELLE, modellJson(modelle))
            // Steht ein KI-Fund inzwischen im Katalog, gilt der Katalog — der Fund wird nicht mehr gebraucht.
            val ki = leseKiModelle()
            if (ki.any { k -> modelle.any { it.id == k.id } }) putString(K_KI_MODELLE, modellJson(ki.filter { k -> modelle.none { it.id == k.id } }))
            // Kennt das gewählte Modell laut neuer Liste die bisherige Denktiefe nicht mehr, auf dessen Standard wechseln —
            // sonst lehnt der Dienst jede Anfrage ab.
            val gewaehlt = modelle.firstOrNull { it.id == (offen.getString(K_MODELL, null) ?: STANDARD_MODELL) }
            val tiefe = offen.getString(K_DENKTIEFE, null) ?: "medium"
            if (gewaehlt != null && gewaehlt.stufen.isNotEmpty() && tiefe !in gewaehlt.stufen) putString(K_DENKTIEFE, gewaehlt.standardStufe)
        }
    }

    fun setzeBildModus(modus: BildModus) = schreibe { putString(K_BILDMODUS, modus.id) }
    fun setzeAusfuehrlichkeit(stufe: Ausfuehrlichkeit) = schreibe { putString(K_AUSFUEHRLICHKEIT, stufe.id) }
    fun setzeMaxKiBilder(anzahl: Int) = schreibe { putInt(K_MAX_KI, anzahl.coerceIn(0, 30)) }
    fun setzeBilderUnterstuetzt(ja: Boolean) = schreibe { putBoolean(K_BILDER_OK, ja) }
    fun setzeDesign(modus: DesignModus) = schreibe { putString(K_DESIGN, modus.id) }
    fun setzeFarbDesign(design: FarbDesign) = schreibe { putString(K_FARBDESIGN, design.id) }
    fun setzeTtsAnbieter(anbieter: TtsAnbieter) = schreibe { putString(K_TTS, anbieter.id) }
    fun setzeGoogleStimme(id: String) = schreibe { putString(K_GOOGLE_STIMME, id) }
    fun setzeEdgeStimme(id: String) = schreibe { putString(K_EDGE_STIMME, id) }
    fun setzeQwenStimme(id: String) = schreibe { putString(K_QWEN_STIMME, id) }
    fun setzeTempo(tempo: Float) = schreibe { putFloat(K_TEMPO, tempo.coerceIn(0.6f, 1.6f)) }
    fun setzeZeitplan(aktiv: Boolean) = schreibe { putBoolean(K_ZEITPLAN, aktiv) }
    fun merkeSicherung(zeit: Long, text: String) = schreibe {
        putLong(K_SICHERUNG_UM, zeit)
        putString(K_SICHERUNG_TEXT, text)
    }

    /**
     * Zeitpunkt des letzten Laufs, der an Kontingent oder Anmeldung gescheitert ist. Solange er
     * nach dem letzten Termin liegt, holt der App-Start keinen Lauf nach — sonst würde jedes
     * Öffnen einen neuen, genauso scheiternden Lauf anstoßen.
     */
    val harterFehlerUm: Long get() = offen.getLong(K_HARTER_FEHLER, 0L)

    fun merkeHartenFehler(zeit: Long) {
        offen.edit().putLong(K_HARTER_FEHLER, zeit).apply()
    }

    // --- Offene Läufe -------------------------------------------------------------------------

    /**
     * Termine, die noch recherchiert werden müssen: je Themen-ID der Zeitpunkt des Termins, dazu
     * „alle“ für den Knopf Aktualisieren. Jeder Wecker trägt seine Themen hier ein, bevor er den
     * Lauf anstößt — so geht kein Termin verloren, auch wenn gerade ein anderer Lauf wartet oder läuft.
     */
    @Synchronized
    fun merkeOffenenLauf(auftrag: Map<String, Long>?) {
        val (alle, termine) = leseOffeneLaeufe()
        val neu = termine.toMutableMap()
        auftrag?.forEach { (id, um) -> neu[id] = maxOf(neu[id] ?: 0L, um) }
        schreibeOffeneLaeufe(alle || auftrag == null, neu)
    }

    /**
     * Die offenen Termine, ohne sie herauszunehmen; `null`, wenn keiner offen ist. Erst wenn sie erledigt
     * sind, nimmt [entferneOffeneLaeufe] sie heraus — so überleben sie auch das Ende des Prozesses mitten im Lauf.
     */
    @Synchronized
    fun offeneLaeufe(): Pair<Boolean, Map<String, Long>>? {
        val offenJetzt = leseOffeneLaeufe()
        return offenJetzt.takeIf { it.first || it.second.isNotEmpty() }
    }

    /** Nimmt die erledigten Termine heraus; inzwischen neu vorgemerkte (späterer Zeitpunkt) bleiben stehen. */
    @Synchronized
    fun entferneOffeneLaeufe(alle: Boolean, termine: Map<String, Long>) {
        val (jetztAlle, jetzt) = leseOffeneLaeufe()
        val rest = jetzt.filter { (id, um) -> termine[id]?.let { um > it } ?: true }
        schreibeOffeneLaeufe(jetztAlle && !alle, rest)
    }

    val hatOffeneLaeufe: Boolean
        @Synchronized get() = leseOffeneLaeufe().let { it.first || it.second.isNotEmpty() }

    private fun leseOffeneLaeufe(): Pair<Boolean, Map<String, Long>> {
        val alle = offen.getBoolean(K_OFFEN_ALLE, false)
        val termine = runCatching {
            val j = JSONObject(offen.getString(K_OFFENE_TERMINE, null) ?: "{}")
            j.keys().asSequence().associateWith { j.getLong(it) }
        }.getOrDefault(emptyMap())
        return alle to termine
    }

    private fun schreibeOffeneLaeufe(alle: Boolean, termine: Map<String, Long>) {
        // commit statt apply: Der Lauf kann gleich danach in einem anderen Thread starten.
        offen.edit()
            .putBoolean(K_OFFEN_ALLE, alle)
            .putString(K_OFFENE_TERMINE, JSONObject(termine as Map<*, *>).toString())
            .commit()
    }

    @Synchronized
    fun setzeGoogleSchluessel(wert: String) {
        geheim.edit().putString(K_GOOGLE_KEY, wert.trim()).apply()
        _stand.value = lies()
    }

    @Synchronized
    fun setzeAlibabaSchluessel(wert: String) {
        geheim.edit().putString(K_ALIBABA_KEY, wert.trim()).apply()
        _stand.value = lies()
    }

    @Synchronized
    fun setzeGroqSchluessel(wert: String) {
        geheim.edit().putString(K_GROQ_KEY, wert.trim()).apply()
        _stand.value = lies()
    }

    companion object {
        const val STANDARD_MODELL = "gpt-6-sol"

        /** Feste ID des vorbelegten KI-Themas, solange die Themenliste noch nie gespeichert wurde. */
        const val STANDARD_THEMA_ID = "standard-ki"

        /** Der erste Block — frei editierbar, ohne eigene Überschrift. */
        const val STANDARD_KI_THEMA =
            "KI-News: Was gibt es Neues bei den großen Frontier-Modellen von OpenAI, Anthropic, " +
                "Google, Meta, xAI, Mistral, DeepSeek und Qwen, und was bewegt die KI-Welt allgemein " +
                "— neue Modelle, Releases, Forschung, Firmen, Regulierung."

        private const val K_THEMEN = "themen"
        private const val K_MODELL = "modell"
        private const val K_DENKTIEFE = "denktiefe"
        private const val K_MODELLE = "modelle_cache"
        private const val K_KI_MODELLE = "modelle_ki"
        private const val K_BILDMODUS = "bildmodus"
        private const val K_MAX_KI = "max_ki_bilder"
        private const val K_AUSFUEHRLICHKEIT = "ausfuehrlichkeit"
        private const val K_HARTER_FEHLER = "harter_fehler_um"
        private const val K_OFFEN_ALLE = "offen_alle"
        private const val K_OFFENE_TERMINE = "offene_termine"
        private const val K_BILDER_OK = "bilder_unterstuetzt"
        private const val K_DESIGN = "design"
        private const val K_FARBDESIGN = "farbdesign"
        private const val K_TTS = "tts_anbieter"
        private const val K_GOOGLE_STIMME = "google_stimme"
        private const val K_EDGE_STIMME = "edge_stimme"
        private const val K_QWEN_STIMME = "qwen_stimme"
        private const val K_TEMPO = "tempo"
        private const val K_ZEITPLAN = "zeitplan"
        private const val K_SICHERUNG_UM = "sicherung_um"
        private const val K_SICHERUNG_TEXT = "sicherung_text"
        private const val K_GOOGLE_KEY = "google_key"
        private const val K_ALIBABA_KEY = "alibaba_key"
        private const val K_GROQ_KEY = "groq_key"
    }
}
