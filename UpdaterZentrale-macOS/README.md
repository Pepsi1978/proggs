# Updater-Zentrale macOS

Ein Mac-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
LM Studio (inklusive Engines und Runtimes), Claude Desktop, Codex Desktop, Claude Code CLI,
Codex CLI, Stream Deck sowie die eigenen Werkzeuge OpenLauncher, TVO und CVO.

macOS-Fassung von `~/proggs/UpdateZentrale` (Windows). Was gleich blieb und was sich zwangsläufig
unterscheidet, steht in **[PORTING.md](PORTING.md)**.

Start über die Verknüpfung **Updater-Zentrale macOS** auf dem Schreibtisch
(`tools/install-desktop-icon.sh` legt sie an) oder über Spotlight.

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Läuft beim Start automatisch für alle Einträge, danach jederzeit einzeln oder gesammelt. Zeigt installierte Version → verfügbare Version. |
| **Aktualisieren** | Läuft für jedes Programm still durch, ohne dass eine fremde Oberfläche aufgeht. Die Schaltfläche lädt nur dann zum Klick ein, wenn wirklich eine neuere Version vorliegt; ist alles aktuell, steht dort „Aktuell" und sie ist abgeschaltet. |
| **Laufende Programme** | Wird pfadgenau erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt – gelesen aus `~/Library/LaunchAgents` und den Anmeldeobjekten. Nur lesend. |
| **Hell und Dunkel** | Umschalter oben rechts; die Wahl wird gemerkt, die Fenstertitelleiste zieht mit. |
| **Protokoll** | Rechts oben steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |
| **Terminal** | Rechts unten eine eingebaute zsh-Zeile im Repo-Ordner, dazu eine Schaltfläche für ein echtes Terminalfenster. |

### Protokolle und Verifikation

Jeder Update-Lauf wird **überprüft und dauerhaft mitgeschrieben** – ein Programm gilt erst dann
als aktualisiert, wenn sich nachweislich etwas geändert hat, nicht schon weil ein Installer
„fertig" gemeldet hat.

Dafür liest die App vor und nach dem Lauf einen *Fingerabdruck* und vergleicht beide:

| Art | Fingerabdruck |
|---|---|
| `brew` | `CFBundleShortVersionString` aus der Info.plist des Programms |
| `cli` | die gemeldete Version, bei den LM-Studio-Runtimes stattdessen der Trockenlauf-Plan – der muss danach leer sein |
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
  zuletzt passiert ist. Das Format ist mit der Windows-Fassung identisch.

Die Schaltfläche **Protokolle** in der Fußzeile öffnet diesen Ordner.

### Keine Administratorrechte

Anders als unter Windows gibt es hier **keinen Administrator-Komplex**: macOS kennt weder den
Kompatibilitätsschalter RUNASADMIN noch die Benutzerkontensteuerung, und Homebrew verweigert den
Betrieb als root ausdrücklich. Alles läuft mit den normalen Benutzerrechten. Begründung in
[PORTING.md](PORTING.md), Abschnitt C.

---

## Neue Programme hinzufügen (ohne Neubau)

Alles steht in **`programs.json`** im Projektordner. Die App liest die Datei bei jedem Start und
über die Schaltfläche **Neu laden**. Ein neues Programm ist also ein weiteres Objekt in
`programme` – kein Code, kein Build.

```jsonc
{
  "id": "beispiel",                          // eindeutig, Schlüssel für Einstellungen und Protokoll
  "name": "Beispielprogramm",
  "gruppe": "KI-Anwendungen",                // Überschrift in der Liste; freie Wahl
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

### Die drei Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `brew` | Grafische Programme mit Homebrew-Cask | `cask`, `appPfad` | Info.plist gegen `brew info --cask … --json=v2` | `brew upgrade --cask …` bzw. `brew install --cask --force …` |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` | `exePfad updateArgumente` |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `projektDatei` | `git rev-list HEAD..origin/main` + Quell-Info.plist gegen gebaute App | ruft das vorhandene Shell-Skript auf |

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

---

## Besonderheiten der eingetragenen Programme

* **LM Studio Engines & Runtimes** – eigener Eintrag. Geprüft mit
  `lms runtime update --all --dry-run`, aktualisiert mit `lms runtime update --all --yes`. Deckt
  llama.cpp für Apple Silicon (Metal) und MLX ab. LM Studio darf dabei laufen.
* **Claude Desktop und LM Studio** – bringen einen eigenen Updater mit. Das Homebrew-Rezept kann
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
| `tools/install-desktop-icon.sh` | Legt die Schreibtisch-Verknüpfung an |
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
