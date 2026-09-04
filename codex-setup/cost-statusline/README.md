# Codex-Kostenanzeige

Version 0.1.2 – 05.09.2026, 00:28 Uhr. Basis: Codex CLI 0.153.2.

Die native CLI-Statuszeile zeigt im vorhandenen Feld `estimated-thread-cost`
die lokale Schätzung `Kosten ≈ $12.34`. Kontext, Weekly und Modell bleiben erhalten.
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

Der Installer erkennt die offizielle Version aus dem installierten npm-Paket und
lädt den passenden Upstream-Tag `rust-v<VERSION>` in einen eigenen lokalen Cache,
wendet die kleinen TUI-Anpassungen an und baut `codex.exe`. Anschließend installiert
er nach `~/.codex/cost-cli/<CODEX-VERSION>-cost.0.1.2-<BUILD-ID>/` und stellt den npm-Launcher auf diese Datei um.
Die Originaldatei `codex.js.before-cost-statusline-<CODEX-VERSION>` bleibt als Sicherung erhalten.
Die Hilfsprogramme der originalen CLI werden in das Installationsverzeichnis kopiert.
Der Build wird in einem eigenen Verzeichnis installiert; laufende EXE-Dateien
werden nicht überschrieben. Identische Builds können wiederverwendet werden.

## Nach einem Codex-Update

Die Installation richtet im npm-Befehlsverzeichnis den Befehl ein:

```powershell
codex-kosten-update
```

Er baut die Kostenanzeige für die aktuell installierte offizielle Codex-Version
neu und aktiviert sie wieder. Um zuerst auch Codex selbst auf den aktuellen
npm-Release zu aktualisieren:

```powershell
codex-kosten-update -MitCodexUpdate
```

Alternativ direkt im Repository: `pwsh -File codex-setup/cost-statusline/update.ps1`.
Es gibt keinen Hintergrunddienst und keine automatische Änderung ohne Aufruf.
Fehlen passende Quellcode-Anker oder schlägt der Build fehl, bricht die Installation
ab. Eine unbekannte neue TUI-Struktur muss dann im Patch angepasst werden; zukünftige
Codex-Versionen können nicht pauschal als kompatibel garantiert werden. Nach Erfolg
Codex vollständig neu starten. Die vorhandene Statuszeilen-Konfiguration bleibt erhalten.

## Auf einem anderen Rechner wiederverwenden

Das Paket ist im Repository `Pepsi1978/proggs` unter `codex-setup/cost-statusline`
versioniert, nicht als eigenständiges npm-Paket veröffentlicht. Für einen anderen
Windows-PC kann dieser Auftrag verwendet werden:

> Installiere die Codex-Kostenanzeige aus `codex-setup/cost-statusline` im aktuellen
> Repository `Pepsi1978/proggs`. Lies die README, richte die Build-Voraussetzungen
> ein und führe `build-install.ps1` aus. Der Installer ermittelt die Codex-Version;
> bei einem inkompatiblen Quellstand muss der Patch zuerst angepasst werden.
> Ergänze `estimated-thread-cost` in `tui.status_line` der Codex-Konfiguration,
> erhalte alle bisherigen Statusanzeigen und verwende den tatsächlichen
> `CODEX_HOME` des Benutzers. Erkläre danach den vollständigen CLI-Neustart.

Andere CLI-Tools brauchen einen eigenen Adapter für ihre Verbrauchsdaten und ihre
Anzeige. `local_cost.rs` liest das Codex-Rolloutformat und ist deshalb kein
universelles Statuszeilen-Plugin. Der mitgelieferte Installer unterstützt Windows;
für macOS/Linux muss insbesondere der Build- und Installationsweg angepasst werden.

Für die Aktivierung ist ein vollständiger CLI-Neustart nötig; `/new` im laufenden
Prozess lädt keine neue EXE. Mit `codex resume` kann die bisherige Session fortgesetzt
werden. Ein npm-Codex-Update kann den Launcher wieder ersetzen; danach den Updater
aufrufen. Rückbau: den zur offiziellen Version passenden gesicherten
npm-Launcher wiederherstellen. Laufende CLI-Prozesse werden nicht beendet.

Quellen: [OpenAI-Tarife](https://developers.openai.com/api/docs/pricing),
[GPT-6 Astra](https://developers.openai.com/api/docs/models/gpt-6-astra),
[Upstream-Statuszeile](https://github.com/openai/codex/blob/rust-v0.153.2/codex-rs/tui/src/chatwidget/status_surfaces.rs).
