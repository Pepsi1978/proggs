package de.frank.claudekompass

import de.frank.kompass.update.ProtokollErnte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die Ernte aus dem Änderungsprotokoll — die zweite Quelle des Aktualisieren-Knopfs.
 *
 * Die Zeilen stammen wörtlich aus dem echten Protokoll. Sie stehen hier, weil jede einzelne
 * eine Falle ist, in die eine lose Suche tappt: Pfade in Klammern sehen aus wie Befehle,
 * Wörter in Rückstrichen sehen aus wie Einstellungen, und fremde Umgebungsvariablen sehen
 * aus wie eigene.
 */
class ProtokollErnteTest {

    private val protokoll = """
        # Changelog

        ## 2.1.269

        - Added `/output-style [name]` to list and switch output styles, including over Remote Control
        - Added a diff of the files a Bash command changed to the Bash tool result when the Bash tool handles file edits (setting `bashEditDiffEnabled`)
        - Added `CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS` (1-256) to raise the Workflow tool's per-run concurrent agent limit
        - Fixed remote and headless sessions reporting "waiting for your input" (set `CLAUDE_CODE_BG_TASKS_REPORT_RUNNING=0` to restore the old behavior)

        ## 2.1.268

        - Fixed deny and ask permission rules on symlinked directories (`/etc`, `/tmp`, `/var` on macOS; `/bin` elsewhere)
        - Added the `gatewayInternalNetworks` managed setting, letting administrators allow `/login` to a gateway

        ## 2.1.240

        - Removed `/altbefehl` — use `/config` instead
        - Renamed `/frueher` to `/heute` for clarity

        ## 2.1.239

        - Added `/frueher` to do something
        - Added `/heute` to do the same thing under its new name
        - Fixed Ctrl+W and vim `df`/`dt` leaving a broken state, respecting the `editor` setting

        ## 2.1.224

        - Fixed worktree-isolated subagents redirecting git into the shared checkout via `GIT_DIR` and `GIT_WORK_TREE`

        ## 2.1.172

        - `/code-review` now keeps the `ultra` option visible when you're not signed in

        ## 2.1.121

        - [VSCode] Voice dictation now respects the `accessibility.voice.speechLanguage` setting

        ## 2.0.96

        - Deprecated `/output-style` command — use `/config` instead
        - Added `/altbefehl` to do something old
    """.trimIndent()

    private fun namen(eintraege: List<de.frank.kompass.update.GelesenerEintrag>) =
        eintraege.map { it.name }

    @Test
    fun `der wieder eingefuehrte Befehl steht drin`() {
        val gefunden = namen(ProtokollErnte.leseSlashBefehle(protokoll))
        // Der Fall, wegen dem es diese Quelle gibt: abgekündigt in 2.0.96, zurück in 2.1.269.
        // Die Befehlstabelle der Doku führt ihn bis heute nicht.
        assertTrue("/output-style fehlt", "/output-style" in gefunden)
    }

    @Test
    fun `Pfade in Klammern sind keine Befehle`() {
        val gefunden = namen(ProtokollErnte.leseSlashBefehle(protokoll))
        listOf("/etc", "/tmp", "/var", "/bin", "/login", "/config", "/code-review").forEach {
            assertFalse("$it wurde faelschlich als Befehl gelesen", it in gefunden)
        }
    }

    @Test
    fun `entfernte und weg-umbenannte Befehle bleiben draussen`() {
        val gefunden = namen(ProtokollErnte.leseSlashBefehle(protokoll))
        assertFalse("/altbefehl wurde entfernt", "/altbefehl" in gefunden)
        assertFalse("/frueher heisst jetzt anders", "/frueher" in gefunden)
        assertTrue("/heute ist der neue Name", "/heute" in gefunden)
    }

    @Test
    fun `Einstellungen kommen nur mit Signalwort daneben`() {
        val gefunden = namen(ProtokollErnte.leseEinstellungen(protokoll))
        assertTrue("bashEditDiffEnabled fehlt", "bashEditDiffEnabled" in gefunden)
        assertTrue("gatewayInternalNetworks fehlt", "gatewayInternalNetworks" in gefunden)
        // `ultra` ist ein Knopf im Bericht, keine Einstellung — und steht nur bei „option".
        assertFalse("ultra ist keine Einstellung", "ultra" in gefunden)
        // `df` und `dt` sind vim-Tasten in einer Zeile, die zufaellig „setting" enthaelt.
        assertFalse("df ist keine Einstellung", "df" in gefunden)
        assertFalse("dt ist keine Einstellung", "dt" in gefunden)
    }

    @Test
    fun `Einstellungen der Editor-Erweiterung bleiben draussen`() {
        val gefunden = namen(ProtokollErnte.leseEinstellungen(protokoll))
        assertFalse(
            "VS-Code-Einstellungen gehoeren nicht in settings.json",
            "accessibility.voice.speechLanguage" in gefunden,
        )
    }

    @Test
    fun `Variablen nur mit eigener Vorsilbe`() {
        val gefunden = namen(ProtokollErnte.leseVariablen(protokoll))
        assertTrue(
            "CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS fehlt",
            "CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS" in gefunden,
        )
        // Steht nur in einer Fehlerbehebung, mit `=0` dahinter — trotzdem eine echte Variable.
        assertTrue(
            "CLAUDE_CODE_BG_TASKS_REPORT_RUNNING fehlt",
            "CLAUDE_CODE_BG_TASKS_REPORT_RUNNING" in gefunden,
        )
        assertFalse("GIT_DIR gehoert nicht zu Claude Code", "GIT_DIR" in gefunden)
        assertFalse("GIT_WORK_TREE gehoert nicht zu Claude Code", "GIT_WORK_TREE" in gefunden)
    }

    @Test
    fun `die Herkunft steht an jedem Eintrag`() {
        val alle = ProtokollErnte.leseSlashBefehle(protokoll) +
            ProtokollErnte.leseEinstellungen(protokoll) +
            ProtokollErnte.leseVariablen(protokoll)
        alle.forEach {
            assertEquals("Herkunft fehlt bei ${it.name}", ProtokollErnte.HERKUNFT, it.kategorie)
        }
    }

    @Test
    fun `die Einfuehrungszeile ist die Grundlage der Erklaerung`() {
        val eintrag = ProtokollErnte.leseVariablen(protokoll)
            .first { it.name == "CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS" }
        assertTrue(
            "Die Beschreibung sollte die Added-Zeile sein",
            eintrag.beschreibung.startsWith("Added"),
        )
    }
}
