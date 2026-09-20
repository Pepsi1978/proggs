# Portierung UpdateZentrale (Windows) → Updater-Zentrale macOS

Stand: 20.09.2026

Die Windows-App `~/proggs/UpdateZentrale` (WPF, .NET 8) ist hier als eigenständige native
macOS-Anwendung nachgebaut: `~/proggs/UpdaterZentrale-macOS` (Swift, AppKit, ohne Xcode-Projekt).
Die Windows-Fassung bleibt unverändert.

**Weg B (native Mac-Version) statt Weg A (gemeinsamer Code-Stand)**, weil WPF auf macOS nicht
läuft. Muster wie bei `OpenLauncher` / `OpenLauncherMac`: `build.sh` ruft `swiftc` direkt auf, der
Build-Zeitstempel wird dort gesetzt, signiert wird zuletzt mit dem Zertifikat „Frank Local Dev".

---

## A) Was nachweislich funktioniert

Geprüft am 20.09.2026 auf diesem Mac (macOS 27, Apple Silicon, 1280 × 832 Punkte):

| Prüfung | Ergebnis |
|---|---|
| Build mit `build.sh` | ✅ grün, echt signiert |
| App startet, Fenster erscheint | ✅ |
| Erste Prüfung läuft automatisch durch alle 10 Karten | ✅ |
| Versionserkennung | ✅ alle 10 Einträge korrekt, siehe Tabelle unten |
| Protokoll `updates-JJJJ-MM-TT.log` wird geschrieben | ✅ |
| Fenstergröße passt auf den Bildschirm | ✅ 1180 × 709 |
| Installation nach `/Applications` + Schreibtisch-Verknüpfung | ✅ |

Ergebnis des ersten Laufs — jede Zeile ist eine echte Erkennung, keine Annahme:

| Programm | Installiert | Verfügbar | Zustand |
|---|---|---|---|
| LM Studio | 0.4.21+2 | 0.4.25 (Cask `0.4.25,1`) | Update verfügbar |
| LM Studio Engines & Runtimes | – | – | Aktuell |
| Claude Desktop | 2.2553.1 | 2.2553.1 (Cask `2.2553.1,c38127e2…`) | Aktuell |
| Codex Desktop (ChatGPT.app) | 26.915.31945 | 26.915.31945 | Aktuell |
| Claude Code CLI | 2.1.278 | 2.1.278 | Aktuell |
| Codex CLI | 0.155.0 | 0.155.1 | Update verfügbar |
| Stream Deck | – | – | Nicht installiert |
| OpenLauncher | 1.24.28 | 1.24.29 | Update verfügbar |
| TVO | 1.47 | 1.51 | Update verfügbar |
| CVO | 1.45 | 1.49 | Update verfügbar |

---

## B) Paritätsliste — Windows-Funktion → macOS-Umsetzung

| Windows | macOS | Status |
|---|---|---|
| **Katalog** `programs.json`, zur Laufzeit gelesen | gleiches Schema, eigene Datei im Projektordner | ✅ 1:1 |
| Neues Programm ohne Neubau, „Neu laden" | identisch | ✅ 1:1 |
| „Katalog öffnen" | öffnet `programs.json` im Standardeditor | ✅ 1:1 |
| **Prüfen** einzeln / „Alle prüfen" (nacheinander) | identisch | ✅ 1:1 |
| Erste Prüfung automatisch nach dem Start | identisch | ✅ 1:1 |
| **Aktualisieren** einzeln / „Alle installieren" mit Rückfrage | identisch | ✅ 1:1 |
| Update-Schaltfläche nur bei echtem Update betont, sonst „Aktuell" und abgeschaltet | identisch | ✅ 1:1 |
| **Fingerabdruck** vor/nach dem Lauf, fünf Ergebnisse | identisch, inkl. „Nicht verifiziert" | ✅ 1:1 |
| **Ausstehend** + rückwirkende Bestätigung + 7-Tage-Frist | identisch | ✅ 1:1 |
| „Jetzt starten und übernehmen" | identisch | ✅ 1:1 |
| **Protokolle** `updates-JJJJ-MM-TT.log` + `verlauf.jsonl` | identisch, JSON-Feldnamen gleich | ✅ 1:1 |
| Kopf/Fuß je Lauf mit Befehl und Stand vorher/nachher | identisch (Zeile „Rechte" sagt jetzt „Standardbenutzer (macOS …)") | ✅ |
| „Protokolle"-Schaltfläche, „Protokoll öffnen" auf der Karte | identisch | ✅ 1:1 |
| **Laufende Programme** pfadgenau erkennen (inkl. Helfer) | `NSWorkspace` für Apps + `pgrep`/`proc_pidpath` für CLI-Werkzeuge; Pfadvergleich bleibt | ✅ 1:1 |
| Vor dem Update beenden, danach neu starten (nach Rückfrage) | identisch (`terminate()` → 2,5 s → `forceTerminate()`) | ✅ 1:1 |
| „Starten" | `NSWorkspace.openApplication` bzw. direkter Start | ✅ 1:1 |
| **Autostart-Kennzeichen** | liest `~/Library/LaunchAgents/*.plist` **und** die Anmeldeobjekte | ✅ 1:1 (nur lesend, wie unter Windows) |
| **Hell / Dunkel**, gemerkt, Titelleiste zieht mit | identisch; `NSAppearance` statt `DwmSetWindowAttribute` | ✅ 1:1 |
| Alle Farbwerte aus `Dunkel.xaml` / `Hell.xaml` | unverändert übernommen | ✅ 1:1 |
| **Protokollbereich** rechts oben | identisch | ✅ 1:1 |
| **Terminal** rechts unten | `zsh -lc` statt PowerShell | ✅ sinngemäß |
| „Fenster" öffnet ein echtes Terminal | iTerm, sonst Terminal.app | ✅ 1:1 |
| **Einzelstart** | Bundle-Kennung statt Mutex; bestehendes Fenster nach vorn | ✅ 1:1 |
| Einstellungen außerhalb des Repos | `~/Library/Application Support/UpdaterZentrale/settings.json` | ✅ 1:1 |
| **Desktop-Verknüpfung** (`create_shortcut.ps1`) | `tools/install-desktop-icon.sh` legt einen Finder-Alias an | ✅ 1:1 |
| **App-Symbol** (`make_icon.ps1` → `app.ico`) | `tools/make-icon.sh` → `AppIcon.icns`, gleiches Motiv | ✅ 1:1 |

### Update-Arten

| Windows `art` | macOS `art` | Erläuterung |
|---|---|---|
| `winget` | `brew` | Cask statt winget-Paket |
| `store` (MSIX über msstore) | `brew` | macOS hat kein Gegenstück; dieselbe App kommt hier als normales Programm |
| `cli` | `cli` | unverändert |
| `reposkript` | `reposkript` | `bash` statt `pwsh`, Mac-Skripte |

---

## C) Nicht portiert — mit Begründung

### 1. Der gesamte Administrator-Komplex

Betrifft: „Als Administrator neu starten", die Plakette „Administrator", den Schiebeschalter
„Immer als Administrator starten", den Schalter „Als Administrator bei jedem Start" auf jeder
Karte, das Band „Kein Administratorstart" für Paket-Apps, die Warnung zu Windows-Fehler 740 und
`Services/Rechte.cs`.

**Grund:** macOS kennt kein Gegenstück. Es gibt weder den Kompatibilitätsschalter RUNASADMIN noch
die Benutzerkontensteuerung; ein Programm läuft mit den Rechten dessen, der es startet. Homebrew
**verweigert** den Betrieb als root sogar ausdrücklich. Ein Schalter, der nichts bewirkt, wäre
eine Lüge — deshalb ist er gar nicht erst da, statt abgeschaltet herumzustehen.

Das Feld `Erhoeht` bleibt in `verlauf.jsonl` erhalten (immer `false`), damit das Dateiformat
zwischen beiden Plattformen gleich bleibt.

### 2. Autostart auf eine geplante Aufgabe umstellen

Betrifft: `Services/Aufgabenplanung.cs`, das Warnband „Dieses Programm steht im Autostart …" und
die Sicherung des Run-Eintrags in `settings.json`.

**Grund:** Das löst ein Windows-Problem, das es hier nicht gibt — Windows überspringt im
`HKCU\…\Run`-Autostart still alle Programme, die Adminrechte anfordern. macOS hat diese Regel
nicht: ein LaunchAgent startet, was in ihm steht. Das Autostart-**Kennzeichen** bleibt (lesend).

### 3. Stream Deck

Der Katalog-Eintrag ist **vorhanden** (Cask `elgato-stream-deck`), die App ist auf diesem Mac aber
nicht installiert. Die Karte zeigt deshalb „Nicht installiert" — bewusst so, statt den Eintrag
wegzulassen: sobald Stream Deck installiert wird, funktioniert die Karte ohne Änderung.

### 4. Microsoft Store als eigene Update-Art

Codex Desktop läuft unter Windows als Store-Paket (`9PLM9XGG6VKS`). Auf dem Mac ist dasselbe
Programm die **ChatGPT.app** (Bundle-Kennung `com.openai.codex`) und kommt über ein normales
Cask. Der Eintrag bleibt, die Art wechselt von `store` auf `brew`.

---

## D) Bewusste Abweichungen

### 1. Downgrade-Schutz beim brew-Anbieter

winget meldet für ein Paket nur dann etwas, wenn wirklich ein Upgrade ansteht. Ein Cask ist
dagegen eine **unabhängige Rezeptdatei** und kann dem eingebauten Updater einer App
hinterherlaufen. Ein reiner Ungleich-Vergleich (wie in `WingetAktualisierer.cs`) hätte hier ein
**Downgrade als Update angeboten**.

Deshalb wird numerisch verglichen: nur eine echt größere Version ist ein Update. Ist der Cask
älter als das Installierte, steht „Auf dem neuesten Stand – neuer als das Homebrew-Rezept".

### 2. Cask-Versionen tragen ein Komma-Anhängsel

Homebrew hängt hinter ein Komma einen zweiten Bestandteil: bei Claude
`2.2553.1,c38127e27202ddc1c8c187102f7798a93b1b8ede`, bei LM Studio `0.4.25,1`. Die Info.plist der
App kennt diesen Teil nicht. Ohne Abschneiden wäre **jede** dieser Apps dauerhaft falsch als
veraltet markiert. `Versionen.caskBereinigen` erledigt das.

### 3. Installierte Version kommt aus der Info.plist, nicht von brew

Keine der verwalteten Apps wurde über brew installiert (`installed` ist bei allen `null`) — sie
bringen ihre eigenen Updater mit. Würde `brew list` befragt, stünde bei jeder Karte „nicht
installiert", obwohl die App da ist.

### 4. `rebuild-overlay.sh` hat auf dem Mac keine eigene Rückfrage

Die Windows-Fassung `rebuild-overlay.ps1` zeigt ein Ja/Nein-Fenster und gibt
`OVERLAY_UPDATE_STATUS=` aus. **Die Mac-Fassung tut beides nicht** — sie beendet die Overlays und
baut sofort (nachgeprüft am 20.09.2026: weder Dialog noch Statuszeile im Skript).

Das Skript bleibt unangetastet, es gehört einem anderen Projekt. Stattdessen stellt die
Updater-Zentrale die Rückfrage bei Einträgen mit leerem `statusPraefix` **selbst**, bevor das
Skript startet, und liest das Ergebnis aus Exit-Code und Schlusszeile („Fertig — alle Ziele …").

> **Hinweis:** Regel 17 der globalen CLAUDE.md beschreibt für macOS ein Ja/Nein-Fenster und
> `OVERLAY_UPDATE_STATUS`-Rückmeldungen, die das Mac-Skript nicht hat. Entweder die Regel oder das
> Skript sollte angeglichen werden.

### 5. Fenstergröße nach Mac-Verhältnissen

Windows startet mit 1340 × 880 bei Mindestgröße 1060 × 620. Dieses MacBook hat 1280 × 832 Punkte —
das Fenster ragte links aus dem Bild. Die Mac-Fassung richtet sich nach dem nutzbaren Bereich
(hier 1180 × 709), Mindestgröße 880 × 540.

### 6. `DEVELOPER_DIR` wird für Kindprozesse gesetzt

`update-launcher.sh` und `rebuild-overlay.sh` bauen mit `swiftc`. Zeigt `xcode-select -p` auf
Xcode.app und ist dessen Lizenz nicht angenommen, bricht jeder dieser Builds ab — und der Launcher
bliebe nach dem Beenden ungebaut zurück. Deshalb setzt `Kommandozeile.umgebung`
`DEVELOPER_DIR=/Library/Developer/CommandLineTools`. Dauerhaft lösbar nur vom Benutzer:
`sudo xcodebuild -license accept`.

---

## E) Auf dem Mac noch von Hand zu prüfen

Nicht automatisch prüfbar, weil ein echter Installationslauf Programme beendet und Daten verändert:

1. **LM Studio aktualisieren** → Rückfrage „Programm beenden?" erscheint, LM Studio wird beendet,
   `brew install --cask --force lm-studio` läuft durch, LM Studio startet wieder, die Karte zeigt
   danach „Aktuell" mit 0.4.25 und ein grünes „Verifiziert: 0.4.21+2 → 0.4.25".
2. **Codex CLI aktualisieren** → läuft ohne Beenden durch, danach 0.155.1.
3. **OpenLauncher aktualisieren** → das Skript zeigt sein eigenes Ja/Nein-Fenster; bei „Nein"
   muss die Karte „Abgebrochen" zeigen, bei „Ja" „Neue Version gebaut und gestartet".
4. **TVO oder CVO aktualisieren** → hier fragt die Updater-Zentrale selbst; bei „Nein" darf das
   Skript gar nicht erst starten.
5. **Hell-Modus** oben rechts umschalten → alle Flächen, Karten und die Fenstertitelleiste ziehen
   mit; nach einem Neustart der App ist die Wahl noch gesetzt.
6. **Terminal** rechts unten: `brew --version` eingeben → Ausgabe erscheint (beweist, dass der
   PATH stimmt). „Fenster" öffnet iTerm im Repo-Ordner.
7. **Einzelstart**: die App ein zweites Mal starten → das vorhandene Fenster kommt nach vorn.
8. **Schreibtisch-Verknüpfung** doppelklicken → die App startet mit ihrem Symbol.
