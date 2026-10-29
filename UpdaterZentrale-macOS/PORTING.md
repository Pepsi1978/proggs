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
| **Protokolle** `updates-JJJJ-MM-TT.log` + `verlauf.jsonl` | gleiche Dateien, gleiche JSON-Feldnamen; `Ergebnis` steht als Text statt als Zahl, `Zeit` als ISO 8601 | ✅ |
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
| **Desktop-Verknüpfung** (`create_shortcut.ps1`) | `tools/install-desktop-icon.sh` legt einen Finder-Alias an, mit eigenem Symbol statt Verknüpfungspfeil | ✅ 1:1 |
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

### 6. Die Schreibtisch-Verknüpfung trägt kein Pfeil-Abzeichen

Der Finder malt den Verknüpfungspfeil nur dann auf, wenn er das Symbol selbst aus dem Ziel
ableitet. Trägt die Datei ein **eigenes** Symbol (Finder-Merkmal `kHasCustomIcon`), zeigt er
dieses unverändert — ohne Abzeichen. `tools/install-desktop-icon.sh` setzt es deshalb mit
`sips`/`DeRez`/`Rez`/`SetFile`.

Nicht mit `NSWorkspace.setIcon`: das meldet bei einer Alias-Datei zwar Erfolg, setzt
`kHasCustomIcon` aber nicht (nachgeprüft am 20.09.2026 — das Flag blieb `0x8000` statt `0x8400`,
und die Ressourcen-Gabel enthielt 286 statt 435 954 Bytes). Die Alias-Daten überleben das
Anhängen der Symbol-Ressource; der Alias zeigt danach weiterhin auf `/Applications`.

### 7. `DEVELOPER_DIR` wird für Kindprozesse gesetzt

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
5. **Hell-Modus** oben rechts umschalten (am 20.09.2026 NICHT visuell geprüft – der Bildschirm
   war gesperrt, die Farbwerte sind aber 1:1 aus `Hell.xaml` übernommen) → alle Flächen, Karten und die Fenstertitelleiste ziehen
   mit; nach einem Neustart der App ist die Wahl noch gesetzt.
6. **Terminal** rechts unten: `brew --version` eingeben → Ausgabe erscheint (beweist, dass der
   PATH stimmt). „Fenster" öffnet iTerm im Repo-Ordner.
7. **Einzelstart**: die App ein zweites Mal starten → das vorhandene Fenster kommt nach vorn.
8. **Schreibtisch-Verknüpfung** doppelklicken → die App startet mit ihrem Symbol.
9. **Karte anklicken** – auf den Beschreibungstext, nicht auf eine Schaltfläche → der Rahmen wird
   violett, und rechts wechselt der Protokollbereich auf dieses Programm.
10. **Beim ersten Start** fragt macOS nach der Erlaubnis, „Systemereignisse" zu steuern (für das
    Auslesen der Anmeldeobjekte). Bei „Nicht erlauben" fehlt nur das Autostart-Kennzeichen aus den
    Anmeldeobjekten – die LaunchAgents werden weiterhin erkannt, alles andere bleibt unberührt.

---

## F) Windows-Gleichstand 01.10.2026 (Mac 1.0.4, Windows bis 1.3.16)

Übernommen aus den Windows-Commits seit dem 20.09.2026:

| Windows | macOS |
|---|---|
| `exePfadAlternativen`, `ausblendenWennFehlt` (Rechner-Fallbacks, Stream Deck ausblendbar) | gleiche Felder; Claude/Codex CLI mit Ausweichpfaden unter `/opt/homebrew/bin` und `/usr/local/bin`, Stream Deck ausgeblendet, wenn nicht installiert |
| npm-Prüfung direkt über `registry.npmjs.org` statt `npm view` | identisch (URLSession), kein node nötig |
| `versionsUrl` + Kimi Code CLI | identisch; Kimi unter `~/.kimi-code/bin/kimi`, Prüfung über `https://code.kimi.ai/kimi-code/latest` |
| Fehlerband wegklicken (`FehlerQuittiertBis`) | Schalter „Ausblenden“ im roten Band, gemerkt in `settings.json` |
| `git pull --ff-only` vor dem Repo-Skript, Hinterstand nur für Build-Quellen | identisch (Build-Quellen: `*.swift`, `*.plist`, `*.sh`, `*.entitlements`, `*.icns`); gescheiterte Git-Abfrage = „unbekannt“, nie „aktuell“ |
| „started“ bei unverändertem Build = bereits aktuell | identisch |
| Pipe-Vererbung: Lauf hängt nicht mehr am Kindprozess | nach Prozessende nur noch 2 s Nachlesen statt Warten aufs Pipe-Ende |
| Prozessbaum bei Zeitlimit beenden | Nachfahren über `pgrep -P`, erst SIGTERM, dann SIGKILL |
| Beenden vor Update prüft Erfolg | identisch; läuft danach noch etwas, startet kein Installer |
| `Laufkoordination` (ein Vorgang app-weit) | identisch; Karten- und Sammelschalter sowie „Neu laden“ sperren sich |
| `UpdateKette` (mehrstufig, verifiziert, nie derselbe Befehl doppelt) | identisch; neues Ergebnis „Bereits aktuell“ |
| Nachprüfung ausstehender Updates mit Ziel + echter Prüfung | identisch (`UpdateKette.stagedUrteil`) |
| Zentrales Diagnoseprotokoll + Export | `logs/diagnose-JJJJ-MM-TT.jsonl`, Schalter „Diagnose exportieren“ → maskiertes ZIP der letzten 14 Tage nach `~/Downloads` |
| Protokoll der Karte begrenzt (200 000 Zeichen) | identisch |

Bewusst nicht übernommen (nur Windows): Startmenü-Verknüpfung, PowerShell 7 als winget-Eintrag,
Selbst-Update einer Administrator-Instanz, Einzelstart-/Admin-Übergabe (`Einzelstart.cs`,
`Rechte.cs`), Fenster-Deckelung und schwarzer Rand beim Maximieren (WPF-spezifisch), das
xUnit-Testprojekt.

## G) Windows-Gleichstand 14.10.2026 (Mac 1.0.7, Windows bis 1.3.21)

Portiert:
* **1.3.17** – alle Karten werden gleichzeitig geprüft (TaskGroup, Kopfzeile „x von n fertig“), vorher
  ein gemeinsamer Repo-Abgleich (`Services/RepoAbgleich.swift`: fetch + `merge --ff-only @{u}`, 2 min
  gültig, auch von den Repo-Karten genutzt statt je eigenem fetch) und gemerkter Prüfstand
  (`Services/Pruefstand.swift`, 60 min; eigene Werkzeuge immer neu).
* **1.3.20** – Kimi Code CLI auch als npm-Installation (`/opt/homebrew/bin/kimi`).
* **1.3.21** – Programmierumgebung als Update-Karten; Katalogfelder `pruefExitCodes`, `pruefAbschnitt`,
  `pruefZeilenMuster`, `updateExePfad`; neu nur auf dem Mac: `formel` (Homebrew-Formeln statt winget) und
  Casks ohne `appPfad`. PowerShell 7 als Formel.

Nicht portiert:
* **Prüfung beim Windows-Start** (`--hintergrund`, geplante Aufgabe, Einzelstart-Signal) – auf dem Mac
  bräuchte das einen LaunchAgent; offen.
* **IPv4-zuerst** – betrifft den .NET-HttpClient; URLSession nutzt Happy Eyeballs.
* **`wingetName`**, winget-Neustart-Codes, Tabellen-Parsing von winget – winget-spezifisch.

## H) Windows-Gleichstand 14.10.2026 (Mac 1.0.8, Windows bis 1.3.25)

Portiert:
* **1.3.22** – verkürzte installierte Version gilt als die angebotene (Android Studio: Info.plist
  „2026.2“, Cask „2026.2.1.8“): `Versionen.istVerkuerzteForm`, in `BrewAktualisierer.pruefen` vor dem
  Zahlenvergleich. Zusätzlich nur auf dem Mac: hat brew das Cask selbst installiert und kennt die
  längere Fassung derselben Version, zählt die längere als installierter Stand und Fingerabdruck –
  so bleibt ein Patch-Update (…1.8 → …1.9) sichtbar und verifizierbar.
* **1.3.22** – Kimi-Update fällt auf npm zurück (`/bin/sh -c "kimi update --yes || npm install -g
  --allow-scripts=… @moonshot-ai/kimi-code@latest"`).
* **1.3.22** – `FrischeUmgebung`: ein ungültiges JAVA_HOME (JDK-Update hat den Ordner entfernt) wird
  je Kindprozess durch das Homebrew-JDK ersetzt oder entfernt (`Kommandozeile.umgebung`).

Nur auf dem Mac (Fehler aus dem Lauf vom 14.10.2026):
* **sudo ohne Terminal** – `dotnet-sdk` räumt alte Pakete per sudo weg; ohne Terminal scheiterte das.
  Die App legt `~/Library/Application Support/UpdaterZentrale/askpass.sh` an (osascript-Passwortdialog,
  5 Minuten Frist) und setzt `SUDO_ASKPASS`; Homebrew reicht die Variable durch und hängt `-A` an.
  `fehlergrundText` unterscheidet Abbruch, falsches Passwort und fehlenden Dialog.
* **npm 12 überspringt Installationsskripte** fremder Pakete (`allow-scripts`-Richtlinie). OpenCode
  braucht sein Postinstall (lädt die Programmdatei): `--allow-scripts=opencode-ai` im Katalog.

Nicht portiert:
* **1.3.22** `wingetUpdatePerInstall`, `wingetInstallerTyp`, „nur über Hersteller“ – winget-Verben;
  PowerShell ist hier eine Formel, Android Studio läuft ohnehin über `brew install --cask --force`.
* **1.3.22** Terminal-Startknopf – gibt es auf dem Mac bereits („Fenster“).
* **1.3.23** Backslashes im Windows-Katalog.
* **1.3.24 / 1.3.25** Übernahme-Knopf für Paket-Apps (Appx-Staging, Windows Terminal) – kein
  macOS-Gegenstück; iTerm2 wird beendet und direkt ersetzt.

---

## E) Windows-Gleichstand 1.3.40 (macOS 1.0.16, 24.10.2026)

Übernommen aus UpdateZentrale 1.3.26–1.3.40:

| Windows | macOS |
|---|---|
| Schutzfilter `cliRisiko`, ⚠ auf der Karte, Rückfrage beim Einzelklick | ✅ – eingeordnet nach Mac-Verhalten (iTerm2, LM Studio + Runtimes, JDK 17/21, Android SDK, VS Code, Windscribe) |
| Zwei unabhängige Sammelknöpfe (CLI-kritisch / nicht CLI-kritisch) mit Plan in der Rückfrage und Abhängigkeitshinweis | ✅ 1:1 |
| Sammelplan: Phasen, Selbst-Update zuerst, `nach`, kürzeste zuerst, `zuletzt`, `hauptversionNurPerKlick`, parallele Bahn (max. 3) | ✅ 1:1 |
| Fortsetzung nach dem Selbst-Update (`SammelFortsetzen`, 30 min) | ✅ 1:1 |
| Dauer je Karte (Median der letzten drei), „⏱ nach Dauer“, `DauerSekunden`/`ArbeitSekunden` im Verlauf, `sammellaeufe.jsonl` | ✅ 1:1 |
| Kopfzeile in Zeilen statt Spalten, Fußzeile mit vollem Status, Fortschritt und Laufdetails | ✅ – AppKit hat kein WrapPanel, die Bedienleiste steht fest auf zwei Zeilen |
| Selbst-Update über Prozessnummer, Helfer als Datei, Kopieren erst nach dem Ende | ✅ – `tools/selbstupdate-helfer.sh`, `ditto` in Zwischenordner und Tausch |
| OpenCode-Update per OpenLauncher-Sitzung (`opencodesitzung`) | ✅ – Mac-Fix-Build (`build-install-macos-tuifix.sh`), OpenLauncher.app `--jarvis-auftrag` |
| Neue Programme | ✅ fd, bat, fzf, delta, lazygit, ShellCheck, shfmt, Trivy, FFmpeg, Rclone, scrcpy, uv, Deno, Docker Desktop, WireGuard (wireguard-tools), Windscribe |
| `versionsMuster` / `versionsUrlMuster`, User-Agent für die GitHub-API | ✅ |
| Statuszeile nur am Zeilenanfang, letzte zählt; `failed`/`refused`/`built` | ✅ |
| Firebase CLI mit erlaubten Installationsskripten | ✅ |

Nicht übernommen, weil es das Programm hier nicht gibt oder es Windows-spezifisch ist: ripgrep (nur als Shell-Funktion von
Claude Code), jq (macOS bringt `/usr/bin/jq` mit, kein Homebrew-Paket), CMake (nur aus den Android-Kommandozeilen-Werkzeugen),
Bionic, Kimi Desktop, .NET SDK 8, Python Launcher, Visual Studio 2026 / Build Tools, Windows SDK, WSL, Windows-Terminal,
App Installer (winget), `zip-werkzeug.ps1` (Gradle, Kotlin und uv kommen hier aus Homebrew), winget-Abfrage-Cache und
Hersteller-Installer bei falschem winget-Manifest.

---

## F) Windows-Gleichstand 1.3.44 (macOS 1.0.20, 28.10.2026)

| Windows | macOS |
|---|---|
| 1.3.41/1.3.42 Knopf „An Claude Code übergeben“ im roten Fehlerband: Auftrag mit Problem und Protokollende (14 000 Zeichen) über den OpenLauncher-Start (macOS: nur die Zeilen dieses Programms aus dem Tagesprotokoll; `--jarvis-auftrag`, Opus 5.5, minimal, Schnellmodus, high), Exit-Code geprüft, Doppelklick gesperrt | ✅ `Services/ClaudeUebergabe.swift`, nutzt `OpenCodeSitzungAktualisierer.launcherStarten` |
| 1.3.43/1.3.44 Meldet ein selbst-aktualisierendes CLI „already in progress“: verwaiste Reste derselben Programmdatei samt Nachfahren beenden, dann ein neuer Versuch | ✅ Waise = Elternprozess launchd (PID 1) und mindestens 2 min alt, Suche über den Programmpfad aller Prozesse (nicht pgrep, Claudes Installer heißt „2.1.x“), Nachfahren über `Kommandozeile.prozessbaum`, SIGTERM dann SIGKILL, gezählt nur, was wirklich endete (1.0.22) |

Nicht übernommen: der Umweg über eine `.cmd`-Datei für erhöhte Rechte – auf dem Mac läuft die App nie erhöht.

## G) macOS → Windows 1.3.49 (29.10.2026)

Aus den macOS-Commits 1.0.21, die Windows noch fehlten, übernommen:

| macOS | Windows |
|---|---|
| 1.0.21 Übergabe an Claude Code enthält nur die Zeilen dieses Programms aus dem Tagesprotokoll, `\r`-Fortschrittszeilen getrennt, Zeilen auf 2000 Zeichen gekappt | ✅ `Services/ClaudeUebergabe.cs` |
| 1.0.21 Update-Waisen: Suche über den Programmpfad aller Prozesse, nur ab 2 min Laufzeit, gezählt nur, was wirklich endete | ✅ `Providers/CliAktualisierer.cs` |
| 1.0.21 Android Studio Zeitlimit 120 Minuten | ✅ `programs.json` |

Nicht übernommen, weil nur macOS: Windscribe per `brew fetch` + eigener Installer (1.0.18/1.0.19), Casks vor dem Beenden vorab laden (1.0.22/1.0.24), Fensteranpassung ans MacBook (1.0.17).
