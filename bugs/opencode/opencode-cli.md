# OpenCode CLI — Bug-Almanach

> **PFLICHT vor jeder echten Arbeit an OpenCode lesen.** Bekannte Bugs, Fallen und ihre
> bewährten, **funktionserhaltenden** Lösungen — damit der Fehler gar nicht erst passiert.
>
> **Stand:** recherchiert am 2026-06-18 für **OpenCode CLI v1.17.8** (Repo `anomalyco/opencode`,
> früher `sst/opencode`; Doku `opencode.ai`). Recherche: 7 parallele Researcher (offizielle Doku +
> Changelog v1.1.64→v1.17.8, GitHub-Issues, Community, Plattform-Fallen, Config/AGENTS.md,
> Agents/Plugins/MCP/Skills, Token/Provider/OpenRouter). Quellen je Eintrag verlinkt.
> **Anker:** opencode=1.17.8 (Erst-Recherche); MCP-Lazy-Loading-Stand bestätigt für **1.17.11** (2026-06-26, #56 — weiterhin kein natives Lazy-Loading)
>
> Gegenstück (wie man es von vornherein richtig macht): `best-practices/opencode/` (8 Dateien).
> Digest-Modell (Stufe A/B/C) siehe `bugs/SYSTEM.md` §11.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Die häufigsten/teuersten Fallen. Bei einem Fehler im Bereich: **Volltext** dieser Datei lesen
> (Stufe B). Volltext-Spalte = Abschnitt unten.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Windows nativ: npm-Wrapper, kaputte Umlaute, Paste tot, Bun-Segfault | Offiziell **WSL nutzen** (`opencode.ai/docs/windows-wsl`). Nativ ist Fallback. | §1, §2, §12 |
| 1a | Windows Terminal: Maus-Kopieren und anklickbare Confirm-Dialoge kollidieren | Für Confirm-/Allow-Klicks OpenCode-`tui.json` auf `"mouse": true` lassen. `"mouse": false` verbessert native Auswahl/Rechtsklick, deaktiviert aber ALLE OpenCode-Mausfunktionen. | §2 (#10a) |
| 2 | ⭐ Kontext/Token läuft voll, viele MCP-Server aktiv | Jeder MCP lädt sein Tool-Schema in JEDEN Prompt (GitHub-MCP ~15–20k Tok; Extrem 147k). **KEIN natives Lazy-Loading** (Stand 1.17.11, anders als Claude Code) → manuelle Per-Agent-Auslagerung ist der EINZIGE Hebel: global `"tools":{"servername*":false}`, im Agent `true`. | §8 (#56) |
| 3 | ⭐ Agent ändert/committet ungefragt | Defaults sind permissiv (`edit`/`bash` = allow). `permission: {edit:"ask", bash:"ask"}`. Permission-Keys **nur lowercase** — PascalCase (`"Bash"`) wird STILL ignoriert! | §6 |
| 3a | ⭐ Modell VERWEIGERT Commit/Push mit erfundener „nur auf Anweisung"-Begründung | Text-Regel reicht nicht (trainiertes Vorsichtsverhalten) → zusätzlich Plugin `git-dirty-watchdog.js` (warnt hörbar bei `session.idle` + dirty Repo) | §6 (#48a) |
| 4 | ⭐ OpenCode startet nicht: `ConfigInvalidError unrecognized keys` | Top-Level-Config ist **strict** → nur dokumentierte Keys. Anweisungen via `instructions`/`AGENTS.md`, nicht erfundene Keys. `opencode debug config`. | §3 |
| 5 | „Model not found" erst beim ersten Request | ID-Format ist `provider/model`; OpenRouter **doppelt**: `openrouter/<author>/<model>`. `opencode models` listet gültige IDs. | §10 |
| 6 | Globale + Projekt-`AGENTS.md` — globale Regeln „fehlen" | Werden NICHT sauber gemergt; globale wird teils still ignoriert. In Projekt-`AGENTS.md` `@~/.config/opencode/AGENTS.md` referenzieren. | §4 |
| 7 | Unerwartet teuer / teures Modell für Nebenaufgaben | `small_model` explizit auf billiges Modell; bewusstes (günstiges) Default-`model`; teure Modelle gezielt zuweisen. | §10, §11 |
| 8 | Lange Session: `context_length_exceeded` / „prompt too long" | Auto-Compaction triggert zu spät / verliert Details. `compaction.prune:true`, `reserved` hoch, Fakten in `AGENTS.md`, große Outputs in Dateien. | §5 |
| 9 | `ProviderInitError` / `AI_APICallError` | Provider-Cache leeren `~/.cache/opencode` (Pakete neu laden); bei korrupter Config `~/.local/share/opencode` (Achtung: löscht Auth/Sessions), dann `/connect`. | §10 |
| 10 | Subagents: versteckte Kosten / falsches Modell | Subagent erbt Modell des Primary, nicht das globale → explizit `model:` setzen. Subagent-Token zählt der TUI-Counter NICHT. Step-Limit setzen. | §6, §11 |
| 11 | Agent/Command/Plugin/Skill „wird nicht erkannt" | Verzeichnisse sind **Plural**: `agents/ commands/ plugins/ skills/ tools/`. `agent create` schreibt fälschlich `agent/` (Singular). | §6, §7, §9 |
| 12 | `opencode upgrade` meldet „unknown" (Windows) | Detection scheitert an `npm.cmd`. Manuell per Paketmanager updaten (`npm i -g opencode-ai@latest` / `scoop update opencode`). | §1 |
| 13 | ⭐ OpenCode-Go: API-Fehler je Modell / GLM früh „aufgebraucht" / Modell erfindet Fakten | **Zwei Endpunkt-Schemata:** DeepSeek/GLM/Kimi/MiMo = OpenAI (`/zen/go/v1/chat/completions`), Qwen/MiniMax = Anthropic (`/zen/go/v1/messages`). GLM-5.x im Go-Tier nur ~4.300 Req/Mo (nicht für Masse). DeepSeek V4 Pro halluziniert bei Nichtwissen → Abstain-Prompt. | §14 |
| 14 | ⭐ OpenCode-Go-Modell einrichten / Thinking aktivieren | Go ist **eingebaut** → `/connect`, KEIN Custom-`@ai-sdk/anthropic`-Block (Key-Verlust #21737); nur MERGE-Block für Optionen. Thinking-Keys in `docs/models` (nicht config): Anthropic `options.thinking.budgetTokens`, OpenAI `reasoningEffort`. MiniMax denkt nativ. | §14.4–14.5 |
| 15 | ⭐ Direkter Python-Call an Go-Gateway/OpenRouter → Cloudflare 403/„1010" | urllib-Default-UA wird geblockt → `User-Agent: curl/8.5.0` setzen (so `mm/or-research _post()`). Erinnerung: `/messages`=`x-api-key`, Thinking `{type:enabled,budget_tokens:N}` (NICHT `adaptive` — das nur bei `/chat/completions`) | §14.6–14.8 |
| 16 | ⭐ Plugin deinstallieren / Plugin kommt nach Neustart wieder („loading plugins") | Es gibt **KEIN** `plugin remove`. Eintrag steht in MEHREREN Config-Dateien (auch `tui.json`, nicht nur `opencode.jsonc`) → **alle** entfernen; OpenCode **schließen** (sonst regeneriert es Cache+Config sofort); Cache `~/.cache/opencode/packages/<scope>` + plugin-eigene Config (z.B. `dcp.jsonc`) löschen; ggf. `~/.config/opencode/package.json` zurücksetzen (append-only). | §7 (#55d) |
| 17 | ⭐ TUI sieht komplett kaputt aus (linke Spalte voller `??`/`M`, `[plugin] …`-Zeilen bluten rein) | Ein Plugin schreibt direkt aufs Terminal — Plugins laufen IM TUI-Prozess. Ursache: `$`-Shell-Aufruf **ohne `.quiet()`** (Bun echoed stdout ans TTY, z.B. `git status --porcelain`) und/oder `console.*` (schreibt auf stderr). **FIX:** an JEDEN `$`-Aufruf `.quiet()`; NIE `console.*`, nur `client.app.log`. | §6 (#48a) |

---

## 1. Installation & Update (Windows / macOS / Linux / WSL)

### 1. ⭐ Windows nativ ist weniger zuverlässig — offiziell WSL empfohlen
**Symptom:** Langsame Performance, Datei-Zugriffsprobleme, Terminal-/Encoding-Fehler unter nativem Windows.
**Ursache:** OpenCode baut auf Dev-Tool-/Dateisystem-Verhalten, das unter WSL besser läuft; nativer Windows-Pfad ist explizit als „General performance issues"-Fallback dokumentiert.
**Versionen:** per Design / alle Versionen.
**FIX:** OpenCode in **WSL** installieren und betreiben; Repo im WSL-Dateisystem (`~/code/…`) halten (nicht `/mnt/c`, nicht `\\wsl$`). Desktop+WSL: `opencode serve --hostname 0.0.0.0 --port 4096`, App auf `http://localhost:4096`; klemmt localhost → WSL-IP via `hostname -I`. Siehe `best-practices/opencode/grundlagen-installation.md` §2.3/§2.4.
**Quelle:** https://opencode.ai/docs/troubleshooting (Windows) · https://opencode.ai/docs/windows-wsl

### 2. npm-Wrapper ruft `/bin/sh.exe` — `opencode` startet nicht (Windows PowerShell/Git Bash)
**Symptom:** Nach `npm install -g opencode-ai`: PowerShell `& : The term '/bin/sh.exe' is not recognized`; Git Bash `cannot execute binary file`.
**Ursache:** Der autogenerierte `opencode.ps1`-Wrapper nimmt eine Unix-Shell an und ruft `/bin/sh …/bin/opencode`. Das Windows-Binary liegt bereits gebündelt: `…/opencode-ai/node_modules/opencode-windows-x64/bin/opencode.exe`.
**Versionen:** Windows 10/11, npm-global (gemeldet 2025-09).
**FIX (funktionserhaltend):** Binary direkt aufrufen, oder im Wrapper auf `opencode.exe` umbiegen — **sauberste Lösung: in WSL per `curl -fsSL https://opencode.ai/install | bash` installieren.** Alternativ Scoop/Chocolatey statt npm.
**Quelle:** https://github.com/anomalyco/opencode/issues/2447

### 3. Falscher npm-Paketname `opencode` statt `opencode-ai` (Supply-Chain-Falle)
**Symptom:** `npm i opencode` → „command not found" oder ein fremdes Programm.
**Ursache:** Das korrekte Paket heißt **`opencode-ai`**; auf npm existiert ein separates, unverwandtes `opencode`-Paket (potenzielles Malware-/Typosquat-Risiko).
**Versionen:** alle, npm-Install.
**FIX:** Immer exakt `npm install -g opencode-ai`. Vor Install Paket-Identität prüfen (siehe CLAUDE.md „Sicherheit bei externem Code").
**Quelle:** https://www.npmjs.com/package/opencode-ai

### 4. `opencode upgrade` erkennt Installationsmethode nicht → „unknown" (Windows)
**Symptom:** `opencode upgrade` meldet „unknown"/„may be managed by a package manager → Install anyways? No" und bricht ab.
**Ursache:** `Installation.method()` spawnt `npm list -g` o.ä. **ohne `shell:true`**; unter Windows ist `npm` = `npm.cmd` (Batch), `spawn` kann `.cmd`/`.bat` nicht auflösen → leere Ausgabe (`nothrow` schluckt Fehler) → Fallback `"unknown"`. Betrifft npm/pnpm/bun.
**Versionen:** Windows, v1.2.25; **Fix via PR #18010** (`shell: cmd.exe`).
**FIX:** Auf Version mit PR #18010 updaten; bis dahin manuell `npm i -g opencode-ai@latest` bzw. `scoop update opencode`.
**Quelle:** https://github.com/anomalyco/opencode/issues/17295 · PR https://github.com/anomalyco/opencode/pull/18010

### 5. `BunInstallFailedError` beim Upgrade
**Symptom:** Upgrade bricht ab; die mitgelieferte Bun-Laufzeit lässt sich nicht installieren (oft Proxy/ARM/fehlende Rechte).
**Versionen:** unabhängig (#4047).
**FIX:** Statt `opencode upgrade` über nativen Installer neu installieren (`curl -fsSL https://opencode.ai/install | bash`) oder Homebrew/Scoop (umgehen die Bun-Abhängigkeit).
**Quelle:** https://github.com/anomalyco/opencode/issues/4047

### 6. NVM-for-Windows: `opencode` verschwindet nach Node-Versionswechsel
**Symptom:** Nach Node-Wechsel ist `opencode` „command not found".
**Ursache:** Globale npm-Installs sind bei nvm-windows **versions-gebunden** (eigenes globales node_modules je Node-Version).
**Versionen:** Windows + nvm-windows (#22600).
**FIX:** OpenCode pro Node-Version neu installieren — besser: versionsunabhängiges `curl | bash`-Standalone statt npm.
**Quelle:** https://github.com/anomalyco/opencode/issues/22600

### 7. `.opencode-ai-*`-Temp-Ordner blockiert künftige globale npm-Updates
**Symptom:** Spätere `npm -g`-Updates scheitern mit `EINVALIDPACKAGENAME`.
**Ursache:** `opencode-ai` legt einen Temp-Ordner (`.opencode-ai-xxxx`) im globalen `node_modules` an und räumt ihn nicht auf; npm versucht den dot-Ordner als Paket zu parsen.
**Versionen:** Windows, npm-global (#4592).
**FIX:** `.opencode-ai-*` im globalen `node_modules` manuell löschen.
**Quelle:** https://github.com/anomalyco/opencode/issues/4592

### 8. macOS/Linux: niemals `sudo brew` / `sudo npm -g`
**Symptom:** `EACCES`/Permission-Konflikte, beschädigte Ownership nach Install/Upgrade.
**Ursache:** `sudo` mit Homebrew/npm-global zerstört User-Ownership (Homebrew ist auf User-Rechte ausgelegt).
**Versionen:** macOS/Linux.
**FIX:** Nie `sudo brew`/`sudo npm -g`. npm-Prefix auf User-Verzeichnis umstellen oder `curl | bash`-Standalone; `brew upgrade` ohne sudo.
**Quelle:** https://docs.brew.sh/Common-Issues · https://opencode.ai/docs/troubleshooting

---

## 2. TUI / Terminal / Eingabe

### 9. Windows: Unicode/UTF-8-Rendering kaputt (Box-Zeichen/Umlaute als Müll)
**Symptom:** Rahmen/Icons/Akzente erscheinen als `â–`, `Ã©` etc. — über Windows Terminal, VS-Code-Terminal, PowerShell, cmd (auch bei Scoop-Install).
**Ursache:** Codepage-/UTF-8-Handling des Windows-Terminals.
**Versionen:** Windows 11, v1.2.10 (offen, Label bug+windows).
**FIX (funktionserhaltend):** `chcp 65001` vor Start; Nerd-/UTF-8-Font; Windows-Region „Beta: UTF-8 weltweit"; robusteste Lösung: **WSL**.
**Quelle:** https://github.com/anomalyco/opencode/issues/14661 · https://github.com/anomalyco/opencode/issues/8410

### 10. Windows-TUI: Strg+V fügt keinen Text ein
**Symptom:** In Windows Terminal/cmd/PowerShell passiert bei Strg+V nichts.
**Ursache:** OpenCode behandelt Strg+V nur für **Bild**-Paste und verlässt sich für Text auf „bracketed paste" — Windows-Terminals senden diese Sequenzen oft nicht korrekt.
**Versionen:** Windows 11, v1.1.53 (offen).
**FIX:** Rechtsklick-Einfügen oder Strg+Umschalt+V; oder in WSL arbeiten.
**Quelle:** https://github.com/anomalyco/opencode/issues/13800

### 10a. Windows Terminal: Maus-Kopieren und anklickbare Confirm-Dialoge kollidieren
**Symptom:** Mit `"mouse": true` kopiert die Linksauswahl unzuverlässig und Rechtsklick wird abgefangen. Mit `"mouse": false` funktionieren native Auswahl und Rechtsklick, aber Confirm-/Allow-Schaltflächen reagieren nicht mehr auf Linksklick.
**Ursache:** `mouse` ist in OpenCode 1.17.18 ein globaler TUI-Schalter. `true` übergibt Mausereignisse an OpenCode, `false` vollständig an das Terminal. Es gibt keinen getrennten Schalter für Textauswahl und anklickbare TUI-Elemente. OpenCodes eigener Copy-on-select-Pfad ist zudem als offener Fehler #17796 dokumentiert.
**Versionen:** OpenCode 1.17.18 und Windows Terminal 1.24.11321.0 bestätigt; OpenCode-Fehler #17796 offen.
**FIX:** Wenn Confirm-/Allow-Dialoge per Maus bedient werden, in `~/.config/opencode/tui.json` `"mouse": true` setzen. Die Windows-Terminal-Werte `"copyOnSelect": true`, `"experimental.rightClickContextMenu": false`, `"copyFormatting": "none"` und Profil-`"selectionBackground": "#808080"` können bestehen bleiben. Wer stattdessen ausschließlich native Mausauswahl und Rechtsklick braucht, kann bewusst `"mouse": false` wählen, verliert dann aber sämtliche OpenCode-Mausinteraktion. Bis OpenCode getrennte Mausmodi anbietet oder #17796 behebt, ist beides mit normalen Mausklicks nicht gleichzeitig verfügbar.
**Quelle:** https://opencode.ai/docs/tui/#options · https://github.com/anomalyco/opencode/issues/17796 · https://learn.microsoft.com/windows/terminal/customize-settings/interaction#automatically-copy-selection-to-clipboard · https://learn.microsoft.com/windows/terminal/customize-settings/interaction#context-menu · https://learn.microsoft.com/windows/terminal/customize-settings/profile-appearance#selection-background-color

### 11. Windows: Bild-Paste schlägt still fehl (Bun strippt `$` in PowerShell)
**Symptom:** Bild aus Zwischenablage lässt sich nicht einfügen, kein Fehler.
**Ursache:** Der Clipboard-Lesebefehl enthält `$img`/`$ms`; Buns `spawn` strippt unter Windows alle `$` → ungültiges PowerShell, das lautlos scheitert.
**Versionen:** Windows, Bun-Runtime (#17616).
**FIX (Workaround):** Bild als Datei speichern und per Pfad referenzieren statt per Clipboard.
**Quelle:** https://github.com/anomalyco/opencode/issues/17616

### 12. Linux: Copy/Paste tut nichts (fehlende Clipboard-Utility)
**Symptom:** Kopieren/Einfügen im TUI ohne Wirkung (Linux).
**Ursache:** OpenCode nutzt externe Clipboard-Tools, die fehlen.
**Versionen:** per Design (Linux-Abhängigkeit).
**FIX:** X11 `apt install -y xclip` (oder `xsel`); Wayland `apt install -y wl-clipboard` (bevorzugt); headless `xvfb` + `export DISPLAY=:99.0`.
**Quelle:** https://opencode.ai/docs/troubleshooting

### 13. TUI bricht in tmux (Eingabefeld als graue Box) — Regression v1.2.17
**Symptom:** In tmux rendert das Eingabefeld als graue Box; Tippen/Shortcuts tot.
**Ursache:** Regression durch Commit 3ebebe0 („prevent orphaned subprocesses").
**Versionen:** v1.2.17–v1.2.24 betroffen, **gefixt v1.2.25**.
**FIX:** Auf v1.2.25+ updaten; bis dahin außerhalb tmux starten.
**Quelle:** https://github.com/anomalyco/opencode/issues/16967

### 14. TUI nach langer Ausgabe / Resume verstümmelt (rein visuell)
**Symptom:** Zeichen überlappen, Layout bricht nach langer Ausgabe oder nach `Ctrl+Z`+`fg`. Die eigentliche Modell-Ausgabe bleibt korrekt.
**Ursache:** Fehlendes vollständiges Repaint (opentui-Renderer).
**Versionen:** unabhängig (#15388, #16327).
**FIX:** Terminal-Fenster kurz resizen → erzwingt Full-Redraw.
**Quelle:** https://github.com/anomalyco/opencode/issues/15388 · https://github.com/anomalyco/opencode/issues/16327

### 15. TUI-Freeze (Spinlock) — Prozess läuft nach Terminal-Schließen weiter
**Symptom:** TUI komplett unresponsive; nach Terminal-Schließen läuft `opencode` mit CPU-Last weiter.
**Ursache:** Abort-Signal aus `session.processor` propagiert nicht zum Render-Thread → futex-Spinlock (opentui).
**Versionen:** ab ~v1.1.53; #12834 **geschlossen (not planned)** (spätere opentui-Überarbeitung). WSL2-Variante (2–10 s Freeze) Regression v1.0.129 (#5361, offen).
**FIX:** `pkill -9 opencode` + Neustart; aktuelle Version nutzen; WSL2-Freeze → native Linux-Shell statt WSL2.
**Quelle:** https://github.com/anomalyco/opencode/issues/12834 · https://github.com/anomalyco/opencode/issues/5361

### 16. Keybinds inkonsistent / Leader-Key `ctrl+x` kollidiert
**Symptom:** Navigations-/Lösch-/Multiline-Bindings reagieren nicht; Default-Leader `ctrl+x` wird vom Terminal/Shell abgefangen (oft macOS).
**Ursache:** Uneinheitliches Keybinding-Handling + Leader-Konflikt.
**Versionen:** alle (#4997).
**FIX:** In `~/.config/opencode/tui.json` Leader auf `ctrl+\` o.ä. ändern, einzelne Keybinds explizit setzen/`"none"`; `leader_timeout` anpassen.
**Quelle:** https://opencode.ai/docs/keybinds · https://github.com/anomalyco/opencode/issues/4997

---

## 3. Konfiguration (opencode.json / opencode.jsonc)

### 17. ⭐ Unbekannte Top-Level-Keys → Hard-Crash (`ConfigInvalidError`)
**Symptom:** Start bricht ab: `ConfigInvalidError … Unrecognized keys: "rules", "context", "notes"`. Kein Fallback.
**Ursache:** Top-Level-Schema ist **strict** (Zod) — erfundene Keys werfen einen Fehler. Häufig durch Verwechslung mit AGENTS.md/Agent-Frontmatter.
**Versionen:** per Design (#7483).
**FIX:** Nur dokumentierte Keys; Anweisungen via `instructions:[…]` + separate `AGENTS.md`. `$schema:"https://opencode.ai/config.json"` setzen (Editor markiert Fehler vorab). `opencode debug config` prüft die aufgelöste Config.
**Quelle:** https://github.com/anomalyco/opencode/issues/7483 · https://opencode.ai/docs/config

### 18. ⭐ `permission`-Block ignoriert unbekannte/PascalCase-Keys STILL (Sicherheitsfalle)
**Symptom:** `"permission": {"Bash":"deny","Read":"ask"}` hat **null Wirkung** — keine Warnung. Agent läuft ungeschützt.
**Ursache:** Anders als das strikte Top-Level nutzt `permission` ein `.catchall`-Schema. Kanonische Keys sind **lowercase** (`bash read edit write glob grep list task webfetch`); alles andere wird still verschluckt. Klassischer Claude-Code-Migrationsfehler (PascalCase + `"Bash(*)"`-Syntax).
**Versionen:** per Design/Bug (#15507, offen).
**FIX:** Permission-Keys **nur lowercase**; NICHT die Claude-Code-Syntax `"Bash(*)"` übernehmen. Nach dem Setzen mit einem provozierten `deny`-Test verifizieren, dass die Regel greift.
**Quelle:** https://github.com/anomalyco/opencode/issues/15507

### 19. Neue Schema-Keys crashen ältere OpenCode-Versionen
**Symptom:** Eine eingecheckte `opencode.json` mit neuem Key (z.B. `"layout"`) lässt eine ältere lokale Installation gar nicht starten.
**Ursache:** Strict-Schema ist versionsgebunden (siehe #17). Tritt in Teams auf (Config neuer als CLI).
**Versionen:** per Design (#1081).
**FIX:** OpenCode aktuell halten (`autoupdate:true` default); in gemischten Teams nur Keys der niedrigsten Version nutzen; bei Crash neuen Key temporär entfernen.
**Quelle:** https://github.com/anomalyco/opencode/issues/1081

### 20. Configs werden GEMERGT (Arrays konkateniert), nicht ersetzt
**Symptom:** Projekt-Config ersetzt nicht die globale; `instructions`/`plugin`-Arrays lassen sich lokal nicht „leeren".
**Ursache:** per Design. Skalare → spätere Quelle gewinnt; Arrays (`instructions`,`plugin`) konkatenieren über alle Quellen. Präzedenz (niedrig→hoch): Remote → Global → `OPENCODE_CONFIG` → Projekt → `.opencode`-Dirs → `OPENCODE_CONFIG_CONTENT` → Managed (`/etc/opencode`, `%ProgramData%\opencode`) → macOS-MDM.
**Versionen:** per Design.
**FIX:** Erwarten, dass Listen sich addieren; global Unerwünschtes global entfernen. Managed/MDM ist bindend. `opencode debug config` prüft die effektive Auflösung.
**Quelle:** https://opencode.ai/docs/config

### 21. `{env:VAR}` fällt bei fehlender Variable still auf Leerstring
**Symptom:** `"apiKey":"{env:ANTHROPIC_API_KEY}"` wird bei nicht gesetzter Variable zu `""` — kein Fehler, Auth scheitert später kryptisch.
**Ursache:** Variablen-Substitution ersetzt fehlende Variablen per Design durch Leerstring.
**Versionen:** per Design.
**FIX:** Sicherstellen, dass die Env-Variable im Start-Kontext der CLI exportiert ist. Secrets aus Datei via `{file:~/.secrets/key}`. Bei „leerem" Verhalten zuerst die Substitution verdächtigen.
**Quelle:** https://opencode.ai/docs/config#variables

### 22. Kommentare nur in `.jsonc` — `.json` mit Kommentaren ist ungültig
**Symptom:** Kommentare/Trailing-Commas in `opencode.json` → Parse-Fehler.
**Ursache:** Kommentare sind nur im JSONC-Format zulässig.
**Versionen:** per Design.
**FIX:** Datei `opencode.jsonc` (bzw. `tui.jsonc`) nennen, wenn Kommentare gewünscht.
**Quelle:** https://opencode.ai/docs/config#format

### 23. Deprecated `theme`/`keybinds`/`tui`-Keys + Singular-Verzeichnisse
**Symptom:** Theme/Keybinds in `opencode.json` greifen unzuverlässig; Agents/Commands in `agent/` werden nicht erkannt.
**Ursache:** `theme`/`keybinds`/`tui` in `opencode.json` sind deprecated (gehören in `tui.json`), Auto-Migration „wenn möglich". Verzeichnisse kanonisch **Plural** (`agents/ commands/ modes/ plugins/ skills/ tools/ themes/`); Singular nur aus Altbestand geduldet.
**Versionen:** Migration/Deprecation.
**FIX:** TUI-Settings nach `tui.json` (`$schema:"https://opencode.ai/tui.json"`); Verzeichnisse im Plural anlegen.
**Quelle:** https://opencode.ai/docs/config#tui

### 24. Permission-Pattern ist literal — ohne `*` werden Argumente blockiert
**Symptom:** `"git status":"allow"` greift, aber `git status --porcelain` wird trotzdem abgefragt/blockiert.
**Ursache:** Pattern-Matching ist literal; ohne `*` matcht nur der exakte Befehl ohne Argumente.
**Versionen:** per Design.
**FIX:** Argument-Wildcard nutzen: `"git status *"`, `"grep *"`. Reihenfolge: Catch-all `"*"` zuerst, spezifische danach (**letzte passende Regel gewinnt**).
**Quelle:** https://opencode.ai/docs/permissions

---

## 4. AGENTS.md / Regeln / Memory / Sessions

### 25. ⭐ Globale + Projekt-`AGENTS.md` werden missverstanden — globale wird teils still ignoriert
**Symptom:** Nutzer glauben, Projekt-`AGENTS.md` ersetze die globale; tatsächlich sollen beide gelten — auf Windows wird die globale aber still ignoriert, wenn eine Projekt-Datei existiert.
**Ursache:** Doku „first matching file wins **in each category**" ist mehrdeutig: „first wins" gilt nur für Dateinamen-Varianten je Kategorie (lokal `AGENTS.md` > `CLAUDE.md`; global `~/.config/opencode/AGENTS.md` > `~/.claude/CLAUDE.md`); die Kategorien werden dann konkateniert. Daneben ein echter `break`-Bug (#22020), der die globale schluckt.
**Versionen:** Doku-Verhalten per Design (#9282); Bug #22020 **Fix via PR #22317** (in v1.17.8 wahrscheinlich behoben — verifizieren).
**FIX (funktionserhaltend):** Wissen, dass beide Ebenen gelten. Workaround: in Projekt-`AGENTS.md` `@~/.config/opencode/AGENTS.md` referenzieren. Hat man `AGENTS.md` UND `CLAUDE.md` im selben Ordner, wird nur `AGENTS.md` gelesen. Verifizieren, indem man den Agenten seine geladenen Regeln rezitieren lässt.
**Quelle:** https://github.com/anomalyco/opencode/issues/9282 · https://github.com/anomalyco/opencode/issues/22020 · https://opencode.ai/docs/rules#precedence

### 26. `OPENCODE_CONFIG_DIR/AGENTS.md` wird ignoriert, wenn globale existiert (Profile brechen)
**Symptom:** Profil-spezifische `AGENTS.md` (per `OPENCODE_CONFIG_DIR`) wird nie geladen, sobald `~/.config/opencode/AGENTS.md` existiert.
**Ursache:** `systemPaths()` bricht beim ersten existierenden File mit `break` ab; `globalFiles()` liefert das globale Config-Dir zuerst.
**Versionen:** v1.1.48; **Fix via PR #11536**.
**FIX:** Auf Version mit PR #11536 updaten; bis dahin globale `AGENTS.md` temporär leeren/umbenennen oder Inhalt per `@`-Referenz in die Profil-Datei ziehen.
**Quelle:** https://github.com/anomalyco/opencode/issues/11534

### 27. Nach `/new` wird `AGENTS.md` nicht automatisch neu geladen
**Symptom:** Nach `/new` befolgt der Agent die `AGENTS.md` nicht mehr.
**Ursache:** Vom Maintainer als „not planned" geschlossen → faktisch erwartetes Verhalten.
**Versionen:** Verhaltensfalle (#11532).
**FIX:** Nach `/new` den Agenten explizit `AGENTS.md` lesen lassen, oder OpenCode neu starten. Kritische Regeln stabiler über `instructions:[…]` in `opencode.json` verankern.
**Quelle:** https://github.com/anomalyco/opencode/issues/11532

### 28. `instructions`-Glob lädt im Monorepo ALLES → Kontext-Aufblähung
**Symptom:** `"instructions":["packages/*/AGENTS.md"]` lädt beim Start ALLE Treffer (auch irrelevante) → aufgeblähter Kontext, Modell „ignoriert die Hälfte".
**Ursache:** Keine kontextabhängige Auto-Discovery; alle Globs + AGENTS.md werden **eager** beim Sessionstart konkateniert. „Mehr Regeln" verbessert nichts.
**Versionen:** per Design (Feature-Wünsche #6316, #10096).
**FIX:** `AGENTS.md` schlank halten (aufteilen ab ~150–200 Zeilen). Globs eng fassen. Lazy-Loading: in `AGENTS.md` per `@docs/xyz.md` referenzieren und das Modell ausdrücklich anweisen, sie bei Bedarf via Read-Tool nachzuladen (OpenCode parst `@`-Referenzen nicht automatisch).
**Quelle:** https://opencode.ai/docs/rules#referencing-external-files · https://github.com/anomalyco/opencode/issues/6316

### 29. CLAUDE.md-Migration greift nur als Fallback (und ist abschaltbar)
**Symptom:** Migrierende Claude-Code-Nutzer wundern sich, dass ihre `CLAUDE.md` ignoriert wird.
**Ursache:** Claude-Kompatibilität ist nur Fallback: `CLAUDE.md` nur ohne `AGENTS.md`; `~/.claude/CLAUDE.md` nur ohne `~/.config/opencode/AGENTS.md`. Zudem deaktivieren `OPENCODE_DISABLE_CLAUDE_CODE*`-Variablen die Kompatibilität.
**Versionen:** per Design.
**FIX:** Auf `AGENTS.md` migrieren (offizieller Name); keine `AGENTS.md` neben einer genutzten `CLAUDE.md` anlegen; `OPENCODE_DISABLE_CLAUDE_CODE*` prüfen.
**Quelle:** https://opencode.ai/docs/rules#claude-code-compatibility

### 30. Missverständnis: OpenCode hat KEIN eingebautes Langzeit-Gedächtnis
**Symptom:** Erwartung, der Agent „lerne" projektübergreifend; nach Sessionende/`/new` ist alles weg.
**Ursache:** Sessions sind bzgl. Langzeitwissen stateless (nur Session-Persistenz, kein Cross-Session-Memory).
**Versionen:** per Design (Feature-Wunsch #16077).
**FIX:** Stabiles Projektwissen in `AGENTS.md`/`instructions`. Echtes Memory nur via Plugin (`opencode-memory-md`, `opencode-mem`, Hindsight u.a.) — externe Plugins vorher auf Prompt-Injection/Exfiltration prüfen.
**Quelle:** https://github.com/anomalyco/opencode/issues/16077 · https://hindsight.vectorize.io/blog/2026/04/20/opencode-persistent-memory

### 31. Session-Speicherort & „Session not found" / leere `/sessions` / fehlende History
**Symptom:** „Session not found: ses_xxx"; `/sessions` leer trotz vorhandener Daten; History/Sidebar fehlt nach Projektwechsel (Desktop/Windows).
**Ursache:** Storage in `~/.local/share/opencode` (Auth/Logs/Projektdaten). Defekte: (a) Schließen kann Session-Files korrumpieren; (b) v0.14.4–0.14.6 API-Änderung → `/sessions` leer (#3026, gefixt); (c) Desktop/Windows: Sessions stehen in `opencode.db`, aber `opencode.global.dat` wird nicht aktualisiert (#26207).
**Versionen:** #3026 (gefixt), #12002, #26207 (offen).
**FIX:** Logs unter `~/.local/share/opencode/log/` prüfen; bei „leer trotz Daten" updaten; Storage vor Eingriffen sichern.
**Quelle:** https://github.com/anomalyco/opencode/issues/12002 · https://github.com/anomalyco/opencode/issues/26207

### 32. SQLite-Korruption bei parallelem Zugriff (lokal + Docker)
**Symptom:** Geteiltes `~/.local/share/opencode` zwischen lokaler Instanz und Docker → DB korrumpiert, sobald zwei Sessions gleichzeitig laufen.
**Ursache:** Kein Multi-Prozess-Locking über geteilten Storage.
**Versionen:** offen (#14194).
**FIX:** Storage NICHT zwischen parallelen Instanzen teilen; für Docker eigenes Daten-Verzeichnis (`XDG_DATA_HOME`) mounten; korrupte DB aus Backup wiederherstellen.
**Quelle:** https://github.com/anomalyco/opencode/issues/14194

### 33. Session-Storage wächst unbegrenzt (kein Prune)
**Symptom:** `~/.local/share/opencode` wächst unkontrolliert; kein `session prune`-Befehl.
**Versionen:** offen (#22110).
**FIX:** Alte Sessions manuell entfernen (vorher sichern, Instanz beenden → vermeidet #32-Korruption).
**Quelle:** https://github.com/anomalyco/opencode/issues/22110

---

## 5. Kontext / Kompaktierung

### 34. ⭐ Auto-Kompaktierung triggert nicht zuverlässig → `context_length_exceeded`
**Symptom:** Trotz `compaction.auto:true` (default) `context_length_exceeded`/„prompt is too long" — besonders in Agent-/Subagent-Workflows und mit Anthropic-Modellen.
**Ursache:** Trigger greift in verschachtelten Kontexten nicht früh genug; akkumulierter Subagent-Kontext überschreitet das Fenster vor der Kompaktierung.
**Versionen:** v1.1.x, offen (#8089, #6068).
**FIX (funktionserhaltend):** `compaction.prune:true` (entfernt alte Tool-Outputs; default `false`); `compaction.reserved` großzügig (z.B. 10000); Subagenten eng scopen; große Tool-Ergebnisse in Dateien auslagern; lange Läufe in kleinere Sessions schneiden.
**Quelle:** https://github.com/anomalyco/opencode/issues/8089 · https://github.com/anomalyco/opencode/issues/6068

### 35. Kompaktierung verliert kritischen Kontext (Constraints, Pastes, Flag-Namen)
**Symptom:** Nach Compaction wiederholt der Agent Beantwortetes, vergisst Constraints/Pastes, generalisiert exakte Strings (Flags, Pfade, Fehlermeldungen) zu vagen Beschreibungen.
**Ursache:** Das LLM-Summary-Prompt bewahrt feinkörnige Details nicht zuverlässig.
**Versionen:** offen (Feature #16512).
**FIX:** Wichtige Fakten in `AGENTS.md`/`instructions` verankern (überleben Compaction); lange Pastes als Datei referenzieren statt einkleben; `compaction.reserved` erhöhen; nach Auto-Compact zentrale Constraints kurz erneut nennen.
**Quelle:** https://github.com/anomalyco/opencode/issues/16512

### 36. `opencode run` (headless) beendet sich nach Kompaktierung
**Symptom:** Im Headless-Modus beendet sich der Prozess nach Auto-Compaction, wenn das Compaction-Modell selbst den Overflow-Threshold überschreitet.
**Versionen:** offen (#13946).
**FIX:** Kleineres separates Compaction-/`small_model` setzen; Kontext vor langen Runs reduzieren.
**Quelle:** https://github.com/anomalyco/opencode/issues/13946

### 37. Session „too large to compact" / 413 durch eingebettete Bilder
**Symptom:** `/compact` scheitert mit „Session too large"/„Request Entity Too Large" (413) — Session festgefahren.
**Ursache:** Per Read-Tool gelesene Bilder hängen als base64 im Kontext und sprengen das Provider-Limit; selbst Compaction baut das nicht mehr ab.
**Versionen:** #14562, #17340.
**FIX:** Bilder/Medien nicht unnötig per Read laden; bei Festfahren neue Session, Stand aus committetem Code/`AGENTS.md` rekonstruieren.
**Quelle:** https://github.com/anomalyco/opencode/issues/14562

### 38. `@`-Mentions & Shell-Output blähen Kontext auf (kein Auto-Pruning)
**Symptom:** Große `@`-Datei-Mentions und umfangreiche Tool-Outputs füllen das Fenster schnell.
**Ursache:** per Design landen `@`-Dateien und Shell-Output im Verlauf; Pruning ist default aus (`compaction.prune:false`).
**Versionen:** per Design.
**FIX:** `compaction.prune:true`; statt ganzer Dateien `@`-mentionen lieber Read mit Zeilenbereichen; laute Befehle in Dateien umleiten; `watcher.ignore` für laute Verzeichnisse pflegen.
**Quelle:** https://opencode.ai/docs/config#compaction

---

## 6. Agents / Modes / Subagents / Permissions

### 39. ⭐ Permissive Defaults — Agent editiert/führt ohne Rückfrage aus
**Symptom:** Agent ändert Dateien/führt bash ohne Nachfrage; in sensiblen Repos riskant.
**Ursache:** Die meisten Permissions stehen default auf `"allow"`; nur `doom_loop` und `external_directory` sind `"ask"`, `.env`-Reads `deny`.
**Versionen:** per Design.
**FIX:** `"permission":{"edit":"ask","bash":"ask"}` (oder granular per Glob). Für Read-only den `plan`-Agent (Tab); echte Garantie nur mit `permission:{edit:"deny",bash:"deny"}`.
**Quelle:** https://opencode.ai/docs/permissions

### 40. `agent create` schreibt in `agent/` statt `agents/` (Singular/Plural)
**Symptom:** Per CLI erstellter Agent erscheint nicht, wird still ignoriert.
**Ursache:** CLI legt `.opencode/agent/<name>.md` (Singular) an; Loader liest `.opencode/agents/` (Plural).
**Versionen:** „latest" (Feb 2026); **Fix via PR #14427**.
**FIX:** Datei nach `agents/` (bzw. global `~/.config/opencode/agents/`) verschieben. Aktuelle Doku nutzt durchgängig **Plural**.
**Quelle:** https://github.com/anomalyco/opencode/issues/14410

### 41. Subagent erhält nur die `description`, nicht den Markdown-Body
**Symptom:** Subagent ignoriert die Body-Anweisungen, startet mit generischem Prompt; verschiebt man sie in `description`, klappt es.
**Ursache:** Beim Subagent-Aufruf wurde der Body nicht als System-Prompt durchgereicht.
**Versionen:** v1.1.21, Issue geschlossen (gefixt; Tag unklar).
**FIX:** Bis Fix Kernanweisungen ins `description`; sonst auf gefixte Version updaten.
**Quelle:** https://github.com/anomalyco/opencode/issues/8733

### 42. Custom-Agent bekommt das `task`-Tool nicht (keine Subagents)
**Symptom:** Custom-Agent kann keine Subagents spawnen; `task` fehlt in der Tool-Liste.
**Ursache:** `task` war für Custom-Agents nicht freigeschaltet; in v1.2.6 startete OpenCode bei `permission:{task:allow}` sogar gar nicht.
**Versionen:** v1.2.6, Issue geschlossen (gefixt).
**FIX:** Auf Version mit `permission.task` updaten (heute der dokumentierte Weg, Glob, letzte Regel gewinnt). Nicht die alte `tools:`-Syntax für `task` erwarten.
**Quelle:** https://github.com/anomalyco/opencode/issues/14308

### 43. `tools:` ist deprecated → `permission:` (Migrations-Falle)
**Symptom:** Tool-Beschränkungen pro Agent „greifen nicht"; Tutorial-Configs funktionieren nicht mehr.
**Ursache:** Altes `tools:`-Feld deprecated; feingranulare Steuerung über `permission:` (Keys werden als Wildcard gegen Tool-Namen gematcht, auch MCP-/Custom-Tools). `tools:{x:true}` ≙ `{"*":"allow"}`.
**Versionen:** per Design.
**FIX:** Auf `permission:` umstellen; `"*"` zuerst, spezifisch danach (letzte passende Regel gewinnt).
**Quelle:** https://opencode.ai/docs/agents

### 44. Plan-Modus ist nicht hart read-only, sondern `ask`
**Symptom:** Erwartung, Plan könne nichts ändern; tatsächlich fragt er nur (`ask`) und schreibt bei Bestätigung.
**Ursache:** Built-in-Plan setzt `edit:ask`, `bash:ask` (nicht `deny`).
**Versionen:** per Design.
**FIX:** Für echte Read-only-Garantie `permission:{edit:"deny",bash:"deny"}` setzen.
**Quelle:** https://opencode.ai/docs/agents

### 45. Pro-Agent-Modellwahl: Subagent erbt das Modell des Primary
**Symptom:** Subagent läuft auf dem Modell des Aufrufers statt dem globalen Default → unerwartete (teure) Modellnutzung.
**Ursache:** per Design — Subagents ohne eigenes `model` erben das des aufrufenden Primary-Agents.
**Versionen:** per Design.
**FIX:** Beim Subagent explizit `model:"provider/model-id"` setzen.
**Quelle:** https://opencode.ai/docs/agents

### 46. Subagent-Modelle bei GitHub-Copilot-Provider ignoriert → alles auf Opus
**Symptom:** Trotz billiger Subagent-Modelle landet alles auf dem Orchestrator-Modell (Opus) in der Copilot-Abrechnung.
**Ursache:** Copilot-Provider respektiert pro-Subagent-Modelle nicht.
**Versionen:** 2026 (#20859).
**FIX:** Subagents vermeiden oder günstige Tasks über separaten Provider/Key; Copilot-Abrechnung beobachten.
**Quelle:** https://github.com/anomalyco/opencode/issues/20859

### 47. `maxSteps` deprecated → `steps`; fehlendes Step-Limit = „doom loop"
**Symptom:** `maxSteps` wirkt nicht; Agent versucht endlos einen Test zu fixen und führt neue Bugs ein.
**Ursache:** Feld umbenannt (`steps`); ohne Tiefenbegrenzung iteriert der autonome Agent im Kreis.
**Versionen:** per Design.
**FIX:** `steps:<n>` setzen (hartes Limit). Bei Loop: `Ctrl+C`, `git stash`/`git reset --hard`, präziser neu prompten.
**Quelle:** https://opencode.ai/docs/agents · https://sanj.dev/post/comparing-ai-cli-coding-assistants/

### 48. Agent committet/pusht ohne Freigabe trotz Rules
**Symptom:** Agent führt `git commit`/`push` ohne Freigabe aus.
**Ursache:** Bash-Permission-Pattern matcht den git-Befehl nicht zuverlässig (vgl. #24 literal Matching).
**Versionen:** offen (#14923).
**FIX:** `git` + Subkommandos explizit per Pattern (`"git push *":"ask"`/`"deny"`); Matching testen.
**Quelle:** https://github.com/anomalyco/opencode/issues/14923

### 48a. ⭐ Umgekehrter Fall: Modell VERWEIGERT Commit/Push mit erfundener Begründung
**Symptom:** Agent schließt eine Aufgabe erfolgreich ab (Build+Install ok), committet/pusht aber
NICHT und begründet das mit „Commit/Push habe ich nicht gemacht, weil das hier nur auf
ausdrückliche Anweisung erlaubt ist" — obwohl KEINE Regel/Config das je vorgeschrieben hat.
Franks `AGENTS.md` + `rules-opencode/commit-push-jede-aufgabe-vor-build.md` verlangen im
Gegenteil IMMER Commit+Push nach jeder Aufgabe, ohne Rückfrage.
**Ursache (recherchiert 2026-07-01, 5-Researcher-Schwarm, quellentreu bestätigt):** OpenCode
liefert einen **hardcodierten System-Prompt** aus, der wörtlich sinngemäß sagt: „NEVER commit
changes unless the user explicitly asks you to" / „DO NOT push to the remote repository unless
the user explicitly asks you to do so." Das erklärt das Symptom vollständig — das Modell zitiert
korrekt OpenCodes EIGENEN eingebauten Prompt, der mit `AGENTS.md`-Regeln kollidiert und sie
überstimmen kann, solange die Regel den eingebauten Vorbehalt nicht aktiv als überschrieben
markiert. Verwandtes Muster (Anbieter-Ebene, nicht OpenCode-spezifisch): Anthropics „Claude Code
Auto Mode"-Engineering-Blog beschreibt einen Permission-Classifier, der Aktionen nach
Reversibilität/Blast-Radius bewertet und Kategorien wie „Bypass review or affect others: pushing
directly to main" explizit als erhöhtes Risiko einstuft (anthropic.com/engineering/claude-code-auto-mode)
— die Vorsicht bei `git push` ist bei mehreren Anbietern bewusst eingebaut, nicht nur Zufall.
Kein 1:1-Duplikat-Issue bei OpenCode gefunden, aber ein enges Cluster: #14923 „Agent commits and
pushes without user approval despite explicit rules" (OPEN, Gegenrichtung — zeigt denselben
Prompt-vs-Regel-Konflikt), #3099 „Agent no follow rules after compact session" (CLOSED/COMPLETED —
kein aktives Risiko mehr), #11534/#11732 „AGENTS.md wird ignoriert" (beide CLOSED/COMPLETED).
Reine Text-Regeln (AGENTS.md) setzen sich gegen einen hardcodierten System-Prompt nicht
zuverlässig durch (vgl. `claude-config.md` §1.1: „Rules in prompts are requests, hooks in code
are laws"). Ein früherer Fix-Versuch (nur zusätzlicher AGENTS.md-Text, Commit #47319) hat das
Problem NICHT gelöst, weil der Text zwar geschrieben, aber nie von der Repo-Datei in die LIVE
`~/.config/opencode/AGENTS.md` übertragen wurde (Deploy-Lücke, separat als Bug erkannt+behoben).
**Versionen:** beobachtet mit GPT-5.5 (Medium+High-Thinking) via OpenCode, 2026-07-01. Nicht
modellspezifisch ausgeschlossen — kann bei jedem vorsichtig trainierten Modell auftreten.
**FIX (Poka-Yoke Stufe 2, Code statt nur Text):** Lokales Plugin `git-dirty-watchdog.js`
(`~/.config/opencode/plugins/`) prüft bei `session.idle` per `git status --porcelain`, ob das
Repo „dirty" ist, und macht das per Ton (`error.wav`) + Log-Eintrag SOFORT unübersehbar — committet
aber bewusst NICHT automatisch (ein Plugin kann nicht sicher unterscheiden, welche Datei zu welcher
parallelen Session gehört, siehe `parallel-sessions-git.md`). `AGENTS.md` bekam zusätzlich einen
Satz, der den eingebauten Vorbehalt EXPLIZIT als überschrieben markiert ("diese Regel HIER ist die
explizite Anweisung, für jede Aufgabe") — reine Erwähnung der Pflicht reicht gegen einen
hardcodierten Prompt nicht, sie muss ihn aktiv für ungültig erklären. Zusätzlich
`commit-push-jede-aufgabe-vor-build.md` um einen Satz ergänzt, der genau diese Ausrede
vorwegnimmt: „Es gibt KEINE Ausnahme für 'ausdrückliche Anweisung'".
**Recherchierter Goldstandard (nicht 1:1 übernommen, siehe unten):** Aider committet automatisch
als Tool-NEBENWIRKUNG nach jedem Edit — das LLM wird dabei nicht gefragt, der Commit kommt aus
einem deterministischen Event-Handler (aider.chat/docs/git.html). Cursor löst es über externe
Lifecycle-Hooks (GitButler-Integration: jeder Chat → eigener Branch, Commit bei Task-Ende).
Gemeinsamer Nenner aller zuverlässigen Lösungen: der Commit kommt aus Code, nie aus LLM-Disziplin.
**Bewusst NICHT 1:1 übernommen:** Alle recherchierten Auto-Commit-Vorbilder nutzen `git add -A` —
passt nicht zu Franks Multi-Session-Setup (würde fremde Dateien anderer Sessions mitreißen, siehe
`parallel-sessions-git.md`). Ausbaustufe „datei-basiertes Auto-Commit" (Plugin committet nur
Dateien, die es selbst per `tool.execute.after` in DIESER Session editiert gesehen hat, analog zum
Tracking-Muster in `tool-first-guard.js`) ist recherchiert, aber (Stand 2026-07-01) nicht
umgesetzt — offene Ausbaustufe, siehe `best-practices/opencode/agent-verhalten-commit-disziplin.md`.
**⚠️ FOLGE-BUG bei der Live-Verifikation (2026-07-01, sofort behoben) — TUI-Corruption durch
Plugin-Terminal-Ausgabe:** Beim ersten echten `session.idle`-Trigger zerstoerte das Plugin die
gesamte OpenCode-TUI (linke Spalte + untracked-Zeilen voller `??`/`M`, dazu die sichtbare
`[git-dirty-watchdog] …`-Zeile). **Root Cause (zwei Terminal-Schreibquellen im TUI-Prozess):**
(1) `await $\`… git status --porcelain\`` **ohne `.quiet()`** — Bun's Shell `$` ECHOED per Default
stdout ans TTY; die git-porcelain-Zeilen bluteten direkt in die TUI. (2) `console.error`/`console.warn`
schreiben auf stderr — dasselbe TTY, auf dem OpenCodes TUI im Alternate-Screen rendert.
**FIX (funktionserhaltend, Poka-Yoke Stufe 3):** an JEDEN Plugin-`$`-Aufruf `.quiet()` anhaengen
(captured stdout weiterhin in `result.stdout`, unterdrueckt nur das TTY-Echo) UND **jedes `console.*`
entfernen** — Plugins loggen ausschliesslich via `await client.app.log({body:{service,level,message}})`
(schreibt in die Log-Datei, nicht ans TTY). Watchdog-Ton + Log-Warnung bleiben voll erhalten.
Gilt fuer JEDES OpenCode-Plugin: **ein Plugin laeuft IM TUI-Prozess und darf NIE direkt aufs
Terminal schreiben** (siehe `best-practices/opencode/plugins-mcp-skills.md`). Auch `tool-first-guard.js`
mitgehaertet (console.* entfernt). Betroffen: `~/.config/opencode/plugins/*.js` + Repo-Spiegel
`opencode-setup/plugins/*.js`.
**Status:** git-dirty-watchdog-Grundfunktion deployed 2026-07-01; TUI-Corruption-Folgebug am selben
Tag behoben (`.quiet()` + console.* entfernt). Erneute Live-Verifikation (OpenCode-Neustart, TUI
sauber + Ton bei dirty-idle) durch Frank ausstehend.
**Quelle:** eigener Vorfall (Frank, CortexAndroid-TTS-Aufgabe, 2026-07-01); verwandt: #14923 (OPEN,
§48), #3099/#11534/#11732 (alle CLOSED/COMPLETED — kein aktives Risiko mehr). Recherche 2026-07-01
(5-Researcher-Schwarm, Sonnet-5): anthropic.com/engineering/claude-code-auto-mode ·
aider.chat/docs/git.html · blog.gitbutler.com/cursor-hooks-integration · arxiv.org/pdf/2605.07769 ·
arxiv.org/html/2407.18418v1.

---

## 7. Plugins (JS/TS) & Custom Tools

### 49. ⭐ `tool.execute.before`-Hook feuert nicht für Subagent-Tool-Calls (Policy-Bypass)
**Symptom:** Plugin blockiert beim Primary korrekt, aber Subagent-Tool-Calls (via `task`) laufen am Hook vorbei.
**Ursache:** Hooks waren nur in der Primary-Tool-Pipeline verdrahtet. **Sicherheitskritisch:** Restriktion per Subagent-Delegation umgehbar.
**Versionen:** v1.0.182, geschlossen (gefixt).
**FIX (Defense-in-Depth):** Restriktionen ZUSÄTZLICH per `permission:` pro Agent verankern (gilt auch für Subagents), nicht nur im Plugin; auf gefixte Version updaten.
**Quelle:** https://github.com/anomalyco/opencode/issues/5894

### 50. MCP-Tool-Calls lösen `tool.execute.before/after` nicht aus
**Symptom:** Plugins können MCP-Tool-Aufrufe nicht abfangen/loggen — Hooks feuern nur für Built-ins.
**Versionen:** **Fix via PR #2320** (gemergt; Tag unklar).
**FIX:** Updaten; übergangsweise `event`-Hooks nutzen.
**Quelle:** https://github.com/anomalyco/opencode/issues/2319

### 51. Hook feuert gar nicht — falscher Name / falsche Rückgabestruktur
**Symptom:** Plugin lädt (Init-Log da), aber Hook wird nie aufgerufen.
**Ursache:** Hook-Namen sind **case-sensitive** und exakt (`"tool.execute.before"`); Hook muss aus dem **zurückgegebenen Objekt** der Plugin-Funktion kommen. Event-Hooks (`event:`) und Tool-Hooks verwechselt.
**Versionen:** Konfig-Falle, versionsübergreifend.
**FIX:** Hook-Namen 1:1 aus der Doku-Eventliste; Struktur prüfen; Lifecycle über `event`, Tool-Interception über `tool.execute.*`.
**Quelle:** https://opencode.ai/docs/plugins

### 52. Plugin-Ladepfad: lokale Datei vs. npm-Liste (+ fehlende package.json)
**Symptom:** Lokales Plugin wird nicht geladen.
**Ursache:** Zwei Mechanismen: lokale `.ts/.js` müssen in `.opencode/plugins/` bzw. `~/.config/opencode/plugins/` (**Plural**); npm-Plugins gehören als Paketname ins `"plugin":[…]`-Array. Lokale Plugins mit externen Deps brauchen eine `.opencode/package.json` (sonst kein `bun install`). Älterer Beispielcode nennt `plugin/` (Singular) — Doku sagt Plural.
**Versionen:** per Design.
**FIX:** Pfade Plural; npm-Pakete ins `"plugin"`-Array; für Imports `.opencode/package.json` mit `dependencies`.
**Quelle:** https://opencode.ai/docs/plugins

### 53. Plugin-Crash beim Start / Desktop hängt — häufigste Ursache: ein Plugin
**Symptom:** Desktop-App crasht/hängt beim Start. (Auch Bun-Segfault nach Entfernen einer Plugin-Zeile, siehe §12.)
**Ursache:** Fehlverhalten eines Plugins (laut Doku häufigste Ursache) oder korrupter Cache.
**Versionen:** per Design.
**FIX (Doku-Reihenfolge):** 1) Plugins deaktivieren (`"plugin":[]`, lokale Plugin-Ordner wegschieben). 2) Cache leeren (`~/.cache/opencode`). 3) Plugins einzeln reaktivieren, um den Verursacher zu finden.
**Quelle:** https://opencode.ai/docs/troubleshooting#disable-plugins

### 54. Custom-Tool-Namenskollision überschreibt Built-in still
**Symptom:** Ein Built-in-Tool verhält sich unerwartet, weil ein Plugin-Tool denselben Namen trägt.
**Ursache:** per Design — Plugin-Tool hat bei Namensgleichheit Vorrang, keine Warnung.
**Versionen:** per Design.
**FIX:** Custom-Tools eindeutig benennen (Präfix), wenn kein Override gewollt.
**Quelle:** https://opencode.ai/docs/plugins

### 55. Windows: `git+https://`-Plugin-Install erzeugt ungültigen Cache-Pfad
**Symptom:** `ENOENT … mkdir 'C:\…\superpowers@git+https:\github.com\…'`; Install bricht ab. Auch „No git binary found in PATH" trotz installiertem Git.
**Ursache:** Cache-Pfad wird aus der rohen URL gebaut; `://`→`:\` → ungültiger Windows-Pfad. Bun-PATH-Lookup findet git nicht.
**Versionen:** v1.3.15; **#21099 gefixt (PR #21135)**; #21826 offen.
**FIX:** Plugin per npm-Paketnamen statt `git+https://`-URL referenzieren; vollen `git.exe`-Pfad sicherstellen.
**Quelle:** https://github.com/anomalyco/opencode/issues/21099 · https://github.com/anomalyco/opencode/issues/21826

### 55a. Kein `opencode plugin add` / `plugin list`-CLI — Plugin-Installation NUR über Datei/Array
**Symptom:** Suche nach einem CLI-Befehl zum Hinzufügen/Auflisten von Plugins (`opencode plugin add …`) läuft ins Leere — der Befehl existiert nicht.
**Ursache:** Plugin-Installation läuft per Design ausschließlich über (a) lokale Datei in `.opencode/plugins/` bzw. `~/.config/opencode/plugins/` oder (b) Eintrag im `plugin`-Array der `opencode.json`. Beides wird beim Start automatisch geladen (npm-Pakete via Bun nach `~/.cache/opencode/node_modules/`). Anders als MCP (`opencode mcp …`) gibt es KEIN Plugin-CLI.
**Versionen:** per Design, Stand Juni 2026 (v1.17.8).
**FIX:** Nicht nach `plugin add` suchen — Datei ablegen oder Paketname ins `plugin`-Array, `opencode` neu starten. (`plugin/` Singular wird abwärtskompatibel weiter geladen, `plugins/` Plural ist der empfohlene Standard.)
**Quelle:** https://opencode.ai/docs/plugins/ · https://opencode.ai/docs/ecosystem/

### 55b. Zwei Doku-Domains: `opencode.ai` offiziell vs. `open-code.ai` gespiegelt (Quellen-Falle)
**Symptom:** Suchtreffer zeigen `open-code.ai/...` (z.B. `open-code.ai/en/docs/plugins`) — ein separater/gespiegelter Host, nicht die offizielle Quelle.
**Ursache:** Neben dem offiziellen `opencode.ai` (Projekt `anomalyco/opencode`, früher `sst/opencode`) existiert ein getrennter Spiegel-Host. Veraltete/abweichende Inhalte möglich; Vertrauens-/Supply-Chain-Risiko bei Plugin-Empfehlungen von dort.
**Versionen:** Stand Juni 2026.
**FIX:** Im Zweifel immer `opencode.ai` (offizielle Doku) + `github.com/anomalyco/opencode` als Primärquelle; Plugin-Discovery offiziell über `opencode.ai/docs/ecosystem/`.
**Quelle:** https://opencode.ai/docs/

### 55c. Plugin-spezifische Fallen einzelner Ecosystem-Plugins (recherchiert Juni 2026)
**Symptom/Ursache + Fix je Plugin** — beim Einsatz des jeweiligen Plugins beachten. Übergreifend gilt: Plugins werden **event-getriggert** (kein Triggerwort, nicht bei jeder Anfrage); „Erst-Plan"-Plugins erzwingen keinen Plan bei trivialen Anfragen.

| Plugin | Falle | Funktionserhaltender Fix | Quelle |
|--------|-------|--------------------------|--------|
| notify/notificator/notifier | kdcokenny/notify: auf Windows nur fester System-Toast (eigene WAV nur macOS); panta82/notificator: Per-Projekt- statt Per-Event-Töne | Für „Ton pro Event auf Windows+Mac" mohak34/opencode-notifier nehmen | DeepWiki/OpenCodeDocs |
| sentry-monitor | `recordInputs`/`recordOutputs` Default **true** → Prompts, Tool-Outputs, ggf. Code/Secrets gehen an DSN-Ziel; kein Secret-Scrubber | `recordInputs:false`+`recordOutputs:false` ODER Self-Hosted-Sentry (eigener DSN) + Data-Scrubbing | github.com/stolinski/opencode-sentry-monitor |
| plannotator | <0.20.0 unter Node-Host importiert Bun-Server-Code → Inkompatibilität; häufige Breaking-Migrations (0.13.1/0.19.1) | v0.20.0+ nutzen; vor Update Migration-Doku lesen | plannotator.ai/docs |
| ocx (npm-Install) | braucht **Bun im PATH** zur Laufzeit; Node allein reicht nicht (Windows) | Auf Windows Binary-Install (install.sh) ODER Bun installieren | ocx.kdco.dev/docs |
| opencode-skillful | wartungsarm (letzter Commit ~4 Mon.), durch native Skills überholt | native Skills nutzen statt Plugin | sourcepulse.org/projects/27131596 |
| micode | keine Lizenz im Repo (Rechts-/Adoptions-Blocker) | vor produktiver Nutzung Lizenz klären | sourcepulse.org/projects/25924494 |
| firecrawl | **Es gibt KEIN npm-Plugin `opencode-firecrawl`** (404 auf npm) — Integration = offizieller **MCP-Server `firecrawl-mcp`** (Firecrawl-Team, MIT) im `mcp`-Block, NICHT im `plugin`-Array. Default = Firecrawl-Cloud (Inhalte → USA); braucht API-Key | Als MCP eintragen: `"mcp":{"firecrawl":{"type":"local","command":["npx","-y","firecrawl-mcp"],"environment":{"FIRECRAWL_API_KEY":"{file:~/SK/.../key}"}}}`. self-host möglich (schwächeres JS-Rendering); Key Free-Tier 1000 Seiten/Mon. Community-Plugin `@lyculs/opencode-firecrawl` v1.0.0 existiert (Einzel-Dev, ungepflegt) | firecrawl.dev · npm `firecrawl-mcp` |
| supermemory | Fehlinfo „cloud-only" kursiert — FALSCH | MIT + `npx supermemory local` (Port 6767), voll self-hostbar | github.com/supermemoryai/supermemory |
| opencode-worktree | erzeugt eigene Branches pro Worktree (git-bedingt) → unvereinbar mit „alles direkt auf main"-Workflows | bei Direkt-auf-main NICHT einsetzen; mehrere Fenster auf einem geteilten Worktree bleiben der Weg | git worktree-Mechanik |

**Versionen:** Stand Juni 2026 (OpenCode v1.17.8-Ökosystem).

### 55d. ⭐ Plugin-Deinstallation: kein `remove`-Befehl → Plugin kommt nach Neustart wieder (mehrere Speicherorte)
**Symptom:** Ein per `opencode plugin <name> --global` installiertes Plugin (real: `@tarquinen/opencode-dcp`) bleibt im Plugin-Picker sichtbar/aktivierbar und lädt bei jedem Start („loading plugins") — **auch nachdem** der `plugin`-Array-Eintrag aus `opencode.jsonc` entfernt und das Cache-Paket gelöscht wurde. Beim nächsten Start sind Cache-Paket **und** plugin-eigene Config (z.B. `dcp.jsonc`) wieder da.
**Ursache:** (1) OpenCode hat **KEINEN** `plugin remove`/`uninstall`-Befehl — `packages/opencode/src/cli/cmd/plug.ts` hat kein remove-Subkommando (Issue #30526). (2) Der `plugin`-Array wird über **mehrere Config-Dateien gemergt** (§20); `opencode plugin --global` schreibt den Eintrag auch nach **`tui.json`** (`{"plugin":[…]}`), nicht nur in `opencode.jsonc`. Ein **übersehener `tui.json`-Eintrag genügt**, damit OpenCode das Plugin beim Start lädt, das Cache-Paket (`~/.cache/opencode/packages/<scope>/`) **neu herunterlädt** und die plugin-eigene Config neu erzeugt. (3) Zusätzliche Drift-Quelle (Issue #30526): `~/.config/opencode/package.json` ist bei `plugin install --global` (arborist `reify save:true`) **append-only** — `patchPluginList()` räumt dort nie auf; bleibt ein Eintrag stehen, installiert `Loader.resolve()`→`Npm.add()` das Plugin beim Start neu. (4) **Läuft OpenCode während der Bereinigung**, erzeugt es die gelöschten Dateien sofort wieder.
**Versionen:** opencode-ai 1.17.x (Windows nativ), Vorfall 2026-07-01; Issue #30526 `[BUG]` OPEN. Hinweis: In diesem Vorfall war die `package.json` sauber (nur `@opencode-ai/plugin`) — alleinige Quelle war der `tui.json`-Eintrag + laufender Prozess. Punkt (3) kann je nach Install-Weg trotzdem greifen → immer mitprüfen.
**FIX (rückstandslos, in dieser Reihenfolge — funktionserhaltend, andere Plugins bleiben):**
1. OpenCode **komplett beenden** (Prozess killen — nicht nur Fenster; sonst regeneriert es). Sessions bleiben in der DB erhalten.
2. `plugin`-Eintrag aus **ALLEN** Config-Quellen entfernen: `~/.config/opencode/opencode.jsonc` **UND** `~/.config/opencode/tui.json` (+ evtl. Projekt-`opencode.json` / `.opencode/`). Verifikation: `grep -rl "<scope>" ~/.config/opencode/` muss leer sein.
3. Falls `~/.config/opencode/package.json` den Eintrag enthält: `package.json`, `package-lock.json`, `node_modules` in `~/.config/opencode/` zurücksetzen — OpenCode baut sie beim Start **nur aus der Config** neu auf (Issue-#30526-Workaround).
4. Cache-Paket löschen: `~/.cache/opencode/packages/<scope>/` (Windows: `Remove-Item -Recurse -Force`; **`rm -rf ~/…` ist per `bash-guard` blockiert** → PowerShell nutzen).
5. Plugin-eigene Config löschen (z.B. `~/.config/opencode/dcp.jsonc`).
6. OpenCode **neu starten**, Picker prüfen. Endkontrolle: kein `<scope>` unter `~/.cache/opencode/packages/` und keine Config nennt `<scope>`.
**Quelle:** https://github.com/anomalyco/opencode/issues/30526 · https://opencode.ai/docs/troubleshooting · eigener Vorfall (Frank, 2026-07-01); Recherche Firecrawl+MiniMax (Engine A).

---

## 8. MCP-Server

### 56. ⭐ MCP-Tool-Schemas fluten Kontext/Token — KEIN natives Lazy-Loading (anders als Claude Code)
**Symptom:** Session startet mit zehntausenden Tokens, Kontext früh voll. 4-Server-Setup ≈ 51.000 Tok (~47 % von 200K); GitHub-MCP allein 15–20k+ nur für Schemas. Extremfall: ein einzelner MCP-Server (lark-mcp-docx) **147k Token** (21k → 168k).
**Ursache:** OpenCode injiziert das vollständige `input_schema` ALLER aktiven MCP-Server eager in jeden Systemprompt — **kein** Lazy-Loading. Bestätigt für Stand **1.17.11 (Recherche 2026-06-26)**: weiterhin nicht implementiert. Offene/ungelöste Feature-Requests: **#8277** (lazy/dynamic loading), **#8625** (mcp search tool), **#9350** (MCP Tool Search, 85 % Reduktion), **#16206** (Two-Step Discovery), #17482 (closed als Duplikat von #8625). **Kontrast:** Claude Code hat seit **v2.1.7** einen automatischen „MCP Tool Search" (greift bei >10 % Kontext) — OpenCode nicht.
**Versionen:** per Design, bestätigt bis 1.17.11.
**FIX (einziger Hebel, da kein natives Lazy-Loading):** Nur benötigte Server aktiv lassen (`"enabled":false` deaktiviert ohne zu entfernen); global per Glob abschalten und **pro Agent** freischalten — global Top-Level `"tools":{"servername*":false}`, im Agent-Frontmatter `tools: {"servername*": true}`. So für Firecrawl umgesetzt (`~/.config/opencode/opencode.jsonc` Punkt 9 + `agents/researcher.md`). Vollständige Hebel: `best-practices/opencode/token-effizienz.md` §8.
**Quelle:** https://github.com/anomalyco/opencode/issues/9350 · https://github.com/anomalyco/opencode/issues/17482 · https://opencode.ai/docs/mcp-servers#caveats

### 57. MCP-Tool-Discovery-Timeout (Default 5000 ms)
**Symptom:** Langsam startende Server (z.B. `npx`-Kaltstart) liefern keine Tools / gelten als „failed".
**Ursache:** Default-`timeout` 5 s für das Tool-Listing.
**Versionen:** per Design (konfigurierbar).
**FIX:** `"timeout"` im MCP-Eintrag hochsetzen.
**Quelle:** https://opencode.ai/docs/mcp-servers#options

### 58. MCP-Kaltstart bei jedem `opencode run`
**Symptom:** Jeder `run` bootet lokale MCP-Server neu → spürbare Verzögerung.
**Ursache:** Ohne laufende Server-Instanz initialisiert jeder `run` die MCPs neu.
**Versionen:** per Design.
**FIX:** An laufende `opencode serve`-Instanz anhängen statt jedes Mal kalt zu starten.
**Quelle:** https://opencode.ai/docs/cli · https://opencode.ai/docs/mcp-servers

### 59. Remote-MCP zeigt „connected", registriert aber still keine Tools
**Symptom:** `type:"remote"` (SSE) steht auf „connected", Tools fehlen — keine Fehlermeldung.
**Ursache:** Verbindung gilt als erfolgreich, obwohl die Tool-Registrierung scheitert; teils fehlende `Accept: application/json, text/event-stream`-Header für SSE.
**Versionen:** v1.1.25, teils gefixt/unklar (#9425, #834).
**FIX (funktionserhaltend):** Server als **`type:"local"`** via `npx`/`bun x` mit API-Key in `environment` einbinden; auf `streamable-http` statt SSE wechseln; lokalen STDIO-Proxy vorschalten.
**Quelle:** https://github.com/anomalyco/opencode/issues/9425 · https://github.com/anomalyco/opencode/issues/834

### 60. Docker-MCP-Server brechen nach Upgrade v1.1.6 → v1.1.7+ (Regression)
**Symptom:** Docker-MCP (`type:local`, `command:["docker","run",…]`) verbinden ab v1.1.7 nicht mehr; Container healthy, Tools stumm, keine Fehlermeldung. Downgrade auf v1.1.6 stellt sofort wieder her.
**Ursache:** Regression im MCP-/STDIO-Lifecycle ab v1.1.7.
**Versionen:** ab v1.1.7, **offen** (#8171). Kompat-Verbesserungen v1.17.6/v1.17.7.
**FIX:** Auf v1.1.6 downgraden oder auf v1.17.6+ updaten (deklariert Client-Capabilities, MCP-Workspace-Root); alternativ MCP als direkten lokalen Prozess statt in Docker.
**Quelle:** https://github.com/anomalyco/opencode/issues/8171

### 61. Fehlender/abgelaufener MCP-Server lässt OpenCode hängen — kein Retry/Keepalive
**Symptom:** Initialisiert ein MCP-Server nicht (`Operation timed out after 5000ms`), gibt OpenCode auf/hängt; nach Idle schließt der Remote-Server, OpenCode markiert ihn „failed" und entfernt ihn — kein Auto-Reconnect.
**Ursache:** Kein Retry/Heartbeat/Pre-Use-Health-Check; reaktive Fehlerbehandlung.
**Versionen:** #829/#3273/#731 offen; #15209 **not planned**.
**FIX:** Betroffenen Server temporär `"enabled":false`; Startzeit verkürzen; manuell `opencode mcp connect <name>` / Neustart; Proxy/LB, der die Verbindung warmhält.
**Quelle:** https://github.com/anomalyco/opencode/issues/15209 · https://github.com/anomalyco/opencode/issues/829

### 62. MCP aus Projekt-Config (`.opencode/opencode.json`) wird nicht erkannt
**Symptom:** Dieselbe `mcp`-Definition wirkt global, aber nicht projektweit; MCP fehlt in `/status`.
**Ursache:** Projekt-Level-MCP-Definitionen wurden nicht zuverlässig geladen.
**Versionen:** v1.0.39, **not planned** (#4054) — vor erneutem Verlass testen.
**FIX (funktionserhaltend):** MCP-Server in die **globale** `~/.config/opencode/opencode.json` legen.
**Quelle:** https://github.com/anomalyco/opencode/issues/4054

### 63. OAuth (RFC 7591) vs. API-Key-Server
**Symptom:** Bei API-Key-Remote-Servern stört der automatische OAuth-Versuch; OAuth-Server brauchen manuelles Triggern.
**Ursache:** OpenCode erkennt 401 → startet automatisch Dynamic Client Registration (RFC 7591).
**Versionen:** per Design.
**FIX:** Für API-Key-Server `"oauth":false` + Key via `headers` (`Authorization: Bearer {env:KEY}`). Für OAuth `opencode mcp auth <name>`; Diagnose `opencode mcp debug <name>`/`mcp auth list`. Tokens in `~/.local/share/opencode/mcp-auth.json`.
**Quelle:** https://opencode.ai/docs/mcp-servers#oauth

### 64. Desktop-App kann MCP (noch) nicht nutzen
**Symptom:** In der Desktop-App (Beta) sind MCP-Tools nicht verfügbar, obwohl global konfiguriert.
**Versionen:** #16689.
**FIX:** MCP-abhängige Arbeit im TUI erledigen, bis die Desktop-App das unterstützt.
**Quelle:** https://github.com/anomalyco/opencode/issues/16689

### 64a. supermemory als self-hosted Memory-MCP anbinden — Fallen (recherchiert 2026-06-19)
**Kontext:** EIN self-hosted Memory-Server für Claude Code (MCP nativ) + OpenCode (remote MCP),
Daten bleiben lokal. Vollständiger Bauplan: `best-practices/opencode/self-hosted-memory-server.md`.
Generische Remote-MCP-Fallen stehen schon oben: §59 (connected, aber keine Tools → `type:local`/
streamable-http), §61 (kein Auto-Reconnect/Keepalive; Session-404 nach Server-Neustart → Re-Init),
§63 (OAuth vs API-Key → `oauth:false`). NEU/supermemory-spezifisch:

| Falle | Symptom/Ursache | Funktionserhaltender Fix | Quelle |
|-------|-----------------|--------------------------|--------|
| „supermemory ist cloud-only" | Verwechslung des gehosteten Endpoints/Pro-Plugins mit dem Produkt | Self-Host = **Single-Binary** (`npx supermemory local` / `curl …/install`), Port 6767, lokale WASM-Embeddings — KEIN Docker/Postgres nötig | supermemory.ai/docs/self-hosting |
| Daten verlassen trotz „self-host" den Server | LLM-Step (Summary/Extraktion) nutzt per Default `gpt-5.1` (Cloud) | Für den LLM-Step auf lokales Ollama umbiegen: `OPENAI_BASE_URL=http://localhost:11434/v1`, `OPENAI_API_KEY=ollama`, `OPENAI_MODEL=<klein, z.B. llama3.2:3b>` | supermemory.ai/docs/self-hosting/configuration |
| npm-Paket `supermemory` startet keinen Server | Das npm-Paket ist die SDK/CLI-Lib, NICHT der Server-Daemon | Server über `npx supermemory local`/curl; Binary heißt `supermemory-server` | npmjs.com/package/supermemory |
| OpenCode-Plugin `opencode-supermemory` „verlangt Pro" | Convenience-Plugin braucht laut Doku den Pro-Plan | Beim Self-Host den **remote-MCP-Weg** nehmen (`mcp{type:remote,oauth:false,headers}`) — kein Plugin nötig (Plugin-Name strittig: `opencode-supermemory` vs `@supermemory/opencode`) | supermemory.ai/docs/integrations/opencode |
| Port 6767 trotz Firewall aus dem Netz erreichbar | Bind `0.0.0.0`; Docker umgeht UFW (schreibt direkt in iptables) | An `127.0.0.1` binden + Caddy (TLS+Bearer) davor + Cloud-Firewall des Hosters (greift VOR Docker) | jeffgeerling.com (Docker exposing ports) |

**Versionen:** Stand 2026-06-19 (supermemory npm 4.24.12, OpenCode v1.17.8).

---

## 9. Skills (SKILL.md, nativ) & Custom Commands

### 65. Skills aus `.claude/skills/` nicht gefunden (Singular/Plural-Glob)
**Symptom:** Trotz versprochener Claude-Kompatibilität werden Skills nicht entdeckt.
**Ursache:** Discovery nutzte Glob `skill/**/SKILL.md` (Singular); Claude legt unter `skills/` (Plural) ab.
**Versionen:** v1.0.201, **Fix via PR #6252**; heutige Doku listet Plural-Pfade.
**FIX:** Updaten; vor Fix Skill symlinken (`ln -s ~/.claude/skills/<n> ~/.config/opencode/skill/<n>`).
**Quelle:** https://github.com/anomalyco/opencode/issues/6177

### 66. Skill taucht nicht auf — Frontmatter-/Namens-/Pfad-Fallen
**Symptom:** Skill erscheint nicht im `skill`-Tool.
**Ursache (mehrere):** Datei muss **`SKILL.md`** (Großschreibung) heißen; Frontmatter braucht `name` UND `description` (Pflicht); `name` = Ordnername und Regex `^[a-z0-9]+(-[a-z0-9]+)*$`; Namen über alle Suchorte eindeutig; `SKILL.md` direkt im Skill-Ordner.
**Versionen:** per Design.
**FIX:** Doku-Checkliste abarbeiten (Großschreibung, Pflichtfelder, Name=Ordner, Eindeutigkeit, Permissions).
**Quelle:** https://opencode.ai/docs/skills

### 67. Skill gelistet, aber nie getriggert — Description-Falle
**Symptom:** Skill ist gelistet, das Modell ruft ihn aber nie auf.
**Ursache:** Das Modell wählt Skills allein anhand der `description`; vage Beschreibung → nie gewählt.
**Versionen:** per Design.
**FIX:** `description` konkret/auslösernah („Use this when …"), 1–1024 Zeichen, klare Trigger.
**Quelle:** https://opencode.ai/docs/skills

### 68. Skill durch Permission versteckt (`deny`/`ask`)
**Symptom:** Skill scheint zu „fehlen", obwohl korrekt abgelegt.
**Ursache:** `skill:{"internal-*":"deny"}` blendet passende Skills aus; `tools:{skill:false}` entfernt den ganzen Block.
**Versionen:** per Design.
**FIX:** Skill-Permissions (global + pro Agent) prüfen, ggf. `allow`.
**Quelle:** https://opencode.ai/docs/skills

### 69. Command-Subagent-Falle (`agent:`/`subtask:`) & `$ARGUMENTS`-Quoting
**Symptom:** Command läuft unerwartet als isolierter Subtask (oder nicht); mehrteilige Argumente werden falsch zusammengesetzt.
**Ursache:** Ist der unter `agent:` referenzierte Agent ein Subagent, triggert der Command per Default eine Subagent-Invocation; `subtask:true` erzwingt sie. `$ARGUMENTS` (Rest) vs. `$1,$2` (positionell); Mehrwort-Argumente müssen gequotet werden; `!`cmd`` und `@datei` injizieren ungefiltert in den Prompt.
**Versionen:** per Design (Doppel-Injection von `$ARGUMENTS` gefixt v1.17.5/#31245).
**FIX:** `subtask:false/true` bewusst setzen; Positionsparameter korrekt quoten; Custom-Commands nicht wie Built-ins (`/init`, `/share`) benennen (sonst Override).
**Quelle:** https://opencode.ai/docs/commands

---

## 10. Provider / Modelle / OpenRouter / Auth

### 70. ⭐ `ProviderModelNotFoundError` — falsches ID-Format (erst beim Request)
**Symptom:** 404/„Model not found" beim ersten Senden; eine falsche Modell-ID wirft KEINEN Config-Fehler beim Start.
**Ursache:** Format zwingend `provider/model`. OpenRouter ist **doppelt**: volle ID `openrouter/<author>/<model>` (z.B. `openrouter/moonshotai/kimi-k2`), aber im `provider.openrouter.models`-**Key** nur `<author>/<model>`. Verwechslung → not found.
**Versionen:** per Design.
**FIX:** Volle ID im `model`/`small_model`/`--model`-Feld; Map-Key ohne `openrouter/`-Präfix. `opencode models` listet gültige IDs; Slug auf openrouter.ai/models prüfen.
**Quelle:** https://opencode.ai/docs/models · https://github.com/anomalyco/opencode/issues/7958

### 71. `ProviderInitError` (korrupte Config) — zurücksetzen
**Symptom:** `ProviderInitError` beim Start/Provider-Init.
**Ursache:** Ungültige/beschädigte gespeicherte Config.
**Versionen:** per Design (Diagnose).
**FIX:** Provider-Setup prüfen; bei Bedarf `rm -rf ~/.local/share/opencode` (Windows `%USERPROFILE%\.local\share\opencode`) — **Achtung: löscht Auth + Sessions** — dann `/connect`.
**Quelle:** https://opencode.ai/docs/troubleshooting#provideriniterror

### 72. `AI_APICallError` — veraltete, gecachte Provider-Pakete
**Symptom:** `AI_APICallError`/Parameter-Kompatibilitätsfehler.
**Ursache:** OpenCode installiert Provider-Pakete dynamisch und cached sie; der Cache veraltet gegenüber API-Änderungen.
**Versionen:** per Design / laufend.
**FIX:** `rm -rf ~/.cache/opencode` (Windows `%USERPROFILE%\.cache\opencode`) → Neustart lädt aktuelle Pakete.
**Quelle:** https://opencode.ai/docs/troubleshooting#ai_apicallerror-and-provider-package-issues

### 73. Custom-Provider: `max_tokens` hart 32000 / `limit.output` ignoriert
**Symptom:** LM Studio o.ä.: OpenCode ignoriert `limit.output`, sendet hart `max_tokens:32000`; ohne `limit` → `maxOutputTokens` 0 → AI-SDK-Fehler.
**Ursache:** Fehlender Fallback/Override für Custom-Provider-Limits.
**Versionen:** offen (#20078).
**FIX:** In der Provider-Config explizit `limit:{context,output}` + `options.max_tokens` pro Modell setzen.
**Quelle:** https://github.com/anomalyco/opencode/issues/20078

### 74. Lokale OpenAI-kompatible Endpoints: Modell-ID muss exakt der API entsprechen
**Symptom:** Custom-/lokaler Provider antwortet nicht oder Tool-Calls scheitern trotz korrekter `baseURL`.
**Ursache:** Jeder Key in `provider.models` muss exakt mit der von `GET /v1/models` gelieferten `id` übereinstimmen; viele lokale Modelle haben schwaches Tool-Calling.
**Versionen:** per Design.
**FIX:** `curl http://<host>:<port>/v1/models` → echte IDs übernehmen; `options.baseURL` korrekt; Modell mit starkem Tool-Calling (Qwen-/DeepSeek-Coder).
**Quelle:** https://opencode.ai/docs/providers

### 75. OpenRouter: Prompt-Caching greift nicht → Kosten explodieren
**Symptom:** Preis steigt bei JEDEM Request, obwohl gleicher Kontext; keine Cache-Read-Reduktion.
**Ursache:** Caching über OpenRouter ist provider-/routing-abhängig; „provider sticky routing" aktiviert sich bei agentischen Workflows erst nach dem ersten Cache-Hit; bei Sprung zwischen Sub-Providern schlägt jeder Cache fehl. Anthropic braucht `cache_control`-Breakpoints/direktes Routing.
**Versionen:** beobachtet (#677); serverseitig veränderlich.
**FIX:** Sub-Provider pinnen: `provider.openrouter.models.<model>.options.provider.order` + `allow_fallbacks:false`; ggf. `session_id` für sofortige Sticky-Sessions; Anthropic direkt routen; Mindest-Tokenlängen beachten (z.B. Sonnet 4.5: 1024, Opus/Haiku 4.5: 4096); Wirkung über `cache_discount`/OpenRouter-Activity prüfen.
**Quelle:** https://github.com/anomalyco/opencode/issues/677 · https://openrouter.ai/docs/guides/best-practices/prompt-caching

### 76. OpenRouter: falsches/teures Sub-Provider-Routing
**Symptom:** Gleiches Modell, unerwartet hoher Preis/Latenz; Cache mal ja, mal nein.
**Ursache:** Ohne Routing-Vorgabe lastverteilt OpenRouter über Sub-Provider mit unterschiedlichen Preisen/Caching.
**Versionen:** per Design.
**FIX:** `options.provider.order:["<billig+cachefähig>"]` + ggf. `allow_fallbacks:false`; `:floor` (günstigster) ans Slug; `:nitro` ist schnell, NICHT sparsam.
**Quelle:** https://openrouter.ai/docs/guides/routing/provider-selection

### 77. OpenRouter Free (`:free`): harte Tages-/Minutenlimits → 429
**Symptom:** `429 Too Many Requests` bei `:free`-Modellen, oft nach wenigen Anfragen; auch fehlgeschlagene Requests zählen aufs Kontingent.
**Ursache:** <10 Credits → 50 `:free`-Requests/Tag; ab $10 Guthaben → 1.000/Tag; zusätzlich ~20 req/min.
**Versionen:** OpenRouter-seitig.
**FIX:** Einmal $10 Guthaben (läuft nie ab) → 1.000/Tag; kein `:free` als Default/`small_model` für ernsthafte Sessions; Exponential Backoff; Idle-Retry-Loops vermeiden.
**Quelle:** https://openrouter.zendesk.com/hc/en-us/articles/39501163636379

### 78. OpenRouter: keine eingebaute Modell-Refresh — Cache löschen
**Symptom:** Neues Modell fehlt in `/models`; Preise veraltet. Es gibt KEIN `opencode models openrouter --refresh`.
**Ursache:** Modell-Liste lokal gecacht, kein Refresh-Befehl.
**Versionen:** #4734.
**FIX:** `~/.cache/opencode/models.json` löschen + Neustart → Modelle/Preise frisch laden.
**Quelle:** https://github.com/anomalyco/opencode/issues/4734

### 79. Anthropic-OAuth (Pro/Max) schlägt fehl: `400 invalid_grant` (PKCE-State-Bug) + ToS-Risiko
**Symptom:** `auth login` → Anthropic → Pro/Max scheitert; Token-Exchange `400 invalid_grant` („Invalid 'code'"), teils `429`.
**Ursache:** Das `opencode-anthropic-auth`-Plugin (v0.0.13) setzt den OAuth-`state` identisch zum PKCE-`verifier`; Anthropic blockt das. **Zusätzlich:** Abo-OAuth über inoffizielle Clients kann gegen Anthropics ToS verstoßen (Ban-Risiko).
**Versionen:** Plugin v0.0.13, offen (#18652, #18329, #6930).
**FIX:** Gepatchte Plugin-Version/Fork (eigener Zufalls-`state`); Code sauber (ohne Whitespace) einfügen. Risiko-bewusst: alternativ offiziellen API-Key statt Abo-OAuth.
**Quelle:** https://github.com/anomalyco/opencode/issues/18652 · https://github.com/anomalyco/opencode/issues/6930

### 80. `--hostname 0.0.0.0` ohne Passwort = offener Server
**Symptom:** `opencode serve --hostname 0.0.0.0` ist im LAN ungesichert erreichbar.
**Ursache:** `0.0.0.0` bindet auf alle Interfaces.
**Versionen:** per Design (Doku-„Caution").
**FIX:** `OPENCODE_SERVER_PASSWORD=… opencode serve --hostname 0.0.0.0`.
**Quelle:** https://opencode.ai/docs/windows-wsl#desktop-app--wsl-server

---

## 11. Token-Effizienz & Kosten

### 81. ⭐ `small_model` nicht gesetzt → teures Hauptmodell für Nebenaufgaben
**Symptom:** Viele kleine teure Calls (Session-Titel etc.); bei Self-Hosting laufen Nebenaufgaben gegen einen ungewollten Default.
**Ursache:** OpenCode nutzt ein „small model" (Default `gpt-5-nano` via OpenCode Zen) für Nebenaufgaben.
**Versionen:** per Design.
**FIX:** `"small_model":"openrouter/google/gemini-2.5-flash"` (o.ä. billig) explizit setzen; bei Self-Hosting `"share":"disabled"`.
**Quelle:** https://opencode.ai/docs/models · `best-practices/opencode/token-effizienz.md`

### 82. Versehentlich teures Default-Modell
**Symptom:** Hohe Kosten, weil das teuerste Modell (Opus/GPT-5.x) als Default auch Triviales bearbeitet; OpenCode merkt sich „last used".
**Versionen:** per Design.
**FIX:** `model` bewusst auf günstiges, tool-call-fähiges Modell; bei Bedarf via `/models` hochschalten; bei OpenRouter `:floor`.
**Quelle:** https://opencode.ai/docs/models

### 83. Riesige `AGENTS.md`/Regeldateien als Token-Grundlast
**Symptom:** Fixe Token-Grundlast pro Prompt; bei großen Regelwerken zigtausende Tokens.
**Ursache:** `instructions` + AGENTS.md werden vollständig in jeden Systemprompt geladen (vgl. §28).
**Versionen:** per Design.
**FIX:** Schlank halten/aufteilen; nur projektspezifische Globs; statische Inhalte vorn (caching-freundlich), dynamische hinten.
**Quelle:** https://www.truefoundry.com/blog/opencode-token-usage-how-it-works-and-how-to-optimize-it

### 84. Ganze Dateien einfügen statt gezieltem Read
**Symptom:** Kontext explodiert durch roh eingefügte große Dateien.
**Versionen:** Bedienfehler.
**FIX:** `@`-Mentions/Read mit Zeilenbereichen; erst grep/glob zur Lokalisierung, dann gezielt lesen.
**Quelle:** https://www.truefoundry.com/blog/opencode-token-usage-how-it-works-and-how-to-optimize-it

### 85. Exzessiver Token-Verbrauch — ganzer Projektkontext pro Request (Regression)
**Symptom:** Seit ~12.01.2026: 13.000+ Tok für „what's the tech stack?" in frischem Projekt; Monatskontingent in einem Tag weg.
**Ursache:** Offenbar wird der gesamte Projektkontext bei jedem Request mitgesendet (Regression).
**Versionen:** ~Jan 2026 (#8234).
**FIX:** Auf gefixte Version updaten; Kontext klein halten; `.gitignore` strikt; in Monorepos chirurgisch arbeiten („nur `pkg/auth/`…").
**Quelle:** https://github.com/anomalyco/opencode/issues/8234

### 86. Kosten-/Step-Falle: kein Step-Limit + falsche Kostenanzeige
**Symptom:** ~438 USD an einem Nachmittag; TUI-Kosten < Hälfte der echten Rechnung.
**Ursache:** (1) kein Max-Step-Limit pro Subagent → Endlosläufe; (2) falsche Gemini-Preisstufen (>200K = 2×-Tarif, OpenCode rechnete Basistarif); (3) stille teure Hintergrund-Modelle.
**Versionen:** extern (Blog), 2026.
**FIX:** Hartes `steps`-Limit; teure Modelle bewusst zuweisen; TUI-Kosten als unzuverlässig behandeln; beim Provider (OpenRouter/Google) Spend-Cap setzen; lange autonome Läufe beaufsichtigen.
**Quelle:** https://www.glukhov.org/ai-devtools/opencode/oh-my-opencode-experience/

### 87. `$ Spent`-Kostentracking $0 bei Custom-Providern
**Symptom:** Bei Custom-/lokalen Providern zeigt OpenCode dauerhaft `$0.00`.
**Ursache:** Preise kommen aus models.dev; für Custom-Modelle haben die `cost`-Felder keine UI-Wirkung.
**Versionen:** offen, PR #17645 (#17223).
**FIX:** Kosten extern überwachen (Provider-Dashboard/LiteLLM/OpenRouter-Activity); UI-`$ Spent` nicht für Budget-Entscheidungen nutzen.
**Quelle:** https://github.com/anomalyco/opencode/issues/17223

### 88. Subagent-Token im TUI unsichtbar
**Symptom:** Der Token-Counter zeigt nur die Hauptsession; per `task` gespawnte Subagents tauchen nirgends auf → versteckte Kosten.
**Versionen:** offen (#22103).
**FIX:** Community-Plugin `opencode-tokenscope` zur Kostenanalyse; Step-Limits setzen.
**Quelle:** https://github.com/anomalyco/opencode/issues/22103

---

## 12. Abstürze (Bun-Runtime — Windows-Segfaults)

> Viele „Crash"-Reports sind KEIN OpenCode-Bug im engeren Sinn, sondern Segfaults der eingebetteten
> **Bun-Runtime** (v.a. Windows). Workaround meist identisch: Bun/OpenCode-Version wechseln, Cache leeren.

### 89. Bun-Segfault nach Entfernen einer Plugin-Zeile → dauerhaft unstartbar
**Symptom:** Sofortiger `panic(main thread): Segmentation fault` beim Start, nachdem ein Eintrag aus dem `plugins`-Array entfernt wurde; danach unstartbar.
**Ursache:** Bun 1.3.13 Segfault (Win x64), getriggert durch verwaisten Plugin-Cache-Zustand.
**Versionen:** Bun 1.3.13/Win11, offen (#26890).
**FIX:** Plugin-Cache leeren (`~/.cache/opencode`) statt nur die Config-Zeile; Plugins über die Config deaktivieren (`"plugin":[]`); Bun/OpenCode-Version wechseln.
**Quelle:** https://github.com/anomalyco/opencode/issues/26890

### 90. Bun-Segfault/Panic beim Chatten oder nach Tool-Calls (Windows)
**Symptom:** CLI startet, crasht im Chat/nach Tool-Calls (`Bun has crashed` / „Illegal instruction").
**Ursache:** Bun-Runtime-Segfault (Win x86_64, CPU-/Baseline-Mismatch); teils Beta-Auth-Plugin im Spiel.
**Versionen:** Bun 1.3.9 u.a., offen (#14724, #15143, #9594).
**FIX:** Bun-Version wechseln (Down-/Upgrade); Baseline-Build für ältere CPUs; Beta-Plugins entfernen. Upstream: oven-sh/bun.
**Quelle:** https://github.com/anomalyco/opencode/issues/14724

### 91. Crash auf aarch64 mit 64-KB-Pagesize
**Symptom:** Sofortiges `Aborted (core dumped)` beim Start (z.B. Grace Hopper, 64-KB-Pages).
**Ursache:** Bun/JS-Engine inkompatibel mit 64-KB-Pagesize.
**Versionen:** offen (#12474).
**FIX:** Kernel/Container/VM mit 4-KB-Pagesize.
**Quelle:** https://github.com/anomalyco/opencode/issues/12474

### 92. Windows: Freeze vor TUI (fehlende `fast-deep-equal`-Dependency)
**Symptom:** OpenCode hängt vor TUI-Start.
**Ursache:** Korrupter/unvollständiger node_modules-Cache (fehlende `fast-deep-equal`).
**Versionen:** offen (#9870).
**FIX:** node_modules-Cache leeren und neu installieren.
**Quelle:** https://github.com/anomalyco/opencode/issues/9870

### 93. Desktop: leeres Fenster (Windows WebView2 / Linux Wayland)
**Symptom:** Desktop-App öffnet blank / startet nicht.
**Ursache:** Windows: fehlende/veraltete Edge **WebView2 Runtime**. Linux: Wayland-Kompatibilität.
**Versionen:** per Design.
**FIX:** Windows: WebView2 installieren/aktualisieren. Linux: mit `OC_ALLOW_WAYLAND=1` starten, sonst X11-Session. Reset: `"plugin":[]`, Cache leeren, ggf. `opencode.settings.dat`/`opencode.global.dat` entfernen.
**Quelle:** https://opencode.ai/docs/troubleshooting

---

## 13. Sicherheit (externer Code: Plugins / MCP / Skills)

### 94. Externe Plugins/MCP/Skills führen ungeprüft Code/Anweisungen aus
**Symptom:** Ein installiertes Plugin/MCP/Skill enthält versteckte Anweisungen, exfiltriert Daten oder führt schädliche Shell-Befehle aus.
**Ursache:** Plugins sind beliebiger JS/TS-Code mit vollem Zugriff auf Bun-Shell (`$`), Env-Injection und Tool-Pipeline; npm-Plugins werden beim Start automatisch via Bun installiert und ausgeführt. MCP-Tool-Beschreibungen + Skill-Bodies fließen in den Modell-Kontext (Prompt-Injection-Vektor). Keine eingebaute Signatur-/Sandbox-Prüfung.
**Versionen:** per Design / Dauerrisiko.
**FIX (präventiv, vgl. CLAUDE.md „Sicherheit bei externem Code"):** Vor Installation Quelltext lesen (keine versteckten Instruktionen, kein Base64-Payload, keine Exfiltration/fremde URLs); nur vertrauenswürdige Quellen; npm-Pakete pinnen. Schutz-Plugin-Muster (z.B. `.env`-Read-Block via `tool.execute.before`); MCP-Token-Flut per Per-Agent-Whitelisting begrenzen (§56). Restriktionen via `permission:` verankern, nicht nur im Plugin (§49).
**Quelle:** https://opencode.ai/docs/plugins · https://opencode.ai/docs/mcp-servers#caveats

---

## Fix-Status (Schritt 3 — was ist belegt gefixt, was noch offen)

> Ehrlichkeit: `gh` war im Recherche-Sandbox nicht verfügbar — der Status stammt aus dem **offiziellen
> Changelog/Release-Notes** (v1.1.64→v1.17.8) und den von Researchern gelesenen Issue-Seiten.
> Strikt getrennt: *belegt gefixt* (Changelog/Release-Tag) vs. *offen / per Design / Status unklar*.
> Im Zweifel gilt: **noch offen**. PR-gemergt ohne sicheres Release-Tag = „gefixt (Tag unklar)".

### Belegt gefixt (in einer Version ≤ v1.17.8)

| Frührer Bug | gefixt ab | Bezug |
|-------------|-----------|-------|
| ACP hing Windows endlos „thinking" | v1.1.65 | §1/§12 |
| `run` crashte bei malformed Tool-Inputs; CLI-Fenster Windows; `--max-count` ignoriert; TUI hing nach Session-Ende; OAuth-Creds nicht invalidiert; `OPENCODE_CONFIG_CONTENT`-Substitution | v1.2.0 / v1.2.7 | §3/§6/§10 |
| `/sessions` leer trotz Daten | ~v1.2.x (nach v0.14.6) | §31 |
| tmux graue Box (Regression v1.2.17) | v1.2.25 | §13 |
| SIGINT/SIGTERM über npm-Shim | v1.14.42 | §1 |
| Plan-Mode-Security-Bypass (Subagent ignorierte Parent-Deny); MCP-Discovery bei kaputtem outputSchema; alte Sessions negative Token-Counts | v1.14.46 | §6/§8 |
| GPT-5 + zu lose Edit-Matches; Bedrock-Sessions hingen; TUI-Crash bei malformed Path/Diff | v1.16.2 | §10/§14 |
| Per-call Tool-Rules in Session-Permission gemergt; MCP-Abort-Signals/Katalog-Pagination; `mcp add` non-interaktiv; OpenRouter-Reasoning-Varianten; Context-Overflow-Recovery | v1.17.0 | §6/§8/§5 |
| MCP-Prompt/Resource-Timeouts; MCP-Client hing bei kaputter Verbindung; macOS-Auto-Updates; `reference`→`references` | v1.17.1 | §8 |
| Subagent nutzt eigene Permissions; abgelaufene Remote-Config-Auth | v1.17.2 (Desktop-Crash-Hotfix v1.17.3) | §6 |
| MCP-Catalog-Timeouts; MCP-Header bei Auth/Debug; content-gefilterte Antworten scheiterten still; Gemini Multi-Type-Schemas; Snapshot-Re-Hashing langsam | v1.17.4 | §8/§5 |
| Abgelaufene MCP-Sessions; `$ARGUMENTS` doppelte Injection; doppelte renderable IDs (TUI) | v1.17.5 | §8/§9/§14 |
| MCP-Client-Capabilities deklariert (Kompatibilität) | v1.17.6 | §8 |
| Plugin-Client nutzt aktive Server-Instanz; Plugin-Shell-Env in PTY; MCP erhält Workspace als Client-Root | v1.17.7 | §7/§8 |
| OpenAI-kompatible Provider lehnten MCP-Tool-Schemas ab; MCP-Tools ohne `properties`; langlaufende MCP-Tools verloren Timeout; MCP-OAuth-Callback-Server blieb offen; Cloudflare AI Gateway Key | v1.17.8 | §8 |
| `opencode upgrade` Windows „unknown" | PR #18010 (Tag unklar) | §1 (#4) |
| Windows `git+https`-Plugin-Pfad | #21099 (PR #21135) | §7 (#55) |
| GPT-5.5 Codex 400k-vs-1M-Limit | #24171 (PR #24212) | §5 |
| globale `AGENTS.md` ignoriert; `OPENCODE_CONFIG_DIR/AGENTS.md` | PR #22317 / #11536 (Tag unklar) | §4 |
| MCP-Tool-Calls lösten Plugin-Hooks nicht aus; Skill-Discovery Singular/Plural; `agent create`-Pfad; Subagent ignorierte Body | PR #2320 / #6252 / #14427 / (#8733) — Tag unklar | §7/§9/§6 |

### Noch NICHT gefixt / per Design / Status unklar (Workaround bleibt aktiv)

- **Bun-Runtime-Segfaults auf Windows** (#26890, #14724, #15143, #9594, #12474) — offen, Bun-seitig.
- **Docker-MCP-Regression ab v1.1.7** (#8171) — offen; v1.17.6/7 bringen Kompat-Verbesserungen.
- **Kein MCP-Keepalive/Reconnect** (#15209) & **Projekt-Level-MCP nicht erkannt** (#4054) — *not planned*.
- **`permission`-Block schluckt PascalCase/unbekannte Keys still** (#15507) & **Top-Level-strict-Crash** (#7483/#1081) — per Design.
- **Config-Merge/Array-Concat** & **`{env:}` still leer** — per Design.
- **Kein eingebautes Langzeit-Memory** (#16077); **Session-Korruption/-Wachstum** (#14194, #22110, #26207).
- **Auto-Compaction triggert zu spät / verliert Details** (#8089, #6068, #16512) — offen.
- **TUI-Freeze busy-wait** (#12834 *not planned*), **WSL2-Freeze v1.0.129** (#5361), **Windows-Encoding/Paste** (#14661, #13800, #17616) — offen.
- **Cost-Tracking $0 bei Custom-Providern** (#17223), **`max_tokens` hart 32000** (#20078), **Subagent-Token unsichtbar** (#22103) — offen.
- **OpenRouter-Caching greift nicht** (#677), **kein `--refresh`** (#4734), **`:free`-Rate-Limits** — teils OpenRouter-seitig.
- **Anthropic-OAuth-PKCE-Bug** (#18652) + **ToS-Ban-Risiko** (#6930) — offen.
- **`opencode upgrade` unknown** / **NVM-windows** / **`.opencode-ai-*`-Temp** — Windows, teils offen.

---

## ✅ Pflicht-Checkliste vor/bei OpenCode-Arbeit

- [ ] **Windows?** → Wenn möglich in **WSL** arbeiten (Repo im WSL-Dateisystem). Nativ nur mit UTF-8/WSL-Workarounds.
- [ ] **Version aktuell?** `opencode --version` gegen Anker (1.17.8) — viele Fixes hängen an v1.17.x.
- [ ] **Config:** nur dokumentierte Top-Level-Keys (`opencode debug config`); `$schema` gesetzt; Kommentare nur in `.jsonc`.
- [ ] **Permissions:** Keys **lowercase**; `edit`/`bash` bewusst auf `ask`/`deny`; nach Setzen mit Deny-Test verifizieren.
- [ ] **Modell-IDs:** `provider/model`, OpenRouter `openrouter/<author>/<model>`; `model` + `small_model` bewusst (günstig) gesetzt.
- [ ] **MCP minimal:** nur benötigte Server; pro Agent freischalten; `timeout` für langsame Server; lokale statt remote bei SSE-Problemen.
- [ ] **AGENTS.md schlank** (<~150 Zeilen); globale per `@`-Referenz mitladen; nach `/new` ggf. neu lesen lassen.
- [ ] **Kompaktierung:** `compaction.prune:true`, `reserved` hoch; kritische Fakten in AGENTS.md; große Outputs in Dateien.
- [ ] **Verzeichnisse Plural:** `agents/ commands/ plugins/ skills/ tools/`.
- [ ] **Kosten:** Step-Limit (`steps`); Spend-Cap beim Provider; TUI-`$ Spent` nicht trauen (besonders Custom-Provider/Subagents).
- [ ] **Externe Plugins/MCP/Skills:** vor Installation Quelltext prüfen (Prompt-Injection/Exfiltration).
- [ ] **Bei Fehler:** Logs `~/.local/share/opencode/log/`; `~/.cache/opencode` (Provider) bzw. `~/.local/share/opencode` (Config, löscht Auth!) gezielt zurücksetzen.

---

## Bezugs-Tabelle: Bug-Abschnitt ↔ Best-Practice-Abschnitt

> Gegenseite: `best-practices/opencode/` (8 Dateien). Jede Bug-Lösung zeigt hier auf die passende
> Best-Practice (wie man es von vornherein richtig macht), und umgekehrt.

| Bug-Almanach (diese Datei) | Best-Practice-Datei (`best-practices/opencode/`) |
|----------------------------|--------------------------------------------------|
| §1 Installation/Update, §2 TUI, §12 Bun-Crashes | `grundlagen-installation.md` (§2 Installation, §4 TUI, §7 Windows-Stolperfallen, §8 Logs/Debug) |
| §3 Konfiguration | `konfiguration.md` (Schema, Präzedenz, Variablen, Permissions, Beispiel-Configs) |
| §4 AGENTS.md/Memory/Sessions, §5 Kontext | `agents-md-memory.md` (AGENTS.md, /init, Precedence, instructions, Memory, Compaction) |
| §6 Agents/Modes/Permissions | `agents-modes.md` (Primary/Subagents, Plan/Build, Custom Agents, Permissions, Modellwahl) |
| §7 Plugins, §8 MCP, §9 Skills/Commands, §13 Sicherheit | `plugins-mcp-skills.md` (MCP, Plugins/Hooks, Custom Tools, Skills, Commands, Sicherheit) |
| §10 Provider/OpenRouter/Auth | `openrouter.md` (Setup, ID-Format, Provider-Routing, Caching, Modelle, Limits) |
| §11 Token-Effizienz & Kosten | `token-effizienz.md` (Spar-Hebel, Caching, günstige Modellstrategie, Kosten beobachten) |
| Querschnitt (Index/Kern) | `README.md` (Index + Kern-Erkenntnisse) |

---

## 14. OpenCode-Go-Abo (Modell-Gateway, Stand Juni 2026)

> Das **OpenCode-Go-Abo** ($5 erster Monat, dann $10/Mo) gibt Zugriff auf 14 Modelle über das
> OpenCode-Zen-Gateway. Recherchiert 2026-06-20. Modell-Auswahl + Pipeline-Empfehlung:
> `best-practices/opencode/go-recherche-modelle.md`.

### 14.1 ⭐ Zwei verschiedene API-Schemata je Modell (Endpunkt-Verwechslung → Fehler)
**Symptom:** API-Aufruf an ein Go-Modell schlägt fehl, obwohl Key + Slug stimmen.
**Ursache:** OpenCode Go bedient **zwei** Schemata gleichzeitig:
- **OpenAI-kompatibel** `…/zen/go/v1/chat/completions` → `deepseek-v4-pro`, `deepseek-v4-flash`, `glm-5.2`, `glm-5.1`, `kimi-k2.7-code`, `kimi-k2.6`, `mimo-v2.5`, `mimo-v2.5-pro`
- **Anthropic** `…/zen/go/v1/messages` → `qwen3.7-max`, `qwen3.7-plus`, `qwen3.6-plus`, `minimax-m3`, `minimax-m2.7`
**Versionen:** OpenCode Go, Stand 2026-06.
**FIX:** Endpunkt zum Modell passend wählen. Für die Recherche-Pipeline → DeepSeek = **OpenAI-Schema**.
Modell-Discovery: `…/zen/go/v1/models`. Config-Referenz `opencode-go/<slug>`.
**Quelle:** opencode.ai/docs/go, bitdoze.com/opencode-go-plan

### 14.2 Dollar-Quote über alle Modelle gemeinsam — GLM-5.x früh „aufgebraucht"
**Symptom:** GLM-5.x reicht nur für sehr wenige Anfragen, dann Quote erschöpft.
**Ursache:** Quoten sind **dollarbasiert** ($12/5h, $30/Woche, $60/Monat) über ALLE Modelle gemeinsam; teure
Modelle verbrauchen das Budget viel schneller. Erreichbare Req/Mo: DeepSeek V4 Flash ~158.000 — **GLM-5.1 nur ~4.300**.
**Versionen:** OpenCode Go, Stand 2026-06.
**FIX:** GLM-5.x **nicht** als Massen-Auswerter nutzen. Für Recherche-Masse DeepSeek V4 Flash/Pro (günstig + hohe Req-Zahl).
**Quelle:** bitdoze.com/opencode-go-plan

### 14.3 DeepSeek V4 Pro/Flash halluziniert bei Nichtwissen (trotz Top-Faktenwissen)
**Symptom:** Modell erfindet plausible Fakten, statt „steht nicht in den Quellen" zu sagen.
**Ursache:** Trotz bestem Open-Weight-Faktenwissen (SimpleQA-Verified 57.9) hat V4 eine hohe
„antwortet-trotzdem"-Quote bei Wissenslücken (AA-Omniscience 94 %).
**Versionen:** DeepSeek V4 Pro/Flash (2026-04-24).
**FIX (funktionserhaltend):** Im Pipeline-Prompt **Abstain erzwingen** („nur aus den Quellen; bei Unsicherheit
explizit sagen; nicht erfinden") + **Thinking-Modus an** (senkt SimpleQA-Halluzination 12.7 % → 10.4 %).
Bei heiklen Fakten zweite Meinung (Kimi K2.6) oder gegen Opus eskalieren.
**Quelle:** medium.com/@leucopsis/deepseek-v4-review, digitalapplied.com/blog/ai-model-hallucination-rate-benchmarks-2026-study

### 14.4 ⭐ Custom-`@ai-sdk/anthropic`-Provider mit eigener baseURL verliert den API-Key zur Laufzeit
**Symptom:** Ein selbst angelegter Provider-Block (`provider: { "opencode-go": { npm:"@ai-sdk/anthropic", options:{baseURL,apiKey} } }`) für ein Anthropic-Schema-Modell (z.B. MiniMax über das Go-Gateway) authentifiziert beim ersten Aufruf, schlägt dann aber mit Auth-Fehler fehl.
**Ursache:** Offener Bug — der Custom-Anthropic-Adapter mit abweichender baseURL hält den Key nicht durch (anomalyco/opencode #21737). Zusätzlich kursiert eine falsche Default-baseURL für OpenCode Zen (`api.opencode-zen.com` statt `opencode.ai/zen/...`).
**Versionen:** OpenCode aktuell (2026-06).
**FIX (funktionserhaltend):** OpenCode Go ist **eingebaut** — per TUI `/connect` → „OpenCode Go" aktivieren (Key in `auth.json`), KEINEN Custom-Provider-Block bauen. Wenn man Modell-Optionen (z.B. Thinking-Budget) braucht: nur einen **MERGE-Block** `provider.opencode-go.models.<id>.options` setzen, OHNE npm/baseURL/apiKey — der Key kommt aus `auth.json`.
**Quelle:** https://github.com/anomalyco/opencode/issues/21737 · https://opencode.ai/docs/go/

### 14.5 Thinking/Reasoning richtig aktivieren — Keys stehen in docs/models, nicht docs/config
**Symptom:** Thinking-Modus lässt sich „nicht aktivieren"; in `docs/config` findet man keinen Schalter.
**Ursache:** Die Thinking/Reasoning-Keys sind in `opencode.ai/docs/models` dokumentiert, nicht in `docs/config` (leicht zu übersehen). Zusätzlich ist der Key **schema-abhängig**: Anthropic-Schema-Modelle nutzen `options.thinking.budgetTokens` (Stil `{type:"enabled",budgetTokens:N}`), OpenAI-Schema-Modelle `reasoningEffort`. Verwechslung → wirkungslos.
**Versionen:** OpenCode aktuell (2026-06).
**FIX:** Schema des Modells prüfen (Go: MiniMax/Qwen = Anthropic `/messages`; DeepSeek/GLM/Kimi/MiMo = OpenAI `/chat/completions`). MiniMax M3 denkt am Anthropic-Endpunkt **nativ** (kein Pflicht-Parameter); `options.thinking.budgetTokens` setzt nur das Budget. Response: Anthropic → `type:"thinking"`-Blöcke im Content (müssen in der History bleiben); OpenAI-Schema → `reasoning_details` (bei `reasoning_split=true`) bzw. `<think>`-Tags.
**Quelle:** https://opencode.ai/docs/models/ · https://platform.minimax.io/docs/guides/text-m3-function-call

### 14.6 ⭐ Direkter Go-API-Call: `/messages` braucht `x-api-key` (nicht Bearer) + TUI-Thinking-Anzeige-Bug
**Symptom A:** Direkter HTTP-POST an `https://opencode.ai/zen/go/v1/messages` (MiniMax/Qwen, Anthropic-Schema) liefert `{"error":{"type":"AuthError","message":"Missing API key"}}`, obwohl `GET /models` mit demselben Key (Bearer) HTTP 200 gibt.
**Ursache:** Der Anthropic-Endpoint `/messages` akzeptiert **nur** den Header `x-api-key: <key>` (+ `anthropic-version`). `Authorization: Bearer` funktioniert NUR bei `/models` und dem OpenAI-Endpoint `/chat/completions`. (LIVE verifiziert 2026-06-20.)
**Versionen:** OpenCode Go Gateway, Stand 2026-06.
**FIX:** Schema-passenden Header nutzen: Anthropic `/messages` → `x-api-key`; OpenAI `/chat/completions` + `/models` → `Authorization: Bearer`. Thinking am `/messages`-Endpoint: `"thinking":{"type":"enabled","budget_tokens":N}` (max_tokens > budget_tokens); Antwort enthält `content`-Blöcke `type:"thinking"` + `type:"text"`.
**Symptom B (TUI):** In der OpenCode-TUI wird MiniMax-M3-Thinking seit ~09.06.2026 nicht mehr angezeigt (Issue #31569; ähnlich M2.7 #22684, M2.5 #20782). Der **direkte API-Call liefert das Thinking aber weiterhin** — es ist nur die TUI-Anzeige.
**Quelle:** Live-curl-Test 2026-06-20 · https://github.com/anomalyco/opencode/issues/31569

### 14.7 Mythos „MiniMax = nur OpenAI-Schema, /messages gibt 404" — durch Live-Test WIDERLEGT
**Symptom:** Web-Recherche (auch starke Modelle wie Opus, Quelle DeepWiki o.ä.) behauptet, MiniMax M3 laufe im Go-Gateway **ausschließlich** über OpenAI `/chat/completions`, der Anthropic-Endpunkt `/messages` gäbe 404, und `thinking.budget_tokens` funktioniere nicht.
**Ursache:** Mehrdeutige/veraltete Sekundärquellen + Übergeneralisierung. **Live-Test 2026-06-20 beweist: BEIDE Schemata funktionieren** — `/messages`+`x-api-key`+`thinking.budget_tokens` liefert HTTP 200 mit Thinking-Blöcken; `/chat/completions`+`Bearer` ebenfalls. Kein 404.
**Lehre:** Bei API-Fakten **empirisch testen (curl) statt Recherche glauben** — ein 10-Sekunden-Probe-Call schlägt jede Sekundärquelle.
**Thinking-Parameter — zwei belegte Wege:** (a) Anthropic `/messages`: `"thinking":{"type":"enabled","budget_tokens":N}` (live ok; wirkt als Obergrenze, M3 denkt adaptiv nach Aufgaben-Komplexität). (b) OpenAI `/chat/completions`: `"thinking":{"type":"adaptive"}` + `"reasoning_split":true` → Thinking in `choices[0].message.reasoning_details[0].text`. Es gibt KEIN numerisches „max"-Level (nur adaptive/enabled vs. disabled).
**Claude Code als Backend:** direkter curl-Bash-Call (kein Proxy) ODER Proxy `oc-go-cc` (übersetzt Anthropic↔OpenAI), dann `ANTHROPIC_BASE_URL=http://127.0.0.1:3456` + `claude`. (Proxy-Weg recherchiert, nicht live getestet.)
**Quelle:** Live-curl-Test 2026-06-20 · deepwiki.com/sst/opencode/4.5-opencode-zen-and-go-services · platform.minimax.io/docs/api-reference/text-openai-api · shravanbhati.com/blog/run-opencode-zen-and-go-models-with-claude-code-cli

### 14.8 ⭐ urllib-Default-User-Agent → Cloudflare 403 / „error code 1010" (Go-Gateway UND OpenRouter)
**Symptom:** Ein direkter Python-`urllib`-POST an `opencode.ai/zen/...` (Go-Gateway) oder `openrouter.ai/api/...`
liefert HTTP 403 mit Cloudflare „error code 1010" / „Just a moment", obwohl Key/Body korrekt sind. Derselbe
Call mit `curl` geht durch.
**Ursache:** Cloudflare vor den Gateways blockt den urllib-Default-User-Agent (`Python-urllib/3.x`) als Bot;
`curl` sendet einen akzeptierten UA.
**Versionen:** beobachtet 2026-06-20 (`mm-research.py`/`or-research.py`); Cloudflare-seitig, dauerhaft.
**FIX (funktionserhaltend):** Bei JEDEM urllib-Request an diese Gateways `User-Agent: curl/8.5.0` setzen
(`headers={"User-Agent":"curl/8.5.0", ...}` — genau das tun beide Skripte in `_post()`). Alternativ
`requests`/`httpx` mit eigenem UA. NIE den urllib-Default-UA gegen Cloudflare-gefrontete APIs lassen.
**Quelle:** eigener Vorfall 2026-06-20 (research-pipeline).

---

## Quellen (Auswahl)

**Offiziell:** opencode.ai/docs (cli, tui, config, rules, agents, permissions, mcp-servers, plugins,
custom-tools, skills, commands, models, providers, keybinds, windows-wsl, troubleshooting, share),
opencode.ai/changelog, github.com/anomalyco/opencode/releases (v1.1.64→v1.17.8),
openrouter.ai/docs (prompt-caching, provider-selection, rate-limits).

**GitHub-Issues (anomalyco/opencode):** #2447 #4047 #4054 #4592 #4734 #4997 #5361 #5894 #6068 #6177
#6316 #677 #7006 #7483 #7958 #8089 #8171 #8234 #8410 #8733 #9282 #9350 #9425 #9870 #11532 #11534
#12002 #12301 #12474 #12834 #13800 #13946 #14194 #14308 #14410 #14562 #14661 #14923 #15143 #15209
#15388 #15507 #16077 #16327 #16512 #16689 #16967 #17223 #17295 #17340 #17482 #17616 #18329 #18362
#18652 #20078 #20859 #21099 #21826 #22020 #22103 #22110 #22600 #23443 #24171 #25264 #26207 #26890
(PRs: #2320 #6252 #11536 #14427 #18010 #21135 #22317 #24212 #30529 #31245 #31745 #31798 #31877 #32052 #32477 #32489).

**Community/extern (gelabelt):** sanj.dev, builder.io, composio.dev, nxcode.io, glukhov.org,
hindsight.vectorize.io, truefoundry.com, portkey.ai, developersdigest.tech, deepwiki.com,
openrouter.zendesk.com, klymentiev.com.
