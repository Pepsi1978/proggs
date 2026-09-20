# UpdateZentrale

Ein Windows-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
LM Studio (inklusive Engines und Runtimes), Claude Desktop, Codex Desktop, Claude Code CLI,
Codex CLI, Stream Deck sowie die eigenen Werkzeuge OpenLauncher, TVO und CVO.

Start über die Desktop-Verknüpfung **UpdateZentrale** (`create_shortcut.ps1` legt sie an).

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Läuft beim Start automatisch für alle Einträge, danach jederzeit einzeln oder gesammelt. Zeigt installierte Version → verfügbare Version. |
| **Aktualisieren** | Die Schaltfläche lädt nur dann zum Klick ein, wenn wirklich eine neuere Version vorliegt. Ist alles aktuell, steht dort „Aktuell" und sie ist abgeschaltet. |
| **Laufende Programme** | Wird pfadgenau erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt und lässt sich auf eine geplante Aufgabe mit Administratorrechten umstellen. |
| **Als Administrator** | Schiebeschalter pro Programm sowie für die UpdateZentrale selbst. |
| **Hell und Dunkel** | Umschalter oben rechts; die Wahl wird gemerkt, die Fenstertitelleiste zieht mit. |
| **Protokoll** | Rechts oben steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |
| **Terminal** | Rechts unten eine eingebaute PowerShell-Zeile mit den Rechten der UpdateZentrale, dazu eine Schaltfläche für ein echtes Terminalfenster. |

### Administratorrechte

* Die UpdateZentrale markiert sich beim ersten Start selbst mit dem Windows-Kompatibilitätsschalter
  **RUNASADMIN**. Dadurch startet sie erhöht – egal ob über die Desktop-Verknüpfung, das Startmenü
  oder einen Doppelklick auf die Programmdatei. Der Schalter in der Fußzeile schaltet das wieder ab.
* **Ausnahme Autostart:** Programme, die Administratorrechte brauchen, überspringt Windows im
  normalen Autostart (`HKCU\...\Run`) still – das ist eine Vorgabe der Benutzerkontensteuerung.
  Deshalb bietet jede betroffene Karte die Umstellung auf eine **geplante Aufgabe** an
  (Anmelde-Trigger, höchste Rechte, keine Rückfrage). Der bisherige Autostart-Eintrag wird dabei
  gesichert und beim Zurückstellen wiederhergestellt. Das Anlegen der Aufgabe setzt voraus, dass die
  UpdateZentrale gerade selbst erhöht läuft.
* Das Programmmanifest bleibt bewusst auf `asInvoker`: ein fest erzwungenes
  `requireAdministrator` würde auch jeden Installer erhöht starten, und benutzerbezogene
  Installationen (winget, Squirrel) landen dann im falschen Profil.

---

## Neue Programme hinzufügen (ohne Neubau)

Alles steht in **`programs.json`** im Projektordner. Die App liest die Datei bei jedem Start und
über die Schaltfläche **Neu laden**. Ein neues Programm ist also ein weiteres Objekt in
`programme` – kein Code, kein Build.

```jsonc
{
  "id": "beispiel",                    // eindeutig, dient als Schlüssel für Einstellungen und Aufgaben
  "name": "Beispielprogramm",
  "gruppe": "KI-Anwendungen",          // Überschrift in der Liste; freie Wahl
  "beschreibung": "Kurztext auf der Karte.",
  "art": "winget",                     // siehe Tabelle unten
  "wingetId": "Hersteller.Paket",
  "exePfad": "%LOCALAPPDATA%/Programs/Beispiel/Beispiel.exe",
  "prozesse": ["Beispiel"],            // Hauptprozess(e), ohne .exe
  "helferProzesse": ["BeispielTray"],  // Helfer, die den Installer sonst blockieren
  "beendenVorUpdate": true,
  "neuStartenNachUpdate": true,
  "akzent": "#3FA9F5"                  // Farbe des Kennzeichens
}
```

Pfade werden mit **Schrägstrichen** geschrieben (kein Escaping nötig); `%VARIABLEN%` werden
aufgelöst und Windows bekommt intern die gewohnte Schreibweise.

Prozessnamen werden immer gegen den Ordner aus `exePfad` geprüft. Kollidieren zwei Programme im
Namen und lässt sich das nicht über den Pfad trennen (etwa `claude` als CLI und als Store-App),
bleibt `prozesse` besser leer – lieber kein Merkmal als ein falsches.

### Die vier Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `winget` | Alles, was winget kennt | `wingetId` | `winget list --id …` | `winget upgrade --id … --silent` |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` | `exePfad updateArgumente` |
| `msstore` | MSIX-Pakete | `appxName`, `packageFamilyName` | `Get-AppxPackage` | Store-Seite oder – mit `selbstAktualisierend` – die App selbst |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `statusPraefix` | `git rev-list HEAD..origin/main` + csproj-Version gegen gebaute Exe | ruft das vorhandene PowerShell-Skript auf |

Weitere optionale Felder:

| Feld | Bedeutung |
|---|---|
| `versionsArgumente` | Argumente, die die installierte Version ausgeben (z. B. `--version`) |
| `pruefArgumente` | Trockenlauf-Befehl; Zeilen mit `→` bzw. `->` gelten als geplante Updates |
| `npmPaket` | Vergleichsquelle für die neueste Version, wenn das Werkzeug auf npm liegt |
| `selbstAktualisierend` | MSIX-Paket mit eigenem Updater: die Schaltfläche öffnet die App statt des Stores |
| `appxAnwendungsId` | Anwendungs-ID im MSIX-Paket (fast immer `App`) |
| `startArgumente` | Argumente beim Start über die Schaltfläche „Starten" und in der geplanten Aufgabe |
| `zeitlimitMinuten` | Zeitlimit des Update-Laufs (Standard 20) |
| `dialogWartezeitSekunden` | Nur `reposkript`: wie lange auf den Ja/Nein-Klick gewartet wird |
| `hinweis` | Kursive Zeile auf der Karte |
| `pfeilZaehlen` | Dokumentiert, dass die Prüfausgabe ihre Updates als `alt → neu` auflistet |

---

## Besonderheiten der eingetragenen Programme

* **LM Studio Engines & Runtimes** – eigener Eintrag. Geprüft wird mit
  `lms runtime update --all --dry-run`, aktualisiert mit `lms runtime update --all --yes`. Deckt
  llama.cpp für CPU/AVX2, NVIDIA CUDA, CUDA12 und Vulkan ab. Lädt teils mehrere Gigabyte;
  LM Studio darf dabei laufen.
* **Claude Desktop und Codex Desktop** – beides MSIX-Pakete, die sich **selbst** aktualisieren
  (nicht über den Microsoft Store). Die Karte zeigt die installierte Paketversion, die
  Schaltfläche öffnet die jeweilige App, in der das Update ausgelöst wird.
* **Codex Desktop vs. Codex CLI** – zwei getrennte Installationen: das MSIX-Paket und der
  Standalone-Build unter `~/.codex/packages`. Deshalb zwei Karten mit unterschiedlichen Wegen.
* **Claude Code CLI** – laufende Sessions werden nie beendet; das Update greift beim nächsten Start.
* **Stream Deck** – die Helferdienste werden beim Update mitbeendet, sonst hängt der Installer.
* **OpenLauncher / TVO / CVO** – die App ruft ausschließlich die vorhandenen Skripte auf
  (`update-launcher.ps1`, `rebuild-overlay.ps1`), **nie mit `-Force`**, und beendet deren Prozesse
  niemals selbst. Jedes Skript zeigt sein eigenes Ja/Nein-Fenster; die Antwort (`started`,
  `already-current`, `cancelled`, `no-answer`) wird ausgelesen und im Klartext angezeigt.

---

## Dateien

| Datei | Zweck |
|---|---|
| `programs.json` | Der Katalog. Hier werden Programme hinzugefügt. |
| `Models/` | Katalog-Eintrag und Prüfergebnis |
| `Services/` | Pfade, Prozesse, Registry, Aufgabenplanung, Rechte, Terminal, Kommandozeile, Einstellungen, Darstellung, Konverter |
| `Providers/` | Je eine Implementierung pro Update-Art |
| `ViewModels/` | Karten- und Fensterlogik |
| `HauptFenster.xaml` | Oberfläche |
| `Theme.xaml` | Formen und Typografie (ohne Farben) |
| `Themes/Dunkel.xaml`, `Themes/Hell.xaml` | Die beiden Farbsätze; gleiche Schlüssel, zur Laufzeit tauschbar |
| `create_shortcut.ps1` | Desktop-Verknüpfung |
| `make_icon.ps1` | Erzeugt `app.ico` |

Benutzereinstellungen (Administrator-Schalter, Hell/Dunkel, gesicherte Autostart-Einträge) liegen
außerhalb des Repos in `%LOCALAPPDATA%\UpdateZentrale\settings.json`, damit `programs.json`
sauber bleibt.

## Bauen

```powershell
cd ~\proggs\UpdateZentrale
dotnet build -c Release
pwsh -File .\create_shortcut.ps1
```
