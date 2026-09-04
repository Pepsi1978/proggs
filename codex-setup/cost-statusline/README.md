# Codex-Kostenanzeige

Version 0.1.1 – 05.09.2026, 00:12 Uhr. Basis: Codex CLI 0.153.2.

Die native CLI-Statuszeile zeigt im vorhandenen Feld `estimated-thread-cost`
die lokale Schätzung `Kosten ≈$12.34`. Kontext, Weekly und Modell bleiben erhalten.
Das ist ein API-Vergleichswert in US-Dollar, keine Abrechnung des ChatGPT-Abos.

`export-prices.mjs` übernimmt die Modellpreise, Priority-Tarife, Kontextstufen und
den lokalen Cache-Read-Aufschlag von 20 % direkt aus der OpenCode-Seitenleiste.
Die exportierten Tarife stehen in `prices.json`; die CLI lädt die installierte
Kopie aus `CODEX_HOME/cost-prices.json` (normalerweise `~/.codex`).
Preisaktualisierung: `node export-prices.mjs`, danach die neue JSON-Datei installieren.
Der Export ist ein datierter Preisschnappschuss, keine permanente Live-Preisabfrage.

Die Berechnung verarbeitet vollständig geschriebene `token_count`-Zeilen aus dem
Rollout inkrementell. `thread_settings_applied` liefert Modell, Anbieter und
Service-Tier; `turn_context` liefert das Modell des Aufrufs. Bei alten Protokollen
ohne Service-Tier gilt Standard. Reasoning wird aus dem gesamten Output abgezogen
und separat zum Reasoning-/Outputtarif gerechnet. Cache Read und Write werden
aus dem gesamten Input abgezogen und separat bepreist. Doppelte Tokenmeldungen
und reine Absenkungen bei Kompaktierung werden nicht als neue Aufrufe gezählt.

Fortgesetzte Sessions werden aus ihrer Historie rekonstruiert. Fehlende Preise,
unbekannte Anbieter und unvollständige Historie ergeben `≥$… (teilw.)`, niemals
eine scheinbar vollständige Nullrechnung. Fehlende Dateien zeigen `n/v`.
Gezählt wird der ausgewählte Thread; eigenständige Subagent-Threads und externe
Toolgebühren sind nicht in dieser Summe enthalten. Andere OpenAI-Service-Tiers
ohne belegten eigenen Tarif werden ebenfalls als unvollständig gekennzeichnet.

## Bauen und installieren unter Windows

Voraussetzungen: Rust, Node.js, npm und Microsoft C++ Build Tools mit Windows SDK.

```powershell
pwsh -File .\build-install.ps1
```

Der Installer lädt den festen Upstream-Tag `rust-v0.153.2` in den lokalen Cache,
wendet die kleinen TUI-Anpassungen an und baut `codex.exe`. Anschließend installiert
er nach `~/.codex/cost-cli/0.1.1/` und stellt den npm-Launcher auf diese Datei um.
Die Originaldatei `codex.js.before-cost-statusline` bleibt als Sicherung erhalten.
Die Hilfsprogramme der originalen CLI werden in das Installationsverzeichnis kopiert.

Für die Aktivierung ist ein vollständiger CLI-Neustart nötig; `/new` im laufenden
Prozess lädt keine neue EXE. Mit `codex resume` kann die bisherige Session fortgesetzt
werden. Ein npm-Codex-Update kann den Launcher wieder ersetzen; den Patch nur auf
der passenden Upstream-Version erneut installieren. Rückbau: den gesicherten
npm-Launcher wiederherstellen. Laufende CLI-Prozesse werden nicht beendet.

Quellen: [OpenAI-Tarife](https://developers.openai.com/api/docs/pricing),
[GPT-6 Astra](https://developers.openai.com/api/docs/models/gpt-6-astra),
[Upstream-Statuszeile](https://github.com/openai/codex/blob/rust-v0.153.2/codex-rs/tui/src/chatwidget/status_surfaces.rs).
