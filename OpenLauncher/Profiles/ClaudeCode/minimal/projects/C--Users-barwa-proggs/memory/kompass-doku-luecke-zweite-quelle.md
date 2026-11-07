---
name: kompass-doku-luecke-zweite-quelle
description: "Drei Kompass-Apps: Eintrag fehlt trotz Aktualisierung – Ursache war immer die Quelle (lückenhaft oder nicht angeschlossen), nie der Abgleich"
metadata:
  node_type: memory
  type: project
  originSessionId: 11d1d21f-9027-4323-97d3-912aff235f4b
  modified: 2026-09-14T11:09:26.805Z
---

**Muster (alle drei Kompass-Apps):** Fehlt ein Eintrag „trotz Aktualisierung“, vor dem Abgleich-Code prüfen: enthält die Quelle ihn (`curl` + `grep`), hängt der Bereich überhaupt an einer Quelle? Zweimal Ursache: `gelesen = mapOf(EIN_BEREICH to …)` – **mehrere Bereiche, Map nur einer → anderer Bereich tot.**

**ClaudeKompass 0.6.10 (14.09.2026):** `/output-style` (seit 2.1.269 wieder da), `bashEditDiffEnabled`, `CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS` fehlten – Anthropic-Doku hinkt dem eigenen CHANGELOG hinterher. Fix: `ProtokollErnte.kt` erntet CHANGELOG als **zweite** Quelle, Doku hat Vorrang. Regeln gegen Halluzination:
- Befehl nur unmittelbar hinter `Added`
- Einstellung nur mit angrenzendem Signalwort
- Variable nur mit Claude-Code-Vorsilbe
- ganze Historie, kein Zeitfenster
- fällt `env-vars.md` aus, bleibt auch die Ernte draußen – sonst ersetzte sie jeden Doku-Text durch eine Protokollzeile.

**CodexKompass 0.6.9 (14.09.2026):** Config-Bereich **1** Eintrag, Referenz 426 – nie angeschlossen. Quelle: `developers.openai.com/codex/config-reference.md`, Komponente `<ConfigTable>`. Fallen: gemischte Anführungszeichen (`'…'`, wenn der Typ selbst `"` enthält – `tools.web_search`, `web_search`); Beschreibungen, die erst in der nächsten Zeile beginnen. Slash-Ernte aus Release-Notes **bewusst nicht** aufs strenge ClaudeKompass-Muster umgestellt: Codex-Notizen nicht formelhaft, `Added`-Regel verlöre `/export`, `/recap`, `/worktree`.

**OCodeKompass 0.6.9 (14.09.2026):** Config-Bereich 27 Einträge (zwei davon Überschriften, keine Schlüssel), `URL_CONFIG` definiert, nie aufgerufen. Quelle gewechselt von `config.mdx` (nur Beispielblöcke – zeigt, was eine Einstellung kann, nicht welche es gibt) auf `opencode.ai/config.json` (JSON-Schema): 252 Schlüssel, zwei Ebenen tief. Tiefengrenze schützt auch gegen selbstverweisende Bausteine.

**Gemeinsame Entscheidungen:**
- Keine „seit Fassung X“-Angabe für Config-Einträge, wenn die Quelle ein Release-Fenster ist (`model` käme sonst „seit 0.142.2“ heraus).
- Schlüssel ohne Beschreibung: ehrlicher Hinweis statt erfundenem Satz.
- Bestandsnamen mit Leerzeichen löschen statt als „entfernt“ führen – waren nie Schlüssel.
- Untergrenzen prüfen nur die primäre Quelle, damit eine zweite Quelle keinen Quellen-Ausfall maskiert.

**Offen (nicht gefixt):** Codex- und OCode-Release-Abruf lädt ~66 MB JSON pro Knopfdruck (zwei Seiten × 100 Releases mit allen Asset-Listen). Verwandt: [[android-apps-immer-installieren]]
