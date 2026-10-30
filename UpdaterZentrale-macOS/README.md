# Updater-Zentrale macOS

Ein Mac-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
Claude Code CLI,
Codex CLI, Stream Deck sowie die eigenen Werkzeuge OpenLauncher, TVO und CVO.

macOS-Fassung von `~/proggs/UpdateZentrale` (Windows). Was gleich blieb und was sich zwangsläufig
unterscheidet, steht in **[PORTING.md](PORTING.md)**.

Start über die Verknüpfung **Updater-Zentrale macOS** auf dem Schreibtisch
(`tools/install-desktop-icon.sh` legt sie an – mit eigenem Symbol und ohne den kleinen
Verknüpfungspfeil) oder über Spotlight.

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Alle Einträge werden gleichzeitig geprüft; die Kopfzeile zeigt „x von n fertig“. Zeigt installierte Version → verfügbare Version. Das Ergebnis wird gemerkt (`pruefstand.json`): Beim nächsten Öffnen steht es sofort da, neu geprüft wird nur, was älter als 60 Minuten ist – die eigenen Werkzeuge immer. |
| **Repo-Abgleich** | Vor der Prüfung der eigenen Werkzeuge wird `~/proggs` einmal mit GitHub abgeglichen: `git fetch` und Vorspulen (`--ff-only`), nie ein Merge. Lässt sich das Repo nicht vorspulen, steht das in der Kopfzeile, und die Karten zählen weiter die wartenden Commits. |
| **Aktualisieren** | Läuft für jedes Programm still durch, ohne dass eine fremde Oberfläche aufgeht. Die Schaltfläche lädt nur dann zum Klick ein, wenn wirklich eine neuere Version vorliegt; ist alles aktuell, steht dort „Aktuell" und sie ist abgeschaltet. |
| **Sammelknöpfe** | Zwei unabhängige Knöpfe: „⚠ CLI-kritische Updates installieren“ und „Updates installieren · nicht CLI-kritisch“. Je eine Rückfrage mit der geplanten Reihenfolge, dann in Phasen (siehe „Zwei Sammelknöpfe“). Unabhängige Selbst-Updater laufen parallel neben der seriellen Bahn. |
| **Dauer und Sortierung** | Jede Karte zeigt, wie lange ihr Update zuletzt gedauert hat (Median der letzten drei echten Updates) oder eine Schätzung. Der Schalter „⏱ nach Dauer“ sortiert innerhalb jeder Gruppe die kürzesten nach oben; die Gruppen bleiben zusammen. |
| **Laufende Programme** | Wird pfadgenau erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt – gelesen aus `~/Library/LaunchAgents` und den Anmeldeobjekten. Nur lesend. |
| **Hell und Dunkel** | Umschalter oben rechts; die Wahl wird gemerkt, die Fenstertitelleiste zieht mit. |
| **Protokoll** | Rechts oben steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |
| **Terminal** | Rechts unten eine eingebaute zsh-Zeile im Repo-Ordner, dazu eine Schaltfläche für ein echtes Terminalfenster. |

### Zwei Sammelknöpfe und ihre Reihenfolge

Gleiche Logik wie die Windows-Fassung (1.3.31–1.3.40):

* **⚠ CLI-kritische Updates installieren** – nur Einträge mit `cliRisiko`. Die Rückfrage davor ist zugleich die
  Aufforderung, Claude Code, Codex, OpenCode und alle Terminalfenster zu schließen. Auf dem Mac eingeordnet nach dem,
  was hier wirklich passiert: iTerm2 (schließt alle Fenster), JDK 17/21 (Homebrew löscht
  den alten JDK-Ordner, Gradle-/Kotlin-Daemons brechen ab), Android SDK (adb-Server), VS Code (integriertes Terminal),
  Windscribe (VPN-Abbruch). Homebrew-Formeln wie node, git und pwsh beenden laufende Prozesse nicht und bleiben unkritisch.
* **Updates installieren · nicht CLI-kritisch** – alles andere, in dieser Reihenfolge:
  1. Repo-Abgleich schon bei der Prüfung.
  2. **Updater-Zentrale zuerst und allein.** Der Rest wird in `settings.json` (`SammelFortsetzen`) gesichert; die neue
     Fassung setzt ihn nach ihrer Startprüfung ohne zweite Rückfrage fort (nur innerhalb von 30 Minuten).
  3. Phase 1 – eigene Werkzeuge (OpenLauncher, TVO, CVO).
  4. Phase 2 – CLI-kritisch gehört zum anderen Knopf; enthält ein Plan sie doch (nach einer Fortsetzung), fragt der Lauf davor.
  5. Phase 3 – der Rest. Seriell: brew (eine Sperre für alles), npm, sdkmanager. Parallel daneben
     (höchstens drei): nur Einträge mit `parallel` – Claude Code, Codex, Antigravity, rustup, OpenCode-Sitzung.
  6. Zum Schluss alles mit `zuletzt`.

Innerhalb jeder Phase geht das kürzeste erwartete Update zuerst; `nach` zieht Voraussetzungen davor
(npm-Werkzeuge nach Node.js, Gradle/Kotlin/Android SDK nach dem JDK).
`hauptversionNurPerKlick` (TypeScript, npm, FFmpeg) bleibt bei einem Hauptversionssprung draußen.
Die Fußzeile zeigt den Gesamtstand und je laufendem Update Position, Phase, Startzeit und erwartete Dauer.

### Protokolle und Verifikation

Jeder Update-Lauf wird **überprüft und dauerhaft mitgeschrieben** – ein Programm gilt erst dann
als aktualisiert, wenn sich nachweislich etwas geändert hat, nicht schon weil ein Installer
„fertig" gemeldet hat.

Dafür liest die App vor und nach dem Lauf einen *Fingerabdruck* und vergleicht beide:

| Art | Fingerabdruck |
|---|---|
| `brew` | `CFBundleShortVersionString` aus der Info.plist des Programms |
| `cli` | die gemeldete Version, bei Werkzeugen ohne Version stattdessen der Trockenlauf-Plan – der muss danach leer sein |
| `reposkript` | Version **und** Schreibzeit der gebauten Programmdatei (die Info.plist-Version allein ändert sich beim Neubau nicht) |

Daraus wird eines von fünf Ergebnissen:

| Ergebnis | Bedeutung |
|---|---|
| **Erfolgreich** | Der Fingerabdruck hat sich geändert – das Update ist nachweislich angekommen. |
| **Ausstehend** | Der Installer hat das Update abgelegt, es wird beim nächsten Start des Programms aktiv. Beim nächsten Start der Updater-Zentrale wird das rückwirkend bestätigt; kommt es binnen sieben Tagen nicht an, wird es als Fehler ausgewiesen. |
| **Abgebrochen** | Es war nichts offen, oder du hast im Rückfragefenster abgelehnt. |
| **Fehlgeschlagen** | Das Werkzeug selbst hat einen Fehler gemeldet – mit Grund. |
| **Nicht verifiziert** | Der heikelste Fall: Erfolg gemeldet, aber nichts hat sich geändert. Wird rot ausgewiesen statt stillschweigend als Erfolg verbucht. |

Abgelegt wird alles in `~/Library/Application Support/UpdaterZentrale/logs/`:

* `updates-JJJJ-MM-TT.log` – ein Tagesprotokoll mit Kopf, roher Ausgabe und Ergebnis je Lauf.
* `verlauf.jsonl` – eine Zeile je Lauf, maschinenlesbar. Daraus lädt die App beim Start, was
  zuletzt passiert ist. Die Feldnamen sind dieselben wie unter Windows; die Werte von
  `Ergebnis` und `Zeit` schreibt Swift lesbarer (Text statt Zahl, ISO-8601-Zeit). Das fällt nicht
  auf: jede Plattform hat ihren eigenen Protokollordner.

Die Schaltfläche **Protokolle** in der Fußzeile öffnet diesen Ordner.

### Keine Administratorrechte

Anders als unter Windows gibt es hier **keinen Administrator-Komplex**: macOS kennt weder den
Kompatibilitätsschalter RUNASADMIN noch die Benutzerkontensteuerung, und Homebrew verweigert den
Betrieb als root ausdrücklich. Alles läuft mit den normalen Benutzerrechten. Begründung in
[PORTING.md](PORTING.md), Abschnitt C.

Einzige Ausnahme: Casks, die alte Pakete per `sudo` wegräumen (`dotnet-sdk`). Dafür zeigt die App
einen Passwortdialog (Askpass-Helfer unter `~/Library/Application Support/UpdaterZentrale/askpass.sh`,
übergeben als `SUDO_ASKPASS`). Abbrechen oder fünf Minuten ohne Eingabe lassen das Update sauber
scheitern – mit Grund auf der Karte.

---

## Neue Programme hinzufügen (ohne Neubau)

Alles steht in **`programs.json`** im Projektordner. Die App liest die Datei bei jedem Start und
über die Schaltfläche **Neu laden**. Ein neues Programm ist also ein weiteres Objekt in
`programme` – kein Code, kein Build.

```jsonc
{
  "id": "beispiel",                          // eindeutig, Schlüssel für Einstellungen und Protokoll
  "name": "Beispielprogramm",
  "gruppe": "Kommandozeile",                 // Überschrift in der Liste; freie Wahl
  "beschreibung": "Kurztext auf der Karte.",
  "art": "brew",                             // siehe Tabelle unten
  "cask": "beispiel",
  "appPfad": "/Applications/Beispiel.app",
  "bundleId": "com.beispiel.app",
  "prozesse": [],                            // nur für Kommandozeilen-Werkzeuge nötig
  "helferProzesse": ["beispiel-helper"],
  "beendenVorUpdate": true,
  "neuStartenNachUpdate": true,
  "akzent": "#3FA9F5"                        // Farbe des Kennzeichens
}
```

Pfade dürfen `~`, `$HOME` und der Vollständigkeit halber auch `%USERPROFILE%`/`%LOCALAPPDATA%`
enthalten (damit ein Eintrag aus dem Windows-Katalog nicht stumm ins Leere zeigt).

### Die Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `brew` | Grafische Programme mit Homebrew-Cask, **oder** Kommandozeilen-Werkzeuge als Homebrew-Formel | `cask` (+ `appPfad`) **oder** `formel` | Info.plist gegen `brew info --cask … --json=v2` | `brew upgrade --cask …` bzw. `brew install --cask --force …` |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` | `exePfad updateArgumente` |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `projektDatei` | `git rev-list HEAD..origin/main` + Quell-Info.plist gegen gebaute App | ruft das vorhandene Shell-Skript auf |
| `opencodesitzung` | OpenCode mit Mac-Fix | `npmPaket` | npm-Version gegen `~/.local/share/opencode-mousefix/current.json` | startet eine OpenLauncher-Sitzung (`--jarvis-auftrag`: Opus 5.5, minimal, Schnellmodus, high), die die neue Version mit `opencode-setup/build-install-macos-tuifix.sh` baut und den Fix bei Bedarf anpasst; die Karte steht auf „Sitzung baut“, bis eine Prüfung die neue Version sieht |

Weitere optionale Felder:

| Feld | Bedeutung |
|---|---|
| `bundleId` | Bundle-Kennung; genauestes Merkmal für „läuft gerade" und zum Beenden |
| `versionsArgumente` | Argumente, die die installierte Version ausgeben (z. B. `--version`) |
| `pruefArgumente` | Trockenlauf-Befehl; Zeilen mit `→` bzw. `->` gelten als geplante Updates |
| `npmPaket` | Vergleichsquelle für die neueste Version, wenn das Werkzeug auf npm liegt |
| `startArgumente` | Argumente beim Start über die Schaltfläche „Starten" |
| `zeitlimitMinuten` | Zeitlimit des Update-Laufs (Standard 20) |
| `dialogWartezeitSekunden` | Nur `reposkript`: wie lange auf den Ja/Nein-Klick gewartet wird |
| `statusPraefix` | Nur `reposkript`: Praefix der Statuszeile des Skripts. **Leer lassen**, wenn das Skript keine ausgibt – dann fragt die App selbst vorher nach |
| `hinweis` | Kursive Zeile auf der Karte |
| `formel` | Nur `brew`: Homebrew-Formel statt Cask (git, node, go, `oven-sh/bun/bun` …). Installierte und verfügbare Version kommen aus `brew info --json=v2`, Update über `brew upgrade`. |
| `pruefExitCodes` | Nur `cli`: Exit-Codes der Prüfung, die eine gültige Antwort sind (Standard `[0]`). `rustup check` endet mit 100, sobald Updates anstehen. |
| `pruefAbschnitt` | Nur `cli`: erst Zeilen nach der ersten Zeile mit diesem Text zählen (sdkmanager: `Available Updates`). Fehlt der Abschnitt, ist nichts offen. |
| `pruefZeilenMuster` | Nur `cli`: regulärer Ausdruck für eine geplante Update-Zeile statt des Pfeils – für Werkzeuge mit Tabellenausgabe. |
| `vorhandenPfad` | Merkmal „vorhanden“ ohne startbare Programmdatei (Homebrew-Formeln: `/opt/homebrew/bin/fd`, OpenCode: `current.json`) – für `ausblendenWennFehlt` |
| `cliRisiko` | Grund, warum dieses Update laufende CLI-Sitzungen beenden kann. Die Karte zeigt ⚠ neben dem Namen (Grund im Tooltip), der Einzelknopf fragt nach, und das Update läuft nur über „⚠ CLI-kritische Updates installieren“. |
| `uebernahmeText` | Text des Bandes „wird übernommen“ statt des Standards |
| `hauptversionNurPerKlick` | Ein Sprung der ersten Versionsstelle bleibt aus den Sammelknöpfen draußen und läuft nur per Klick |
| `dauerSchaetzungSekunden` | Erwartete Update-Dauer, bis echte Messungen vorliegen (sonst ein Viertel von `zeitlimitMinuten`) |
| `zuletzt` | Immer das letzte Update eines Sammellaufs, nach allen Phasen |
| `parallel` | Darf im Sammellauf in der parallelen Bahn laufen (nur `cli` und `opencodesitzung`, nie mit `cliRisiko`) |
| `nach` | Ids, die im selben Lauf vorher drankommen müssen |
| `versionsMuster` / `versionsUrlMuster` | Regulärer Ausdruck, dessen Gruppe 1 die Version in der Versionsausgabe bzw. der Antwort von `versionsUrl` ist |
| `updateExePfad` | Nur `cli`: anderes Programm für den Update-Lauf als für die Versionsabfrage (npm-Pakete: Version über `tsc --version`, Update über `npm install -g …`). Läuft im eigenen Ordner, damit npm seine Module findet. |

Ein Cask ohne `appPfad` (z. B. `dotnet-sdk`) liest die installierte Version aus dem `installed`-Feld von brew.

### Entwicklungswerkzeuge

Die Gruppen „Entwicklung: …“ halten die Programmierumgebung aktuell – Gegenstück zur Windows-Fassung 1.3.25:
.NET SDK, Node.js, OpenJDK 17/21, Rust (rustup), Go, Python 3.14, Kotlin, Gradle, VS Code, Android Studio,
Android SDK (sdkmanager), Git, GitHub CLI, Bun, Biome, iTerm2, PowerShell 7 sowie globale npm-Pakete
(TypeScript, tsx, npm, OpenCode CLI, Firebase nur wenn installiert). Visual Studio, Build Tools,
CMake, WSL, Python Launcher und Windows-Terminal gibt es auf dem Mac nicht bzw. nicht installiert.
Der Cask `android-commandlinetools` ist bewusst **kein** Eintrag: sein Ordner ist das SDK selbst
(`ANDROID_HOME`), ein Cask-Upgrade würde die installierten SDK-Pakete mitreißen.

---

## Besonderheiten der eingetragenen Programme

* **Claude Desktop** – bringt einen eigenen Updater mit. Das Homebrew-Rezept kann
  deshalb **älter** sein als das Installierte; dann steht auf der Karte „Auf dem neuesten Stand –
  neuer als das Homebrew-Rezept". Ein Downgrade wird nie angeboten.
* **Codex Desktop** – auf dem Mac ist das die **ChatGPT.app** (Bundle-Kennung `com.openai.codex`).
  Ein Store-Paket wie unter Windows gibt es hier nicht.
* **Claude Code CLI** – laufende Sessions werden nie beendet; das Update greift beim nächsten Start.
* **Stream Deck** – auf diesem Mac nicht installiert. Der Eintrag bleibt stehen, damit die Karte
  das sagt statt zu schweigen.
* **OpenLauncher / TVO / CVO** – die App ruft ausschließlich die vorhandenen Skripte auf
  (`OpenLauncherMac/update-launcher.sh`, `rebuild-overlay.sh`), **nie mit einem Force-Schalter**,
  und beendet deren Prozesse niemals selbst.
  `update-launcher.sh` zeigt sein eigenes Ja/Nein-Fenster; seine Antwort (`started`,
  `already-current`, `cancelled`, `no-answer`) wird ausgelesen und im Klartext angezeigt.
  `rebuild-overlay.sh` hat auf dem Mac **kein** eigenes Fenster – dort fragt die Updater-Zentrale
  selbst, bevor sie es startet.

---

### Protokolle der Sammelläufe

* `verlauf.jsonl` trägt seit 1.0.16 `DauerSekunden`, `ArbeitSekunden`, `UpdateAufrufe`, `SammelId`, `SammelPhase` und `SammelPosition`.
* `sammellaeufe.jsonl` – je Sammellauf eine Zeile `plan` (Reihenfolge, Phase, parallel, erwartete Dauer) und eine Abschlusszeile mit Beginn, Ende, Dauer und Ergebnis je Programm.
* Im Tagesprotokoll stehen im Fuß jedes Laufs `Programm:`, `Dauer:` und `Sammellauf:`.

### Selbst-Update

`update-zentrale.sh --app-pid <PID>` baut, legt eine Zwischenkopie ab und startet `tools/selbstupdate-helfer.sh`. Die App
schreibt Protokoll und Verlauf fertig und beendet sich selbst; der Helfer wartet auf genau diesen Prozess (höchstens 60 s),
legt erst dann die neue Fassung nach `/Applications` und startet sie. Die neue Fassung bestätigt das ausstehende Update bei
ihrem Start. Jeder Schritt steht in `~/Library/Logs/UpdaterZentrale-selbstupdate.log`.

## Dateien

| Datei | Zweck |
|---|---|
| `programs.json` | Der Katalog. Hier werden Programme hinzugefügt. |
| `UpdaterZentrale/Models/` | Katalog-Eintrag, Prüfergebnis und Update-Bericht |
| `UpdaterZentrale/Services/` | Pfade, Versionsvergleich, Kommandozeile, Protokollierung, Prozesse, Autostart, Terminal, Einstellungen, Farben |
| `UpdaterZentrale/Providers/` | Je eine Umsetzung pro Update-Art |
| `UpdaterZentrale/ViewModels/` | Karten- und Fensterlogik |
| `UpdaterZentrale/Views/` | Oberfläche (AppKit, programmatisch) |
| `build.sh` | Baut das `.app`-Bündel und signiert es |
| `tools/make-icon.sh` | Erzeugt `AppIcon.icns` |
| `tools/install-desktop-icon.sh` | Legt die Schreibtisch-Verknüpfung an – mit eigenem Symbol, ohne Verknüpfungspfeil |
| `PORTING.md` | Paritätsliste, bewusste Abweichungen, was nicht portiert wurde |

Benutzereinstellungen (Hell/Dunkel) liegen außerhalb des Repos in
`~/Library/Application Support/UpdaterZentrale/settings.json`, damit `programs.json` sauber bleibt.

## Bauen

```bash
cd ~/proggs/UpdaterZentrale-macOS
bash build.sh
ditto "build/Updater-Zentrale macOS.app" "/Applications/Updater-Zentrale macOS.app"
bash tools/install-desktop-icon.sh
```

Die Version steht in `UpdaterZentrale/Info.plist` (`CFBundleShortVersionString` **und**
`CFBundleVersion`, beide gleich halten). Den Build-Zeitstempel setzt `build.sh` selbst – er wird
nie von Hand eingetragen.
