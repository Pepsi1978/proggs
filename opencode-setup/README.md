# OpenCode-Setup — plattformuebergreifende Umgebung (Windows + macOS)

> Stand: v1.0.5 - 17.07.2026, 18:53 Uhr

> Zweck: Damit OpenCode auf JEDEM Rechner (Windows wie macOS) **1:1 dieselbe Umgebung** einliest.
> Die hier gespiegelten globalen Dateien liegen im echten Betrieb unter `~/.config/opencode/`
> (NICHT im Repo) — dieser Ordner haelt sie versioniert fest, sodass ein neuer Rechner mit **einem
> Befehl** exakt nachgezogen werden kann. Gesetzt 2026-06-26, Installer 2026-06-27 (Frank).
>
> **Das Ziel in einem Satz:** OpenCode-CLI frisch installieren → `install`-Skript laufen lassen →
> Voraussetzungen (Keys/Tunnel) setzen → fertig, identische Umgebung wie auf dem Hauptrechner.

---

## Was OpenCode beim Start einliest (und was das Setup mitbringt)

| Ebene | Datei (Betrieb) | Im Repo? | Spiegelung hier | Vom Installer kopiert? |
|-------|-----------------|----------|-----------------|------------------------|
| 1. Globale Regeln | `~/.config/opencode/AGENTS.md` | nein (lokal) | **`AGENTS-global.md`**; unter Windows nach erfolgreicher Profilmigration ein kleiner Bootstrap | ja |
| 2. Globale Config | `~/.config/opencode/opencode.jsonc` | nein (lokal) | **`opencode.jsonc`** | ja (shell angepasst) |
| 2b. Globale Agents | `~/.config/opencode/agents/*.md` | nein (lokal) | **`agents/`** (z.B. `researcher.md`) | ja |
| 2c. Globale Plugins | `~/.config/opencode/plugins/*.{js,mjs}` + Plugin-Pakete | nein (lokal) | **`plugins/`** (z.B. `tool-first-guard.js`, `token-cost-sidebar/`) | ja |
| 2c1. TUI-Plugin-Liste | `~/.config/opencode/tui.json` | nein (lokal) | **`tui.json`** | ja |
| 2c2. Globale Skills | `~/.config/opencode/skill/<name>/SKILL.md` | nein (lokal) | **`skill/`** (z.B. `session-opencode`) | ja |
| 2d. Notifier-Sounds | `~/.config/opencode/sounds/*.wav` | nein (lokal) | **`sounds/`** (complete/error/permission) | ja |
| 2e. Notifier-Config | `~/.config/opencode/opencode-notifier.json` | nein (lokal) | — (Installer **generiert** sie mit lokalen Pfaden) | ja (erzeugt) |
| 2f. Windows-Fix-Binary | `~/.local/share/opencode-mousefix/versions/*/opencode.exe` | nein (lokal) | Patch + Buildskript | ja (gebaut) |
| 2g. OpenCode Launcher | `~/proggs/OpenCodeLauncher/bin/Release/.../OpenCodeLauncher.exe` | **ja** (Quellcode) | `OpenCodeLauncher/` | ja (gebaut + Shortcut) |
| 3. Projektbasis | `~/proggs/AGENTS.md` | **ja** | (liegt schon im Repo) | — |
| 3b. OpenCode-Profile | `%APPDATA%\OpenCodeLauncher\profiles\opencode\<profil>\` | nein (lokal) | Standardvorlagen im `OpenCodeLauncher` | beim ersten Profilzugriff erzeugt |
| 4. Projekt-CLAUDE.md | `~/proggs/CLAUDE.md` | **ja** | (liegt schon im Repo) | — |

**OpenCode-Launcher unter Windows:** Die globale und die Projekt-`AGENTS.md` bleiben klein. Der Launcher
erzeugt für Minimal, Standard und Strikt getrennte Quelldateien und lädt beim Start unveränderliche
Sitzungssnapshots über `OPENCODE_CONFIG`. Das bestehende Standardprofil wird bei der ersten Migration
verlustfrei übernommen; erst danach ersetzt der Launcher die globale Datei durch den Bootstrap. Claude
Code ist davon nicht betroffen.

**Nicht in einer Profildatei (kommt zentral vom Server):** Das Standardprofil weist OpenCode an, zu
Session-Beginn alle Arbeitsregeln aus dem **zweiten Gehirn** zu laden (`second-brain`-MCP, Kategorie
`Programmierung/Rules`, per Nummer iteriert). Diese Regeln liegen also zentral — auf jedem Rechner
identisch, ohne Kopieren. Dafuer muss der WireGuard-Tunnel stehen (siehe Voraussetzungen).

> **NICHT versioniert** (von OpenCode/Bun beziehungsweise dem Updater selbst erzeugt und nur lokal):
> `node_modules/`, `package.json`, `package-lock.json`, `bun.lock`, `opencode-notifier-state.json`,
> `opencode-notifier-update-state.json`, `opencode-notifier-update.log` und der kurzlebige Update-Lock.
> Diese werden nicht gespiegelt; Abhängigkeiten pflegt der Installer beziehungsweise Auto-Updater lokal.

---

## Schnellstart: identische Umgebung auf einem neuen Rechner

### Schritt 1 — OpenCode-CLI installieren (frische Installation)

Das macht der Installer NICHT (Paketmanager/interaktiv) — einmal von Hand:

**macOS:**
```bash
brew install anomalyco/tap/opencode     # empfohlen (immer aktuell)
# oder universell:  curl -fsSL https://opencode.ai/install | bash
opencode --version                      # verifizieren
```

**Windows:**
```powershell
scoop install opencode                  # empfohlen (setzt PATH automatisch)
# oder:  choco install opencode
opencode --version
```
Für den vollständigen Windows-Fix braucht der automatische Build zusätzlich **Git, Bun, npm/Node.js,
PowerShell 7 und das .NET-8-SDK**. Der Installer bricht einen Windows-Fix-Build sicher ab, wenn eine dieser
Voraussetzungen fehlt; eine bereits aktive funktionierende Version bleibt dabei erhalten.
> Offizielle Empfehlung fuer Windows ist eigentlich **WSL** (bester Support). Nativ geht auch — dann
> bleibt `"shell": "pwsh"` in der Config richtig. Details: `best-practices/opencode/grundlagen-installation.md`.

### Schritt 2 — Repo klonen (falls noch nicht da)
```sh
git clone https://github.com/Pepsi1978/proggs ~/proggs
```
Damit sind Projektbasis (`AGENTS.md`) + `CLAUDE.md` (Ebene 3+4) automatisch da.

### Schritt 3 — Das Installer-Skript ausfuehren (DER eine Befehl)

Kopiert Config, Regeln, Agents, Plugins und Sounds an ihren Platz, erzeugt die Notifier-Config mit lokalen
Pfaden und prüft am Ende die Voraussetzungen. Unter Windows baut derselbe Befehl außerdem den aktuellen
geprüften `windowsfix`, den Launcher und die Desktop-Verknüpfung:

**macOS / Linux:**
```bash
bash ~/proggs/opencode-setup/install.sh
```

**Windows:**
```powershell
pwsh ~/proggs/opencode-setup/install.ps1
```

Vorhandene Dateien werden vorher nach `~/.config/opencode/.backup-<zeit>/` gesichert (idempotent,
beliebig oft wiederholbar — auch zum Aktualisieren nach einem `git pull`).

### Schritt 4 — Voraussetzungen setzen (was der Installer NUR meldet, nicht selbst kann)

Der Installer zeigt am Ende eine `OK`/`FEHLT`-Liste. Diese Dinge sind manuell zu erledigen:

- **SK-Keys** (Secrets liegen NIE im Repo, siehe Regel `secrets-in-sk-folder`) — fuer die Recherche-Pipeline:
  - `~/SK/OpenCode/firecrawl-api-key.txt` (Engine A)
  - `~/SK/OpenCode/go-api-key.txt` (OpenCode-Go / MiniMax)
  - `~/SK/ClaudeCodeOpenRouter/openrouter.key` (Engine B)
- **`OPENROUTER_API_KEY`** als User-Umgebungsvariable (fuer den Owl/OpenRouter-Provider).
- **WireGuard-Tunnel aktiv** — der `second-brain`-MCP laeuft auf `http://10.8.0.1:8001/mcp` und ist
  NUR ueber den Tunnel erreichbar. Ohne ihn werden die Gehirn-Regeln nicht geladen. Einrichtung:
  Almanach `bugs/server/wireguard.md`.
- **`opencode auth login`** (bzw. `/connect` in der TUI) — fuer das Go-Abo (opencode-go/MiniMax + Plan).

### Schritt 5 — Start & Selbst-Check
Unter Windows über die erzeugte Desktop-Verknüpfung **OpenCode Launcher** starten. Ein nacktes `opencode`
im PATH ist nur der ungepatchte offizielle Fallback. Unter macOS/Linux weiterhin `cd ~/proggs && opencode`.

Beim ersten Prompt MUSS OpenCode melden: **"N Regeln aus dem zweiten Gehirn eingelesen."** — dann ist
die Umgebung komplett. Die npm-Plugins (`@mohak34/opencode-notifier`, `@plannotator/opencode`)
installiert OpenCode beim Start selbst aus der `plugin`-Liste.

Die rechte TUI-Seitenleiste muss danach auf beiden Windows-Rechnern dieselben Funktionen zeigen:

- Session-Datum und Uhrzeit
- Freimodus, Schnellmodus, Normalmodus und Gründlichkeitsmodus
- Modellname, Tokenmengen, Eingabe-/Ausgabepreise und Sitzungskosten
- Theme-Auswahl sowie Dunkel-/Hell-Umschalter
- OpenCode-Version mit `-windowsfix.<revision>`

Der Windows-Build erhält zusätzlich Linksauswahl-Copy, Rechtsklick-Paste, Mausrad, anklickbare Dialoge,
automatische Full-Repaint-Recovery, prozesslokales `--variant` sowie eine direkte TUI-Variantensteuerung
für den klickbaren, modellabhängigen Varianten-Umschalter einschließlich None, Minimal, Low,
Medium, High, XHigh, Max sowie providerspezifischen Varianten und eine vollständige
Plugin-Verwaltung: Die Plugin-Seite zeigt TUI- und Runtime-Hook-Plugins gemeinsam an und kann beide Typen
dauerhaft aktivieren oder deaktivieren. Runtime-Schalter werden kompatibel in `tui.json` gespeichert;
OpenCode lädt die Server-Plugins danach automatisch neu.

## Automatische OpenCode-Updates unter Windows

Der Launcher prüft höchstens einmal täglich im Hintergrund die stabile npm-Version von `opencode-ai`.
Ist eine neuere Version vorhanden, wird sie nicht ungeprüft über das funktionierende Binary geschrieben:

1. Exakten Git-Tag klonen und den Windows-Patch prüfen.
2. Maus-, Variant-, Full-Repaint- und Runtime-Plugin-Fähigkeiten sicherstellen.
3. Vier Typechecks, fokussierte Regressionstests, Binary-Build und `--version`-Smoke-Test ausführen.
4. Das Binary in einen unveränderlichen Versionsordner unter
   `~/.local/share/opencode-mousefix/versions/` installieren.
5. Erst danach `current.json` atomar auf die neue grüne Version umschalten.

Laufende Sitzungen behalten ihr altes Binary; neue Sitzungen verwenden automatisch den neuen Pointer.
Scheitert Patch, Test, Build, Netzwerk oder Git-Tag, bleibt `current.json` bytegenau auf der letzten grünen
Version. Der Launcher fällt bei einem beschädigten Pointer auf `previous`, dann auf das alte stabile Binary
und zuletzt auf das offizielle `opencode` im PATH zurück. Dadurch gehen Sidebar, Arbeitsmodi, Preise,
Themes, Mausfix, Rendererfix und Reasoning-Stufe bei Updates nicht still verloren.

---

## Was der Installer Schritt fuer Schritt tut (Transparenz)

1. Prueft, ob `opencode` im PATH ist (nur Hinweis, kein Abbruch).
2. Legt `~/.config/opencode/{agents,plugins,sounds}` an und sichert vorhandene Dateien nach `.backup-<zeit>/`.
3. Kopiert `opencode.jsonc` — auf **macOS/Linux** wird `"shell": "pwsh"` → `"bash"` ersetzt; auf **Windows** bleibt `pwsh`; kopiert außerdem `tui.json` fuer TUI-Plugins.
4. Legt `AGENTS-global.md` nur auf frischen Installationen als `AGENTS.md` an. Eine vorhandene lokale Datei bleibt erhalten, damit der Windows-Launcher genau diesen Ist-Zustand beim ersten Profilzugriff verlustfrei migriert und erst danach den Bootstrap setzt. Außerdem werden Agents, Plugins, TUI-Plugin-Dependencies, Sounds und Skills installiert.
5. **Erzeugt** `opencode-notifier.json` neu mit den korrekten lokalen Sound-Pfaden (Windows BOM-frei).
   Nur Abschluss nach echter Arbeit und eine echte KI-Rückfrage melden sich auch dann, wenn OpenCode
   gerade fokussiert ist. Alle anderen Ereignisse und der Dirty-Worktree-Watchdog bleiben stumm.
   Anschließend wird npm sofort und danach bei OpenCode-Starts höchstens einmal täglich auf eine neue
   Notifier-Version geprüft. Ein Kandidat wird isoliert installiert und nur übernommen, wenn der echte
   Vertragstest weiterhin exakt `question` und `complete` sowie keine Zwischenalarme bestätigt.
6. Unter Windows: neueste stabile OpenCode-Version prüfen, vollständig testen, versioniert installieren und
   erst danach atomar aktivieren; Launcher `Release` bauen und Desktop-Verknüpfung erzeugen.
7. Voraussetzungs-Check (SK-Keys, `OPENROUTER_API_KEY`, WireGuard `10.8.0.1`) + TODO-Liste.

## Lokale Plugins (Detail)

`plugins/*.js` werden vom Installer kopiert und beim Start automatisch geladen (kein Eintrag in
`opencode.jsonc` noetig). Jeder dieser Entry-Points exportiert exakt eine Plugin-Funktion; testbare
Helper liegen unter `plugins/lib/`, weil OpenCode sonst jeden Helper als zusaetzliches Plugin ausfuehrt.
Aktuell:
- **`notifier-completion-guard.js`** — lädt den Notifier genau einmal und lässt einen Abschlussalarm nur
  nach einem Busy-Zyklus mit echter Schreib-/Build-Arbeit durch. Nur das `question`-Tool bleibt sofort
  hörbar; Eingaben, Freigaben, Fehler, Planwechsel, interne Unteraufgaben und Abbrüche bleiben stumm.
- **`notifier-auto-updater.mjs`** — prüft mit 24-Stunden-Cooldown und Prozess-Lock die npm-Version,
  installiert Updates zunächst in ein Temp-Verzeichnis, führt dort `notifier-candidate-contract.mjs`
  gegen den echten Kandidaten aus und promoted erst danach. Fehler werden nur protokolliert; die letzte
  funktionierende Version bleibt aktiv und wird nach einer Stunde erneut geprüft.
- **`tool-first-guard.js`** — setzt die Anti-Halluzinations-Regel "Tool-first, nicht Memory-first" im
  Code durch: warnt (Log), wenn eine bestehende Datei mit `edit`/`patch` geaendert wird, ohne sie
  vorher mit `read` gelesen zu haben. Mit `OPENCODE_TOOL_FIRST_ENFORCE=1` blockt es hart statt zu
  warnen ("Laws"-Ebene). Hintergrund: `best-practices/agents/anti-halluzination-regeln.md` §1+§7.
- **`windows/terminal-task-title.js`** — Windows-spezifische Quelle fuer intelligente Drei-Wort-
  Terminaltitel. Der Installer legt sie als Top-Level-Plugin ab und entfernt die alte `.ts`-Fassung;
  in headless `opencode run`-Aufrufen bleibt sie deaktiviert.
- **`token-cost-sidebar/`** — lokales TUI-Plugin fuer die rechte Seitenleiste. Zeigt aktuelles Modell,
  Input-, Output- und Reasoning-Tokens, drei Modellpreise sowie die Gesamtkosten in US-Dollar. Cache-R/W-
  Tokens und Kosten-Einzelposten bleiben in der kompakten Anzeige verborgen. Ein mit der Maus
  bedienbares Dropdown zeigt beim Navigieren per Maus oder Pfeiltasten sofort eine Theme-Vorschau und
  merkt sich die bestaetigte Auswahl; beim Abbrechen kehrt es zum vorherigen Theme zurueck.
  Direkt darunter schaltet eine kompakte Dunkel/Hell-Auswahl die Variante des gewaehlten Themes um.
  Das Plugin bevorzugt die von
  OpenCode gespeicherte echte `cost`; bei fehlenden oder
  veralteten Nullpreisen laedt es den aktuellen Modellpreis von models.dev und beruecksichtigt
  kontextabhaengige Preisstufen. Bei Fast-Aliasnamen gilt der Priority-Tarif nur, wenn der Provider
  den Modellschritt tatsächlich als `priority` bestätigt; `default` fällt auf den Standardtarif zurück.
  Geladen wird es ueber `tui.json` (`./plugins/token-cost-sidebar`).
  Vor einer Einzelinstallation oder Aktualisierung muss
  **`plugins/token-cost-sidebar/README.md` vollständig gelesen werden**, weil Sidebar,
  `work-mode.js`, `tui.json` und npm-Abhängigkeiten nur gemeinsam die vollständige Funktion liefern.
- **`token-usage-audit.js`** — schreibt jeden abgeschlossenen Modellschritt append-only nach
  `opencode-setup/Tokenverbrauch.jsonl`. Das JSONL enthält Datum, Session-/Message-/Part-ID,
  Launcher-Modell und Service-Tier, normalisierte Tokenwerte, Provider-Meldestatus und rohe
  Usage-Metadaten. Zusätzlich klassifiziert es Cold Starts, Subagenten, Compress, Modell-/Prefix-
  Wechsel, Tool-Fortsetzungen und Partial Hits und schreibt getrennte Read-/Write-Zusammenfassungen.
  Benutzertexte werden gekürzt und Secrets redigiert; Toolausgaben werden nicht kopiert. Die Datei
  bleibt lokal und ist bewusst von Git ausgeschlossen. Interne Title-Anfragen beeinflussen weder
  Datensätze noch Sequenz- oder Änderungsursachen.
- **`openai-fast-service-tier.js`** — erzwingt fuer die vier Launcher-Fast-Aliase `serviceTier:
  "priority"` im letzten `chat.params`-Hook. Damit bleibt Fast auch nach Konfigurationsmigrationen
  aktiv; das Audit trennt die Request-Option vom rohen Response-Tier. Bei ChatGPT-OAuth ist ein
  Response-Wert `default` laut OpenAI kein verlaesslicher Gegenbeweis fuer serverseitiges Fast-Routing.

## Manueller Weg (Fallback, falls das Skript nicht passt)
```sh
mkdir -p ~/.config/opencode/agents ~/.config/opencode/plugins ~/.config/opencode/sounds
cp ~/proggs/opencode-setup/opencode.jsonc   ~/.config/opencode/opencode.jsonc   # macOS: shell auf "bash" aendern
cp ~/proggs/opencode-setup/tui.json          ~/.config/opencode/tui.json
cp ~/proggs/opencode-setup/AGENTS-global.md ~/.config/opencode/AGENTS.md
cp ~/proggs/opencode-setup/agents/*.md      ~/.config/opencode/agents/
cp ~/proggs/opencode-setup/plugins/*.js     ~/.config/opencode/plugins/
cp -R ~/proggs/opencode-setup/plugins/lib   ~/.config/opencode/plugins/
cp -R ~/proggs/opencode-setup/plugins/token-cost-sidebar ~/.config/opencode/plugins/
# Windows zusätzlich: plugins/windows/terminal-task-title.js als plugins/terminal-task-title.js kopieren.
npm --prefix ~/.config/opencode install @opencode-ai/plugin@1.17.15 @opentui/core@0.4.3 @opentui/solid@0.4.3 solid-js@1.9.12 @mohak34/opencode-notifier@0.2.8
cp ~/proggs/opencode-setup/sounds/*.wav     ~/.config/opencode/sounds/
# opencode-notifier.json mit lokalen Pfaden von Hand anlegen (siehe install-Skript als Vorlage)
```

---

## Pflege (wichtig — sonst laeuft es auseinander)

- Aenderst du `~/.config/opencode/` (Config, AGENTS.md, agents, plugins, sounds), **spiegle die
  Aenderung sofort hierher** (`opencode-setup/`) und committe — sonst hat der andere Rechner den
  alten Stand. (Gleiche Idee wie `claude-code-setup/` fuer Claude Code, nur fuer OpenCode.)
- Die eigentlichen **Arbeitsregeln** aenderst du NICHT hier, sondern zentral im **zweiten Gehirn**
  (Kategorie `Programmierung/Rules`) — von dort holt sie jeder Rechner automatisch.
- Nach Änderungen an Config/Plugin/Launcher auf dem zweiten Rechner zuerst `git pull`, danach den
  Windows-Installer erneut ausführen. Der Vorgang ist idempotent und sichert bestehende Config-Dateien.
- `opencode.jsonc` enthaelt **keine** Klartext-Secrets (nur `{file:}`/`{env:}`-Referenzen) — daher
  unbedenklich im Repo.
- `rules-opencode/` sind kompakte Regel-Fassungen fuer das Gehirn (Token-Sparen) — sie werden NICHT
  nach `~/.config/opencode/` kopiert, sondern dienen dem Zurueckspeichern in `Programmierung/Rules`.
