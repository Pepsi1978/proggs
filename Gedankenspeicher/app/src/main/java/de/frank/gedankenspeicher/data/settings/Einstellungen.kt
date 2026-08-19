package de.frank.gedankenspeicher.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.gedankenspeicher.auth.VERBESSERUNG_AUFTRAG
import de.frank.gedankenspeicher.auth.VERBESSERUNG_AUFTRAG_ALT
import de.frank.gedankenspeicher.tts.TtsProvider

/**
 * **Die Einstellungen aus `01-FUNKTIONS-SPEC.md` §3 — verschlüsselt auf dem Gerät.**
 *
 * Auch die harmlosen Werte (Erscheinung, Stimme) liegen im verschlüsselten Speicher, obwohl
 * nur die Schlüssel es müssten. Zwei getrennte Ablagen wären zwei Stellen, an denen man beim
 * Sichern eine vergessen kann — und die Verschlüsselung kostet hier nichts.
 */
class Einstellungen(ctx: Context) {

    private val p: SharedPreferences = run {
        val schluessel = MasterKey.Builder(ctx.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx.applicationContext,
            "gedankenspeicher_einstellungen",
            schluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // --- F-15: Erscheinung ---------------------------------------------------------------
    /**
     * `hell` · `dunkel` · `goldHell` · `goldDunkel`.
     *
     * Es gibt **keine** Kopplung an die Systemeinstellung: Frank wählt selbst, und die Wahl
     * gilt, bis er sie ändert (F-15, Regeln).
     */
    var erscheinung: String
        get() = p.getString(ERSCHEINUNG, "goldDunkel")!!
        set(v) { p.edit().putString(ERSCHEINUNG, v).commit() }

    // --- F-11: Codex ---------------------------------------------------------------------
    var codexModell: String
        get() = p.getString(CODEX_MODELL, "gpt-5.6-luna")!!
        set(v) { p.edit().putString(CODEX_MODELL, v).commit() }

    var codexEffort: String
        get() = p.getString(CODEX_EFFORT, "medium")!!
        set(v) { p.edit().putString(CODEX_EFFORT, v).commit() }

    // --- F-07: Textverbesserung ------------------------------------------------------------
    /**
     * Die Textverbesserung bekommt ein **eigenes** Modell. Sie tut etwas anderes als die
     * Auswertung: sie denkt nicht nach, sie räumt auf. Dafür zählt Tempo, nicht Tiefe —
     * deshalb ist hier Luna vorbelegt, während die Auswertung beim grossen Modell bleiben darf.
     */
    var verbesserungModell: String
        get() = p.getString(VERBESSERUNG_MODELL, "gpt-5.6-luna")!!
        set(v) { p.edit().putString(VERBESSERUNG_MODELL, v).commit() }

    var verbesserungEffort: String
        get() = p.getString(VERBESSERUNG_EFFORT, "low")!!
        set(v) { p.edit().putString(VERBESSERUNG_EFFORT, v).commit() }

    /**
     * Der Auftrag, mit dem die Textverbesserung arbeitet — von Hand änderbar (B-04).
     *
     * Wer noch den alten Auftrag gespeichert hat, ohne ihn je angefasst zu haben, bekommt
     * den neuen. Sonst bliebe eine unveränderte Vorbelegung für immer die alte, und der
     * neue Auftrag käme nur bei einer frischen Installation an.
     */
    var verbesserungPrompt: String
        get() = p.getString(VERBESSERUNG_PROMPT, VERBESSERUNG_AUFTRAG)!!
            .let { if (it.trim() == VERBESSERUNG_AUFTRAG_ALT.trim()) VERBESSERUNG_AUFTRAG else it }
        set(v) { p.edit().putString(VERBESSERUNG_PROMPT, v).commit() }

    /**
     * Die Grundhaltung zur Websuche: `aus` · `immer` · `kiEntscheidet`.
     *
     * Im KI-Blatt (B-03) lässt sich sie für eine einzelne Auswertung überstimmen — deshalb
     * ist das hier nur die Vorbelegung, nicht die Entscheidung.
     */
    var websucheGrundhaltung: String
        get() = p.getString(WEBSUCHE, "aus")!!
        set(v) { p.edit().putString(WEBSUCHE, v).commit() }

    // --- F-18: Stimme --------------------------------------------------------------------
    /**
     * Vorbelegt ist Edge: es braucht Netz, aber keinen Schlüssel. Google Cloud als Standard
     * hätte zur Folge, dass ein frisch installiertes Gerät auf jeden Druck auf einen
     * Lautsprecher mit einer Fehlermeldung antwortet.
     */
    var ttsAnbieter: String
        get() = p.getString(TTS_ANBIETER, TtsProvider.EDGE.id)!!
        set(v) { p.edit().putString(TTS_ANBIETER, v).commit() }

    var stimmeEdge: String
        get() = p.getString(STIMME_EDGE, "de-DE-SeraphinaMultilingualNeural")!!
        set(v) { p.edit().putString(STIMME_EDGE, v).commit() }

    var stimmeGoogle: String
        get() = p.getString(STIMME_GOOGLE, "de-DE-Chirp3-HD-Kore")!!
        set(v) { p.edit().putString(STIMME_GOOGLE, v).commit() }

    var stimmeQwen: String
        get() = p.getString(STIMME_QWEN, "")!!
        set(v) { p.edit().putString(STIMME_QWEN, v).commit() }

    var sprechtempo: Float
        get() = p.getFloat(SPRECHTEMPO, 1.0f)
        set(v) { p.edit().putFloat(SPRECHTEMPO, v).commit() }

    // --- Schlüssel -----------------------------------------------------------------------
    var groqSchluessel: String
        get() = p.getString(GROQ, "")!!
        set(v) { p.edit().putString(GROQ, v).commit() }

    var googleTtsSchluessel: String
        get() = p.getString(GOOGLE_TTS, "")!!
        set(v) { p.edit().putString(GOOGLE_TTS, v).commit() }

    var qwenSchluessel: String
        get() = p.getString(QWEN, "")!!
        set(v) { p.edit().putString(QWEN, v).commit() }

    // --- F-13: zuletzt geöffnete Sitzung ---------------------------------------------------
    var offeneSitzung: Long
        get() = p.getLong(OFFENE_SITZUNG, 0L)
        set(v) { p.edit().putLong(OFFENE_SITZUNG, v).commit() }

    // --- Fingerabdruck ---------------------------------------------------------------------
    /**
     * Ist er an, verlangen geschützte Notizen den Fingerabdruck — zum Öffnen des Reiters
     * ebenso wie zum Schützen und zum Aufheben des Schutzes.
     */
    var fingerabdruckAn: Boolean
        get() = p.getBoolean(FINGERABDRUCK, false)
        set(v) { p.edit().putBoolean(FINGERABDRUCK, v).commit() }

    // --- F-17: Sicherung -------------------------------------------------------------------
    var driveSicherungAn: Boolean
        get() = p.getBoolean(DRIVE_AN, false)
        set(v) { p.edit().putBoolean(DRIVE_AN, v).commit() }

    /** Der von Frank gewählte Sicherungsordner (SAF-Berechtigung, bleibt beim Gerät). */
    var sicherungsordner: String
        get() = p.getString(DRIVE_ORDNER, "")!!
        set(v) { p.edit().putString(DRIVE_ORDNER, v).commit() }

    var letzteSicherungZeit: Long
        get() = p.getLong(DRIVE_ZEIT, 0L)
        set(v) { p.edit().putLong(DRIVE_ZEIT, v).commit() }

    var letzteSicherungGroesse: Long
        get() = p.getLong(DRIVE_GROESSE, 0L)
        set(v) { p.edit().putLong(DRIVE_GROESSE, v).commit() }

    private companion object {
        const val ERSCHEINUNG = "erscheinung"
        const val CODEX_MODELL = "codex_modell"
        const val CODEX_EFFORT = "codex_effort"
        const val VERBESSERUNG_MODELL = "verbesserung_modell"
        const val VERBESSERUNG_EFFORT = "verbesserung_effort"
        const val VERBESSERUNG_PROMPT = "verbesserung_prompt"
        const val WEBSUCHE = "websuche_grundhaltung"
        const val TTS_ANBIETER = "tts_anbieter"
        const val STIMME_EDGE = "stimme_edge"
        const val STIMME_GOOGLE = "stimme_google"
        const val STIMME_QWEN = "stimme_qwen"
        const val SPRECHTEMPO = "sprechtempo"
        const val GROQ = "groq_schluessel"
        const val GOOGLE_TTS = "google_tts_schluessel"
        const val QWEN = "qwen_schluessel"
        const val OFFENE_SITZUNG = "offene_sitzung"
        const val FINGERABDRUCK = "fingerabdruck_an"
        const val DRIVE_AN = "drive_an"
        const val DRIVE_ORDNER = "drive_ordner"
        const val DRIVE_ZEIT = "drive_zeit"
        const val DRIVE_GROESSE = "drive_groesse"
    }
}

/** Die drei Haltungen zur Websuche (F-09, Schritt 3). */
enum class Websuche(val id: String, val label: String) {
    AUS("aus", "aus"),
    IMMER("immer", "immer"),
    KI_ENTSCHEIDET("kiEntscheidet", "KI entscheidet"),
    ;

    companion object {
        fun vonId(v: String): Websuche = entries.firstOrNull { it.id == v } ?: AUS
    }
}
