---
name: kompass-doku-luecke-zweite-quelle
description: "Die drei Kompass-Apps — fehlende Einträge kamen nie vom Abgleich, sondern von der Quelle (lückenhaft oder gar nicht angeschlossen)"
metadata: 
  node_type: memory
  type: project
  originSessionId: 11d1d21f-9027-4323-97d3-912aff235f4b
  modified: 2026-09-14T11:09:26.805Z
---

**Muster (gilt für alle drei Kompass-Apps):** Wenn ein Eintrag „trotz Aktualisierung fehlt",
erst prüfen, ob die Quelle ihn überhaupt enthält (`curl` + `grep`) und ob der Bereich
überhaupt an einer Quelle hängt — bevor der Abgleich-Code durchsucht wird. Zweimal war
`gelesen = mapOf(EIN_BEREICH to …)` die Ursache: **Hat eine App mehrere Bereiche, aber die
Map nur einen, ist der andere Bereich tot.**

**ClaudeKompass 0.6.10 (14.09.2026):** `/output-style` (seit 2.1.269 wieder da),
`bashEditDiffEnabled`, `CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS` fehlten — die
Anthropic-Doku hinkt ihrem eigenen CHANGELOG hinterher. Fix: `ProtokollErnte.kt` erntet das
CHANGELOG als **zweite** Quelle, Doku behält Vorrang. Strenge Regeln gegen Halluzinationen:
Befehl nur unmittelbar hinter `Added`, Einstellung nur mit angrenzendem Signalwort, Variable
nur mit Claude-Code-Vorsilbe. Ganze Historie, kein Zeitfenster. Fällt `env-vars.md` aus,
bleibt auch die Ernte draussen — sonst ersetzte sie jeden Doku-Text durch eine Protokollzeile.

**CodexKompass 0.6.9 (14.09.2026):** Config-Bereich hatte **1** Eintrag, Referenz hat 426 —
der Bereich war nie angeschlossen. Quelle: `developers.openai.com/codex/config-reference.md`,
Komponente `<ConfigTable>`. Zwei Fallen: gemischte Anführungszeichen (`'…'`, wenn der Typ
selbst ein `"` enthält — betrifft `tools.web_search`, `web_search`) und Beschreibungen, die
erst in der nächsten Zeile beginnen. Die Slash-Ernte aus Release-Notes wurde **bewusst nicht**
auf das strenge ClaudeKompass-Muster umgestellt: Codex-Notizen sind nicht formelhaft, die
`Added`-Regel verlöre `/export`, `/recap`, `/worktree`.

**OCodeKompass 0.6.9 (14.09.2026):** Config-Bereich hatte 27 Einträge (zwei davon
Überschriften, keine Schlüssel), `URL_CONFIG` war definiert und wurde nie aufgerufen. Quelle
gewechselt von `config.mdx` (nur Beispielblöcke — zeigt, was eine Einstellung kann, nicht
welche es gibt) auf `opencode.ai/config.json`, das JSON-Schema: 252 Schlüssel, zwei Ebenen
tief. Tiefengrenze schützt auch gegen Bausteine, die auf sich selbst verweisen.

**Gemeinsame Entscheidungen:** Keine „seit Fassung X"-Angabe für Config-Einträge, wenn die
Quelle ein Release-Fenster ist (`model` käme sonst „seit 0.142.2" heraus). Schlüssel ohne
Beschreibung bekommen einen ehrlichen Hinweis statt eines erfundenen Satzes. Bestandsnamen
mit Leerzeichen werden gelöscht statt als „entfernt" geführt — sie waren nie Schlüssel.
Untergrenzen prüfen immer nur die primäre Quelle, damit eine zweite Quelle keinen
Quellen-Ausfall maskiert.

**Offen (nicht gefixt):** Codex- und OCode-Release-Abruf lädt ~66 MB JSON pro Knopfdruck
(zwei Seiten × 100 Releases mit allen Asset-Listen). Verwandt: [[android-apps-immer-installieren]]
